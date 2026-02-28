package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.games.GameSpawn;
import org.alexdev.kepler.game.games.enums.GameType;
import org.alexdev.kepler.game.games.battleball.BattleBallMap;
import org.alexdev.kepler.game.games.player.GameRank;
import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.game.room.models.RoomModel;

import org.alexdev.kepler.game.games.history.GameHistory;
import org.alexdev.kepler.game.games.history.GameHistoryData;
import org.alexdev.kepler.game.games.history.GameHistoryPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameDao {
    public static List<GameRank> getRanks() {
        List<GameRank> ranks = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM games_ranks", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                ranks.add(new GameRank(resultSet.getInt("id"), resultSet.getString("type"),
                        resultSet.getString("title"), resultSet.getInt("min_points"),
                        resultSet.getInt("max_points")));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return ranks;
    }

    public static List<RoomModel> getGameMaps() {
        List<RoomModel> maps = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM games_maps", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String modelName = "bb" + "_arena_" + resultSet.getInt("map_id");

                if (!resultSet.getString("game_type").equals("battleball")) {
                    modelName = "ss_arena_" + resultSet.getInt("map_id");
                }

                maps.add(new RoomModel(modelName, modelName, Integer.MAX_VALUE, Integer.MAX_VALUE, Double.MAX_VALUE, 0, resultSet.getString("heightmap"), null));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }


        return maps;
    }

    public static List<BattleBallMap> getBattleballTileMaps() {
        List<BattleBallMap> maps = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM games_maps WHERE game_type = 'battleball'", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                maps.add(new BattleBallMap(resultSet.getInt("map_id"), GameType.BATTLEBALL, resultSet.getString("tile_map")));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }


        return maps;
    }

    public static List<GameSpawn> getGameSpawns() {
        List<GameSpawn> spawns = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM games_player_spawns", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                spawns.add(new GameSpawn(resultSet.getInt("team_id"),  resultSet.getInt("map_id"), resultSet.getString("type"),
                        resultSet.getInt("x"), resultSet.getInt("y"), resultSet.getInt("rotation")));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }


        return spawns;
    }

    /**
     * Atomically increase tickets.
     *
     * @param details the player details
     */
    public static void increasePoints(PlayerDetails details, GameType type, int amount) {
        String column = type.name().toLowerCase() + "_points";

        Connection conn = null;
        PreparedStatement updateQuery = null;
        PreparedStatement fetchQuery = null;
        ResultSet row = null;

        try {
            conn = Storage.getStorage().getConnection();

            // We disable autocommit to make sure the following queries share the same atomic transaction
            conn.setAutoCommit(false);

            // Increase credits
            updateQuery = Storage.getStorage().prepare("UPDATE users SET " + column + " = " + column + " + ? WHERE id = ?", conn);
            updateQuery.setInt(1, amount);
            updateQuery.setInt(2, details.getId());
            updateQuery.execute();

            // Fetch increased amount
            fetchQuery = Storage.getStorage().prepare("SELECT " + column + " FROM users WHERE id = ?", conn);
            fetchQuery.setInt(1, details.getId());
            row = fetchQuery.executeQuery();

            // Commit these queries
            conn.commit();

            // Set amount
            if (row != null && row.next()) {
                int updatedAmount = row.getInt(column);

                if (type == GameType.BATTLEBALL) {
                    details.setBattleballPoints(updatedAmount);
                }

                if (type == GameType.SNOWSTORM) {
                    details.setSnowStormPoints(updatedAmount);
                }
            }

        } catch (Exception e) {
            try {
                // Rollback these queries
                conn.rollback();
            } catch(SQLException re) {
                Storage.logError(re);
            }

            Storage.logError(e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ce) {
                Storage.logError(ce);
            }

            Storage.closeSilently(row);
            Storage.closeSilently(updateQuery);
            Storage.closeSilently(fetchQuery);
            Storage.closeSilently(conn);
        }
    }

    /**
     * Save a completed game history entry and its player data to the database.
     *
     * @param history the game history to persist
     */
    public static void saveGameHistory(GameHistory history) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                    "INSERT INTO game_history (game_type, name, game_creator, map_id, winning_team, winning_team_score, extra_data, played_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())", sqlConnection);

            preparedStatement.setString(1, history.getGameType().name().toLowerCase());
            preparedStatement.setString(2, history.getName());
            preparedStatement.setString(3, history.getGameCreator());
            preparedStatement.setInt(4, history.getMapId());
            preparedStatement.setInt(5, history.getWinningTeam());
            preparedStatement.setInt(6, history.getWinningTeamScore());
            preparedStatement.setString(7, history.getExtraData());
            preparedStatement.executeUpdate();

            resultSet = preparedStatement.getGeneratedKeys();

            if (resultSet != null && resultSet.next()) {
                int historyId = resultSet.getInt(1);
                saveGameHistoryPlayers(historyId, history.getHistoryData());
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Save all player entries for a game history record.
     *
     * @param historyId the generated game_history id
     * @param data the game history data containing player information
     */
    private static void saveGameHistoryPlayers(int historyId, GameHistoryData data) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();

            for (Map.Entry<Integer, List<GameHistoryPlayer>> entry : data.getTeamData().entrySet()) {
                for (GameHistoryPlayer player : entry.getValue()) {
                    preparedStatement = Storage.getStorage().prepare(
                            "INSERT INTO game_history_players (history_id, user_id, team_id, score) VALUES (?, ?, ?, ?)", sqlConnection);

                    preparedStatement.setInt(1, historyId);
                    preparedStatement.setInt(2, player.getUserId());
                    preparedStatement.setInt(3, player.getTeamId());
                    preparedStatement.setInt(4, player.getScore());
                    preparedStatement.executeUpdate();

                    Storage.closeSilently(preparedStatement);
                    preparedStatement = null;
                }
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Get leaderboard data for a specific game type, aggregated by player.
     *
     * @param gameType the game type string (e.g. "battleball", "snowstorm")
     * @param limit maximum number of results to return
     * @return list of maps with keys: user_id, total_score, games_played, wins
     */
    public static List<Map<String, Object>> getLeaderboard(String gameType, int limit) {
        List<Map<String, Object>> leaderboard = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                    "SELECT ghp.user_id, SUM(ghp.score) as total_score, COUNT(*) as games_played, " +
                    "SUM(CASE WHEN ghp.team_id = gh.winning_team THEN 1 ELSE 0 END) as wins " +
                    "FROM game_history_players ghp " +
                    "JOIN game_history gh ON ghp.history_id = gh.id " +
                    "WHERE gh.game_type = ? " +
                    "GROUP BY ghp.user_id ORDER BY total_score DESC LIMIT ?", sqlConnection);

            preparedStatement.setString(1, gameType);
            preparedStatement.setInt(2, limit);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("user_id", resultSet.getInt("user_id"));
                entry.put("total_score", resultSet.getLong("total_score"));
                entry.put("games_played", resultSet.getInt("games_played"));
                entry.put("wins", resultSet.getInt("wins"));
                leaderboard.add(entry);
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return leaderboard;
    }
}
