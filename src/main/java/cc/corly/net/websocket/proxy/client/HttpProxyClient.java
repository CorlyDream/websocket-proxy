package cc.corly.net.websocket.proxy.client;

import cc.corly.net.websocket.proxy.server.container.Container;
import cc.corly.net.websocket.proxy.server.container.ContainerHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class HttpProxyClient implements Container {
    private static final Logger log = LoggerFactory.getLogger(HttpProxyClient.class);
    private NioEventLoopGroup bossGroup = new NioEventLoopGroup();

    private NioEventLoopGroup workerGroup = new NioEventLoopGroup();

    @Override
    public void start() {
        ServerBootstrap httpServerBootstrap = new ServerBootstrap();
        String ip = "0.0.0.0";
        Integer port = 18999;
        initHttpProxyServer(httpServerBootstrap, bossGroup, workerGroup);
        try {
            httpServerBootstrap.bind(ip, port).get();
            log.info("http proxy server started {}", ip + ":" + port);
        } catch (Exception e) {
            log.error("start http proxy server failed", e);
        }
    }

    @Override
    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    private void initHttpProxyServer(ServerBootstrap httpServerBootstrap, NioEventLoopGroup bossGroup, NioEventLoopGroup workerGroup) {
        httpServerBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        log.error("exceptionCaught", cause);
                        super.exceptionCaught(ctx, cause);
                    }

                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ForwardWebSocketHandler());
                        pipeline.addLast("httpServerCodec", new HttpServerCodec());
                        pipeline.addLast("schemaProcessHandler", new SchemaProcessHandler());
                    }
                });
    }

    public static void main(String[] args) {
        ContainerHelper.start(Arrays.asList(new HttpProxyClient()));
    }
}
