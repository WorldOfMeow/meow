package world.getmeow.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

class LoginServerHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (isLoginMessage(msg)) {
            String[] parts = msg.split(" ", 3);
            if (parts.length != 3) {
                ctx.writeAndFlush("login.failed:Invalid login request. Please try again.");
                return;
            }
            String username = parts[1];
            String password = parts[2];

            if (isValidLogin(username, password)) {
                ctx.writeAndFlush("login.successful");
                ctx.pipeline().remove(this); // Remove this handler from the pipeline
            } else {
                ctx.writeAndFlush("login.failed:Invalid username or password.");
                ctx.close(); // Close the connection
            }
        } else {
            ctx.writeAndFlush("login.failed:Invalid login request. Please try again.");
            ctx.close(); // Close the connection
        }
    }

    protected boolean isLoginMessage(String message) {
        return message.startsWith("LOGIN ");
    }

    private boolean isValidLogin(String username, String password) {
        return true;
    }
}