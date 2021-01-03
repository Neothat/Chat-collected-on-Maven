package sample;

import NetworkChatClientServer.Command;
import NetworkChatClientServer.commands.*;
import javafx.application.Platform;
import sample.controllers.Controller;

import java.io.*;
import java.net.Socket;

import static NetworkChatClientServer.Command.authCommand;
import static NetworkChatClientServer.Command.publicMessageCommand;

public class Network {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8189;

    private final String host;
    private final int port;
    private ClientChat clientChat;
    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String nickname;

    public Network() {
        this(SERVER_ADDRESS, SERVER_PORT);
    }

    public Network(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Network(ClientChat clientChat) {
        this();
        this.clientChat = clientChat;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            return true;
        } catch (IOException e) {
            System.err.println("Соединение не было установленно!");
            e.printStackTrace();
            return false;
        }
    }

    public void sendPrivateMessage(String receiver, String message) throws IOException {
        sendCommand(Command.privateMessageCommand(receiver, message));
    }

    public void sendMessage(String message) throws IOException {
        sendCommand(publicMessageCommand(nickname, message));
    }

    private void sendCommand(Command command) throws IOException {
        outputStream.writeObject(command);
    }

    public void waitMessages(Controller controller) {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    Command command = readCommand();
                    if (command == null) {
                        continue;
                    }
                    if (clientChat.getState() == ClientChatState.AUTHENTICATION) {
                        processAuthResult(command);
                    } else {
                        processMessage(controller, command);
                    }
                }
            } catch (IOException | IllegalAccessException e) {
                System.err.println("Соединение было разорвано!");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void processMessage(Controller controller, Command command) throws IllegalAccessException {
        switch (command.getType()) {
            case INFO_MESSAGE: {
                MessageInfoCommandData data = (MessageInfoCommandData) command.getData();
                Platform.runLater(() -> {
                    controller.appendMessage(data.getMessage());
                });
                break;
            }
            case CLIENT_MESSAGE: {
                ClientMessageCommandData data = (ClientMessageCommandData) command.getData();
                String sender = data.getSender();
                String message = data.getMessage();
                Platform.runLater(() -> {
                    controller.appendMessage(String.format("%s %s", sender, message));
                });
                break;
            }
            case ERROR: {
                ErrorCommandData data = (ErrorCommandData) command.getData();
                Platform.runLater(() -> {
                    ClientChat.showNetworkError(data.getErrorMessage(), "Server error");
                });
                break;
            }
            case UPDATE_USERS_LIST: {
                UpdateUsersListCommandData data = (UpdateUsersListCommandData) command.getData();
                Platform.runLater(() -> {
                    clientChat.updateUsers(data.getUsers());
                });
                break;
            }
            default:
                throw new IllegalAccessException("Unknown command type: " + command.getType());
        }
    }

    private void processAuthResult(Command command) throws IllegalAccessException {
        switch (command.getType()) {
            case AUTH_OK: {
                AuthOkCommandData data = (AuthOkCommandData) command.getData();
                nickname = data.getUsername();
                Platform.runLater(() -> {
                    clientChat.activeChatDialog(nickname);
                });
                Platform.runLater(() -> {
                    clientChat.activeChatDialog(nickname);
                });
                break;
            }
            case ERROR: {
                ErrorCommandData data = (ErrorCommandData) command.getData();
                Platform.runLater(() -> {
                    ClientChat.showNetworkError(data.getErrorMessage(), "Auth error");
                });
                break;
            }
            default:
                throw new IllegalAccessException("Unknown command type: " + command.getType());
        }
    }

    private Command readCommand() throws IOException {
        Command command = null;
        try {
            command = (Command) inputStream.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка при прочтении Command class");
        }
        return command;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAuthMessage(String login, String password) throws IOException {
        sendCommand(authCommand(login, password));
    }
}
