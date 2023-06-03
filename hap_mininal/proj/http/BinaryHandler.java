package http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.io.IOException;
import java.util.List;

public class BinaryHandler extends ByteToMessageCodec<ByteBuf> {
    
    private final HomekitClientConnection connection;
    private boolean started = false;
    
    public BinaryHandler(HomekitClientConnection connection) {
        this.connection = connection;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out)
            throws Exception {
        if (started) {
            byte[] b = new byte[msg.readableBytes()];
            msg.readBytes(b);
            traceData("Sending data", b, ctx);
            out.writeBytes(connection.encryptResponse(b));
        } else {
            out.writeBytes(msg);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in,
            List<Object> out) throws Exception {
        byte[] b = new byte[in.readableBytes()];
        in.readBytes(b);
        byte[] decrypted = connection.decryptRequest(b);
        traceData("Received data", decrypted, ctx);
        out.add(Unpooled.copiedBuffer(decrypted));
        started = true;
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        boolean errorLevel = !(cause instanceof IOException);
        super.exceptionCaught(ctx, cause);
        System.out.println("Exception in binary handler: " + cause.getMessage());

    }
    
    private void traceData(String msg, byte[] b, ChannelHandlerContext ctx) throws Exception {
//        if (logger.isTraceEnabled() && b.length > 0) {
//            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
//                HexDump.dump(b, 0, stream, 0);
//                stream.flush();
//                logger.trace(String.format("%s [%s]:%n%s%n", msg, ctx.channel().remoteAddress().toString(),
//                        stream.toString(StandardCharsets.UTF_8.name())));
//            }
//        }
    }

}
