package world.getmeow.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.SocketChannel;

import java.util.concurrent.atomic.AtomicBoolean;

public interface MeowServerInterface<T> {
    void onConnected(Meow.ServerClient<T> client);
    void onStop();
    void onReceived(Meow.ServerClient<T> client, T data, AtomicBoolean forward);
    void onDisconnected(Meow.ServerClient<T> client);
    void onException(Meow.ServerClient<T> client, Throwable e);
    void onConfigured(ServerBootstrap bootstrap);
    void onChannelInitialized(SocketChannel channel);
    void beforeStart(Meow.Server<Meow.ServerClient<T>, T> server);
}
