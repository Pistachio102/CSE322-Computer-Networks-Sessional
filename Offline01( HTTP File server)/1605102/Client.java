import java.io.IOException;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", Server.PORT);
        System.out.println("Connection established.");

        Thread clientWorker = new ClientWorker(socket);
        clientWorker.start();
    }
}
