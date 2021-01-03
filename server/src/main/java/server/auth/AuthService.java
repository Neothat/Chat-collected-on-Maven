package server.auth;

public interface AuthService {

    default void start() {}
    default void stop() {}

    String getNickByLoginPass(String login, String password);

}
