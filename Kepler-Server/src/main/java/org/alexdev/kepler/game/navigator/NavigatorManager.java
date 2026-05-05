package org.alexdev.kepler.game.navigator;

import org.alexdev.kepler.dao.mysql.NavigatorDao;
import org.alexdev.kepler.dao.mysql.RoomDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerManager;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.messages.outgoing.navigator.NAVNODEINFO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class NavigatorManager {
    private static NavigatorManager instance;
    private final HashMap<Integer, NavigatorCategory> categoryMap;

    private NavigatorManager() {
        this.categoryMap = NavigatorDao.getCategories();
    }

    /**
     * Get all categories by the parent id.
     *
     * @param parentId the parent id of the categories
     * @return the list of categories
     */
    public List<NavigatorCategory> getCategoriesByParentId(int parentId) {
        List<NavigatorCategory> categories =  new ArrayList<>();

        for (NavigatorCategory category : this.categoryMap.values()) {
            if (category.getParentId() == parentId) {
                categories.add(category);
            }
        }

        return categories;
    }

    /**
     * Get the {@link NavigatorCategory} by id
     *
     * @param categoryId the id of the category
     * @return the category instance
     */
    public NavigatorCategory getCategoryById(int categoryId) {
        if (this.categoryMap.containsKey(categoryId)) {
            return this.categoryMap.get(categoryId);
        }

        return null;
    }

    /**
     * Get the map of navigator categories
     *
     * @return the list of categories
     */
    public HashMap<Integer, NavigatorCategory> getCategories() {
        return this.categoryMap;
    }

    public boolean sendCategoryView(Player player, int categoryId, boolean hideFull, boolean rememberView) {
        NavigatorCategory category = this.getCategoryById(categoryId);

        if (category == null) {
            return false;
        }

        int rank = player.getDetails().getRank().getRankId();

        if (category.getMinimumRoleAccess().getRankId() > rank) {
            return false;
        }

        List<NavigatorCategory> subCategories = this.getCategoriesByParentId(category.getId());
        subCategories.sort(Comparator.comparingDouble(NavigatorCategory::getCurrentVisitors).reversed());

        List<Room> rooms = new ArrayList<>();
        int categoryCurrentVisitors = category.getCurrentVisitors();
        int categoryMaxVisitors = category.getMaxVisitors();

        if (category.isPublicSpaces()) {
            for (Room room : RoomManager.getInstance().replaceQueryRooms(RoomDao.getRoomsByUserId(0))) {
                if (room.getData().isNavigatorHide()) {
                    continue;
                }

                if (room.getData().getCategoryId() != category.getId()) {
                    continue;
                }

                if (hideFull && room.getData().getVisitorsNow() >= room.getData().getVisitorsMax()) {
                    continue;
                }

                rooms.add(room);
            }
        } else {
            for (Room room : RoomManager.getInstance().replaceQueryRooms(NavigatorDao.getRecentRooms(30, category.getId()))) {
                if (room.getData().getCategoryId() != category.getId()) {
                    continue;
                }

                if (hideFull && room.getData().getVisitorsNow() >= room.getData().getVisitorsMax()) {
                    continue;
                }

                rooms.add(room);
            }
        }

        RoomManager.getInstance().sortRooms(rooms);
        RoomManager.getInstance().ratingSantiyCheck(rooms);

        if (rememberView) {
            player.setLastNavigatorView(category.getId(), hideFull);
        }

        player.send(new NAVNODEINFO(player, category, rooms, hideFull, subCategories, categoryCurrentVisitors, categoryMaxVisitors, rank));
        return true;
    }

    public void refreshOpenNavigatorViewsForRoom(Room room) {
        if (room == null) {
            return;
        }

        for (Player player : PlayerManager.getInstance().getPlayers()) {
            int categoryId = player.getLastNavigatorCategoryId();

            if (categoryId < 0) {
                continue;
            }

            if (!this.navigatorViewCanContainRoom(categoryId, room)) {
                continue;
            }

            this.sendCategoryView(player, categoryId, player.getLastNavigatorHideFull(), false);
        }
    }

    private boolean navigatorViewCanContainRoom(int categoryId, Room room) {
        if (room.getData().getCategoryId() == categoryId) {
            return true;
        }

        for (NavigatorCategory subCategory : this.getCategoriesByParentId(categoryId)) {
            if (subCategory.getId() == room.getData().getCategoryId()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get instance of {@link NavigatorManager}
     *
     * @return the manager instance
     */
    public static NavigatorManager getInstance() {
        if (instance == null) {
            instance = new NavigatorManager();
        }

        return instance;
    }

    /**
     * Resets the navigator manager singleton, reloading categories from the database.
     */
    public static void reset() {
        instance = null;
        NavigatorManager.getInstance();
    }
}
