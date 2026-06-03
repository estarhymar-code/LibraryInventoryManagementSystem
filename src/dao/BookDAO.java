package dao;

import config.DatabaseConnection;
import models.Book;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    public int[] getLibraryStatistics() {
        int[] stats = new int[4]; 
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
            book.setTitle(rs.getString("title_name"));       
            book.setAuthor(rs.getString("author_name"));     
            book.setPublisher(rs.getString("publisher_name")); 
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
    // PHASE 1: Ensure all lookup descriptions exist in their master tables first.
    // If the admin typed a brand new Category, Author, Publisher, or Title, this creates it instantly.
    try (Connection conn = DatabaseConnection.getConnection()) {
        
        // Ensure Title text exists in titles table
        String insertTitle = "INSERT IGNORE INTO titles (title_name) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertTitle)) {
            stmt.setString(1, b.getTitle());
            stmt.executeUpdate();
        }
        
        // Ensure Author text exists in authors table
        if (b.getAuthor() != null && !b.getAuthor().trim().isEmpty()) {
            String insertAuthor = "INSERT IGNORE INTO authors (author_name) VALUES (?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertAuthor)) {
                stmt.setString(1, b.getAuthor());
                stmt.executeUpdate();
            }
        }
        
        // Ensure Publisher text exists in publishers table
        if (b.getPublisher() != null && !b.getPublisher().trim().isEmpty()) {
            String insertPublisher = "INSERT IGNORE INTO publishers (publisher_name) VALUES (?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertPublisher)) {
                stmt.setString(1, b.getPublisher());
                stmt.executeUpdate();
            }
        }

        // Ensure Category text exists in categories table
        if (b.getCategoryName() != null && !b.getCategoryName().trim().isEmpty()) {
            String insertCategory = "INSERT IGNORE INTO categories (category_name) VALUES (?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertCategory)) {
                stmt.setString(1, b.getCategoryName()); 
                stmt.executeUpdate();
            }
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }

    // PHASE 2: Insert into the books table using nested SELECT subqueries to map ALL text names to IDs
    String sql = "INSERT INTO books (isbn, title_id, author_id, publisher_id, category_id, publication_year, quantity) " +
                 "VALUES (?, " +
                 "(SELECT title_id FROM titles WHERE title_name = ?), " +
                 "(SELECT author_id FROM authors WHERE author_name = ?), " +
                 "(SELECT publisher_id FROM publishers WHERE publisher_name = ?), " +
                 "(SELECT category_id FROM categories WHERE category_name = ?), " + 
                 "?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setString(1, b.getIsbn());
        stmt.setString(2, b.getTitle());        // Maps to title_id subquery
        stmt.setString(3, b.getAuthor());       // Maps to author_id subquery
        stmt.setString(4, b.getPublisher());    // Maps to publisher_id subquery
        stmt.setString(5, b.getCategoryName()); // Maps to category_id subquery (e.g., "Computer Science")
        stmt.setInt(6, b.getPublicationYear());
        stmt.setInt(7, b.getQuantity());

        return stmt.executeUpdate() > 0;
    } catch (SQLException e) { 
        e.printStackTrace(); 
        return false; 
    }
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