package org.alexdev.kepler.messages.incoming.navigator;

import org.alexdev.kepler.game.navigator.NavigatorManager;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

public class NAVIGATE implements MessageEvent {

    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        boolean hideFull = reader.readInt() == 1;
        int categoryId = reader.readInt();

        boolean wasFollow = false;
        if (categoryId >= RoomManager.PUBLIC_ROOM_OFFSET) { // Public room follow, there should not any categories with an ID of 1000 or over... lol
            Room room = RoomManager.getInstance().getRoomById(categoryId - RoomManager.PUBLIC_ROOM_OFFSET);

            if (room != null) {
                wasFollow = true;
                categoryId = room.getCategory().getId();
            }
        }

        if (!NavigatorManager.getInstance().sendCategoryView(player, categoryId, hideFull, true)) {
            return;
        }

        if (wasFollow && player.getMessenger().getFollowed() != null) {
            player.getMessenger().getFollowed().forward(player, false);
            player.getMessenger().hasFollowed(null);
        }
    }
}
