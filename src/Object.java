import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.UUID;

enum ecs {
    ECS_TETROMINO,
    ECS_PARTICLE,
    ECS_GOBLIN,
    ECS_CONTROL_AI,
    ECS_CONTROL_LOCAL,
    ECS_CONTROL_NETWORK
}

enum PlayerControlScheme {
    PCS_LOCAL,
    PCS_EXTERN,
    PCS_AI
}

public abstract class Object {
    public int destroy,id;
    public double sprite,x,y,w,h,xd,hsp,vsp,grv;
    public Tetris2805 main;
    public D2D draw;
    public Game game;
    public Object(Game game){
        this.game = game;
        this.main = game.main;
        this.draw = game.draw;
        this.destroy = 0;
        this.w = main.SPR_WIDTH;
        this.h = main.SPR_WIDTH;
        sprite = 0;
        x = 0;
        y = 0;
        hsp = 0;
        vsp = 0;
        grv = 0.02;

    }
    public void update(){ }

    public static final ArrayList<Object> objects = new ArrayList<Object>();
    public static Object CreateObject(Object object){
        objects.add(object);
        return object;
    }
    public static void DestroyAllObjects(){
        objects.clear();
    }
}

class ObjectResource extends Object {
    public int resource, hp, hps, timer;
    public ObjectResource(Game game, int x, int y, int sprite, int resource, int hp){
        super(game);
        this.x = x;
        this.y = y;
        this.sprite = sprite;
        this.resource = resource;
        this.hp = hp;
        this.hps = hp;

        w = D2D.sprites[sprite].getWidth();
        h = D2D.sprites[sprite].getHeight();
        timer = 0;
    }

    @Override
    public void update(){
        double dmg = (1-(double)hp/hps)*0.6;
        timer--;
        hsp -= hsp*0.1;
        vsp -= vsp*0.1;
        if(Math.abs(main.mousex-x)+Math.abs(main.mousey-y) < 20){
            dmg += 0.2;
            if(main.input.get(-1) == 1 && timer <= 0){
                timer = 240;
                hp -= 1;
                hsp =  4-2*Math.random();
                vsp = Math.random();
            }
        }
        int bx = Math.max(Math.min((int)(Math.floor(x-game.boardx)/main.SPR_WIDTH),game.boardWidth-1),0);
        int by = Math.max(Math.min((int)(Math.floor(y-game.boardy)/main.SPR_WIDTH),game.boardHeight-1),0);

        if(hp <= 0) destroy = 1;
        double disttocenter = Math.abs(x - (draw.view_x+draw.view_w/2))/(draw.view_w*0.5);
        double l = Math.min(1,Math.max(0,Math.min(1-(4+game.light[bx][by])/10f,disttocenter)+dmg));
        Color c = new Color((int)D2D.lerp(255,24,l),(int)D2D.lerp(255,20,l),(int)D2D.lerp(255,37,l));
        draw.batchPush((int)sprite,x-w/2+hsp,y-h+vsp,w,h,c);
    }
}

class ObjectParticle extends Object {
    public int start, end, time;
    public double spd;
    public Color colour;
    public ObjectParticle(Game game,int x, int y, double hsp, double vsp, int start,int end,double spd,int time, Color colour){
        super(game);
        this.x = x;
        this.y = y;
        this.hsp = hsp;
        this.vsp = vsp;
        this.sprite = start;
        this.start = start;
        this.end = end;
        this.spd = spd;
        this.time = time;
        this.colour = colour;
    }

    @Override
    public void update(){
        if(destroy == 1) return;
        sprite += spd;
        if(sprite > end) destroy = 1;
        time--;
        if(time <= 0) destroy = 1;
        draw.batchPush((int)Math.floor(sprite),x,y,w,h,colour);
    }
}

abstract class PlayerObject extends Object {
    public PlayerControlScheme control_scheme;
    public PlayerObject(Game game, PlayerControlScheme pcs){
        super(game);
        this.control_scheme = pcs;
    }
}

class ObjectCharacter extends PlayerObject {
    public static final String[] taunts = {"YOU SUCK","???","GONNA CRY?","LOL","AINT MEAN IF U AINT GREEN","GOBLINZ RULE","BUYING GOBLIN GF","SO GOBLINCORE","GOBLINMAXXING RN",
            "WOW...","ZZZ","STOP TRYING","IM IN JAVA?","GOBLINPILLED","GOBLIN4LIFE","...","THEY NOT LIKE US","I LOVE GRIMES"};
    public static final Color[] tauntcolours = {new Color(104,46,108), new Color(38,92,66), new Color(25,60,62), new Color(58,68,102), new Color(38,43,68), new Color(62,39,49)};
    public String txt,taunt;
    public Color txtcolour;

