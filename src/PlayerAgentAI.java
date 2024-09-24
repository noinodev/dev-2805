import java.awt.*;

public class PlayerAgentAI {
    //static Tetris2805 main;
    //static byte[][] board; // copy of the visible space of the board
    static final double[] weights = {-0.510066,0.760666,-0.35663,-0.184483};

    static Object[] getBestPosition(int[][] gameboard, int xb, int xw,ObjectTetromino[] pieces, int index){
        D2D draw = D2D.D2Dget();
        double bestscore = Double.MAX_VALUE;
        ObjectTetromino best = null;
        int[][] bestboard = null;
        int[][] board = cpy(gameboard,xb,xw);
        for(int rotation = 0; rotation < 4; rotation++){
            ObjectTetromino agent = new ObjectTetromino(pieces[index]);
            agent.dx -= xb;
            //System.out.print("[rot:"+rotation+"]");
            agent.rotation = rotation;
            while(valid(board,agent) == 1) agent.dx--;
            agent.dx++;
            //System.out.print("[x:"+agent.dx+"]");
            while(valid(board,agent) == 1){
                ObjectTetromino ta = new ObjectTetromino(agent);
                while(valid(board,ta) == 1) ta.dy++;
                ta.dy--;
                //System.out.print("[y:"+ta.dy+"]");

                int[][] tb = cpy(board,0,board.length);
                merge(tb,ta);


                double score = Double.MAX_VALUE;
                if(index == pieces.length-1) score = -weights[0]*aggregateHeight(tb)+weights[1]*lines(tb)-weights[2]*holes(tb)-weights[3]*bumpiness(tb);
                else score = (double) getBestPosition(tb,0,tb.length,pieces,index+1)[1];
                if(score < bestscore){
                    //System.out.print("[score:"+score+"]\n");
                    bestscore = score;
                    best = new ObjectTetromino(agent);
                    bestboard = cpy(tb,0,tb.length);

                    best.dx += xb;
                }
                agent.dx++;
            }
        }
        /*if(best != null){
            double s = 4;
            for(int i = 0; i < bestboard.length; i++){
                for(int j = 0; j < bestboard[0].length; j++){
                    if(bestboard[i][j] == 1) draw.batchPush(9,draw.view_x+draw.view_w/6+(i)* s,draw.view_y+draw.view_h/6+(j)* s,s,s);
                    else draw.batchPush(18,draw.view_x+draw.view_w/6+(i)* s,draw.view_y+draw.view_h/6+(j)* s,s,s);
                }
            }
            for(int i = 0; i < 4; i++){
                for(int j = 0; j < 4; j++){
                    if(ObjectTetromino.tetrominoList[best.index][best.rotation][i][j] > 0){
                        draw.batchPush(9,draw.view_x+draw.view_w/6+(best.dx-xb+i)* s,draw.view_y+draw.view_h/6+(best.dy+j)* s,s,s, Color.MAGENTA); // draw tetromino
                    }
                }
            }
        }*/
        Object[] ret = new Object[2];
        ret[0] = best;
        ret[1] = bestscore;
        return ret;
    }

    static int[][] cpy(int[][] gameboard, int xb, int xw){
        // copy the contents of the gameboard within the specified bounds to the ai agent 'view'
        int[][] board = new int[xw][gameboard[0].length];
        for(int x = xb; x < xb+xw; x++){
            for(int y = 0; y < gameboard[0].length; y++){
                int i = gameboard[x][y];
                board[x-xb][y] = 0;
                if(i > 0 && (i < 100 || i == 114)) board[x-xb][y] = 1;
            }
        }
        return board;
    }

    static int columnHeight(int[][] board, int x){
        int y;
        for(y = 0; y < board[0].length && board[x][y] == 0; y++);
        return board[0].length-y;
    }
    static int aggregateHeight(int[][] board){
        int total = 0;
        for(int x = 0; x < board.length; x++) total += columnHeight(board,x);
        return total;
    }
    static int bumpiness(int[][] board){
        int total = 0;
        for(int x = 0; x < board.length-1; x++) total += Math.abs(columnHeight(board,x)-columnHeight(board,x+1));
        for(int x = 1; x < board.length; x++) total += Math.abs(columnHeight(board,x)-columnHeight(board,x-1));
        return total/2;
    }
    static int lines(int[][] board){
        int total = 0;
        for(int y = 0; y < board[0].length; y++){
            int isline = 1;
            for(int x = 0; x < board.length; x++){
                if(board[x][y] == 0){
                    isline = 0;
                    break;
                }
            }
            total += isline;
        }
        return total;
    }
    static int holes(int[][] board){
        int total = 0;
        for(int x = 0; x < board.length; x++){
            int block = 0;
             // come back and double check this
            for(int y = 0; y < board[0].length; y++){
                if(board[x][y] != 0) block = 1;
                else if(board[x][y] == 0 && block == 1) total++;
            }
        }
        return total;
    }

    static void merge(int[][] board, ObjectTetromino agent){
        for(int i = 0; i < 4; i++){
            for(int j = 0; j < 4; j++){
                int tx = agent.dx+i, ty = agent.dy+j;
                if(ObjectTetromino.tetrominoList[agent.index][agent.rotation][i][j] > 0) board[tx][ty] = 1;
            }
        }
    }

    static byte valid(int[][] board, ObjectTetromino agent){ // tetromino collision function
        for(int i = 0; i < 4; i++){
            for(int j = 0; j < 4; j++){
                if(ObjectTetromino.tetrominoList[agent.index][agent.rotation][i][j] > 0){
                    int tx = agent.dx+i, ty = agent.dy+1+j;
                    if(ty < 0 || ty >= board[0].length || tx < 0 || tx >= board.length || board[tx][ty] > 0){
                        return 0;
                    }
                }
            }
        }
        return 1;
    }
}
