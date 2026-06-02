package dao;

import config.DatabaseConnection;
import models.Book;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    public int[] getLibraryStatistics() {
        int[] stats = new int[4]; // Order: [TotalBooks, TotalBorrowed, TotalOverdue, TotalStudents]
        String q1 = "SELECT IFNULL(SUM(quantity), 0) FROM books";
        String q2 = "SELECT COUNT(*) FROM transactions WHERE status = 'BORROWED'";
        String q3 = "SELECT COUNT(*) FROM transactions WHERE status = 'OVERDUE'";
        String q4 = "SELECT COUNT(*) FROM users WHERE role = 'STUDENT'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            try (ResultSet rs = stmt.executeQuery(q1)) { if (rs.next()) stats[0] = rs.getInt(1); }
            try (ResultSet rs = stmt.executeQuery(q2)) { if (rs.next()) stats[1] = rs.getInt(1); }
            try (ResultSet rs = stmt.executeQuery(q3)) { if (rs.next()) stats[2] = rs.getInt(1); }
            try (ResultSet rs = stmt.executeQuery(q4)) { if (rs.next()) stats[3] = rs.getInt(1); }
            
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }

    public List<Book> getAllBooks() {
    List<Book> list = new ArrayList<>();
    String query = "SELECT b.book_id, b.isbn, t.title_name, a.author_name, p.publisher_name, c.category_name, b.publication_year, b.quantity " +
                   "FROM books b " +
                   "JOIN titles t ON b.title_id = t.title_id " +
                   "LEFT JOIN authors a ON b.author_id = a.author_id " +
                   "LEFT JOIN publishers p ON b.publisher_id = p.publisher_id " +
                   "LEFT JOIN categories c ON b.category_id = c.category_id";
                   
    try (Connection conn = DatabaseConnection.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {
        
        while (rs.next()) {
            Book book = new Book();
            book.setBookId(rs.getInt("book_id"));
            book.setIsbn(rs.getString("isbn"));
            book.setTitle(rs.getString("title_name"));       // Text name from titles table
            book.setAuthor(rs.getString("author_name"));     // Text name from authors table
            book.setPublisher(rs.getString("publisher_name")); // Text name from publishers table
            book.setCategoryName(rs.getString("category_name"));
            book.setPublicationYear(rs.getInt("publication_year"));
            book.setQuantity(rs.getInt("quantity"));
            list.add(book);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return list;
}

    public boolean addBook(Book b) {
        String sql = "INSERT INTO books (isbn, title, author, publisher, publication_year, category_id, quantity) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, b.getIsbn());
            stmt.setString(2, b.getTitle());
            stmt.setString(3, b.getAuthor());
            stmt.setString(4, b.getPublisher());
            stmt.setInt(5, b.getPublicationYear());
            stmt.setInt(6, b.getCategoryId());
            stmt.setInt(7, b.getQuantity());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateBook(Book b) {
        String sql = "UPDATE books SET isbn=?, title=?, author=?, publisher=?, publication_year=?, category_id=?, quantity=? WHERE book_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, b.getIsbn());
            stmt.setString(2, b.getTitle());
            stmt.setString(3, b.getAuthor());
            stmt.setString(4, b.getPublisher());
            stmt.setInt(5, b.getPublicationYear());
            stmt.setInt(6, b.getCategoryId());
            stmt.setInt(7, b.getQuantity());
            stmt.setInt(8, b.getBookId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deleteBook(int id) {
        String sql = "DELETE FROM books WHERE book_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}