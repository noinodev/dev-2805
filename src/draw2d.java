import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class draw2d {
    public BufferedImage[] sprites;
    public BufferedImage framebuffer;
    public Graphics2D viewport;
    public class quad {
        public int id,x,y,w,h;
    }
    public ArrayList<quad> batch;
    /*public draw2d(){
        batch = new ArrayList<quad>();
    }*/

    public void batchReset(){
        batch.clear();
    }

    public void batchPush(int id,int x,int y,int w,int h){
        quad q = new quad();
        q.id = id;
        q.x = x;
        q.y = y;
        q.w = w;
        q.h = h;
        batch.add(q);
    }

    public void batchDraw(){
        //Graphics2D viewport = framebuffer.createGraphics();
        viewport.setColor(Color.white);
        viewport.fillRect(0,0,framebuffer.getWidth(),framebuffer.getHeight());
        //batchPush(1,20,20,10,10);

        if(batch.size() > 0){
            for(int i = 0; i < batch.size(); i++) {
                quad j = batch.get(i);
                //System.out.println(j.id + j.x + j.y);
                if(j != null) viewport.drawImage(sprites[j.id], j.x, j.y, j.w, j.h, null);
            }
        }
        batch.clear();
        //batchPush(1,20,20,10,10);
        //viewport.dispose();
    }
}