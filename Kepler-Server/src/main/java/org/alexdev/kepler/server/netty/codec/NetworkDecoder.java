package org.alexdev.kepler.server.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.alexdev.kepler.util.encoding.Base64Encoding;

import java.util.List;

public class NetworkDecoder extends ByteToMessageDecoder {
    private static final int HEADER_LENGTH = 5;
    private static final int MAX_PACKET_SIZE = 256 * 1024;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) {
        if (buffer.readableBytes() < HEADER_LENGTH) {
            // If the incoming data is less than 5 bytes, it's junk.
            return;
        }

        buffer.markReaderIndex();
        int length = Base64Encoding.decode(new byte[]{buffer.readByte(), buffer.readByte(), buffer.readByte()});

        if (length < 0 || length > MAX_PACKET_SIZE) {
            ctx.close();
            return;
        }

        if (buffer.readableBytes() < length) {
            buffer.resetReaderIndex();
            return;
        }

        out.add(new NettyRequest(buffer.readBytes(length)));
    }
}
