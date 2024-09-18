import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

abstract class scene { // scene base class
    protected Tetris2805 main;
    protected D2D draw;

    public scene(Tetris2805 m, D2D d){
        main = m;
        draw = d;
    }

    public void loop(){}
}

class splash extends scene { // splash screen
    private int time;
    public splash(Tetris2805 m, D2D d) {
        super(m, d);
        time = 0;
        D2D.clearColour = new Color(24,20,37);
        main.sceneIndex = 0;
    }
    @Override
    public void loop(){
        time++;
        if(time > main.TPS) main.currentScene = new menu(main,draw);
        draw.batchPush(43,0,0,main.FRAMEBUFFER_W,main.FRAMEBUFFER_H);

        //draw.batchPush(9,0,0,main.FRAMEBUFFER_W,main.FRAMEBUFFER_H,new Color(24,20,37));
        draw.drawText("TETRIS",main.FRAMEBUFFER_W/2,main.FRAMEBUFFER_H/2-20,10,10,new Color(time,time,time),1);
        draw.drawText("JAVA GAME",main.FRAMEBUFFER_W/2,main.FRAMEBUFFER_H/2-10,8,6,new Color(time/2,time/2,time/2),1);
    }
}

class menu extends scene { // main menu
    private int time;
    public menu(Tetris2805 m, D2D d) {
        super(m, d);
        time = 0;
        D2D.clearColour = new Color(24,20,37);
        main.sceneIndex = 1;
    }
    @Override
    public void loop(){
        if(time < main.TPS) time++;
        double a = time/main.TPS;
        main.bgtx = (main.frame*0.4);

        draw.drawText("TETRIS",20,20,10,8,new Color((int)(255*a),(int)(255*a),(int)(255*a)));
        draw.drawText("JAVA GAME BY NATHAN BURG",20,30,8,6,new Color((int)(255*(a/2)),(int)(255*(a/2)),(int)(255*(a/2))));

        int bx = 20;//main.FRAMEBUFFER_W/2-100;
        if(a > 0.25 && draw.drawButton("PLAY",bx,40,80,10) == 1) main.currentScene = new Game(main,draw);
        if(a > 0.5 && draw.drawButton("CONFIGURE",bx,51,80,10) == 1) main.currentScene = new config(main,draw);
        if(a > 0.75 && draw.drawButton("HIGHSCORE",bx,62,80,10) == 1) main.currentScene = new hscore(main,draw);
        if(a >= 1 && draw.drawButton("EXIT",bx,73,80,10) == 1) main.displayconfirm = main.DIALOG_CONTEXT_EXIT;
    }

}

class config extends scene { // config menu
    private int time;
    public config(Tetris2805 m, D2D d) {
        super(m, d);
        time = 0;
        D2D.clearColour = new Color(24,20,37);
        main.sceneIndex = 2;
    }
    @Override
    public void loop(){
        main.bgtx = 400+main.frame*0.1;
        if(time < main.TPS) time++;
        double a = time/main.TPS;
        draw.drawText("CONFIGURE",20,20,10,8,new Color((int)(255*a),(int)(255*a),(int)(255*a)));

        main.cfg.put("width",draw.drawSlider("BOARD WIDTH",20,30,main.FRAMEBUFFER_W-40,10,main.cfg.get("width"),5,15));
        main.cfg.put("height",draw.drawSlider("BOARD HEIGHT",20,30+10,main.FRAMEBUFFER_W-40,10,main.cfg.get("height"),15,30));
        main.cfg.put("level",draw.drawSlider("LEVEL",20,30+10*2,main.FRAMEBUFFER_W-40,10,main.cfg.get("level"),0,10));

        main.cfg.put("music",draw.drawToggle("MUSIC",20,30+10*3,main.FRAMEBUFFER_W-40,10,main.cfg.get("music")));
        main.cfg.put("sound",draw.drawToggle("SFX",20,30+10*4,main.FRAMEBUFFER_W-40,10,main.cfg.get("sound")));
        main.cfg.put("ai",draw.drawToggle("AI PLAY",20,30+10*5,main.FRAMEBUFFER_W-40,10,main.cfg.get("ai")));
        main.cfg.put("extend",draw.drawToggle("GOBLIN MODE",20,30+10*6,main.FRAMEBUFFER_W-40,10,main.cfg.get("extend")));

        if(draw.drawButton("APPLY",20,30+10*9,80,10) == 1) main.saveData(main.cfg,"src/data/config.txt");
        if(draw.drawButton("RESET",20,30+10*8,80,10) == 1) main.cfg = main.loadData("src/data/cfgdef.txt");

        if(draw.drawButton("BACK",20,main.FRAMEBUFFER_H-20,80,10) == 1) main.currentScene = new menu(main,draw);
    }

}

class hscore extends scene { // leaderboard menu
    private int time;
    private ArrayList<Map.Entry<String, Integer>> list;
    public hscore(Tetris2805 m, D2D d) {
        super(m, d);
        time = 0;
        D2D.clearColour = new Color(24,20,37);
        // sorted score list
        list = new ArrayList<>(main.scores.entrySet());
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        main.sceneIndex = 3;
    }
    @Override
    public void loop(){
        main.bgtx = 800+main.frame*0.1;
        if(time < main.TPS) time++;
        double a = time/main.TPS;
        draw.drawText("HIGHSCORES",20,20,10,8,new Color((int)(255*a),(int)(255*a),(int)(255*a)));
        // draw leaderboard
        int i = 0;
        for(Map.Entry<String, Integer> entry : list){
            int c = (int)(255*(a/(2+i)));
            draw.drawText(entry.getKey() + " " + entry.getValue(),20,30+8*i,8,6,new Color(c,c,c));
            i++;
        }
        if(draw.drawButton("BACK",20,main.FRAMEBUFFER_H-20,80,10) == 1) main.currentScene = new menu(main,draw);
    }

}
