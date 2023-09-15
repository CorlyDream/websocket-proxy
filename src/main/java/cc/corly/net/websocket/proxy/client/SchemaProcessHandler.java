package cc.corly.net.websocket.proxy.client;

import cc.corly.net.websocket.proxy.Config;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 识别转发的协议是 http or https
 */
public class SchemaProcessHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(SchemaProcessHandler.class);
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            DefaultHttpRequest request = (DefaultHttpRequest) msg;
            String uri = request.getUri();
            if (request.method() == HttpMethod.CONNECT && !uri.startsWith("http")) {
                uri = "https://" + uri;
            }
            ctx.channel().attr(Config.httpUri).set(uri);
            ctx.channel().attr(Config.httpMethod).set(request.method());
        }else {
            // 理论不可能到这里
            log.error("not support msg {}", msg);
        }
    }
}
