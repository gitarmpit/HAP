package http;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class AccessoryHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    
    private HomekitClientConnection connection;
    private final HomekitClientConnectionFactory homekitClientConnectionFactory;
    
    public AccessoryHandler(HomekitClientConnectionFactory homekitClientConnectionFactory) {
        this.homekitClientConnectionFactory = homekitClientConnectionFactory;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final Channel channel = ctx.pipeline().channel();
        this.connection = homekitClientConnectionFactory.createConnection(response -> {
            if (!channel.isActive()) { return; }
            channel.writeAndFlush(NettyResponseUtil.createResponse(response));
        });
        //LOGGER.info("New homekit connection from "+ctx.channel().remoteAddress().toString());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //LOGGER.info("Terminated homekit connection from "+ctx.channel().remoteAddress().toString());
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req)
            throws Exception {
        try {
            HttpResponse response = connection.handleRequest(new FullRequestHttpRequestImpl(req));
            if (response.doUpgrade()) {
                ChannelPipeline pipeline = ctx.channel().pipeline();
                pipeline.addBefore(ServerInitializer.HTTP_HANDLER_NAME, "binary", new BinaryHandler(connection));
            }
            sendResponse(response, ctx);
        } catch (Exception e) {
            System.out.println("Error handling homekit http request: " + e.getMessage());
            sendResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error: "+e.getMessage(), ctx);
        }
    }
    
    private void sendResponse(HttpResponseStatus status, String responseBody, ChannelHandlerContext ctx) {
        if (responseBody == null) {
            responseBody = "";
        }
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, 
                Unpooled.copiedBuffer(responseBody.getBytes(StandardCharsets.UTF_8)));
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        ctx.write(response);
        ctx.flush();
    }
    
    private void sendResponse(HttpResponse homekitResponse, ChannelHandlerContext ctx) {
        FullHttpResponse response = NettyResponseUtil.createResponse(homekitResponse);
        ctx.write(response);
        ctx.flush();
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        super.channelReadComplete(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        boolean errorLevel = !(cause instanceof IOException);
        System.out.println ("error: " + cause.getMessage());
        ctx.close();
    }
}
