import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class draw2d{
    public BufferedImage[] sprites;
    public BufferedImage framebuffer;
    public Graphics2D viewport;
    public Color clearColour;
    public static class quad { public int id,x,y,w,h; Color c;}
    public static class ptcl { public int f; double id,animspd,x,y,hsp,vsp; Color c;}
    private ArrayList<quad> batch;
    private ArrayList<ptcl> particles;
    public final Map<Character,Integer> textAtlas = new HashMap<>();
    private Tetris2805 main;
    private double buttonanim;
    private int lastbutton;

    public draw2d(Tetris2805 m){
        main = m;
        batch = new ArrayList<>();
        particles = new ArrayList<>();
        buttonanim = 0;
        lastbutton = 0;
    }

    public double lerp(double a, double b, double f) {
        return (a * (1.0 - f)) + (b * f);
    }

    private BufferedImage tintImage(BufferedImage src, Color color) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] scales = { color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f };
        float[] offsets = new float[4];
        RescaleOp op = new RescaleOp(scales, offsets, null);
        op.filter(src, out);
        return out;
    }

    // feels janky ?
    public void particlePush(double id, int f, double animspd, int x, int y, double hsp, double vsp, Color c){
        ptcl p = new ptcl();
        p.id = id;
        p.f = f;
        p.x = x;
        p.y = y;
        p.animspd = animspd;
        p.hsp = hsp;
        p.vsp = vsp;
        p.c = c;
        particles.add(p);
    }

    public void batchPush(int id,int x,int y,int w,int h, Color c){
        quad q = new quad();
        q.id = id;
        q.x = x;
        q.y = y;
        q.w = w;
        q.h = h;
        q.c = c;
        batch.add(q);
    }

    public void batchPush(int id,int x,int y,int w,int h){
        batchPush(id,x,y,w,h,null);
    }

    public void drawBox(int x, int y, int w, int h, int size){
        batchPush(10,x,y,size,size);
        //batchPush(11,x+w-size,y,size,size);
        batchPush(12,x,y+h-size,size,size);
        //batchPush(13,x+w-size,y+h-size,size,size);
    }

    public void drawText(String s, int x, int y, int size, int space, Color c){
        if(s.length() > 0){
            for(int i = 0; i < s.length(); i++){
                char j = s.charAt(i);
                int chr = textAtlas.get(j);
                if(chr != -1){
                    batchPush(textAtlas.get(j),x+i*space,y+1,size,size,Color.BLACK);
                    batchPush(textAtlas.get(j),x+i*space,y,size,size,c);
                }
            }
        }
    }

    public void batchDraw(){
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
                    }else batchPush((int)Math.floor(j.id),(int)j.x,(int)j.y,main.SPR_WIDTH,main.SPR_WIDTH,j.c);
                }
            }
        }

        viewport.setColor(clearColour);
        viewport.fillRect(0,0,framebuffer.getWidth(),framebuffer.getHeight());

        if(batch.size() > 0){
            for(int i = 0; i < batch.size(); i++) {
                quad j = batch.get(i);
                if(j != null){
                    BufferedImage finaldraw = sprites[j.id];
                    if(j.c != null) finaldraw = tintImage(finaldraw,j.c);
                    viewport.drawImage(finaldraw, j.x, j.y, j.w, j.h, null);
                }
            }
        }
        batch.clear();
    }

    public int getButtonContext(int x, int y, int w, int h, int mousecontext){
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
        int c = 255-(int)(80*m);
        batchPush(9,x+m,y+m,w-2*m,h-2*m,new Color(24,20,37));
        drawBox(x-m,y-m,w+2*m,h+2*m,7-m);
        drawText(label,x+1+(int)(5*mouseanim),y+1,8,6,new Color(c,c,c,160+(int) (Math.sin((main.frame/main.TPS) * 2*Math.PI))*80));
        if(m == 1 && main.input.get(-1) == 1) return 1;
        return 0;
    }

    public int drawSlider(String label, int x, int y, int w, int h, int val, int lb, int ub){
        int m = getButtonContext(x,y,w,h,1);
        double mouseanim = m*buttonanim;
        int c = 255-(int)(80*m);
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
        val = (int)((sx/96)*(double)(ub-lb))+lb;
        return val;
    }

    public int drawToggle(String label, int x, int y, int w, int h, int val){
        int m = getButtonContext(x,y,w,h,1);
        double mouseanim = m*buttonanim;
        int c = 255-(int)(80*m);
        batchPush(9,x+m,y+m,w-2*m,h-2*m,new Color(24,20,37));
        drawBox(x-m,y-m,w+2*m,h+2*m,7-m);
        drawText(label,x+1+(int)(5*mouseanim),y+1,8,6,new Color(c,c,c,160+(int) (Math.sin((main.frame/main.TPS) * 2*Math.PI))*80));
        batchPush(val == 0 ? 40 : 41, x+w-main.SPR_WIDTH,y,main.SPR_WIDTH,main.SPR_WIDTH);
        if(m == 1 && main.input.get(-1) == 1) return 1-val;
        return val;
    }

    public String drawTextfield(String label, int x, int y, int w, int h){
        int m = getButtonContext(x,y,w,h,2);
        if(m == 1 && main.input.get(-1) == 1 && main.keycontext != x+y){
            main.keycontext = x+y;
            main.keybuffer = "";
        }
        double mouseanim = m*buttonanim;
        int c = 255-(80*m);
        batchPush(9,x+m,y+m,w-2*m,h-2*m,new Color(24,20,37));
        drawBox(x-m,y-m,w+2*m,h+2*m,7-m);
        if(main.keycontext == x+y){
            //System.out.println(main.keybuffer/* + ((main.frame%(main.TPS) < main.TPS/2) ? "|" : "")*/);
            drawText(main.keybuffer,x+1,y+1,8,6,new Color(c,c,c,255));
            int a = (int) (Math.sin((main.frame/main.TPS) * 2*Math.PI)*255);
            batchPush(9,x+main.keybuffer.length()*8+2,y+2,1,4,new Color(255,255,255,Math.abs(a)));
            if(main.input.get(KeyEvent.VK_ENTER) == 1){
                main.keycontext = -1;
                return main.keybuffer;
            }
        }else drawText(label,x+1+(int)(2*mouseanim),y+1,8,6,new Color(c,c,c,255));
        return "";
    }
}