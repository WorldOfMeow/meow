import world.getmeow.Meow;

import java.nio.charset.StandardCharsets;

public class TestSimpleNettyInNewEnvironment {
    public static void main(String[] args) throws InterruptedException {
        //A serializer, deserializer for the type of the transmitted data: String
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

//Instantiate and initialize a new Server.
//ServerClient is the class which is bound to all connected clients.
        Meow.Server<Meow.ServerClient<String>, String> server = new Meow.Server<>(stringSerializer, Meow.ServerClient::new);
        server.onReceived((client, data) -> client.send(data));

//Bind the server to no specified address, but to the port 800.
        server.start(null, 800);

//Instantiate and initialize a new Client.
        Meow.Client<String> client = new Meow.Client<>(stringSerializer);
        client.onConnected(() -> client.send("Hello Server!"));
        client.onReceived(System.out::println);

//Connect to the localhost address on the port 800 without any timeout.
        client.connect("localhost", 800, 0);
        while (true) {}
    }
}
