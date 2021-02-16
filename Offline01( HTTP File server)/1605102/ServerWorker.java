import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;


public class ServerWorker extends Thread {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    // Constructor
    ServerWorker(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    private void showHomePage() throws IOException {
        File file = new File("index.html");
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }

        String content = stringBuilder.toString();

        String header = "HTTP/1.1 200 OK\r\n" +
                "Server: Java HTTP Server: 1.0\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + content.length() + "\r\n" +
                "\r\n";

        out.write(header);
        out.write(content);
        out.flush();

    }

    private void showFilePage(String dirPath, FileWriter fileWriter) throws IOException {
        File file = new File("filePage.html");
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }
        File f = new File(dirPath);

        File[] files = f.listFiles();
        if (files != null) {
            for (File file1 : files) {
                if (file1.isDirectory()) {
                    String link = "http://localhost:" + socket.getLocalPort() + "/" + file1.getAbsolutePath();
                    link = link.replaceAll("\\\\", "/");
                    link = link.replaceAll("\\s", "%20");
                    System.out.println(link);
                    stringBuilder.append(" <a style=\"font-weight:bold\" href=\"").append(link).append("\">").append(file1.getName()).append("</a>").append("<br>").append("\n");
                    stringBuilder.append("\n");
                } else {
                    String link = "http://localhost:" + socket.getLocalPort() + "/" + file1.getAbsolutePath();
                    link = link.replaceAll("\\\\", "/");
                    link = link.replaceAll("\\s", "%20");
                    stringBuilder.append(" <a href=\"").append(link).append("\">").append(file1.getName()).append("</a>").append("<br>").append("\n");
                    stringBuilder.append("\n");
                }
            }
        }

        stringBuilder.append("</body>");
        stringBuilder.append("\n");
        stringBuilder.append("</html>");


        String content = stringBuilder.toString();

        String header = "HTTP/1.1 200 OK\r\n" +
                "Server: Java HTTP Server: 1.0\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + content.length() + "\r\n" +
                "\r\n";

        out.write(header);
        out.write(content);
        out.flush();


        fileWriter.write(header);
        fileWriter.write(content);
    }

    private void showNullPage(FileWriter fileWriter) throws IOException {
        File file = new File("filePage.html");
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }

        stringBuilder.append("<h1>Error 404: Page not found</h1>\n");

        stringBuilder.append("</body>");
        stringBuilder.append("\n");
        stringBuilder.append("</html>");


        String content = stringBuilder.toString();

        String header = "HTTP/1.1 404 NOT FOUND\r\n" +
                "Server: Java HTTP Server: 1.0\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + content.length() + "\r\n" +
                "\r\n";

        out.write(header);
        out.write(content);
        out.flush();

        fileWriter.write(header);
        fileWriter.write(content);

        System.out.println("Error 404: Page not found");


    }

    private void sendFile(String dirPath, FileWriter fileWriter) {

        FileInputStream fis;
        BufferedInputStream bis;
        OutputStream os;
        BufferedOutputStream bos;
        try {
            Path source = Paths.get(dirPath);
            String MIMEType = Files.probeContentType(source);

            File input = new File(dirPath);

            String header = "HTTP/1.1 200 OK\r\n" +
                    "Server: Java HTTP Server: 1.0\r\n" +
                    "Date: " + new Date() + "\r\n" +
                    "Content-Type: " + MIMEType + "\r\n" +
                    "Content-Length: " + input.length() + "\r\n" +
                    "\r\n";

            out.write(header);
            out.flush();

            fileWriter.write(header);

            fis = new FileInputStream(input);
            bis = new BufferedInputStream(fis);
            os = socket.getOutputStream();
            bos = new BufferedOutputStream(os);
            byte[] buffer = new byte[1024];
            int data;
            while (true) {
                data = bis.read(buffer);
                if (data != -1) {
                    bos.write(buffer, 0, 1024);
                    bos.flush();
                } else {
                    bis.close();
                    bos.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void run() {
        try {
            String input = in.readLine();
//            System.out.println(input);

            if (input == null || input.replaceAll("\\s++", "").length() == 0) {
                throw new Exception("Null input provided");
            } else if (!input.startsWith("GET")) {
                if (input.startsWith("Error:")) {
                    System.out.println(input);
                } else {

                    DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    FileOutputStream fos = new FileOutputStream(input);

                    int count;
                    byte[] bytes = new byte[1024];


                    while ((count = dataInputStream.read(bytes)) > 0)
                    {
                        fos.write(bytes, 0, count);
                        System.out.println("...");
                    }

                    fos.close();

                }

            } else {
                System.out.println(input);
                String logFile = "httpLog" + Server.REQUEST_COUNT + ".txt";
                Server.REQUEST_COUNT++;
                File log = new File(logFile);
                log.createNewFile();

                FileWriter fileWriter = new FileWriter(logFile);
                fileWriter.write("Request : \n");
                fileWriter.write(input + "\n\n");
                fileWriter.write("Response : \n");


                String[] inputs = input.split("\\s");
                if ("/".equals(inputs[1])) {
                    showHomePage();
                } else {
                    String path = inputs[1].substring(1);

                    path = path.replaceAll("%20", " ");

                    File file = new File(path);
                    if (!file.exists()) {
                        showNullPage(fileWriter);
                    } else {
                        if (file.isFile()) {
                            sendFile(path, fileWriter);
                        } else if (file.isDirectory()) {
                            showFilePage(path, fileWriter);
                        }
                    }

                    fileWriter.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Request served.");

    }
}
