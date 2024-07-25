import javax.imageio.ImageIO;
import javax.lang.model.type.NullType;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Main extends Frame {
    private BufferedImage[] sprites;
    private BufferedImage viewbuffer;

    private BufferedImage loadAtlas(String path){
        try {
            return ImageIO.read(Main.class.getResource(path));
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

    public Main(){
        viewbuffer = new BufferedImage(108,192,BufferedImage.TYPE_INT_ARGB);
        BufferedImage atlas = loadAtlas("atlas.png");
        this.sprites = splitAtlas(atlas,10);

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
        final double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        while (true) {
            long now = System.nanoTime();
            delta = (now - lastTime) / ns;
            lastTime = now;
            //update();
            repaint();
        }
    }

    @Override
    public void paint(Graphics g){
        super.paint(g);
        Graphics2D viewport = viewbuffer.createGraphics();
        viewport.setColor(Color.white);
        viewport.fillRect(0,0,viewbuffer.getWidth(),viewbuffer.getHeight());
        viewport.drawImage(sprites[(int)(Math.random()*15)],80,80,null);
        for(int i = 0; i < 200; i++) viewport.drawImage(sprites[(int)(Math.random()*15)],(int)(Math.random()*500),(int)(Math.random()*500),null);

        g.drawImage(viewbuffer,0,0, getWidth()/viewbuffer.getWidth(), getHeight()/viewbuffer.getHeight(),null);
        //g.drawImage(viewbuffer,80,80, 1, 1,null);
        g.drawImage(viewbuffer,0,0, getWidth(), getHeight(),null);
        viewport.dispose();
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    public static void main(String[] args){
        new Main();
    }
}