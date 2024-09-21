import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

class Game extends scene { // main gameplay scene, i put it in its own class file because its huge. i couldve separated it into other classes but i dont really care
    public final int TET_WIDTH = 4;
    public int boardWidth,boardHeight,posx,posy,boardx,boardy,time,score,state,oldstate,clearx,cleary,clearflash,level,lines,nextTetronimo,lives; // yuck
    public double cleardy,cleardx,illum;
    public final Color[] flash = {new Color(255,255,255), new Color(255,0,68), new Color(99,199,77), new Color(44,232,245), new Color(254,231,97)}; // merge particle colours
    public final int scores[] = {40,100,300,1200}; // tetris scores
    public int[][] board;
    public int board_bound_x, board_bound_w;
    public double[][] light;
    //public int[][][][] tetrominoList;
    public final BufferedImage levelimage = main.loadTexture("resources/load/levelatlas.png");
    public ObjectTetromino currentTetromino;
    //public final String[] taunts = {"YOU SUCK","???","GONNA CRY?","LOL","AINT MEAN IF U AINT GREEN","GOBLINZ RULE","BUYING GOBLIN GF","SO GOBLINCORE","GOBLINMAXXING RN",
    //        "WOW...","ZZZ","STOP TRYING","IM IN JAVA?","GOBLINPILLED","GOBLIN4LIFE","...","THEY NOT LIKE US","I LOVE GRIMES"};
    //public final Color[] tauntcolours = {new Color(104,46,108), new Color(38,92,66), new Color(25,60,62), new Color(58,68,102), new Color(38,43,68), new Color(62,39,49)};
    // took me way too long to write these consts, i was just remembering the integers before lol (notice how i missed 5 ????)
    public final int STATE_PLAY = 0, STATE_PAUSE = 1, STATE_GAMEOVER = 2, STATE_CLEAR = 3, STATE_LOSE = 4, STATE_oops = 5, STATE_STARTLEVEL = 6, STATE_ENDLEVEL = 7;

    private final BufferedImage water = new BufferedImage(main.FRAMEBUFFER_W,main.FRAMEBUFFER_H,BufferedImage.TYPE_INT_ARGB);
    private final Graphics2D water2d = water.createGraphics();

    /*public final ArrayList<Object> objects = new ArrayList<Object>();

    public Object CreateObject(Object object){
        objects.add(object);
        return object;
    }*/

    /*private class Tetromino { // tetromino class
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
    private Tetromino currentTetromino;*/

    /*private class Enemy { // goblins
        double x,y,hsp,vsp;
        int spr,xd;
        String taunt,txt;
        Color c;
        public Enemy(int _spr, double _x, double _y){
            x = _x;
            y = _y;
            spr = _spr;
            xd = 1;
            taunt = "";
            txt = "";
            c = tauntcolours[(int)(Math.random()*tauntcolours.length)];
        }
    }
    private final ArrayList<Enemy> enemylist = new ArrayList<Enemy>();*/

    private void loadLevel(int w, int h){ // loads goblin levels from an image, colour data represents what goes where
        BufferedImage in = levelimage;
        for(int x = -w; x < 2*w; x++){
            for(int y = 0; y < h; y++){
                if(x >= 0 && x < in.getWidth()){
                    int[][] b = board;
                    int c = in.getRGB(x,y) & 0xFFFFFF;
                    b[x][y] = 0;
                    if(c == 0x0000FF) b[x][y] = 114; // bricks                    all sprites arent named and i couldnt be bothered
                    else if(c == 0x00FF00) b[x][y] = 160; // torch up             you kinda just gotta figure it out
                    else if(c == 0x00FF80) b[x][y] = 161; // torch left
                    else if(c == 0x80FF00) b[x][y] = 162; // torch right
                    else if(c == 0xFFFF00) b[x][y] = 190; // scaffold vertical
                    else if(c == 0xFFFF80) b[x][y] = 191; // scaffold horizontal
                    else if(c == 0xFF00FF) b[x][y] = 192; // pole
                    else if(c == 0xFF80FF) b[x][y] = 163; // flag
                    if(c == 0xFF0000 && b == board) GameObject.CreateObject(new ObjectCharacter(this,PlayerControlScheme.PCS_AI,137+10*(int)(Math.random()*3),posx+x*main.SPR_WIDTH,posy+y*main.SPR_WIDTH));//spawnEnemy(posx+x*main.SPR_WIDTH,posy+y*main.SPR_WIDTH); // goblin only spawn in board area
                    //else System.out.println("funny colour dattebayo.. " + c); // precision error logging
                }
            }
        }
    }

