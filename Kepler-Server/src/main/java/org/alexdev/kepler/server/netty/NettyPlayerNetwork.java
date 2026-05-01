package org.alexdev.kepler.server.netty;

import io.netty.channel.Channel;
import org.alexdev.kepler.messages.types.MessageComposer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class NettyPlayerNetwork {
    private int port;
    private Channel channel;
    private int connectionId;

    public NettyPlayerNetwork(Channel channel, int connectionId) {
        this.channel = channel;
        this.connectionId = connectionId;
        this.port = getPort(channel.localAddress());
    }

    public Channel getChannel() {
        return this.channel;
    }

    public int getPort() {
        return port;
    }

    public void send(Object response) {
        this.channel.writeAndFlush(response);
    }

    public void sendQueued(MessageComposer response) {
        this.channel.write(response);
    }

    public void flush() {
        this.channel.flush();
    }

    public void disconnect() {
        this.channel.close();
    }

    public int getConnectionId() {
        return connectionId;
    }

    public static String getIpAddress(Channel channel) {
        SocketAddress remoteAddress = channel.remoteAddress();

        if (remoteAddress instanceof InetSocketAddress inetSocketAddress) {
            return inetSocketAddress.getAddress().getHostAddress();
        }

        return remoteAddress != null ? remoteAddress.toString().replace("/", "").split(":")[0] : "";
    }

    private static int getPort(SocketAddress localAddress) {
        if (localAddress instanceof InetSocketAddress inetSocketAddress) {
            return inetSocketAddress.getPort();
        }

        if (localAddress == null) {
            return 0;
        }

        String[] addressParts = localAddress.toString().split(":");
        return Integer.parseInt(addressParts[addressParts.length - 1]);
    }
}
