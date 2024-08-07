import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        if(time > main.TPS) main.currentScene = new menu(main,draw);

        draw.drawText("TETRIS",20,20,10,8,new Color(time,time,time));
        draw.drawText("JAVA GAME",20,30,8,6,new Color(time/2,time/2,time/2));
    }
}

class tetris extends scene {
    public final int TET_WIDTH = 4;
    private int boardWidth,boardHeight,posx,posy,boardx,boardy,time,score,state,cleary,clearflash,level,lines,nextTetronimo;
    private double cleardy;
    private final Color[] flash = {new Color(255,255,255), new Color(255,0,68), new Color(99,199,77), new Color(44,232,245), new Color(254,231,97)};
    private final int scores[] = {40,100,300,1200}; // tetris scores
    private int[][] board;
    private int[][][][] tetrominoList;
    private class tetromino {
        public int x,y,i,j,t;
        double dx,dy;
        public tetromino(int _x, int _y, int _i, int _j, int _t){
            x = _x;
            y = _y;
            dx = x*main.SPR_WIDTH;
            dy = y*main.SPR_WIDTH;
            i = _i;
            j = _j;
            t = _t;
        }
    }
    private tetromino currentTetromino;

    private int[][][][] getTetrominoes(BufferedImage in){
        int count = in.getHeight()/4;
        int[][][][] out = new int[count][4][TET_WIDTH][TET_WIDTH];
        for(int i = 0; i < count; i++){
            for(int j = 0; j < 4; j++){
                for(int x = 0; x < TET_WIDTH; x++){
                    for(int y = 0; y < TET_WIDTH; y++){
                        out[i][j][x][y] = Math.min(in.getRGB(j*TET_WIDTH+x,i*TET_WIDTH+y) & 0xff,1);
                    }
                }
            }
        }
        return out;
    }

    private boolean checkBoardState(){
        tetromino t = currentTetromino;
        boolean collision = true;
        for(int i = 0; i < TET_WIDTH; i++){
            for(int j = 0; j < TET_WIDTH; j++){
                if(tetrominoList[t.i][t.j][i][j] > 0){
                    int x = t.x+i, y = t.y+1+j;
                    if(y < 0 || y >= boardHeight || x < 0 || x >= boardWidth || board[x][y] > 0){
                        collision = false;
                        if((int)(Math.random()*2) == 0)draw.particlePush(29,34,0.05+0.05*Math.random(),boardx+x*main.SPR_WIDTH,boardy+(y-1)*main.SPR_WIDTH,-0.1+0.2*Math.random(),-0.1+0.3*Math.random(),flash[(int)(Math.random()*5)]);
                    }
                }
            }
        }
        return collision;
    }

    private int clearRows(){
        int rows = 0;
        for(int i = boardHeight-1; i > 0; i--){
            int clear = 1;
            for(int j = 0; j < boardWidth; j++){
                if(board[j][i] == 0) clear = 0;
            }
            if(clear == 1){
                for(int y = i; y > 1; y--){
                    for(int x = 0; x < boardWidth; x++) board[x][y] = board[x][y-1];
                }
                rows++;
                break;
            }
        }
        return rows;
    }

    private int checkRows(){
        int rows = 0;
        for(int i = boardHeight-1; i > 0; i--){
            int clear = 1;
            for(int j = 0; j < boardWidth; j++){
                if(board[j][i] == 0) clear = 0;
            }
            if(clear == 1){
                if(rows == 0){
                    cleary = i;
                    cleardy = 0;
                    clearflash = (int)(Math.random()*5);
                }
                rows++;
                for(int k = 0; k < 10; k++) draw.particlePush(29,34,0.05+0.05*Math.random(),boardx+(int)(boardWidth*main.SPR_WIDTH*Math.random()),boardy+cleary*main.SPR_WIDTH,-0.1+0.2*Math.random(),-0.1+0.2*Math.random(),flash[(int)(Math.random()*5)]);
                //break;
            }
        }
        return rows;
    }

    private tetromino spawnTetromino(){
        tetromino out = new tetromino(boardWidth / 2 - TET_WIDTH / 2, 0, nextTetronimo, 0, 10*Math.min(level,9)+4+(int)(Math.random()*3));
        nextTetronimo = (int) (Math.random() * tetrominoList.length);
        return out;
    }

    public tetris(Tetris2805 m, draw2d d) {
        super(m, d);
        draw.clearColour = new Color(24,20,37);
        tetrominoList = getTetrominoes(main.loadTexture("tetrominoatlas.png"));
        boardWidth = 10;
        boardHeight = 22;
        posx = 80;
        posy = 6;
        boardx = posx;
        boardy = posy;
        currentTetromino = spawnTetromino();
        nextTetronimo =
        time = 0;
        score = 0;
        state = 0; // state 0 run, state 1 pause, state 2 gameover, state 3 is clearing
        level = 0;
        lines = 0;

        board = new int[boardWidth][boardHeight];
        for(int i = 0; i < boardWidth; i++){
            for(int j = 0; j < boardHeight; j++){
                board[i][j] = 0;
            }
        }
    }

