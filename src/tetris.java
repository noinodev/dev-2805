import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

class tetris extends scene {
    public final int TET_WIDTH = 4;
    private int boardWidth,boardHeight,posx,posy,boardx,boardy,time,score,state,clearx,cleary,clearflash,level,lines,nextTetronimo,lives;
    private double cleardy,cleardx,illum;
    private final Color[] flash = {new Color(255,255,255), new Color(255,0,68), new Color(99,199,77), new Color(44,232,245), new Color(254,231,97)};
    private final int scores[] = {40,100,300,1200}; // tetris scores
    private int[][] board;
    private double[][] light;
    private int[][][][] tetrominoList;
    private final String[] taunts =
    {"YOU SUCK","???","GONNA CRY?","LOL","LOSER","GOBLINS RULE","BUYING GOBLIN GF","IN THE STRIPPED CLUB","STRAIGHT JORKIN IT",
    "WOW...","ZZZ","TRY HARDER","IM IN JAVA?","SRS??","GOBLIN4LIFE","..."};
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

    private class enemy {
        double x,y,hsp,vsp;
        int spr,xd;
        String taunt,txt;
        public enemy(int _spr, double _x, double _y){
            x = _x;
            y = _y;
            spr = _spr;
            xd = 1;
            taunt = "";
            txt = "";
        }
    }
    private final ArrayList<enemy> enemylist = new ArrayList<enemy>();

    private void loadLevel(int offset, int w, int h){
        BufferedImage in = main.loadTexture("levelatlas.png");
        for(int x = 0; x < w; x++){
            for(int y = 0; y < h; y++){
                int c = in.getRGB(offset+x,y) & 0xFFFFFF;
                if(c == 0x000000)board[x][y] = 0;
                else if(c == 0x0000FF) board[x][y] = 114;
                else if(c == 0x00FF00) board[x][y] = 160;
                else if(c == 0x00FF80) board[x][y] = 161;
                else if(c == 0x80FF00) board[x][y] = 162;
                else if(c == 0xFF0000) spawnEnemy(posx+x*main.SPR_WIDTH,posy+y*main.SPR_WIDTH);
                else System.out.println("funny colour dattebayo.. " + c);

            }
        }
    }

    private int getLightLevel(int x,int y,double scale){
        double minDistance = Math.sqrt(Math.pow((currentTetromino.dx/main.SPR_WIDTH)+1 - x, 2) + Math.pow((currentTetromino.dy/main.SPR_WIDTH)+1 - y, 2));
        //double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < boardWidth; i++) {
            for (int j = 0; j < boardHeight; j++) {
                if (board[i][j] != 0 && board[i][j] != 114) {
                    double distance = Math.sqrt(Math.pow(i - x, 2) + Math.pow(j - y, 2));
                    minDistance = Math.min(minDistance, distance);
                }
            }
        }