    public ObjectCharacter(Game game, PlayerControlScheme pcs, int sprite, int x, int y){
        super(game,pcs);
        this.sprite = sprite;
        this.x = x;
        this.y = y;
        txt = "";
        taunt = "";
        txtcolour = tauntcolours[(int)(Math.random()*tauntcolours.length)];
    }

    @Override
    public void update(){
        if(game.state == game.STATE_PLAY){
            if(hsp != 0) xd = hsp > 0 ? 1 : -1; // set facing direction
            if(game.pointCheck(x+2*xd+hsp,y) == 1) hsp = 0; // collisions
            if(game.pointCheck(x,y+vsp) == 1) vsp = 0;
            x += hsp;
            y += vsp;
            vsp += grv; // gravity
            if(txt.length() == taunt.length()){ // taunt reset
                if(txt.length() > 0 && (int)(Math.random()*(main.TPS*2)) == 0){
                    txt = "";
                    taunt = "";
                }
            }

            switch(control_scheme){
                case PCS_LOCAL:
                    //
                    main.inputtype = 1;
                    draw.view_x -= (draw.view_x-(x-main.FRAMEBUFFER_W/2.))*0.1;
                    draw.view_y -= (draw.view_y-(y-main.FRAMEBUFFER_H/2.))*0.1;
                    hsp = (main.input.get(KeyEvent.VK_D)-main.input.get(KeyEvent.VK_A))*0.2;
                    if(main.input.get(KeyEvent.VK_W) == 1 && game.pointCheck(x,y+2) == 1) vsp -= 0.5;
                    //System.out.println(Math.abs(x - (draw.view_x+draw.view_w/2)));
                    /* == 1 || main.input.get(KeyEvent.VK_RIGHT) > main.TPS/8) dx++;
                    if(main.input.get(KeyEvent.VK_LEFT) == 1 || main.input.get(KeyEvent.VK_LEFT) > main.TPS/8) dx--;
                    if(main.input.get(KeyEvent.VK_UP) == 1 || main.input.get(KeyEvent.VK_UP) > main.TPS/8) rotation = (rotation+1)%4;
                    if(main.input.get(KeyEvent.VK_A) == 1 && game.board_bound_x > 0)*/
                break;
                case PCS_EXTERN:
                    //
                break;
                case PCS_AI:
                    //System.out.print("gobby... ");
                    if(taunt.length() > txt.length() && (int)(Math.random()*5) == 0) txt = taunt.substring(0,txt.length()+1); // taunt animation
                    if(game.state == game.STATE_LOSE || (taunt == "" && (int)(Math.random()*(main.TPS*5)) == 0)) taunt = taunts[(int)(Math.random()*taunts.length)]; // new taunt

                    // run away from current tetromino
                    /*if(Math.abs(currentTetromino.dx+boardx+2*main.SPR_WIDTH-e.x) < 2*main.SPR_WIDTH) e.hsp = -(currentTetromino.dx+boardx+2*main.SPR_WIDTH-e.x)*0.01;
                    else */if((int)(Math.random()*main.TPS) == 0) hsp = -0.25+0.5*Math.random(); // wander
                    if((int)(Math.random()*main.TPS) == 0) hsp = 0;
                break;
            }

            if(game.pointCheck(x,y) == 1){ // die from being crushed
                destroy = 1;
                //for(int j = 0; j < 4; j++) draw.particlePush(130,134,0.03+0.02*Math.random(),(int)x,(int)y,-0.1+0.2*Math.random(),-0.1+0.2*Math.random(), Color.WHITE);
                //draw.particlePush(150,154,0.09+0.01*Math.random(),(int)x-main.SPR_WIDTH/2,(int)y-main.SPR_WIDTH,-0.01+0.02*Math.random(),-0.08,Color.WHITE);
                for(int j = 0; j < 4; j++) CreateObject(new ObjectParticle(game,(int)x,(int)y,-0.1+0.2*Math.random(),-0.1+0.2*Math.random(),130,134,0.03+0.02*Math.random(),240,Color.WHITE));
                CreateObject(new ObjectParticle(game, (int)x-main.SPR_WIDTH/2, (int)y-main.SPR_WIDTH, -0.01+0.02*Math.random(), -0.08, 150, 154, 0.09+0.01*Math.random(), 240, Color.WHITE));
            }
        }

        // draw self
        int bx = Math.max(Math.min((int)(Math.floor(x-game.boardx)/main.SPR_WIDTH),game.boardWidth-1),0);
        int by = Math.max(Math.min((int)(Math.floor(y-game.boardy)/main.SPR_WIDTH),game.boardHeight-1),0);

        double disttocenter = Math.abs(x - (draw.view_x+draw.view_w/2))/(draw.view_w*0.5);
        double l = Math.min(1,Math.max(0,Math.min(1-(4+game.light[bx][by])/10f,disttocenter)));
        Color c = new Color((int)D2D.lerp(255,24,l),(int)D2D.lerp(255,20,l),(int)D2D.lerp(255,37,l));

        draw.batchPush((int)sprite+(int)(main.frame/(main.TPS/2))%2+(hsp != 0 ? 1 : 0),
                (int)x-xd*main.SPR_WIDTH/2,
                (int)y-main.SPR_WIDTH+(int)Math.abs(Math.sin((main.frame/main.TPS + id*234)*Math.PI)*Math.abs(hsp)),
                xd*main.SPR_WIDTH,main.SPR_WIDTH,c);
        // draw taunt
        if(txt != ""){
            draw.batchPush(9,(int)x-10,(int)y-18-(int)x%2,txt.length()*6,8,new Color(24,20,37));
            draw.drawText(txt,(int)x-10,(int)y-18-(int)x%2,8,6,txtcolour);
        }
    }
}

