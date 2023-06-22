package world.getmeow.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;

/**
 * Connects the {@link DataSerializer} to Netty's {@link ByteToMessageDecoder}.
 */
public class NettyPackets {
    public static class PacketDecoder<D> extends ByteToMessageDecoder {
        private final DataSerializer<D> serializer;
        private int size = -1;

        public PacketDecoder(DataSerializer<D> serializer) {
            this.serializer = serializer;
        }



        @Override
        protected void decode(ChannelHandlerContext context, ByteBuf inputBuffer, List<Object> output) {
            if (size == -1) {
                if (inputBuffer.readableBytes() < 4) {
                    return;
                }

                size = inputBuffer.readInt();
            }

            if (inputBuffer.readableBytes() >= size) {
                byte[] bytes = new byte[size];
                inputBuffer.readBytes(bytes);
                output.add(serializer.deserialize(bytes));
                size = -1;
            }
        }
    }

    public static class PacketEncoder<D> extends MessageToByteEncoder<D> {
        private final DataSerializer<D> serializer;

        public PacketEncoder(DataSerializer<D> serializer) {
            super(serializer.getType());
            this.serializer = serializer;
        }

        @Override
        protected void encode(ChannelHandlerContext context, D data, ByteBuf outputBuffer) {
            outputBuffer.writeBytes(serializer.serialize(data));
        }
    }

}