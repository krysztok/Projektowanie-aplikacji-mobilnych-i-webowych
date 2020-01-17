import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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
        server.createContext("/newPoz", new newPozHandler());
        server.createContext("/sendPDFaPoz", new MyHandlerPoz());
        server.createContext("/mobiLogin", new MobiLoginHandler());
        server.createContext("/mobiGetPoz", new MobiGetPozHandler());
        server.createContext("/mobiGetFiles", new MobiGetFilesHandler());
        server.createContext("/mobiUpload", new MobiUploadHandler());
        server.createContext("/mobiNewPoz", new MobiNewPozHandler());
        server.createContext("/mobiDeletePoz", new MobiDeletePozHandler());
        server.createContext("/mobiDeleteFile", new MobiDeleteFileHandler());
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
                file = new File("html/notlogged.html");

            }else if(httpExchange.getRequestURI().equals(URI.create("/logout"))) {
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
                file = new File("html/notlogged.html");
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
                    String username = auth.getUsername(token);
                    file = createLogged(username);

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

            }else {
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

    public static List<String> getUserPozList(String user){
        File directory = new File("Pozycje/");
        File[] children = directory.listFiles();
        List pozList = new ArrayList();

        for(int i =0; i < children.length; i++) {
            if (children[i].toString().contains(user)){
                pozList.add(children[i].toString());
            }
        }


        return pozList;
    }

    public static List<String> getPozFileList(String pozName){
        File directory = new File("pozycjepliki/");
        File[] children = directory.listFiles();
        List pozFiles = new ArrayList();

        System.out.println(pozName);

        for(int i =0; i < children.length; i++) {
            //System.out.println(children[i]);
            //System.out.println(pozName);
            if (children[i].toString().contains(pozName)){
                pozFiles.add(children[i].toString());
            }
        }


        return pozFiles;
    }

    public static File createLogged(String username) throws IOException {
        List<String> files = getUserFilesList(username);
        File file = new File("html/logged.html");
        File fileTmp = new File("html/loginTmp.html");

        Scanner myReader = new Scanner(file);
        String body = "";
        StringBuilder sb = new StringBuilder(body);
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            if(data.compareTo("</div>") == 0){
                break;
            }
            sb.append(data);
        }

        /*pliki pdf*/
        sb.append("<ul style=\"list-style-type:square;\">");

        for(int i = 0; i < files.size(); i++){
            sb.append("<li><a href=\"");
            sb.append("http://localhost:8889/");
            sb.append(files.get(i));
            sb.append("\">");
            sb.append(files.get(i));
            sb.append("</a></li>");
        }

        sb.append("</ul>");

        /*pozycje dodanie*/
        sb.append("<hr><h1>Pozycje Bibliograficzne</h1><table><tr><td><td>nowa pozycja:</td><td><input type=\"text\" " +
                "name=\"newPoz\" id =\"newPoz\" ></td><td><button type=\"submit\" onclick=\"addPoz()\">" +
                "Dodaj</button></td></tr></table>");

        /*pozycje lista*/
        List<String> pozycje = getUserPozList(username);

        for(int i = 0; i < pozycje.size(); i++){
            sb.append("<table>");
            sb.append("<tr>");
            sb.append("<td><h2>");
            sb.append(pozycje.get(i).split("_")[1].replace("-"," "));
            sb.append("</h2></td>");
            sb.append("<td> <button type=\"submit\" onclick=\"showLogoutPage()\">Usuń</button></td>");
            sb.append("</tr>");
            sb.append("</table>");

            List<String> pozycjePliki = getPozFileList(username + pozycje.get(i).split(username)[1]);

            /*lista plikow pozycji*/
            for (int j = 0; j < pozycjePliki.size(); j++){
                System.out.println(pozycjePliki.get(0));
                //sb.append("<li>");
                sb.append("<table id =\"tab\">");
                sb.append("<tr><td>");
                sb.append("<a href=\"");
                sb.append("http://localhost:8889/");
                sb.append(pozycjePliki.get(j));
                sb.append("\">");
                sb.append(pozycjePliki.get(j).split("_")[2]);
                sb.append("</td>");

                sb.append("<td> <button type=\"submit\" onclick=\"showLogoutPage()\">Usuń</button></td>");

                sb.append("</a>");
                sb.append("</tr>");
                sb.append("</table>");
                //sb.append("</li>");
            }


            sb.append("    <form action=\"/sendPDFaPoz\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                    "<input type=\"hidden\" name=\"info\" value=\"");
            sb.append(pozycje.get(i));
            sb.append("\">" +
                    "        <input type=\"file\" name=\"nazwa\" accept=\"application/pdf\">\n" +
                    "        <input id=\"submitPDF\" type=\"submit\">\n" +
                    "    </form>");

        }


        sb.append("</div></body></html>");
        body = sb.toString();
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileTmp));
        writer.write(body);

        writer.close();
        return fileTmp;
    }


    public static class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {


            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(),"Cp1252"))) {
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
            //PrintWriter writer = new PrintWriter(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "Cp1252"));
            /*writer.print(pdfBody);
            writer.close();*/
            writer.write(pdfBody);
            writer.flush();
            writer.close();

            //System.out.println(httpExchange.getRequestHeaders().values());

            httpExchange.sendResponseHeaders(200, 0);
            File filer = new File("html/logged.html");
            filer = createLogged(auth.getUsername(token));
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(filer.toPath(), outputStream);
            }

        }
    }

    public static class MyHandlerPoz implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {


            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(),"Cp1252"))) {
                char[] buffer = new char[256];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    body.append(buffer, 0, read);
                }
            }


            String[] parsedString = body.toString().split("application/pdf");
            String pdfBody = parsedString[1];
            String pdfName = parsedString[0];
            parsedString = pdfName.split("name=\"info\"");
            String pdfPoz = parsedString[1];
            pdfPoz = pdfPoz.split("------")[0];
            pdfPoz = pdfPoz.trim();
            pdfPoz = pdfPoz.split("_")[1];
            pdfName = pdfName.split("filename")[1];
            pdfName = pdfName.substring(2);
            pdfName = pdfName.split("\"")[0];

            System.out.println(pdfName);
            pdfBody = pdfBody.replaceFirst("^\\s*", "");


            String token = httpExchange.getRequestHeaders().get("Cookie").get(0);
            /*token = token.split(";")[2];
            token = token.substring(1);*/
            Authentication auth = new Authentication();

            File file = new File ("pozycjepliki/" + auth.getUsername(token) + "_" + pdfPoz + "_" +pdfName);
            String name = auth.getUsername(token);
            System.out.println(pdfPoz);
            //file.createNewFile();
            //PrintWriter writer = new PrintWriter(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "Cp1252"));
            /*writer.print(pdfBody);
            writer.close();*/
            writer.write(pdfBody);
            writer.flush();
            writer.close();

            //System.out.println(httpExchange.getRequestHeaders().values());

            httpExchange.sendResponseHeaders(200, 0);
            File filer = new File("html/logged.html");
            filer = createLogged(auth.getUsername(token));
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(filer.toPath(), outputStream);
            }

        }
    }
    public static class newPozHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            Authentication auth = new Authentication();
            String token = httpExchange.getRequestHeaders().get("Cookie").get(0);

            StringBuilder body = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(httpExchange.getRequestBody())) {
                char[] buffer = new char[256];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    body.append(buffer, 0, read);
                }
            }

            String[] parsedString = body.toString().split("\n");
            String newPoz = parsedString[3].trim();
            newPoz = newPoz.replace(" ", "-");

            File fileNew = new File ("Pozycje/"+ auth.getUsername(token) + "_" + newPoz);
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileNew));
            writer.write(newPoz);
            writer.flush();
            writer.close();


            httpExchange.sendResponseHeaders(200, 0);
            File file = createLogged(auth.getUsername(token));
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(file.toPath(), outputStream);
            }
        }
    }

    public static class MobiLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            File file = new File("tmp/loginMobi.html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write("notLogged");

            writer.close();


            String data = httpExchange.getRequestURI().toString();

                String[] parsedString = data.split("&");
                String login = parsedString[0].split("/")[2].trim();
                String password = parsedString[1].trim();

                System.out.println(login + password);

                Authentication auth = new Authentication();


                if (auth.checkPassword(login, password) == true) {
                    String token = auth.createJWTToken(login,300000);

                    BufferedWriter writera = new BufferedWriter(new FileWriter(file));
                    writera.write("logged\n");
                    writera.write(token +"\nl" );
                    writera.close();


                }

            //file = new File("html/index.html");

            httpExchange.sendResponseHeaders(200, file.length());
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(file.toPath(), outputStream);
            }

        }
    }

    public static class MobiGetPozHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {

            File file = new File("tmp/pozMobi.html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write("empty");
            writer.close();


            String data = httpExchange.getRequestURI().toString();
            String login = data.split("/")[2].trim();
            List<String> pozycje = getUserPozList(login);

            BufferedWriter writera = new BufferedWriter(new FileWriter(file));
            writera.write("pozycje\n");
            for(int i = 0; i < pozycje.size(); i++){
                writera.write(pozycje.get(i));
                writera.write(";");
            }
            writera.write("\nl");
            writera.close();



            //File file = new File("html/index.html");

            httpExchange.sendResponseHeaders(200, file.length());
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(file.toPath(), outputStream);
            }

        }
    }

    public static class MobiNewPozHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {


            String data = httpExchange.getRequestURI().toString();
            String[] parsedString = data.split("&");
            String login = parsedString[0].split("/")[2].trim();
            String pozycja = parsedString[1].trim();


            File fileNew = new File ("Pozycje/"+ login + "_" + pozycja);
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileNew));
            writer.write(pozycja);
            writer.flush();
            writer.close();




            File file = new File("html/index.html");

            httpExchange.sendResponseHeaders(200, file.length());
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(file.toPath(), outputStream);
            }

        }
    }

    public static class MobiGetFilesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {

            File file = new File("tmp/pozMobi.html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write("empty");
            writer.close();


            String data = httpExchange.getRequestURI().toString();
            String[] parsedString = data.split("&");
            String login = parsedString[0].split("/")[2].trim();
            String pozycja = parsedString[1].trim();

            List<String> pozycje = getPozFileList(login + "_" + pozycja);

            System.out.println(pozycje.size());

            BufferedWriter writera = new BufferedWriter(new FileWriter(file));
            writera.write("pliki\n");
            for(int i = 0; i < pozycje.size(); i++){
                writera.write(pozycje.get(i));
                writera.write(";");

            }
            writera.write("\nl");
            writera.close();



            //File file = new File("html/index.html");

            httpExchange.sendResponseHeaders(200, file.length());
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(file.toPath(), outputStream);
            }

        }
    }

    public static class MobiUploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {



            System.out.println(httpExchange.getRequestURI().toString());
            String data = httpExchange.getResponseBody().toString();

            System.out.println(data);


            StringBuilder body = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(httpExchange.getRequestBody())) {
                char[] buffer = new char[256];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    body.append(buffer, 0, read);
                }
            }

            System.out.println(body.toString());

            File file = new File("html/index.html");

            httpExchange.sendResponseHeaders(200, file.length());
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(file.toPath(), outputStream);
            }

        }
    }

    public static class MobiDeletePozHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {



            String data = httpExchange.getRequestURI().toString();
            String[] parsedString = data.split("&");
            String login = parsedString[0].split("/")[2].trim();
            String pozycja = parsedString[1].trim();

            File filea = new File("Pozycje/"+ login + "_" + pozycja);
            filea.delete();


            File file = new File("html/index.html");

            httpExchange.sendResponseHeaders(200, file.length());
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(file.toPath(), outputStream);
            }

        }
    }

    public static class MobiDeleteFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {



            String data = httpExchange.getRequestURI().toString();
            String[] parsedString = data.split("&");
            String login = parsedString[0].split("/")[2].trim();
            String pozycja = parsedString[1].trim();
            String plik = parsedString[2].trim();

            System.out.println("aaaa" + "pozycjepliki/"+ login + "_" + pozycja +"_" + plik );

            File filea = new File("pozycjepliki/"+ login + "_" + pozycja +"_" + plik);
            filea.delete();


            File file = new File("html/index.html");

            httpExchange.sendResponseHeaders(200, file.length());
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                Files.copy(file.toPath(), outputStream);
            }

        }
    }

}
