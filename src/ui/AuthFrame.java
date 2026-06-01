package ui;

import dao.UserDAO;
import models.User;
import javax.swing.*;
import java.awt.*;

public class AuthFrame extends JFrame {
    private JTextField txtUser, txtFullName, txtCourse, txtContact, txtEmail;
    private JPasswordField txtPass;
    private JComboBox<String> cbYearLevel, cbRole;
    private JButton btnLogin, btnRegister;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private UserDAO userDAO = new UserDAO();

    public AuthFrame() {
        setTitle("Library Access Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 580); 
        setLocationRelativeTo(null);
        setResizable(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(buildLoginPanel(), "LOGIN");
        mainPanel.add(buildRegisterPanel(), "REGISTER");

        add(mainPanel);
        setVisible(true);
    }

    private JPanel buildLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(245, 246, 248));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Welcome Back", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        lblTitle.setForeground(Color.DARK_GRAY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        p.add(lblTitle, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0; p.add(new JLabel("Username:"), gbc);
        txtUser = new JTextField(15);
        gbc.gridx = 1; p.add(txtUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2; p.add(new JLabel("Password:"), gbc);
        txtPass = new JPasswordField(15);
        gbc.gridx = 1; p.add(txtPass, gbc);

        btnLogin = new JButton("Sign In");
        styleButton(btnLogin);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        p.add(btnLogin, gbc);

        JButton btnSwitch = new JButton("Create an account");
        btnSwitch.setBorderPainted(false);
        btnSwitch.setContentAreaFilled(false);
        btnSwitch.setForeground(Color.DARK_GRAY);
        gbc.gridy = 4;
        p.add(btnSwitch, gbc);

        btnLogin.addActionListener(e -> {
            User user = userDAO.loginUser(txtUser.getText(), new String(txtPass.getPassword()));
            if (user != null) {
                this.dispose();
                new DashboardFrame(user);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.", "Auth Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSwitch.addActionListener(e -> cardLayout.show(mainPanel, "REGISTER"));
        return p;
    }

    private JPanel buildRegisterPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(245, 246, 248));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Register Account", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitle.setForeground(Color.DARK_GRAY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        p.add(lblTitle, gbc);

        gbc.gridwidth = 1;
        
        gbc.gridx = 0; gbc.gridy = 1; p.add(new JLabel("Username:"), gbc);
        JTextField txtRegUser = new JTextField(15);
        gbc.gridx = 1; p.add(txtRegUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2; p.add(new JLabel("Password:"), gbc);
        JPasswordField txtRegPass = new JPasswordField(15);
        gbc.gridx = 1; p.add(txtRegPass, gbc);

        gbc.gridx = 0; gbc.gridy = 3; p.add(new JLabel("Full Name:"), gbc);
        txtFullName = new JTextField(15);
        gbc.gridx = 1; p.add(txtFullName, gbc);

        gbc.gridx = 0; gbc.gridy = 4; p.add(new JLabel("Course:"), gbc);
        txtCourse = new JTextField(15);
        gbc.gridx = 1; p.add(txtCourse, gbc);

        gbc.gridx = 0; gbc.gridy = 5; p.add(new JLabel("Year Level:"), gbc);
        cbYearLevel = new JComboBox<>(new String[]{"1st Year", "2nd Year", "3rd Year", "4th Year", "N/A (Admin)"});
        gbc.gridx = 1; p.add(cbYearLevel, gbc);

        gbc.gridx = 0; gbc.gridy = 6; p.add(new JLabel("Contact No:"), gbc);
        txtContact = new JTextField(15);
        gbc.gridx = 1; p.add(txtContact, gbc);

        gbc.gridx = 0; gbc.gridy = 7; p.add(new JLabel("Email Address:"), gbc);
        txtEmail = new JTextField(15);
        gbc.gridx = 1; p.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 8; p.add(new JLabel("System Role:"), gbc);
        cbRole = new JComboBox<>(new String[]{"STUDENT", "ADMIN"});
        gbc.gridx = 1; p.add(cbRole, gbc);

        btnRegister = new JButton("Register Now");
        styleButton(btnRegister);
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2;
        p.add(btnRegister, gbc);

        JButton btnSwitch = new JButton("Back to login");
        btnSwitch.setBorderPainted(false);
        btnSwitch.setContentAreaFilled(false);
        btnSwitch.setForeground(Color.DARK_GRAY);
        gbc.gridy = 10;
        p.add(btnSwitch, gbc);

        btnRegister.addActionListener(e -> {
            if(txtRegUser.getText().isEmpty() || new String(txtRegPass.getPassword()).isEmpty() || txtFullName.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username, Password, and Full Name are required fields.", "Form Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            User u = new User(0, txtRegUser.getText(), new String(txtRegPass.getPassword()), txtFullName.getText(),
                              txtCourse.getText(), cbYearLevel.getSelectedItem().toString(), txtContact.getText(), 
                              txtEmail.getText(), cbRole.getSelectedItem().toString(), "");
            
            if (userDAO.registerUser(u)) {
                JOptionPane.showMessageDialog(this, "Registration Successful!");
                cardLayout.show(mainPanel, "LOGIN");
            } else {
                JOptionPane.showMessageDialog(this, "User registration failed. Username might be taken.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSwitch.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        return p;
    }

    private void styleButton(JButton btn) {
        btn.setBackground(Color.WHITE);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
    }
}