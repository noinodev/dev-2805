import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

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
        if(time > main.TPS) main.currentScene = new tetris(main,draw);

        draw.batchPush(7,15,15,10,10);
        draw.drawText("HELLO WORLD..",20,20,10,8,new Color(time,time,time));
    }
}

class tetris extends scene {
    public final int TET_WIDTH = 4;
    private int boardWidth,boardHeight,time,score,state;
    private int[][] board;
    private int[][][][] tetrominoList;
    private class tetromino {
        public int x,y,i,j,t;
        public tetromino(int _x, int _y, int _i, int _j, int _t){
            x = _x;
            y = _y;
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
        for(int i = 0; i < TET_WIDTH; i++){
            for(int j = 0; j < TET_WIDTH; j++){
                if(tetrominoList[t.i][t.j][i][j] > 0){
                    int x = t.x+i, y = t.y+1+j;
                    if(y < 0 || y >= boardHeight || x < 0 || x >= boardWidth || board[x][y] > 0) return false;
                }
            }
        }
        return true;
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
                i++;
                rows++;
            }
        }
        return rows;
    }

    private tetromino spawnTetromino(){
        return new tetromino(boardWidth / 2 - TET_WIDTH / 2, 0, (int) (Math.random() * tetrominoList.length), 0, 1+(int)(Math.random()*3));
    }

    public tetris(Tetris2805 m, draw2d d) {
        super(m, d);
        draw.clearColour = new Color(62,39,49);
        tetrominoList = getTetrominoes(main.loadTexture("tetrominoatlas.png"));
        boardWidth = 10;
        boardHeight = 22;
        currentTetromino = spawnTetromino();
        time = 0;
        score = 0;
        state = 0; // state 0 run, state 1 pause, state 2 gameover

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

        time++;
        if(state != 2){
            if(main.input.get(KeyEvent.VK_ESCAPE) == 1) state = 1-state;
            if(state != 1){
                if(time%(main.TPS/(Math.max(1,score/250f))) < 1 || main.input.get(KeyEvent.VK_DOWN) == 1){
                    time = 0;
                    if(!checkBoardState()){
                        tetromino t = currentTetromino;
                        for(int i = 0; i < TET_WIDTH; i++){
                            for(int j = 0; j < TET_WIDTH; j++){
                                int x = t.x+i, y = t.y+j;
                                if(tetrominoList[t.i][t.j][i][j] > 0) board[x][y] = t.t;
                            }
                        }
                        int rows = clearRows();
                        if(rows > 0) score += Math.abs(100*rows*(1+Math.log(rows)));
                        currentTetromino = spawnTetromino();
                        if(!checkBoardState()) state = 2;
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

            // draw
            for(int i = 0; i < boardWidth; i++){
                for(int j = 2; j < boardHeight; j++){
                    draw.batchPush(board[i][j],i*(main.SPR_WIDTH),j*(main.SPR_WIDTH), main.SPR_WIDTH,main.SPR_WIDTH);
                }
            }

            tetromino t = currentTetromino;
            for(int i = 0; i < TET_WIDTH; i++){
                for(int j = 0; j < TET_WIDTH; j++){
                    int x = t.x+i, y = t.y+j;
                    if(tetrominoList[t.i][t.j][i][j] > 0) draw.batchPush(t.t,x*(main.SPR_WIDTH),y*(main.SPR_WIDTH),main.SPR_WIDTH,main.SPR_WIDTH);
                }
            }
            if(state == 1){
                draw.drawText("PAUSED",10,20,10,8,null);
                draw.drawText("ESC TO RESUME",10,30,8,6,Color.GRAY);
            }
        }else{
            draw.drawBox(10,40,80,10,7);
            draw.drawText("MAIN MENU",12,42,8,6,Color.GRAY);
            if(main.mouseInArea(10,40,80,10) == 1 && main.input.get(-1) == 1) main.currentScene = new splash(main,draw);

            draw.drawText("GAME OVER",10,20,10,10,Color.RED);
        }

        draw.drawText(""+score,10,10,10,10,new Color(155+score%100,155+score%100,155+score%100));
        draw.drawText(main.keybuffer,10,40,8,6,Color.GRAY);

        draw.batchPush(6+main.input.get(-1),(int)main.mousex,(int)main.mousey,6,6);
    }
}

class menu extends scene {
    public menu(Tetris2805 m, draw2d d) {
        super(m, d);
    }

    @Override
    public void loop(){

    }

}
