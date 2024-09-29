//import Lobby;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

abstract class scene { // scene base class
    protected Tetris2805 main;
    protected D2D draw;

    public scene(Tetris2805 m, D2D d){
        main = m;
        draw = d;
    }

    public void loop(){}
}

class splash extends scene { // splash screen
    private int time;
    public splash(Tetris2805 m, D2D d) {
        super(m, d);
        time = 0;
        D2D.clearColour = new Color(24,20,37);
        main.sceneIndex = 0;
    }
    @Override
    public void loop(){
        time++;
        if(time > main.TPS) main.currentScene = new menu(main,draw);
        draw.batchPush(43,0,0,main.FRAMEBUFFER_W,main.FRAMEBUFFER_H);

        //draw.batchPush(9,0,0,main.FRAMEBUFFER_W,main.FRAMEBUFFER_H,new Color(24,20,37));
        draw.drawText("TETRIS",main.FRAMEBUFFER_W/2,main.FRAMEBUFFER_H/2-20,10,10,new Color(time,time,time),1);
        draw.drawText("JAVA GAME",main.FRAMEBUFFER_W/2,main.FRAMEBUFFER_H/2-10,8,6,new Color(time/2,time/2,time/2),1);
    }
}

class menu extends scene { // main menu
    private int time;
    public menu(Tetris2805 m, D2D d) {
        super(m, d);
        time = 0;
        D2D.clearColour = new Color(24,20,37);
        main.sceneIndex = 1;
    }
    @Override
    public void loop(){
        if(time < main.TPS) time++;
        double a = time/main.TPS;
        main.bgtx = (main.frame*0.4);

        draw.drawText("TETRIS",20,20,10,8,new Color((int)(255*a),(int)(255*a),(int)(255*a)));
        draw.drawText("JAVA GAME BY NATHAN BURG",20,30,8,6,new Color((int)(255*(a/2)),(int)(255*(a/2)),(int)(255*(a/2))));

        int bx = 20;//main.FRAMEBUFFER_W/2-100;
        if(a > 0.2 && draw.drawButton("SINGLEPLAYER",bx,40,80,10) == 1){
            main.gamemode = GM.GM_OFFLINE;
            main.currentScene = new Game(main,draw);
        }
        if(a > 0.4 && draw.drawButton("MULTIPLAYER",bx,51,80,10) == 1) main.currentScene = new MenuLobby(main,draw);
        if(a > 0.6 && draw.drawButton("CONFIGURE",bx,62,80,10) == 1) main.currentScene = new config(main,draw);
        if(a > 0.8 && draw.drawButton("HIGHSCORE",bx,73,80,10) == 1) main.currentScene = new hscore(main,draw);
        if(a >= 1 && draw.drawButton("EXIT",bx,84,80,10) == 1) main.displayconfirm = main.DIALOG_CONTEXT_EXIT;
    }

}

