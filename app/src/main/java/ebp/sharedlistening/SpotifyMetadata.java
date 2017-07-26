package ebp.sharedlistening;

/**
 * Created by milan_000 on 26.07.2017.
 */

public class SpotifyMetadata {
    public SpotifyMetadata(String trackId, String artistName, String albumName, String trackName, int trackLengthInSec) {
        this.trackId = trackId;
        this.artistName = artistName;
        this.albumName = albumName;
        this.trackName = trackName;
        this.trackLengthInSec = trackLengthInSec;
    }

    public String getTrackId() {

        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public int getTrackLengthInSec() {
        return trackLengthInSec;
    }

    public void setTrackLengthInSec(int trackLengthInSec) {
        this.trackLengthInSec = trackLengthInSec;
    }

    private String trackId;
    private String artistName;
    private String albumName;
    private String trackName;
    private int trackLengthInSec;
}
