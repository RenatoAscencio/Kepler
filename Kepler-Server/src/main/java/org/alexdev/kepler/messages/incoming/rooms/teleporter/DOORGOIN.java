package org.alexdev.kepler.messages.incoming.rooms.teleporter;

import org.alexdev.kepler.dao.mysql.ItemDao;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.base.ItemBehaviour;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.messages.outgoing.rooms.items.BROADCAST_TELEPORTER;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.apache.commons.lang3.StringUtils;

public class DOORGOIN implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        String contents = reader.contents();

        if (!StringUtils.isNumeric(contents) || player.getRoomUser().getRoom() == null) {
            return;
        }

        int itemId = Integer.parseInt(contents);

        if (player.getRoomUser().getAuthenticateTelporterId() == itemId) {
            Item item = player.getRoomUser().getRoom().getItemManager().getById(itemId);

            if (item == null || !item.hasBehaviour(ItemBehaviour.TELEPORTER)) {
                return;
            }

            player.getRoomUser().getRoom().send(new BROADCAST_TELEPORTER(item, player.getDetails().getName(), false));
        }
    }
}