class config extends scene { // config menu
    private int time;
    private int menu;
    public config(Tetris2805 m, D2D d) {
        super(m, d);
        time = 0;
        D2D.clearColour = new Color(24,20,37);
        main.sceneIndex = 2;
        menu = 0;
    }
    @Override
    public void loop(){
        main.bgtx = 400+main.frame*0.1;
        if(time < main.TPS) time++;
        double a = time/main.TPS;
        draw.drawText("CONFIGURE",20,20,10,8,new Color((int)(255*a),(int)(255*a),(int)(255*a)));
        if(menu == 0) draw.drawText("GAME",21,31,8,6,new Color(38,43,68));
        else if(draw.drawButton("GAME",20,30,80,10) == 1) menu = 0;
        if(menu == 1) draw.drawText("VIDEO",101,31,8,6,new Color(38,43,68));
        else if(draw.drawButton("VIDEO",100,30,80,10) == 1) menu = 1;
        if(menu == 2) draw.drawText("USER",181,31,8,6,new Color(38,43,68));
        else if(draw.drawButton("USER",180,30,80,10) == 1) menu = 2;

        switch(menu){
            case 0:
                main.cfg.put("width",draw.drawSlider("BOARD WIDTH",20,30+10,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("width"),5,15));
                main.cfg.put("height",draw.drawSlider("BOARD HEIGHT",20,30+10*2,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("height"),15,30));
                main.cfg.put("level",draw.drawSlider("LEVEL",20,30+10*3,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("level"),0,10));
                main.cfg.put("light",draw.drawSlider("DARKNESS",20,30+10*4,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("light"),0,10));

                main.cfg.put("music",draw.drawToggle("MUSIC",20,30+10*5,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("music")));
                main.cfg.put("sound",draw.drawToggle("SFX",20,30+10*6,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("sound")));
                main.cfg.put("extend",draw.drawToggle("GOBLIN MODE",20,30+10*7,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("extend")));
                main.cfg.put("ai",draw.drawToggle("AI MODE",20,30+10*8,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("ai")));
                //main.cfg.put("profiler",draw.drawToggle("PROFILER",20,30+10*6,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("profiler")));
            break;
            case 1:
                main.cfg.put("profiler",draw.drawToggle("PROFILER",20,30+10,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("profiler")));
                main.cfg.put("video.colouring",draw.drawToggle("SPRITE COLOURING",20,30+10*2,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("video.colouring")));
                main.cfg.put("video.particles",draw.drawToggle("PARTICLES",20,30+10*3,main.FRAMEBUFFER_W-40,10,(Integer)main.cfg.get("video.particles")));
            break;
            case 2:
                String uname = (String)main.cfg.get("username");
                String name = draw.drawTextfield("USERNAME",uname,20,30+10*3,main.FRAMEBUFFER_W-40,10);
                if(name != "") main.cfg.put("username",name.replace(" ",""));

                String uip = (String)main.cfg.get("networkip");
                String ip = draw.drawTextfield("SERVER HOSTNAME",uip,20,30+10,main.FRAMEBUFFER_W-40,10);
                if(ip != "") main.cfg.put("networkip",ip);

                String uport = ((Integer)main.cfg.get("networkport")).toString();
                String port = draw.drawTextfield("SERVER PORT",uport,20,30+10*2,main.FRAMEBUFFER_W-40,10);
                if(port != "") main.cfg.put("networkport",Integer.getInteger(port));
            break;
        }

        /*String uname = (String)main.cfg.get("username");
        String name = draw.drawTextfield("USERNAME",uname,20,30+10*9,main.FRAMEBUFFER_W-40,10);
        if(name != "") main.cfg.put("username",name.replace(" ",""));

        String uip = (String)main.cfg.get("networkip");
        String ip = draw.drawTextfield("SERVER HOSTNAME",uip,20,30+10*10,main.FRAMEBUFFER_W-40,10);
        if(ip != "") main.cfg.put("networkip",ip);

        String uport = ((Integer)main.cfg.get("networkport")).toString();
        String port = draw.drawTextfield("SERVER PORT",uport,20,30+10*11,main.FRAMEBUFFER_W-40,10);
        if(port != "") main.cfg.put("networkport",Integer.getInteger(port));*/

        if(draw.drawButton("APPLY",20,30+10*14,80,10) == 1) Tetris2805.saveData(main.cfg,"src/data/config.json",ParseFormat.JSON);
        if(draw.drawButton("RESET",20,30+10*13,80,10) == 1) main.cfg = Tetris2805.loadData("src/data/config_def.json",ParseFormat.JSON);

        if(draw.drawButton("BACK",20,main.FRAMEBUFFER_H-20,80,10) == 1) main.currentScene = new menu(main,draw);
    }

}

// TODO Visisble gobbies heuristic??????

