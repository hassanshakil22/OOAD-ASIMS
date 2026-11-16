package com.asims.auth;

import com.asims.repository.AccountRepository;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Logs users in and remembers who is logged in.
 *
 * login()  checks the username + password against the database and, if correct,
 *          creates a random session token. The browser keeps the token in a cookie
 *          and sends it with every request, so the server knows who is calling.
 */
public class AuthService {

    private final AccountRepository accounts;
    private final Map<String, Account> sessions = new HashMap<>();   // token -> logged-in account
    private final SecureRandom random = new SecureRandom();

    public AuthService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    /** Returns a new session token, or throws if the credentials are wrong. */
    public synchronized String login(String username, String password) {
        Account account = accounts.findByUsername(username == null ? "" : username.trim());
        // the same message for "no such user" and "wrong password", so it reveals nothing
        if (account == null || !account.checkPassword(password)) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder token = new StringBuilder();
        for (byte b : bytes) token.append(String.format("%02x", b));
        sessions.put(token.toString(), account);
        return token.toString();
    }

    /** The account that owns this token, or null if nobody is logged in with it. */
    public synchronized Account getAccount(String token) {
        return token == null ? null : sessions.get(token);
    }

    public synchronized void logout(String token) {
        if (token != null) sessions.remove(token);
    }
}
