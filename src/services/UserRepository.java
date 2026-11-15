package services;

import models.User;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private static final List<User> users = new ArrayList<>();

    static {
        // STAFF
        users.add(new User("admin", "1234",
                "System", "Admin",
                "admin@starviewhotel.ug", "+256 700 111 111",
                "Head Office",
                User.Role.STAFF, 0));

        // CUSTOMERS
        users.add(new User("john", "pass123",
                "John", "Doe",
                "john@example.com", "+256 700 222 222",
                "Kampala, Uganda",
                User.Role.CUSTOMER, 4500));

        users.add(new User("mary", "mary2024",
                "Mary", "Smith",
                "mary@example.com", "+256 700 333 333",
                "Entebbe, Uganda",
                User.Role.CUSTOMER, 1200));
    }

    public static User authenticate(String identifier, String password, User.Role expectedRole) {
        for (User u : users) {
            boolean matches = u.getUsername().equalsIgnoreCase(identifier)
                           || u.getEmail().equalsIgnoreCase(identifier);
            if (matches && u.getPassword().equals(password) && u.getRole() == expectedRole) {
                return u;
            }
        }
        return null;
    }

    public static boolean addUser(User user) {
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(user.getEmail())
                || u.getUsername().equalsIgnoreCase(user.getUsername())) {
                return false;
            }
        }
        users.add(user);
        return true;
    }

    public static List<User> getAllUsers() {
        return new ArrayList<>(users);
    }
}