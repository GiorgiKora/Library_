package org.example;

import org.example.db.DatabaseConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

@WebServlet("/books")
public class BookServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM books")) {

            StringBuilder html = new StringBuilder("<html><body><h1>Books</h1><ul>");
            while (rs.next()) {
                html.append("<li>").append(rs.getString("title")).append(" by ").append(rs.getString("author")).append(" (").append(rs.getString("code")).append(") </li>");
            }
            html.append("</ul></body></html>");
            response.getWriter().write(html.toString());

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String title = request.getParameter("title");
        String author = request.getParameter("author");
        String code = request.getParameter("code");
        String quantityStr = request.getParameter("quantity");

        if (title == null || author == null || code == null || quantityStr == null || title.isEmpty() || author.isEmpty() || code.isEmpty() || quantityStr.isEmpty()) {
            response.setStatus(422);
            response.getWriter().write("All fields are required.");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            response.setStatus(422);
            response.getWriter().write("Invalid quantity.");
            return;
        }

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement checkStmt = connection.prepareStatement("SELECT 1 FROM books WHERE code = ?")) {
            checkStmt.setString(1, code);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    response.setStatus(422);
                    response.getWriter().write("Error: Book with this code already exists.");
                    return;
                }
            }

            try (PreparedStatement pstmt = connection.prepareStatement("INSERT INTO books (code, title, author) VALUES (?, ?, ?)")) {
                pstmt.setString(1, code);
                pstmt.setString(2, title);
                pstmt.setString(3, author);
                pstmt.executeUpdate();
                response.setStatus(201);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String bookCode = request.getPathInfo().substring(1);
        String title = request.getParameter("title");
        String author = request.getParameter("author");

        if (title == null || author == null || title.isEmpty() || author.isEmpty()) {
            response.setStatus(422);
            response.getWriter().write("Title and Author fields are required.");
            return;
        }

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement("UPDATE books SET title = ?, author = ? WHERE code = ?")) {
            pstmt.setString(1, title);
            pstmt.setString(2, author);
            pstmt.setString(3, bookCode);
            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                response.setStatus(200);
            } else {
                response.setStatus(404);
                response.getWriter().write("Error: Book not found for update.");
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String bookCode = request.getPathInfo().substring(1);

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement("DELETE FROM books WHERE code = ?")) {
            pstmt.setString(1, bookCode);
            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted > 0) {
                response.setStatus(204);
            } else {
                response.setStatus(404);
                response.getWriter().write("Error: Book not found for deletion.");
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }
}