    private int getLightLevel(int x,int y,double scale){
        // starting light distance is distance to current tetromino, as it makes the lighting smoother as it drops between cells
        double minDistance = Math.sqrt(Math.pow((currentTetromino.x/main.SPR_WIDTH)+1 - x, 2) + Math.pow((currentTetromino.y/main.SPR_WIDTH)+1 - y, 2));
        for (int i = board_bound_x; i < board_bound_x+board_bound_w; i++) {
            for (int j = 0; j < boardHeight; j++) {
                if (board[i][j] != 0 && (board[i][j] < 100 || (board[i][j] >= 160 && board[i][j] <= 162))) { // brick and scaffold tiles specifically are ignored kinda hacky until i add more decorations and fix it
                    double distance = Math.sqrt(Math.pow(i - x, 2) + Math.pow(j - y, 2)); // euclidean distance to target cell
                    minDistance = Math.min(minDistance, distance); // self explanatory
                }
            }
        }
        return (int)(10-Math.min(Math.max(minDistance*scale, 1),10)); // 10 light levels
    }

    private int getTileIndex(int i, int x, int y, int[][] tboard){ // autotiling helper function, just for bricks. its actually really slow !
        int l=0,r=0,t=0,b=0;
        if(x > 0 && tboard[x-1][y] == i) l = 1;
        if(x < tboard.length-1 && tboard[x+1][y] == i) r = 1;
        if(y > 0 && tboard[x][y-1] == i) t = 1;
        if(y < tboard[0].length-1 && tboard[x][y+1] == i) b = 1;
        int a = l+r+t+b;

        if(a == 4) return i;
        else if(a == 3){
            if(l == 0) return i-1;
            if(r == 0) return i+1;
            if(t == 0) return i-10;
            if(b == 0) return i+10;
        }else if(a == 2){
            if(t == 0 && l == 0) return i-11;
            if(t == 0 && r == 0) return i-9;
            if(b == 0 && l == 0) return i+9;
            if(b == 0 && r == 0) return i+11;
            if(t == 0 && b == 0) return i+2;
            if(l == 0 && r == 0) return i-8;
        }else if(a == 1){
            if(b == 1) return i+12;
            if(l == 1) return i-7;
            if(t == 1) return i+3;
            if(r == 1) return i+13;
        }
        return i-6;
    }

    public int pointCheck(double x, double y){ // collision function for enemies, check if out of bounds -> check if inside an occupied cell
        int bx = (int)Math.floor((x-posx)/main.SPR_WIDTH), by = (int)((y-posy)/main.SPR_WIDTH);
        if(bx < 0 || bx >= boardWidth || by < 0 || by >= boardHeight || (board[bx][by] > 0 && board[bx][by] < 160)) return 1;
        return 0;
    }

    // get tetromino data from atlas, can add funny shaped ones if i want (??????)
    /*private int[][][][] getTetrominoes(BufferedImage in){
        int count = in.getHeight()/4;
        int[][][][] out = new int[count][4][TET_WIDTH][TET_WIDTH];
        for(int i = 0; i < count; i++){
            for(int j = 0; j < 4; j++){
                for(int x = 0; x < TET_WIDTH; x++){
                    for(int y = 0; y < TET_WIDTH; y++){
                        // i=tetromino index, j=rotation index, x=x, y=y
                        // it was easier to handle rotations this way, as if im gonna do a rotation matrix just for this that would be silly
                        out[i][j][x][y] = Math.min(in.getRGB(j*TET_WIDTH+x,i*TET_WIDTH+y) & 0xff,1); // set cells of tetromino grid by colour data
                    }
                }
            }
        }
        return out;
    }*/

