import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class draw2d {
    public BufferedImage[] sprites;
    public BufferedImage framebuffer;
    public Graphics2D viewport;
    public Color clearColour;
    public static class quad { public int id,x,y,w,h; }
    public ArrayList<quad> batch;
    public final Map<Character,Integer> textAtlas = new HashMap<>();

    public void batchPush(int id,int x,int y,int w,int h){
        quad q = new quad();
        q.id = id;
        q.x = x;
        q.y = y;
        q.w = w;
        q.h = h;
        batch.add(q);
    }

    public void drawText(String s, int x, int y, int size){
        for(int i = 0; i < s.length(); i++){
            char j = s.charAt(i);
            batchPush(textAtlas.get(j),x+i*size,y,size,size);
        }
    }

    public void batchDraw(){
        viewport.setColor(clearColour);
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