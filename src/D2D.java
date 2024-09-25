import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class D2DSprite {
    public BufferedImage sprite;
    public double x,y,w,h;
    Color colour;

    public D2DSprite(BufferedImage sprite, double x, double y, double w, double h, Color colour){
        this.sprite = sprite;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.colour = colour;
    }
}

/*class D2DRenderBatch {
    private ArrayList<D2DSprite> batch;

    private BufferedImage tintImage(BufferedImage src, Color color) { // image colouring, pretty sure this is really slow but i just wanted colours
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] scales = { color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, 1f };
        float[] offsets = new float[4];
        RescaleOp op = new RescaleOp(scales, offsets, null);
        op.filter(src, out);
        return out;
    }

    public void batchPush(D2D d2d, int id,double x,double y,double w,double h){ batchPush(d2d,id,x,y,w,h,null); }
    public void batchPush(D2D d2d, int id,double x,double y,double w,double h, Color c){
        D2DSprite spr = new D2DSprite(d2d.sprites[id],x,y,w,h,c);
        batch.add(spr);
    }

    public void batchClear(){
        batch.clear();
    }

    public void batchDraw(D2D d2d, D2DFramebuffer fbo){
        // add particles to batch
        if(particles.size() > 0){
            for(int i = 0; i < particles.size(); i++) {
                ptcl j = particles.get(i);
                if(j != null){
                    j.x += j.hsp;
                    j.y += j.vsp;
                    j.id += j.animspd;
                    if(j.id > j.f){
                        particles.remove(i);
                        i--;
                    }else batchPush((int)Math.floor(j.id),j.x,j.y,main.SPR_WIDTH,main.SPR_WIDTH,j.c);
                }
            }
        }

        //viewport.setColor(Color.white);
        //viewport.fillRect(0,0,framebuffer.getWidth(),framebuffer.getHeight());

        if(batch.size() > 0){
            for(int i = 0; i < batch.size(); i++) {
                D2DSprite spr = batch.get(i);
                if(spr != null){
                    BufferedImage sprfinal = spr.sprite;
                    if(spr.colour != null && spr.colour != Color.WHITE) sprfinal = tintImage(sprfinal,spr.colour);
                    int x,y,w,h;
                    x = (int)(spr.x-fbo.x);
                    y = (int)(spr.y-fbo.y);
                    w = (int)(spr.w*(d2d.d2d_fbo_main.w/fbo.w));
                    h = (int)(spr.h*(d2d.d2d_fbo_main.h/fbo.h));

                    fbo.viewport.drawImage(sprfinal, x, y, w, h, null);
                }
            }
        }
        //batch.clear();
    }
}*/

/*class D2DFramebuffer {
    public double x,y,w,h; // bounds of framebuffer in 'world'
    public BufferedImage framebuffer;
    public Graphics2D viewport;
    public Color clearcolour;
    //public D2DRenderBatch batch;

    public D2DFramebuffer(double x, double y, int w, int h){
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;

        framebuffer = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        viewport = framebuffer.createGraphics();
    }

    public void resize(){

    }
}*/

public class D2D{
    private static Tetris2805 main;
    public static final Map<Character,Integer> textAtlas = new HashMap<>();
    public static BufferedImage[] sprites;
    public static int width,height;
    public static Color clearColour;
    private static D2D instance;
    //public BufferedImage framebuffer;
    //public D2DFramebuffer fbo;
    public double view_x,view_y,view_w,view_h,view_wscale,view_hscale; // bounds of framebuffer in 'world'
    public BufferedImage framebuffer;
    public Graphics2D viewport;
    //public D2DRenderBatch batch;

    /*public void d2d_set_target(D2DFramebuffer view){

    }*/


    //public ArrayList<D2DFramebuffer> d2d_fbo;
    //public ArrayList<D2DRenderBatch> d2d_batch;

    //public Graphics2D viewport;
    //public D2DFramebuffer d2d_fbo_main;
    //public Color clearColour;
    //public static class quad { public int id; double x,y,w,h; Color c;}
    //public static class ptcl { public int f; double id,animspd,x,y,hsp,vsp; Color c;}
    //private ArrayList<quad> batch;
    //private ArrayList<ptcl> particles;
    private double buttonanim;
    private int lastbutton;

