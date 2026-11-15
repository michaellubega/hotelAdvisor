package models;

public class User {

    public enum Role { STAFF, CUSTOMER }

    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String location;
    private Role role;
    private int loyaltyPoints;

    public User(String username, String password,
                String firstName, String lastName,
                String email, String phone, String location,
                Role role, int loyaltyPoints) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.location = location;
        this.role = role;
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getLocation() { return location; }
    public Role getRole() { return role; }
    public int getLoyaltyPoints() { return loyaltyPoints; }

    public boolean isStaff() { return role == Role.STAFF; }
    public boolean isCustomer() { return role == Role.CUSTOMER; }
}