class ObjectTetromino extends PlayerObject {
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

    public static int[][][][] tetrominoList = getTetrominoes(Tetris2805.loadTexture("resources/load/tetrominoatlas5.png"));

    public int index,rotation,t;
    int dx,dy;
    public ObjectTetromino(Game game, PlayerControlScheme pcs, int sprite, int _x, int _y, int _i, int _j){
        super(game,pcs);
        this.sprite = sprite;
        this.x = _x;
        this.y = _y;
        this.dx = (int)x/main.SPR_WIDTH;
        this.dy = (int)y/main.SPR_WIDTH;
        this.index = _i;
        this.rotation = _j;
    }


    @Override
    public void update(){
        switch(control_scheme){
            case PCS_LOCAL:
                controlLocal(game);
            break;
            case PCS_EXTERN:

            break;
            case PCS_AI:

            break;
        }
        /*int time = game.time;

        if((time/4f > Math.max(1,60-6*game.level) || main.input.get(KeyEvent.VK_DOWN)%12 == 1) && Math.abs(currentTetromino.dx-currentTetromino.x*main.SPR_WIDTH) + Math.abs(currentTetromino.dy-currentTetromino.y*main.SPR_WIDTH) < 10){
            time = 0;
            if(!checkBoardState()){ // collision on drop
                Game.Tetromino t = currentTetromino;
                // merge tetromino
                for(int i = 0; i < TET_WIDTH; i++){
                    for(int j = 0; j < TET_WIDTH; j++){
                        int x = t.x+i, y = t.y+j;
                        if(tetrominoList[t.i][t.j][i][j] > 0) board[x][y] = t.t;
                    }
                }
                // add score and set state to clear rows
                int rows = checkRows();
                if(rows > 0){ //
                    score += scores[rows-1]*(level+1);
                    state = STATE_CLEAR;
                }
                // spawn new tetromino
                currentTetromino = spawnTetromino();
                if(!checkBoardState()){ // fail state, if tetromino spawns fouled then state is set to lose
                    lives--;
                    clearx = 0;
                    state = STATE_LOSE;
                }
                boardx += 2-(int)(Math.random()*4); // shake the board
                boardy += 2-(int)(Math.random()*4);
                illum = 0.5; // light up the board
            }else{
                currentTetromino.y++; // drop tetromino
                illum *= 0.8;
            }
        }
        // all other tetromino inputs
        int xp =currentTetromino.x, rp = currentTetromino.j;
        if(main.input.get(KeyEvent.VK_RIGHT) == 1 || main.input.get(KeyEvent.VK_RIGHT) > main.TPS/8) currentTetromino.x++;
        if(main.input.get(KeyEvent.VK_LEFT) == 1 || main.input.get(KeyEvent.VK_LEFT) > main.TPS/8) currentTetromino.x--;
        if(main.input.get(KeyEvent.VK_UP) == 1 || main.input.get(KeyEvent.VK_UP) > main.TPS/8) currentTetromino.j = (currentTetromino.j+1)%4;

        dx -= (dx-x*main.SPR_WIDTH)/interpolatespeed; // interpolate tetronimo position
        dy -= (dy-y*main.SPR_WIDTH)/interpolatespeed;
        for(int i = 0; i < TET_WIDTH; i++){
            for(int j = 0; j < TET_WIDTH; j++){
                int x = (int)t.dx+i*(main.SPR_WIDTH), y = (int)t.dy+j*(main.SPR_WIDTH); // normalized coordinates
                if(tetrominoList[t.i][t.j][i][j] > 0){
                    draw.batchPush(t.t,boardx+x,boardy+y,main.SPR_WIDTH,main.SPR_WIDTH); // draw tetromino
                    if(t.x+i >= 0 && t.x+i < boardWidth && t.y+j >= 2 && t.y+j < boardHeight && board[t.x+i][t.y+j] == 0) light[t.x+i][t.y+j] = 10; // illuminate current spot
                }
                if(tetrominoList[nextTetronimo][0][i][j] > 0) draw.batchPush(8,20+(i+1)*main.SPR_WIDTH,boardy+(j+4)*main.SPR_WIDTH,main.SPR_WIDTH, main.SPR_WIDTH); // show next tetromino in hud
            }
        }*/

        x -= (x-dx*main.SPR_WIDTH)/16; // interpolate tetronimo position
        y -= (y-dy*main.SPR_WIDTH)/16;
        for(int i = 0; i < game.TET_WIDTH; i++){
            for(int j = 0; j < game.TET_WIDTH; j++){
                //int tx = (int)x+i*(main.SPR_WIDTH), ty = (int)y+j*(main.SPR_WIDTH); // normalized coordinates
                if(tetrominoList[index][rotation][i][j] > 0){
                    draw.batchPush((int)sprite,game.boardx+x+i* main.SPR_WIDTH,game.boardy+y+j* main.SPR_WIDTH,main.SPR_WIDTH,main.SPR_WIDTH); // draw tetromino
                    if(dx+i >= 0 && dx+i < game.boardWidth && dy+j >= 2 && dy+j < game.boardHeight && game.board[dx+i][dy+j] == 0) game.light[dx+i][dy+j] = 10; // illuminate current spot
                }
                if(tetrominoList[game.nextTetronimo][0][i][j] > 0) draw.batchPush(8,20+(i+1)*main.SPR_WIDTH,game.boardy+(j+4)*main.SPR_WIDTH,main.SPR_WIDTH, main.SPR_WIDTH); // show next tetromino in hud
            }
        }
    }

