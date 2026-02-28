package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class TransactionDao {

    public static void logTransaction(int userId, String type, String description, int creditsChange, String itemsChange) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                "INSERT INTO users_transactions (user_id, type, description, credits_change, items_change) VALUES (?, ?, ?, ?, ?)",
                sqlConnection
            );
            preparedStatement.setInt(1, userId);
            preparedStatement.setString(2, type);
            preparedStatement.setString(3, description);
            preparedStatement.setInt(4, creditsChange);
            preparedStatement.setString(5, itemsChange);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }
}
