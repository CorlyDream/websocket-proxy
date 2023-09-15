package cc.corly.net.websocket.proxy.client;

import cc.corly.net.websocket.proxy.Config;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class WebSocketProxyClient extends WebSocketClient {
    private static final Logger log = LoggerFactory.getLogger(WebSocketProxyClient.class);

    private Channel channel;
    private  Function<ServerHandshake, Boolean> connectedCallback;

    public WebSocketProxyClient(URI serverUri, Map<String, String> httpHeaders, Channel channel) {
        super(serverUri,new Draft_6455(), httpHeaders, 1000*3);
        this.channel = channel;
    }
    public void setConnectedCallback( Function<ServerHandshake, Boolean> connectedCallback) {
        this.connectedCallback = connectedCallback;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("new connection opened");
        if (this.connectedCallback != null) {
            this.connectedCallback.apply(handshakedata);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("closed with exit code " + code + " additional info: " + reason);
        channel.close();
    }

    @Override
    public void onMessage(String message) {
        log.info("received message: " + message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
//        log.info("received ByteBuffer {}", message);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(message);
        channel.writeAndFlush(byteBuf);
    }

    @Override
    public void onError(Exception ex) {
        log.error("an error to connect {}", this.getURI(), ex);
        channel.close();
    }

    public static WebSocketProxyClient getWebSocket(String uri, Channel channel) throws URISyntaxException, InterruptedException {
        Map<String, String> header = new HashMap<>();
        header.put(Config.FORWARD_URI, uri);
        WebSocketProxyClient client = new WebSocketProxyClient(new URI("ws://127.0.0.1:9000"), header, channel);
        return client;
    }
}
