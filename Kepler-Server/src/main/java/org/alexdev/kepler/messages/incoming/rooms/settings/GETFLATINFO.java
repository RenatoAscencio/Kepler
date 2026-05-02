package org.alexdev.kepler.messages.incoming.rooms.settings;

import org.alexdev.kepler.dao.mysql.RoomDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.messages.outgoing.rooms.settings.FLATINFO;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.apache.commons.lang3.StringUtils;

public class GETFLATINFO implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        String contents = reader.contents();

        if (!StringUtils.isNumeric(contents)) {
            return;
        }

        int roomId = Integer.parseInt(contents);

        Room room = RoomManager.getInstance().getRoomById(roomId);

        if (room == null) {
            return;
        }

        player.send(new FLATINFO(player, room));
    }
}
