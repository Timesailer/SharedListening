package ebp.sharedlistening;

/**
 * Created by milan_000 on 26.07.2017.
 */

public class SpotifyPlaybackState {
    boolean playing;

    public SpotifyPlaybackState(boolean playing, int positionInMs) {
        this.playing = playing;
        this.positionInMs = positionInMs;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public int getPositionInMs() {
        return positionInMs;
    }

    public void setPositionInMs(int positionInMs) {
        this.positionInMs = positionInMs;
    }

    int positionInMs;
}