    private ArrayList<D2DSprite> batch;

    private static BufferedImage tintImage(BufferedImage src, Color color) { // image colouring, pretty sure this is really slow but i just wanted colours
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] scales = { color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, 1f };
        float[] offsets = new float[4];
        RescaleOp op = new RescaleOp(scales, offsets, null);
        op.filter(src, out);
        return out;
    }

    public void batchPush(int id,double x,double y,double w,double h){ batchPush(id,x,y,w,h,null); }
    public void batchPush(int id,double x,double y,double w,double h, Color c){
        //int vw = (int)(framebuffer.getWidth()/view_w), vh = (int)(framebuffer.getHeight()/view_h);
        if(x+w > view_x && x < view_x+view_w && y+h > view_y && y < view_y+view_h){
            D2DSprite spr = new D2DSprite(sprites[id],x,y,w,h,c);
            batch.add(spr);
        }
    }
    public void batchPush(BufferedImage s,double x,double y,double w,double h){
        if(x+w > view_x && x < view_x+view_w && y+h > view_y && y < view_y+view_h){
            D2DSprite spr = new D2DSprite(s,x,y,w,h,null);
            batch.add(spr);
        }
    }

    /*public void batchDraw(){
        // add particles to batch

        //viewport.setColor(Color.white);
        //viewport.fillRect(0,0,framebuffer.getWidth(),framebuffer.getHeight());

        if(batch.size() > 0){
            for(int i = 0; i < batch.size(); i++) {
                D2DSprite spr = batch.get(i);
                if(spr != null){
                    BufferedImage sprfinal = spr.sprite;
                    if(spr.colour != null && spr.colour != Color.WHITE) sprfinal = tintImage(sprfinal,spr.colour);
                    int x,y,w,h;
                    x = (int)(spr.x-fbo.x);
                    y = (int)(spr.y-fbo.y);
                    w = (int)(spr.w*(fbo.w/fbo.w));
                    h = (int)(spr.h*(d2d.d2d_fbo_main.h/fbo.h));

                    fbo.viewport.drawImage(sprfinal, x, y, w, h, null);
                }
            }
        }
        //batch.clear();
    }*/

    private D2D(){}
    public static D2D D2Dget (Tetris2805 m){
        if(instance == null) instance = new D2D();
        main = m;
        return instance;
    }
    public static D2D D2Dget (){
        return instance;
    }

    public void D2Dstart(Tetris2805 m){
        instance.framebuffer = new BufferedImage(m.FRAMEBUFFER_W,m.FRAMEBUFFER_H,BufferedImage.TYPE_INT_ARGB);
        instance.viewport = instance.framebuffer.createGraphics();
        instance.view_w = m.FRAMEBUFFER_W;
        instance.view_h = m.FRAMEBUFFER_H;
    }

    public void D2Dinit(Tetris2805 m){
        instance.main = m;
        D2Dstart(m);
        /*instance.viewport.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        instance.viewport.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        instance.viewport.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);*/
        instance.batch = new ArrayList<>();
        instance.view_x = 0;
        instance.view_y = 0;
        /*instance.view_wscale = instance.framebuffer.getWidth()/instance.view_w;
        instance.view_hscale = instance.framebuffer.getHeight()/instance.view_h;
        AffineTransform scaleTransform = AffineTransform.getScaleInstance(instance.view_wscale, instance.view_hscale);
        instance.viewport.setTransform(scaleTransform);*/
        instance.buttonanim = 0;
        instance.lastbutton = 0;
        //System.out.println("w="+instance.framebuffer.getWidth()/instance.view_w);
    }

    public static double lerp(double a, double b, double f) { // helper method, java stl doesnt have lerp???
        return (a * (1.0 - f)) + (b * f);
    }

    // feels janky ?
    //public void particlePush(double id, int f, double animspd, int x, int y, double hsp, double vsp, Color c){
        /*ptcl p = new ptcl();
        p.id = id;
        p.f = f;
        p.x = x;
        p.y = y;
        p.animspd = animspd;
        p.hsp = hsp;
        p.vsp = vsp;
        p.c = c;
        particles.add(p);*/
    //}

    /*public void batchPush(int id,double x,double y,double w,double h){ batchPush(id,x,y,w,h,null); }
    public void batchPush(int id,double x,double y,double w,double h, Color c){
        D2DSprite spr = new D2DSprite(sprites[id],x,y,w,h,c);
        batch.add(q);
    }*/

    public void drawBox(int x, int y, int w, int h, int size){ // only really used for ui elements
        batchPush(10,x,y,size,size);
        //batchPush(11,x+w-size,y,size,size);
        batchPush(12,x,y+h-size,size,size);
        //batchPush(13,x+w-size,y+h-size,size,size);
    }

    public void drawText(String s, int x, int y, int size, int space) { drawText(s,x,y,size,space,null,0); }
    public void drawText(String s, int x, int y, int size, int space, Color c) { drawText(s,x,y,size,space,c,0); }
    public void drawText(String s, int x, int y, int size, int space, Color c, int alignment){
        if(s.length() > 0){
            if(alignment == 1) x -= (s.length()*space)/2;
            for(int i = 0; i < s.length(); i++){
                char j = s.charAt(i);
                if(textAtlas.get(j) != null){
                    int chr = textAtlas.get(j);
                    if(chr != -1){
                        batchPush(textAtlas.get(j),x+i*space,y+1,size,size,Color.BLACK);
                        batchPush(textAtlas.get(j),x+i*space,y,size,size,c);
                    }
                }
            }
        }
    }

    public void batchDraw(){
        /*ArrayList<> batch = d2dbo.batch;
        // add particles to batch
        if(particles.size() > 0){
            for(int i = 0; i < particles.size(); i++) {
                ptcl j = particles.get(i);
                if(j != null){
                    j.x += j.hsp;
                    j.y += j.vsp;
                    j.id += j.animspd;
                    if(j.id > j.f){
                        particles.remove(i);
                        i--;
                    }else batchPush((int)Math.floor(j.id),j.x,j.y,main.SPR_WIDTH,main.SPR_WIDTH,j.c);
                }
            }
        }*/

        //viewport.setColor(Color.white);
        //viewport.fillRect(0,0,framebuffer.getWidth(),framebuffer.getHeight());
        double vw = (framebuffer.getWidth()/view_w), vh = (framebuffer.getHeight()/view_h);
        //int s=0,ns=0;

        if(batch.size() > 0){
            for(int i = 0; i < batch.size(); i++) {
                D2DSprite spr = batch.get(i);
                if(spr != null){
                    BufferedImage finaldraw = spr.sprite;
                    if(spr.colour != null && spr.colour != Color.WHITE) finaldraw = tintImage(finaldraw,spr.colour);
                    int x,y,w,h;
                    x = (int)((spr.x-view_x)*vw);
                    y = (int)((spr.y-view_y)*vh);
                    w = (int)(spr.w*(framebuffer.getWidth()/view_w));
                    h = (int)(spr.h*(framebuffer.getHeight()/view_h));
                    //System.out.println(""+x+", "+y+", "+w+", "+h);
                    /*if(x+spr.w > 0 && x < main.FRAMEBUFFER_W && y+spr.h > 0 && y < main.FRAMEBUFFER_H)*/ viewport.drawImage(finaldraw, x, y, (int)(spr.w), (int)(spr.h), null);
                }
            }
        }
        batch.clear();
        if(view_w != main.FRAMEBUFFER_W) D2Dstart(main);//D2Dinit(main);
    }

    // ui elements and functions

    public int getButtonContext(int x, int y, int w, int h, int mousecontext){ // get cursor context for buttons, text fields etc, and get the current button to reset the animation
        int m = main.mouseInArea(x,y,w,h);
        if(m == 1){
            if(lastbutton != x+y){ // dum dum button hash, say NO to classes !
                buttonanim = 0;
                lastbutton = x+y;
            }else buttonanim = Math.min((buttonanim+(1-buttonanim)*0.2),1);
            main.cursorcontext = mousecontext;
        }
        return m;
    }

    public int drawButton(String label, int x, int y, int w, int h){
        int m = getButtonContext(x,y,w,h,1);
        double mouseanim = m*buttonanim;
        int c = 255-80*m;
        batchPush(9,x+m,y+m,w-2*m,h-2*m,new Color(24,20,37));
        drawBox(x-m,y-m,w+2*m,h+2*m,7-m);
        drawText(label,x+1+(int)(5*mouseanim),y+1,8,6,new Color(c,c,c,160+(int) (Math.sin((main.frame/main.TPS) * 2*Math.PI))*80));
        if(m == 1 && main.input.get(-1) == 1) return 1;
        return 0;
    }

    public int drawSlider(String label, int x, int y, int w, int h, int val, int lb, int ub){
        int m = getButtonContext(x,y,w,h,1);
        double mouseanim = m*buttonanim;
        int c = 255-(int)(80*mouseanim);
        batchPush(9,x+m,y+m,w-2*m,h-2*m,new Color(24,20,37));
        drawBox(x-m,y-m,w+2*m,h+2*m,7-m);
        drawText(label,x+1+(int)(5*mouseanim),y+1,8,6,new Color(c,c,c,160+(int) (Math.sin((main.frame/main.TPS) * 2*Math.PI))*80));
        drawText(""+val,x+w-120,y+1,8,6,Color.WHITE);
        batchPush(9,x+w-100,y+1,1,8);
        batchPush(9,x+w-1,y+1,1,8);
        batchPush(9,x+w-98,y+5,96,1);
        double sx = ((val-lb)/(double)(ub-lb))*96;
        batchPush(9,x+w-98+(int)sx,y,1,10);
        if(m == 1 && main.input.get(-1) == 2) sx = main.mousex-(x+w-98);
        sx = Math.max(Math.min(96,sx),0);
        //val = (int)(sx*(double)(ub-lb)*96+lb);
        val = (int)Math.round((sx/96)*(double)(ub-lb))+lb;
        return val;
    }

    public int drawToggle(String label, int x, int y, int w, int h, int val){
        int m = getButtonContext(x,y,w,h,1);
        double mouseanim = m*buttonanim;
        int c = 255-(int)(80*mouseanim);
        batchPush(9,x+m,y+m,w-2*m,h-2*m,new Color(24,20,37));
        drawBox(x-m,y-m,w+2*m,h+2*m,7-m);
        drawText(label,x+1+(int)(5*mouseanim),y+1,8,6,new Color(c,c,c,160+(int) (Math.sin((main.frame/main.TPS) * 2*Math.PI))*80));
        batchPush(val == 0 ? 40 : 41, x+w-main.SPR_WIDTH,y,main.SPR_WIDTH,main.SPR_WIDTH);
        if(m == 1 && main.input.get(-1) == 1) return 1-val;
        return val;
    }

    public String drawTextfield(String label, String val, int x, int y, int w, int h){
        int m = getButtonContext(x,y,w,h,2);
        if(m == 1 && main.input.get(-1) == 1 && main.keycontext != x+y){
            main.keycontext = x+y;
            main.keybuffer = "";
        }
        double mouseanim = m*buttonanim;
        int c = 255-(int)(80*mouseanim);
        batchPush(9,x+m,y+m,w-2*m,h-2*m,new Color(24,20,37));
        drawBox(x-m,y-m,w+2*m,h+2*m,7-m);
        batchPush(9,x+w-60,y,59,10,new Color(38,43,68));
        if(main.keycontext == x+y){
            drawText(main.keybuffer,x+1+w-60,y+1,8,6,new Color(c,c,c,255));
            int a = (int) (Math.sin((main.frame/main.TPS) * 2*Math.PI)*255);
            batchPush(9,x+w-60+main.keybuffer.length()*8+2,y+2,1,8,new Color(255,255,255,Math.abs(a)));
            if(main.input.get(KeyEvent.VK_ENTER) == 1){
                main.keycontext = -1;
                return main.keybuffer;
            }
        }else drawText(val,x+1+w-60,y+1,8,6,new Color(c,c,c,255));
        drawText(label,x+1+(int)(2*mouseanim),y+1,8,6,new Color(c,c,c,255));
        return "";
    }
}