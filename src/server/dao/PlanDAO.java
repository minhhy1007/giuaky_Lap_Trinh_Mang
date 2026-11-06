package server.dao;

import server.db.DBConnection;
import server.model.Plan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bảng `plans`.
 */
public class PlanDAO {

    public List<Plan> getAllPlans() {
        List<Plan> list = new ArrayList<>();
        String sql = "SELECT plan_id, plan_name, max_quota_bytes, price, duration_days, description, is_active " +
                     "FROM plans WHERE is_active = TRUE ORDER BY plan_id ASC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[PlanDAO] Error getAllPlans: " + e.getMessage());
        }
        return list;
    }

    public Plan getPlanById(int planId) {
        String sql = "SELECT plan_id, plan_name, max_quota_bytes, price, duration_days, description, is_active " +
                     "FROM plans WHERE plan_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PlanDAO] Error getPlanById: " + e.getMessage());
        }
        return null;
    }

    public Plan getPlanByName(String planName) {
        String sql = "SELECT plan_id, plan_name, max_quota_bytes, price, duration_days, description, is_active " +
                     "FROM plans WHERE LOWER(plan_name) = LOWER(?)";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, planName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PlanDAO] Error getPlanByName: " + e.getMessage());
        }
        return null;
    }

    private Plan mapResultSet(ResultSet rs) throws SQLException {
        return new Plan(
                rs.getInt("plan_id"),
                rs.getString("plan_name"),
                rs.getLong("max_quota_bytes"),
                rs.getDouble("price"),
                rs.getInt("duration_days"),
                rs.getString("description"),
                rs.getBoolean("is_active")
        );
    }
}

