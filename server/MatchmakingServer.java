import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.rmi.server.UID;
import java.util.*;

class NPH { //NetworkPacketHeader
    public static final byte NET_HOST = 0; // send to matchmaking server to start a new lobby
    public static final byte NET_GET=1;
    public static final byte NET_JOIN=2; // send to matchmaking server to get list of active lobbies
    public static final byte NET_START=3;
    public static final byte NET_PING=4; // keepalive packet
    public static final byte NET_GAME=5; // handshake packet containing client info to establish UDP connections
    public static final byte NET_EVENT=6; // game event, such as a player hitting something or a tetromino merging
    public static final byte NET_STATE=7; // probably board state updates
    public static final byte NET_OBJ=8; // dynamic network synchronized objects, sent from host to clients
    public static final byte NET_DISCONNECT=9;
    public static final byte NET_KICK=10;
    public static final byte NET_ACK=11;
    public static final byte NET_SYN=12;
    public static final byte NET_HIT=13;
    public static final byte NET_TILE=14;
    public static final byte NET_CHAT=15;
    public static final byte NET_NAME=15;
}

// class for client entries on server side
class SClient {
    String UID;
    String LUID;
    InetAddress ipaddr;
    int port;
    long timeout;

    public SClient(String id,/* String name,*/ InetAddress ip, int p){
        UID = id;
        ipaddr = ip;
        port = p;
        timeout = System.currentTimeMillis();
    }
}

// class for lobby entries on server side. maintains list of clients
class SLobby {
    String host;
    Map<String, SClient> clients;
    byte punch;

    public SLobby(SClient h){
        this.host = h.UID;
        clients = new HashMap<>();
        clients.put(h.UID,h);
        punch = 0;
    }
}

public class MatchmakingServer {
    public static final Map<String, SClient> clients = new HashMap<>();
    public static final HashMap<String, SLobby> lobbies = new HashMap<String, SLobby>();
    public static final ByteBuffer buffer_recv = ByteBuffer.allocate(1024);
    public static final ByteBuffer buffer_send = ByteBuffer.allocate(1024);
    public static DatagramSocket socket;
    public static Thread networkThread;
    public static int lock;
    public static int wait;
    public static final SLobby[] queue = new SLobby[64];
    public static int lobbycount;

