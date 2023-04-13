package pro.gravit.launcher.client.gui.scenes.debug;

import animatefx.animation.FadeIn;
import animatefx.animation.SlideInLeft;
import animatefx.animation.SlideInUp;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.impl.ContextHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.scenes.console.ConsoleScene;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.*;

public class DebugScene extends AbstractScene {
    private static final long MAX_LENGTH = 163840;
    private static final int REMOVE_LENGTH = 1024;
    public Process currentProcess;
    public Thread writeParametersThread;
    private Thread readThread;
    private TextArea output;

    public DebugScene(JavaFXApplication application) {
        super("scenes/debug/debug.fxml", application);
        this.isResetOnShow = true;
    }

    @Override
    protected void doInit() {
        output = LookupHelper.lookup(layout, "#output");
        LookupHelper.<ButtonBase>lookupIfPossible(header, "#kill").ifPresent((x) -> x.setOnAction((e) -> {
            if (currentProcess != null && currentProcess.isAlive())
                currentProcess.destroyForcibly();
        }));

        LookupHelper.<Label>lookupIfPossible(layout, "#version").ifPresent((v) -> v.setText(ConsoleScene.getMiniLauncherInfo()));
        LookupHelper.<ButtonBase>lookup(header, "#back").setOnAction((e) -> {
            if (writeParametersThread != null && writeParametersThread.isAlive()) {
                writeParametersThread.interrupt();
            }
            if (currentProcess != null && currentProcess.isAlive()) {
                Process process = currentProcess;
                currentProcess = null;
                readThread.interrupt();
                writeParametersThread = null;
                readThread = null;
                try {
                    process.getErrorStream().close();
                    process.getInputStream().close();
                    process.getOutputStream().close();
                } catch (IOException ex) {
                    errorHandle(ex);
                }
            }
            try {
                application.gui.serverInfoScene.reset();
                switchScene(application.gui.serverInfoScene);
                application.gui.serverInfoScene.reset();
            } catch (Exception ex) {
                errorHandle(ex);
            }
        });
    }


    @Override
    public void reset() {
        new SlideInUp(LookupHelper.lookup(layout, "#content")).play();
        new FadeIn(LookupHelper.lookup(layout, "#leftPane")).play();
        ClientProfile profile = application.stateService.getProfile();
        LookupHelper.<Label>lookup(layout, "#serverName").setText(profile.getTitle());
        LookupHelper.<Label>lookupIfPossible(layout, "#nickname").ifPresent((e) -> e.setText(application.stateService.getUsername()));
        output.clear();
    }

    public void onProcess(Process process) {
        if (readThread != null && readThread.isAlive())
            readThread.interrupt();
        if (currentProcess != null && currentProcess.isAlive())
            currentProcess.destroyForcibly();
        readThread = CommonHelper.newThread("Client Process Console Reader", true, () -> {
            InputStream stream = process.getInputStream();
            byte[] buf = IOHelper.newBuffer();
            try {
                for (int length = stream.read(buf); length >= 0; length = stream.read(buf)) {
                    append(new String(buf, 0, length));
                }
                if (currentProcess.isAlive()) currentProcess.waitFor();
                onProcessExit(currentProcess.exitValue());
            } catch (IOException e) {
                errorHandle(e);
            } catch (InterruptedException ignored) {

            }
        });
        readThread.start();
        currentProcess = process;
    }

    public void append(String text) {
        ContextHelper.runInFxThreadStatic(() -> {
            if (output.lengthProperty().get() > MAX_LENGTH)
                output.deleteText(0, REMOVE_LENGTH);
            output.appendText(text);
        });
    }

    @Override
    public void errorHandle(Throwable e) {
        if (!(e instanceof EOFException)) {
            if (LogHelper.isDebugEnabled())
                append(e.toString());
        }
        if (currentProcess != null && !currentProcess.isAlive()) {
            onProcessExit(currentProcess.exitValue());
        }
    }

    @Override
    public String getName() {
        return "debug";
    }

    private void onProcessExit(int code) {
        append(String.format("Process exit code %d", code));
        if (writeParametersThread != null) writeParametersThread.interrupt();
    }
}
