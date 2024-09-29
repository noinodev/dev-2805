import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;

public class ObjectCharacter extends PlayerObject {
    //public byte inst = 2;
    public static final String[] taunts = {"YOU SUCK","???","GONNA CRY?","LOL","AINT MEAN IF U AINT GREEN","GOBLINZ RULE","BUYING GOBLIN GF","SO GOBLINCORE","GOBLINMAXXING RN",
            "WOW...","ZZZ","STOP TRYING","IM IN JAVA?","GOBLINPILLED","GOBLIN4LIFE","...","THEY NOT LIKE US","I LOVE GRIMES"};
    public static final Color[] tauntcolours = {new Color(104,46,108), new Color(38,92,66), new Color(25,60,62), new Color(58,68,102), new Color(38,43,68), new Color(62,39,49)};
    public String txt,taunt;
    public String name;
    public Color txtcolour;
    public int chat;

    public ObjectCharacter(Game game, PlayerControlScheme pcs, int sprite, int x, int y){
        super(game,pcs);
        this.sprite = sprite;
        this.x = x;
        this.y = y;
        txt = "";
        taunt = "";
        name = "";
        txtcolour = tauntcolours[(int)(Math.random()*tauntcolours.length)];
        inst = 2;
        chat = 0;
    }

    private void setTaunt(String t){
        String[] a = {"speak3","speak4","speak5","speak6"};
        AudioManager.audio.get(a[(int)(Math.random()*3)]).play(x,y,draw.view_x+draw.view_w/2,draw.view_y+draw.view_h/2);
        taunt = t;
    }

