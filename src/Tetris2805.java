import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class Tetris2805 extends Frame {
    private final int SPR_WIDTH = 10;
    private final int TET_WIDTH = 4;
    public final int FRAMEBUFFER_W = 108, FRAMEBUFFER_H = 192, VIEWPORT_W = 720, VIEWPORT_H = 1280;
    public final float TPS = 240;

    private draw2d draw;
    public float frame;
    public double delta;

    public scene currentScene;

    private BufferedImage loadTexture(String path){
        try {
            URL in = Tetris2805.class.getResource(path);
            if(in != null){
                BufferedImage out =  ImageIO.read(in);
                if(out != null) return out;
            }
        } catch(IOException e){
            System.out.println("failed to load texture atlas");
            System.exit(1);
        }
        return null;
    }

    private BufferedImage[] getTextureAtlasSquare(BufferedImage a, int size){
        BufferedImage[] out = new BufferedImage[size*size];
        int w = a.getWidth() / size;
        for(int i = 0; i < w; i++){
            for(int j = 0; j < w; j++){
                out[i+w*j] = a.getSubimage(i*size,j*size,size,size);
            }
        }

        return out;
    }

    public Tetris2805(){
        BufferedImage atlas = loadTexture("atlas.png");
        draw = new draw2d();
        draw.framebuffer = new BufferedImage(108,192,BufferedImage.TYPE_INT_ARGB);
        draw.viewport = draw.framebuffer.createGraphics();
        draw.sprites = getTextureAtlasSquare(atlas,SPR_WIDTH);
        draw.batch = new ArrayList<draw2d.quad>();
        draw.clearColour = new Color(38,43,68);

        setSize(new Dimension(720,1280));
        setVisible(true);

        this.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                super.windowClosing(e);
                System.exit(0);
            }
        });

        currentScene = new splash(this,draw);

        long lastTime = System.nanoTime();
        double expectedFrametime = 1000000000 / TPS;
        delta = 0;
        frame = 0;
        while (true) {
            long now = System.nanoTime();
            delta = (now - lastTime) / expectedFrametime;
            frame += delta;
            lastTime = now;

            //for(int i = 0; i < 10000; i++) draw.batchPush(1,(int)((frame+i)%FRAMEBUFFER_W),(int)((frame+i)%FRAMEBUFFER_H),10,10);

            currentScene.loop();
            repaint();

            long timeTaken = System.nanoTime() - now,
            sleepTime = (long)(expectedFrametime - timeTaken);

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime / 1000000, (int)(sleepTime % 1000000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        draw.batchDraw();
        g.drawImage(draw.framebuffer,0,0, getWidth(), getHeight(),null);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    public static void main(String[] args){
        new Tetris2805();
    }
}