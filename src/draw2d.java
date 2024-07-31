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
        viewport.setColor(Color.white);
        viewport.fillRect(0,0,framebuffer.getWidth(),framebuffer.getHeight());

        if(batch.size() > 0){
            for(int i = 0; i < batch.size(); i++) {
                quad j = batch.get(i);
                if(j != null) viewport.drawImage(sprites[j.id], j.x, j.y, j.w, j.h, null);
            }
        }
        batch.clear();
    }
}