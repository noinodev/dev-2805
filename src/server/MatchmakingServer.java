package server;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

class NPH { //NetworkPacketHeader
    public static final byte NET_HOST = 0; // send to matchmaking server to start a new lobby
    public static final byte NET_GET=1;
    public static final byte NET_JOIN=2; // send to matchmaking server to get list of active lobbies
    public static final byte NET_START=3;
    public static final byte NET_PING=4; // keepalive packet
    public static final byte NET_ACK=5; // handshake packet containing client info to establish UDP connections
    public static final byte NET_EVENT=6; // game event, such as a player hitting something or a tetromino merging
    public static final byte NET_STATE=7; // probably board state updates
    public static final byte NET_OBJ=8; // dynamic network synchronized objects, sent from host to clients
    public static final byte NET_DISCONNECT=9;
    public static final byte NET_KICK=10;
}

class Client {
    String UID;
    String LUID;
    //String uname;
    InetAddress ipaddr;
    int port;
    long timeout;

    public Client(String id,/* String name,*/ InetAddress ip, int p){
        UID = id;
        //uname = name;
        ipaddr = ip;
        port = p;
        timeout = System.currentTimeMillis();
    }
}

class Lobby {
    String host;
    Map<String,Client> clients;
    byte punch;

    public Lobby(Client h){
        this.host = h.UID;
        clients = new HashMap<>();
        clients.put(h.UID,h);
        punch = 0;
    }
}

public class MatchmakingServer {

    public static final Map<String,Client> clients = new HashMap<>();
    public static final HashMap<String,Lobby> lobbies = new HashMap<String,Lobby>();
    public static final ByteBuffer buffer_recv = ByteBuffer.allocate(1024);
    public static final ByteBuffer buffer_send = ByteBuffer.allocate(1024);
    public static DatagramSocket socket;
    public static Thread networkThread;
    public static int lock;