    // send a buffer to 1 client
    public static void send(ByteBuffer send, InetAddress ip, int p){
        DatagramPacket packet = new DatagramPacket(send.array(), send.position(), ip, p);
        try{
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // listen for incoming udp packets in a loop
    public static void listener(){
        try {
            System.out.println("Starting UDP matchmaking server on port "+socket.getLocalPort());

            while (true) {
                // Prepare a packet to receive data
                buffer_recv.clear();
                DatagramPacket receivePacket = new DatagramPacket(buffer_recv.array(), buffer_recv.capacity());
                socket.receive(receivePacket);  // Receive the data

                lock = 1; // idiot mutex because im lazy

                // extract ip and port from packet
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                int header;
                byte[] str = new byte[8];
                String UID;

                // start packet handling
                if(buffer_recv.remaining() > 0){
                    // deserialize common, mandatory data, like UID and header
                    header = buffer_recv.get();
                    buffer_recv.get(str); // every client message should contain a header and the users id
                    UID = new String(str, StandardCharsets.UTF_8);

                    if(clients.get(UID) == null){
                        clients.put(UID,new SClient(UID,clientAddress,clientPort));
                        System.out.println("new client: "+UID);
                    }
                    SClient client = clients.get(UID);

                    switch(header){
                        // i hate java so much
                        // fake programming language
                        // what do you mean each case is in the scope of the entire switch????????????????????????
                        // i gotta use {} in a switch??
                        case NPH.NET_PING: {
                            // set keepalive for lobby+user combo
                            buffer_recv.get(str);
                            String uidlobby = new String(str, StandardCharsets.UTF_8);
                            SLobby l = lobbies.get(uidlobby);
                            if(l != null){
                                SClient c = l.clients.get(UID);
                                if(c != null) c.timeout = System.currentTimeMillis();
                            }
                            client.timeout = System.currentTimeMillis();

                            buffer_send.clear();
                            buffer_send.put(NPH.NET_PING);
                            send(buffer_send,clientAddress,clientPort);

                        } break;
                        case NPH.NET_HOST: { // create a lobby
                            client.LUID = UID;
                            queue[lobbycount++] = new SLobby(client); // append lobby to work queue buffer, for thread safety
                            System.out.println("new lobby created for "+UID);

                            buffer_send.clear();
                            buffer_send.put(NPH.NET_HOST);
                            send(buffer_send,clientAddress,clientPort);
                        } break;
                        case NPH.NET_GET: { // get list of lobbies
                            buffer_send.clear();
                            buffer_send.put(NPH.NET_GET);
                            // this is the format of NET_GET on the client side when it receives it
                            int lc = 0;
                            // get lobby count, because not all entries are valid
                            for (Map.Entry<String, SLobby> entry : lobbies.entrySet()) {
                                SLobby lobby = entry.getValue();
                                String lobbyuid = entry.getKey();
                                if(lobby != null && clients.get(lobbyuid) != null) lc++;
                            }
                            // serialize lobbies
                            buffer_send.put((byte)lc);
                            for (Map.Entry<String, SLobby> entry : lobbies.entrySet()) {
                                SLobby lobby = entry.getValue();
                                String lobbyuid = entry.getKey();
                                if(lobby != null && clients.get(lobbyuid) != null) buffer_send.put(lobbyuid.getBytes());
                            }
                            // send to client who asked
                            send(buffer_send,clientAddress,clientPort);
                            System.out.println("sent lobby information to "+UID);
                        } break;
                        case NPH.NET_JOIN: { // join a lobby
                            //next 8 bytes should be join code / host uid
                            buffer_recv.get(str);
                            String join = new String(str, StandardCharsets.UTF_8);
                            SLobby lobby = lobbies.get(join);
                            if(lobby != null){
                                // lobby request is valid
                                // add client to lobby, notify other clients in lobby
                                lobby.clients.put(client.UID,client);
                                client.LUID = join;
                                System.out.println("added "+UID+" to lobby "+join);

                                buffer_send.clear();
                                buffer_send.put(NPH.NET_JOIN);
                                buffer_send.put(UID.getBytes());

                                System.out.print("told: ");
                                for (Map.Entry<String, SClient> entry : lobby.clients.entrySet()) {
                                    SClient i = entry.getValue();
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
                            SLobby lobby = lobbies.get(UID);
                            if(lobby != null){
                                lock = 1;
                                lobby.punch=10;
                                buffer_send.clear();
                                buffer_send.put(NPH.NET_START);

                                // send list of IPs and ports to the host

                                byte clientcount = 0;
                                for (Map.Entry<String, SClient> entry : lobby.clients.entrySet()) {
                                    if (entry.getValue() != null) {
                                        clientcount++;
                                    }
                                }
                                buffer_send.put((byte)(clientcount-1));
                                System.out.println("CLIENTS: " + clientcount);
                                for (Map.Entry<String, SClient> entry : lobby.clients.entrySet()) {
                                    SClient i = entry.getValue();
                                    if(i != null && !UID.equals(i.UID)){
                                        System.out.println(i.UID + ", " + i.ipaddr.toString() + ", " + i.port);
                                        buffer_send.put(i.UID.getBytes());
                                        buffer_send.put(i.ipaddr.getAddress());
                                        buffer_send.putInt(i.port);
                                    }
                                }

                                send(buffer_send,clientAddress,clientPort); // send server list of ips


                                // send host IP and port to the clients
                                buffer_send.clear();
                                buffer_send.put(NPH.NET_START);
                                buffer_send.put((byte)1);
                                buffer_send.put(UID.getBytes());
                                buffer_send.put(clientAddress.getAddress());
                                buffer_send.putInt(clientPort);

                                for (Map.Entry<String, SClient> entry : lobby.clients.entrySet()) {
                                    SClient i = entry.getValue();
                                    if(i != null && !Objects.equals(i.UID, UID)){
                                        // send clients server ip
                                        send(buffer_send,i.ipaddr,i.port);
                                    }
                                }
                                lock = 0;
                            }else System.out.println("lobby null?");
                        } break;

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
        lobbycount = 0;
        lock = 0;
        // bind socket
        try{
            socket = new DatagramSocket(22565);
            networkThread = new Thread(() -> listener());
            networkThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // asynchronous control, timeout etc
        ByteBuffer buffer_async = ByteBuffer.allocate(1024);
        buffer_async.clear();
        buffer_async.put(NPH.NET_PING);

        while(true){
            long time = System.currentTimeMillis();

            while(lobbycount > 0) lobbies.put(queue[--lobbycount].host,queue[lobbycount]);

            if(lock == 0 && lobbies.size() > 0){
                Iterator<Map.Entry<String, SLobby>> lobbyIterator = lobbies.entrySet().iterator();
                while (lobbyIterator.hasNext()) {
                    Map.Entry<String, SLobby> le = lobbyIterator.next();
                    SLobby lobby = le.getValue();
                    if(lobby != null){
                        if(clients.get(lobby.host) == null){
                            lobbyIterator.remove();
                        }else{
                            Iterator<Map.Entry<String, SClient>> clientIterator = lobby.clients.entrySet().iterator();
                            while (clientIterator.hasNext()) {
                                Map.Entry<String, SClient> ce = clientIterator.next();
                                SClient i = ce.getValue();

                                // timeout mechanism
                                if (time - i.timeout > 10000) {
                                    System.out.println(ce.getKey() + " timed out");
                                    lobbies.put(i.UID,null);
                                    clientIterator.remove();
                                }
                                // UDP holepunching mechanism, this is trying to open user ports
                                if(Objects.equals(i.LUID, lobby.host) && lobby.punch > 0){
                                    send(buffer_async,i.ipaddr,i.port);
                                    lobby.punch--;
                                }
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
