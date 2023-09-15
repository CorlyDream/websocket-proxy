package cc.corly.net.websocket.proxy;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.AttributeKey;
import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public interface Config {
    Properties configuration = initConfig();

    static Properties initConfig() {
        Properties conf = new Properties();
        InputStream is = Config.class.getClassLoader().getResourceAsStream("config.properties");
        try {
            conf.load(is);
            is.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return conf;
    }

    AttributeKey<WebSocket> clientSocket = AttributeKey.newInstance("clientSocket");

    AttributeKey<WebSocketServer> serverSocket = AttributeKey.newInstance("serverSocket");

    AttributeKey<HttpMethod> httpMethod = AttributeKey.newInstance("httpMethod");
    AttributeKey<String> httpUri = AttributeKey.newInstance("httpUri");

    public static final String FORWARD_URI = "forwardUri";
}
