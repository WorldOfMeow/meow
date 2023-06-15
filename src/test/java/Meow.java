//package world.getmeow;
//
//import io.netty.bootstrap.Bootstrap;
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.*;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.SocketChannel;
//import io.netty.channel.socket.nio.NioServerSocketChannel;
//import io.netty.channel.socket.nio.NioSocketChannel;
//
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Scanner;
//import java.util.function.BiConsumer;
//import java.util.function.Consumer;
//
//public class Meow {
//
//    // Server implementation
//    public static class Server<T> {
//        private final EventLoopGroup bossGroup;
//        private final EventLoopGroup workerGroup;
//        private final BiConsumer<ChannelClient, T> messageConsumer;
//        private final Consumer<ChannelClient> connectConsumer;
//        private final Consumer<ChannelClient> disconnectConsumer;
//        private final DataSerializer<T> serializer;
//        private final List<ChannelClient> connectedClients;
//
//        public Server(BiConsumer<ChannelClient, T> messageConsumer,
//                      Consumer<ChannelClient> connectConsumer,
//                      Consumer<ChannelClient> disconnectConsumer,
//                      DataSerializer<T> serializer) {
//            bossGroup = new NioEventLoopGroup();
//            workerGroup = new NioEventLoopGroup();
//            this.messageConsumer = messageConsumer;
//            this.connectConsumer = connectConsumer;
//            this.disconnectConsumer = disconnectConsumer;
//            this.serializer = serializer;
//            this.connectedClients = new ArrayList<>();
//        }
//
//        public void start(int port) throws Exception {
//            try {
//                ServerBootstrap b = new ServerBootstrap();
//                b.group(bossGroup, workerGroup)
//                        .channel(NioServerSocketChannel.class)
//                        .childHandler(new ChannelInitializer<SocketChannel>() {
//                            @Override
//                            protected void initChannel(SocketChannel ch) {
//                                ChannelPipeline pipeline = ch.pipeline();
//                                pipeline.addLast(new ServerHandler());
//                            }
//                        });
//
//                ChannelFuture f = b.bind(port).sync();
//
//                f.channel().closeFuture().sync();
//            } finally {
//                workerGroup.shutdownGracefully();
//                bossGroup.shutdownGracefully();
//            }
//        }
//
//        private class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
//            private ChannelClient client;
//
//            @Override
//            public void channelActive(ChannelHandlerContext ctx) {
//                Channel channel = ctx.channel();
//                client = new ChannelClient(channel);
//                connectedClients.add(client);
//                connectConsumer.accept(client);
//            }
//
//            @Override
//            public void channelInactive(ChannelHandlerContext ctx) {
//                disconnectClient();
//            }
//
//            @Override
//            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
//                byte[] data = new byte[msg.readableBytes()];
//                msg.readBytes(data);
//                T message = serializer.deserialize(data);
//                messageConsumer.accept(client, message);
//                broadcastMessage(message);
//            }
//
//            @Override
//            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//                disconnectClient();
//                ctx.close();
//            }
//
//            private void disconnectClient() {
//                connectedClients.remove(client);
//                disconnectConsumer.accept(client);
//            }
//
//            private void broadcastMessage(T message) {
//                for (ChannelClient client : connectedClients) {
//                    client.send((String) message);
//                }
//            }
//        }
//    }
//
//    public static class ChannelClient {
//        public Channel getChannel() {
//            return channel;
//        }
//
//        public void setChannel(Channel channel) {
//            this.channel = channel;
//        }
//
//        private Channel channel;
//        public ChannelClient(Channel channel) {
//            this.channel = channel;
//        }
//        public void send(String message) {
//            if (channel != null && channel.isActive()) {
//                byte[] data = message.getBytes(StandardCharsets.UTF_8);
//                ByteBuf buf = channel.alloc().buffer(data.length);
//                buf.writeBytes(data);
//                channel.writeAndFlush(buf);
//            } else {
//                throw new RuntimeException("Not connected");
//            }
//        }
//    }
//
//    public static class Client<T> {
//        private ClientHandler handler;
//        private final EventLoopGroup group;
//        private final Consumer<T> messageConsumer;
//        private final Consumer<Client> connectConsumer;
//        private final Consumer<Client> disconnectConsumer;
//        private final DataSerializer<T> serializer;
//
//        public Client(Consumer<T> messageConsumer,
//                      Consumer<Client> connectConsumer,
//                      Consumer<Client> disconnectConsumer,
//                      DataSerializer<T> serializer) {
//            group = new NioEventLoopGroup();
//            this.messageConsumer = messageConsumer;
//            this.connectConsumer = connectConsumer;
//            this.disconnectConsumer = disconnectConsumer;
//            this.serializer = serializer;
//        }
//
//        public void connect(String host, int port) throws Exception {
//            ClientHandler handler = new ClientHandler();
//            try {
//                Bootstrap b = new Bootstrap();
//                b.group(group)
//                        .channel(NioSocketChannel.class)
//                        .handler(new ChannelInitializer<SocketChannel>() {
//                            @Override
//                            protected void initChannel(SocketChannel ch) {
//                                ChannelPipeline pipeline = ch.pipeline();
//                                pipeline.addLast(handler);
//                            }
//                        });
//
//                ChannelFuture f = b.connect(host, port).sync();
//                this.handler = handler;
//                f.channel().closeFuture().sync();
//            } finally {
//                group.shutdownGracefully();
//            }
//        }
//
//        public void send(T message) {
//            if (handler != null && handler.channel != null && handler.channel.isActive()) {
//                byte[] data = serializer.serialize(message);
//                ByteBuf buf = handler.channel.alloc().buffer(data.length);
//                buf.writeBytes(data);
//                handler.channel.writeAndFlush(buf);
//            } else {
//                throw new RuntimeException("Not connected");
//            }
//        }
//
//
//        private class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
//            public Channel channel;
//
//            @Override
//            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
//                byte[] data = new byte[msg.readableBytes()];
//                msg.readBytes(data);
//                T message = serializer.deserialize(data);
//                messageConsumer.accept(message);
//            }
//
//            @Override
//            public void channelActive(ChannelHandlerContext ctx) {
//                channel = ctx.channel();
//                connectConsumer.accept(Client.this);
//            }
//
//            @Override
//            public void channelInactive(ChannelHandlerContext ctx) {
//                disconnectConsumer.accept(Client.this);
//            }
//        }
//
//        public static void main(String[] args) throws Exception {
//            // Define a serializer for String messages
//            DataSerializer<String> stringSerializer = new DataSerializer<String>() {
//                @Override
//                public byte[] serialize(String data) {
//                    return data.getBytes(StandardCharsets.UTF_8);
//                }
//
//                @Override
//                public String deserialize(byte[] bytes) {
//                    return new String(bytes, StandardCharsets.UTF_8);
//                }
//
//                @Override
//                public Class<String> getType() {
//                    return String.class;
//                }
//            };
//
//            new Thread(() -> {
//                Server<String> server = new Server<>(
//                        (channel, message) -> { System.out.println("Received message: " + message); channel.send(message);},
//                        channel -> System.out.println("Client connected"),
//                        channel -> System.out.println("Client disconnected: " + channel.getChannel().remoteAddress()),
//                        stringSerializer
//                );
//                try {
//                    server.start(8080);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }).start();
//
//            Thread.sleep(1000);
//
//            Client<String> client = new Client<>(
//                    message -> System.out.println("Received message: " + message),
//                    c -> {
//                        System.out.println("Connected to server!");
//                        Scanner scanner = new Scanner(System.in);
//                        while (c.handler.channel.isActive()) {
//                            String message = scanner.nextLine();
//                            c.send(message);
//                        }
//                    },
//                    unused -> System.out.println("Disconnected from server"),
//                    stringSerializer
//            );
//            client.connect("localhost", 8080);
//        }
//    }
//
//    // Serializer interface
//    public interface DataSerializer<T> {
//        byte[] serialize(T data);
//
//        T deserialize(byte[] bytes);
//
//        Class<T> getType();
//    }
//}
