package pro.gravit.launcher.client.gui.scenes.update;

import javafx.beans.property.DoubleProperty;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import pro.gravit.launcher.AsyncDownloader;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.impl.ContextHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.events.request.UpdateRequestEvent;
import pro.gravit.launcher.hasher.FileNameMatcher;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.hasher.HashedEntry;
import pro.gravit.launcher.hasher.HashedFile;
import pro.gravit.launcher.profiles.optional.OptionalView;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.profiles.optional.actions.OptionalActionFile;
import pro.gravit.launcher.request.update.UpdateRequest;
import pro.gravit.utils.Downloader;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class UpdateScene extends AbstractScene {
    private Label postClient;
    private Text speed;
    private Label volume;
    private TextArea logOutput;
    private Button cancel;
    private Text speedtext;
    private Text speederr;

    private VisualDownloader downloader;

    public UpdateScene(JavaFXApplication application) {
        super("scenes/update/update.fxml", application);
    }

    @Override
    protected void doInit() {
        speed = LookupHelper.lookup(layout, "#speed");
        speederr = LookupHelper.lookup(layout, "#speedErr");
        speedtext = LookupHelper.lookup(layout, "#speed-text");
        cancel = LookupHelper.lookup(layout, "#cancel");
        volume = LookupHelper.lookup(layout, "#volume");
        logOutput = LookupHelper.lookup(layout, "#outputUpdate");
        logOutput.setText("");
        LookupHelper.<ButtonBase>lookup(layout, "#reload").setOnAction(
                (e) -> reset()
        );
        LookupHelper.<ButtonBase>lookup(layout, "#cancel").setOnAction(
                (e) -> {
                    if (downloader.isDownload()) {
                        downloader.cancel();
                    } else {
                        try {
                            switchScene(application.gui.serverInfoScene);
                        } catch (Exception exception) {
                            errorHandle(exception);
                        }
                    }
                });
    }
    public void addLog(String string) {
        LogHelper.dev("Update event %s", string);
        logOutput.appendText(string.concat("\n"));
    }

    @Override
    public void reset() {
        logOutput.clear();
        volume.setText("");
        speed.setText("0");
        //reload.setDisable(true);
        //reload.setStyle("-fx-opacity: 0");
        cancel.setDisable(false);
        cancel.setStyle("-fx-opacity: 1");
        speed.getStyleClass().removeAll("speedError");
        speed.setStyle("-fx-opacity: 1");
        speedtext.setStyle("-fx-opacity: 1");
        speederr.setStyle("-fx-opacity: 0");
    }

    @Override
    public void errorHandle(Throwable e) {
        addLog(String.format("Exception %s: %s", e.getClass(), e.getMessage() == null ? "" : e.getMessage()));
        speed.setStyle("-fx-opacity: 0");
        speedtext.setStyle("-fx-opacity: 0");
        speederr.setStyle("-fx-opacity: 1");
        LogHelper.error(e);
        //reload.setDisable(false);
        //reload.setStyle("-fx-opacity: 1");
        cancel.setDisable(true);
        cancel.setStyle("-fx-opacity: 0");
    }

    @Override
    public String getName() {
        return "update";
    }
}
