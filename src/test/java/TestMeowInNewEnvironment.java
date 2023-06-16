import world.getmeow.Meow;
import world.getmeow.Templates;

public class TestMeowInNewEnvironment {
    public static void main(String[] args) throws InterruptedException {
        Meow.DataSerializer<String> stringSerializer = Templates.stringSerializer;
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
        Meow.setLengthFieldLength(8).enableLogging().disableLogging();
    }
}
