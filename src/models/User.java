package models;

public class User {
    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String course;
    private String yearLevel;
    private String contactNumber;
    private String email;
    private String role;
    private String dateRegistered;

    public User() {}

    public User(int userId, String username, String password, String fullName, String course, 
                String yearLevel, String contactNumber, String email, String role, String dateRegistered) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.course = course;
        this.yearLevel = yearLevel;
        this.contactNumber = contactNumber;
        this.email = email;
        this.role = role;
        this.dateRegistered = dateRegistered;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getYearLevel() { return yearLevel; }
    public void setYearLevel(String yearLevel) { this.yearLevel = yearLevel; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDateRegistered() { return dateRegistered; }
    public void setDateRegistered(String dateRegistered) { this.dateRegistered = dateRegistered; }
}