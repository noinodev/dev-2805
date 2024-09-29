import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class AudioClip{
    // container-of-container class for spatial audio clips
    public Clip clip;
    public FloatControl fcgain;
    public FloatControl fcpan;
    public float xf,yf,xt,yt;

    public AudioClip(Clip clip){
        // initialize float controls
        this.clip = clip;
        // check file gain/pan support (mono audio does not support pan control)
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) fcgain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        if (clip.isControlSupported(FloatControl.Type.PAN)) fcpan = (FloatControl) clip.getControl(FloatControl.Type.PAN);
    }

    // pan floatcontrol value calculation
    public float getPan(){
        return Math.max(Math.min((xf-xt)/20,1),-1);
    }

    // gain / distance floatcontrol calculation
    public float getGain(){
        float dist = (float)Math.max(Math.min((300./Math.sqrt((xf-xt)*(xf-xt)+(yf-yt)*(yf-yt))),1),0);
        float minGain = fcgain.getMinimum();
        float maxGain = fcgain.getMaximum();
        return minGain + (maxGain - minGain) * (dist);
    }

    // play sound, overload for non-spatial or mono sounds
    public void play(){ play(0f,0f,0f,0f); }
    public void play(double _xf, double _yf, double _xt, double _yt){
        if((Integer)Tetris2805.main.cfg.get("sound") == 1){
            xf = (float)_xf;
            yf = (float)_yf;
            xt = (float)_xt;
            yt = (float)_yt;

            float pan = getPan();
            float gain = getGain();

            if(fcpan != null) fcpan.setValue(pan);
            if(fcgain != null) fcgain.setValue(gain);

            clip.setFramePosition(0);
            clip.start();
        }
    }

    // set the position of a sound while it is playing (used for ambient tracks mainly, because there is vertical spatial transition between two ambient tracks)
    public void pos(double _xf, double _yf, double _xt, double _yt){
        xf = (float)_xf;
        yf = (float)_yf;
        xt = (float)_xt;
        yt = (float)_yt;
    }

    // stop a track
    public void stop(){
        clip.stop();
    }

    // check if a track is playing
    public boolean playing(){
        return clip.isRunning();
    }

}

// static audio manager class
public class AudioManager {
    static final Map<String, AudioClip> audio = new HashMap<>(); // map of all audio clips

    public static void load(String name, String path) {
        load(name,path,".wav"); // default file extension, i found out after implementing this that Java only supports .wav
    }
    public static void load(String name, String path, String ext) {
        try {
            // open and load the clip
            File file = new File(path+name+ext);
            AudioInputStream as = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(as);

            audio.put(name, new AudioClip(clip));
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    // start audiomanager thread, necessary because sounds need to be updated asynchronously otherwise it gets weird
    public static void start(){
        Thread audiothread = new Thread(() -> {
            double expectedFrametime = 1000000000 / (60.);
            while(Tetris2805.main.gameShouldClose == 0){
                long now = System.nanoTime();
                // iterate all clips
                for(String key : audio.keySet()){
                    AudioClip ac = audio.get(key);
                    // sound config setting
                    if((Integer)Tetris2805.main.cfg.get("sound") == 0){
                        if(ac.playing()) ac.stop();
                        continue;
                    }
                    // music config setting
                    if((Integer)Tetris2805.main.cfg.get("music") == 0 && (key == "ambienthigh" || key == "ambientlow")){
                        if(ac.playing()) ac.stop();
                        continue;
                    }
                    // interpolate spatial positions
                    float xf = ac.xf;
                    float yf = ac.yf;
                    float xt = ac.xt;
                    float yt = ac.yt;
                    float pan = ac.getPan();
                    float gain = ac.getGain();
                    if(ac.fcpan != null) ac.fcpan.setValue(ac.fcpan.getValue()-(ac.fcpan.getValue()-pan)*0.1f);
                    if(ac.fcgain != null) ac.fcgain.setValue(ac.fcgain.getValue()-(ac.fcgain.getValue()-gain)*0.1f);
                }
                long timeTaken = System.nanoTime() - now, sleepTime = (long)(expectedFrametime - timeTaken);
                Tetris2805.main.frametimes[1] = timeTaken; // set EDT frametime for profiling
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
