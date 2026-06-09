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
        String query = "SELECT b.book_id, b.isbn, b.title, a.author_name, p.publisher_name, c.category_name, b.publication_year, b.quantity " +
                       "FROM books b " +
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
                book.setTitle(rs.getString("title"));     
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
        try (Connection conn = DatabaseConnection.getConnection()) {
            
            if (b.getAuthor() != null && !b.getAuthor().trim().isEmpty()) {
                String insertAuthor = "INSERT IGNORE INTO authors (author_name) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertAuthor)) {
                    stmt.setString(1, b.getAuthor().trim());
                    stmt.executeUpdate();
                }
            }
            
            if (b.getPublisher() != null && !b.getPublisher().trim().isEmpty()) {
                String insertPublisher = "INSERT IGNORE INTO publishers (publisher_name) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertPublisher)) {
                    stmt.setString(1, b.getPublisher().trim());
                    stmt.executeUpdate();
                }
            }

            if (b.getCategoryName() != null && !b.getCategoryName().trim().isEmpty()) {
                String insertCategory = "INSERT IGNORE INTO categories (category_name) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertCategory)) {
                    stmt.setString(1, b.getCategoryName().trim()); 
                    stmt.executeUpdate();
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        String sql = "INSERT INTO books (isbn, title, author_id, publisher_id, category_id, publication_year, quantity) " +
                     "VALUES (?, ?, " +
                     "(SELECT author_id FROM authors WHERE author_name = ?), " +
                     "(SELECT publisher_id FROM publishers WHERE publisher_name = ?), " +
                     "(SELECT category_id FROM categories WHERE category_name = ?), " + 
                     "?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, b.getIsbn().trim());
            stmt.setString(2, b.getTitle().trim());        
            stmt.setString(3, b.getAuthor().trim());       
            stmt.setString(4, b.getPublisher().trim());    
            stmt.setString(5, b.getCategoryName().trim()); 
            stmt.setInt(6, b.getPublicationYear());
            stmt.setInt(7, b.getQuantity());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return false; 
        }
    }

    public boolean updateBook(Book b) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (b.getAuthor() != null && !b.getAuthor().trim().isEmpty()) {
                String sqlAuth = "INSERT IGNORE INTO authors (author_name) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlAuth)) { stmt.setString(1, b.getAuthor().trim()); stmt.executeUpdate(); }
            }
            if (b.getPublisher() != null && !b.getPublisher().trim().isEmpty()) {
                String sqlPub = "INSERT IGNORE INTO publishers (publisher_name) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlPub)) { stmt.setString(1, b.getPublisher().trim()); stmt.executeUpdate(); }
            }
            if (b.getCategoryName() != null && !b.getCategoryName().trim().isEmpty()) {
                String sqlCat = "INSERT IGNORE INTO categories (category_name) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlCat)) { stmt.setString(1, b.getCategoryName().trim()); stmt.executeUpdate(); }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        String sql = "UPDATE books SET isbn=?, title=?, " +
                     "author_id=(SELECT author_id FROM authors WHERE author_name=?), " +
                     "publisher_id=(SELECT publisher_id FROM publishers WHERE publisher_name=?), " +
                     "category_id=(SELECT category_id FROM categories WHERE category_name=?), " +
                     "publication_year=?, quantity=? WHERE book_id=?";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, b.getIsbn().trim());
            stmt.setString(2, b.getTitle().trim());
            stmt.setString(3, b.getAuthor().trim());
            stmt.setString(4, b.getPublisher().trim());
            stmt.setString(5, b.getCategoryName().trim());
            stmt.setInt(6, b.getPublicationYear());
            stmt.setInt(7, b.getQuantity());
            stmt.setInt(8, b.getBookId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return false; 
        }
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