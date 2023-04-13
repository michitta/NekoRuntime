package pro.gravit.launcher.client.gui.scenes.login;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;

public class LoginAuthButtonComponent {
    private final JavaFXApplication application;
    private final Pane layout;
    private final Pane authActive;
    private final Button button;

    public LoginAuthButtonComponent(Pane authButton, JavaFXApplication application, EventHandler<ActionEvent> eventHandler) {
        this.application = application;
        this.layout = authButton;
        this.authActive = LookupHelper.lookup(layout, "#authActive");
        this.button = LookupHelper.lookup(authActive, "#authButton");
        this.button.setOnAction(eventHandler);
    }

    public Pane getLayout() {
        return layout;
    }

    public String getText() {
        return button.getText();
    }

    public void setText(String text) {
        button.setText(text);
    }
}
