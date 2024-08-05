import java.awt.*;
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
    public ArrayList<quad> batch;
    public final Map<Character,Integer> textAtlas = new HashMap<>();

    private BufferedImage tintImage(BufferedImage src, Color color) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] scales = { color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f };
        float[] offsets = new float[4];
        RescaleOp op = new RescaleOp(scales, offsets, null);
        op.filter(src, out);
        return out;
    }

    public void batchPush(int id,int x,int y,int w,int h){
        quad q = new quad();
        q.id = id;
        q.x = x;
        q.y = y;
        q.w = w;
        q.h = h;
        q.c = null;
        batch.add(q);
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

    public void drawBox(int x, int y, int w, int h, int size){
        batchPush(10,x,y,size,size);
        batchPush(11,x+w-size,y,size,size);
        batchPush(12,x,y+h-size,size,size);
        batchPush(13,x+w-size,y+h-size,size,size);
    }

    public void drawText(String s, int x, int y, int size, int space, Color c){
        for(int i = 0; i < s.length(); i++){
            char j = s.charAt(i);
            batchPush(textAtlas.get(j),x+i*space,y,size,size,c);
        }
    }

    public void batchDraw(){
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
}