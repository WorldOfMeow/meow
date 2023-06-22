package world.getmeow.netty;

import io.netty.bootstrap.Bootstrap;

import java.util.concurrent.atomic.AtomicBoolean;

public interface MeowClientInterface <T> {
        void beforeReconnect(Meow.Client<T> client, AtomicBoolean allow);
        void onConnected(Meow.Client<T> client);
        void beforeDisconnect(Meow.Client<T> client);
        void onReceived(Meow.Client<T> client, T data, AtomicBoolean forward);
        void onDisconnected(Meow.Client<T> client);
        void onException(Meow.Client<T> client, Throwable e);
        void beforeConnect(Meow.Client<T> client, String host, int port, long timeoutMillis);
        void onConfigured(Meow.Client<T> dClient, Bootstrap bootstrap);
}
