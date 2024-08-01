import java.awt.*;

public class scene {
    protected Tetris2805 main;
    protected draw2d draw;

    public scene(Tetris2805 m, draw2d d){
        main = m;
        draw = d;
    }

    public void loop(){}
}

class splash extends scene {
    private int time;
    public splash(Tetris2805 m, draw2d d) {
        super(m, d);
        time = 0;
        draw.clearColour = new Color(24,20,37);
    }
    @Override
    public void loop(){
        time++;
        if(time > 3*main.TPS) main.currentScene = new tetris(main,draw);

        draw.batchPush(7,15,15,10,10);
    }
}

class tetris extends scene {
    private boolean paused;
    private int[][] board;
    public tetris(Tetris2805 m, draw2d d) {
        super(m, d);
        draw.clearColour = new Color(62,39,49);
    }
    @Override
    public void loop(){
        draw.batchPush(6,10,10,10,10);
    }
}

class menu extends scene {
    public menu(Tetris2805 m, draw2d d) {
        super(m, d);
    }

}
