import java.awt.*;
import java.awt.event.KeyEvent;

public class ObjectCharacter extends PlayerObject {
    //public byte inst = 2;
    public static final String[] taunts = {"YOU SUCK","???","GONNA CRY?","LOL","AINT MEAN IF U AINT GREEN","GOBLINZ RULE","BUYING GOBLIN GF","SO GOBLINCORE","GOBLINMAXXING RN",
            "WOW...","ZZZ","STOP TRYING","IM IN JAVA?","GOBLINPILLED","GOBLIN4LIFE","...","THEY NOT LIKE US","I LOVE GRIMES"};
    public static final Color[] tauntcolours = {new Color(104,46,108), new Color(38,92,66), new Color(25,60,62), new Color(58,68,102), new Color(38,43,68), new Color(62,39,49)};
    public String txt,taunt;
    public Color txtcolour;

    public ObjectCharacter(Game game, PlayerControlScheme pcs, int sprite, int x, int y){
        super(game,pcs);
        this.sprite = sprite;
        this.x = x;
        this.y = y;
        txt = "";
        taunt = "";
        txtcolour = tauntcolours[(int)(Math.random()*tauntcolours.length)];
        inst = 2;
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

                    /*int mx = (int)Math.floor((main.mousex-game.posx)/main.SPR_WIDTH), my = (int)((main.mousey-game.posy)/main.SPR_WIDTH);
                    draw.batchPush(155,game.posx+mx*main.SPR_WIDTH,game.posy+my*main.SPR_WIDTH,main.SPR_WIDTH,main.SPR_WIDTH);
                    if(mx != tbx || my != tby){
                        tilebreak = 0;
                        tbx = mx;
                        tby = my;
                    }
                    if(main.input.get(-1) > 0){
                        if(mx < 0 || mx >= game.boardWidth || my < 0 || my >= game.boardHeight || (game.board[mx][my] > 0)){
                            tilebreak += 1/(main.TPS*2.);
                            if(tilebreak > 0) draw.batchPush(142+(int)(5*tilebreak),game.posx+mx*main.SPR_WIDTH,game.posy+my*main.SPR_WIDTH,main.SPR_WIDTH,main.SPR_WIDTH);
                            if(tilebreak >= 1){
                                game.board[mx][my] = 0;
                                ByteBuffer buffer = NetworkHandler.packet_start(NPH.NET_TILE);
                                buffer.putInt(mx);
                                buffer.putInt(my);
                                buffer.putInt(0);
                                NetworkHandler.send_all(buffer);
                            }
                            //game.board[mx][my] = 0;
                        }
                    }*///else tilebreak = 0;

                    //System.out.println(Math.abs(x - (draw.view_x+draw.view_w/2)));
                    /* == 1 || main.input.get(KeyEvent.VK_RIGHT) > main.TPS/8) dx++;
                    if(main.input.get(KeyEvent.VK_LEFT) == 1 || main.input.get(KeyEvent.VK_LEFT) > main.TPS/8) dx--;
                    if(main.input.get(KeyEvent.VK_UP) == 1 || main.input.get(KeyEvent.VK_UP) > main.TPS/8) rotation = (rotation+1)%4;
                    if(main.input.get(KeyEvent.VK_A) == 1 && game.board_bound_x > 0)*/
                    break;
                case PCS_EXTERN:
                    //
                    x -= (x-tx)*0.3;
                    y -= (y-ty)*0.3;
                    break;
                case PCS_AI:
                    //System.out.print("gobby... ");
                    if(taunt.length() > txt.length() && (int)(Math.random()*5) == 0){
                        txt = taunt.substring(0,txt.length()+1); // taunt animation
                        //if(Math.random() < 0.1)  AudioManager.audio.get("speak2").play(x,y,draw.view_x+draw.view_w/2,draw.view_y+draw.view_h/2);
                    }
                    if(game.state == game.STATE_LOSE || (taunt == "" && (int)(Math.random()*(main.TPS*5)) == 0) && vis == 1){
                        String[] a = {"speak3","speak4","speak5","speak6"};
                        AudioManager.audio.get(a[(int)(Math.random()*3)]).play(x,y,draw.view_x+draw.view_w/2,draw.view_y+draw.view_h/2);
                        taunt = taunts[(int)(Math.random()*taunts.length)]; // new taunt
                    }

                    // run away from current tetromino
                    /*if(Math.abs(currentTetromino.dx+boardx+2*main.SPR_WIDTH-e.x) < 2*main.SPR_WIDTH) e.hsp = -(currentTetromino.dx+boardx+2*main.SPR_WIDTH-e.x)*0.01;
                    else */if((int)(Math.random()*main.TPS) == 0) hsp = -0.25+0.5*Math.random(); // wander
                    if((int)(Math.random()*main.TPS) == 0) hsp = 0;
                    break;
            }

            if(Math.abs(hsp)+Math.abs(vsp) > 0.01) change = 1;

            if(game.pointCheck(x,y) == 1){ // die from being crushed
                destroy = 1;
                AudioManager.audio.get("speak3").play(x,y,draw.view_x+draw.view_w/2,draw.view_y+draw.view_h/2);
                //for(int j = 0; j < 4; j++) draw.particlePush(130,134,0.03+0.02*Math.random(),(int)x,(int)y,-0.1+0.2*Math.random(),-0.1+0.2*Math.random(), Color.WHITE);
                //draw.particlePush(150,154,0.09+0.01*Math.random(),(int)x-main.SPR_WIDTH/2,(int)y-main.SPR_WIDTH,-0.01+0.02*Math.random(),-0.08,Color.WHITE);
                for(int j = 0; j < 4; j++) CreateObject(new ObjectParticle(game,(int)x,(int)y,-0.1+0.2*Math.random(),-0.1+0.2*Math.random(),130,134,0.03+0.02*Math.random(),240,Color.WHITE));
                CreateObject(new ObjectParticle(game, (int)x-main.SPR_WIDTH/2, (int)y-main.SPR_WIDTH, -0.01+0.02*Math.random(), -0.08, 150, 154, 0.09+0.01*Math.random(), 240, Color.WHITE));
            }
        }

        // draw self
        int bx = Math.max(Math.min((int)(Math.floor(x-game.boardx)/main.SPR_WIDTH),game.boardWidth-1),0);
        int by = Math.max(Math.min((int)(Math.floor(y-game.boardy)/main.SPR_WIDTH),game.boardHeight-1),0);

        Color c = game.getLightLocal(x,y,0);

        draw.batchPush((int)sprite+(int)(main.frame/(main.TPS/2))%2+(hsp != 0 ? 1 : 0),
                (int)x-xd*main.SPR_WIDTH/2,
                (int)y-main.SPR_WIDTH+(int)Math.abs(Math.sin((main.frame/main.TPS + id*234)*Math.PI)*Math.abs(hsp)),
                xd*main.SPR_WIDTH,main.SPR_WIDTH,c);
        // draw taunt
        if(txt != ""){
            draw.batchPush(9,(int)x-10,(int)y-18-(int)x%2,txt.length()*6,8,new Color(24,20,37));
            draw.drawText(txt,(int)x-10,(int)y-18-(int)x%2,8,6,txtcolour);
        }
    }
}