class hscore extends scene { // leaderboard menu
    private int time;
    private ArrayList<Map.Entry<String, Object>> list;
    public hscore(Tetris2805 m, D2D d) {
        super(m, d);
        time = 0;
        D2D.clearColour = new Color(24,20,37);
        // sorted score list
        list = new ArrayList<>(main.scores.entrySet());
        list.sort((entry1, entry2) -> { // comparator sort
            Integer value1 = (Integer) entry1.getValue();
            Integer value2 = (Integer) entry2.getValue();
            return value2.compareTo(value1);
        });
        main.sceneIndex = 3;
    }
    @Override
    public void loop(){
        main.bgtx = 800+main.frame*0.1;
        if(time < main.TPS) time++;
        double a = time/main.TPS;
        draw.drawText("HIGHSCORES",20,20,10,8,new Color((int)(255*a),(int)(255*a),(int)(255*a)));
        // draw leaderboard
        int i = 0;
        for(Map.Entry<String, Object> entry : list){
            int c = (int)(255*(a/(2+i)));
            draw.drawText(entry.getKey() + " " + entry.getValue(),20,30+8*i,8,6,new Color(c,c,c));
            i++;
        }
        if(draw.drawButton("BACK",20,main.FRAMEBUFFER_H-20,80,10) == 1) main.currentScene = new menu(main,draw);
    }

}

