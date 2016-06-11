import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerException;

import java.io.File;

/**
 * MusicPlayer class represents a persistent MusicPlayer object
 * This class handles all operations related to music playing
 *
 * @author shiTunes inc.
 */
public class MusicPlayer {

    private int loadedSongRow;   // Table row of loaded song
    private BasicPlayer player;
    private BasicController controller;
    private double volume;

    /**
     * MusicPlayer default constructor, instantiates the persistent BasicPlayer object
     *
     */
    public MusicPlayer() {
        player = new BasicPlayer();
        controller = (BasicController) player;
        volume = -1.0;    // indicates that gain has yet to be initialized
    }

    /**
     * Gets the current volume level of the player/system converted to Volume Slider value
     *
     * @return the current volume level as an int value in range [0, 100]
     */
    public int getSliderVolume() {
        return (int)(this.volume * 100);
    }

    /**
     * Returns the current volume in range [0.0, 1.0]
     *
     * @return the current volume
     */
    public double getVolume() { return volume; };

    public BasicPlayer getPlayer() {
        return player;
    }

    /**
     * Adjusts the volume to the given value
     * <p>
     * Note: the volume value must be in range [0.0, 1.0] as per
     * BasicPlayer setGain() method requirement
     *
     * @param volume the volume to change to (double value in range [0.0, 1.0]
     */
    public void adjustVolume(double volume) {
        try {
            controller.setGain(volume);
            this.volume = volume;
        } catch (BasicPlayerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Plays the selected song
     *
     * @param filePath the file path of the song to play
     * @return true if song plays successfully
     */
    public boolean play(String filePath) {
        try {
            controller.open(new File(filePath));
            // play loaded song
            controller.play();
            // setGain to default .5 value
            if(this.volume == -1.0) {
                controller.setGain(0.5);
                this.volume = 0.5;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Resumes a previously paused song
     *
     * @return true if song is resumed successfully
     */
    public boolean resume() {
        try {
            controller.resume();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Pauses the currently playing song
     *
     * @return true if the song is paused successfully
     */
    public boolean pause() {
        try {
            controller.pause();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Stops the currently playing song
     *
     * @return true if song stopped successfully
     */
    public boolean stop() {
        try {
            controller.stop();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gets the currently loaded song row
     *
     * @return the currently loaded song's row
     */
    public int getLoadedSongRow() {
        return loadedSongRow;
    }

    /**
     * Sets the currently loaded song using it's song id
     *
     * @param row the song id of the song being loaded
     */
    public void setLoadedSongRow(int row) {
        this.loadedSongRow = row;
    }

}
