package world.getmeow.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import world.getmeow.netty.Meow;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class MeowHttp {
    // This is a simple wrapper for com.sun.net.httpserver.HttpServer made for Meow.
    // No guarantee for any security issues or performance. Report any issues to the Meow project.

    private HttpServer server;
    private int port;

    /**
     * @return the HttpsServer server instance.
     */
    public HttpServer getServer() {
        return server;
    }

    /**
     * @return the port of the HttpsServer.
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port of the HttpsServer.
     */
    public MeowHttp(int port) {
        this.port = port;
    }

    /**
     * HashMap of route handlers.
     */
    private HashMap<String, RouteHandler> routeHandlers = new HashMap<>();

    /**
     * @param path the path of the route.
     * @param handler the handler of the route.
     * @return the MeowHttp instance.
     */
    public MeowHttp addRouteHandler(String path, HttpHandler handler) {
        routeHandlers.put(path, (RouteHandler) handler);
        if(server != null) {
            server.createContext(path, handler);
        }
        return this;
    }

    /**
     * Starts the HttpsServer with a backlog of 0.
     */
    public MeowHttp start() throws IOException {
        start(0);
        return this;
    }

    /**
     * Starts the HttpsServer.
     * @param backlog the backlog of the HttpsServer.
     * @return the MeowHttp instance.
     */
    public MeowHttp start(int backlog) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), backlog);
        server.setExecutor(null);
        for (Map.Entry<String, RouteHandler> handler : routeHandlers.entrySet()) {
            server.createContext(handler.getKey(), handler.getValue());
        }
        Meow.getLogger().info("HttpServer started at port: " + port);
        server.start();
        return this;
    }

    /**
     * Stops the HttpsServer with a delay of 1 second.
     * @return the MeowHttp instance.
     */
    public MeowHttp stop() {
        stop(1);
        return this;
    }

    /**
     * Stops the HttpsServer.
     * @param delay the delay of the HttpsServer.
     * @return the MeowHttp instance.
     */
    public MeowHttp stop(int delay) {
        server.stop(delay);
        Meow.getLogger().info("HttpServer:" + port + " stopped.");
        return this;
    }
}

