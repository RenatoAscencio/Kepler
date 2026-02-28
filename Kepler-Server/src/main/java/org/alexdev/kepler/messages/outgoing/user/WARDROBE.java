package org.alexdev.kepler.messages.outgoing.user;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.util.Map;

public class WARDROBE extends MessageComposer {
    private final Map<Integer, String[]> wardrobe;

    public WARDROBE(Map<Integer, String[]> wardrobe) {
        this.wardrobe = wardrobe;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.wardrobe.size());
        for (Map.Entry<Integer, String[]> entry : this.wardrobe.entrySet()) {
            response.writeInt(entry.getKey());           // slot number
            response.writeString(entry.getValue()[0]);   // figure
            response.writeString(entry.getValue()[1]);   // sex
        }
    }

    @Override
    public short getHeader() {
        return 267;  // WARDROBE header
    }
}
