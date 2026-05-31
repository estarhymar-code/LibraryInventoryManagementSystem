package dao;

import config.DatabaseConnection;
import java.sql.*;
import java.util.Vector;

public class TransactionDAO {

   
    public boolean borrowBook(int bookId, int userId, int durationDays) {
        String checkQty = "SELECT quantity FROM books WHERE book_id = ?";
        String insertTx = "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status) " +
                          "VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL ? DAY), 'BORROWED')";
        String deductQty = "UPDATE books SET quantity = quantity - 1 WHERE book_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); 

            try (PreparedStatement stmt = conn.prepareStatement(checkQty)) {
                stmt.setInt(1, bookId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt("quantity") <= 0) {
                        conn.rollback(); 
                        return false;
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(insertTx)) {
                stmt.setInt(1, bookId);
                stmt.setInt(2, userId);
                stmt.setInt(3, durationDays);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(deductQty)) {
                stmt.setInt(1, bookId);
                stmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }


    public boolean returnBook(int transactionId, int bookId) {
        String updateTx = "UPDATE transactions SET return_date = CURDATE(), status = 'RETURNED' WHERE transaction_id = ?";
        String addQty = "UPDATE books SET quantity = quantity + 1 WHERE book_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); 

            try (PreparedStatement stmt = conn.prepareStatement(updateTx)) {
                stmt.setInt(1, transactionId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(addQty)) {
                stmt.setInt(1, bookId);
                stmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public Vector<Vector<Object>> getActiveTransactions() {
        Vector<Vector<Object>> data = new Vector<>();
        String query = "SELECT t.transaction_id, b.title, u.full_name, t.borrow_date, t.due_date, t.status, b.book_id " +
                       "FROM transactions t " +
                       "JOIN books b ON t.book_id = b.book_id " +
                       "JOIN users u ON t.user_id = u.user_id " +
                       "WHERE t.status != 'RETURNED'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String updateOverdue = "UPDATE transactions SET status = 'OVERDUE' WHERE due_date < CURDATE() AND status = 'BORROWED'";
            stmt.executeUpdate(updateOverdue);

            try (ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("transaction_id"));
                    row.add(rs.getString("title"));
                    row.add(rs.getString("full_name"));
                    row.add(rs.getDate("borrow_date"));
                    row.add(rs.getDate("due_date"));
                    row.add(rs.getString("status"));
                    row.add(rs.getInt("book_id")); // Hidden helper index tracking column
                    data.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public Vector<Vector<Object>> getStudentTransactions(int userId) {
        Vector<Vector<Object>> data = new Vector<>();
        String query = "SELECT t.transaction_id, b.title, u.full_name, t.borrow_date, t.due_date, t.status, b.book_id " +
                       "FROM transactions t " +
                       "JOIN books b ON t.book_id = b.book_id " +
                       "JOIN users u ON t.user_id = u.user_id " +
                       "WHERE t.status != 'RETURNED' AND t.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("transaction_id"));
                    row.add(rs.getString("title"));
                    row.add(rs.getString("full_name"));
                    row.add(rs.getDate("borrow_date"));
                    row.add(rs.getDate("due_date"));
                    row.add(rs.getString("status"));
                    row.add(rs.getInt("book_id")); 
                    data.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }
}