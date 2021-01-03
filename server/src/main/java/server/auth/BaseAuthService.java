package server.auth;

import server.User;

import java.util.Map;

public class BaseAuthService implements AuthService{

    private static final Map<User, String> USERS = Map.of(
            new User("login1", "pass1", "Oleg"), "Oleg",
            new User("login2", "pass2", "Erlan"), "Erlan",
            new User("login3", "pass3", "Denis"), "Denis"
    );

    @Override
    public void start() {
        System.out.println("Auth service is running");
    }

    @Override
    public void stop() {
        System.out.println("Auth service has been stopped");
    }

    @Override
    public String getNickByLoginPass(String login, String password) {
        User requestedUser = new User(login, password, null);
        return USERS.get(requestedUser);
    }
}
