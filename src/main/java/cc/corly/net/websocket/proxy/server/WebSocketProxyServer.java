package cc.corly.net.websocket.proxy.server;

import cc.corly.net.websocket.proxy.Config;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class WebSocketProxyServer extends WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketProxyServer.class);
    public WebSocketProxyServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        String uri = clientHandshake.getFieldValue(Config.FORWARD_URI);
        try {
            Channel channel = HttpProxyChannel.getChannel(uri, webSocket);
            webSocket.setAttachment(channel);
        } catch (Exception e) {
            log.error("connect to {} failed", uri, e);
            webSocket.close(CloseFrame.UNEXPECTED_CONDITION, e.getMessage());
            return;
        }
        log.info("new connection {} to uri {}", webSocket.getRemoteSocketAddress(), uri);
    }

    @Override
    public void onClose(WebSocket webSocket,  int code, String reason, boolean remote) {
        log.info("closed " + webSocket.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
        Channel channel = webSocket.getAttachment();
        if (channel != null) {
            channel.close();
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        log.info("received message from "	+ webSocket.getRemoteSocketAddress() + ": " + message);
    }

    public void onMessage(WebSocket webSocket, ByteBuffer message) {
//        log.info("received ByteBuffer from " + webSocket.getRemoteSocketAddress() + ": " + message);
        Channel channel = webSocket.getAttachment();
        ByteBuf byteBuf = Unpooled.wrappedBuffer(message);
        channel.writeAndFlush(byteBuf);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        log.error("an error occurred on connection {}", webSocket.getRemoteSocketAddress(), e);
        Channel channel = webSocket.getAttachment();
        if (channel != null) {
            channel.close();
        }
    }

    @Override
    public void onStart() {
        log.info("server started successfully {}", this.getAddress());
    }
    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 9000;

        WebSocketServer server = new WebSocketProxyServer(new InetSocketAddress(host, port));
        server.run();
    }
}
