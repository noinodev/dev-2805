import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Tetris2805 extends JPanel implements ActionListener {
    public final int SPR_WIDTH = 10;
    public final int FRAMEBUFFER_W = 144, FRAMEBUFFER_H = 256, VIEWPORT_W = 720, VIEWPORT_H = 1280;
    public final float TPS = 240;

    public final Map<Integer,Integer> input = new HashMap<>();

    private draw2d draw;
    public float frame;
    public double delta;

    public scene currentScene;

    public BufferedImage loadTexture(String path){
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
        draw.framebuffer = new BufferedImage(FRAMEBUFFER_W,FRAMEBUFFER_H,BufferedImage.TYPE_INT_ARGB);
        draw.viewport = draw.framebuffer.createGraphics();
        draw.sprites = getTextureAtlasSquare(atlas,SPR_WIDTH);
        draw.batch = new ArrayList<draw2d.quad>();
        draw.clearColour = new Color(38,43,68);

        setPreferredSize(new Dimension(VIEWPORT_W, VIEWPORT_H));
        setFocusable(true);
        requestFocusInWindow();

        input.put(KeyEvent.VK_RIGHT,0);
        input.put(KeyEvent.VK_LEFT,0);
        input.put(KeyEvent.VK_UP,0);
        input.put(KeyEvent.VK_DOWN,0);


        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed (KeyEvent e) {
                input.put(e.getKeyCode(),1);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                input.put(e.getKeyCode(),0);
            }
        });

        currentScene = new splash(this,draw);

        Thread gameThread = new Thread(() -> {
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

                input.put(KeyEvent.VK_RIGHT,0);
                input.put(KeyEvent.VK_LEFT,0);
                input.put(KeyEvent.VK_UP,0);
                input.put(KeyEvent.VK_DOWN,0);

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
        });
        gameThread.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw.batchDraw();
        g.drawImage(draw.framebuffer,0,0, getWidth(), getHeight(),null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    public static void main(String[] args){
        JFrame frame = new JFrame("Tetris");
        Tetris2805 game = new Tetris2805();
        frame.add(game);
        frame.setSize(new Dimension(720,1280));
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}