class MenuLobby extends scene { // config menu
    private int time;
    String currentlobby, trylobby;
    public ArrayList<Lobby> lobbies;// = new ArrayList<>();
    public MenuLobby(Tetris2805 m, D2D d) {
        super(m, d);
        //NetworkHandler.mlobby = this;
        lobbies = new ArrayList<>();

        time = 0;
        D2D.clearColour = new Color(24,20,37);
        main.sceneIndex = 4;
    }
    @Override
    public void loop(){
        main.bgtx = 400+main.frame*0.1;
        if(time < main.TPS) time++;
        double a = time/main.TPS;
        draw.drawText("MULTIPLAYER",20,20,10,8,new Color((int)(255*a),(int)(255*a),(int)(255*a)));

        if(NetworkManager.async_load.get("udp.connecting") != null){
            //main.currentScene = new Game(main,draw);
            /*System.out.println("NAT TRAVERSE STATUS:");
            int count = 0;
            for (Map.Entry<String, Client> entry : NetworkHandler.clients.entrySet()) {
                Client i = entry.getValue();
                if(i!=null&&i.syn==1&&i.ack==1) count++;
                System.out.println("uid: "+i.UID+" syn: "+i.syn+" ack: "+i.ack);
            }

            System.out.println("udp holepunching ret: "+count+"/"+NetworkHandler.clients.size());*/
            if(NetworkManager.async_load.get("udp.success") != null){
                NetworkManager.async_load.remove("udp.success");
                System.out.println("NAT traversal success");
                if(main.gamemode == GM.GM_HOST){
                    ByteBuffer buffer = NetworkManager.packet_start(NPH.NET_GAME);
                    for (Map.Entry<String, Client> entry : NetworkManager.clients.entrySet()) {
                        Client i = entry.getValue();
                        if(i!=null){
                            NetworkManager.send(buffer,i.ipaddr,i.port);
                        }
                    }
                    //main.currentScene = new Game(main,draw);
                    // send game settings to all clients and start game
                    // when clients receive, start game
                }
                NetworkManager.async_load.remove("udp.connecting");
                NetworkManager.async_load.remove("udp.success");
                main.currentScene = new Game(main,draw);
            }
        }else{
            switch(main.gamemode){
                case GM.GM_HOST:
                    // tell matchmaking server that it want to start a lobby
                    // if there are 2 or more players in the lobby, let user start the game
                    // when starting, tell server to attempt handshake
                    // wait for server to call handshake
                    // if confirmed and all clients in list validate handshake, start game

                    // send initial packet to server
                    if(main.gamemode_last != main.gamemode){
                        main.gamemode_last = main.gamemode;
                        //initialize telling the server that you are hosting
                        ByteBuffer buffer = NetworkManager.packet_start(NPH.NET_HOST);
                        NetworkManager.send(buffer, NetworkManager.mmserver_ip, NetworkManager.mmserver_port);
                    }

                    if((int)main.frame%main.TPS == 0){
                        ByteBuffer buffer = NetworkManager.packet_start(NPH.NET_PING);
                        NetworkManager.send(buffer, NetworkManager.mmserver_ip, NetworkManager.mmserver_port);
                    }

                    if(draw.drawButton("START GAME",20,30+10*2,80,10) == 1){
                        ByteBuffer buffer = NetworkManager.packet_start(NPH.NET_START);
                        NetworkManager.send(buffer, NetworkManager.mmserver_ip, NetworkManager.mmserver_port);
                    }
                break;
                case GM.GM_JOIN:
                    // tell matchmaking server that it wants to join a lobby
                    // wait for list of lobbies or enter game code
                    // join a game
                    // wait for server to call handshake
                    // attempt handshake until timeout (tell static method to do this)
                    // when host confirms, start game and start handling game packets
                    if(main.gamemode_last != main.gamemode){
                        main.gamemode_last = main.gamemode;
                        ByteBuffer buffer = NetworkManager.packet_start(NPH.NET_GET);

                        NetworkManager.send(buffer, NetworkManager.mmserver_ip, NetworkManager.mmserver_port);
                    }

                    if(trylobby == null){

                        //ArrayList<Lobby> list = (ArrayList<Lobby>) NetworkHandler.async_load.get("mm.lobbies");
                        if(NetworkManager.async_load.get("mm.lobbies") != null){
                            // yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck yuck
                            lobbies.clear();
                            lobbies.addAll((ArrayList<Lobby>) NetworkManager.async_load.get("mm.lobbies"));
                            NetworkManager.async_load.remove("mm.lobbies");
                        }

                        int i = 0;
                        //System.out.print("\nlobbylist: ");
                        for(Lobby l : lobbies){
                            if(l != null){
                                //System.out.print(l.uid+", ");
                                if(draw.drawButton(l.uid,30,50+8*i,80,10) == 1){
                                    ByteBuffer buffer = NetworkManager.packet_start(NPH.NET_JOIN);
                                    //buffer.put((byte)main.UID.length());
                                    //buffer.put(main.UID.getBytes());
                                    buffer.put(l.uid.getBytes());

                                    NetworkManager.send(buffer, NetworkManager.mmserver_ip, NetworkManager.mmserver_port);
                                    trylobby = l.uid;
                                }
                                i++;
                            }
                        }
                    }else if(currentlobby != trylobby){
                        draw.drawText("WAITING FOR SERVER TO RESPOND",30,50,8,6,Color.GRAY);
                        //int ack = (String) NetworkHandler.async_load.get("mm.join.ack");
                        if(NetworkManager.async_load.get("mm.ack.join") != null){
                            NetworkManager.async_load.remove("mm.ack.join");
                            currentlobby = trylobby; // join ack
                        }
                    }else{
                        if((int)main.frame%main.TPS == 0){
                            ByteBuffer buffer = NetworkManager.packet_start(NPH.NET_PING);
                            buffer.put(main.UID.getBytes());

                            NetworkManager.send(buffer, NetworkManager.mmserver_ip, NetworkManager.mmserver_port);
                        }
                        draw.drawText("WAITING FOR HOST",30,50,10,10,Color.MAGENTA);
                    }
                break;
                default:
                    if(draw.drawButton("HOST GAME",20,30+10*2,80,10) == 1){
                        main.gamemode = GM.GM_HOST;
                        //main.currentScene = new host(main,draw);
                    }
                    if(draw.drawButton("JOIN GAME",20,30+10*3,80,10) == 1){
                        main.gamemode = GM.GM_JOIN;
                        //ByteBuffer buffer = NetworkHandler.buffer_send;
                        //buffer.clear();
                        //buffer.put(NPH.NET_JOIN);
                        //main.currentScene = new join(main,draw);
                    }
                break;
            }
        }

        if(draw.drawButton("BACK",20,main.FRAMEBUFFER_H-20,80,10) == 1){
            main.gamemode = GM.GM_OFFLINE;
            main.currentScene = new menu(main,draw);
        }
    }

}
