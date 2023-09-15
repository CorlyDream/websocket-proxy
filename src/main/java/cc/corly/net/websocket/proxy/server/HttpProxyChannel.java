package cc.corly.net.websocket.proxy.server;

import cc.corly.net.websocket.proxy.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * 流量转发
 */
public class HttpProxyChannel {
    private static final Logger log = LoggerFactory.getLogger(HttpProxyChannel.class);
    private static Bootstrap proxyClientBootstrap = new Bootstrap();
    static {
        initProxyClient();
    }
    /**
     * 初始化代理服务客户端
     */
    private static void initProxyClient() {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        proxyClientBootstrap.channel(NioSocketChannel.class);
        proxyClientBootstrap.group(workerGroup).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {

                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        ByteBuf buf = (ByteBuf) msg;
//                        log.info("read from remote channel {}", ctx.channel());
                        ctx.channel().attr(Config.clientSocket).get().send(buf.nioBuffer());
                    }


                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        log.info("remote channel Inactive {}", ctx.channel());
                        WebSocket clientChannel = ctx.channel().attr(Config.clientSocket).get();
                        if (clientChannel != null) {
                            clientChannel.close(CloseFrame.NORMAL, "remote channel Inactive");
                        }
                        super.channelInactive(ctx);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        super.exceptionCaught(ctx, cause);
                        log.error("remote channel exception", cause);
                    }
                });
            }
        });
    }

    public static Channel getChannel(String uri, WebSocket webSocket) throws URISyntaxException {
        URI uriObj = new URI(uri);
        ChannelFuture future = proxyClientBootstrap.connect(uriObj.getHost(), uriObj.getPort());
        future.awaitUninterruptibly(500, TimeUnit.MILLISECONDS);
        if (future.isSuccess()) {
            Channel channel = future.channel();
            channel.attr(Config.clientSocket).set(webSocket);
            return channel;
        } else {
            throw new RuntimeException(future.cause());
        }
    }
}
