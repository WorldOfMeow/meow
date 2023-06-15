import world.getmeow.Meow;

import java.nio.charset.StandardCharsets;

public class TestMeowInNewEnvironment {
    public static void main(String[] args) throws InterruptedException {
        Meow.DataSerializer<String> stringSerializer = new Meow.DataSerializer<String>() {
            @Override
            public byte[] serialize(String data) {
                return data.getBytes(StandardCharsets.US_ASCII);
            }

            @Override
            public String deserialize(byte[] bytes) {
                return new String(bytes, StandardCharsets.US_ASCII);
            }

            @Override
            public Class<String> getType() {
                return String.class;
            }
        };

        Meow.Server<Meow.ServerClient<String>, String> server = new Meow.Server<>(stringSerializer, Meow.ServerClient::new);
        server.onReceived((client, data) -> client.send(data));

        server.start(null, 800);

        Meow.Client<String> client = new Meow.Client<>(stringSerializer);
        client.setAutoReconnect(true);
        client.beforeReconnect((allow) -> {
            System.out.println("Reconnecting...");
            allow.set(true);
        });
        client.onConnected(() -> client.send("Hello Server!"));
        client.onDisconnected(() -> System.out.println("Disconnected from server!"));
        client.onReceived(System.out::println);

        client.connect("localhost", 800, 0);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                server.stop();
                Thread.sleep(7000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                server.start(null, 800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        while (true) {}
    }
}
