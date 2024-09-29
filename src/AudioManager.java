import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class AudioClip{
    public Clip clip;
    public FloatControl fcgain;
    public FloatControl fcpan;
    public float xf,yf,xt,yt;

    public AudioClip(Clip clip){
        this.clip = clip;
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) fcgain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        if (clip.isControlSupported(FloatControl.Type.PAN)) fcpan = (FloatControl) clip.getControl(FloatControl.Type.PAN);
    }

    public void play(double _xf, double _yf, double _xt, double _yt){
        if((Integer)Tetris2805.main.cfg.get("sound") == 1){
            xf = (float)_xf;
            yf = (float)_yf;
            xt = (float)_xt;
            yt = (float)_yt;
            //if(xf+yf+xt+yt != 0){
                float pan = Math.max(Math.min((xf-xt)/40,1),-1);
                float dist = (float)Math.max(Math.min((1.-Math.sqrt((xf-xt)*(xf-xt)+(yf-yt)*(yf-yt))/100.),1),0);
                //if(dist > 0.01){
                if(fcpan != null) fcpan.setValue(fcpan.getValue()-(fcpan.getValue()-pan)*0.1f);
                if(fcgain != null){
                    float minGain = fcgain.getMinimum();
                    float maxGain = fcgain.getMaximum();
                    float gainValue = minGain + (maxGain - minGain) * (1 - dist);
                    fcgain.setValue(fcgain.getValue()-(fcgain.getValue()-gainValue)*0.1f);
                }
                //}
            //}
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public void pos(double _xf, double _yf, double _xt, double _yt){
        xf = (float)_xf;
        yf = (float)_yf;
        xt = (float)_xt;
        yt = (float)_yt;
    }

    public void stop(){
        clip.stop();
    }

    public boolean playing(){
        return clip.isRunning();
    }

}

public class AudioManager {
    static final Map<String, AudioClip> audio = new HashMap<>();

    public static void load(String name, String path) {
        load(name,path,".wav");
    }
    public static void load(String name, String path, String ext) {
        try {
            File file = new File(path+name+ext);
            AudioInputStream as = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(as);

            // Store the clip in the HashMap
            audio.put(name, new AudioClip(clip));
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void start(){
        Thread audiothread = new Thread(() -> {
            double expectedFrametime = 1000000000 / (60.);
            while(Tetris2805.main.gameShouldClose == 0){
                long now = System.nanoTime();
                for(String key : audio.keySet()){
                    AudioClip ac = audio.get(key);
                    if((Integer)Tetris2805.main.cfg.get("sound") == 0){
                        if(ac.playing()) ac.stop();
                        //System.out.println("audio stopped");
                        //break;
                        continue;
                    }
                    if((Integer)Tetris2805.main.cfg.get("music") == 0 && (key == "ambienthigh" || key == "ambientlow")){
                        if(ac.playing()) ac.stop();
                        //System.out.println("music stopped");
                        //break;
                        continue;
                    }
                    float xf = ac.xf;
                    float yf = ac.yf;
                    float xt = ac.xt;
                    float yt = ac.yt;
                    float pan = Math.max(Math.min((xf-xt)/40,1),-1);
                    float dist = (float)Math.max(Math.min((1.-Math.sqrt((xf-xt)*(xf-xt)+(yf-yt)*(yf-yt))/100.),1),0);
                    //if(dist > 0.01){
                    if(ac.fcpan != null) ac.fcpan.setValue(ac.fcpan.getValue()-(ac.fcpan.getValue()-pan)*0.1f);
                    if(ac.fcgain != null){
                        float minGain = ac.fcgain.getMinimum();
                        float maxGain = ac.fcgain.getMaximum();
                        float gainValue = minGain + (maxGain - minGain) * (1 - dist);
                        ac.fcgain.setValue(ac.fcgain.getValue()-(ac.fcgain.getValue()-gainValue)*0.1f);
                    }
                    //}
                }
                long timeTaken = System.nanoTime() - now, sleepTime = (long)(expectedFrametime - timeTaken);
                Tetris2805.main.frametimes[1] = timeTaken;
                if (sleepTime/1000000 > 0) {
                    try {
                        Thread.sleep(sleepTime / 1000000, (int)(sleepTime % 1000000));
                    } catch (InterruptedException e) {
                        // e.printStackTrace(); // shouldnt happen anyway
                    }
                }
            }
        });
        audiothread.start();
    }
}
