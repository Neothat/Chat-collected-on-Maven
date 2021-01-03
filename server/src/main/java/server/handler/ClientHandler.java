package server.handler;

import NetworkChatClientServer.Command;
import NetworkChatClientServer.CommandType;
import NetworkChatClientServer.commands.AuthCommandData;
import NetworkChatClientServer.commands.PrivateMessageCommandData;
import NetworkChatClientServer.commands.PublicMessageCommandData;
import server.MyServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import static NetworkChatClientServer.Command.*;

public class ClientHandler {

    private final MyServer myServer;
    private final Socket clientSocket;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    private String nickname;

    public ClientHandler(MyServer myServer, Socket clientSocket) {
        this.myServer = myServer;
        this.clientSocket = clientSocket;
    }

    public void handle() throws IOException {
        in = new ObjectInputStream(clientSocket.getInputStream());
        out = new ObjectOutputStream(clientSocket.getOutputStream());

        new Thread(() -> {
        try {
            authentication();
            readMessage();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                closeConnection();
            } catch (IOException e){
                System.err.println("Ошибка при закрытии соединения");
            }
        }
        }).start();
    }

    private void authentication() throws IOException {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(nickname == null) {
                    try {
                        closeConnection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 120_000);

        while (true) {
            Command command = readCommand();
            if (command == null){
                continue;
            }

            if (command.getType() == CommandType.AUTH) {
                AuthCommandData data = (AuthCommandData) command.getData();
                String login = data.getLogin();
                String password = data.getPassword();
                String nickname = myServer.getAuthService().getNickByLoginPass(login, password);
                if (nickname == null) {
                    sendCommand(errorCommand("Некорректные логин или пароль"));
                    continue;
                }

                if(myServer.isNickBusy(nickname)) {
                    sendCommand(Command.errorCommand("Такой пользователь уже существует"));
                    continue;
                }

                sendCommand(authOkCommand(nickname));
                setNickname(nickname);
                myServer.broadcastMessage(String.format("Пользователь '%s' зашел в чат", nickname), null);
                myServer.subscribe(this);
                return;
            }
        }
    }

    public void sendCommand(Command command) throws IOException {
        out.writeObject(command);
    }

    private Command readCommand() throws IOException{
        Command command = null;
        try {
            command = (Command) in.readObject();
        }catch (ClassNotFoundException e){
            System.err.println("Ошибка при прочтении Command class");
        }
        return command;
    }

    private void setNickname(String nickname) {
        this.nickname = nickname;
    }

    private void readMessage() throws IOException {
        while (true){
            Command command = readCommand();
            if(command == null){
                continue;
            }

            switch (command.getType()) {
                case PRIVATE_MESSAGE: {
                    PrivateMessageCommandData data = (PrivateMessageCommandData) command.getData();
                    String receiver = data.getReceiver();
                    String message = data.getMessage();
                    myServer.sendPrivateMessage(this, receiver, message);
                    break;
                }
                case PUBLIC_MESSAGE: {
                    PublicMessageCommandData data= (PublicMessageCommandData) command.getData();
                    String message = data.getMessage();
                    myServer.broadcastMessage(message, this);
                    break;
                }
                case END:
                    return;
                default:
                    throw new IllegalArgumentException("Unknown command type: " + command.getType());
            }
        }
    }

    private void closeConnection() throws IOException {
        myServer.unsubscribe(this);
        clientSocket.close();
    }

    public void sendMessage(String message) throws IOException {
        sendCommand(Command.messageInfoCommand(message));
    }

    public void sendMessage(String sender, String message) throws IOException {
        sendCommand(Command.clientMessageCommand(sender, message));
    }

    public String getNickname() {
        return nickname;
    }
}
