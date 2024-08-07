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

    public final Map<Integer,Integer> input = new HashMap<>();
    public final int keybuffermax = 64;
    public String keybuffer;
    public double mousex,mousey;
    public int cursorcontext;

    private draw2d draw;
    public float frame;
    public double delta;

    public scene currentScene;
    public int gameShouldClose;

    private void saveData() {
        try {
        BufferedWriter a = new BufferedWriter(new FileWriter("src/hscore.txt"));
            Set<String> keys = scores.keySet();
            for(String key: keys) {
                a.write(key+" "+scores.get(key)+'\n');
            }
            a.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadData(){
        scores = new HashMap<>();
        try {
            Scanner scan = new Scanner(new File("src/hscore.txt"));
            while(scan.hasNextLine()) {
                String[] entry = scan.nextLine().split(" ");
                scores.put(entry[0], Integer.parseInt(entry[1]));
                System.out.println(entry[0]);
                System.out.println(entry[1]);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
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
        BufferedImage[] out = new BufferedImage[size*size];
        int w = a.getWidth() / size;
        for(int i = 0; i < w; i++){
            for(int j = 0; j < w; j++){
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

    public Tetris2805(){
        gameShouldClose = 0;

        BufferedImage atlas = loadTexture("atlas.png");
        draw = new draw2d(this);
        draw.framebuffer = new BufferedImage(FRAMEBUFFER_W,FRAMEBUFFER_H,BufferedImage.TYPE_INT_ARGB);
        draw.viewport = draw.framebuffer.createGraphics();
        draw.sprites = getTextureAtlasSquare(atlas,SPR_WIDTH);
        draw.clearColour = new Color(38,43,68);

        for (char c = 'A'; c <= 'Z'; c++) draw.textAtlas.put(c, 74+c - 'A');
        for (char c = '0'; c <= '9'; c++) draw.textAtlas.put(c, 63 + (c - '0'));
        draw.textAtlas.put('.',73);
        draw.textAtlas.put(' ',62);

        setPreferredSize(new Dimension(VIEWPORT_W, VIEWPORT_H));
        setFocusable(true);
        requestFocusInWindow();
        keybuffer = "";
        setInput();

        loadData();

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isShowing()) {
                    Point mouse = MouseInfo.getPointerInfo().getLocation(),
                    screen = getLocationOnScreen();
                    mousex = ((mouse.getX()-screen.getX())/getWidth())*FRAMEBUFFER_W;
                    mousey = ((mouse.getY()-screen.getY())/getHeight())*FRAMEBUFFER_H;
                }
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

                currentScene.loop();
                //draw.drawButton
                draw.batchPush(20+cursorcontext,(int)mousex-1,(int)mousey,SPR_WIDTH,SPR_WIDTH);
                repaint();
                setInput();

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
            saveData(); // TODO
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