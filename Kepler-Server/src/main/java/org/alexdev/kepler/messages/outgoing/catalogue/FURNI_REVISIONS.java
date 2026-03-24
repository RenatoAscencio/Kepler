package org.alexdev.kepler.messages.outgoing.catalogue;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

public class FURNI_REVISIONS extends MessageComposer {
    @Override
    public void compose(NettyResponse response) {
        // The client's handle_furni_revisions expects two groups of (count + class/revision pairs).
        // Send two empty groups so the DD can proceed with downloads.
        response.writeInt(0); // Floor item revisions count
        response.writeInt(0); // Wall item revisions count
    }

    @Override
    public short getHeader() {
        return 295; // "Dg" - same header as SPRITE_LIST (furni revisions response)
    }
}
