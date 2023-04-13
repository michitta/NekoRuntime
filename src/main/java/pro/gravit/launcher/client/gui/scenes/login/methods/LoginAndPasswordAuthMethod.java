package pro.gravit.launcher.client.gui.scenes.login.methods;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.overlays.AbstractOverlay;
import pro.gravit.launcher.client.gui.scenes.login.LoginAuthButtonComponent;
import pro.gravit.launcher.client.gui.scenes.login.LoginScene;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.details.AuthPasswordDetails;

import java.util.concurrent.CompletableFuture;

public class LoginAndPasswordAuthMethod extends AbstractAuthMethod<AuthPasswordDetails> {
    private final LoginAndPasswordOverlay overlay;
    private final JavaFXApplication application;
    private final LoginScene.LoginSceneAccessor accessor;

    public LoginAndPasswordAuthMethod(LoginScene.LoginSceneAccessor accessor) {
        this.accessor = accessor;
        this.application = accessor.getApplication();
        this.overlay = application.gui.registerOverlay(LoginAndPasswordOverlay.class);
        this.overlay.accessor = accessor;
    }

    @Override
    public void prepare() {
    }

    @Override
    public void reset() {
        overlay.reset();
    }

    @Override
    public CompletableFuture<Void> show(AuthPasswordDetails details) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            accessor.showOverlay(overlay, (e) -> future.complete(null));
        } catch (Exception e) {
            accessor.errorHandle(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<LoginScene.LoginAndPasswordResult> auth(AuthPasswordDetails details) {
        overlay.future = new CompletableFuture<>();
        String login = overlay.login.getText();
        AuthRequest.AuthPasswordInterface password;
        if (overlay.password.getText().isEmpty() && overlay.password.getPromptText().equals(
                application.getTranslation("runtime.scenes.login.password.saved"))) {
            password = application.runtimeSettings.password;
            return CompletableFuture.completedFuture(new LoginScene.LoginAndPasswordResult(login, password));
        }
        return overlay.future;
    }

    @Override
    public CompletableFuture<Void> hide() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        accessor.hideOverlay(0, (e) -> future.complete(null));
        return future;
    }

    @Override
    public boolean isSavable() {
        return true;
    }

    public static class LoginAndPasswordOverlay extends AbstractOverlay {
        private static final UserAuthCanceledException USER_AUTH_CANCELED_EXCEPTION = new UserAuthCanceledException();
        private TextField login;
        private TextField password;
        private LoginAuthButtonComponent authButton;
        private LoginScene.LoginSceneAccessor accessor;
        private CompletableFuture<LoginScene.LoginAndPasswordResult> future;

        public LoginAndPasswordOverlay(JavaFXApplication application) {
            super("scenes/login/loginpassword.fxml", application);
        }

        @Override
        public String getName() {
            return "loginandpassword";
        }

        @Override
        protected void doInit() {
            login = LookupHelper.lookup(layout, "#login");
            password = LookupHelper.lookup(layout, "#password");
            authButton = new LoginAuthButtonComponent(LookupHelper.lookup(layout, "#authButtonBlock"), application, e -> {
                String rawLogin = login.getText();
                String rawPassword = password.getText();
                future.complete(new LoginScene.LoginAndPasswordResult(rawLogin, accessor.getAuthService().makePassword(rawPassword)));
            });
            
            if (application.guiModuleConfig.createAccountURL != null)
                LookupHelper.<Label>lookup(layout, "#createAccount").setOnMouseClicked((e) ->
                    application.openURL(application.guiModuleConfig.createAccountURL));
            
            if (application.runtimeSettings.login != null) {
                login.setText(application.runtimeSettings.login);
            }
            if (application.runtimeSettings.password != null) {
                password.getStyleClass().add("hasSaved");
                password.setPromptText("Сохранён");
            }

        }


        @Override
        public void reset() {
            if (password == null) return;
            password.getStyleClass().removeAll("hasSaved");
            password.setText("");
            login.setText("");
        }
    }
}
