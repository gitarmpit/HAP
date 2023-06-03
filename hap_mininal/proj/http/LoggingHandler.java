//package http;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.*;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//
//import org.apache.commons.io.HexDump;
//
//public class LoggingHandler extends ChannelDuplexHandler {
//    
//
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg)
//            throws Exception {
//        super.channelRead(ctx, msg);
//    }
//    
//    @Override
//    public void write(ChannelHandlerContext ctx, Object msg,
//            ChannelPromise promise) throws Exception {
//        super.write(ctx, msg, promise);
//    }
//    
//    private void logBytes(String type, ByteBuf buf, ChannelHandlerContext ctx) throws IOException {
//        if (buf.readableBytes() > 0) {
//            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
//                byte[] bytes = new byte[buf.readableBytes()];
//                buf.getBytes(0, bytes, 0, bytes.length);
//                HexDump.dump(bytes, 0, stream, 0);
//                stream.flush();
//            }
//        }
//    }
//}
