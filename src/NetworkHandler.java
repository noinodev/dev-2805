import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

enum NPH { //NetworkPacketHeader
    NET_HOST, // send to matchmaking server to start a new lobby
    NET_GET,
    NET_JOIN, // send to matchmaking server to get list of active lobbies
    NET_START,
    NET_PING, // keepalive packet
    NET_SHAKE, // handshake packet containing client info to establish UDP connections
    NET_EVENT, // game event, such as a player hitting something or a tetromino merging
    NET_STATE, // probably board state updates
    NET_OBJ, // dynamic network synchronized objects, sent from host to clients
    NET_DISCONNECT,
    NET_KICK
}

class Client {
    String UID;
    InetAddress ipaddr;
    int port;
    long timeout;
    Object agent;
}

public class NetworkHandler {
    public static final ArrayList<Client> clients = new ArrayList<Client>();
    public static Thread networkThread;
    public static void networkInit(){
        try{
            DatagramSocket socket = new DatagramSocket(0);
            byte[] receiveBuffer = new byte[1024];
            System.out.println("Starting UDP listener on port "+socket.getLocalPort());

            while (true) {
                // Prepare a packet to receive data
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);  // Receive the data

                // Extract data and sender information
                String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                System.out.println("Received from client: " + receivedData);

                // Send a response
                String response = "Hello from server!";
                byte[] sendBuffer = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
                socket.send(sendPacket);  // Send response back to client
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void startNetworkThread() {
        networkThread = new Thread(() -> networkInit());
        networkThread.start();
    }

    public static void stopNetworkThread(){
        try{
            networkThread.join();
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}