        return (int)(10-Math.min(Math.max(minDistance*scale, 1),10));
    }

    private int getTileIndex(int i, int x, int y){
        int l=0,r=0,t=0,b=0;
        if(x > 0 && board[x-1][y] == i) l = 1;
        if(x < boardWidth-1 && board[x+1][y] == i) r = 1;
        if(y > 0 && board[x][y-1] == i) t = 1;
        if(y < boardHeight-1 && board[x][y+1] == i) b = 1;
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

    private int pointCheck(double x, double y){
        int bx = (int)Math.floor((x-posx)/main.SPR_WIDTH), by = (int)((y-posy)/main.SPR_WIDTH);
        if(bx < 0 || bx >= boardWidth || by < 0 || by >= boardHeight || board[bx][by] > 0) return 1;
        return 0;
    }

    // get tetromino data from atlas, can add funny shaped ones if i want (??????)
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
                    if(y < 0 || y >= boardHeight || x < 0 || x >= boardWidth || (board[x][y] > 0 && board[x][y] < 160)){ // hack to ignore decorative tiles
                        collision = false;
                        if((int)(Math.random()*2) == 0) draw.particlePush(29,34,0.05+0.05*Math.random(),boardx+x*main.SPR_WIDTH,boardy+(y-1)*main.SPR_WIDTH,-0.1+0.2*Math.random(),-0.1+0.3*Math.random(),flash[(int)(Math.random()*5)]); // yuck
                    }
                }
            }
        }
        return collision;
    }

    // 2 functions that pretty much do the same thing
    // kinda hacky sorry

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
                for(int k = 0; k < 10; k++) draw.particlePush(29,34,0.05+0.05*Math.random(),boardx+(int)(boardWidth*main.SPR_WIDTH*Math.random()),boardy+cleary*main.SPR_WIDTH,-0.1+0.2*Math.random(),-0.1+0.2*Math.random(),flash[(int)(Math.random()*5)]); // yuck
                //break;
            }
        }
        return rows;
    }

    private tetromino spawnTetromino(){
        tetromino out = new tetromino(boardWidth/2-TET_WIDTH/2,0,nextTetronimo,0,10*Math.min(level/3,5)+4+(int)(Math.random()*4));
        nextTetronimo = (int)(Math.random() * tetrominoList.length);
        return out;
    }

    private enemy spawnEnemy(double x, double y){
        enemy out = new enemy(135+10*(int)(Math.random()*3),x,y);
        enemylist.add(out);
        return out;
    }

    public tetris(Tetris2805 m, draw2d d) {
        super(m, d);
        draw.clearColour = new Color(24,20,37);
        main.sceneIndex = 4;
        tetrominoList = getTetrominoes(main.loadTexture("tetrominoatlas.png"));
        boardWidth = main.cfg.get("width");
        boardHeight = main.cfg.get("height")+2;
        posx = 80;
        posy = 6;
        boardx = posx;
        boardy = posy;
        currentTetromino = spawnTetromino();
        nextTetronimo = (int)(Math.random() * tetrominoList.length);
        time = 0;
        score = 0;
        state = 0; // state 0 run, state 1 pause, state 2 gameover, state 3 is clearing
        level = main.cfg.get("level");
        lines = 0;
        illum = 1;
        lives = 1+2*main.cfg.get("extend");
        clearx = 0;
        cleary = 0;
        cleardy = 0;
        cleardx = 0;

        board = new int[boardWidth][boardHeight];
        light = new double[boardWidth][boardHeight];
        for(int i = 0; i < boardWidth; i++){
            for(int j = 0; j < boardHeight; j++){
                board[i][j] = 0;
                light[i][j] = 0;
            }
        }
        loadLevel(0,boardWidth,boardHeight);
    }

    @Override
    public void loop(){
        //draw.batchPush(6,10,10,10,10);
        double interpolatespeed = 16-12*main.input.get(KeyEvent.VK_DOWN);
        boardx -= Math.ceil(boardx-posx)*0.2;
        boardy -= Math.ceil(boardy-posy)*0.2;
        illum -= (illum-(2+1*(time/main.TPS)/*+Math.sin(9*(main.frame/main.TPS))*0.2*/))*0.05;

        time++;
        if(state != 2){
            if(main.input.get(KeyEvent.VK_ESCAPE) == 1) state = 1-state;
            main.bgtx = score+800*level;
            if(state == 0){
                //main.bgtx = score+800*level;// +main.frame*0.2;
                if((time/4f > Math.max(1,60-6*level) || main.input.get(KeyEvent.VK_DOWN) == 1) && Math.abs(currentTetromino.dx-currentTetromino.x*main.SPR_WIDTH) + Math.abs(currentTetromino.dy-currentTetromino.y*main.SPR_WIDTH) < 10){
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
                        if(!checkBoardState()){
                            lives--;
                            clearx = 0;
                            state = 4;
                        }
                        boardx += 2-(int)(Math.random()*4);
                        boardy += 2-(int)(Math.random()*4);
                        illum = 0.5;
                    }else{
                        currentTetromino.y++;
                        illum *= 0.8;
                    }
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
                    // draw.batchPush((i+j)%4,boardx+i*main.SPR_WIDTH,boardy+j*main.SPR_WIDTH, main.SPR_WIDTH,main.SPR_WIDTH);
                    double tl = getLightLevel(i,j,illum)*4;
                    if(i < boardWidth-1) tl += light[i+1][j];
                    if(i > 0) tl += light[i-1][j];
                    if(j < boardHeight-1) tl += light[i][j+1];
                    if(j > 0) tl += light[i][j-1];
                    tl /= 8;
                    light[i][j] -= (light[i][j]-tl)*(0.05/illum);
                    int li = (int)light[i][j];
                    draw.batchPush((li > 0) ? (18+10*(li%5)+li/5) : (i+j)%4,boardx+i*main.SPR_WIDTH,boardy+j*main.SPR_WIDTH, main.SPR_WIDTH,main.SPR_WIDTH);
                    if(board[i][j] < 0) board[i][j] = 0;
                }
            }

            int lo = 0;
            if(state == 6){
                lo = (int)cleardx;
                cleardx -= ((boardWidth+1)*main.SPR_WIDTH-cleardx)*0.01;
                boardx += (int)(cleardx*0.1)-(int)(Math.random()*(cleardx*0.2));
                boardy += (int)(cleardx*0.1)-(int)(Math.random()*(cleardx*0.2));
                illum = 10;
                if(cleardx <= 0.1){
                    state = 0;
                    cleardx = 0;
                }
            }

            //filled board space
            for(int i = 0; i < boardWidth; i++){
                for(int j = 2; j < boardHeight; j++){
                    if(board[i][j] > 0 && j != cleary){
                        int k = board[i][j];
                        if(k == 114) k = getTileIndex(k,i,j); // the brick tile is the only tile with an index over 100 so
                        else if(k >= 160) k += ((int)(main.frame/(main.TPS/4))%2)*10;
                        draw.batchPush(k,boardx+i*main.SPR_WIDTH + lo,boardy+j*main.SPR_WIDTH + (j < cleary ? (int)cleardy : 0), main.SPR_WIDTH,main.SPR_WIDTH);
                    }
                }
            }

            //update enemies
            if(main.cfg.get("extend") == 1){
                // if(main.input.get(-1) == 1) spawnEnemy(main.mousex,main.mousey);
                if(enemylist.size() > 0){
                    for(int i = 0; i < enemylist.size(); i++){
                        enemy e = enemylist.get(i);
                        if(state == 0){
                            if(e.hsp != 0) e.xd = e.hsp > 0 ? 1 : -1;
                            if(pointCheck(e.x+2*e.xd+e.hsp,e.y) == 1) e.hsp = 0;
                            if(pointCheck(e.x,e.y+e.vsp) == 1) e.vsp = 0;
                            e.x += e.hsp;
                            e.y += e.vsp;
                            e.vsp += 0.02;
                            if(e.txt.length() == e.taunt.length()){
                                if(e.txt.length() > 0 && (int)(Math.random()*(main.TPS*2)) == 0){
                                    e.txt = "";
                                    e.taunt = "";
                                }
                            }

                            if(e.taunt.length() > e.txt.length() && (int)(Math.random()*5) == 0)e.txt = e.taunt.substring(0,e.txt.length()+1);
                            if(state == 4 || (e.taunt == "" && (int)(Math.random()*(main.TPS*5)) == 0)) e.taunt = taunts[(int)(Math.random()*taunts.length)];

                            if(Math.abs(currentTetromino.dx+boardx+2*main.SPR_WIDTH-e.x) < 2*main.SPR_WIDTH) e.hsp = -(currentTetromino.dx+boardx+2*main.SPR_WIDTH-e.x)*0.01;
                            else if((int)(Math.random()*main.TPS) == 0) e.hsp = -0.25+0.5*Math.random();
                            if((int)(Math.random()*main.TPS) == 0) e.hsp = 0;
                        }

                        if(pointCheck(e.x,e.y) == 1){
                            enemylist.remove(i);
                            for(int j = 0; j < 4; j++) draw.particlePush(130,134,0.03+0.02*Math.random(),(int)e.x,(int)e.y,-0.1+0.2*Math.random(),-0.1+0.2*Math.random(),Color.WHITE);
                            draw.particlePush(150,154,0.09+0.01*Math.random(),(int)e.x-main.SPR_WIDTH/2,(int)e.y-main.SPR_WIDTH,-0.01+0.02*Math.random(),-0.08,Color.WHITE);
                            i--;
                        }

                        draw.batchPush(e.spr+(int)(main.frame/(main.TPS/2))%2+(e.hsp != 0 ? 1 : 0),
                                (int)e.x-e.xd*main.SPR_WIDTH/2 + lo,
                                (int)e.y-main.SPR_WIDTH+(int)Math.abs(Math.sin((main.frame/main.TPS + i*234)*Math.PI)*Math.abs(e.hsp)),
                                e.xd*main.SPR_WIDTH,main.SPR_WIDTH);
                        // taunts
                        if(e.txt != ""){
                            draw.batchPush(9,(int)e.x-10,(int)e.y-18-(int)e.x%2,e.txt.length()*6,8,new Color(24,20,37));
                            draw.drawText(e.txt,(int)e.x-10,(int)e.y-18-(int)e.x%2,8,6,Color.WHITE);
                        }
                    }
                }else if(state == 0){
                    //level++;
                    //loadLevel(level*10,boardWidth,boardHeight);
                    state = 7;
                    clearx = 0;
                    //cleardx = boardWidth*main.SPR_WIDTH;
                }
            }

            //draw tetromino
            tetromino t = currentTetromino;
            t.dx -= (t.dx-t.x*main.SPR_WIDTH)/interpolatespeed;
            t.dy -= (t.dy-t.y*main.SPR_WIDTH)/interpolatespeed;
            for(int i = 0; i < TET_WIDTH; i++){
                for(int j = 0; j < TET_WIDTH; j++){
                    int x = (int)t.dx+i*(main.SPR_WIDTH), y = (int)t.dy+j*(main.SPR_WIDTH), ix = (t.x+i)*main.SPR_WIDTH, iy = (t.y+j)*main.SPR_WIDTH;
                    if(tetrominoList[t.i][t.j][i][j] > 0){
                        /*double da = 1-Math.max(0,1/(Math.sqrt(Math.pow(x-ix,2)+Math.pow(y-iy,2))));
                        int c = Math.min(255,(int)(da*255));
                        if(c > 10) draw.batchPush(9,boardx+ix+1,boardy+iy+1,8,8,new Color((int)draw.lerp(255,24,da),(int)draw.lerp(255,20,da),(int)draw.lerp(255,37,da),c));*/
                        draw.batchPush(t.t,boardx+x,boardy+y,main.SPR_WIDTH,main.SPR_WIDTH);
                        if(t.x+i >= 0 && t.x+i < boardWidth && t.y+j >= 2 && t.y+j < boardHeight && board[t.x+i][t.y+j] == 0) board[t.x+i][t.y+j] = -1;
                    }
                    // if(tetrominoList[nextTetronimo][0][i][j] > 0) draw.batchPush(8,boardx+boardWidth*main.SPR_WIDTH+(i+1)*main.SPR_WIDTH,boardy+(j+4)*main.SPR_WIDTH,main.SPR_WIDTH, main.SPR_WIDTH);
                    if(tetrominoList[nextTetronimo][0][i][j] > 0) draw.batchPush(8,20+(i+1)*main.SPR_WIDTH,boardy+(j+4)*main.SPR_WIDTH,main.SPR_WIDTH, main.SPR_WIDTH);
                }
            }

            draw.batchPush(9,boardx,boardy,boardWidth*main.SPR_WIDTH,2*main.SPR_WIDTH-1,new Color(24,20,37));

            //special goblin mode thing
            if(main.cfg.get("extend") == 1){

                for(int i = -main.SPR_WIDTH; i < main.FRAMEBUFFER_W; i += main.SPR_WIDTH){
                    /*draw.batchPush(100+Math.abs(i)%3,i+(int)cleardx%main.SPR_WIDTH,(int)(Math.sin((i/main.SPR_WIDTH)*2*Math.PI))+boardy+boardHeight*main.SPR_WIDTH, main.SPR_WIDTH, main.SPR_WIDTH);
                    draw.batchPush(110+Math.abs(i)%3,i+(int)cleardx%main.SPR_WIDTH,(int)(Math.sin((i/main.SPR_WIDTH)*2*Math.PI))+boardy+(boardHeight+1)*main.SPR_WIDTH, main.SPR_WIDTH, main.SPR_WIDTH);
                    draw.batchPush(120+Math.abs(i)%3,i+(int)cleardx%main.SPR_WIDTH,(int)(Math.sin((i/main.SPR_WIDTH)*2*Math.PI))+boardy+(boardHeight+2)*main.SPR_WIDTH, main.SPR_WIDTH, main.SPR_WIDTH);*/
                    draw.batchPush(100+Math.abs(i)%3,i+(int)cleardx%main.SPR_WIDTH,(i/main.SPR_WIDTH*i)%3-1+boardy+boardHeight*main.SPR_WIDTH, main.SPR_WIDTH, main.SPR_WIDTH);
                    draw.batchPush(110+Math.abs(i)%3,i+(int)cleardx%main.SPR_WIDTH,(i/main.SPR_WIDTH*i)%3-1+boardy+(boardHeight+1)*main.SPR_WIDTH, main.SPR_WIDTH, main.SPR_WIDTH);
                    draw.batchPush(120+Math.abs(i)%3,i+(int)cleardx%main.SPR_WIDTH,(i/main.SPR_WIDTH*i)%3-1+boardy+(boardHeight+2)*main.SPR_WIDTH, main.SPR_WIDTH, main.SPR_WIDTH);
                }
            }

            if(state == 3){
                illum *= 0.9;
                cleardy += main.SPR_WIDTH/(16*(1+cleardy));
                boardx += 2-(int)(Math.random()*4);
                boardy += 2-(int)(Math.random()*4);
                double i = cleardy/main.SPR_WIDTH;
                int j = (int)(Math.random()*5);
                draw.batchPush(9,boardx,boardy+cleary*main.SPR_WIDTH+(int)cleardy,boardWidth*main.SPR_WIDTH,Math.max(1,1+main.SPR_WIDTH-(int)cleardy),
                        new Color((int)draw.lerp(flash[clearflash].getRed(),24,i),(int)draw.lerp(flash[clearflash].getBlue(),20,i),(int)draw.lerp(flash[clearflash].getGreen(),37,i)));
                if(cleardy > main.SPR_WIDTH){
                    for(int k = 0; k < 10; k++) draw.particlePush(30,33,0.05+0.05*Math.random(),boardx+(int)(boardWidth*main.SPR_WIDTH*Math.random()),boardy+cleary*main.SPR_WIDTH,0,0,Color.WHITE);
                    cleardy = 0;
                    cleary = 0;
                    clearRows();
                    lines++;
                    if(lines > 10*(level+1) && main.cfg.get("extend") == 0){
                        level++;
                    }
                    if(checkRows() == 0) state = 0;
                }
            }else cleary = 0;

            if(state == 4 || state == 7){
                while(board[clearx%boardWidth][clearx/boardWidth] == 0 && clearx < boardWidth*boardHeight-1) clearx++;
                int x = clearx%boardWidth, y = clearx/boardWidth;
                if(board[x][y] > 0 && board[x][y] < 100){
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
                    boardy += 2-(int)(Math.random()*4);
                }else draw.particlePush(30,31,0.01+0.01*Math.random(),boardx+(x*main.SPR_WIDTH),boardy+y*main.SPR_WIDTH,0,0,Color.WHITE);

                if(clearx >= boardWidth*boardHeight-1){
                    if(state == 7){
                        level++;
                        loadLevel(level*10,boardWidth,boardHeight);
                        state = 6;
                        cleardx = boardWidth*main.SPR_WIDTH;
                        clearx = 0;
                    }else if(state == 4 && lives <= 0) state = 2;
                    else state = 0;
                }else clearx++;
                // draw.drawText(""+clearx+". "+state,80,80,8,6); // debug
            }else if(clearx > 0) clearx--;

            if(state == 1){
                draw.drawText("PAUSED",main.FRAMEBUFFER_W/2,main.FRAMEBUFFER_H/2,10,8,null,1);
                draw.drawText("ESC TO RESUME",main.FRAMEBUFFER_W/2,main.FRAMEBUFFER_H/2+10,8,6,Color.GRAY,1);
            }
            if(main.cfg.get("extend") == 1){
                double j = Math.min(1,(clearx*Math.log(1+clearx))/((double)boardWidth*boardHeight));
                if(state == 7) j = 0; //hack
                int dx = (int)draw.lerp(12,boardx+(boardWidth/2f)*main.SPR_WIDTH-12,j)+(int)(Math.random()*j*4-2*j),
                dy = (int)draw.lerp(30,main.FRAMEBUFFER_H/2f,j)+(int)(Math.random()*j*4-2*j);
                for(int i = 0; i < 3; i++){
                    draw.batchPush(140,dx+i*(main.SPR_WIDTH+1),dy,main.SPR_WIDTH,main.SPR_WIDTH);
                    if(lives > i) draw.batchPush(141,dx+i*(main.SPR_WIDTH+1),dy,main.SPR_WIDTH,main.SPR_WIDTH);
                }
                if(j == 1){
                    String txt;
                    if(lives == 2) txt = "TRY AGAIN";
                    else if(lives == 1) txt = "CAREFUL NOW";
                    else txt = "GOODBYE";
                    int w = (int)((txt.length()*10)/2f);
                    draw.batchPush(9,dx+12-w,dy+10,2*w,10,new Color(24,20,37));
                    draw.drawText(txt,dx+12-w,dy+10,10,10,null);
                }
                if(state == 6) draw.drawText("LEVEL CLEARED".substring(0,(int)(13*Math.min(1,((boardWidth*main.SPR_WIDTH-cleardx)*3)/(boardWidth*main.SPR_WIDTH)))),main.FRAMEBUFFER_W/2-65,main.FRAMEBUFFER_H/2,10,10,null);
            }
        }else{
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

        draw.drawText("LEVEL "+level,10,10,10,8,Color.WHITE);
        draw.drawText(""+score,10,20,8,6,flash[(score/100)%5]);
    }
}