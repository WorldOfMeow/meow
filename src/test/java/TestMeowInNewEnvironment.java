import com.sun.net.httpserver.HttpExchange;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.SocketChannel;
import world.getmeow.http.MeowHttp;
import world.getmeow.http.RouteHandler;
import world.getmeow.netty.Meow;
import world.getmeow.netty.MeowClientInterface;
import world.getmeow.netty.MeowServerInterface;
import world.getmeow.utils.DataSerializer;
import world.getmeow.utils.HttpUtils;
import world.getmeow.utils.Templates;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestMeowInNewEnvironment {
    public static void main(String[] args) throws InterruptedException {
        Meow.enableLogging();
        new Thread(() -> {
            MeowHttp meowHttp = new MeowHttp(8090);
            try {
                meowHttp.addRouteHandler("/hello", new RouteHandler() {
                    @Override
                    public void handle(HttpExchange httpExchange) {
                        HttpUtils.sendResponse(httpExchange, "Hello, world!");
                    }
                }).addRouteHandler("/", new RouteHandler() {
                    @Override
                    public void handle(HttpExchange httpExchange) {
                        HttpUtils.sendResponse(httpExchange, "Home page test!");
                    }
                })
                        .start()
                        .addRouteHandler("/bye", new RouteHandler() {
                    @Override
                    public void handle(HttpExchange httpExchange) {
                        meowHttp.stop();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            meowHttp.stop();
        }).start();
        DataSerializer<String> stringSerializer = Templates.stringSerializer;
        Meow.Server<Meow.ServerClient<String>, String> server = new Meow.Server<>(stringSerializer, Meow.ServerClient::new);
        MeowServerInterface<String> meowServerInterface = new MeowServerInterface<>() {
            Meow.Server<Meow.ServerClient<String>, String> server;
            @Override
            public void onConnected(Meow.ServerClient<String> client) {
                System.out.println("Client connected!");
            }

            @Override
            public void onStop() {
                System.out.println("Server stopped!");
            }

            @Override
            public void onReceived(Meow.ServerClient<String> client, String data, AtomicBoolean forward) {
                forward.set(false); // Interrupted the forwarding of the data to other listeners.
                client.send(data);
            }

            @Override
            public void onDisconnected(Meow.ServerClient<String> client) {
                System.out.println("Client disconnected!");
            }

            @Override
            public void onException(Meow.ServerClient<String> client, Throwable e) {
                System.out.println("Exception occurred!");
            }

            @Override
            public void onConfigured(ServerBootstrap bootstrap) {
                System.out.println("Server configured!");
            }

            @Override
            public void onChannelInitialized(SocketChannel channel) {
                System.out.println("Client connecting...");
            }

            @Override
            public void beforeStart(Meow.Server<Meow.ServerClient<String>, String> server) {
                this.server = server;
                System.out.println("Server is starting...");
            }
        };

        server.addInterface(meowServerInterface);

        server.onReceived((client, data) -> {
            System.out.println("Received data: " + data);
        });

        server.start(null, 800);

        Meow.Client<String> client = new Meow.Client<>(stringSerializer);
        client.setAutoReconnect(true);
        client.beforeReconnect((allow) -> {
            System.out.println("Reconnecting...");
            allow.set(true);
        });

        client.addInterface(new MeowClientInterface<>() {
            @Override
            public void beforeReconnect(Meow.Client<String> client, AtomicBoolean allow) {
                System.out.println("Reconnecting...");
            }

            @Override
            public void onConnected(Meow.Client<String> client) {
                System.out.println("Connected to server!");
            }

            @Override
            public void beforeDisconnect(Meow.Client<String> client) {
                System.out.println("Disconnecting from server...");
            }

            @Override
            public void onReceived(Meow.Client<String> client, String data, AtomicBoolean forward) {
                System.out.println("Received data: " + data);
            }

            @Override
            public void onDisconnected(Meow.Client<String> client) {
                System.out.println("Disconnected from server!");
            }

            @Override
            public void onException(Meow.Client<String> client, Throwable e) {
                System.out.println("Exception occurred!");
            }

            @Override
            public void beforeConnect(Meow.Client<String> client, String host, int port, long timeoutMillis) {
                System.out.println("Connecting to server...");
            }

            @Override
            public void onConfigured(Meow.Client<String> dClient, Bootstrap bootstrap) {
                System.out.println("Client configured!");
            }
        });

        client.onConnected(() -> client.send("Hello Server!"));
        client.onDisconnected(() -> System.out.println("Disconnected from server!"));
        client.onReceived(System.out::println);

        client.connect("localhost", 800, 0);

        Thread.sleep(3000);

        client.disconnect();
        server.stop();
    }
}
