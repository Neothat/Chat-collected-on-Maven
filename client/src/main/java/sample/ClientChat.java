package sample;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sample.controllers.AuthDialog;
import sample.controllers.Controller;

import java.io.IOException;
import java.util.List;

public class ClientChat extends Application {

    public static final List<String> USERS = List.of("Oleg", "Erlan", "Denis");

    private ClientChatState state = ClientChatState.AUTHENTICATION;
    private Stage primaryStage;
    private Stage authDialogStage;
    private Network network;
    private Controller controller;

    public void updateUsers(List<String> users) {
        controller.usersList.setItems(FXCollections.observableList(users));
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ClientChat.class.getResource("/sample.fxml"));

        AnchorPane root = loader.load();
        controller = loader.getController();

        primaryStage.setTitle("Messenger");
        primaryStage.setScene(new Scene(root));
        controller.getTextField().requestFocus();

        network = new Network(this);
        if (!network.connect()) {
            showNetworkError(" ", "Failed to connect to server");
        }
        Controller controller = loader.getController();
        controller.setNetwork(network);

        network.waitMessages(controller);

        primaryStage.setOnCloseRequest(event ->
        {
            try {
                network.sendMessage("/end");
            } catch (IOException e) {
                e.printStackTrace();
            }

            network.close();
        });
        openAuthDialog();
    }

    private void openAuthDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ClientChat.class.getResource("/authDialog.fxml"));
        AnchorPane parent = loader.load();

        authDialogStage = new Stage();
        authDialogStage.initModality(Modality.WINDOW_MODAL);
        authDialogStage.initOwner(primaryStage);

        AuthDialog authDialog = loader.getController();
        authDialog.setNetwork(network);

        authDialogStage.setScene(new Scene(parent));
        authDialogStage.show();

    }

    public static void showNetworkError(String errorDetails, String errorTitle) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Network Error");
        alert.setHeaderText(errorTitle);
        alert.setContentText(errorDetails);
        alert.showAndWait();
    }


    public static void main(String[] args) {
        launch(args);
    }

    public ClientChatState getState() {
        return state;
    }

    public void setState(ClientChatState state) {
        this.state = state;
    }

    public void activeChatDialog(String nickname) {
        primaryStage.setTitle(nickname);
        state = ClientChatState.CHAT;
        authDialogStage.close();
        primaryStage.show();
        controller.getTextField().requestFocus();
    }
}