    /*private boolean checkBoardState(){ // tetromino collision function
        Tetromino t = currentTetromino;
        boolean collision = true;
        for(int i = 0; i < TET_WIDTH; i++){
            for(int j = 0; j < TET_WIDTH; j++){
                if(tetrominoList[t.i][t.j][i][j] > 0){
                    int x = t.x+i, y = t.y+1+j; // normalized x and y for tetromino position
                    if(y < 0 || y >= boardHeight || x < board_bound_x || x >= board_bound_x+board_bound_w || (board[x][y] > 0 && board[x][y] < 160)){ // hack to ignore decorative tiles
                        collision = false; // this doesnt make sense because if this happens then there is a collision, but 'nocollision' as a variable name seems silly and i didnt want to reverse usages of the function
                        // create particles even for potential collision because it gives player feedback
                        if((int)(Math.random()*2) == 0) draw.particlePush(29,34,0.05+0.05*Math.random(),boardx+x*main.SPR_WIDTH,boardy+(y-1)*main.SPR_WIDTH,-0.1+0.2*Math.random(),-0.1+0.3*Math.random(),flash[(int)(Math.random()*5)]); // yuck
                    }
                }
            }
        }
        return collision;
    }*/

    // 2 functions that pretty much do the same thing
    // kinda hacky sorry

    public int clearRows(){
        int rows = 0;
        for(int i = boardHeight-1; i > 0; i--){
            int clear = 1;
            for(int j = board_bound_x; j < board_bound_x+board_bound_w; j++){
                if(board[j][i] == 0 || board[j][i] >= 160) clear = 0; // decorative tiles are all indexed above 160
            }
            if(clear == 1){
                for(int y = i; y > 1; y--){ // shift rows above row to be cleared
                    for(int x = board_bound_x; x < board_bound_x+board_bound_w; x++) board[x][y] = board[x][y-1];
                }
                rows++;
                break;
            }
        }
        return rows;
    }

    public int checkRows(){
        int rows = 0;
        for(int i = boardHeight-1; i > 0; i--){
            int clear = 1;
            for(int j = board_bound_x; j < board_bound_x+board_bound_w; j++){
                if(board[j][i] == 0 || board[j][i] >= 160) clear = 0; // decorative tiles are all indexed above 160
            }
            if(clear == 1){
                if(rows == 0){ // reset row clear animation for first found row, i would break here but this function also counts how many rows are cleared at once for scoring
                    cleary = i;
                    cleardy = 0;
                    clearflash = (int)(Math.random()*5);
                }
                rows++; // count the number of rows for scoring purposes
                for(int k = 0; k < 10; k++) draw.particlePush(29,34,0.05+0.05*Math.random(),boardx+(int)(board_bound_x+(board_bound_w/2.)*main.SPR_WIDTH*Math.random()),boardy+cleary*main.SPR_WIDTH,-0.1+0.2*Math.random(),-0.1+0.2*Math.random(),flash[(int)(Math.random()*5)]); // yuck
                // particle effect ^^
            }
        }
        return rows;
    }

    /*private Tetromino spawnTetromino(){ // self explanatory
        Tetromino out = new Tetromino(board_bound_x+board_bound_w/2-TET_WIDTH/2,0,nextTetronimo,0,10*Math.min(level/2,5)+4+(int)(Math.random()*4));
        nextTetronimo = (int)(Math.random() * tetrominoList.length);
        return out;
    }*/

    /*private Enemy spawnEnemy(double x, double y){ // self explanatory
        Enemy out = new Enemy(137+10*(int)(Math.random()*3),x,y);
        enemylist.add(out);
        return out;
    }*/

