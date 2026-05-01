package org.alexdev.kepler.server.mus.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import org.alexdev.kepler.server.mus.MusUtil;
import org.alexdev.kepler.server.mus.streams.MusMessage;
import org.alexdev.kepler.server.mus.streams.MusTypes;

import java.util.List;

public class MusNetworkDecoder extends ByteArrayDecoder {
    private static final int HEADER_LENGTH = 6;
    private static final int MAX_MESSAGE_SIZE = 1024 * 1024;
    private static final int MAX_RECEIVERS = 256;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) {
        if (buffer.readableBytes() < HEADER_LENGTH) {
            return;
        }

        buffer.markReaderIndex();
        byte headerTag = buffer.readByte();
        buffer.readByte();

        if (headerTag != 'r') {
            ctx.channel().close();
        } else {
            MusMessage musMessage = new MusMessage();
            musMessage.setSize(buffer.readInt());

            if (musMessage.getSize() < 0 || musMessage.getSize() > MAX_MESSAGE_SIZE) {
                ctx.channel().close();
                return;
            }

            if (buffer.readableBytes() < musMessage.getSize()) {
                buffer.resetReaderIndex();
                return;
            }

            ByteBuf body = buffer.readBytes(musMessage.getSize());

            try {
                musMessage.setErrorCode(body.readInt());
                musMessage.setTimestamp(body.readInt());
                musMessage.setSubject(MusUtil.readEvenPaddedString(body));
                musMessage.setSenderId(MusUtil.readEvenPaddedString(body));

                int receiverCount = body.readInt();

                if (receiverCount < 0 || receiverCount > MAX_RECEIVERS) {
                    ctx.channel().close();
                    return;
                }

                String[] receivers = new String[receiverCount];

                for (int i = 0; i < receivers.length; i++) {
                    receivers[i] = MusUtil.readEvenPaddedString(body);
                }

                if ("Logon".equals(musMessage.getSubject())) {
                    // Read in remaining data
                    byte[] tmpBytes = new byte[body.readableBytes()];
                    body.readBytes(tmpBytes);

                    // Set fields
                    musMessage.setContentType(MusTypes.String);
                    musMessage.setContentString(new String(tmpBytes));
                } else {
                    musMessage.setContentType(body.readShort());

                    if (musMessage.getContentType() == MusTypes.Integer)
                        musMessage.setContentInt(body.readInt());
                    else if (musMessage.getContentType() == MusTypes.String)
                        musMessage.setContentString(MusUtil.readEvenPaddedString(body));
                    else if (musMessage.getContentType() == MusTypes.PropList)
                        musMessage.setContentPropList(MusUtil.readPropList(body));
                }

                out.add(musMessage);
            } finally {
                body.release();
            }
        }
    }
}
