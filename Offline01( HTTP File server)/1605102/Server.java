import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    static final int PORT = 6789;
    static int REQUEST_COUNT = 1;

    public static void main(String[] args) throws IOException {

        ServerSocket serverConnect = new ServerSocket(PORT);

        System.out.println("Server started.");
        System.out.println("Listening for connections on port : " + PORT + " ...");

        System.out.println();

        while (true) {
            Socket socket = serverConnect.accept();
            System.out.println("Connection established.");

            Thread serverWorker = new ServerWorker(socket);
            serverWorker.start();

        }

    }
}
