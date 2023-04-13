package pro.gravit.launcher.client.gui.overlays;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.impl.ContextHelper;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.function.Consumer;

public class ProcessingOverlay extends AbstractOverlay {
    private RotateTransition rt;

    public ProcessingOverlay(JavaFXApplication application) {
        super("overlay/processing/processing.fxml", application);
    }

    @Override
    public String getName() {
        return "processing";
    }

    @Override
    protected void doInit() {
        SVGPath loading = LookupHelper.lookup(layout, "#loading");
        rt = new RotateTransition(Duration.millis(1000), loading);
        rt.setByAngle(360);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.setCycleCount(RotateTransition.INDEFINITE);
        rt.play();
    }

    @Override
    public void reset() {
    }

    public void errorHandle(Throwable e) {
        super.errorHandle(e);
    }

    public final <T extends WebSocketEvent> void processRequest(AbstractScene scene, String message, Request<T> request, Consumer<T> onSuccess, EventHandler<ActionEvent> onError) {
        processRequest(scene, message, request, onSuccess, null, onError);
    }

    public final <T extends WebSocketEvent> void processRequest(AbstractScene scene, String message, Request<T> request, Consumer<T> onSuccess, Consumer<Throwable> onException, EventHandler<ActionEvent> onError) {
        try {
            scene.showOverlay(this, (e) -> {
                try {
                    application.service.request(request).thenAccept((result) -> {
                        LogHelper.dev("RequestFuture complete normally");
                        rt.stop();
                        onSuccess.accept(result);
                    }).exceptionally((error) -> {
                        if (onException != null) onException.accept(error);
                        ContextHelper.runInFxThreadStatic(() -> errorHandle(error.getCause()));
                        hide(2500, scene, onError);
                        return null;
                    });
                } catch (IOException ex) {
                    errorHandle(ex);
                    rt.stop();
                    hide(2500, scene, onError);
                }
            });
        } catch (Exception e) {
            errorHandle(e);
        }
    }
}