    @Override
    public void loop(){
        //draw.batchPush(6,10,10,10,10);
        double interpolatespeed = 16-12*main.input.get(KeyEvent.VK_DOWN);
        boardx -= Math.ceil(boardx-posx)*0.2;
        boardy -= Math.ceil(boardy-posy)*0.2;

        time++;
        if(state != 2){
            if(main.input.get(KeyEvent.VK_ESCAPE) == 1) state = 1-state;
            if(state == 0){
                if((time/4f > 60-2*level || main.input.get(KeyEvent.VK_DOWN) == 1) && Math.abs(currentTetromino.dx-currentTetromino.x*main.SPR_WIDTH) + Math.abs(currentTetromino.dy-currentTetromino.y*main.SPR_WIDTH) < 10){
                    time = 0;
                    if(!checkBoardState()){
                        tetromino t = currentTetromino;
                        for(int i = 0; i < TET_WIDTH; i++){
                            for(int j = 0; j < TET_WIDTH; j++){
                                int x = t.x+i, y = t.y+j;
                                if(tetrominoList[t.i][t.j][i][j] > 0) board[x][y] = t.t;
                            }
                        }
                        int rows = checkRows();
                        if(rows > 0){
                            score += scores[rows-1]*(level+1);
                            state = 3;
                        }
                        currentTetromino = spawnTetromino();
                        if(!checkBoardState()) state = 2;
                        boardx += 2-(int)(Math.random()*4);
                        boardy += 2-(int)(Math.random()*4);
                    }else currentTetromino.y++;
                }

                int xp =currentTetromino.x, rp = currentTetromino.j;
                if(main.input.get(KeyEvent.VK_RIGHT) == 1) currentTetromino.x++;
                if(main.input.get(KeyEvent.VK_LEFT) == 1) currentTetromino.x--;
                if(main.input.get(KeyEvent.VK_UP) == 1) currentTetromino.j = (currentTetromino.j+1)%4;
                if(!checkBoardState()){
                    currentTetromino.x = xp;
                    currentTetromino.j = rp;
                }
            }

            draw.batchPush(9,boardx-1,boardy-1+2*main.SPR_WIDTH,boardWidth*main.SPR_WIDTH+1,(boardHeight-2)*main.SPR_WIDTH+1,new Color(38,43,68));
            draw.batchPush(9,boardx,boardy+2*main.SPR_WIDTH,boardWidth*main.SPR_WIDTH+1,(boardHeight-2)*main.SPR_WIDTH+1,new Color(139,155,180));

            // draw board
            for(int i = 0; i < boardWidth; i++){
                for(int j = 2; j < boardHeight; j++){
                    draw.batchPush((i+j)%4,boardx+i*main.SPR_WIDTH,boardy+j*main.SPR_WIDTH, main.SPR_WIDTH,main.SPR_WIDTH);
                }
            }

            for(int i = 0; i < boardWidth; i++){
                for(int j = 2; j < boardHeight; j++){
                    if(board[i][j] != 0 && j != cleary) draw.batchPush(board[i][j],boardx+i*main.SPR_WIDTH,boardy+j*main.SPR_WIDTH + (j < cleary ? (int)cleardy : 0), main.SPR_WIDTH,main.SPR_WIDTH);
                }
            }

            //draw tetromino
            tetromino t = currentTetromino;
            t.dx -= (t.dx-t.x*main.SPR_WIDTH)/interpolatespeed;
            t.dy -= (t.dy-t.y*main.SPR_WIDTH)/interpolatespeed;
            for(int i = 0; i < TET_WIDTH; i++){
                for(int j = 0; j < TET_WIDTH; j++){
                    int x = (int)t.dx+i*(main.SPR_WIDTH), y = (int)t.dy+j*(main.SPR_WIDTH);
                    if(tetrominoList[t.i][t.j][i][j] > 0) draw.batchPush(t.t,boardx+x,boardy+y,main.SPR_WIDTH,main.SPR_WIDTH);
                    if(tetrominoList[nextTetronimo][0][i][j] > 0) draw.batchPush(8,boardx+boardWidth*main.SPR_WIDTH+(i+1)*main.SPR_WIDTH,boardy+(j+4)*main.SPR_WIDTH,main.SPR_WIDTH, main.SPR_WIDTH);
                }
            }

            draw.batchPush(9,boardx,boardy,boardWidth*main.SPR_WIDTH,2*main.SPR_WIDTH-1,new Color(24,20,37));

            if(state == 3){
                cleardy += main.SPR_WIDTH/(16*(1+cleardy));
                boardx += 2-(int)(Math.random()*4);
                boardy += 2-(int)(Math.random()*4);
                double i = cleardy/main.SPR_WIDTH;
                int j = (int)(Math.random()*5);
                draw.batchPush(9,boardx,boardy+cleary*main.SPR_WIDTH+(int)cleardy,boardWidth*main.SPR_WIDTH,Math.max(1,1+main.SPR_WIDTH-(int)cleardy),
                    new Color((int)draw.lerp(flash[clearflash].getRed(),24,i),(int)draw.lerp(flash[clearflash].getBlue(),20,i),(int)draw.lerp(flash[clearflash].getGreen(),37,i)));
                if(cleardy > main.SPR_WIDTH){
                    //for(int k = 0; k < 10; k++) draw.particlePush(30,33,0.03+0.02*Math.random(),boardx+(int)(boardWidth*main.SPR_WIDTH*Math.random()),boardy+cleary*main.SPR_WIDTH,-0.1+0.2*Math.random(),-0.1+0.2*Math.random(),Color.WHITE);
                    cleardy = 0;
                    cleary = 0;
                    clearRows();
                    lines++;
                    if(lines > 10*(level+1)) level++;
                    else if(checkRows() == 0) state = 0;
                }
            }

            if(state == 1){
                draw.drawText("PAUSED",10,20,10,8,null);
                draw.drawText("ESC TO RESUME",10,30,8,6,Color.GRAY);
            }
        }else{
            /*int m = main.mouseInArea(10,40,80,10);
            if(m == 1) main.cursorcontext = m;
            int c = 255-80*m;
            draw.batchPush(9,10+m,40+m,80-2*m,10-2*m,new Color(24,20,37));
            draw.drawBox(10-m,40-m,80+2*m,10+2*m,7-m);
            draw.drawText("MAIN MENU",11+5*m,41,8,6,new Color(c,c,c,160+(int) (Math.sin((main.frame/main.TPS) * 2*Math.PI))*80));
            if(m == 1 && main.input.get(-1) == 1) main.currentScene = new splash(main,draw);*/
            if(draw.drawButton("MAIN MENU",10,40,80,10) == 1) main.currentScene = new menu(main,draw);
            if(draw.drawButton("QUIT",10,51,80,10) == 1) System.exit(1);

            draw.drawText("GAME OVER",10,20,10,10,Color.RED);

            // TODO text field, push highscore
        }

        draw.drawText(""+level,10,10,10,8,Color.WHITE);
        draw.drawText(""+score,10,20,8,6,flash[(score/100)%5]);
        draw.drawText(main.keybuffer,10,40,8,6,Color.GRAY);
    }
}

