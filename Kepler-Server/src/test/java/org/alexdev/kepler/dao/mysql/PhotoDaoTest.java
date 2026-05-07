package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.game.item.Photo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Drives PhotoDao against an H2 in-memory database in MariaDB compatibility
 * mode. The H2 schema mirrors the production items_photos columns and uses
 * VARBINARY in place of LONGBLOB (H2's BLOB has subtle JDBC differences but
 * VARBINARY round-trips through setBytes/getBytes the same way the MariaDB
 * driver does).
 */
class PhotoDaoTest {
    private static final String JDBC_URL =
            "jdbc:h2:mem:photo;MODE=MariaDB;DB_CLOSE_DELAY=-1";

    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DriverManager.getConnection(JDBC_URL, "sa", "");

        try (Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS items_photos");
            st.execute("""
                    CREATE TABLE items_photos (
                        photo_id INT NOT NULL PRIMARY KEY,
                        photo_user_id BIGINT NOT NULL,
                        timestamp BIGINT NOT NULL,
                        photo_data VARBINARY(67108864) NOT NULL,
                        photo_checksum INT NOT NULL
                    )""");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    @Test
    void addPhotoThenGetPhotoRoundTripsAllFields() throws SQLException {
        byte[] payload = bytes(2048);

        PhotoDao.addPhoto(conn, 42, 1234, 1_700_000_000L, payload, 0xDEADBEEF);
        Photo loaded = PhotoDao.getPhoto(conn, 42);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getId()).isEqualTo(42);
        assertThat(loaded.getChecksum()).isEqualTo(0xDEADBEEF);
        assertThat(loaded.getTime()).isEqualTo(1_700_000_000L);
        assertThat(loaded.getData()).isEqualTo(payload);
    }

    @Test
    void getPhotoReturnsNullWhenMissing() throws SQLException {
        assertThat(PhotoDao.getPhoto(conn, 999)).isNull();
    }

    /**
     * Camera v14 BINDATA payloads can exceed the 1 MB legacy MUS limit; the
     * dc4543c server change raised the MUS frame ceiling to 32 MB and the
     * column is LONGBLOB. Verify that PhotoDao does not lose bytes under that
     * regime.
     */
    @Test
    void addPhotoHandlesMultiMegabytePayload() throws SQLException {
        byte[] payload = bytes(4 * 1024 * 1024);

        PhotoDao.addPhoto(conn, 1, 7, 0L, payload, 1);
        Photo loaded = PhotoDao.getPhoto(conn, 1);

        assertThat(loaded.getData()).hasSize(payload.length);
        assertThat(loaded.getData()).isEqualTo(payload);
    }

    @Test
    void addPhotoPreservesByteIntegrityAcrossFullRange() throws SQLException {
        // Every possible byte value, twice, to detect any signed-byte bugs.
        byte[] payload = new byte[512];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }

        PhotoDao.addPhoto(conn, 5, 1, 0L, payload, 0);
        Photo loaded = PhotoDao.getPhoto(conn, 5);

        assertThat(loaded.getData()).isEqualTo(payload);
    }

    @Test
    void addPhotoRejectsDuplicatePrimaryKey() throws SQLException {
        byte[] payload = bytes(64);
        PhotoDao.addPhoto(conn, 7, 1, 0L, payload, 0);

        assertThatThrownBy(() -> PhotoDao.addPhoto(conn, 7, 1, 0L, payload, 0))
                .isInstanceOf(SQLException.class);
    }

    private static byte[] bytes(int size) {
        byte[] result = new byte[size];
        new Random(0xC0FFEE).nextBytes(result);
        return result;
    }
}
