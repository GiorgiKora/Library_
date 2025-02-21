package org.example;

import org.example.db.DatabaseConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/members")
public class MemberServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM members")) {

            List<Member> members = new ArrayList<>();
            while (rs.next()) {
                Member member = new Member(rs.getInt("id"), rs.getString("name"), rs.getString("email"));
                members.add(member);
            }

            // Convert members list to JSON (using a library like Jackson or Gson)
            String membersJson = convertToJson(members); // Implement convertToJson()

            response.getWriter().write(membersJson);

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("Database error"); // Or a more specific message
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");

        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement("INSERT INTO members (name, email) VALUES (?, ?)")) {

            if (isDuplicateEmail(connection, email)) {
                response.setStatus(422);
                response.getWriter().write("Error: Member with this email already exists.");
                return;
            }

            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.executeUpdate();
            response.setStatus(201); // 201 Created

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("Database error");
        }
    }


    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            response.setStatus(400); // Bad Request
            response.getWriter().write("Member ID is missing.");
            return;
        }

        try {
            int memberId = Integer.parseInt(pathInfo.substring(1));
            String name = request.getParameter("name");
            String email = request.getParameter("email");

            try (Connection connection = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = connection.prepareStatement("UPDATE members SET name = ?, email = ? WHERE id = ?")) {

                if (isDuplicateEmailForUpdate(connection, email, memberId)) {
                    response.setStatus(422);
                    response.getWriter().write("Error: Member with this email already exists.");
                    return;
                }

                pstmt.setString(1, name);
                pstmt.setString(2, email);
                pstmt.setInt(3, memberId);

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated > 0) {
                    response.setStatus(200); // OK
                } else {
                    response.setStatus(404); // Not Found
                    response.getWriter().write("Member not found.");
                }

            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
                response.setStatus(500);
                response.getWriter().write("Database error");
            }

        } catch (NumberFormatException e) {
            response.setStatus(400); // Bad Request
            response.getWriter().write("Invalid member ID.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            response.setStatus(400); // Bad Request
            response.getWriter().write("Member ID is missing.");
            return;
        }

        try {
            int memberId = Integer.parseInt(pathInfo.substring(1));

            try (Connection connection = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement pstmt = connection.prepareStatement("DELETE FROM members WHERE id = ?")) {

                pstmt.setInt(1, memberId);
                int rowsDeleted = pstmt.executeUpdate();

                if (rowsDeleted > 0) {
                    response.setStatus(204); // No Content
                } else {
                    response.setStatus(404); // Not Found
                    response.getWriter().write("Member not found.");
                }

            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
                response.setStatus(500);
                response.getWriter().write("Database error");
            }

        } catch (NumberFormatException e) {
            response.setStatus(400); // Bad Request
            response.getWriter().write("Invalid member ID.");
        }
    }


    private boolean isDuplicateEmail(Connection connection, String email) throws SQLException {
        try (PreparedStatement checkStmt = connection.prepareStatement("SELECT 1 FROM members WHERE email = ?")) {
            checkStmt.setString(1, email);
            try (ResultSet rs = checkStmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isDuplicateEmailForUpdate(Connection connection, String email, int memberId) throws SQLException {
        try (PreparedStatement checkStmt = connection.prepareStatement("SELECT 1 FROM members WHERE email = ? AND id != ?")) {
            checkStmt.setString(1, email);
            checkStmt.setInt(2, memberId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Helper method to convert Member objects to JSON
    private String convertToJson(List<Member> members) {
        // Use a library like Jackson or Gson to convert the list to JSON
        // Example using Jackson:
        // ObjectMapper objectMapper = new ObjectMapper();
        // return objectMapper.writeValueAsString(members);
        return ""; // Replace with actual JSON conversion
    }

    // Inner class to represent a Member
    private static class Member {
        private int id;
        private String name;
        private String email;

        public Member(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        // Getters for id, name, and email
        public int getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
    }
}