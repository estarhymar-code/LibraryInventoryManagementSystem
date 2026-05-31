package ui;

import dao.BookDAO;
import dao.TransactionDAO;
import dao.UserDAO;
import models.Book;
import models.User;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class DashboardFrame extends JFrame {
    private User currentUser;
    private JTable tableBooks, tableTx, tableUsers;
    private DefaultTableModel bookModel, txModel, userModel;
    private TableRowSorter<DefaultTableModel> bookSorter;
    
    private BookDAO bookDAO = new BookDAO();
    private TransactionDAO txDAO = new TransactionDAO();
    private UserDAO userDAO = new UserDAO();

    private JTextField txtIsbn, txtTitle, txtAuthor, txtPublisher, txtPubYear, txtQty, txtCatId, txtSearch;
    private int selectedBookId = -1; 
    
    private JTextField txtUName, txtUPass, txtUFullName, txtUCourse, txtUContact, txtUEmail;
    private JComboBox<String> cbUYear, cbURole;
    private int selectedUserId = -1; 

    public DashboardFrame(User user) {
        this.currentUser = user;
        setTitle("LMS Dashboard - " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 750);
        setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(220);
        splitPane.setEnabled(false);

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JPanel topSidebar = new JPanel(new GridLayout(2, 1, 5, 5));
        topSidebar.setOpaque(false);
        JLabel lblUser = new JLabel(currentUser.getFullName(), SwingConstants.CENTER);
        lblUser.setForeground(Color.WHITE);
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel lblRole = new JLabel("(" + currentUser.getRole() + ")", SwingConstants.CENTER);
        lblRole.setForeground(new Color(189, 195, 199));
        lblRole.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        topSidebar.add(lblUser);
        topSidebar.add(lblRole);
        sidebar.add(topSidebar, BorderLayout.NORTH);

        JButton btnLogout = new JButton("Log Out");
        btnLogout.setBackground(Color.WHITE);
        btnLogout.setForeground(Color.BLACK);
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogout.setFocusPainted(false);
        btnLogout.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        sidebar.add(btnLogout, BorderLayout.SOUTH);

        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Logout Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                this.dispose();
                new AuthFrame();
            }
        });

        splitPane.setLeftComponent(sidebar);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Catalog Explorer", buildBooksTab());
        
        String txTabTitle = "ADMIN".equals(currentUser.getRole()) ? "Circulation Desk (All Loans)" : "My Rented Books";
        tabbedPane.addTab(txTabTitle, buildTransactionsTab());
        
        if ("ADMIN".equals(currentUser.getRole())) {
            tabbedPane.addTab("User Management", buildUsersTab());
        }
        
        splitPane.setRightComponent(tabbedPane);

        add(splitPane);
        refreshBookData();
        refreshTxData();
        if ("ADMIN".equals(currentUser.getRole())) {
            refreshUserData();
        }
        setVisible(true);
    }

    private JPanel buildBooksTab() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search Catalog: "));
        txtSearch = new JTextField(25);
        searchPanel.add(txtSearch);
        main.add(searchPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "ISBN", "Title", "Author", "Publisher", "Year", "Category", "Stock Qty"};
        bookModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableBooks = new JTable(bookModel);
        
        bookSorter = new TableRowSorter<>(bookModel);
        tableBooks.setRowSorter(bookSorter);
        
        main.add(new JScrollPane(tableBooks), BorderLayout.CENTER);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterCatalog(); }
            @Override public void removeUpdate(DocumentEvent e) { filterCatalog(); }
            @Override public void changedUpdate(DocumentEvent e) { filterCatalog(); }
            
            private void filterCatalog() {
                String searchString = txtSearch.getText();
                if (searchString.trim().length() == 0) {
                    bookSorter.setRowFilter(null);
                } else {
                    bookSorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchString));
                }
            }
        });

        if ("ADMIN".equals(currentUser.getRole())) {
            JPanel form = new JPanel(new GridLayout(9, 2, 5, 5));
            form.setBorder(BorderFactory.createTitledBorder("Manage Inventory Assets"));

            form.add(new JLabel("ISBN:")); txtIsbn = new JTextField(); form.add(txtIsbn);
            form.add(new JLabel("Title:")); txtTitle = new JTextField(); form.add(txtTitle);
            form.add(new JLabel("Author:")); txtAuthor = new JTextField(); form.add(txtAuthor);
            form.add(new JLabel("Publisher:")); txtPublisher = new JTextField(); form.add(txtPublisher);
            form.add(new JLabel("Publication Year:")); txtPubYear = new JTextField(); form.add(txtPubYear);
            form.add(new JLabel("Category ID (1-4):")); txtCatId = new JTextField(); form.add(txtCatId);
            form.add(new JLabel("Stock Quantity:")); txtQty = new JTextField(); form.add(txtQty);

            JPanel actionButtonPanel = new JPanel(new GridLayout(1, 3, 5, 5));
            JButton btnAdd = new JButton("Save New Asset");
            JButton btnEdit = new JButton("Update Asset Details");
            JButton btnDelete = new JButton("Remove Asset");
            
            btnAdd.setBackground(Color.WHITE); btnAdd.setForeground(Color.BLACK);
            btnEdit.setBackground(Color.WHITE); btnEdit.setForeground(Color.BLACK);
            btnDelete.setBackground(Color.WHITE); btnDelete.setForeground(Color.BLACK);
            
            actionButtonPanel.add(btnAdd);
            actionButtonPanel.add(btnEdit);
            actionButtonPanel.add(btnDelete);
            
            form.add(new JLabel("Control Operations:"));
            form.add(actionButtonPanel);
            
            main.add(form, BorderLayout.SOUTH);

            tableBooks.getSelectionModel().addListSelectionListener(e -> {
                int row = tableBooks.getSelectedRow();
                if (row != -1) {
                    int modelRow = tableBooks.convertRowIndexToModel(row);
                    selectedBookId = (int) bookModel.getValueAt(modelRow, 0);
                    txtIsbn.setText(bookModel.getValueAt(modelRow, 1).toString());
                    txtTitle.setText(bookModel.getValueAt(modelRow, 2).toString());
                    txtAuthor.setText(bookModel.getValueAt(modelRow, 3).toString());
                    txtPublisher.setText(bookModel.getValueAt(modelRow, 4).toString());
                    txtPubYear.setText(bookModel.getValueAt(modelRow, 5).toString());
                    txtCatId.setText(bookModel.getValueAt(modelRow, 6).toString().equals("Fiction") ? "1" : 
                                    bookModel.getValueAt(modelRow, 6).toString().equals("Non-Fiction") ? "2" : 
                                    bookModel.getValueAt(modelRow, 6).toString().equals("Science") ? "3" : "4"); 
                    txtQty.setText(bookModel.getValueAt(modelRow, 7).toString());
                }
            });

            btnAdd.addActionListener(e -> {
                if(txtIsbn.getText().isEmpty() || txtTitle.getText().isEmpty() || txtPubYear.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "ISBN, Title, and Publication Year are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    Book b = new Book();
                    b.setIsbn(txtIsbn.getText());
                    b.setTitle(txtTitle.getText());
                    b.setAuthor(txtAuthor.getText());
                    b.setPublisher(txtPublisher.getText());
                    b.setPublicationYear(Integer.parseInt(txtPubYear.getText().trim()));
                    b.setCategoryId(Integer.parseInt(txtCatId.getText().trim()));
                    b.setQuantity(Integer.parseInt(txtQty.getText().trim()));
                    
                    if (bookDAO.addBook(b)) {
                        JOptionPane.showMessageDialog(this, "Asset added safely.");
                        refreshBookData();
                        clearBookFields();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Publication Year, Category ID, and Quantity must be valid integers.", "Type Format Mismatch", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnEdit.addActionListener(e -> {
                if (selectedBookId == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a book from the table list above to edit.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                try {
                    Book b = new Book();
                    b.setBookId(selectedBookId);
                    b.setIsbn(txtIsbn.getText());
                    b.setTitle(txtTitle.getText());
                    b.setAuthor(txtAuthor.getText());
                    b.setPublisher(txtPublisher.getText());
                    b.setPublicationYear(Integer.parseInt(txtPubYear.getText().trim()));
                    b.setCategoryId(Integer.parseInt(txtCatId.getText().trim()));
                    b.setQuantity(Integer.parseInt(txtQty.getText().trim()));
                    
                    if (bookDAO.updateBook(b)) {
                        JOptionPane.showMessageDialog(this, "Book asset metadata updated successfully.");
                        refreshBookData();
                        clearBookFields();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please verify that numeric fields contain integers.", "Type Format Mismatch", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnDelete.addActionListener(e -> {
                if (selectedBookId == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a book from the table list above to remove.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to permanently erase this book from inventory records?", "Confirm Destructive Deletion", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (bookDAO.deleteBook(selectedBookId)) {
                        JOptionPane.showMessageDialog(this, "Book resource safely deleted from system registries.");
                        refreshBookData();
                        clearBookFields();
                    }
                }
            });
            
        } else {
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton btnBorrow = new JButton("Rent Selected Book");
            btnBorrow.setBackground(Color.WHITE);
            btnBorrow.setForeground(Color.BLACK);
            btnBorrow.setFont(new Font("Segoe UI", Font.BOLD, 12));
            actionPanel.add(btnBorrow);
            main.add(actionPanel, BorderLayout.SOUTH);

            btnBorrow.addActionListener(e -> {
                int row = tableBooks.getSelectedRow();
                if (row != -1) {
                    int modelRow = tableBooks.convertRowIndexToModel(row);
                    int bookId = (int) bookModel.getValueAt(modelRow, 0);
                    
                    if (txDAO.borrowBook(bookId, currentUser.getUserId(), 14)) {
                        JOptionPane.showMessageDialog(this, "Book successfully rented! Check your 'My Rented Books' panel.");
                        refreshBookData();
                        refreshTxData();
                    } else {
                        JOptionPane.showMessageDialog(this, "Item out of stock.", "Transaction Exception", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Please select a book from the table first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }
        return main;
    }

    private JPanel buildTransactionsTab() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"Tx ID", "Book Title", "Borrower", "Issued Date", "Deadline Date", "Status", "Book ID"};
        txModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableTx = new JTable(txModel);
        tableTx.removeColumn(tableTx.getColumnModel().getColumn(6));
        
        main.add(new JScrollPane(tableTx), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnReturn = new JButton("Return Selected Book");
        btnReturn.setBackground(Color.WHITE);
        btnReturn.setForeground(Color.BLACK);
        btnReturn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnReturn.setFocusPainted(false);
        controlPanel.add(btnReturn);
        
        main.add(controlPanel, BorderLayout.SOUTH);

        btnReturn.addActionListener(e -> {
            int row = tableTx.getSelectedRow();
            if (row != -1) {
                int txId = (int) txModel.getValueAt(row, 0);
                int bookId = (int) txModel.getValueAt(row, 6);
                if (txDAO.returnBook(txId, bookId)) {
                    JOptionPane.showMessageDialog(this, "Book returned successfully and updated in inventory storage.");
                    refreshBookData();
                    refreshTxData();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a rented book item to return.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        return main;
    }

    private JPanel buildUsersTab() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"User ID", "Username", "Password", "Full Name", "Course", "Year Level", "Contact No", "Email", "Role"};
        userModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableUsers = new JTable(userModel);
        main.add(new JScrollPane(tableUsers), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(5, 4, 5, 5));
        form.setBorder(BorderFactory.createTitledBorder("Manage Accounts Data Profiles"));

        form.add(new JLabel("Username:")); txtUName = new JTextField(); form.add(txtUName);
        form.add(new JLabel("Password:")); txtUPass = new JTextField(); form.add(txtUPass);
        form.add(new JLabel("Full Name:")); txtUFullName = new JTextField(); form.add(txtUFullName);
        form.add(new JLabel("Course:")); txtUCourse = new JTextField(); form.add(txtUCourse);
        
        form.add(new JLabel("Year Level:"));
        cbUYear = new JComboBox<>(new String[]{"1st Year", "2nd Year", "3rd Year", "4th Year", "N/A (Admin)"});
        form.add(cbUYear);
        
        form.add(new JLabel("Contact No:")); txtUContact = new JTextField(); form.add(txtUContact);
        form.add(new JLabel("Email Address:")); txtUEmail = new JTextField(); form.add(txtUEmail);
        
        form.add(new JLabel("System Role:"));
        cbURole = new JComboBox<>(new String[]{"STUDENT", "ADMIN"});
        form.add(cbURole);

        JButton btnAddU = new JButton("Add Account");
        JButton btnEditU = new JButton("Update Profile");
        JButton btnDeleteU = new JButton("Remove Account");
        
        btnAddU.setBackground(Color.WHITE); btnAddU.setForeground(Color.BLACK);
        btnEditU.setBackground(Color.WHITE); btnEditU.setForeground(Color.BLACK);
        btnDeleteU.setBackground(Color.WHITE); btnDeleteU.setForeground(Color.BLACK);

        form.add(btnAddU); form.add(btnEditU); form.add(btnDeleteU);
        main.add(form, BorderLayout.SOUTH);

        tableUsers.getSelectionModel().addListSelectionListener(e -> {
            int row = tableUsers.getSelectedRow();
            if (row != -1) {
                selectedUserId = (int) userModel.getValueAt(row, 0);
                txtUName.setText(userModel.getValueAt(row, 1).toString());
                txtUPass.setText(userModel.getValueAt(row, 2).toString());
                txtUFullName.setText(userModel.getValueAt(row, 3).toString());
                txtUCourse.setText(userModel.getValueAt(row, 4).toString());
                cbUYear.setSelectedItem(userModel.getValueAt(row, 5).toString());
                txtUContact.setText(userModel.getValueAt(row, 6).toString());
                txtUEmail.setText(userModel.getValueAt(row, 7).toString());
                cbURole.setSelectedItem(userModel.getValueAt(row, 8).toString());
            }
        });

        btnAddU.addActionListener(e -> {
            if(txtUName.getText().isEmpty() || txtUPass.getText().isEmpty() || txtUFullName.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username, Password, and Full Name are mandatory fields.", "Validation Flag", JOptionPane.WARNING_MESSAGE);
                return;
            }
            User u = new User(0, txtUName.getText(), txtUPass.getText(), txtUFullName.getText(), txtUCourse.getText(), cbUYear.getSelectedItem().toString(), txtUContact.getText(), txtUEmail.getText(), cbURole.getSelectedItem().toString());
            if(userDAO.registerUser(u)) {
                JOptionPane.showMessageDialog(this, "Account created smoothly!");
                refreshUserData();
                clearUserFields();
            }
        });

        btnEditU.addActionListener(e -> {
            if (selectedUserId == -1) {
                JOptionPane.showMessageDialog(this, "Please choose an active profile item from the table grid layout below.", "Selection Flag", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            User u = new User(selectedUserId, txtUName.getText(), txtUPass.getText(), txtUFullName.getText(), txtUCourse.getText(), cbUYear.getSelectedItem().toString(), txtUContact.getText(), txtUEmail.getText(), cbURole.getSelectedItem().toString());
            if(userDAO.updateUser(u)) {
                JOptionPane.showMessageDialog(this, "Profile updated inside the registry database context.");
                refreshUserData();
                clearUserFields();
            }
        });

        btnDeleteU.addActionListener(e -> {
            if (selectedUserId == -1) {
                JOptionPane.showMessageDialog(this, "Select the record tracking parameter index you wish to remove.", "Selection Flag", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Are you certain you want to completely erase this user registry account? This operation is irreversible.", "Destructive Execution Warning", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if(userDAO.deleteUser(selectedUserId)) {
                    JOptionPane.showMessageDialog(this, "Registry reference scrubbed safely.");
                    refreshUserData();
                    clearUserFields();
                }
            }
        });

        return main;
    }

    private void refreshBookData() {
        bookModel.setRowCount(0);
        List<Book> books = bookDAO.getAllBooks();
        for (Book b : books) {
            bookModel.addRow(new Object[]{b.getBookId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getPublisher(), b.getPublicationYear(), b.getCategoryName(), b.getQuantity()});
        }
    }

    private void refreshTxData() {
        txModel.setRowCount(0);
        Vector<Vector<Object>> data;
        if ("ADMIN".equals(currentUser.getRole())) {
            data = txDAO.getActiveTransactions();
        } else {
            data = txDAO.getStudentTransactions(currentUser.getUserId());
        }
        for (Vector<Object> row : data) {
            txModel.addRow(row);
        }
    }

    private void refreshUserData() {
        userModel.setRowCount(0);
        List<User> users = userDAO.getAllUsers();
        for(User u : users) {
            userModel.addRow(new Object[]{u.getUserId(), u.getUsername(), u.getPassword(), u.getFullName(), u.getCourse(), u.getYearLevel(), u.getContactNumber(), u.getEmail(), u.getRole()});
        }
    }

    private void clearBookFields() {
        selectedBookId = -1;
        txtIsbn.setText(""); txtTitle.setText(""); txtAuthor.setText(""); 
        txtPublisher.setText(""); txtPubYear.setText(""); txtCatId.setText(""); txtQty.setText("");
        tableBooks.clearSelection();
    }

    private void clearUserFields() {
        selectedUserId = -1;
        txtUName.setText(""); txtUPass.setText(""); txtUFullName.setText(""); 
        txtUCourse.setText(""); txtUContact.setText(""); txtUEmail.setText("");
        cbUYear.setSelectedIndex(0); cbURole.setSelectedIndex(0);
        tableUsers.clearSelection();
    }
}