    private boolean checkBoardState(){ // tetromino collision function
        boolean collision = true;
        int[][] board = game.board;
        for(int i = 0; i < game.TET_WIDTH; i++){
            for(int j = 0; j < game.TET_WIDTH; j++){
                if(tetrominoList[index][rotation][i][j] > 0){
                    int tx = dx+i, ty = dy+1+j; // normalized x and y for tetromino position
                    if(ty < 0 || ty >= game.boardHeight || tx < game.board_bound_x || tx >= game.board_bound_x+game.board_bound_w || (board[tx][ty] > 0 && board[tx][ty] < 160)){ // hack to ignore decorative tiles
                        collision = false; // this doesnt make sense because if this happens then there is a collision, but 'nocollision' as a variable name seems silly and i didnt want to reverse usages of the function
                        // create particles even for potential collision because it gives player feedback
                        if((int)(Math.random()*2) == 0) draw.particlePush(29,34,0.05+0.05*Math.random(),game.boardx+tx*main.SPR_WIDTH,game.boardy+(ty-1)*main.SPR_WIDTH,-0.1+0.2*Math.random(),-0.1+0.3*Math.random(),game.flash[(int)(Math.random()*5)]); // yuck
                        if((int)(Math.random()*2) == 0) CreateObject(new ObjectParticle(game,game.boardx+tx*main.SPR_WIDTH,game.boardy+(ty-1)*main.SPR_WIDTH,-0.1+0.2*Math.random(),-0.1+0.3*Math.random(),29,34,0.05+0.05*Math.random(),240,Color.WHITE));
                        //CreateObject(new ObjectParticle(game, (int)x-main.SPR_WIDTH/2, (int)y-main.SPR_WIDTH, -0.01+0.02*Math.random(), -0.08, 150, 154, 0.09+0.01*Math.random(), 240, Color.WHITE));
                    }
                }
            }
        }
        return collision;
    }

