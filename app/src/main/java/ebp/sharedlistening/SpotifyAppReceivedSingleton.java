package ebp.sharedlistening;

/**
 * Created by milan_000 on 26.07.2017.
 */

public class SpotifyAppReceivedSingleton {

    private static SpotifyAppReceivedSingleton instance;

    SpotifyMetadata spotifyMetadata = null;
    SpotifyPlaybackState spotifyPlaybackState = null;
    long lastMetadataUpdate = 0;

    String userid = "";

    public static SpotifyAppReceivedSingleton getInstance () {
        if (SpotifyAppReceivedSingleton.instance == null) {
            SpotifyAppReceivedSingleton.instance = new SpotifyAppReceivedSingleton ();
        }
        return SpotifyAppReceivedSingleton.instance;
    }

    public SpotifyMetadata getMetadata(){
        return spotifyMetadata;
    }

    public SpotifyPlaybackState getPlaybackState(){
        return spotifyPlaybackState;
    }

    public void updateMetadata(SpotifyMetadata data, long updateTime){
        spotifyMetadata = data;
        lastMetadataUpdate = updateTime;
    }

    public void updatePlaybackState(SpotifyPlaybackState state){
        spotifyPlaybackState = state;
    }

    public long getLastMetadataUpdate(){
        return lastMetadataUpdate;
    }

    public String getUserid(){
        return userid;
    }

    public void setUserid(String id){
        userid = id;
    }
}
