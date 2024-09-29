import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

enum PlayerControlScheme {
    PCS_LOCAL,
    PCS_EXTERN,
    PCS_AI
}

public abstract class GameObject {
    public static Game g;
    public int destroy,id;
    public byte inst,change;
    public double sprite,x,y,w,h,xd,hsp,vsp,grv,tx,ty;
    public Tetris2805 main;
    public D2D draw;
    public Game game;
    public String UID;
    public GameObject(Game game){
        this.game = game;
        this.main = game.main;
        this.draw = game.draw;
        this.destroy = 0;
        this.w = main.SPR_WIDTH;
        this.h = main.SPR_WIDTH;
        sprite = 0;
        x = 0;
        y = 0;
        tx = 0;
        ty = 0;
        hsp = 0;
        vsp = 0;
        grv = 0.02;
        change = 1;

    }
    public void update(){ }

    public static final ArrayList<GameObject> objects = new ArrayList<GameObject>();
    public static final Map<String,GameObject> netobjects = new HashMap<>();
    public static byte lock = 0;

    public static GameObject syncObject(GameObject object){
        String UID = NetworkManager.generateUID();
        return syncObject(object,UID);
    }
    public static GameObject syncObject(GameObject object, String UID){
        if(g != null){
            object.UID = UID;
            if(netobjects.get(UID) == null){
                netobjects.put(UID,object);
                objects.add(object);
            }
        }
        return object;
    }
    public static GameObject CreateObject(GameObject object){
        objects.add(object);
        return object;
    }
    public static void DestroyAllObjects(){
        objects.clear();
        netobjects.clear();
    }
}

class ObjectResource extends GameObject {
    //public byte inst = 0;
    public double z;
    public int resource, hp, hps, timer;
    public ObjectResource(Game game, int x, int y, double z, int sprite, int hp){
        super(game);
        this.x = x;
        this.y = y;
        this.z = z;
        this.sprite = sprite;
        this.hp = hp;
        this.hps = hp;

        w = D2D.sprites[sprite].getWidth();
        h = D2D.sprites[sprite].getHeight();
        timer = 0;
        inst = 0;
    }

    @Override
    public void update(){
        timer--;
        hsp -= hsp*0.1;
        vsp -= vsp*0.1;
        int vx = (int)x+(int)((x-(draw.view_x+draw.view_w/2))*(-z)), vy = (int)y+(int)((y-(draw.view_y+draw.view_h/2))*(-z*0.5));
        Color c = game.getLightLocal(x,vy,0);
        draw.batchPush((int)sprite,vx-w/2+hsp,vy-h+vsp,w,h,c);
    }
}

class ObjectParticle extends GameObject {
    //public byte inst = 1;
    public int start, end, time;
    public double spd;
    public Color colour;
    public ObjectParticle(Game game,int x, int y, double hsp, double vsp, int start,int end,double spd,int time, Color colour){
        super(game);
        this.x = x;
        this.y = y;
        this.hsp = hsp;
        this.vsp = vsp;
        this.sprite = start;
        this.start = start;
        this.end = end;
        this.spd = spd;
        this.time = time;
        this.colour = colour;
        inst = 1;
        if((Integer)Tetris2805.main.cfg.get("video.particles") == 0) destroy = 1;
    }

    @Override
    public void update(){
        if(destroy == 1) return;
        sprite += spd;
        if(sprite > end) destroy = 1;
        time--;
        if(time <= 0) destroy = 1;
        x += hsp;
        y += vsp;
        if(sprite != 0) draw.batchPush((int)Math.floor(sprite),x,y,w,h,colour);
    }
}

abstract class PlayerObject extends GameObject {
    public PlayerControlScheme control_scheme;
    public PlayerObject(Game game, PlayerControlScheme pcs){
        super(game);
        this.control_scheme = pcs;
    }
}