    @Override
    public void update(){
        int vis = 0;
        if(x > game.boardx+game.board_bound_x*main.SPR_WIDTH && x < game.boardx+(game.board_bound_x+game.board_bound_w)*main.SPR_WIDTH){
            game.enemy_visible = 1;
            vis = 1;
            //draw.view_x -= (draw.view_x-(x-main.FRAMEBUFFER_W/2.))*0.01;
            //draw.view_y -= (draw.view_y-(y-main.FRAMEBUFFER_H/2.))*0.01;
        }
        if(game.state == game.STATE_PLAY){
            if(hsp != 0) xd = hsp > 0 ? 1 : -1; // set facing direction
            if(game.pointCheck(x+2*xd+hsp,y) == 1) hsp = 0; // collisions
            if(game.pointCheck(x,y+vsp) == 1) vsp = 0;
            x += hsp;
            y += vsp;
            vsp += grv; // gravity
            if(txt.length() == taunt.length()){ // taunt reset
                if(txt.length() > 0 && (int)(Math.random()*(main.TPS*2)) == 0){
                    txt = "";
                    taunt = "";
                }
            }

            switch(control_scheme){
                case PCS_LOCAL:
                    //
                    main.inputtype = 1;
                    //draw.view_x -= (draw.view_x-(x-main.FRAMEBUFFER_W/2.))*0.05;
                    //draw.view_y -= (draw.view_y-(y-main.FRAMEBUFFER_H/2.))*0.05;
                    hsp = (main.input.get(KeyEvent.VK_D)-main.input.get(KeyEvent.VK_A))*0.2;
                    if(main.input.get(KeyEvent.VK_W) == 1 && game.pointCheck(x,y+2) == 1) vsp -= 0.5;

                    if(main.gamemode == GM.GM_JOIN){
                        if(main.input.get(KeyEvent.VK_T) == 1 && chat == 0){
                            //main.keycontext = UID.hashCode();
                            main.keybuffer = "";
                            chat = 1;
                        }
                        if(chat == 1){
                            main.keycontext = main.UID.hashCode();
                            if(main.input.get(KeyEvent.VK_ENTER) == 1){
                                chat = 0;
                                main.keycontext = -1;
                                // network send
                                if(main.keybuffer.length() > 0){
                                    ByteBuffer buffer = NetworkManager.packet_start(NPH.NET_CHAT);
                                    buffer.put((byte)1);
                                    buffer.put((byte)0);
                                    buffer.put(main.UID.getBytes());
                                    buffer.put((byte)main.keybuffer.length());
                                    buffer.put(main.keybuffer.getBytes());
                                    NetworkManager.send_all(buffer);
                                }
                            }
                            taunt = main.keybuffer;
                            txt = main.keybuffer;
                        }
                    }
                    break;
                case PCS_EXTERN:
                    //
                    x -= (x-tx)*0.3;
                    y -= (y-ty)*0.3;
                    if(NetworkManager.async_load.get("game.chat."+UID) != null){
                        taunt = (String) NetworkManager.async_load.get("game.chat."+UID);
                        NetworkManager.async_load.remove("game.chat."+UID);
                    }
                    break;
                case PCS_AI:
                    //System.out.print("gobby... ");
                    if(game.state == game.STATE_LOSE || (taunt == "" && (int)(Math.random()*(main.TPS*5)) == 0) && vis == 1){
                        setTaunt(taunts[(int)(Math.random()*taunts.length)]);
                        /*String[] a = {"speak3","speak4","speak5","speak6"};
                        AudioManager.audio.get(a[(int)(Math.random()*3)]).play(x,y,draw.view_x+draw.view_w/2,draw.view_y+draw.view_h/2);
                        taunt = taunts[(int)(Math.random()*taunts.length)]; // new taunt*/
                    }

                    // run away from current tetromino
                    /*if(Math.abs(currentTetromino.dx+boardx+2*main.SPR_WIDTH-e.x) < 2*main.SPR_WIDTH) e.hsp = -(currentTetromino.dx+boardx+2*main.SPR_WIDTH-e.x)*0.01;
                    else */if((int)(Math.random()*main.TPS) == 0) hsp = -0.25+0.5*Math.random(); // wander
                    if((int)(Math.random()*main.TPS) == 0) hsp = 0;
                    break;
            }

            if(Math.abs(hsp)+Math.abs(vsp) > 0.01) change = 1;

        }

        if(taunt.length() > txt.length() && (int)(Math.random()*5) == 0){
            txt = taunt.substring(0,txt.length()+1); // taunt animation
        }

        if(game.pointCheck(x,y) == 1){ // die from being crushed
            destroy = 1;
            AudioManager.audio.get("speak3").play(x,y,draw.view_x+draw.view_w/2,draw.view_y+draw.view_h/2);
            //for(int j = 0; j < 4; j++) draw.particlePush(130,134,0.03+0.02*Math.random(),(int)x,(int)y,-0.1+0.2*Math.random(),-0.1+0.2*Math.random(), Color.WHITE);
            //draw.particlePush(150,154,0.09+0.01*Math.random(),(int)x-main.SPR_WIDTH/2,(int)y-main.SPR_WIDTH,-0.01+0.02*Math.random(),-0.08,Color.WHITE);
            for(int j = 0; j < 4; j++) CreateObject(new ObjectParticle(game,(int)x,(int)y,-0.1+0.2*Math.random(),-0.1+0.2*Math.random(),130,134,0.03+0.02*Math.random(),240,Color.WHITE));
            CreateObject(new ObjectParticle(game, (int)x-main.SPR_WIDTH/2, (int)y-main.SPR_WIDTH, -0.01+0.02*Math.random(), -0.08, 150, 154, 0.09+0.01*Math.random(), 240, Color.WHITE));
        }

        if(game.playerObject == this){
            if(Math.random() < 0.2) game.illum *= 0.8;
            draw.view_x -= (draw.view_x-(x-main.FRAMEBUFFER_W/2.))*0.05;
            draw.view_y -= (draw.view_y-(y-main.FRAMEBUFFER_H/2.))*0.05;
        }//else if(game.playerObject == null) game.playerObject = this;

        // draw self
        int bx = Math.max(Math.min((int)(Math.floor(x-game.boardx)/main.SPR_WIDTH),game.boardWidth-1),0);
        int by = Math.max(Math.min((int)(Math.floor(y-game.boardy)/main.SPR_WIDTH),game.boardHeight-1),0);

        Color c = game.getLightLocal(x,y,0);

        draw.batchPush((int)sprite+(int)(main.frame/(main.TPS/6))%2+(hsp != 0 ? 1 : 0),
                (int)x-xd*main.SPR_WIDTH/2,
                (int)y-main.SPR_WIDTH+(int)Math.abs(Math.sin((main.frame/main.TPS + id*234)*Math.PI)*Math.abs(hsp)),
                xd*main.SPR_WIDTH,main.SPR_WIDTH,c);

        if(name != ""){
            draw.batchPush(9,(int)x-10-name.length()*3,(int)y+4,name.length()*6,8,new Color(24,20,37));
            draw.drawText(name,(int)x-10,(int)y+4-(int)x%2,8,6,txtcolour,1);
        }
        // draw taunt
        if(txt != ""){
            draw.batchPush(9,(int)x-10,(int)y-18-(int)x%2,txt.length()*6,8,new Color(24,20,37));
            draw.drawText(txt,(int)x-10,(int)y-18-(int)x%2,8,6,txtcolour);
        }
    }
}