    private void controlLocal(Game game){
        int time = game.time;

        if((time/4f > Math.max(1,60-6*game.level) || main.input.get(KeyEvent.VK_DOWN)%12 == 1) && Math.abs(x-dx*main.SPR_WIDTH) + Math.abs(y-dy*main.SPR_WIDTH) < 10){
            game.time = 0;
            if(!checkBoardState()){ // collision on drop
                // merge tetromino
                for(int i = 0; i < game.TET_WIDTH; i++){
                    for(int j = 0; j < game.TET_WIDTH; j++){
                        int tx = dx+i, ty = dy+j;
                        if(tetrominoList[index][rotation][i][j] > 0) game.board[tx][ty] = (int)sprite;
                    }
                }
                // add score and set state to clear rows
                int rows = game.checkRows();
                if(rows > 0){ //
                    game.score += game.scores[rows-1]*(game.level+1);
                    game.state = game.STATE_CLEAR;
                }
                // spawn new tetromino
                //game.currentTetromino = spawnTetromino();
                //Tetromino out = new Tetromino(board_bound_x+board_bound_w/2-TET_WIDTH/2,0,nextTetronimo,0,10*Math.min(level/2,5)+4+(int)(Math.random()*4));
                //nextTetronimo = (int)(Math.random() * tetrominoList.length);
                ResetTetromino(/*game.board_bound_x+game.board_bound_w/2-game.TET_WIDTH/2,0,game.nextTetronimo,10*Math.min(game.level/2,5)+4+(int)(Math.random()*4)*/);
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
        // all other tetromino inputs
        int xp =dx, rp = rotation;
        if(main.input.get(KeyEvent.VK_RIGHT) == 1 || main.input.get(KeyEvent.VK_RIGHT) > main.TPS/8) dx++;
        if(main.input.get(KeyEvent.VK_LEFT) == 1 || main.input.get(KeyEvent.VK_LEFT) > main.TPS/8) dx--;
        if(main.input.get(KeyEvent.VK_UP) == 1 || main.input.get(KeyEvent.VK_UP) > main.TPS/8) rotation = (rotation+1)%4;
        if(main.input.get(KeyEvent.VK_A) == 1 && game.board_bound_x > 0){
            game.board_bound_x -= 1;
            dx -= 1;
        }
        if(main.input.get(KeyEvent.VK_D) == 1 && game.board_bound_x < game.boardWidth-game.board_bound_w){
            game.board_bound_x += 1;
            dx += 1;
        }
        //game.board_bound_x = (Math.min(game.boardWidth-game.board_bound_w,Math.max(0,dx-game.board_bound_w/2))/game.board_bound_w)*game.board_bound_w;
        draw.view_x -= (draw.view_x-(x-main.FRAMEBUFFER_W/4.))*0.1;
        draw.view_y -= (draw.view_y-(y-main.FRAMEBUFFER_H/4.)*.2)*0.1;
        if(!checkBoardState()){
            dx = xp;
            rotation = rp;
        }

        /*x -= (x-dx*main.SPR_WIDTH)/16; // interpolate tetronimo position
        y -= (y-dy*main.SPR_WIDTH)/16;
        for(int i = 0; i < game.TET_WIDTH; i++){
            for(int j = 0; j < game.TET_WIDTH; j++){
                //int tx = (int)x+i*(main.SPR_WIDTH), ty = (int)y+j*(main.SPR_WIDTH); // normalized coordinates
                if(tetrominoList[index][j][i][j] > 0){
                    draw.batchPush(t,game.boardx+x,game.boardy+y,main.SPR_WIDTH,main.SPR_WIDTH); // draw tetromino
                    if(dx+i >= 0 && dx+i < game.boardWidth && dy+j >= 2 && dy+j < game.boardHeight && game.board[dx+i][dy+j] == 0) game.light[dx+i][dy+j] = 10; // illuminate current spot
                }
                if(tetrominoList[game.nextTetronimo][0][index][j] > 0) draw.batchPush(8,20+(i+1)*main.SPR_WIDTH,game.boardy+(j+4)*main.SPR_WIDTH,main.SPR_WIDTH, main.SPR_WIDTH); // show next tetromino in hud
            }
        }*/
    }

    public void ResetTetromino(/*int x, int y, int index, int sprite*/){
        //game.board_bound_x+game.board_bound_w/2-game.TET_WIDTH/2,0,game.nextTetronimo,10*Math.min(game.level/2,5)+4+(int)(Math.random()*4)
        dx = game.board_bound_x+game.board_bound_w/2-game.TET_WIDTH/2;
        dy = 0;
        x = dx*main.SPR_WIDTH;
        y = dy*main.SPR_WIDTH;
        this.index = game.nextTetronimo;
        this.sprite = 10*Math.min(game.level/2,5)+4+(int)(Math.random()*4);
        game.nextTetronimo = (int)(Math.random() * tetrominoList.length);
    }
}

