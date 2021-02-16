import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

//Done!
public class NetworkUtility {
    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;



    public void print(){
        System.out.println(socket.getLocalAddress().toString());
        System.out.println(socket.getInetAddress().toString());
        System.out.println(socket.getRemoteSocketAddress().toString());
        System.out.println(socket.getLocalSocketAddress().toString());
    }

    public SocketAddress getRemoteSocketAddress(){return socket.getRemoteSocketAddress();}
    public InetAddress getInetAddress(){return socket.getInetAddress();}

    public NetworkUtility(String s, int port){
        try {
            this.socket = new Socket(s, port);
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            //socket.setSoTimeout(3 * 1000);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NetworkUtility(Socket socket){
        try {
            this.socket = socket;
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object read(){
        Object object = null;
        try {
            object = objectInputStream.readObject();
        } catch (SocketTimeoutException e) {
            return "Timed out haha";
        }catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return object;
    }

    public void write(Object object){
        try {
            objectOutputStream.writeObject(object);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection(){
        try {
            objectInputStream.close();
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
