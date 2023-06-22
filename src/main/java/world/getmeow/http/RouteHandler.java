package world.getmeow.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class RouteHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            httpExchange.sendResponseHeaders(404, 0);
            httpExchange.getResponseBody().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
