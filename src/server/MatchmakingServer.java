package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class Client {
    String UID;
    String uname;
    InetAddress ipaddr;
    int port;

    public Client(String id, String name, InetAddress ip, int p){
        UID = id;
        uname = name;
        ipaddr = ip;
        port = p;
    }
}

class Lobby {
    String host;
    ArrayList<Client> clients;

    public Lobby(Client host){
        this.host = host.UID;
        clients = new ArrayList<Client>();
        clients.add(host);
    }
}

public class MatchmakingServer {

    public static final ArrayList<Client> clients = new ArrayList<Client>();
    public static final HashMap<String,Lobby> lobbies = new HashMap<String,Lobby>();

    /*public static int byteToInt(Byte b){

    }*/

    public static void main(String[] args){
        try{
            DatagramSocket socket = new DatagramSocket(0);
            byte[] recv = new byte[1024];
            System.out.println("Starting UDP listener on port "+socket.getLocalPort());

            while (true) {
                // Prepare a packet to receive data
                DatagramPacket receivePacket = new DatagramPacket(recv, recv.length);
                socket.receive(receivePacket);  // Receive the data
                ByteBuffer buffer = ByteBuffer.wrap(recv);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                int header;//Byte.toUnsignedInt(b);
                byte[] str = new byte[8];
                //buffer.get(str); // every client message should contain a header and the users id
                String UID;// = new String(str, StandardCharsets.UTF_8);

                int namelen; // host messages have the username, which is the string length followed by the string
                byte[] namestr;
                String username;

                while(buffer.remaining() > 0){
                    header = buffer.get();
                    buffer.get(str); // every client message should contain a header and the users id
                    UID = new String(str, StandardCharsets.UTF_8);
                    switch(header){
                        case 0: // create a lobby
                            namelen = buffer.get(); // host messages have the username, which is the string length followed by the string
                            namestr = new byte[namelen];
                            buffer.get(namestr);
                            username = new String(namestr, StandardCharsets.UTF_8);
                            Client host = new Client(UID,username,clientAddress,clientPort);
                            lobbies.put(UID,new Lobby(host));
                        break;
                        case 1: // get list of lobbies
                            //Client client = new Client(UID,clientAddress,clientPort);
                            ByteBuffer send = ByteBuffer.allocate(1024);// = new byte[1024];
                            send.put((byte)header);
                            // this is the format of NET_GET on the client side when it receives it
                            for (Map.Entry<String, Lobby> entry : lobbies.entrySet()) {
                                //String key = entry.getKey();
                                Lobby lobby = entry.getValue();
                                send.put(lobby.host.getBytes());
                                send.put((byte)lobby.clients.get(0).uname.length());
                                send.put(lobby.clients.get(0).uname.getBytes());
                            }

                            DatagramPacket sendPacket = new DatagramPacket(send.array(), buffer.position(), clientAddress, clientPort);
                            socket.send(sendPacket);
                        break;
                        case 2: // join a lobby
                            namelen = buffer.get(); // host messages have the username, which is the string length followed by the string
                            namestr = new byte[namelen];
                            buffer.get(namestr);
                            username = new String(namestr, StandardCharsets.UTF_8);
                            Client client = new Client(UID,username,clientAddress,clientPort);

                            //next 8 bytes should be join code / host uid
                            buffer.get(str);
                            String join = new String(str, StandardCharsets.UTF_8);
                            Lobby lobby = lobbies.get(join);
                            if(lobby != null){
                                lobby.clients.add(client);
                            }
                        break;
                        case 3:
                            // start lobby
                        break;

                        default:
                            //bad header or malformed packet
                            buffer.clear();
                        break;
                    }
                }


                // Extract data and sender information
                /*String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                System.out.println("Received from client: " + receivedData);*/

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
}
