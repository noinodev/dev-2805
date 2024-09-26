import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class GM {
    public static final byte GM_OFFLINE = 0;
    public static final byte GM_HOST = 1;
    public static final byte GM_JOIN = 2;
}

class ParseFormat {
    public static final byte JSON = 0;
    public static final byte MAP = 1;
    public static final byte SERIAL = 2;
}

public class Tetris2805 extends JPanel implements ActionListener {
    public static Tetris2805 main;
    public final int SPR_WIDTH = 10;
    public int FRAMEBUFFER_W = 256, FRAMEBUFFER_H = 256;
    public int VIEWPORT_W = 1080, VIEWPORT_H = 1080;
    public final float TPS = 240;
    public final int DIALOG_CONTEXT_EXIT = 1, DIALOG_CONTEXT_MENU = 2;

    public String UID = "AAAAAAAA";

    public Map<String,Object> scores;
    public Map<String,Object> cfg;

    public final HashMap<Integer,Integer> input = new HashMap<>();
    public final int keybuffermax = 10;
    public String keybuffer;
    public double mousex,mousey;
    public int cursorcontext, keycontext, displayconfirm, inputtype;
    public double bgx,bgy,bgtx;

    private D2D draw;
    public float frame;
    public double delta;

    public scene currentScene;
    public int sceneIndex;
    public int gameShouldClose;
    public byte gamemode;
    public byte gamemode_last;

    public Path working_directory;

    public static String mapToJson(Map<String,Object> map){
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        int i = 0;
        Set<String> keys = map.keySet();
        for(String key: keys){
            json.append("   \"").append(key).append("\":");
            Object val = map.get(key);

            if(val instanceof String) json.append("\"").append(val).append("\"");
            else json.append(val);

            if(i < map.size() - 1) json.append(",\n");
            else json.append("\n");
            i++;
        }

        json.append("}");
        return json.toString();
    }

    public static Map<String,Object> JsonToMap(String json){
        Map<String, Object> map = new HashMap<>();
        json = json.replace("\n", "");
        json = json.trim().substring(1, json.length() - 1); // Remove curly braces

        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Split by comma outside quotes
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            String key = keyValue[0].trim().replaceAll("^\"|\"$", ""); // Remove surrounding quotes

            String valueStr = keyValue[1].trim();
            Object value;

            if(valueStr.startsWith("\"")) {
                value = valueStr.replaceAll("^\"|\"$", ""); // It's a String
            }else{
                try {
                    value = Integer.parseInt(valueStr); // It's an integer
                } catch (NumberFormatException e) {
                    value = valueStr; // If parsing fails, leave it as a String
                }
            }

            map.put(key, value);
        }

