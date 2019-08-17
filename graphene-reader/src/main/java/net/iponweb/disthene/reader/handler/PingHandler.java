package net.iponweb.disthene.reader.handler;

import io.netty.handler.codec.http.*;

/**
 * @author Andrei Ivanov
 */
public class PingHandler implements DistheneReaderHandler {

    @Override
    public FullHttpResponse handle(HttpRequest request) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }
}
