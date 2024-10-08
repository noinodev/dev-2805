import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class ObjectTetromino extends PlayerObject {
    //public byte inst = 3;
    private static int[][][][] getTetrominoes(BufferedImage in){
        int count = in.getHeight()/4;
        int[][][][] out = new int[count][4][4][4];
        for(int i = 0; i < count; i++){
            for(int j = 0; j < 4; j++){
                for(int x = 0; x < 4; x++){
                    for(int y = 0; y < 4; y++){
                        // i=tetromino index, j=rotation index, x=x, y=y
                        // it was easier to handle rotations this way, as if im gonna do a rotation matrix just for this that would be silly
                        out[i][j][x][y] = Math.min(in.getRGB(j*4+x,i*4+y) & 0xff,1); // set cells of tetromino grid by colour data
                    }
                }
            }
        }
        return out;
    }

    public static int[][][][] tetrominoList = getTetrominoes(Tetris2805.loadTexture("resources/load/tetrominoatlas.png"));

    public int index,rotation,t;
    int dx,dy;
    //ObjectTetromino[] pieces;
    //public ObjectTetromino best;

    public ObjectTetromino(Game game, PlayerControlScheme pcs, int sprite, int _x, int _y, int _i, int _j){
        super(game,pcs);
        this.sprite = sprite;
        this.x = _x;
        this.y = _y;
        this.dx = (int)x/main.SPR_WIDTH;
        this.dy = (int)y/main.SPR_WIDTH;
        this.index = _i;
        this.rotation = _j;
        inst = 3;
    }

    public ObjectTetromino(ObjectTetromino other) { // only used for the ai agent
        super(other.game,other.control_scheme);
        this.dx = other.dx;
        this.dy = other.dy;
        this.index = other.index;
        this.rotation = other.rotation;
        inst = 3;
    }


    @Override
    public void update(){

        switch(control_scheme){
            case PCS_LOCAL:
                change = 1;
                controlLocal(game);
                break;
            case PCS_EXTERN:

                break;
            case PCS_AI:
                change = 1;
                controlLocal(game);
                break;
        }

        int time = game.time;
        if(game.game_start_wait <= 0 && game.state == game.STATE_PLAY){
            if((time/4. > Math.max(1,60-6*game.level) || (main.input.get(KeyEvent.VK_DOWN)%12 == 1 && control_scheme != PlayerControlScheme.PCS_EXTERN)) && Math.abs(x-dx*main.SPR_WIDTH) + Math.abs(y-dy*main.SPR_WIDTH) < 10){
                AudioManager.audio.get("fwop1").play(0f,280f,0f,0f);
                game.time = 0;
                if(!checkBoardState()){ // collision on drop
                    // merge tetromino
                    AudioManager.audio.get("tap2").play(x,y,draw.view_x+draw.view_w/2,draw.view_y+draw.view_h/2);
                    for(int i = 0; i < game.TET_WIDTH; i++){
                        for(int j = 0; j < game.TET_WIDTH; j++){
                            int tx = dx+i, ty = dy+j;
                            if(tetrominoList[index][rotation][i][j] > 0){
                                game.board[tx][ty] = (int)sprite;
                                game.light[tx][ty] = 100;
                            }
                        }
                    }
                    // add score and set state to clear rows
                    int rows = game.checkRows();
                    if(rows > 0){ //
                        game.score += game.scores[rows-1]*(game.level+1);
                        game.state = game.STATE_CLEAR;
                    }

                    ResetTetromino();
                    if(!checkBoardState()){ // fail state, if tetromino spawns fouled then state is set to lose
                        game.lives--;
                        game.clearx = 0;
                        game.state = game.STATE_LOSE;
                    }
                    game.boardx += 2-(int)(Math.random()*4); // shake the board
                    game.boardy += 2-(int)(Math.random()*4);
                    game.illum = 0.5; // light up the board
                }else{
                    dy++; // drop tetromino
                    game.illum *= 0.8;
                }
            }
        }

        x -= (x-dx*main.SPR_WIDTH)/Math.max(1,16-game.level); // interpolate tetronimo position
        y -= (y-dy*main.SPR_WIDTH)/Math.max(1,16-game.level);
        for(int i = 0; i < game.TET_WIDTH; i++){
            for(int j = 0; j < game.TET_WIDTH; j++){
                if(tetrominoList[index][rotation][i][j] > 0){
                    draw.batchPush((int)sprite,game.boardx+x+i* main.SPR_WIDTH,game.boardy+y+j* main.SPR_WIDTH,main.SPR_WIDTH,main.SPR_WIDTH); // draw tetromino
                    if(dx+i >= 0 && dx+i < game.boardWidth && dy+j >= 2 && dy+j < game.boardHeight && game.board[dx+i][dy+j] == 0) game.light[dx+i][dy+j] = 10; // illuminate current spot
                }
                if(tetrominoList[game.nextTetronimo][0][i][j] > 0) draw.batchPush(8,draw.view_x+20+(i+1)*main.SPR_WIDTH,draw.view_y+game.boardy+(j+4)*main.SPR_WIDTH,main.SPR_WIDTH, main.SPR_WIDTH); // show next tetromino in hud
            }
        }
    }

    public boolean checkBoardState(){ // tetromino collision function
        boolean collision = true;
        int[][] board = game.board;
        for(int i = 0; i < game.TET_WIDTH; i++){
            for(int j = 0; j < game.TET_WIDTH; j++){
                if(tetrominoList[index][rotation][i][j] > 0){
                    int tx = dx+i, ty = dy+1+j; // normalized x and y for tetromino position
                    if(ty < 0 || ty >= game.boardHeight || tx < game.board_bound_x || tx >= game.board_bound_x+game.board_bound_w || (board[tx][ty] > 0 && board[tx][ty] < 160)){ // hack to ignore decorative tiles
                        collision = false; // this doesnt make sense because if this happens then there is a collision, but 'nocollision' as a variable name seems silly and i didnt want to reverse usages of the function
                        // create particles even for potential collision because it gives player feedback
                        if((int)(Math.random()*2) == 0){
                            CreateObject(new ObjectParticle(game,game.boardx+tx*main.SPR_WIDTH,game.boardy+(ty-1)*main.SPR_WIDTH,
                                    -0.1+0.2*Math.random(),-0.1+0.3*Math.random(),
                                    30,34,0.05+0.05*Math.random(),240,game.flash[(int)(Math.random()*5)]));
                        }
                    }
                }
            }
        }
        return collision;
    }

    private void controlLocal(Game game){
        // all tetromino inputs. play a sound when you move
        int xp =dx, rp = rotation;
        if(main.input.get(KeyEvent.VK_RIGHT) == 1 || main.input.get(KeyEvent.VK_RIGHT) > main.TPS/8) {
            dx++;
            AudioManager.audio.get("tap1").play(x,y, draw.view_x+draw.view_w/2, draw.view_y+draw.view_h/2);
        }
        if(main.input.get(KeyEvent.VK_LEFT) == 1 || main.input.get(KeyEvent.VK_LEFT) > main.TPS/8) {
            dx--;
            AudioManager.audio.get("tap2").play(x,y, draw.view_x+draw.view_w/2, draw.view_y+draw.view_h/2);
        }
        if(main.input.get(KeyEvent.VK_UP) == 1 || main.input.get(KeyEvent.VK_UP) > main.TPS/8){
            rotation = (rotation+1)%4;
            AudioManager.audio.get("fwop1").play(x,y, draw.view_x+draw.view_w/2, draw.view_y+draw.view_h/2);
        }
        if(main.input.get(KeyEvent.VK_A) == 1 && game.board_bound_x > 0){
            game.board_bound_x -= 1;
            dx -= 1;
            AudioManager.audio.get("slide2").play(x,y, draw.view_x+draw.view_w/2, draw.view_y+draw.view_h/2);
        }
        if(main.input.get(KeyEvent.VK_D) == 1 && game.board_bound_x < game.boardWidth-game.board_bound_w){
            game.board_bound_x += 1;
            dx += 1;
            AudioManager.audio.get("slide2").play(x,y, draw.view_x+draw.view_w/2, draw.view_y+draw.view_h/2);
        }

        // focus camera near tetromino
        draw.view_x -= (draw.view_x-(x-main.FRAMEBUFFER_W/4.))*0.1;
        draw.view_y -= (draw.view_y-(y-main.FRAMEBUFFER_H/4.)*.4)*0.1;
        if(!checkBoardState()){
            dx = xp;
            rotation = rp;
        }
    }

    public void ResetTetromino(){
        dx = game.board_bound_x+game.board_bound_w/2-game.TET_WIDTH/2;
        dy = 0;
        x = dx*main.SPR_WIDTH;
        y = dy*main.SPR_WIDTH;
        this.index = game.nextTetronimo;
        this.sprite = 10*Math.min(game.level/2,5)+4+(int)(Math.random()*4);
        game.nextTetronimo = (int)(Math.random() * tetrominoList.length);
    }
}
