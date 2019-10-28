import java.net.*;
import java.io.*;
import java.nio.file.Files;

import com.sun.net.httpserver.*;

public class Server {
    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(8889), 0);
        server.createContext("/", new RootHandler());
        server.setExecutor(null);
        server.start();
    }

    public static class RootHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            System.out.println(httpExchange.getRequestURI().toString());

            System.out.println(httpExchange.getAttribute("connection"));

            File file = new File("html/index.html");

            if (httpExchange.getRequestURI().equals(URI.create("/"))){
                file = new File("html/index.html");
            } else {
                file = new File(httpExchange.getRequestURI().toString().substring(1));
            }

            httpExchange.sendResponseHeaders(200, file.length());
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(file.toPath(), outputStream);
            }


        }
    }



}