    public Game(Tetris2805 m, D2D d) { // this is really gross
        super(m, d);
        draw.clearColour = new Color(24,20,37);
        main.sceneIndex = 4;

        //NetworkHandler.game = this;

        posx = 80; // board anchor position
        posy = 6;
        boardx = posx;
        boardy = posy;

        time = 0;
        score = 0;
        state = 0;
        oldstate = state;
        lines = 0;
        illum = 1;
        clearx = 0;
        cleary = 0;
        cleardy = 0;
        cleardx = 0;
        //ObjectTetromino.tetrominoList = getTetrominoes(main.loadTexture("resources/load/tetrominoatlas5.png"));
        //boardWidth = main.cfg.get("width");
        //boardHeight = main.cfg.get("height")+2;
        switch(main.gamemode){
            case GM.GM_OFFLINE:
                boardWidth = levelimage.getWidth();
                boardHeight = main.cfg.get("height")+2;
                board_bound_x = 0;
                board_bound_w = main.cfg.get("width");
                nextTetronimo = (int)(Math.random() * ObjectTetromino.tetrominoList.length); // random tetromino same as in spawnTetromino
                currentTetromino = (ObjectTetromino) GameObject.CreateObject(new ObjectTetromino(this,PlayerControlScheme.PCS_AI,0,0,0,0,0));
                currentTetromino.ResetTetromino();
                level = main.cfg.get("level"); // starting level
                lives = 1+2*main.cfg.get("extend"); // only goblin mode has 3 lives
                board = new int[boardWidth][boardHeight]; // board init
                light = new double[boardWidth][boardHeight]; // light init
                for(int i = 0; i < boardWidth; i++){
                    for(int j = 0; j < boardHeight; j++){
                        board[i][j] = 0;
                        light[i][j] = 0;
                    }
                }
                if(main.cfg.get("extend") == 1) loadLevel(boardWidth,boardHeight); // first goblin level
                GameObject.CreateObject(new ObjectCharacter(this,PlayerControlScheme.PCS_LOCAL,137,boardx+40,boardy+10));
                for(int i = 0; i < 40; i++){
                    GameObject.CreateObject(new ObjectResource(this,(int)(boardx+boardWidth*main.SPR_WIDTH*Math.random()),boardy+boardHeight*main.SPR_WIDTH,Math.random() > 0.5 ? 109 : 119,1,10));
                }
            break;
            case GM.GM_HOST:
                boardWidth = 50;
                boardHeight = main.cfg.get("height")+2;
                board_bound_x = 0;
                board_bound_w = main.cfg.get("width");
                nextTetronimo = (int)(Math.random() * ObjectTetromino.tetrominoList.length); // random tetromino same as in spawnTetromino
                currentTetromino = (ObjectTetromino) GameObject.CreateObject(new ObjectTetromino(this,PlayerControlScheme.PCS_LOCAL,0,0,0,0,0));
                currentTetromino.ResetTetromino();
                level = main.cfg.get("level"); // starting level
                lives = 3; // multiplayer is goblin mode only
                board = new int[boardWidth][boardHeight]; // board init
                light = new double[boardWidth][boardHeight]; // light init
                for(int i = 0; i < boardWidth; i++){
                    for(int j = 0; j < boardHeight; j++){
                        board[i][j] = 0;
                        light[i][j] = 0;
                    }
                }
                for(int i = 0; i < 40; i++){
                    GameObject.CreateObject(new ObjectResource(this,(int)(boardx+boardWidth*main.SPR_WIDTH*Math.random()),boardy+boardHeight*main.SPR_WIDTH,Math.random() > 0.5 ? 109 : 119,1,10));
                }
            break;
            case GM.GM_JOIN:
                Object o = GameObject.CreateObject(new ObjectCharacter(this,PlayerControlScheme.PCS_LOCAL,137,boardx+40,boardy+10));
            break;
        }

        //Object o = Object.CreateObject(new ObjectCharacter(this,PlayerControlScheme.PCS_LOCAL,137,boardx+40,boardy+10));
    }