        return map;
    }

    public static void saveData(Map<String,Object> map, String file, byte format) {
        try {
            BufferedWriter a = new BufferedWriter(new FileWriter(file));
            //Set<String> keys = map.keySet();
            //for(String key: keys) a.write(key+" "+map.get(key)+'\n');
            switch(format){
                case ParseFormat.JSON:
                    a.write(mapToJson(map));
                    break;
                case ParseFormat.MAP:
                    Set<String> keys = map.keySet();
                    for(String key: keys) a.write(key+" "+map.get(key)+'\n');
                    break;
            }
            a.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String,Object> loadData(String file, byte format){
        Map<String,Object> out = null;
        try {
            switch(format){
                case ParseFormat.JSON:
                    String json = new String(Files.readAllBytes(Paths.get(file)));
                    if(json.charAt(0) == '{') out = JsonToMap(json);
                    else System.out.println("not json: "+file);
                break;
                case ParseFormat.MAP:
                    out = new HashMap<>();
                    Scanner scan = new Scanner(new File(file));
                    while(scan.hasNextLine()) {
                        String[] entry = scan.nextLine().split(" ");
                        out.put(entry[0], Integer.parseInt(entry[1]));
                    }
                break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    private static BufferedImage convertToARGB(BufferedImage in) {
        BufferedImage out = new BufferedImage(in.getWidth(),in.getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = out.createGraphics();
        g2d.drawImage(in,0,0,null);
        g2d.dispose();
        return out;
    }

    public static BufferedImage loadTexture(String path){
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

    private static BufferedImage[] getTextureAtlasSquare(BufferedImage a, int size){
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
        if(inputtype == 0){
            input.put(-1,0); // mouse left
            for (int c = KeyEvent.VK_UNDEFINED; c <= KeyEvent.VK_CONTEXT_MENU; c++) input.put(c,0);
            cursorcontext = 0;
        }else inputtype = 0;
    }

    private void updateInput(){
        for (int c = KeyEvent.VK_UNDEFINED; c <= KeyEvent.VK_CONTEXT_MENU; c++){
            if(input.get(c) > 0) input.put(c,input.get(c)+1);
            //else input.put(c,0);
        }
    }

    public int mouseInArea(int x, int y, int w, int h){
        if(mousex >= x && mousex <= x+w && mousey >= y && mousey <= y+h) return 1;
        return 0;
    }

    public void handleMouse(MouseEvent e){
        if (isShowing()) {
            Point mouse = MouseInfo.getPointerInfo().getLocation(), screen = getLocationOnScreen();
            mousex = draw.view_x+((mouse.getX()-screen.getX())/getWidth())*FRAMEBUFFER_W;
            mousey = draw.view_y+((mouse.getY()-screen.getY())/getHeight())*FRAMEBUFFER_H;
        }
    }

    /*public void initGraphics(){
        draw.framebuffer = new BufferedImage(FRAMEBUFFER_W,FRAMEBUFFER_H,BufferedImage.TYPE_INT_ARGB);
        draw.viewport = draw.framebuffer.createGraphics();
    }*/

    public Tetris2805(){
        Tetris2805.main = this;
        gameShouldClose = 0;
        gamemode = GM.GM_OFFLINE;
        gamemode_last = gamemode;
        working_directory = Path.of("").toAbsolutePath();
        //String cwd = working_directory.toString();
        //System.out.println(cwd+"src/resources/atlas.png");

        UID = NetworkHandler.generateUID();
        System.out.println(UID);

        // draw init
        BufferedImage atlas = loadTexture("resources/assets/atlas.png");

        draw = D2D.D2Dget(this);
        //initGraphics();
        //D2D.main = this;
        draw.D2Dinit(this);
        D2D.clearColour = new Color(38,43,68);
        D2D.sprites = getTextureAtlasSquare(atlas,SPR_WIDTH);
        D2D.sprites[43] = loadTexture("resources/assets/splashtex.png");
        D2D.sprites[42] = loadTexture("resources/assets/bgtex6.png");
        D2D.sprites[50] = loadTexture("resources/assets/bgtex1.png"); // reserving sprites for background, kinda hacky but whatever its not a real texture atlas
        D2D.sprites[51] = loadTexture("resources/assets/bgtex2.png");
        D2D.sprites[52] = loadTexture("resources/assets/bgtex3.png");
        D2D.sprites[60] = loadTexture("resources/assets/bgtex4.png");
        D2D.sprites[61] = loadTexture("resources/assets/bgtex5.png");
        D2D.sprites[109] = loadTexture("resources/assets/spr_tree.png");
        D2D.sprites[119] = loadTexture("resources/assets/spr_rock.png");
        D2D.sprites[118] = loadTexture("resources/assets/spr_waterhighlights.png");
        D2D.sprites[129] = loadTexture("resources/assets/fgtex1.png");
        D2D.sprites[128] = loadTexture("resources/assets/fgtex2.png");
        D2D.sprites[135] = loadTexture("resources/assets/fgtex3.png");
        D2D.sprites[136] = loadTexture("resources/assets/fgtex4.png");
        D2D.sprites[44] = loadTexture("resources/assets/spr_wall.png");
        D2D.sprites[54] = loadTexture("resources/assets/spr_wall2.png");
        D2D.sprites[23] = loadTexture("resources/assets/spr_aithing.png");
        bgx = 0;
        bgy = 0;
        bgtx = 0;
        String audiopath = "src/resources/audio/";
        AudioManager.load("ambienthigh",audiopath);
        AudioManager.load("ambientlow",audiopath);
        AudioManager.load("slide1",audiopath);
        AudioManager.load("slide2",audiopath);
        AudioManager.load("slide3",audiopath);
        AudioManager.load("fwop1",audiopath);
        AudioManager.load("fwop2",audiopath);
        AudioManager.load("drop1",audiopath);
        AudioManager.load("speak2",audiopath);
        AudioManager.load("speak3",audiopath);
        AudioManager.load("speak4",audiopath);
        AudioManager.load("speak5",audiopath);
        AudioManager.load("speak6",audiopath);
        AudioManager.load("tap1",audiopath);
        AudioManager.load("tap2",audiopath);
        AudioManager.start();

        // char -> sprite map
        for (char c = 'A'; c <= 'Z'; c++) D2D.textAtlas.put(c, 74+c - 'A');
        for (char c = '0'; c <= '9'; c++) D2D.textAtlas.put(c, 63 + (c - '0'));
        D2D.textAtlas.put('.',73);
        D2D.textAtlas.put('?',62);
        D2D.textAtlas.put(' ',-1);

        // jpanel init
        setPreferredSize(new Dimension(VIEWPORT_W, VIEWPORT_H));
        setFocusable(true);
        requestFocusInWindow();

        // input init
        keybuffer = "";
        keycontext = -1;
        displayconfirm = 0;
        //input.put(-1,0);
        setInput();

        scores = loadData("src/data/hscore.txt",ParseFormat.MAP);
        cfg = loadData("src/data/config.txt",ParseFormat.JSON);

        NetworkHandler.main = this;
        NetworkHandler.startNetworkThread();

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
                // key input
                input.put(e.getKeyCode(),1);
                // keybuffer
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

            // main loop
            while (gameShouldClose == 0) {
                // delta timing
                long now = System.nanoTime();
                delta = (now - lastTime) / expectedFrametime;
                frame += delta;
                lastTime = now;

                // parallax background
                //bgx -= (bgx-bgtx)*0.05;
                /*bgx = draw.view_x;
                bgy = draw.view_y;
                int[] bgtex = {42,61,60,52,51,50};
                for(int i = 0; i < bgtex.length; i++){
                    draw.batchPush(bgtex[i],(int)((bgx*(0.1+0.1*i))%FRAMEBUFFER_W),(int)((bgy*(0.1+0.1*i))%FRAMEBUFFER_H),FRAMEBUFFER_W,FRAMEBUFFER_H);
                    draw.batchPush(bgtex[i],(int)((bgx*(0.1+0.1*i))%FRAMEBUFFER_W-FRAMEBUFFER_W),(int)((bgy*(0.1+0.1*i))%FRAMEBUFFER_H),FRAMEBUFFER_W,FRAMEBUFFER_H);
                }*/

                draw.batchPush(9,draw.view_x,draw.view_y,draw.view_w,draw.view_h,new Color(24,20,37));

                // scene loop
                currentScene.loop();
                //if(sceneIndex >= 2 && draw.drawButton("BACK",20,FRAMEBUFFER_H-20,80,10) == 1) currentScene = new menu(this,draw);

                // confirm exit dialog
                if(displayconfirm > 0){
                    int w = 80, h = 80, x = FRAMEBUFFER_W/2-w/2, y = FRAMEBUFFER_H/2-h/2;
                    draw.batchPush(9,x,y,w,h,new Color(24,20,37));
                    draw.drawText(displayconfirm == DIALOG_CONTEXT_EXIT ? "EXIT?" : "TO MENU?",x+w/2,y+4,10,8,Color.WHITE,1);
                    int act = draw.drawButton("YES",x+10,y+40,60,10);
                    if(displayconfirm == DIALOG_CONTEXT_EXIT && act == 1) gameShouldClose = 1;
                    else if(displayconfirm == DIALOG_CONTEXT_MENU && act == 1) currentScene = new menu(this,draw);
                    if(act == 1) displayconfirm = 0;
                    else displayconfirm = displayconfirm*(1-draw.drawButton("NO",x+10,y+50,60,10));
                }

                // cursor
                draw.batchPush(20+cursorcontext,(int)mousex-1,(int)mousey+1,SPR_WIDTH,SPR_WIDTH,Color.BLACK);
                draw.batchPush(20+cursorcontext,(int)mousex-1,(int)mousey,SPR_WIDTH,SPR_WIDTH);
                repaint();
                setInput();

                long timeTaken = System.nanoTime() - now,
                sleepTime = (long)(expectedFrametime - timeTaken);
                // debug data
                /*if(cfg.get("diag") == 1)*/draw.drawText(""+timeTaken/1000000f,(int)draw.view_x+20,FRAMEBUFFER_H-40,8,6,Color.WHITE); // frametime

                // sleep loop
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime / 1000000, (int)(sleepTime % 1000000));
                    } catch (InterruptedException e) {
                        // e.printStackTrace(); // shouldnt happen anyway
                    }
                }
            }
            // save data on safe close
            saveData(scores,"src/data/hscore.txt",ParseFormat.MAP);
            saveData(cfg,"src/data/config.txt",ParseFormat.JSON);
            System.exit(1);
        });
        gameThread.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw.batchDraw();

        // draw framebuffer
        VIEWPORT_W = getWidth();
        VIEWPORT_H = getHeight();
        //System.out.println("w:"+getWidth()+" h:"+getHeight());

        int w = getWidth(), h = getHeight();
        g.setColor(new Color(24,20,37));
        g.clearRect(0,0,getWidth(),getHeight());
        g.drawImage(draw.framebuffer,(int)((1-draw.view_x%1)*((double)VIEWPORT_W /FRAMEBUFFER_W)),(int)((1-draw.view_y%1)*((double)VIEWPORT_H/FRAMEBUFFER_H)), w, h,null);

        double ratio = VIEWPORT_W/(double)VIEWPORT_H;
        int nfw = (int)(FRAMEBUFFER_H*ratio);
        if(FRAMEBUFFER_W != nfw){
            FRAMEBUFFER_W = nfw;
            //draw.D2Dinit(this);

        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // unused
    }

    public static void main(String[] args){
        // jframe init
        JFrame frame = new JFrame("Tetris");
        // remove cursor
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
        frame.setLocationRelativeTo(null);
    }
}