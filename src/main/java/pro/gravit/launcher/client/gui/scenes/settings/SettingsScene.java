package pro.gravit.launcher.client.gui.scenes.settings;

import animatefx.animation.FadeIn;
import animatefx.animation.SlideInLeft;
import animatefx.animation.SlideInUp;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import oshi.SystemInfo;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerMenuScene;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.helper.LogHelper;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.function.Consumer;

public class SettingsScene extends AbstractScene {
    private Pane componentList;
    private ImageView avatar;
    private Image originalAvatarImage;
    private Label ramLabel;
    private Slider ramSlider;
    private HBox hbox;
    private RuntimeSettings.ProfileSettingsView profileSettings;

    public SettingsScene(JavaFXApplication application) {
        super("scenes/settings/settings.fxml", application);
    }

    @Override
    protected void doInit() {
        ramSlider = LookupHelper.lookup(layout, "#ramSlider");
        ramLabel = LookupHelper.lookup(layout, "#ramLabel");
        try {
            SystemInfo systemInfo = new SystemInfo();
            ramSlider.setMax(systemInfo.getHardware().getMemory().getTotal() >> 20);
        } catch (Throwable e) {
            ramSlider.setMax(2048);
        }
        ramSlider.setSnapToTicks(true);
        ramSlider.setShowTickMarks(true);
        ramSlider.setShowTickLabels(true);
        ramSlider.setMinorTickCount(1);
        ramSlider.setMajorTickUnit(1024);
        ramSlider.setBlockIncrement(1024);
        ramSlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                return String.format("%.0fG", object / 1024);
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });
        hbox = (HBox) LookupHelper.<ScrollPane>lookup(layout, "#settingslist").getContent();
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
        componentList = (Pane) LookupHelper.<ScrollPane>lookup(layout, "#settingslist").getContent();
        reset();
    }

    @Override
    public void reset() {
        profileSettings = new RuntimeSettings.ProfileSettingsView(application.getProfileSettings());
        ramSlider.setValue(profileSettings.ram);
        ramSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            profileSettings.ram = newValue.intValue();
            updateRamLabel();
        });
        updateRamLabel();
        new SlideInUp(LookupHelper.lookup(layout, "#content")).play();
        new FadeIn(LookupHelper.lookup(layout, "#leftPane")).play();
        avatar.setImage(originalAvatarImage);
        LookupHelper.<Label>lookupIfPossible(layout, "#nickname").ifPresent((e) -> e.setText(application.stateService.getUsername()));
        ServerMenuScene.putAvatarToImageView(application, application.stateService.getUsername(), avatar);
        ClientProfile profile = application.stateService.getProfile();
        LookupHelper.<Label>lookup(layout, "#serverName").setText(profile.getTitle());
        LookupHelper.<ButtonBase>lookupIfPossible(header, "#back").ifPresent(a -> a.setOnAction((e) -> {
            try {
                profileSettings.apply();
                profileSettings = null;
                application.triggerManager.process(profile, application.stateService.getOptionalView());
                switchScene(application.gui.serverInfoScene);
                application.gui.serverInfoScene.reset();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));
        LookupHelper.<Button>lookupIfPossible(layout, "#clientSettings").ifPresent(x -> x.setOnAction((e) -> {
            try {
                switchScene(application.gui.optionsScene);
                profileSettings.apply();
                application.triggerManager.process(profile, application.stateService.getOptionalView());
                application.gui.optionsScene.reset();
                application.gui.optionsScene.addProfileOptionals(application.stateService.getOptionalView());
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));
        LookupHelper.<Label>lookup(layout, "#serverName").setText(profile.getTitle());
        Path clientfolder = DirBridge.dirUpdates.resolve(profile.getDir());
        Path assetfolder = DirBridge.dirUpdates.resolve(profile.getDir());
        LookupHelper.<Button>lookupIfPossible(layout, "#fixClient").ifPresent(x -> x.setOnAction((e) -> {
            try {
                deleteDir(clientfolder.toFile());
                deleteDir(assetfolder.toFile());
                application.messageManager.createNotification("Уведомление", "Клиент удалён!");
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));
        LookupHelper.<Button>lookupIfPossible(layout, "#openGameFolder").ifPresent(x -> x.setOnAction((e) -> {
            try {
                if (clientfolder.toFile().exists()){
                    Desktop.getDesktop().open(clientfolder.toFile());
                } else {
                    Desktop.getDesktop().open(DirBridge.dirUpdates.toFile());
                }
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));
        componentList.getChildren().clear();
        //add("Debug", profileSettings.debug, (value) -> profileSettings.debug = value);
        add("AutoEnter", profileSettings.autoEnter, (value) -> profileSettings.autoEnter = value);
        add("Fullscreen", profileSettings.fullScreen, (value) -> profileSettings.fullScreen = value);
    }

    @Override
    public String getName() {
        return "settings";
    }

    public void add(String languageName, boolean value, Consumer<Boolean> onChanged) {
        String nameKey = String.format("runtime.scenes.settings.properties.%s.name", languageName.toLowerCase());
        String descriptionKey = String.format("runtime.scenes.settings.properties.%s.description", languageName.toLowerCase());
        add(application.getTranslation(nameKey, languageName), application.getTranslation(descriptionKey, languageName), value, onChanged);
    }

    public void add(String name, String description, boolean value, Consumer<Boolean> onChanged) {
        hbox.setSpacing(20);
        Pane mainPane = new Pane();
        CheckBox checkBox = new CheckBox();
        Pane pane = new Pane();
        pane.setMinWidth(200);
        pane.setMaxWidth(200);
        pane.setMinHeight(88);
        pane.setMaxHeight(88);
        Label label = new Label();
        Label maintext = new Label();
        mainPane.getChildren().add(checkBox);
        pane.getChildren().add(label);
        pane.getChildren().add(maintext);
        checkBox.setGraphic(pane);
        mainPane.setMaxWidth(200);
        mainPane.setMaxHeight(88);
        mainPane.setOpacity(0.5);
        mainPane.getStyleClass().add("settings-container");
        checkBox.setSelected(value);
        checkBox.setMinWidth(200);
        checkBox.setMaxWidth(200);
        checkBox.setMinHeight(88);
        checkBox.setMaxHeight(88);
        checkBox.setCursor(Cursor.HAND);
        checkBox.setOnAction((e) -> {
            onChanged.accept(checkBox.isSelected());
        });
        checkBox.getStyleClass().add("settings-checkbox");
        maintext.setText(name);
        maintext.setWrapText(true);
        maintext.setPrefWidth(180);
        maintext.getStyleClass().add("maintext");
        label.setPrefWidth(180);
        label.setText(description);
        label.setWrapText(true);
        label.getStyleClass().add("descriptiontext");
        componentList.getChildren().add(mainPane);
    }
    void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }
    public void updateRamLabel() {
        ramLabel.setText(profileSettings.ram == 0 ? application.getTranslation("runtime.scenes.settings.ramAuto") : MessageFormat.format(application.getTranslation("runtime.scenes.settings.ram"), profileSettings.ram));
    }
}
