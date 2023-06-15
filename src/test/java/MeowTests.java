import world.getmeow.MeowClient;
import world.getmeow.MeowServer;

public class MeowTests {
    public void testMeowServer() throws Exception {
        MeowServer server = new MeowServer(8080);
        server.run();
    }
    public void testMeowClient() throws Exception {
        MeowClient client = new MeowClient("localhost", 8080);
        client.run();
    }
}
