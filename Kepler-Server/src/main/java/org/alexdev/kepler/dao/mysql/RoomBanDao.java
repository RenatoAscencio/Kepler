package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RoomBanDao {

    public static boolean isRoomBanned(int roomId, int userId) {
        boolean banned = false;

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                "SELECT id FROM rooms_bans WHERE room_id = ? AND user_id = ? AND (expires_at > NOW() OR expires_at IS NULL)",
                sqlConnection
            );
            preparedStatement.setInt(1, roomId);
            preparedStatement.setInt(2, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                banned = true;
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return banned;
    }

    public static void addRoomBan(int roomId, int userId, int durationMinutes) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                "INSERT INTO rooms_bans (room_id, user_id, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE)) ON DUPLICATE KEY UPDATE expires_at = DATE_ADD(NOW(), INTERVAL ? MINUTE)",
                sqlConnection
            );
            preparedStatement.setInt(1, roomId);
            preparedStatement.setInt(2, userId);
            preparedStatement.setInt(3, durationMinutes);
            preparedStatement.setInt(4, durationMinutes);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    public static void removeRoomBan(int roomId, int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                "DELETE FROM rooms_bans WHERE room_id = ? AND user_id = ?", sqlConnection
            );
            preparedStatement.setInt(1, roomId);
            preparedStatement.setInt(2, userId);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }
}
