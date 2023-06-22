package world.getmeow.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

class LoginClientHandler extends SimpleChannelInboundHandler<String> {
    private final String password;
    private final String username;

    public LoginClientHandler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.equals("login.successful")) {
            ctx.pipeline().remove(this);
        } else {
            ctx.close();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String loginRequest = "LOGIN " + username + " " + password;
        ctx.writeAndFlush(loginRequest);
    }
}