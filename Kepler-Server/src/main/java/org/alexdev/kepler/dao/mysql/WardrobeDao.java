package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class WardrobeDao {

    public static Map<Integer, String[]> getWardrobe(int userId) {
        Map<Integer, String[]> wardrobe = new LinkedHashMap<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                "SELECT slot_number, figure, sex FROM users_wardrobes WHERE user_id = ? ORDER BY slot_number ASC",
                sqlConnection
            );
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                wardrobe.put(
                    resultSet.getInt("slot_number"),
                    new String[]{ resultSet.getString("figure"), resultSet.getString("sex") }
                );
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return wardrobe;
    }

    public static void saveSlot(int userId, int slotNumber, String figure, String sex) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                "INSERT INTO users_wardrobes (user_id, slot_number, figure, sex) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE figure = ?, sex = ?",
                sqlConnection
            );
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, slotNumber);
            preparedStatement.setString(3, figure);
            preparedStatement.setString(4, sex);
            preparedStatement.setString(5, figure);
            preparedStatement.setString(6, sex);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }
}
