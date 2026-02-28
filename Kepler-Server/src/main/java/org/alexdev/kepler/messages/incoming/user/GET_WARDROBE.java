package org.alexdev.kepler.messages.incoming.user;

import org.alexdev.kepler.dao.mysql.WardrobeDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.outgoing.user.WARDROBE;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

import java.util.Map;

public class GET_WARDROBE implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        if (!player.isLoggedIn()) {
            return;
        }

        Map<Integer, String[]> wardrobe = WardrobeDao.getWardrobe(player.getDetails().getId());
        player.send(new WARDROBE(wardrobe));
    }
}
