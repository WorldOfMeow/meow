package world.getmeow.utils;

import world.getmeow.netty.Meow;

import java.nio.charset.StandardCharsets;

/**
 * This class contains some Templates and Examples for Meow.
 */
public class Templates {
    public static DataSerializer<String> stringSerializer = new DataSerializer<>() {
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
    public static Meow.Server<Meow.ServerClient<String>, String> getEchoServerTemplate() {
        Meow.Server<Meow.ServerClient<String>, String> server = new Meow.Server<>(stringSerializer, Meow.ServerClient::new);
        server.onReceived(Meow.ServerClient::send);
        return server;
    }
    public static Meow.Client<String> getEchoClientTemplate() {
        Meow.Client<String> client = new Meow.Client<>(stringSerializer);
        client.onConnected(() -> client.send("Hello Server!"));
        client.onDisconnected(() -> System.out.println("Disconnected from server!"));
        client.onReceived(System.out::println);
        return client;
    }
}
