package org.example;

import org.example.db.DatabaseConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

@WebServlet("/borrow")
public class BorrowingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT b.title, m.name, bor.borrow_date, bor.return_date " +
                     "FROM borrowings bor " +
                     "JOIN books b ON bor.book_code = b.code " +
                     "JOIN members m ON bor.member_id = m.id")) {

            StringBuilder html = new StringBuilder("<html><body><h1>Borrowings</h1><ul>");
            while (rs.next()) {
                html.append("<li>").append(rs.getString("title")).append(" - ").append(rs.getString("name"))
                        .append(" - ").append(rs.getDate("borrow_date")).append(" - ");
                Date returnDate = rs.getDate("return_date");
                if (returnDate != null) {
                    html.append(returnDate);
                } else {
                    html.append("Not Returned");
                }
                html.append("</li>");
            }
            html.append("</ul></body></html>");
            response.getWriter().write(html.toString());

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String bookCode = request.getParameter("book_code");
        int memberId = Integer.parseInt(request.getParameter("member_id"));

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement checkStmt = connection.prepareStatement("SELECT 1 FROM borrowings WHERE book_code = ? AND return_date IS NULL")) {

            checkStmt.setString(1, bookCode);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) { // Book is not available
                    response.setStatus(422);
                    response.getWriter().write("Error: Book is not available.");
                    return; // Stop processing
                }

                try (PreparedStatement pstmt = connection.prepareStatement("INSERT INTO borrowings (book_code, member_id, borrow_date) VALUES (?, ?, ?)")) {
                    pstmt.setString(1, bookCode);
                    pstmt.setInt(2, memberId);
                    pstmt.setDate(3, Date.valueOf(LocalDate.now()));
                    pstmt.executeUpdate();
                    response.setStatus(201);
                }

            }
        } catch (SQLException | ClassNotFoundException e) {
            response.setStatus(500);
            e.printStackTrace();
        }
    }


    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String bookCode = request.getParameter("book_code");

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement("UPDATE borrowings SET return_date = ? WHERE book_code = ? AND return_date IS NULL")) {

            pstmt.setDate(1, Date.valueOf(LocalDate.now()));
            pstmt.setString(2, bookCode);
            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                response.setStatus(200); // OK - Updated
            } else {
                response.setStatus(404); // Not Found - No book with that code for return
                response.getWriter().write("Error: Book not found for return or already returned.");
            }

        } catch (SQLException | ClassNotFoundException e) {
            response.setStatus(500);
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(405); // Method Not Allowed for /borrow
        response.getWriter().write("DELETE method is not allowed for /borrow endpoint.");
    }
}