    @Override
    public void loop(){
        // animations and interpolations for various states
        double interpolatespeed = 16-12*Math.min(1.,main.input.get(KeyEvent.VK_DOWN));
        boardx -= Math.ceil(boardx-posx)*0.2;
        boardy -= Math.ceil(boardy-posy)*0.2;
        illum -= (illum-(2+1*(time/main.TPS)))*0.05;

        int boardvisw = (int)(main.FRAMEBUFFER_W/main.SPR_WIDTH)+1, boardvisx = Math.min(boardWidth-boardvisw,Math.max(0,(int)((draw.view_x-boardx)/main.SPR_WIDTH)));

        double bgx = draw.view_x;
        double bgy = draw.view_y;
        int[] bgtex = {42,61,60,52,51,50};
        for(int i = 0; i < bgtex.length; i++){
            for(int j = 0; j <= (boardWidth*main.SPR_WIDTH)/main.FRAMEBUFFER_W; j += 1){
                draw.batchPush(bgtex[i],(int)((bgx*(1.-0.1*i))%main.FRAMEBUFFER_W+main.FRAMEBUFFER_W*j),(int)((bgy*(1-0.1*i))%main.FRAMEBUFFER_H),main.FRAMEBUFFER_W,main.FRAMEBUFFER_H);
            }
            //draw.batchPush(bgtex[i],(int)((bgx*(0.1+0.1*i))%FRAMEBUFFER_W-FRAMEBUFFER_W),(int)((bgy*(0.1+0.1*i))%FRAMEBUFFER_H),FRAMEBUFFER_W,FRAMEBUFFER_H);
        }

        //if(main.input.get(-1) == 1) Object.CreateObject(new ObjectCharacter(this,PlayerControlScheme.PCS_AI,137,(int)main.mousex,(int)main.mousey));

        time++;
        if(state != STATE_GAMEOVER){
            if(main.input.get(KeyEvent.VK_ESCAPE) == 1 || main.input.get(KeyEvent.VK_P) == 1) state = 1-state; // pause input
            main.bgtx = score+800*level+main.frame*0.1;

            //play area borders
            draw.batchPush(9,boardx+board_bound_x*main.SPR_WIDTH,boardy+2*main.SPR_WIDTH,1,(boardHeight-2)*main.SPR_WIDTH+1,new Color(139,155,180));
            draw.batchPush(9,boardx+(board_bound_x+board_bound_w)*main.SPR_WIDTH,boardy+2*main.SPR_WIDTH,1,(boardHeight-2)*main.SPR_WIDTH+1,new Color(139,155,180));

            // draw board
            for(int i = boardvisx; i < boardvisx+boardvisw; i++){
                for(int j = 2; j < boardHeight; j++){
                    // draw.batchPush((i+j)%4,boardx+i*main.SPR_WIDTH,boardy+j*main.SPR_WIDTH, main.SPR_WIDTH,main.SPR_WIDTH);
                    double tl = getLightLevel(i,j,illum)*4;
                    if(i < boardWidth-1) tl += light[i+1][j];
                    if(i > 0) tl += light[i-1][j];
                    if(j < boardHeight-1) tl += light[i][j+1];
                    if(j > 0) tl += light[i][j-1];
                    tl /= 8;
                    light[i][j] -= (light[i][j]-tl)*(0.05/illum); // this didnt need to look so hacky but i wanted the lights to be smoother and take the average of adjacent cells
                    int li = (int)light[i][j]; // VVV that hack is for the sprite indexes for different light levels since theyre in funny parts of the sprite sheet
                    //draw.batchPush((li > 0) ? (18+10*(li%5)+li/5) : (i+j)%4,boardx+i*main.SPR_WIDTH,boardy+j*main.SPR_WIDTH, main.SPR_WIDTH,main.SPR_WIDTH);
                    //if(li > 5) draw.batchPush(59,boardx+i*main.SPR_WIDTH+main.SPR_WIDTH/2-32,boardy+j*main.SPR_WIDTH+main.SPR_WIDTH/2-32, 64,64);
                    if(board[i][j] < 0) board[i][j] = 0; // moving tetromino sets board cells to negative values, reset this
                }
            }

            int lo = 0;
            if(state == STATE_STARTLEVEL){ // level clear animation
                lo = (int)cleardx;
                cleardx -= ((boardWidth+1)*main.SPR_WIDTH-cleardx)*0.01; // cleardx is the horizontal scrolling of the board between levels
                boardx += (int)(cleardx*0.01)-(int)(Math.random()*(cleardx*0.02));
                boardy += (int)(cleardx*0.01)-(int)(Math.random()*(cleardx*0.02));
                illum = 10;
                if(cleardx <= 0.1){
                    state = STATE_PLAY;
                    cleardx = 0;
                }
            }

            //filled board space
            for(int i = boardvisx; i < boardvisx+boardvisw; i++){
                for(int j = 2; j < boardHeight; j++){
                    if(board[i][j] > 0 && j != cleary){
                        int k = board[i][j];
                        if(k == 114) k = getTileIndex(k,i,j,board); // the brick tile is the only tile with autotiling
                        else if(k >= 160 && k < 170) k += ((int)(main.frame/(main.TPS/6))%3)*10; // torch animation

                        double disttocenter = Math.abs(boardx+i*main.SPR_WIDTH - (draw.view_x+draw.view_w/2))/(draw.view_w*0.5);

                        double l = Math.min(1,Math.max(0,Math.min(1-(4+light[i][j])/10f,disttocenter)));
                        draw.batchPush(k,boardx+i*main.SPR_WIDTH + lo,boardy+j*main.SPR_WIDTH + (j < cleary ? (int)cleardy : 0), main.SPR_WIDTH,main.SPR_WIDTH,
                                new Color((int)draw.lerp(255,24,l),(int)draw.lerp(255,20,l),(int)draw.lerp(255,37,l))); // ternary operator to only draw rows above cleardy in animated state for row clear, also lerp for light level
                    }
                }
            }

            if(GameObject.objects.size() > 0){
                for(int i = 0; i < GameObject.objects.size(); i++){
                    GameObject obj = GameObject.objects.get(i);
                    if(obj != null && obj.destroy == 0){
                        obj.update();
                    }else{
                        GameObject.objects.remove(i);
                        i--;
                    }
                }
            }

            //update enemies in goblin mode
            if(main.cfg.get("extend") == 1){
                int killscore = 0;
            }

            // draw ground
            for(int i = boardvisx; i <= boardvisx+boardvisw; i += 1){
                double disttocenter = Math.abs(boardx+i*main.SPR_WIDTH - (draw.view_x+draw.view_w/2))/(draw.view_w*0.5);
                //double l = Math.min(1,Math.max(0,1-(4+light[i][boardHeight-1])/10f + disttocenter));
                double l = Math.min(1,Math.max(0,Math.min(1-(4+light[i][boardHeight-1])/10f,disttocenter)));
                //double l = Math.min(1,2*Math.abs(i/((double)main.SPR_WIDTH)-boardWidth*1.5f)/(boardWidth*3f));
                Color c = new Color((int)draw.lerp(255,24,l),(int)draw.lerp(255,20,l),(int)draw.lerp(255,37,l));
                draw.batchPush(100+Math.abs(i)%3,boardx+i*main.SPR_WIDTH+(int)cleardx%main.SPR_WIDTH,(i/main.SPR_WIDTH*i)%3-1+boardy+boardHeight*main.SPR_WIDTH, main.SPR_WIDTH, main.SPR_WIDTH,c);
                draw.batchPush(110+Math.abs(i)%3,boardx+i*main.SPR_WIDTH+(int)cleardx%main.SPR_WIDTH,(i/main.SPR_WIDTH*i)%3-1+boardy+(boardHeight+1)*main.SPR_WIDTH, main.SPR_WIDTH, main.SPR_WIDTH,c);
                draw.batchPush(120+Math.abs(i)%3,boardx+i*main.SPR_WIDTH+(int)cleardx%main.SPR_WIDTH,(i/main.SPR_WIDTH*i)%3-1+boardy+(boardHeight+2)*main.SPR_WIDTH, main.SPR_WIDTH, main.SPR_WIDTH,c);
            }

            int waterY = (int)(boardy+(boardHeight+3)*main.SPR_WIDTH-draw.view_y);
            if(time%2==0){
                int waterHeight = main.FRAMEBUFFER_H-waterY;

                water2d.setComposite(AlphaComposite.Clear);
                water2d.fillRect(0, 0, water.getWidth(), water.getHeight());
                water2d.setComposite(AlphaComposite.SrcOver);
                water2d.setClip(0, waterY, water.getWidth(), waterHeight);

                AffineTransform flip = new AffineTransform();
                flip.scale(1, -0.5);
                flip.translate(0, -3*waterY);

                water2d.drawImage(draw.framebuffer, flip, null);
            }

            draw.batchPush(water,draw.view_x,draw.view_y,draw.view_w,draw.view_h);

            for(int i = 1; i <= 12; i += 2){
                int s = 118, h = 1;
                for(int j = 0; j <= (boardWidth*main.SPR_WIDTH)/main.FRAMEBUFFER_W; j += 1){
                    draw.batchPush(s,(int)(((boardWidth*main.SPR_WIDTH*0.5-bgx)*(1.+0.02*i)+(Math.sin(i*234)*0.06)*main.frame+25*i)%main.FRAMEBUFFER_W+main.FRAMEBUFFER_W*j),(int)(waterY+(draw.view_y)*(1+0.01*i))+2*i,main.FRAMEBUFFER_W,h);
                }
            }

            int[] fgtex = {129,128,135,136};
            for(int i = 0; i < fgtex.length; i++){
                for(int j = 0; j <= (boardWidth*main.SPR_WIDTH)/main.FRAMEBUFFER_W; j += 1){
                    //System.out.println(""+i+": "+D2D.sprites[fgtex[i]]);
                    draw.batchPush(fgtex[i],(int)((-bgx*(0.4+0.5*i))%main.FRAMEBUFFER_W+main.FRAMEBUFFER_W*j-main.FRAMEBUFFER_W),(int)(waterY+(draw.view_y)*(1.1+0.02*i))-10*i,main.FRAMEBUFFER_W,128/*,new Color(24,20,37)*/);
                }
            }

            if(state == STATE_CLEAR){
                illum *= 0.9; // illuminate the board
                cleardy += main.SPR_WIDTH/(16*(1+cleardy)); // cleardy animation for clearing a row
                boardx += 2-(int)(Math.random()*4);
                boardy += 2-(int)(Math.random()*4); // shake the board
                double i = cleardy/main.SPR_WIDTH;
                //int j = (int)(Math.random()*5);
                // coloured rectangle that squishes as the board clears a row
                draw.batchPush(9,boardx,boardy+cleary*main.SPR_WIDTH+(int)cleardy,boardWidth*main.SPR_WIDTH,Math.max(1,1+main.SPR_WIDTH-(int)cleardy),
                        new Color((int)draw.lerp(flash[clearflash].getRed(),24,i),(int)draw.lerp(flash[clearflash].getBlue(),20,i),(int)draw.lerp(flash[clearflash].getGreen(),37,i)));
                if(cleardy > main.SPR_WIDTH){
                    // anim reset, check if more rows need to be cleared
                    for(int k = 0; k < 10; k++) draw.particlePush(30,33,0.05+0.05*Math.random(),boardx+(int)(boardWidth*main.SPR_WIDTH*Math.random()),boardy+cleary*main.SPR_WIDTH,0,0,Color.WHITE);
                    cleardy = 0;
                    cleary = 0;
                    clearRows();
                    lines++;
                    if(lines > 10*(level+1) && main.cfg.get("extend") == 0) level++; // next level if cleared 10+ rows on current level
                    if(checkRows() == 0) state = STATE_PLAY;
                }
            }else cleary = 0; // reset animation

            if(state == STATE_LOSE || state == STATE_ENDLEVEL){ // lose / level transition board clear animation
                while(board[clearx%boardWidth][clearx/boardWidth] == 0 && clearx < boardWidth*boardHeight-1) clearx++; // skip to next player-placed tile, speeds up the animation
                int x = clearx%boardWidth, y = clearx/boardWidth; // x y coords from animation state clearx
                if(board[x][y] > 0 && board[x][y] < 100){ // only clear tetromino sprites
                    board[x][y] = 0;
                    //draw.particlePush(29,34,0.05+0.05*Math.random(),boardx+(int)(boardWidth*main.SPR_WIDTH*Math.random()),boardy+cleary*main.SPR_WIDTH,-0.1+0.2*Math.random(),-0.2+0.4*Math.random(),flash[(int)(Math.random()*5)]);
                    /*for(int k = 0; k < 3; k++){
                        draw.particlePush(29,34,0.03+0.02*Math.random(),
                        boardx+(int)(x*main.SPR_WIDTH+main.SPR_WIDTH*Math.random()),
                        boardy+y*main.SPR_WIDTH+(int)(main.SPR_WIDTH*Math.random()),
                        -0.2+0.4*Math.random()+(boardx+(int)(x*main.SPR_WIDTH+main.SPR_WIDTH*Math.random())-(12+main.SPR_WIDTH*lives))*-0.01,
                        -0.2+0.4*Math.random()+(boardy+y*main.SPR_WIDTH+(int)(main.SPR_WIDTH*Math.random())-30)*-0.01,
                        flash[(int)(Math.random()*5)]);
                    }*/
                    for(int k = 0; k < 3; k++) draw.particlePush(29,34,0.05+0.05*Math.random(),boardx+(int)(x*main.SPR_WIDTH+main.SPR_WIDTH*Math.random()),boardy+y*main.SPR_WIDTH+(int)(main.SPR_WIDTH*Math.random()),-0.3+0.6*Math.random(),0,flash[(int)(Math.random()*5)]);
                    draw.particlePush(7,10,0.05+0.05*Math.random(),boardx+(x*main.SPR_WIDTH),boardy+y*main.SPR_WIDTH,0,0,Color.WHITE);
                    illum = Math.random();
                    boardx += 2-(int)(Math.random()*4);
                    boardy += 2-(int)(Math.random()*4); // lighting, board shake, particle effects
                }//else draw.particlePush(30,31,0.01+0.01*Math.random(),boardx+(x*main.SPR_WIDTH),boardy+y*main.SPR_WIDTH,0,0,Color.WHITE);

                if(clearx >= boardWidth*boardHeight-1){
                    if(state == STATE_ENDLEVEL){ // if animation is for end of level, load next level and start level clear animation
                        level++;
                        currentTetromino.ResetTetromino();// = spawnTetromino();
                        loadLevel(boardWidth,boardHeight);
                        state = 6;
                        cleardx = boardWidth*main.SPR_WIDTH;
                        clearx = 0;
                    }else if(state == STATE_LOSE && lives <= 0) state = 2; // otherwise kill the player
                    else state = STATE_PLAY;
                }else clearx++;
            }else if(clearx > 0) clearx--; // gradually reset animation

            if(state == STATE_PAUSE){ // 'pause menu'
                draw.drawText("PAUSED",main.FRAMEBUFFER_W/2,main.FRAMEBUFFER_H/2,10,8,null,1);
                draw.drawText("ESC TO RESUME",main.FRAMEBUFFER_W/2,main.FRAMEBUFFER_H/2+10,8,6,Color.GRAY,1);
            }

            if(main.cfg.get("extend") == 1){ // goblin-specific ui
                double j = Math.min(1,(clearx*Math.log(1+clearx))/((double)boardWidth*boardHeight)); // animation curve
                if(state == STATE_ENDLEVEL) j = 0; // hack to not move the hearts during the level clear animation
                //animation to move 'lives' display to center of screen anytime the player loses a life
                int dx = (int)draw.lerp(12,boardx+(boardWidth/2f)*main.SPR_WIDTH-12,j)+(int)(Math.random()*j*4-2*j),
                        dy = (int)draw.lerp(30,main.FRAMEBUFFER_H/2f,j)+(int)(Math.random()*j*4-2*j);
                for(int i = 0; i < 3; i++){ // 'healthbar'
                    draw.batchPush(140,dx+i*(main.SPR_WIDTH+1),dy,main.SPR_WIDTH,main.SPR_WIDTH);
                    if(lives > i) draw.batchPush(141,dx+i*(main.SPR_WIDTH+1),dy,main.SPR_WIDTH,main.SPR_WIDTH);
                }
                if(j == 1){ // scary little message when you lose a life
                    String txt;
                    if(lives == 2) txt = "TRY AGAIN";
                    else if(lives == 1) txt = "CAREFUL NOW";
                    else txt = "GOODBYE";
                    int k = (int)(Math.random()*(txt.length()-1));
                    txt = txt.replace(txt.substring(k,k+1),""+('A'+(int)(Math.random()*('Z'-'A'))) ); // pretty unnecessary but this obfuscates the text
                    int w = (int)((txt.length()*10)/2f);
                    draw.batchPush(9,dx+12-w,dy+10,2*w,10,new Color(24,20,37));
                    draw.drawText(txt,dx+12-w,dy+10,10,10,null);
                }
                // level clear message
                if(state == STATE_STARTLEVEL) draw.drawText("LEVEL CLEARED".substring(0,(int)(13*Math.min(1,((boardWidth*main.SPR_WIDTH-cleardx)*3)/(boardWidth*main.SPR_WIDTH)))),main.FRAMEBUFFER_W/2-65,main.FRAMEBUFFER_H/2,10,10,null);
            }
        }else{ // gameover screen
            main.bgtx = 0;
            String name = draw.drawTextfield("ENTER NAME",10,60,80,10);
            if(name != ""){
                main.scores.put(name.replace(" ",""),score);
                main.saveData(main.scores,"src/hscore.txt");
            }
            if(draw.drawButton("MAIN MENU",10,40,80,10) == 1 || name != "") main.currentScene = new menu(main,draw);
            if(draw.drawButton("QUIT",10,51,80,10) == 1) main.displayconfirm = 1;
            draw.drawText("GAME OVER",10,30,10,10,Color.RED);
        }
        // context independent ui for level and score
        draw.drawText("LEVEL "+level,10,10,10,8,Color.WHITE);
        draw.drawText(""+score,10,20,8,6,flash[(score/100)%5]);

        if(state == STATE_oops){
            if(main.displayconfirm != main.DIALOG_CONTEXT_MENU) state = oldstate;
        }else if(draw.drawButton("BACK",20,main.FRAMEBUFFER_H-20,80,10) == 1){
            if(state != STATE_GAMEOVER){
                oldstate = state;
                state = STATE_oops;
                main.displayconfirm = main.DIALOG_CONTEXT_MENU;
            }else main.currentScene = new menu(main,draw);
        }
    }
}