package org.alexdev.kepler.server.rcon.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.alexdev.kepler.server.rcon.messages.RconMessage;
import org.alexdev.kepler.util.StringUtil;

import java.util.HashMap;
import java.util.List;

public class RconNetworkDecoder extends ByteToMessageDecoder {
    private static final int HEADER_LENGTH = 8;
    private static final int MAX_MESSAGE_SIZE = 64 * 1024;
    private static final int MAX_PARAMETER_COUNT = 100;
    private static final int MAX_STRING_SIZE = 16 * 1024;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) {
        if (buffer.readableBytes() < HEADER_LENGTH) {
            // If the incoming data is less than 8 bytes, it's junk.
            return;
        }

        buffer.markReaderIndex();
        int length = buffer.readInt();

        if (length < 0 || length > MAX_MESSAGE_SIZE) {
            ctx.close();
            return;
        }

        if (buffer.readableBytes() < length) {
            buffer.resetReaderIndex();
            return;
        }

        ByteBuf buf = buffer.readBytes(length);

        try {
            String header = readString(buf);

            if (header == null || buf.readableBytes() < Integer.BYTES) {
                return;
            }

            int parameterCount = buf.readInt();

            if (parameterCount < 0 || parameterCount > MAX_PARAMETER_COUNT) {
                ctx.close();
                return;
            }

            HashMap<String, String> parameters = new HashMap<>(parameterCount);

            for (int i = 0; i < parameterCount; i++) {
                String key = readString(buf);
                String value = readString(buf);

                if (key == null || value == null) {
                    return;
                }

                parameters.put(key, value);
            }

            // Send new rcon message
            out.add(new RconMessage(header, parameters));
        } finally {
            clear(buf);
        }
    }

    private void clear(ByteBuf buf) {
        if (buf.refCnt() > 0) {
            buf.release();
        }
    }

    /**
     * Read string from byte buffer.
     *
     * @param buffer the buffer to read from
     * @return the string
     */
    public String readString(ByteBuf buffer) {
        if (buffer.readableBytes() < Integer.BYTES) {
            return null;
        }

        int length = buffer.readInt();

        if (length < 0 || length > MAX_STRING_SIZE) {
            return null;
        }

        byte[] data = this.readBytes(buffer, length);

        if (data == null) {
            return null;
        }

        return new String(data, StringUtil.getCharset());
    }

    /**
     * Read bytes of byte buffer.
     *
     * @param buf the buffer to read the bytes from
     * @param len the amount of bytes to read
     * @return the bytes returned
     */
    public byte[] readBytes(ByteBuf buf, int len) {
        if (buf.readableBytes() < len) {
            return null;
        }

        byte[] payload = new byte[len];
        buf.readBytes(payload);
        return payload;
    }

}
