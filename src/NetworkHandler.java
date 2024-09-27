//import server.Client;
//import server.Lobby;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
}

class Client {
    String UID;
    InetAddress ipaddr;
    int port;
    long timeout;
    Object agent;
    byte syn,ack;
    public Client(String uid, InetAddress ip, int p){
        UID = uid;
        ipaddr = ip;
        port = p;
        syn=0;
        ack=0;
    }
}

class Lobby {
    String uid, name;
    public Lobby(String uid, String name){
        this.uid = uid;
        this.name = name;
    }
}

public class NetworkHandler {
    public static int mmserver_port;
    public static InetAddress mmserver_ip;
    /*static {
        try {
            Scanner scan = new Scanner(new File("src/data/network.txt"));
            String ipaddr;
            if (scan.hasNextLine()) {
                ipaddr = scan.nextLine();
            }else ipaddr = "localhost";
            System.out.println("mmserver: "+ipaddr+":"+mmserver_port);
            mmserver_ip = InetAddress.getByName(ipaddr);
        } catch (UnknownHostException | FileNotFoundException e) {
            throw new RuntimeException("Failed to resolve IP address", e);
        }
    }*/

    public static final Map<String, Object> async_load = new HashMap<>();

    public static final Map<String,Client> clients = new HashMap<>();
    public static Client host;
    public static Thread networkThread;
    public static final ByteBuffer buffer_send = ByteBuffer.allocate(4096);
    public static final ByteBuffer buffer_recv = ByteBuffer.allocate(4096);
    public static Tetris2805 main;
    //public static Game game;
    //public static lobby mlobby;
    public static byte[] uidrecv = new byte[8];
    public static DatagramSocket socket;

    public static long timeout;
    public static byte disconnect;
    public static byte natpass;

