import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;

public class Tetris2805 extends JPanel implements ActionListener {
    public final int SPR_WIDTH = 10;
    public final int FRAMEBUFFER_W = 256, FRAMEBUFFER_H = 256, VIEWPORT_W = 1080, VIEWPORT_H = 1080;
    public final float TPS = 240;

    public Map<String,Integer> scores;
    public Map<String,Integer> cfg;

    public final HashMap<Integer,Integer> input = new HashMap<>();
    public final int keybuffermax = 10;
    public String keybuffer;
    public double mousex,mousey;
    public int cursorcontext, keycontext, displayconfirm;
    public double bgx,bgtx;

    private draw2d draw;
    public float frame;
    public double delta;

    public scene currentScene;
    public int sceneIndex;
    public int gameShouldClose;

    public void saveData(Map<String,Integer> map, String file) {
        try {
            BufferedWriter a = new BufferedWriter(new FileWriter(file));
            Set<String> keys = map.keySet();
            for(String key: keys) a.write(key+" "+map.get(key)+'\n');
            a.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String,Integer> loadData(String file){
        Map<String,Integer> out = new HashMap<>();
        try {
            Scanner scan = new Scanner(new File(file));
            while(scan.hasNextLine()) {
                String[] entry = scan.nextLine().split(" ");
                out.put(entry[0], Integer.parseInt(entry[1]));
                System.out.println(entry[0]);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    private BufferedImage convertToARGB(BufferedImage in) {
        BufferedImage out = new BufferedImage(in.getWidth(),in.getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = out.createGraphics();
        g2d.drawImage(in,0,0,null);
        g2d.dispose();
        return out;
    }

    public BufferedImage loadTexture(String path){
        try {
            URL in = Tetris2805.class.getResource(path);
            if(in != null){
                BufferedImage out =  ImageIO.read(in);
                if(out != null) return convertToARGB(out);
            }
        } catch(IOException e){
            System.out.println("failed to load texture atlas");
            System.exit(1);
        }
        return null;
    }

    private BufferedImage[] getTextureAtlasSquare(BufferedImage a, int size){
        int w = a.getWidth()/size, h = a.getHeight()/size;
        BufferedImage[] out = new BufferedImage[w*h];
        for(int i = 0; i < w; i++){
            for(int j = 0; j < h; j++){
                out[i+w*j] = a.getSubimage(i*size,j*size,size,size);
            }
        }
        return out;
    }

    private void setInput(){
        input.put(-1,0); // mouse left
        for (int c = KeyEvent.VK_UNDEFINED; c <= KeyEvent.VK_CONTEXT_MENU; c++) input.put(c,0);
        cursorcontext = 0;
    }

    public int mouseInArea(int x, int y, int w, int h){
        if(mousex >= x && mousex <= x+w && mousey >= y && mousey <= y+h) return 1;
        return 0;
    }

    public void handleMouse(MouseEvent e){
        if (isShowing()) {
            Point mouse = MouseInfo.getPointerInfo().getLocation(), screen = getLocationOnScreen();
            mousex = ((mouse.getX()-screen.getX())/getWidth())*FRAMEBUFFER_W;
            mousey = ((mouse.getY()-screen.getY())/getHeight())*FRAMEBUFFER_H;
        }
    }

    public Tetris2805(){
        gameShouldClose = 0;

        BufferedImage atlas = loadTexture("atlas.png");
        draw = new draw2d(this);
        draw.framebuffer = new BufferedImage(FRAMEBUFFER_W,FRAMEBUFFER_H,BufferedImage.TYPE_INT_ARGB);
        draw.viewport = draw.framebuffer.createGraphics();
        draw.sprites = getTextureAtlasSquare(atlas,SPR_WIDTH);
        draw.sprites[42] = loadTexture("bgtex6.png");
        draw.sprites[50] = loadTexture("bgtex1.png"); // reserving sprites for background, kinda hacky but whatever its not a real texture atlas
        draw.sprites[51] = loadTexture("bgtex2.png");
        draw.sprites[52] = loadTexture("bgtex3.png");
        draw.sprites[60] = loadTexture("bgtex4.png");
        draw.sprites[61] = loadTexture("bgtex5.png");
        //draw.sprites[62] = loadTexture("bgtex6.png");
        bgx = 0;
        bgtx = 0;

        draw.clearColour = new Color(38,43,68);

        for (char c = 'A'; c <= 'Z'; c++) draw.textAtlas.put(c, 74+c - 'A');
        for (char c = '0'; c <= '9'; c++) draw.textAtlas.put(c, 63 + (c - '0'));
        draw.textAtlas.put('.',73);
        draw.textAtlas.put('?',62);
        draw.textAtlas.put(' ',-1);

        setPreferredSize(new Dimension(VIEWPORT_W, VIEWPORT_H));
        setFocusable(true);
        requestFocusInWindow();
        keybuffer = "";
        keycontext = -1;
        displayconfirm = 0;
        setInput();

        scores = loadData("src/hscore.txt");
        cfg = loadData("src/config.txt");

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouse(e);
            }
            public void mouseDragged(MouseEvent e) {
                handleMouse(e);
                input.put(-1,2);
            }

        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                input.put(-1,1);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                input.put(-1,0);
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed (KeyEvent e) {
                input.put(e.getKeyCode(),1);
                if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE && keybuffer.length() > 0) keybuffer = keybuffer.substring(0,keybuffer.length()-1);
                char c = (char)e.getKeyCode();
                if(((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '.') && keybuffer.length() < keybuffermax) keybuffer = keybuffer + (char)e.getKeyCode();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                input.put(e.getKeyCode(),0);
            }
        });

        sceneIndex = -1;
        currentScene = new splash(this,draw);
        Thread gameThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            double expectedFrametime = 1000000000 / TPS;
            delta = 0;
            frame = 0;
            while (gameShouldClose == 0) {
                long now = System.nanoTime();
                delta = (now - lastTime) / expectedFrametime;
                frame += delta;
                lastTime = now;

                bgx -= (bgx-bgtx)*0.05;
                draw.batchPush(42,(int)((bgx*0.1)%FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                draw.batchPush(42,(int)((bgx*0.1)%FRAMEBUFFER_W-FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                draw.batchPush(61,(int)((bgx*0.2)%FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                draw.batchPush(61,(int)((bgx*0.2)%FRAMEBUFFER_W-FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                draw.batchPush(60,(int)((bgx*0.3)%FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                draw.batchPush(60,(int)((bgx*0.3)%FRAMEBUFFER_W-FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                draw.batchPush(52,(int)((bgx*0.4)%FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                draw.batchPush(52,(int)((bgx*0.4)%FRAMEBUFFER_W-FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                draw.batchPush(51,(int)((bgx*0.5)%FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                draw.batchPush(51,(int)((bgx*0.5)%FRAMEBUFFER_W-FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                draw.batchPush(50,(int)((bgx*0.6)%FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                draw.batchPush(50,(int)((bgx*0.6)%FRAMEBUFFER_W-FRAMEBUFFER_W),0,FRAMEBUFFER_W,FRAMEBUFFER_H);
                // holy yuck

                currentScene.loop();
                if(sceneIndex >= 2 && draw.drawButton("BACK",20,FRAMEBUFFER_H-20,80,10) == 1) currentScene = new menu(this,draw);
                if(displayconfirm == 1){
                    int w = 80, h = 80, x = FRAMEBUFFER_W/2-w/2, y = FRAMEBUFFER_W/2-h/2;
                    draw.batchPush(9,x,y,w,h,new Color(24,20,37));
                    draw.drawText("EXIT?",x+10,y+4,10,8,Color.WHITE);
                    gameShouldClose = draw.drawButton("YES",x+10,y+40,60,10);
                    displayconfirm = 1-draw.drawButton("NO",x+10,y+50,60,10);
                }
                draw.batchPush(20+cursorcontext,(int)mousex-1,(int)mousey+1,SPR_WIDTH,SPR_WIDTH,Color.BLACK);
                draw.batchPush(20+cursorcontext,(int)mousex-1,(int)mousey,SPR_WIDTH,SPR_WIDTH);
                repaint();
                setInput();

                long timeTaken = System.nanoTime() - now,
                sleepTime = (long)(expectedFrametime - timeTaken);

                //draw.drawText(""+timeTaken/1000000f,20,FRAMEBUFFER_H-40,8,6,Color.WHITE);

                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime / 1000000, (int)(sleepTime % 1000000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            saveData(scores,"src/hscore.txt");
            saveData(cfg,"src/config.txt");
            System.exit(1);
        });
        gameThread.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw.batchDraw();

        int w = Math.min(VIEWPORT_W,getWidth()), h = Math.min(VIEWPORT_H,getHeight());
        g.setColor(new Color(24,20,37));
        g.clearRect(0,0,getWidth(),getHeight());
        g.drawImage(draw.framebuffer,(getWidth()-w)/2,(getHeight()-h)/2, w, h,null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // unused
    }

    public static void main(String[] args){
        JFrame frame = new JFrame("Tetris");
        frame.setCursor( frame.getToolkit().createCustomCursor(
                new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB ),
                new Point(),
                null ) );
        Tetris2805 game = new Tetris2805();
        frame.add(game);
        frame.setSize(new Dimension(720,1280));
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}