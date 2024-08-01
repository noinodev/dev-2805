import java.awt.*;
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
        if(time > 3*main.TPS) main.currentScene = new tetris(main,draw);

        draw.batchPush(7,15,15,10,10);
    }
}

class tetris extends scene {
    private boolean paused;
    private int boardWidth,boardHeight,time,score;
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
        int[][][][] out = new int[count][4][main.TET_WIDTH][main.TET_WIDTH];
        for(int i = 0; i < count; i++){
            for(int j = 0; j < 4; j++){
                for(int x = 0; x < main.TET_WIDTH; x++){
                    for(int y = 0; y < main.TET_WIDTH; y++){
                        out[i][j][x][y] = Math.min(in.getRGB(j*main.TET_WIDTH+x,i*main.TET_WIDTH+y) & 0xff,1);
                    }
                }
            }
        }
        return out;
    }

    private boolean checkBoardState(){
        tetromino t = currentTetromino;
        for(int i = 0; i < main.TET_WIDTH; i++){
            for(int j = 0; j < main.TET_WIDTH; j++){
                int x = t.x+i, y = t.y+1+j;
                if(y > 12 || (tetrominoList[t.i][t.j][i][j] & board[x][y]) > 0) return false;
            }
        }
        currentTetromino.y++;
        return true;
    }

    private tetromino spawnTetromino(){
        return new tetromino(boardWidth/2-main.TET_WIDTH/2,0,(int)(Math.random()*tetrominoList.length),0,1);
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
        if(time%(main.TPS/(4*1)) == 0){
            System.out.println("checking board state");
            if(!checkBoardState()){
                System.out.println("collision");
                tetromino t = currentTetromino;
                for(int i = 0; i < main.TET_WIDTH; i++){
                    for(int j = 0; j < main.TET_WIDTH; j++){
                        int x = t.x+i, y = t.y+j;
                        if(tetrominoList[t.i][t.j][i][j] > 0) board[x][y] = t.t;
                    }
                }
                currentTetromino = spawnTetromino();
            }

        }

        for(int i = 0; i < boardWidth; i++){
            for(int j = 2; j < boardHeight; j++){
                draw.batchPush(board[i][j],10+i*(main.SPR_WIDTH/2),10+j*(main.SPR_WIDTH/2),main.SPR_WIDTH/2,main.SPR_WIDTH/2);
            }
        }

        tetromino t = currentTetromino;
        for(int i = 0; i < main.TET_WIDTH; i++){
            for(int j = 0; j < main.TET_WIDTH; j++){
                int x = t.x+i, y = t.y+j;
                if(tetrominoList[t.i][t.j][i][j] > 0) draw.batchPush(t.t,10+x*(main.SPR_WIDTH/2),10+y*(main.SPR_WIDTH/2),main.SPR_WIDTH/2,main.SPR_WIDTH/2);
            }
        }
    }
}

class menu extends scene {
    public menu(Tetris2805 m, draw2d d) {
        super(m, d);
    }

}