    public static void networkInit(){
        try{
            mmserver_ip = InetAddress.getByName((String)Tetris2805.main.cfg.get("networkip"));
            mmserver_port = (Integer)Tetris2805.main.cfg.get("networkport");
            disconnect = 0;
            timeout = System.currentTimeMillis();
            socket = new DatagramSocket(0);
            //byte[] receiveBuffer = new byte[1024];
            System.out.println("Starting UDP listener on port "+socket.getLocalPort());
            host = null;

            while (true) {
                buffer_recv.clear();
                DatagramPacket receivePacket = new DatagramPacket(buffer_recv.array(), buffer_recv.capacity());
                socket.receive(receivePacket);  // Receive the data
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                //System.out.println("len: "+receivePacket.getLength());

                byte header;

                if(buffer_recv.remaining() > 0){
                    header = buffer_recv.get();
                    switch(header){
                        case NPH.NET_PING: {
                            timeout = System.currentTimeMillis();
                            //System.out.println("pong!");
                        } break;
                        case NPH.NET_GET: {
                            System.out.println("Receiving lobby information");
                            byte lobbycount = buffer_recv.get();
                            //if(mlobby != null){
                            byte hostnamelen;
                            byte[] hostnamestr;
                            String hostname, hostuid;
                            List<Lobby> lobbylist  = new ArrayList<>();
                            for(int i = 0; i < lobbycount; i++){
                                buffer_recv.get(uidrecv);
                                //hostnamelen = buffer_recv.get(); // host messages have the username, which is the string length followed by the string
                                //hostnamestr = new byte[hostnamelen];
                                //buffer_recv.get(hostnamestr);
                                //hostname = new String(hostnamestr, StandardCharsets.UTF_8);
                                Lobby l = new Lobby(new String(uidrecv, StandardCharsets.UTF_8), new String(uidrecv, StandardCharsets.UTF_8));
                                System.out.println(new String(uidrecv, StandardCharsets.UTF_8) + ", " + new String(uidrecv, StandardCharsets.UTF_8));
                                lobbylist.add(l);
                                //mlobby.lobbies.add(l);
                                //buffer_recv.get(hostname);
                                //username = new String(namestr, StandardCharsets.UTF_8);
                                //byte hostunamelen = buffer_recv.get();

                            }
                            if(lobbylist.size() > 0) async_load.put("mm.lobbies",lobbylist);
                            //}
                        } break;
                        case NPH.NET_HOST: {
                            System.out.println("Server ACK lobby");
                        } break;
                        case NPH.NET_JOIN: {
                            buffer_recv.get(uidrecv);
                            String uid = new String(uidrecv, StandardCharsets.UTF_8);

                            if(uid.equals(main.UID)){
                                System.out.println("ACK!");
                                async_load.put("mm.ack.join",1);
                            }else System.out.println(uid+ " joined");
                        } break;
                        case NPH.NET_START: {
                            // UDP HOLEPUNCHING
                            byte clientcount = buffer_recv.get();
                            System.out.println("attempting to start with "+clientcount+" players");
                            //List<Client> clientlist  = new ArrayList<>();
                            clients.clear();
                            for(int i = 0; i < clientcount; i++){
                                buffer_recv.get(uidrecv);
                                String uid = new String(uidrecv, StandardCharsets.UTF_8);
                                byte[] ipbyte = new byte[4];
                                buffer_recv.get(ipbyte);
                                InetAddress ip = InetAddress.getByAddress(ipbyte);
                                int port = buffer_recv.getInt();
                                System.out.println("connection: "+uid+", "+ip+", "+port);
                                //hostnamelen = buffer_recv.get(); // host messages have the username, which is the string length followed by the string
                                //hostnamestr = new byte[hostnamelen];
                                //buffer_recv.get(hostnamestr);
                                //hostname = new String(hostnamestr, StandardCharsets.UTF_8);
                                //String ipstr =
                                //Client l = new Client(uid,ip,port);

                                //ByteBuffer buffer = packet_start(NPH.NET_SYN);
                                //buffer.put(main.UID.getBytes());
                                //send(buffer,ip,port);

                                //System.out.println(new String(uidrecv, StandardCharsets.UTF_8) + ", " + new String(uidrecv, StandardCharsets.UTF_8));
                                clients.put(uid,new Client(uid,ip,port));
                                //mlobby.lobbies.add(l);
                                //buffer_recv.get(hostname);
                                //username = new String(namestr, StandardCharsets.UTF_8);
                                //byte hostunamelen = buffer_recv.get();

                            }
                            if(clients.size() > 0){
                                //int i = udpConnect(); // start holepunching protocol for all clients

                                Thread punchThread = new Thread(NetworkHandler::udpConnect);
                                punchThread.start();
                                //punchThread.join();

                                /*int count = 0;
                                for (Map.Entry<String, Client> entry : clients.entrySet()) {
                                    Client i = entry.getValue();
                                    if(i!=null&&i.syn==1&&i.ack==1) count++;
                                }

                                System.out.println("udp holepunching ret: "+count);*/
                                async_load.put("udp.connecting",1);
                                //natpass = 1;
                            }else System.out.println("host has started the game, but no clients were received?");

                        } break;
                        case NPH.NET_SYN: {
                            // holy shit it gets received!!?
                            buffer_recv.get(uidrecv);
                            String uid = new String(uidrecv, StandardCharsets.UTF_8);
                            System.out.println("SYN from "+uid);
                            clients.get(uid).syn=1;

                            ByteBuffer buffer = packet_start(NPH.NET_ACK);
                            //buffer.put(main.UID.getBytes());
                            send(buffer,clientAddress,clientPort);

                        } break;
                        case NPH.NET_ACK: {
                            buffer_recv.get(uidrecv);
                            String uid = new String(uidrecv, StandardCharsets.UTF_8);
                            clients.get(uid).ack=1;
                            System.out.println("CONNECTION SUCCESS TO "+uid);
                            async_load.put("udp.ack."+uid,1);
                        } break;

                        case NPH.NET_GAME: {

                        } break;

                        case NPH.NET_STATE: {
                            buffer_recv.get(uidrecv);

                            int dx = buffer_recv.getInt();
                            int dy = buffer_recv.getInt();
                            int index = buffer_recv.getInt();
                            int rot = buffer_recv.getInt();
                            double x = buffer_recv.getDouble();
                            double y = buffer_recv.getDouble();
                            double sprite = buffer_recv.getDouble();
                            async_load.put("game.state.dx",dx);
                            async_load.put("game.state.dy",dy);
                            async_load.put("game.state.index",index);
                            async_load.put("game.state.rot",rot);
                            async_load.put("game.state.x",x);
                            async_load.put("game.state.y",y);
                            async_load.put("game.state.sprite",sprite);
                            //System.out.println(async_load);

                            int bx = buffer_recv.get();
                            int bw = buffer_recv.get();
                            int bh = buffer_recv.get();
                            async_load.put("game.state.pos",bx);
                            async_load.put("game.state.width",bw);
                            async_load.put("game.state.height",bh);

                            //System.out.println("received a thing!" +x + " " + w + " "+h);

                            int[][] array = new int[bw][bh];
                            for (int i = 0; i < bw; i++) {
                                for (int j = 0; j < bh; j++) {
                                    array[i][j] = buffer_recv.getInt();
                                }
                            }
                            //System.out.println(array);
                            async_load.put("game.state.board",array);
                        } break;

                        case NPH.NET_OBJ: {
                            GameObject.lock = 1; // poor mans mutex
                            buffer_recv.get(uidrecv);
                            //String cuid = new String(uidrecv, StandardCharsets.UTF_8);
                            int objcount = buffer_recv.getInt();
                            for(int i = 0; i < objcount; i++){
                                buffer_recv.get(uidrecv);
                                String uid = new String(uidrecv, StandardCharsets.UTF_8);
                                byte inst = buffer_recv.get();
                                double x = buffer_recv.getDouble();
                                double y = buffer_recv.getDouble();
                                double sprite = buffer_recv.getDouble();
                                double hsp = buffer_recv.getDouble();
                                double vsp = buffer_recv.getDouble();
                                //GameObject p = GameObject.netobjects.get(uid);
                                if(!uid.equals(main.UID)){
                                    if(GameObject.netobjects.get(uid) == null){
                                        GameObject o = null;
                                        if(inst == 0){
                                            o = new ObjectResource(GameObject.g,(int)x,(int)y,0.1*Math.random(),(int)sprite,10);
                                            o.w = D2D.sprites[(int)sprite].getWidth();
                                            o.h = D2D.sprites[(int)sprite].getHeight();
                                            //System.out.println("tree/rock at: "+x+","+y);
                                        }else if(inst == 2){
                                            o = new ObjectCharacter(GameObject.g,PlayerControlScheme.PCS_EXTERN,137,140,50);
                                            System.out.println("gobby at: "+x+","+y);
                                        }
                                        else if(inst == 3) o = new ObjectTetromino(GameObject.g,PlayerControlScheme.PCS_EXTERN,(int)sprite,(int)x,(int)y,0,0);
                                        if(o != null) GameObject.syncObject(o,uid);
                                    }
                                    GameObject p = GameObject.netobjects.get(uid);
                                    if(p != null){
                                        p.tx = x;
                                        p.ty = y;
                                        if(p.inst == 0){
                                            p.x = x;
                                            p.y = y;
                                        }
                                        p.sprite = sprite;
                                        p.hsp = hsp;
                                        p.vsp = vsp;
                                        p.change = 1;
                                    }
                                }
                            }
                            GameObject.lock = 0;
                            //System.out.println("object: "+x+", "+y+", "+sprite);*/
                            //async_load.put("game.obj.x."+uid,x);

                            //handle_object(buffer_recv);
                            /*buffer_recv.get(uidrecv);
                            buffer_recv.get(uidrecv);
                            String objuid = new String(uidrecv, StandardCharsets.UTF_8);
                            byte inst = buffer_recv.get();
                            double x = buffer_recv.getDouble();
                            double y = buffer_recv.getDouble();
                            double sprite = buffer_recv.getDouble();
                            //byte hp = buffer_recv.get();
                            GameObject obj = GameObject.getNetObject(objuid);
                            if(obj == null){
                                obj = new ObjectResource(GameObject.g,(int)x,(int)y,(int)sprite,hp);
                                GameObject.syncObject(obj,objuid);
                            }else{
                                obj.x = x;
                                obj.y = y;
                                obj.sprite = sprite;
                            }*/
                        } break;
                        case NPH.NET_HIT: {

                        } break;

                        case NPH.NET_TILE: {
                            buffer_recv.get(uidrecv);
                            int x = buffer_recv.getInt();
                            int y = buffer_recv.getInt();
                            int val = buffer_recv.getInt();
                            GameObject.g.board[x][y] = val;
                        } break;

                        default:
                            //pretty much just skip through irrelevant packets
                            System.out.println("somethin aint right");
                        break;
                    }
                    //byte i;
                    //while(buffer_recv.remaining() > 0) i = buffer_recv.get();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void udpConnect(){
        long time = System.currentTimeMillis();
        long steptime = System.currentTimeMillis();
        int count = 0;
        while(System.currentTimeMillis()-time < 5000){
            count = 0;
            if(System.currentTimeMillis()-steptime > 200){
                steptime = System.currentTimeMillis();
                for (Map.Entry<String, Client> entry : clients.entrySet()) {
                    Client i = entry.getValue();
                    if(i != null && i.ack == 0){
                        ByteBuffer buffer = packet_start(NPH.NET_SYN);
                        send(buffer,i.ipaddr,i.port);
                    }
                    if(i!=null&&i.syn==1&&i.ack==1) count++;
                }
                if(count == clients.size()){
                    async_load.put("udp.success",1);
                    break;
                }
            }
        }
        //return count-clients.size(); // negative if couldnt establish all connections
    }

    public static void udpMaintain(){

    }

    public static void startNetworkThread() {
        networkThread = new Thread(NetworkHandler::networkInit);
        networkThread.start();
    }

    public static void stopNetworkThread(){
        try{
            networkThread.join();
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void send(/*DatagramPacket packet*/ByteBuffer buffer, InetAddress ip, int p){
        DatagramPacket send = new DatagramPacket(buffer.array(), buffer.position(), ip, p);
        try {
            socket.send(send);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void send_all(ByteBuffer buffer){
        try {
            for (Map.Entry<String, Client> entry : clients.entrySet()) {
                Client i = entry.getValue();
                if(i != null && i.UID != main.UID){
                    DatagramPacket send = new DatagramPacket(buffer.array(), buffer.position(), i.ipaddr, i.port);
                    socket.send(send);
                }
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer packet_start(byte header){
        return packet_start(header,main.UID);
    }
    public static ByteBuffer packet_start(byte header, String uid){
        buffer_send.clear();
        buffer_send.put(header);
        buffer_send.put(uid.getBytes());
        return buffer_send;
    }

    public static void handle_object(ByteBuffer buffer){

    }

    public static String generateUID(){
        String UID = "AAAAAAAA";
        StringBuilder newUID = new StringBuilder(UID.length());
        for (int i = 0; i < UID.length(); i++) {
            char randomChar = (char) ('A' + (int) (Math.random() * ('Z' - 'A' + 1)));
            newUID.append(randomChar);
        }
        return newUID.toString();
    }

    //public static void packet_send(Data
}