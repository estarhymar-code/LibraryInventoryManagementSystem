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

    private JLabel lblStatTotalBooks, lblStatBorrowed, lblStatOverdue, lblStatStudents;

    private JTextField txtIsbn, txtTitle, txtAuthor, txtPublisher, txtPubYear, txtQty, txtCatId, txtSearch;
    private int selectedBookId = -1;
    
    private JTextField txtUName, txtUPass, txtUFullName, txtUCourse, txtUContact, txtUEmail;
    private JLabel lblRegDateValue; 
    private JComboBox<String> cbUYear, cbURole;
    private int selectedUserId = -1; 

    public DashboardFrame(User user) {
        this.currentUser = user;
        setTitle("Library Database Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1250, 820);
        setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setEnabled(false);

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Color.DARK_GRAY);
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JPanel topSidebar = new JPanel(new GridLayout(2, 1, 5, 5));
        topSidebar.setOpaque(false);
        JLabel lblUser = new JLabel(currentUser.getFullName(), SwingConstants.CENTER);
        lblUser.setForeground(Color.WHITE);
        lblUser.setFont(new Font("Arial", Font.BOLD, 14));
        JLabel lblRole = new JLabel("(" + currentUser.getRole() + ")", SwingConstants.CENTER);
        lblRole.setForeground(Color.LIGHT_GRAY);
        topSidebar.add(lblUser);
        topSidebar.add(lblRole);
        sidebar.add(topSidebar, BorderLayout.NORTH);

        JButton btnLogout = new JButton("Log Out");
        formatButton(btnLogout);
        sidebar.add(btnLogout, BorderLayout.SOUTH);

        btnLogout.addActionListener(e -> {
            this.dispose();
            new AuthFrame();
        });

        splitPane.setLeftComponent(sidebar);

        JPanel contentContainer = new JPanel(new BorderLayout(10, 10));
        contentContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if ("ADMIN".equals(currentUser.getRole())) {
            contentContainer.add(buildAnalyticsDashboardPanel(), BorderLayout.NORTH);
        }

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Books Available", buildBooksTab());
        
        String txTabTitle = "ADMIN".equals(currentUser.getRole()) ? "Circulation Desk" : "My Borrowed Books";
        tabbedPane.addTab(txTabTitle, buildTransactionsTab());
        
        if ("ADMIN".equals(currentUser.getRole())) {
            tabbedPane.addTab("User Management", buildUsersTab());
        }
        
        contentContainer.add(tabbedPane, BorderLayout.CENTER);
        splitPane.setRightComponent(contentContainer);
        add(splitPane);

        refreshAllWorkspaceData();
        setVisible(true);
    }

    private void formatButton(JButton button) {
        button.setBackground(Color.WHITE);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 12));
    }

    private JPanel buildAnalyticsDashboardPanel() {
        JPanel container = new JPanel(new GridLayout(1, 4, 15, 0));
        container.setBorder(BorderFactory.createTitledBorder("System Overview Metrics"));
        
        lblStatTotalBooks = createMetricCard(container, "Total Volume Assets");
        lblStatBorrowed = createMetricCard(container, "Active Borrow Checked-Out");
        lblStatOverdue = createMetricCard(container, "Unreturned / Overdue Accounts");
        lblStatStudents = createMetricCard(container, "Registered Student Accounts");
        
        return container;
    }

    private JLabel createMetricCard(JPanel parent, String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        JLabel lblTitle = new JLabel(title, SwingConstants.LEFT);
        lblTitle.setFont(new Font("Arial", Font.PLAIN, 11));
        lblTitle.setForeground(Color.GRAY);
        
        JLabel lblValue = new JLabel("0", SwingConstants.RIGHT);
        lblValue.setFont(new Font("Arial", Font.BOLD, 22));
        lblValue.setForeground(Color.BLACK);
        
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        parent.add(card);
        
        return lblValue;
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
            JButton btnAdd = new JButton("Save New");
            JButton btnEdit = new JButton("Update");
            JButton btnDelete = new JButton("Delete");
            
            formatButton(btnAdd); formatButton(btnEdit); formatButton(btnDelete);
            
            actionButtonPanel.add(btnAdd); actionButtonPanel.add(btnEdit); actionButtonPanel.add(btnDelete);
            form.add(new JLabel("Operations:")); form.add(actionButtonPanel);
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
                    txtCatId.setText("1"); 
                    txtQty.setText(bookModel.getValueAt(modelRow, 7).toString());
                }
            });

            btnAdd.addActionListener(e -> {
                try {
                    Book b = new Book();
                    b.setIsbn(txtIsbn.getText()); b.setTitle(txtTitle.getText()); b.setAuthor(txtAuthor.getText());
                    b.setPublisher(txtPublisher.getText()); b.setPublicationYear(Integer.parseInt(txtPubYear.getText().trim()));
                    b.setCategoryId(Integer.parseInt(txtCatId.getText().trim())); b.setQuantity(Integer.parseInt(txtQty.getText().trim()));
                    if (bookDAO.addBook(b)) {
                        JOptionPane.showMessageDialog(this, "Book asset mapped successfully.");
                        refreshAllWorkspaceData(); clearBookFields();
                    }
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Production Error: Check numeric formatting lines for year/quantity levels!"); }
            });

            btnEdit.addActionListener(e -> {
                if (selectedBookId == -1) return;
                try {
                    Book b = new Book(); b.setBookId(selectedBookId);
                    b.setIsbn(txtIsbn.getText()); b.setTitle(txtTitle.getText()); b.setAuthor(txtAuthor.getText());
                    b.setPublisher(txtPublisher.getText()); b.setPublicationYear(Integer.parseInt(txtPubYear.getText().trim()));
                    b.setCategoryId(Integer.parseInt(txtCatId.getText().trim())); b.setQuantity(Integer.parseInt(txtQty.getText().trim()));
                    if (bookDAO.updateBook(b)) {
                        JOptionPane.showMessageDialog(this, "Asset specifications recorded.");
                        refreshAllWorkspaceData(); clearBookFields();
                    }
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Please check form validation requirements."); }
            });

            btnDelete.addActionListener(e -> {
                if (selectedBookId == -1) return;
                if (bookDAO.deleteBook(selectedBookId)) {
                    JOptionPane.showMessageDialog(this, "Asset removed from mapping ledger.");
                    refreshAllWorkspaceData(); clearBookFields();
                }
            });
        } else {
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton btnBorrow = new JButton("Rent Selected Book");
            formatButton(btnBorrow);
            actionPanel.add(btnBorrow);
            main.add(actionPanel, BorderLayout.SOUTH);

            btnBorrow.addActionListener(e -> {
                int row = tableBooks.getSelectedRow();
                if (row != -1) {
                    int modelRow = tableBooks.convertRowIndexToModel(row);
                    int bookId = (int) bookModel.getValueAt(modelRow, 0);
                    
                    if (txDAO.borrowBook(bookId, currentUser.getUserId(), 14)) {
                        JOptionPane.showMessageDialog(this, "Book checkout cleared! Please collect your item and return within 14 calendar days.");
                        refreshAllWorkspaceData();
                    }
                }
            });
        }
        return main;
    }

    private JPanel buildTransactionsTab() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"Tx ID", "Book Title", "Borrower", "Borrowed Date", "Due Date", "Returned Date", "Status", "Book ID"};
        txModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableTx = new JTable(txModel);
        
        tableTx.removeColumn(tableTx.getColumnModel().getColumn(7)); 
        main.add(new JScrollPane(tableTx), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnPrint = new JButton("Print Circulation Report");
        formatButton(btnPrint);
        controlPanel.add(btnPrint);

        JButton btnReturn = new JButton("Return Selected Book");
        formatButton(btnReturn);
        controlPanel.add(btnReturn);
        
        main.add(controlPanel, BorderLayout.SOUTH);

        btnReturn.addActionListener(e -> {
            int row = tableTx.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select an active transaction item from the table first.");
                return;
            }

            int modelRow = tableTx.convertRowIndexToModel(row);

            int txId = (int) txModel.getValueAt(modelRow, 0);
            String status = (String) txModel.getValueAt(modelRow, 6);
            int bookId = (int) txModel.getValueAt(modelRow, 7);

            if ("RETURNED".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(this, "This book has already been processed as returned.");
                return;
            }

            if (txDAO.returnBook(txId, bookId)) {
                JOptionPane.showMessageDialog(this, "Book successfully returned and logged back into inventory!");

                refreshAllWorkspaceData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to process book return. Check system console logs.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPrint.addActionListener(e -> {
            try {
                boolean completed = tableTx.print(JTable.PrintMode.FIT_WIDTH, 
                    new java.text.MessageFormat("Library Operations Center - Master Circulation Audit Ledger"), 
                    new java.text.MessageFormat("Page {0}"));
                if(completed) {
                    JOptionPane.showMessageDialog(this, "Printing system document queue cleared.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Printing subsystem failure details: " + ex.getMessage());
            }
        });

        return main;
    }

    private JPanel buildUsersTab() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"User ID", "Username", "Password", "Full Name", "Course", "Year Level", "Contact No", "Email", "Role", "Registered Date"};
        userModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableUsers = new JTable(userModel);
        main.add(new JScrollPane(tableUsers), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(6, 4, 5, 5));
        form.setBorder(BorderFactory.createTitledBorder("Manage Accounts Data Profiles"));

        form.add(new CenterLabel("Username:")); txtUName = new JTextField(); form.add(txtUName);
        form.add(new CenterLabel("Password:")); txtUPass = new JTextField(); form.add(txtUPass);
        form.add(new CenterLabel("Full Name:")); txtUFullName = new JTextField(); form.add(txtUFullName);
        form.add(new CenterLabel("Course:")); txtUCourse = new JTextField(); form.add(txtUCourse);
        form.add(new CenterLabel("Year Level:")); cbUYear = new JComboBox<>(new String[]{"1st Year", "2nd Year", "3rd Year", "4th Year", "N/A (Admin)"}); form.add(cbUYear);
        form.add(new CenterLabel("Contact No:")); txtUContact = new JTextField(); form.add(txtUContact);
        form.add(new CenterLabel("Email Address:")); txtUEmail = new JTextField(); form.add(txtUEmail);
        form.add(new CenterLabel("System Role:")); cbURole = new JComboBox<>(new String[]{"STUDENT", "ADMIN"}); form.add(cbURole);
        
        form.add(new CenterLabel("Date Registered:")); lblRegDateValue = new JLabel("-"); form.add(lblRegDateValue);

        JButton btnAddU = new JButton("Add Account");
        JButton btnEditU = new JButton("Update Profile");
        JButton btnDeleteU = new JButton("Remove Account");

        formatButton(btnAddU); formatButton(btnEditU); formatButton(btnDeleteU);

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
                lblRegDateValue.setText(userModel.getValueAt(row, 9).toString()); 
            }
        });

        btnAddU.addActionListener(e -> {
            User u = new User(0, txtUName.getText(), txtUPass.getText(), txtUFullName.getText(), txtUCourse.getText(), cbUYear.getSelectedItem().toString(), txtUContact.getText(), txtUEmail.getText(), cbURole.getSelectedItem().toString(), "");
            if(userDAO.registerUser(u)) {
                JOptionPane.showMessageDialog(this, "Profile added to database.");
                refreshAllWorkspaceData(); clearUserFields();
            }
        });

        btnEditU.addActionListener(e -> {
            if (selectedUserId == -1) return;
            User u = new User(selectedUserId, txtUName.getText(), txtUPass.getText(), txtUFullName.getText(), txtUCourse.getText(), cbUYear.getSelectedItem().toString(), txtUContact.getText(), txtUEmail.getText(), cbURole.getSelectedItem().toString(), lblRegDateValue.getText());
            if(userDAO.updateUser(u)) {
                JOptionPane.showMessageDialog(this, "Profile properties modified.");
                refreshAllWorkspaceData(); clearUserFields();
            }
        });

        btnDeleteU.addActionListener(e -> {
            if (selectedUserId == -1) return;
            if(userDAO.deleteUser(selectedUserId)) {
                JOptionPane.showMessageDialog(this, "Account dropped from database registries.");
                refreshAllWorkspaceData(); clearUserFields();
            }
        });

        return main;
    }

    private void refreshAllWorkspaceData() {
    try {
        bookModel.setRowCount(0);
        List<Book> books = bookDAO.getAllBooks();
        for (Book b : books) {
            bookModel.addRow(new Object[]{b.getBookId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getPublisher(), b.getPublicationYear(), b.getCategoryName(), b.getQuantity()});
        }
    } catch (Exception e) {
        System.out.println("DEBUG ERROR: Failed to load Book Catalog table.");
        e.printStackTrace();
    }

    try {
        txModel.setRowCount(0);
        
        if (tableTx.getRowSorter() != null) {
            tableTx.setRowSorter(null);
        }
        
        Vector<Vector<Object>> data;
        if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            data = txDAO.getActiveTransactions(); 
        } else {
            data = txDAO.getStudentTransactions(currentUser.getUserId());
        }
        
        if (data == null || data.isEmpty()) {
            System.out.println("DEBUG WARNING: Transaction database query returned 0 records for user ID: " + currentUser.getUserId());
        } else {
            for (Vector<Object> row : data) {
                if (row.size() == 8) {
                    txModel.addRow(row);
                } else {
                    System.out.println("DEBUG ERROR: Row data size mismatch! Expected 8 columns, got " + row.size());
                }
            }
        }
    } catch (Exception e) {
        System.out.println("CRITICAL UI ERROR: The transaction refresh routine crashed!");
        e.printStackTrace(); 
    }

    if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
        try {
            userModel.setRowCount(0);
            List<User> users = userDAO.getAllUsers();
            for(User u : users) {
                userModel.addRow(new Object[]{u.getUserId(), u.getUsername(), u.getPassword(), u.getFullName(), u.getCourse(), u.getYearLevel(), u.getContactNumber(), u.getEmail(), u.getRole(), u.getDateRegistered()});
            }
            
            int[] currentStats = bookDAO.getLibraryStatistics();
            lblStatTotalBooks.setText(String.valueOf(currentStats[0]));
            lblStatBorrowed.setText(String.valueOf(currentStats[1]));
            lblStatOverdue.setText(String.valueOf(currentStats[2]));
            lblStatStudents.setText(String.valueOf(currentStats[3]));
        } catch (Exception e) {
            System.out.println("DEBUG ERROR: Failed to load Admin specific panels.");
            e.printStackTrace();
        }
    }
}

    private void clearBookFields() {
        selectedBookId = -1;
        txtIsbn.setText(""); txtTitle.setText(""); txtAuthor.setText(""); txtPublisher.setText(""); txtPubYear.setText(""); txtCatId.setText(""); txtQty.setText("");
        tableBooks.clearSelection();
    }

    private void clearUserFields() {
        selectedUserId = -1;
        txtUName.setText(""); txtUPass.setText(""); txtUFullName.setText(""); txtUCourse.setText(""); txtUContact.setText(""); txtUEmail.setText("");
        lblRegDateValue.setText("-"); cbUYear.setSelectedIndex(0); cbURole.setSelectedIndex(0);
        tableUsers.clearSelection();
    }
    
    private static class CenterLabel extends JLabel {
        public CenterLabel(String text) {
            super(text, SwingConstants.LEFT);
        }
    }
}