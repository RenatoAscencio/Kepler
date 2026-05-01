package org.alexdev.kepler.server.netty.codec.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

import java.util.function.Consumer;

public class WebSocketHandshakeCompleteHandler extends ChannelInboundHandlerAdapter {
    private final Consumer<ChannelPipeline> nativeConfigurer;

    public WebSocketHandshakeCompleteHandler(Consumer<ChannelPipeline> nativeConfigurer) {
        this.nativeConfigurer = nativeConfigurer;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        boolean handshakeComplete =
                event == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE ||
                event instanceof WebSocketServerProtocolHandler.HandshakeComplete;

        if (handshakeComplete) {
            this.nativeConfigurer.accept(ctx.pipeline());
            ctx.pipeline().remove(this);
            return;
        }

        super.userEventTriggered(ctx, event);
    }
}
