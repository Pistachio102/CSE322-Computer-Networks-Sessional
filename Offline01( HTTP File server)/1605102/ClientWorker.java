import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientWorker extends Thread {
    private final Socket socket;
    private final PrintWriter out;

    // Constructor
    ClientWorker(Socket socket) throws IOException {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void run() {
        try {
            while (true) {

                Scanner input = new Scanner(System.in);
                String fileName = input.nextLine();

                File file = new File(fileName);

                if (!file.exists()) {

                    String errorMessage = "Error: " + fileName + " doesn't exist.\r\n";
                    System.out.println(errorMessage);
                    out.write(errorMessage);
                    out.flush();

                } else {
                    out.write(file.getName() + "\r\n");
                    out.flush();
                    FileInputStream fis;
                    BufferedInputStream bis;
                    OutputStream os;
                    BufferedOutputStream bos;

                    fis = new FileInputStream(file);
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
                    System.out.println("File Sent");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
