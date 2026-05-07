package org.alexdev.kepler.server.mus;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.alexdev.kepler.server.mus.codec.MusNetworkDecoder;
import org.alexdev.kepler.server.mus.codec.MusNetworkEncoder;
import org.alexdev.kepler.server.mus.diag.MusBytesDumpHandler;
import org.alexdev.kepler.server.netty.codec.websocket.ProtocolDetector;
import org.alexdev.kepler.server.netty.codec.websocket.WebSocketBinaryFrameCodec;
import org.alexdev.kepler.server.netty.codec.websocket.WebSocketHandshakeCompleteHandler;

public class MusChannelInitializer extends ChannelInitializer<SocketChannel> {
    private static final int MUS_HEADER_LENGTH = 6;
    private static final int MAX_MUS_MESSAGE_SIZE = 32 * 1024 * 1024;
    private static final int MAX_MUS_FRAME_SIZE = MAX_MUS_MESSAGE_SIZE + MUS_HEADER_LENGTH;

    private final MusServer musServer;

    public MusChannelInitializer(MusServer musServer) {
        this.musServer = musServer;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        // Diagnostic byte-dump handler at the head of the pipeline. Passthrough;
        // forwards every byte to the protocol detector unchanged. Caps output
        // at 8 chunks × 256 bytes per connection so it can't fill the disk
        // on production traffic. Will be removed once the camera path is
        // confirmed working.
        pipeline.addLast("musBytesDump", new MusBytesDumpHandler());
        pipeline.addLast("protocolDetector", new ProtocolDetector(
                this::configureWebSocket,
                this::configureNative
        ));
    }

    private void configureWebSocket(ChannelPipeline pipeline) {
        pipeline.addLast("httpCodec", new HttpServerCodec());
        // Match the native MUS frame ceiling so WebSocket-based clients (browser
        // builds, headless tests) can upload v14 camera BINDATA without hitting
        // the previous 64 KB cap.
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(MAX_MUS_FRAME_SIZE));
        pipeline.addLast("wsProtocol", new WebSocketServerProtocolHandler("/", null, true, MAX_MUS_FRAME_SIZE));
        pipeline.addLast("wsHandshakeComplete", new WebSocketHandshakeCompleteHandler(p -> {
            p.addLast("wsCodec", new WebSocketBinaryFrameCodec());
            configureNative(p);
        }));
    }

    private void configureNative(ChannelPipeline pipeline) {
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(MAX_MUS_FRAME_SIZE, 2, 4, 0, 0));
        pipeline.addLast("gameDecoder", new MusNetworkDecoder());
        pipeline.addLast("gameEncoder", new MusNetworkEncoder());
        pipeline.addLast("handler", new MusConnectionHandler(this.musServer));
    }
}
