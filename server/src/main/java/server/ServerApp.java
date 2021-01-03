package server;

public class ServerApp {
    public static final int DEFAULT_PORT = 8190;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        }
        new MyServer().start(port);
    }
}