class menu extends scene {
    private int time;
    public menu(Tetris2805 m, draw2d d) {
        super(m, d);
        time = 0;
        draw.clearColour = new Color(24,20,37);
    }
    @Override
    public void loop(){
        if(time < main.TPS) time++;
        double a = time/main.TPS;

        draw.drawText("TETRIS",20,20,10,8,new Color((int)(255*a),(int)(255*a),(int)(255*a)));
        draw.drawText("JAVA GAME",20,30,8,6,new Color((int)(255*(a/2)),(int)(255*(a/2)),(int)(255*(a/2))));

        int bx = main.FRAMEBUFFER_W/2-100;
        if(a > 0.25 && draw.drawButton("PLAY",bx,40,80,10) == 1) main.currentScene = new tetris(main,draw);
        if(a > 0.5 && draw.drawButton("CONFIGURE",bx,51,80,10) == 1) main.currentScene = new config(main,draw);
        if(a > 0.75 && draw.drawButton("HIGHSCORE",bx,62,80,10) == 1) main.currentScene = new hscore(main,draw);
        if(a >= 1 && draw.drawButton("EXIT",bx,73,80,10) == 1) main.gameShouldClose = 1;
    }

}

class config extends scene {
    private int time;
    public config(Tetris2805 m, draw2d d) {
        super(m, d);
        time = 0;
        draw.clearColour = new Color(24,20,37);
    }
    @Override
    public void loop(){
        if(time < main.TPS) time++;
        double a = time/main.TPS;
        draw.drawText("CONFIGURE",20,20,10,8,new Color((int)(255*a),(int)(255*a),(int)(255*a)));
    }

}

class hscore extends scene {
    private int time;
    private ArrayList<Map.Entry<String, Integer>> list;
    public hscore(Tetris2805 m, draw2d d) {
        super(m, d);
        time = 0;
        draw.clearColour = new Color(24,20,37);
        list = new ArrayList<>(main.scores.entrySet());
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
    }
    @Override
    public void loop(){
        if(time < main.TPS) time++;
        double a = time/main.TPS;
        draw.drawText("HIGHSCORES",20,20,10,8,new Color((int)(255*a),(int)(255*a),(int)(255*a)));
        int i = 0;
        for(Map.Entry<String, Integer> entry : list){
            draw.drawText(entry.getKey() + " " + entry.getValue(),20,30+8*i,8,6,new Color((int)(255*(a/2)),(int)(255*(a/2)),(int)(255*(a/2))));
            i++;
        }
    }

}
