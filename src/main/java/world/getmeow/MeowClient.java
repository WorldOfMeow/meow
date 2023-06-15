package world.getmeow;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class MeowClient {
    private String host;
    private int port;

    public MeowClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new MeowClientHandler());
                        }
                    });

            ChannelFuture f = b.connect(host, port).sync();
            System.out.println("Connected to server: " + host + ":" + port);

            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private class MeowClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            // Client connected
            System.out.println("Connected to server: " + ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            // Server disconnected
            System.out.println("Disconnected from server: " + ctx.channel().remoteAddress());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            String message = (String) msg;
            System.out.println("Received message from server: " + message);
        }
    }
}
