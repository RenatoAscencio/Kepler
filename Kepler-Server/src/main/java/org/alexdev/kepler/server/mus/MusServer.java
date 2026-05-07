package org.alexdev.kepler.server.mus;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.alexdev.kepler.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class MusServer {
    final private static int BACK_LOG = 20;
    // Adaptive receive buffer bounds. The default 1 KB initial was clearly
    // sized for tiny game messages; camera BINDATA frames can run up to 32 MB
    // (see MusChannelInitializer). Boost the read window so multi-MB photo
    // uploads don't get split into thousands of 8 KB reads.
    private static final int RECV_BUFFER_MIN = 64;
    private static final int RECV_BUFFER_INITIAL = 64 * 1024;
    private static final int RECV_BUFFER_MAX = 1024 * 1024;
    final private static Logger log = LoggerFactory.getLogger(MusServer.class);

    private final String ip;
    private final int port;

    private DefaultChannelGroup channels;
    private ServerBootstrap bootstrap;
    private AtomicInteger connectionIds;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public MusServer(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.bootstrap = new ServerBootstrap();
        this.connectionIds = new AtomicInteger(0);
    }

    /**
     * Create the Netty sockets.
     */
    public void createSocket() {
        int workerThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
        this.bossGroup = (Epoll.isAvailable()) ? new EpollEventLoopGroup(1) : new NioEventLoopGroup(1);
        this.workerGroup = (Epoll.isAvailable()) ? new EpollEventLoopGroup(workerThreads) : new NioEventLoopGroup(workerThreads);

        // SO_RCVBUF is intentionally unset so the kernel can auto-tune the
        // socket buffer for large camera uploads. Setting it explicitly to a
        // small value (the previous 8 KB) caps the in-flight TCP window and
        // turns multi-MB BINDATA into hundreds of round trips.
        this.bootstrap.group(bossGroup, workerGroup)
                .channel((Epoll.isAvailable()) ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new MusChannelInitializer(this))
                .option(ChannelOption.SO_BACKLOG, BACK_LOG)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.RCVBUF_ALLOCATOR,
                        new AdaptiveRecvByteBufAllocator(RECV_BUFFER_MIN, RECV_BUFFER_INITIAL, RECV_BUFFER_MAX))
                .childOption(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true));
    }

    /**
     * Bind the server to its address that's been specified
     */
    public void bind() {
        this.bootstrap.bind(new InetSocketAddress(this.getIp(), this.getPort())).addListener(objectFuture -> {
            if (!objectFuture.isSuccess()) {
                Log.getErrorLogger().error("Failed to start MUS server on address: {}:{}", this.getIp(), this.getPort());
                Log.getErrorLogger().error("Please double check there's no programs using the same port, and you have set the correct IP address to listen on.", this.getIp(), this.getPort());
            } else {
                log.info("Multi User Server (MUS) is listening on {}:{}", this.getIp(), this.getPort());
            }
        });
    }

    /**
     * Dispose the server handler.
     *
     * @throws InterruptedException will throw exception if fails
     */
    public void dispose() throws InterruptedException {
        if (this.channels != null) {
            this.channels.close().sync();
        }

        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully().sync();
        }

        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully().sync();
        }
    }

    /**
     * Get the IP of this server.
     *
     * @return the server ip
     */
    private String getIp() {
        return ip;
    }

    /**
     * Get the port of this server.
     *
     * @return the port
     */
    private Integer getPort() {
        return port;
    }

    /**
     * Get default channel group of channels
     * @return channels
     */
    public DefaultChannelGroup getChannels() {
        return channels;
    }

    /**
     * Get handler for connection ids.
     *
     * @return the atomic int instance
     */
    public AtomicInteger getConnectionIds() {
        return connectionIds;
    }
}