    public static void send(ByteBuffer send, InetAddress ip, int p){
        //System.out.println("len: "+packet.getLength());
        DatagramPacket packet = new DatagramPacket(send.array(), send.position(), ip, p);
        try{
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*public static int byteToInt(Byte b){

    }*/
    public static void listener(){
        try {
            System.out.println("Starting UDP matchmaking server on port "+socket.getLocalPort());

            while (true) {
                // Prepare a packet to receive data
                buffer_recv.clear();
                DatagramPacket receivePacket = new DatagramPacket(buffer_recv.array(), buffer_recv.capacity());
                socket.receive(receivePacket);  // Receive the data
                //System.out.println("recevied packet");
                //ByteBuffer buffer = ByteBuffer.wrap(recv);

                lock = 1;

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                //System.out.println(""+clientAddress.toString()+":"+clientPort);

                int header;//Byte.toUnsignedInt(b);
                byte[] str = new byte[8];
                //buffer.get(str); // every client message should contain a header and the users id
                String UID;// = new String(str, StandardCharsets.UTF_8);

                int namelen; // host messages have the username, which is the string length followed by the string
                byte[] namestr;
                String username;

                if(buffer_recv.remaining() > 0){
                    header = buffer_recv.get();
                    buffer_recv.get(str); // every client message should contain a header and the users id
                    UID = new String(str, StandardCharsets.UTF_8);

                    if(clients.get(UID) == null){
                        clients.put(UID,new Client(UID,clientAddress,clientPort));
                        System.out.println("new client: "+UID);
                    }
                    Client client = clients.get(UID);



                    //System.out.println("header: " + header + ", " + UID);

                    switch(header){
                        // i hate java so much
                        // fake programming language
                        // what do you mean each case is in the scope of the entire switch????????????????????????
                        // i gotta use {} in a switch??
                        case NPH.NET_PING: {
                            // set keepalive for lobby+user combo
                            buffer_recv.get(str);
                            String uidlobby = new String(str, StandardCharsets.UTF_8);
                            /*Lobby l = lobbies.get(uidlobby);
                            if(l != null){
                                Client c = l.clients.get(UID);
                                if(c != null){
                                    c.timeout = System.currentTimeMillis();
                                    //System.out.println("reset timer on "+c.UID);
                                }
                            }*/
                            client.timeout = System.currentTimeMillis();

                            buffer_send.clear();
                            buffer_send.put(NPH.NET_PING);
                            //buffer_send.put(NPH.NET_PING);
                            //DatagramPacket sendPacket = new DatagramPacket(buffer_send.array(), buffer_send.position(), clientAddress, clientPort);
                            send(buffer_send,clientAddress,clientPort);

                        } break;
                        case NPH.NET_HOST: { // create a lobby
                            /*namelen = buffer_recv.get(); // host messages have the username, which is the string length followed by the string
                            namestr = new byte[namelen];
                            buffer_recv.get(namestr);
                            username = new String(namestr, StandardCharsets.UTF_8);*/
                            //Client host = new Client(UID,clientAddress,clientPort);
                            lobbies.put(UID,new Lobby(client));
                            client.LUID = UID;
                            System.out.println("new lobby created for "+UID);

                            buffer_send.clear();
                            buffer_send.put(NPH.NET_HOST);
                            send(buffer_send,clientAddress,clientPort);
                        } break;
                        case NPH.NET_GET: { // get list of lobbies
                            //Client client = new Client(UID,clientAddress,clientPort);
                            //ByteBuffer send = ByteBuffer.allocate(1024);// = new byte[1024];
                            buffer_send.clear();
                            buffer_send.put(NPH.NET_GET);
                            // this is the format of NET_GET on the client side when it receives it
                            int lc = 0;
                            for (Map.Entry<String, Lobby> entry : lobbies.entrySet()) {
                                Lobby lobby = entry.getValue();
                                String lobbyuid = entry.getKey();
                                if(lobby != null && clients.get(lobbyuid) != null) lc++;
                            }
                            buffer_send.put((byte)lc);
                            for (Map.Entry<String, Lobby> entry : lobbies.entrySet()) {
                                Lobby lobby = entry.getValue();
                                String lobbyuid = entry.getKey();
                                if(lobby != null && clients.get(lobbyuid) != null) buffer_send.put(lobbyuid.getBytes());
                                //buffer_send.put((byte)lobby.clients.get(0).uname.length());
                                /////////////buffer_send.put(lobby.clients.get(0).uname.getBytes());
                            }

                            send(buffer_send,clientAddress,clientPort);
                            System.out.println("sent lobby information to "+UID);
                        } break;
                        case NPH.NET_JOIN: { // join a lobby
                            /*namelen = buffer_recv.get(); // host messages have the username, which is the string length followed by the string
                            namestr = new byte[namelen];
                            buffer_recv.get(namestr);
                            username = new String(namestr, StandardCharsets.UTF_8);*/

                            //next 8 bytes should be join code / host uid
                            buffer_recv.get(str);
                            String join = new String(str, StandardCharsets.UTF_8);
                            Lobby lobby = lobbies.get(join);
                            if(lobby != null){
                                lobby.clients.put(client.UID,client);
                                client.LUID = join;
                                System.out.println("added "+UID+" to lobby "+join);

                                buffer_send.clear();
                                buffer_send.put(NPH.NET_JOIN);
                                buffer_send.put(UID.getBytes());

                                System.out.print("told: ");
                                /*for(Client i : lobby.clients){
                                    if(i.UID != UID){
                                        DatagramPacket sendPacket = new DatagramPacket(buffer_send.array(), buffer_send.position(), i.ipaddr, i.port);
                                        send(sendPacket);
                                        System.out.print(i.UID+", ");
                                    }
                                }*/
                                for (Map.Entry<String, Client> entry : lobby.clients.entrySet()) {
                                    Client i = entry.getValue();
                                    if(i != null/* && !Objects.equals(i.UID, UID)*/){
                                        send(buffer_send,i.ipaddr,i.port);
                                        System.out.print(i.UID+", ");
                                    }
                                }
                                System.out.println();
                            }else System.out.println("lobby "+join+" does not exist");

                            buffer_send.clear();
                        } break;
                        case NPH.NET_START: {
                            // start lobby
                            System.out.println("attempting to start "+UID);
                            Lobby lobby = lobbies.get(UID);
                            if(lobby != null){
                                lobby.punch=10;
                                //Client host = lobby.clients.get(0);
                                //if(clientAddress == client.ipaddr && clientPort == client.port){
                                    //ByteBuffer send = ByteBuffer.allocate(1024);// = new byte[1024];
                                    buffer_send.clear();
                                    buffer_send.put(NPH.NET_START);

                                    byte clientcount = 0;
                                    for (Map.Entry<String, Client> entry : lobby.clients.entrySet()) {
                                        if (entry.getValue() != null) {
                                            clientcount++;
                                        }
                                    }
                                    buffer_send.put((byte)(clientcount-1));
                                    System.out.println("CLIENTS: " + clientcount);
                                    for (Map.Entry<String, Client> entry : lobby.clients.entrySet()) {
                                        Client i = entry.getValue();
                                        if(i != null && !UID.equals(i.UID)){
                                            System.out.println(i.UID + ", " + i.ipaddr.toString() + ", " + i.port);
                                            buffer_send.put(i.UID.getBytes());
                                            /*buffer_send.put((byte)i.uname.length());
                                            buffer_send.put(i.uname.getBytes());*/
                                            //buffer_send.put((byte)i.ipaddr.toString().length());
                                            buffer_send.put(i.ipaddr.getAddress());
                                            buffer_send.putInt(i.port);
                                        }
                                    }

                                    send(buffer_send,clientAddress,clientPort); // send server list of ips

                                    buffer_send.clear();
                                    buffer_send.put(NPH.NET_START);
                                    buffer_send.put((byte)1);
                                    buffer_send.put(UID.getBytes());
                                    //buffer_send.put((byte)clientAddress.toString().length());
                                    buffer_send.put(clientAddress.getAddress());
                                    buffer_send.putInt(clientPort);

                                    for (Map.Entry<String, Client> entry : lobby.clients.entrySet()) {
                                        Client i = entry.getValue();
                                        if(i != null && !Objects.equals(i.UID, UID)){
                                            // send clients server ip
                                            send(buffer_send,i.ipaddr,i.port);
                                        }
                                    }
                                //}else System.out.println("start from wrong address?");
                            }else System.out.println("lobby null?");
                        } break;

                        /*case NPH.NET_ACK: {
                            if(lobbies.get(UID) != null){
                                lobbies.remove(UID);
                                System.out.println(UID+ " finished UDP holepunching, server not needed anymore");
                            }
                        } break;*/

                        default: {
                            //bad header or malformed packet
                            System.out.println("something aint right server");
                            buffer_recv.clear();
                        } break;
                    }
                }

                lock = 0;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args){
        lock = 0;
        try{
            socket = new DatagramSocket(22565);
            //byte[] recv = new byte[1024];
            //System.out.println("Starting UDP matchmaking server on port "+socket.getLocalPort());

            networkThread = new Thread(() -> listener());
            networkThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ByteBuffer buffer_async = ByteBuffer.allocate(1024);
        buffer_async.clear();
        buffer_async.put(NPH.NET_PING);

        while(true){
            long time = System.currentTimeMillis();
            if(lock == 0 && lobbies.size() > 0){
                Iterator<Map.Entry<String, Lobby>> lobbyIterator = lobbies.entrySet().iterator();
                while (lobbyIterator.hasNext()) {
                    Map.Entry<String, Lobby> le = lobbyIterator.next();
                    Lobby lobby = le.getValue();
                //llfor (Map.Entry<String, Lobby> le : lobbies.entrySet()) {
                    //String key = entry.getKey();
                    //Lobby lobby = le.getValue();
                    if(lobby != null){
                        Iterator<Map.Entry<String, Client>> clientIterator = lobby.clients.entrySet().iterator();
                        while (clientIterator.hasNext()) {
                            Map.Entry<String, Client> ce = clientIterator.next();
                            Client i = ce.getValue();

                            if (time - i.timeout > 10000) {
                                System.out.println(ce.getKey() + " timed out");
                                lobbies.put(i.UID,null);
                                clientIterator.remove(); // Remove using the iterator
                            }
                            if(Objects.equals(i.LUID, lobby.host) && lobby.punch > 0){
                                send(buffer_async,i.ipaddr,i.port);
                                lobby.punch--;
                            }
                        }
                    }
                }
            }
            try{
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
