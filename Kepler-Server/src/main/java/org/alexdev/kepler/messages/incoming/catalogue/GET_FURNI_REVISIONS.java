package org.alexdev.kepler.messages.incoming.catalogue;

import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.outgoing.catalogue.FURNI_REVISIONS;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

public class GET_FURNI_REVISIONS implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        player.send(new FURNI_REVISIONS());
    }
}
