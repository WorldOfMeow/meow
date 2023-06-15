package world.getmeow;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.internal.PlatformDependent;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Meow {
    /**
     * A server-side class bound to a client which is connected to the server.
     * This class is expected be extended in order to store state information (eg. client ID) and add more functions.
     *
     * @param <D> the type of the data being transmitted
     */
    public static class ServerClient<D> {
        private volatile ChannelHandlerContext context;



        /**
         * Asynchronously sends data to the client.
         *
         * @param data the data to send
         */
        public void send(D data) {
            context.writeAndFlush(data);
        }

        /**
         * Asynchronously sends data to the client and closes the connection as soon as the transmission is done.
         *
         * @param data the data to send
         */
        public void sendAndClose(D data) {
            context.writeAndFlush(data).addListener(ChannelFutureListener.CLOSE);
        }

        /**
         * Asynchronously sends data to the client and executes the specified action as soon as the transmission is done.
         *
         * @param data the data to send
         * @param runnable the action to execute
         */
        public void sendAndThen(D data, Runnable runnable) {
            context.writeAndFlush(data).addListener((ChannelFutureListener) runnable);
        }



        /**
         * Asynchronously closes the connection with the client.
         */
        public void close() {
            context.close();
        }

        /**
         * Returns the connection channel's context, allowing direct interaction with Netty.
         * Null is returned in case the client is no longer connected.
         *
         * @return the context or null, if the client is no longer connected
         */
        public ChannelHandlerContext getContext() {
            return context;
        }



        void setContext(ChannelHandlerContext context) {
            this.context = context;
        }

        void sendRaw(byte[] data) {
            context.writeAndFlush(context.alloc().buffer().writeBytes(data));
        }

        void sendRawAndClose(byte[] data) {
            context.writeAndFlush(context.alloc().buffer().writeBytes(data))
                    .addListener(ChannelFutureListener.CLOSE);
        }

        void sendRawAndThen(byte[] data, Runnable runnable) {
            context.writeAndFlush(context.alloc().buffer().writeBytes(data))
                    .addListener((ChannelFutureListener) runnable);
        }
    }

    /**
     * A server which can accept connections and communicate with its clients.
     *
     * @param <C> the type of the object which is bound to all connected clients
     * @param <D> the type of the data being transmitted
     */
    public static class Server<C extends ServerClient<D>, D> {
        private final Set<C> clients = new HashSet<>();
        private final DataSerializer<D> serializer;
        private final Supplier<C> clientSupplier;
        private EventLoopGroup bossGroup;
        private EventLoopGroup workerGroup;

        private volatile Consumer<ServerBootstrap> onConfigured;
        private volatile Consumer<SocketChannel> onChannelInitialized;
        private volatile Consumer<C> onConnected;
        private volatile BiConsumer<C, D> onReceived;
        private volatile Consumer<C> onDisconnected;
        private volatile BiConsumer<C, Throwable> onException = (client, cause) -> cause.printStackTrace();

        /**
         * Create a new instance with the specified {@link DataSerializer} and {@link ServerClient} instantiator.
         *
         * @param serializer the serializer and deserializer of the transmitted data
         * @param clientSupplier the supplier which creates {@link C} instances
         */
        public Server(DataSerializer<D> serializer, Supplier<C> clientSupplier) {
            this.serializer = serializer;
            this.clientSupplier = clientSupplier;
        }



        /**
         * Called when the {@link ServerBootstrap} has been configured.
         *
         * @param onConfigured the code to execute, can be null
         */
        public void onConfigured(Consumer<ServerBootstrap> onConfigured) {
            this.onConfigured = onConfigured;
        }

        /**
         * Called when a new channel has been created (leading to a client) and its initialization has been completed.
         *
         * @param onChannelInitialized the code to execute, can be null
         */
        public void onChannelInitialized(Consumer<SocketChannel> onChannelInitialized) {
            this.onChannelInitialized = onChannelInitialized;
        }

        /**
         * Called when a new client has connected and is not ready to receive data.
         *
         * @param onConnected the code to execute, can be null
         */
        public void onConnected(Consumer<C> onConnected) {
            this.onConnected = onConnected;
        }

        /**
         * Called when data has been received from a client.
         *
         * @param onReceived the code to execute, can be null
         */
        public void onReceived(BiConsumer<C, D> onReceived) {
            this.onReceived = onReceived;
        }

        /**
         * Called when a client has disconnected.
         *
         * @param onDisconnected the code to execute, can be null
         */
        public void onDisconnected(Consumer<C> onDisconnected) {
            this.onDisconnected = onDisconnected;
        }

        /**
         * Called when an uncaught exception occurs in the client's pipeline.
         *
         * @param onException the code to execute, can be null
         */
        public void onException(BiConsumer<C, Throwable> onException) {
            this.onException = onException;
        }



        /**
         * Starts the server synchronously. Once it is completed, the server is ready to receive connections.
         *
         * @param host the address of the server, can be null
         * @param port the port of the server
         * @throws InterruptedException if the thread gets interrupted while
         * the {@code host} and {@code port} are being bound
         */
        public void start(String host, int port) throws InterruptedException {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) {
                            channel.pipeline().addLast(new Packets.PacketDecoder<>(serializer),
                                    new LengthFieldPrepender(4),
                                    new Packets.PacketEncoder<>(serializer),
                                    new ServerChannelHandler());

                            Consumer<SocketChannel> consumer = onChannelInitialized;
                            if (consumer != null) {
                                consumer.accept(channel);
                            }
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            Consumer<ServerBootstrap> consumer = onConfigured;
            if (consumer != null) {
                consumer.accept(bootstrap);
            }

            ChannelFuture future = host == null ? bootstrap.bind(port) : bootstrap.bind(host, port);
            future.sync();
        }

        /**
         * Stops the server synchronously, freeing up all resources.
         *
         * @throws InterruptedException if the thread gets interrupted while the {@link EventLoopGroup}s are being shut down
         */
        public void stop() throws InterruptedException {
            synchronized (clients) {
                clients.clear();
            }
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
        }



        /**
         * Send the specified data to the specified clients.
         * The data is only serialized once, therefore using this method is better than
         * calling {@link ServerClient#send(Object)} on each client.
         *
         * @param data the data to send
         * @param clients the recipients
         */
        public void sendTo(D data, Collection<C> clients) {
            byte[] serialized = serializer.serialize(data);
            clients.forEach(client -> client.sendRaw(serialized));
        }

        /**
         * Send the specified data to the specified clients,
         * while also closing the connections directly after sending.
         * The data is only serialized once, therefore using this method is better than
         * calling {@link ServerClient#send(Object)} on each client.
         *
         * @param data the data to send
         * @param clients the recipients
         */
        public void sendToAndClose(D data, Collection<C> clients) {
            byte[] serialized = serializer.serialize(data);
            clients.forEach(client -> client.sendRawAndClose(serialized));
        }

        /**
         * Send the specified data to the specified clients,
         * while also executing the specified action directly after sending.
         * The data is only serialized once, therefore using this method is better than
         * calling {@link ServerClient#send(Object)} on each client.
         *
         * @param data the data to send
         * @param clients the recipients
         * @param runnable the action to execute
         */
        public void sendToAndThen(D data, Collection<C> clients, Runnable runnable) {
            byte[] serialized = serializer.serialize(data);
            clients.forEach(client -> client.sendRawAndThen(serialized, runnable));
        }



        /**
         * Gets all connected clients.
         *
         * @return all connected clients
         */
        public Collection<C> getAllClients() {
            synchronized (clients) {
                return new ArrayList<>(clients);
            }
        }

        /**
         * Gets all connected clients, excluding the specified one.
         *
         * @param excluding the client to exclude
         * @return all connected clients, excluding one
         */
        public Collection<C> getAllClientsExcept(C excluding) {
            Set<C> set;
            synchronized (clients) {
                set = new HashSet<>(clients);
            }
            set.remove(excluding);
            return set;
        }

        /**
         * Gets all connected clients, excluding the specified ones.
         *
         * @param excluding the clients to exclude
         * @return all connected clients, excluding some
         */
        public Collection<C> getAllClientsExcept(Collection<C> excluding) {
            Set<C> set;
            synchronized (clients) {
                set = new HashSet<>(clients);
            }
            set.removeAll(excluding);
            return set;
        }



        private class ServerChannelHandler extends ChannelInboundHandlerAdapter {
            private C client;

            @Override
            public void channelActive(ChannelHandlerContext context) {
                client = clientSupplier.get();
                client.setContext(context);
                synchronized (clients) {
                    clients.add(client);
                }
                Consumer<C> consumer = onConnected;
                if (consumer != null) {
                    consumer.accept(client);
                }
            }

            @Override
            public void channelRead(ChannelHandlerContext context, Object message) {
                BiConsumer<C, D> consumer = onReceived;
                if (consumer != null) {
                    //noinspection unchecked
                    consumer.accept(client, (D) message);
                }
            }

            @Override
            public void channelInactive(ChannelHandlerContext context) {
                synchronized (clients) {
                    clients.remove(client);
                }
                Consumer<C> consumer = onDisconnected;
                if (consumer != null) {
                    consumer.accept(client);
                }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
                BiConsumer<C, Throwable> consumer = onException;
                if (consumer != null) {
                    consumer.accept(client, cause);
                }
            }
        }
    }

    /**
     * A client which can connect to and communicate with a server.
     *
     * @param <D> the type of the data being transmitted
     */
    public static class Client<D> {
        private final DataSerializer<D> serializer;
        private Bootstrap bootstrap;
        private EventLoopGroup workerGroup;
        private volatile ChannelHandlerContext context;
        private final AtomicBoolean initialized = new AtomicBoolean(false);

        private volatile Consumer<Bootstrap> onConfigured;
        private volatile Consumer<SocketChannel> onChannelInitialized;
        private volatile Runnable onConnected;
        private volatile Consumer<D> onReceived;
        private volatile Runnable onDisconnected;
        private volatile Consumer<Throwable> onException = Throwable::printStackTrace;

        /**
         * Create a new instance with the specified {@link DataSerializer}.
         *
         * @param serializer the serializer and deserializer of the transmitted data
         */
        public Client(DataSerializer<D> serializer) {
            this.serializer = serializer;
        }



        /**
         * Called when the {@link Bootstrap} has been configured.
         *
         * @param onConfigured the code to execute, can be null
         */
        public void onConfigured(Consumer<Bootstrap> onConfigured) {
            this.onConfigured = onConfigured;
        }

        /**
         * Called when the channel has been created (leading to the server) and its initialization has been completed.
         *
         * @param onChannelInitialized the code to execute, can be null
         */
        public void onChannelInitialized(Consumer<SocketChannel> onChannelInitialized) {
            this.onChannelInitialized = onChannelInitialized;
        }

        /**
         * Called when the connection has been established and data
         * transmission between the server and this client becomes possible.
         *
         * @param onConnected the code to execute, can be null
         */
        public void onConnected(Runnable onConnected) {
            this.onConnected = onConnected;
        }

        /**
         * Called when data has been received from the server.
         *
         * @param onReceived the code to execute, can be null
         */
        public void onReceived(Consumer<D> onReceived) {
            this.onReceived = onReceived;
        }

        /**
         * Called when this client gets disconnected from the server.
         *
         * @param onDisconnected the code to execute, can be null
         */
        public void onDisconnected(Runnable onDisconnected) {
            this.onDisconnected = onDisconnected;
        }

        /**
         * Called when an uncaught exception occurs in the pipeline.
         *
         * @param onException the code to execute, can be null
         */
        public void onException(Consumer<Throwable> onException) {
            this.onException = onException;
        }



        /**
         * Connect to the server synchronously. Once it is completed, the client is ready to send and receive data.
         *
         * @param host the address of the server
         * @param port the port of the server
         * @param timeoutMillis the timeout for the connection in millis, or a non-positive value for no timeout
         * @return true if the connection was successful
         * @throws InterruptedException if the thread gets interrupted while connecting
         */
        public boolean connect(String host, int port, long timeoutMillis) throws InterruptedException {
            if (!initialized.getAndSet(true)) {
                bootstrap = new Bootstrap();
                workerGroup = new NioEventLoopGroup();

                bootstrap.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel channel) {
                                channel.pipeline().addLast(new Packets.PacketEncoder<>(serializer),
                                        new LengthFieldPrepender(4),
                                        new Packets.PacketEncoder<>(serializer),
                                        new ClientChannelHandler());

                                Consumer<SocketChannel> consumer = onChannelInitialized;
                                if (consumer != null) {
                                    consumer.accept(channel);
                                }
                            }
                        })
                        .option(ChannelOption.SO_KEEPALIVE, true);

                Consumer<Bootstrap> consumer = onConfigured;
                if (consumer != null) {
                    consumer.accept(bootstrap);
                }
            }

            ChannelFuture future = bootstrap.connect(host, port);
            if (timeoutMillis <= 0) {
                future.sync();
                return true;
            }

            boolean inTime = future.await(timeoutMillis);
            if (future.cause() != null) {
                System.out.println("Exception...");
                PlatformDependent.throwException(future.cause());
            }
            return inTime;
        }

        /**
         * Synchronously disconnect from the server.
         *
         * @throws InterruptedException if the thread gets interrupted while disconnecting
         */
        public void disconnect() throws InterruptedException {
            context.close().sync();
        }

        /**
         * Synchronously uninitialize the client, freeing up all resources.
         *
         * @throws InterruptedException if the thread gets interrupted while the {@link EventLoopGroup} is being shut down
         */
        public void uninitialize() throws InterruptedException {
            if (initialized.getAndSet(false)) {
                workerGroup.shutdownGracefully().sync();
            }
        }

        /**
         * Returns the connection channel's context, allowing direct interaction with Netty.
         * Null is returned in case the client is not connected.
         *
         * @return the context or null, if the client is not connected
         */
        public ChannelHandlerContext getContext() {
            return context;
        }



        /**
         * Asynchronously sends data to the server.
         *
         * @param data the data to send
         */
        public void send(D data) {
            context.writeAndFlush(data);
        }

        /**
         * Asynchronously sends data to the server and closes the connection as soon as the transmission is done.
         *
         * @param data the data to send
         */
        public void sendAndClose(D data) {
            context.writeAndFlush(data).addListener(ChannelFutureListener.CLOSE);
        }

        /**
         * Asynchronously sends data to the server and executes the specified action as soon as the transmission is done.
         *
         * @param data the data to send
         * @param runnable the action to execute
         */
        public void sendAndThen(D data, Runnable runnable) {
            context.writeAndFlush(data).addListener((ChannelFutureListener) runnable);
        }



        private class ClientChannelHandler extends ChannelInboundHandlerAdapter {
            @Override
            public void channelActive(ChannelHandlerContext context) {
                Client.this.context = context;
                Runnable runnable = onConnected;
                if (runnable != null) {
                    runnable.run();
                }
            }

            @Override
            public void channelRead(ChannelHandlerContext context, Object message) {
                Consumer<D> consumer = onReceived;
                if (consumer != null) {
                    //noinspection unchecked
                    consumer.accept((D) message);
                }
            }

            @Override
            public void channelInactive(ChannelHandlerContext context) {
                Client.this.context = null;
                Runnable runnable = onDisconnected;
                if (runnable != null) {
                    runnable.run();
                }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
                Consumer<Throwable> consumer = onException;
                if (consumer != null) {
                    consumer.accept(cause);
                }
            }
        }
    }

    /**
     * Connects the {@link DataSerializer} to Netty's {@link ByteToMessageDecoder}.
     */
    public static class Packets {
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

    /**
     * A serializer and deserializer for all the data which is sent between the server and the client.
     * The implementation should be thread-safe.
     *
     * @param <D> the type of the data which can be processed
     */
    public static interface DataSerializer<D> {
        byte[] serialize(D data);

        /**
         * Deserializes a single instance of the data from the provided byte array.
         *
         * @param bytes the serialized data
         * @return the deserialized data
         */
        D deserialize(byte[] bytes);

        /**
         * Returns the type of the data which can be processed.
         *
         * @return the type of the data which can be processed
         */
        Class<D> getType();
    }
}
