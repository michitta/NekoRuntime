package pro.gravit.launcher.client.gui.scenes.serverinfo;

import animatefx.animation.*;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.ClientLauncherProcess;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.impl.ContextHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.scenes.debug.DebugScene;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerMenuScene;
import pro.gravit.launcher.client.gui.scenes.update.VisualDownloader;
import pro.gravit.launcher.hasher.FileNameMatcher;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.optional.OptionalView;
import pro.gravit.launcher.request.auth.SetProfileRequest;
import pro.gravit.utils.helper.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.function.Consumer;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerInfoScene extends AbstractScene {
    private ImageView avatar;
    private Image originalAvatarImage;
    private VisualDownloader downloader;
    private Pane downloadButton;
    private Pane playButton;
    private Pane updateButton;
    private ButtonBase deauthButton;
    private Button back;
    private ButtonBase clientSettings;
    private ButtonBase settings;

    public ServerInfoScene(JavaFXApplication application) {
        super("scenes/serverinfo/serverinfo.fxml", application);
    }

    RotateTransition rt;
    public boolean updateIsRun = false;

    @Override
    protected void doInit() throws Exception {
        SVGPath loading = LookupHelper.lookup(layout, "#loading");
        rt = new RotateTransition(Duration.millis(1000), loading);
        rt.setByAngle(360);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.setCycleCount(RotateTransition.INDEFINITE);
        avatar = LookupHelper.lookup(layout, "#avatar");
        originalAvatarImage = avatar.getImage();
        LookupHelper.<ImageView>lookupIfPossible(layout, "#avatar").ifPresent(
                (h) -> {
                    try {
                        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(h.getFitWidth(), h.getFitHeight());
                        clip.setArcWidth(h.getFitWidth());
                        clip.setArcHeight(h.getFitHeight());
                        h.setClip(clip);
                        h.setImage(originalAvatarImage);
                    } catch (Throwable e) {
                        LogHelper.warning("Skin head error");
                    }
                }
        );
        downloadButton = LookupHelper.lookup(layout, "#downloadButton");
        playButton = LookupHelper.lookup(layout, "#playButton");
        updateButton = LookupHelper.lookup(layout, "#updateButton");
        deauthButton = LookupHelper.lookup(layout, "#deauth");
        downloader = new VisualDownloader(application, this::errorHandle, (log) -> {
            contextHelper.runInFxThread(() -> addLog(log));
        });
        back = LookupHelper.lookup(layout, "#back");
        back.setOnAction((e) -> {
            try {
                switchScene(application.gui.serverMenuScene);
            } catch (Exception exception) {
                errorHandle(exception);
            }
        });
        clientSettings = LookupHelper.lookup(layout, "#clientSettings");
        clientSettings.setOnAction((e) -> {
            try {
                if (application.stateService.getProfile() == null)
                    return;
                switchScene(application.gui.optionsScene);
                application.gui.optionsScene.reset();
                application.gui.optionsScene.addProfileOptionals(application.stateService.getOptionalView());
            } catch (Exception ex) {
                errorHandle(ex);
            }
        });


        settings = LookupHelper.lookup(layout, "#settings");
        settings.setOnAction((e) -> {
            try {
                switchScene(application.gui.settingsScene);
                application.gui.settingsScene.reset();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        });
        reset();
    }
    public void addLog(String string) {
        LookupHelper.<Label>lookup(layout, "#postClientUpdate").setText(string);
    }

    @Override
    public void reset() {
        if (application.stateService.getOptionalView().all.size() == 0) clientSettings.setDisable(true);
        ClientProfile profile = application.stateService.getProfile();
        Pane content = LookupHelper.lookup(layout, "#content");
        try {
            Image img = new Image(JavaFXApplication.getResourceURL("images/components/" + profile.getTitle() + ".png").toString());
            BackgroundImage bImg = new BackgroundImage(img,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.DEFAULT,
                    new BackgroundSize(1.0, 1.0, true, true, false, false));
            Background bGround = new Background(bImg);
            content.setBackground(bGround);
        } catch (IOException ignored) {}
        new SlideInUp(content).play();
        new FadeIn(LookupHelper.lookup(layout, "#leftPane")).play();
        avatar.setImage(originalAvatarImage);
        Path clientfolder = DirBridge.dirUpdates.resolve(profile.getDir());
        Path assetfolder = DirBridge.dirUpdates.resolve(profile.getDir());
        if (IOHelper.exists(clientfolder) && IOHelper.exists(assetfolder)){
            playButton.setVisible(true);
            downloadButton.setVisible(false);
            updateButton.setVisible(false);
        } else {
            downloadButton.setVisible(true);
            updateButton.setVisible(false);
            playButton.setVisible(false);
        }
        application.pingService.getPingReport(profile.getDefaultServerProfile().name).thenAccept((report) -> {
            LogHelper.info(report.toString());
            if(report == null) {
                LookupHelper.<Labeled>lookup(layout,"#online").setText("хз " + "игроков");
            } else {
                LookupHelper.<Labeled>lookup(layout, "#online").setText(report.playersOnline + " игроков");
            }
        });
        LookupHelper.<Label>lookupIfPossible(layout, "#serverName").ifPresent((e) -> e.setText(profile.getTitle()));
        LookupHelper.<Label>lookupIfPossible(layout, "#serverDescription").ifPresent((e) -> e.setText(profile.getInfo()));
        LookupHelper.<Label>lookupIfPossible(layout, "#nickname").ifPresent((e) -> e.setText(application.stateService.getUsername()));
        LookupHelper.<Button>lookup(layout, "#launchClient").setOnMouseClicked((e) -> launchClient());
        LookupHelper.<Button>lookup(layout, "#downloadClient").setOnMouseClicked((e) -> launchClient());
        ServerMenuScene.putAvatarToImageView(application, application.stateService.getUsername(), avatar);
        back.setDisable(false);
        if (!application.stateService.getOptionalView().all.isEmpty()) {
            clientSettings.setDisable(false);
        }
        settings.setDisable(false);
    }

    @Override
    public String getName() {
        return null;
    }

    private void downloadClients(ClientProfile profile, JavaHelper.JavaVersion javaVersion, HashedDir jvmHDir) {
        try {
            Path target = DirBridge.dirUpdates.resolve(profile.getAssetDir());
            LogHelper.info("Start update to %s", target.toString());
            Consumer<HashedDir> next = (assetHDir) -> {
                Path targetClient = DirBridge.dirUpdates.resolve(profile.getDir());
                LogHelper.info("Start update to %s", targetClient.toString());
                sendUpdateRequest(profile.getDir(), targetClient, profile.getClientUpdateMatcher(), true, application.stateService.getOptionalView(), true, (clientHDir) -> {
                    LogHelper.info("Success update");
                    try {
                        doLaunchClient(target, assetHDir, targetClient, clientHDir, profile, application.stateService.getOptionalView(), javaVersion, jvmHDir);
                    } catch (Throwable e) {
                        LogHelper.error(e);
                        ContextHelper.runInFxThreadStatic(() -> LogHelper.info(String.format("launchClient error %s:%s", e.getClass().getName(), e.getMessage())));
                    }
                });
            };

            if(profile.getVersion().compareTo(ClientProfile.Version.MC164) <= 0) {
                sendUpdateRequest(profile.getAssetDir(), target, profile.getAssetUpdateMatcher(), true, null, false, next);
            } else {
                sendUpdateAssetRequest(profile.getAssetDir(), target, profile.getAssetUpdateMatcher(), true, profile.getAssetIndex(), next);
            }
        } catch (Throwable e) {
            rt.stop();
            back.setDisable(false);
            if (!application.stateService.getOptionalView().all.isEmpty()) {
                clientSettings.setDisable(false);
            }
            settings.setDisable(false);
            deauthButton.setDisable(false);
            application.gui.serverInfoScene.reset();
        }
    }

    private void doLaunchClient(Path assetDir, HashedDir assetHDir, Path clientDir, HashedDir clientHDir, ClientProfile profile, OptionalView view, JavaHelper.JavaVersion javaVersion, HashedDir jvmHDir) {
        RuntimeSettings.ProfileSettings profileSettings = application.getProfileSettings();
        if(javaVersion == null) {
            javaVersion = application.javaService.getRecommendJavaVersion(profile);
        }
        if(javaVersion == null) {
            javaVersion = JavaHelper.JavaVersion.getCurrentJavaVersion();
        }
        ClientLauncherProcess clientLauncherProcess = new ClientLauncherProcess(clientDir, assetDir, javaVersion,
                clientDir.resolve("resourcepacks"), profile, application.stateService.getPlayerProfile(), view,
                application.stateService.getAccessToken(), clientHDir, assetHDir, jvmHDir);
        clientLauncherProcess.params.ram = profileSettings.ram;
        clientLauncherProcess.params.offlineMode = application.offlineService.isOfflineMode();
        if (clientLauncherProcess.params.ram > 0) {
            clientLauncherProcess.jvmArgs.add("-Xms" + clientLauncherProcess.params.ram + 'M');
            clientLauncherProcess.jvmArgs.add("-Xmx" + clientLauncherProcess.params.ram + 'M');
        }
        clientLauncherProcess.params.fullScreen = profileSettings.fullScreen;
        clientLauncherProcess.params.autoEnter = profileSettings.autoEnter;
        contextHelper.runCallback(() -> {
            Thread writerThread = CommonHelper.newThread("Client Params Writer Thread", true, () -> {
                try {
                    clientLauncherProcess.runWriteParams(new InetSocketAddress("127.0.0.1", Launcher.getConfig().clientPort));
                    if (!profileSettings.debug) {
                        LogHelper.debug("Params writted successful. Exit...");
                        LauncherEngine.exitLauncher(0);
                    }
                } catch (Throwable e) {
                    LogHelper.error(e);
                    if (getCurrentStage().getVisualComponent() instanceof DebugScene) { //TODO: FIX
                        DebugScene debugScene = (DebugScene) getCurrentStage().getVisualComponent();
                        debugScene.append(String.format("Launcher fatal error(Write Params Thread): %s: %s", e.getClass().getName(), e.getMessage()));
                        if (debugScene.currentProcess != null && debugScene.currentProcess.isAlive()) {
                            debugScene.currentProcess.destroy();
                        }
                    }
                }
            });
            writerThread.start();
            application.gui.debugScene.writeParametersThread = writerThread;
            clientLauncherProcess.start(true);
            contextHelper.runInFxThread(() -> {
                addLog("Выполняется запуск клиента...");
                application.gui.debugScene.onProcess(clientLauncherProcess.getProcess());
            });
        });
    }

    private String getJavaDirName(Path javaPath) {
        String prefix = DirBridge.dirUpdates.toAbsolutePath().toString();
        if (javaPath == null || !javaPath.startsWith(prefix)) {
            return null;
        }
        Path result = DirBridge.dirUpdates.relativize(javaPath);
        return result.toString();
    }

    private void showJavaAlert(ClientProfile profile) {
        if((JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM32 || JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM64) && profile.getVersion().compareTo(ClientProfile.Version.MC112) <= 0) {
            application.messageManager.showDialog(application.getTranslation("runtime.scenes.serverinfo.javaalert.lwjgl2.header"),
                    String.format(application.getTranslation("runtime.scenes.serverinfo.javaalert.lwjgl2.description"), profile.getRecommendJavaVersion()), () -> {}, () -> {}, true);
        } else {
            application.messageManager.showDialog(application.getTranslation("runtime.scenes.serverinfo.javaalert.header"),
                    String.format(application.getTranslation("runtime.scenes.serverinfo.javaalert.description"), profile.getRecommendJavaVersion()), () -> {}, () -> {}, true);
        }
    }

    public void launchClient() {
        ClientProfile profile = application.stateService.getProfile();
        if (profile == null)
            return;
        downloadButton.setVisible(false);
        playButton.setVisible(false);
        updateButton.setVisible(true);
        updateIsRun = true;
        rt.play();
        back.setDisable(true);
        clientSettings.setDisable(true);
        settings.setDisable(true);
        deauthButton.setDisable(true);
        processRequest(application.getTranslation("runtime.overlay.processing.text.setprofile"), new SetProfileRequest(profile), (result) -> contextHelper.runInFxThread(() -> {
            hideOverlay(0, (ev) -> {
                RuntimeSettings.ProfileSettings profileSettings = application.getProfileSettings();
                JavaHelper.JavaVersion javaVersion = null;
                for(JavaHelper.JavaVersion v : application.javaService.javaVersions) {
                    if(v.jvmDir.toAbsolutePath().toString().equals(profileSettings.javaPath)) {
                        javaVersion = v;
                    }
                }
                if(javaVersion == null && profileSettings.javaPath != null && !application.guiModuleConfig.forceDownloadJava) {
                    try {
                        javaVersion = JavaHelper.JavaVersion.getByPath(Paths.get(profileSettings.javaPath));
                    } catch (Throwable e) {
                        if(LogHelper.isDevEnabled()) {
                            LogHelper.error(e);
                        }
                        LogHelper.warning("Incorrect java path %s", profileSettings.javaPath);
                    }
                }
                if(javaVersion == null || application.javaService.isIncompatibleJava(javaVersion, profile)) {
                    javaVersion = application.javaService.getRecommendJavaVersion(profile);
                }
                if(javaVersion == null) {
                    showJavaAlert(profile);
                    back.setDisable(false);
                    if (!application.stateService.getOptionalView().all.isEmpty()) {
                        clientSettings.setDisable(false);
                    }
                    settings.setDisable(false);
                    deauthButton.setDisable(false);
                    downloadButton.setVisible(false);
                    playButton.setVisible(true);
                    updateButton.setVisible(false);
                    return;
                }
                String jvmDirName = getJavaDirName(javaVersion.jvmDir);
                if (jvmDirName != null) {
                    final JavaHelper.JavaVersion finalJavaVersion = javaVersion;
                    sendUpdateRequest(jvmDirName, javaVersion.jvmDir, null, true, application.stateService.getOptionalView(), false, (jvmHDir) -> {
                        if(JVMHelper.OS_TYPE == JVMHelper.OS.LINUX || JVMHelper.OS_TYPE == JVMHelper.OS.MACOSX) {
                            Path javaFile = finalJavaVersion.jvmDir.resolve("bin").resolve("java");
                            if(Files.exists(javaFile)) {
                                if(!javaFile.toFile().setExecutable(true)) {
                                    LogHelper.warning("Set permission for %s unsuccessful", javaFile.toString());
                                }
                            }
                        }
                        downloadClients(profile, finalJavaVersion, jvmHDir);

                    });
                    updateIsRun = false;
                } else {
                    downloadClients(profile, javaVersion, null);
                }
            });
        }), null);
    }
    public void sendUpdateRequest(String dirName, Path dir, FileNameMatcher matcher, boolean digest, OptionalView view, boolean optionalsEnabled, Consumer<HashedDir> onSuccess) {
        downloader.sendUpdateRequest(dirName, dir, matcher, digest, view, optionalsEnabled, onSuccess);
    }
    public void sendUpdateAssetRequest(String dirName, Path dir, FileNameMatcher matcher, boolean digest, String assetIndex, Consumer<HashedDir> onSuccess) {
        downloader.sendUpdateAssetRequest(dirName, dir, matcher, digest, assetIndex, onSuccess);
    }
}
