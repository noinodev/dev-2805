

public class TetrisPlayer {
    public Game game;
    public Tetris2805 main;

    private class Tetromino { // tetromino class
        public int x,y,i,j,t;
        double dx,dy;
        public Tetromino(int _x, int _y, int _i, int _j, int _t){
            x = _x;
            y = _y;
            dx = x*main.SPR_WIDTH;
            dy = y*main.SPR_WIDTH;
            i = _i;
            j = _j;
            t = _t;
        }
    }
    private Tetromino currentTetromino;
}
