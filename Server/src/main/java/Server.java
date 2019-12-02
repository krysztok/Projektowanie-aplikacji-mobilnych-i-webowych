import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sun.net.httpserver.*;
import redis.clients.jedis.Jedis;

public class Server {
    public static void main(String[] args) throws IOException {

        //pobranie użytych loginów
        loadUsersList();

        HttpServer server = HttpServer.create(new InetSocketAddress(8889), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/sendPDFa", new MyHandler());
        server.setExecutor(null);
        server.start();
    }

    public static class RootHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            System.out.println(httpExchange.getRequestURI().toString());
            System.out.println(httpExchange.getAttribute("connection"));

            File file = new File("html/index.html");


            if (httpExchange.getRequestURI().equals(URI.create("/login"))) {
                System.out.println("login");
                StringBuilder body = new StringBuilder();
                try (InputStreamReader reader = new InputStreamReader(httpExchange.getRequestBody())) {
                    char[] buffer = new char[256];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        body.append(buffer, 0, read);
                    }
                }

                String[] parsedString = body.toString().split("\n");
                String login = parsedString[3].trim();
                String password = parsedString[7].trim();

                Authentication auth = new Authentication();

                if (auth.checkPassword(login, password) == true) {
                    String token = auth.createJWTToken(login,300000);
                    Headers responseHeader = httpExchange.getResponseHeaders();
                    List<String> values = new ArrayList<>();
                    values.add(token);
                    responseHeader.put("Set-Cookie", values);
                    System.out.println("Token: ");
                    System.out.println(token);
                }

            } else if (httpExchange.getRequestURI().equals(URI.create("/html/notlogged.html"))) {
                Authentication auth = new Authentication();

                String tokenOld = httpExchange.getRequestHeaders().get("Cookie").get(0);
                String [] tokens = tokenOld.split(";");
                tokenOld = tokens[tokens.length-1];
                tokenOld = tokenOld.replaceFirst("^\\s*", "");
                //tokenOld = tokenOld.substring(1);

                String token = auth.createJWTToken(auth.getUsername(tokenOld),-300000);
                Headers responseHeader = httpExchange.getResponseHeaders();
                List<String> values = new ArrayList<>();
                values.add(token);
                responseHeader.put("Set-Cookie", values);
                file = new File("html/login.html");

            }else if (httpExchange.getRequestURI().equals(URI.create("/html/logged.html"))) {
                Authentication auth = new Authentication();
                System.out.println("token z req");

                //wyciecie tokena z ciasteczka
                String token = httpExchange.getRequestHeaders().get("Cookie").get(0);
                String [] tokens = token.split(";");
                token = tokens[tokens.length-1];
                token = token.replaceFirst("^\\s*", "");
                //token = token.substring(1);

                System.out.println(token);
                if (auth.verifyJWTToken(token)) {
                    file = new File("html/logged.html");
                } else {
                    file = new File("html/notlogged.html");
                }

            } else if (httpExchange.getRequestURI().equals(URI.create("/sendPDF"))) {
                System.out.println("wysylanie:");
                System.out.println(httpExchange.getRequestHeaders());
                file = new File("html/logged.html");
            } else if (httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Headers requestHeaders = httpExchange.getRequestHeaders();
                System.out.println("atatatatat");
                StringBuilder body = new StringBuilder();
                try (InputStreamReader reader = new InputStreamReader(httpExchange.getRequestBody())) {
                    char[] buffer = new char[256];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        body.append(buffer, 0, read);
                    }
                }

                //System.out.println(body.toString());
                register(body.toString());
            } else if (httpExchange.getRequestURI().equals(URI.create("/"))) {
                //file = new File("html/index.html");
                file = new File("html/login.html");
            } else {
                file = new File(httpExchange.getRequestURI().toString().substring(1));
            }

            httpExchange.sendResponseHeaders(200, file.length());
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(file.toPath(), outputStream);
            }
        }

        public void register(String string) throws IOException {
            loadUsersList();
            String[] parsedString = string.split("\n");
            String firstname = parsedString[3].trim();
            String lastname = parsedString[7].trim();
            String login = parsedString[11].trim();
            String password = parsedString[15].trim();
            String email = parsedString[19].trim();
            String birthday = parsedString[23].trim();
            String gender = parsedString[27].trim();

            StringBuilder stringBuilder = new StringBuilder("aaa");
            stringBuilder.append(login);
            stringBuilder.append(lastname);

            System.out.print(stringBuilder.toString());

            Jedis jedis = new Jedis("192.168.99.100", 9001);
            //check whether server is running or not
            System.out.println("Server is running: " + jedis.ping());
            jedis.set("tutorial-name", "Redis tutorial");
            //jedis.lpush(login, firstname, lastname, password, email, birthday, gender);
            jedis.hset(login, "name", firstname);
            jedis.hset(login, "lastname", lastname);
            jedis.hset(login, "password", password);
            jedis.hset(login, "email", email);
            jedis.hset(login, "birthday", birthday);
            jedis.hset(login, "gender", gender);

            jedis.sadd("nicknames", login);

            // Get the stored data and print it
            System.out.println(login);
            System.out.println("Stored string in redis:: " + jedis.hget(login, "name"));
            loadUsersList();
        }
    }

    public static void loadUsersList() throws IOException {
        System.out.println("tutej");
        Jedis jedis = new Jedis("192.168.99.100", 9001);
        Set<String> nicknames = jedis.smembers("nicknames");
        System.out.println(nicknames);

        File file = new File("tmp/users");
        file.createNewFile();
        PrintWriter writer = new PrintWriter(file);
        writer.print("");

        for (String nick : nicknames) {
            writer.print(nick);
            writer.print("\n");
        }

        writer.close();
    }

    public static List<String> getUserFilesList(String user){
        File directory = new File("pdf/");
        File[] children = directory.listFiles();
        List filesList = new ArrayList();

        for(int i =0; i < children.length; i++) {
            if (children[i].toString().contains(user)){
                filesList.add(children[i].toString());
            }
        }

        System.out.println(filesList);

        return filesList;
    }


    public static class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {


            StringBuilder body = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(httpExchange.getRequestBody())) {
                char[] buffer = new char[256];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    body.append(buffer, 0, read);
                }
            }


            String[] parsedString = body.toString().split("application/pdf");
            String pdfBody = parsedString[1];
            String pdfName = parsedString[0];
            pdfName = pdfName.split("filename")[1];
            pdfName = pdfName.substring(2);
            pdfName = pdfName.split("\"")[0];

            System.out.println(pdfName);
            pdfBody = pdfBody.replaceFirst("^\\s*", "");


            String token = httpExchange.getRequestHeaders().get("Cookie").get(0);
            /*token = token.split(";")[2];
            token = token.substring(1);*/
            Authentication auth = new Authentication();

            File file = new File ("pdf/"+ auth.getUsername(token) + "_" + pdfName);
            //file.createNewFile();
            PrintWriter writer = new PrintWriter(file);
            writer.print(pdfBody);
            writer.close();


            //System.out.println(httpExchange.getRequestHeaders().values());

            httpExchange.sendResponseHeaders(200, 0);
            File filer = new File("html/logged.html");
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(filer.toPath(), outputStream);
            }

        }
    }


}
