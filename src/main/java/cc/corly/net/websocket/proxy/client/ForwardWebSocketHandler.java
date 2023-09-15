package cc.corly.net.websocket.proxy.client;

import cc.corly.net.websocket.proxy.Config;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ForwardWebSocketHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ForwardWebSocketHandler.class);

    private volatile WebSocketProxyClient targetSocket;

    private volatile boolean connected = false;

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (connected && targetSocket != null) {
            ByteBuf buf = (ByteBuf) msg;
//            log.info("write to target {}", buf.readableBytes());
            targetSocket.send(buf.nioBuffer());
            return;
        }
        ByteBuf sourceCopy = ((ByteBuf) msg).copy();
        super.channelRead(ctx, msg);
        String uri = ctx.channel().attr(Config.httpUri).get();
        HttpMethod httpMethod = ctx.channel().attr(Config.httpMethod).get();

        try {
            targetSocket = WebSocketProxyClient.getWebSocket(uri, ctx.channel());
            targetSocket.connect();
            targetSocket.setConnectedCallback(serverHandshake -> {
                connected = true;
                if (httpMethod == HttpMethod.CONNECT) {
                    FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "Connection Established"));
                    ctx.channel().writeAndFlush(resp).addListener((ChannelFutureListener) channelFuture -> removeCodecHandler(ctx.pipeline()));
                } else {
                    removeCodecHandler(ctx.pipeline());
                    targetSocket.send(sourceCopy.nioBuffer());
                }
                return connected;
            });

            ctx.channel().attr(Config.clientSocket).set(targetSocket);
        } catch (Exception e) {
            log.error("connect uri {} failed", uri, e);
            ctx.channel().close();
        }
    }


    private void removeCodecHandler(ChannelPipeline pipeline) {
        pipeline.remove("httpServerCodec");
        pipeline.remove("schemaProcessHandler");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        WebSocket targetChannel = ctx.channel().attr(Config.clientSocket).get();
        if (targetChannel != null) {
            targetChannel.close(CloseFrame.NORMAL, "client channel Inactive");
        }
        super.channelInactive(ctx);
    }
}
