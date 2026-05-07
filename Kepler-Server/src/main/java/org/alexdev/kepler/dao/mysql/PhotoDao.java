package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.item.Photo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PhotoDao {
    private static final String INSERT_SQL =
            "INSERT INTO items_photos (photo_id, photo_user_id, timestamp, photo_data, photo_checksum) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_SQL =
            "SELECT photo_id, photo_checksum, photo_data, timestamp FROM items_photos WHERE photo_id = ?";

    public static void addPhoto(int photoId, int userId, long timestamp, byte[] photo, int checksum) throws SQLException {
        Connection conn = null;

        try {
            conn = Storage.getStorage().getConnection();

            if (conn == null) {
                throw new SQLException("Could not insert photo " + photoId + " because the database connection is unavailable");
            }

            addPhoto(conn, photoId, userId, timestamp, photo, checksum);
        } catch (SQLException e) {
            Storage.logError(e);
            throw e;
        } finally {
            Storage.closeSilently(conn);
        }
    }

    /**
     * Insert a photo blob using a caller-supplied Connection. Visible for tests
     * so the pool-managed entry point above can stay thin while still routing
     * through the same SQL on production.
     *
     * The column is LONGBLOB and v14 BINDATA payloads can exceed 16 MB, so we
     * write the bytes via PreparedStatement.setBytes(int, byte[]) rather than
     * Connection.createBlob() + setBlob(). The intermediate Blob object adds
     * an extra driver round-trip and has historically been the source of size
     * and chunk-handling regressions on the MariaDB connector for very large
     * binary payloads.
     */
    static void addPhoto(Connection conn, int photoId, int userId, long timestamp, byte[] photo, int checksum) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setInt(1, photoId);
            ps.setInt(2, userId);
            ps.setLong(3, timestamp);
            ps.setBytes(4, photo);
            ps.setInt(5, checksum);
            ps.execute();
        }
    }

    public static Photo getPhoto(int photoId) throws SQLException {
        Connection conn = null;

        try {
            conn = Storage.getStorage().getConnection();

            if (conn == null) {
                throw new SQLException("Could not load photo " + photoId + " because the database connection is unavailable");
            }

            return getPhoto(conn, photoId);
        } catch (SQLException e) {
            Storage.logError(e);
            throw e;
        } finally {
            Storage.closeSilently(conn);
        }
    }

    static Photo getPhoto(Connection conn, int photoId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_SQL)) {
            ps.setInt(1, photoId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Photo(
                            rs.getInt("photo_id"),
                            rs.getInt("photo_checksum"),
                            rs.getBytes("photo_data"),
                            rs.getLong("timestamp"));
                }
            }
        }

        return null;
    }
}
