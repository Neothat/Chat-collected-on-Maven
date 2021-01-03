package sample.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import sample.ClientChat;
import sample.Network;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

public class Controller {
    @FXML
    private TextField textField;
    @FXML
    public ListView<String> usersList;

    @FXML
    private TextArea chatField;

    @FXML
    private Button sendButton;

    private Network network;
    private String selectedUser;

    @FXML
    public void initialize(){
        usersList.setItems(FXCollections.observableArrayList(ClientChat.USERS));
        textField.requestFocus();

        usersList.setCellFactory(lv -> {
            MultipleSelectionModel <String> selectionModel = usersList.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                usersList.requestFocus();
                if (! cell.isEmpty()) {
                    int index = cell.getIndex();
                    if (selectionModel.getSelectedIndices().contains(index)) {
                        selectionModel.clearAndSelect(index);
                        selectedUser = null;
                    } else {
                        selectionModel.select(index);
                        selectedUser = cell.getItem();
                    }
                    event.consume();
                }
            });
            return cell;
        });
    }

    @FXML
    private void sendMessage() {
        String message = textField.getText();
        appendMessage("Я: " + message);
        textField.clear();

        try {
            if (selectedUser != null) {
                network.sendPrivateMessage(selectedUser, message);
            } else {
                network.sendMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
            String errorMessage = "Failed to send message";
            ClientChat.showNetworkError(e.getMessage(), errorMessage);
        }
    }
    public void setNetwork(Network network) {
        this.network = network;
    }

    public void appendMessage(String message) {
        chatField.appendText(message);
        chatField.appendText(System.lineSeparator());
    }

    public TextField getTextField() {
        return textField;
    }
}
