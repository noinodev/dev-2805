import com.sun.tools.javac.Main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

public class Tetris2805 extends Frame {
    private final int SPR_WIDTH = 10;
    private final int TET_WIDTH = 4;
    public final int FRAMEBUFFER_W = 108, FRAMEBUFFER_H = 192, VIEWPORT_W = 720, VIEWPORT_H = 1280;

    private draw2d scene;
    private long window;
    public float frame;
    public double delta;

    private BufferedImage loadAtlas(String path){
        try {
            return ImageIO.read(Tetris2805.class.getResource(path));
        } catch(IOException e){
            System.out.println("failed to load texture atlas");
            System.exit(1);
        }
        return null;
    }

    private BufferedImage[] splitAtlas(BufferedImage a, int size){
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
        BufferedImage atlas = loadAtlas("atlas.png");
        scene = new draw2d();
        scene.framebuffer = new BufferedImage(108,192,BufferedImage.TYPE_INT_ARGB);
        scene.viewport = scene.framebuffer.createGraphics();
        scene.sprites = splitAtlas(atlas,SPR_WIDTH);
        scene.batch = new ArrayList<draw2d.quad>();

        setSize(new Dimension(720,1280));
        setVisible(true);

        this.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                super.windowClosing(e);
                System.exit(0);
            }
        });

        long lastTime = System.nanoTime();
        final double amountOfTicks = 240.0;
        double expectedFrametime = 1000000000 / amountOfTicks;
        delta = 0;
        frame = 0;
        while (true) {
            long now = System.nanoTime();
            delta = (now - lastTime) / expectedFrametime;
            frame += delta;
            lastTime = now;

            for(int i = 0; i < 10000; i++){
                scene.batchPush(1,(int)((frame+i)%FRAMEBUFFER_W),(int)((frame+i)%FRAMEBUFFER_H),10,10);
            }

            //update();
            repaint();

            long timeTaken = System.nanoTime() - now;
            long sleepTime = (long)(expectedFrametime - timeTaken);

            // Ensure sleepTime is positive
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
        scene.batchDraw();
        g.setColor(Color.white);
        g.fillRect(0,0,getWidth(),getHeight());
        g.drawImage(scene.framebuffer,0,0, getWidth(), getHeight(),null);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    public static void main(String[] args){
        new Tetris2805();
    }
}