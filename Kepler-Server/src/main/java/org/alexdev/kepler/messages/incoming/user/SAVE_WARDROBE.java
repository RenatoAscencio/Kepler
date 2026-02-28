package org.alexdev.kepler.messages.incoming.user;

import org.alexdev.kepler.dao.mysql.WardrobeDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

public class SAVE_WARDROBE implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        if (!player.isLoggedIn()) {
            return;
        }

        int slotNumber = reader.readBase64();

        if (slotNumber < 1 || slotNumber > 10) {
            return;  // Invalid slot
        }

        String figure = player.getDetails().getFigure();
        String sex = Character.toString(player.getDetails().getSex());

        WardrobeDao.saveSlot(player.getDetails().getId(), slotNumber, figure, sex);
    }
}
