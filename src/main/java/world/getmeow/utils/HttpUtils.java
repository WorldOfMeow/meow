package world.getmeow.utils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * This class contains some Utilities for HttpServer.
 * Not properly tested and documented.
 * @since 1.0.5
 */
public class HttpUtils {
    /**
     * This method creates a response.
     */
    public static void sendResponse(HttpExchange httpExchangeString, String response) {
        try {
            OutputStream os = httpExchangeString.getResponseBody();
            httpExchangeString.sendResponseHeaders(200, response.getBytes().length);
            os.write(response.getBytes());
            os.close();
        } catch (IOException e) {
            throw new RuntimeException("Error while sending response: " + e.getMessage());
        }
    }

    /**
     * This method creates a default response with CORS enabled using a status code and response.
     */
    public static void createDefaultCorsStringResponse(HttpExchange httpExchange, int statusCode, String response) throws IOException {
        Headers headers = httpExchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Content-Type", ContentType.HTML.type);
        httpExchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    /**
     * This method creates a default response with status code, response, contentType and  CORS enabled/disabled.
     */
    public static void createResponse(HttpExchange httpExchange, int statusCode, String response, String contentType, boolean cors) throws IOException {
        Headers headers = httpExchange.getResponseHeaders();
        if (cors) {
            headers.add("Access-Control-Allow-Origin", "*");
        }
        if (contentType != null) {
            headers.add("Content-Type", contentType);
        }
        httpExchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    /**
     * This method creates a default response with a status code, response, custom Headers, contentType and CORS enabled/disabled.
     */
    public static void createResponseWithHeaders(HttpExchange httpExchange, int statusCode, String response, Headers headers, String contentType, boolean cors) throws IOException {
        httpExchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    /**
     * This method returns a file with CORS enabled.
     */
    public static void returnFile(HttpExchange httpExchange, String filePath, String contentType) throws IOException {
        Headers headers = httpExchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Content-Type", contentType);
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        httpExchange.sendResponseHeaders(200, fileBytes.length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(fileBytes);
        os.close();
    }

    /**
     * Enum for common Content Types.
     */
    public enum ContentType {
        HTML("text/html"),
        CSS("text/css"),
        JS("text/javascript"),
        PLAIN("text/plain"),
        JPEG("image/jpeg"),
        PNG("image/png"),
        GIF("image/gif"),
        JSON("application/json"),
        XML("application/xml"),
        PDF("application/pdf"),
        MP3("audio/mpeg"),
        WAV("audio/x-wav"),
        MPEG("video/mpeg"),
        MP4("video/mp4"),
        CSV("text/csv"),
        RTF("application/rtf"),
        TIFF("image/tiff"),
        ICO("image/x-icon"),
        BMP("image/bmp"),
        ZIP("application/zip");

        private final String type;

        ContentType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    /**
     * RequestType enum for common HTTP Request Types.
     */
    public enum RequestType {
        GET,
        POST,
        PUT,
        DELETE;

        /**
         * This method checks if the RequestType is the same as the HTTP Request Method.
         */
        public boolean isType(HttpExchange exchange) {
            Objects.requireNonNull(exchange, "HTTP exchange cannot be null");
            return this.name().equalsIgnoreCase(exchange.getRequestMethod());
        }
    }
}