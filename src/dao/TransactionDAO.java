package dao;

import config.DatabaseConnection;
import javax.swing.JOptionPane;
import java.sql.*;
import java.util.Vector;

public class TransactionDAO {

    private static final double DEFAULT_DAILY_FINE_RATE = 20;

    public boolean borrowBook(int bookId, int userId, int durationDays) {
        String countActiveQuery = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND status = 'BORROWED'";
        String countOverdueQuery = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND status = 'OVERDUE'";
        String checkQty = "SELECT quantity FROM books WHERE book_id = ?";
        String insertTx = "INSERT INTO transactions (book_id, user_id, borrow_date, due_date, status, fine_amount, fine_paid) VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL ? DAY), 'BORROWED', 0.00, FALSE)";
        String deductQty = "UPDATE books SET quantity = quantity - 1 WHERE book_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); 

            try (PreparedStatement stmt = conn.prepareStatement(countOverdueQuery)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(null, "Borrow Blocked: Overdue item restriction active.", "Account Restricted", JOptionPane.WARNING_MESSAGE);
                        conn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(countActiveQuery)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) >= 3) {
                        JOptionPane.showMessageDialog(null, "Borrow Blocked: Maximum borrow limit reached (3 books).", "Limit Exceeded", JOptionPane.WARNING_MESSAGE);
                        conn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(checkQty)) {
                stmt.setInt(1, bookId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt("quantity") <= 0) {
                        JOptionPane.showMessageDialog(null, "Borrow Blocked: Out of physical stock.", "Inventory Shortage", JOptionPane.INFORMATION_MESSAGE);
                        conn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(insertTx)) {
                stmt.setInt(1, bookId); stmt.setInt(2, userId); stmt.setInt(3, durationDays);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(deductQty)) {
                stmt.setInt(1, bookId);
                stmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); } }
        }
    }

    public boolean returnBook(int txId, int bookId) {
        String updateTxSql = "UPDATE transactions SET " +
                             "return_date = CURDATE(), " +
                             "status = 'RETURNED', " +
                             "fine_amount = CASE WHEN CURDATE() > due_date THEN DATEDIFF(CURDATE(), due_date) * ? ELSE 0.00 END " +
                             "WHERE transaction_id = ?";
        String updateBookSql = "UPDATE books SET quantity = quantity + 1 WHERE book_id = ?";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); 
            
            try (PreparedStatement stmtTx = conn.prepareStatement(updateTxSql)) {
                stmtTx.setDouble(1, DEFAULT_DAILY_FINE_RATE);
                stmtTx.setInt(2, txId);
                stmtTx.executeUpdate();
            }
            
            try (PreparedStatement stmtBook = conn.prepareStatement(updateBookSql)) {
                stmtBook.setInt(1, bookId);
                stmtBook.executeUpdate();
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            System.out.println("DEBUG ERROR: Return book database transaction failed.");
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return false;
    }

    public Vector<Vector<Object>> getActiveTransactions() {
        Vector<Vector<Object>> list = new Vector<>();
        
        updateLiveOverdueFines();

        String sql = "SELECT t.transaction_id, b.isbn, u.full_name, t.borrow_date, t.due_date, t.return_date, t.status, t.fine_amount, " +
                     "CASE WHEN t.fine_paid = TRUE THEN 'PAID' ELSE 'EARLY RETURN' END AS fine_status, t.book_id " +
                     "FROM transactions t " +
                     "INNER JOIN books b ON t.book_id = b.book_id " +
                     "INNER JOIN users u ON t.user_id = u.user_id " +
                     "ORDER BY t.transaction_id DESC";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("transaction_id"));          
                row.add(rs.getString("isbn"));        
                row.add(rs.getString("full_name"));    
                row.add(rs.getDate("borrow_date"));    
                row.add(rs.getDate("due_date"));       
                row.add(rs.getDate("return_date") != null ? rs.getDate("return_date") : "Not Returned");
                row.add(rs.getString("status"));       
                row.add(rs.getDouble("fine_amount"));  
                row.add(rs.getString("fine_status"));  
                row.add(rs.getInt("book_id"));          
                list.add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Vector<Vector<Object>> getStudentTransactions(int userId) {
        Vector<Vector<Object>> list = new Vector<>();
        
        updateLiveOverdueFines();

        String sql = "SELECT t.transaction_id, b.isbn, u.full_name, t.borrow_date, t.due_date, t.return_date, t.status, t.fine_amount, " +
                     "CASE WHEN t.fine_paid = TRUE THEN 'PAID' ELSE 'EARLY RETURN' END AS fine_status, t.book_id " +
                     "FROM transactions t " +
                     "INNER JOIN books b ON t.book_id = b.book_id " +
                     "INNER JOIN users u ON t.user_id = u.user_id " +
                     "WHERE t.user_id = ? " +
                     "ORDER BY t.transaction_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("transaction_id"));
                    row.add(rs.getString("isbn"));
                    row.add(rs.getString("full_name"));
                    row.add(rs.getDate("borrow_date"));
                    row.add(rs.getDate("due_date"));
                    row.add(rs.getDate("return_date") != null ? rs.getDate("return_date") : "Active");
                    row.add(rs.getString("status"));
                    row.add(rs.getDouble("fine_amount")); 
                    row.add(rs.getString("fine_status")); 
                    row.add(rs.getInt("book_id"));
                    list.add(row);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

   
    private void updateLiveOverdueFines() {
        String query = "UPDATE transactions SET " +
                       "status = 'OVERDUE', " +
                       "fine_amount = DATEDIFF(CURDATE(), due_date) * ? " +
                       "WHERE due_date < CURDATE() AND return_date IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDouble(1, DEFAULT_DAILY_FINE_RATE);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DEBUG WARNING: Background fine routine maintenance pass skipped.");
            e.printStackTrace();
        }
    }

   
    public boolean settleTransactionFine(int txId) {
        String sql = "UPDATE transactions SET fine_paid = TRUE WHERE transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, txId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}