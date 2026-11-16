package com.asims.auth;

/**
 * A login account.
 *
 * ENCAPSULATION: the password hash is private and there is no getter for the password —
 * the only way to use it is checkPassword().
 * ASSOCIATION: a pilot account points to its Pilot through staffId (admins have none).
 */
public class Account {

    private final String username;
    private final String fullName;
    private final Role role;
    private final String staffId;          // e.g. "P1" for pilots, null for admin / managers
    private final String passwordHash;

    public Account(String username, String fullName, Role role, String staffId, String passwordHash) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.staffId = staffId;
        this.passwordHash = passwordHash;
    }

    public boolean checkPassword(String password) {
        return PasswordHasher.matches(password, passwordHash);
    }

    public boolean can(Permission permission) {
        return role.has(permission);
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public String getStaffId() { return staffId; }

    /** Only the repository needs this, to save the account. */
    public String getPasswordHash() { return passwordHash; }
}
