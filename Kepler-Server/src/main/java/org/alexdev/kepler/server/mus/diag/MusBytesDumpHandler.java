package org.alexdev.kepler.server.mus.diag;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Diagnostic-only handler placed at the head of the MUS pipeline. It logs the
 * raw bytes the client sends on each connection (capped at a few KB and a few
 * inbound chunks per connection so we don't fill the disk on production
 * traffic), plus the close reason. Will be removed once the camera path is
 * confirmed working.
 *
 * The handler is passthrough — it forwards every ByteBuf to the next handler
 * unchanged via super.channelRead(ctx, msg).
 */
@ChannelHandler.Sharable
public class MusBytesDumpHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(MusBytesDumpHandler.class);

    private static final int MAX_CHUNKS_LOGGED = 8;
    private static final int MAX_BYTES_LOGGED = 256;

    private static final io.netty.util.AttributeKey<int[]> CHUNK_COUNT_KEY =
            io.netty.util.AttributeKey.valueOf("MusBytesDumpHandler.chunkCount");

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("[DIAG] mus connection opened from {}", ctx.channel().remoteAddress());
        ctx.channel().attr(CHUNK_COUNT_KEY).set(new int[] { 0 });
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf in) {
            int[] count = ctx.channel().attr(CHUNK_COUNT_KEY).get();
            if (count != null && count[0] < MAX_CHUNKS_LOGGED) {
                count[0]++;
                int readable = in.readableBytes();
                int toShow = Math.min(readable, MAX_BYTES_LOGGED);
                String hex = ByteBufUtil.hexDump(in, in.readerIndex(), toShow);
                log.info("[DIAG] mus chunk #{} from {} — {} bytes (showing {}): 0x{}",
                        count[0],
                        ctx.channel().remoteAddress(),
                        readable,
                        toShow,
                        hex);
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("[DIAG] mus connection closed from {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("[DIAG] mus exception on {}: {}",
                ctx.channel().remoteAddress(),
                cause.toString());
        super.exceptionCaught(ctx, cause);
    }
}
