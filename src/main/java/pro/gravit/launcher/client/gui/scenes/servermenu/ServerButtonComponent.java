package pro.gravit.launcher.client.gui.scenes.servermenu;

import javafx.animation.*;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.impl.AbstractVisualComponent;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.helper.LogHelper;

import java.net.URL;

public class ServerButtonComponent extends AbstractVisualComponent {
    private static final String SERVER_BUTTON_FXML = "components/serverButton.fxml";
    private static final String SERVER_BUTTON_CUSTOM_FXML = "components/serverButton/%s.fxml";
    public ClientProfile profile;
    private Pane serverButtonLayout;

    protected ServerButtonComponent(JavaFXApplication application, ClientProfile profile) {
        super(getFXMLPath(application, profile), application);
        this.profile = profile;
    }

    private static String getFXMLPath(JavaFXApplication application, ClientProfile profile) {
        String customFxmlName = String.format(SERVER_BUTTON_CUSTOM_FXML, profile.getUUID());
        URL customFxml = application.tryResource(customFxmlName);
        if (customFxml != null) {
            return customFxmlName;
        }
        return SERVER_BUTTON_FXML;
    }

    @Override
    public String getName() {
        return "serverButton";
    }

    @Override
    protected void doInit() throws Exception {
        serverButtonLayout = LookupHelper.lookup(layout, "#serverButtonLayout");
        application.pingService.getPingReport(profile.getDefaultServerProfile().name).thenAccept((report) -> {
            if(report == null) {
                LookupHelper.<Label>lookup(layout,"#online").setText("Версия: " + profile.getAssetIndex() + " • Онлайн: недоступен");
            } else {
                LookupHelper.<Label>lookup(layout, "#online").setText("Версия: " + profile.getAssetIndex() + " • Онлайн: " + report.playersOnline + " из " + report.maxPlayers);
            }
        });
        Pane buttonContent = LookupHelper.lookup(layout, "#goServerHover");
        LookupHelper.<Labeled>lookup(layout, "#nameServer").setText(profile.getTitle());
        LookupHelper.<ImageView>lookupIfPossible(layout, "#serverLogo").ifPresent((a) -> {
            try {
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(a.getFitWidth(), a.getFitHeight());
                clip.setArcWidth(20.0);
                clip.setArcHeight(20.0);
                a.setClip(clip);
            } catch (Throwable e) {
                LogHelper.error(e);
            }
        });
        GaussianBlur gaussianBlur = new GaussianBlur();
        buttonContent.setOnMouseEntered((event) -> {
            Timeline timeline = new Timeline(
                    new KeyFrame(
                            Duration.ZERO,
                            new KeyValue(gaussianBlur.radiusProperty(), 0)
                    ),
                    new KeyFrame(
                            Duration.seconds(0.3),
                            new KeyValue(gaussianBlur.radiusProperty(), 120)
                    )
            );
            timeline.setCycleCount(1);
            serverButtonLayout.setEffect(gaussianBlur);
            timeline.play();
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.2), layout);
            transition.setToX(1.02);
            transition.setToY(1.02);
            transition.play();
            Rectangle rect = new Rectangle(200,158);
            rect.setArcHeight(14);
            rect.setArcWidth(14);
            serverButtonLayout.setClip(rect);
        });
        buttonContent.setOnMouseExited((event) -> {
            Timeline timeline = new Timeline(
                    new KeyFrame(
                            Duration.ZERO,
                            new KeyValue(gaussianBlur.radiusProperty(), 120)
                    ),
                    new KeyFrame(
                            Duration.seconds(0.3),
                            new KeyValue(gaussianBlur.radiusProperty(), 0)
                    )
            );
            timeline.setCycleCount(1);
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.2), layout);
            transition.setToX(1);
            transition.setToY(1);
            transition.play();
            serverButtonLayout.setEffect(gaussianBlur);
            timeline.play();
        });
    }

    public void setOnMouseClicked(EventHandler<? super MouseEvent> eventHandler) {
        layout.setOnMouseClicked(eventHandler);
        serverButtonLayout.setEffect(new GaussianBlur(0));
    }

    public void addTo(Pane pane, int position) {
        if (!isInit()) {
            try {
                init();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        }
        pane.getChildren().add(position, layout);
    }

    @Override
    public void reset() {

    }

    @Override
    public void disable() {

    }

    @Override
    public void enable() {

    }
}
