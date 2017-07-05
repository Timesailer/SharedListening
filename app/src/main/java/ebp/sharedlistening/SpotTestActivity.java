package ebp.sharedlistening;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

public class SpotTestActivity extends Activity implements SpotifyPlayer.NotificationCallback, ConnectionStateCallback{

    //Spotify API Credentials
    private static final String SPOTIFY_CLIENT_ID = "c85909acec5348d1adba2e032cd9561e";
    private static final String SPOTIFY_REDIRECT_URI = "sharedListening-spotify://callback";
    //Spotify Request Code for Verification - Any Integer
    private static final int SPOTIFY_REQUEST_CODE = 1337;
    //Spotify Player
    private Player mPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spot_test);

        AuthenticationRequest.Builder spotAuthBuilder = new AuthenticationRequest.Builder(SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, SPOTIFY_REDIRECT_URI);
        spotAuthBuilder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest spotAuthReq = spotAuthBuilder.build();
        AuthenticationClient.openLoginActivity(this, SPOTIFY_REQUEST_CODE, spotAuthReq);

        final Button button = (Button)findViewById(R.id.testTrack);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                Log.v("Spottest", "Test");
                Log.d("Spottest", "Player: " + mPlayer);
                mPlayer.playUri(null, "spotify:track:2TpxZ7JUBn3uw46aR7qd6V", 0, 0);
            }
        });

    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        Log.v("MapActivity", "OnActivityResult executed");
        //SpotifyLoginActivity
        if(resultCode == SPOTIFY_REQUEST_CODE){
            AuthenticationResponse spotAuthRes = AuthenticationClient.getResponse(resultCode, intent);
            if(spotAuthRes.getType() == AuthenticationResponse.Type.TOKEN){
                Config spotPlayerConfig = new Config(this, spotAuthRes.getAccessToken(), SPOTIFY_CLIENT_ID);
                Spotify.getPlayer(spotPlayerConfig, this, new SpotifyPlayer.InitializationObserver(){
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer){
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(SpotTestActivity.this);
                        mPlayer.addNotificationCallback(SpotTestActivity.this);
                    }

                    @Override
                    public void onError(Throwable error){
                        Log.e("MapsActivity", "Could not initialize Spotify Player: " + error.getMessage());
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy(){
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.v("MapActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.v("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.v("MapActivity", "User logged in");

        mPlayer.playUri(null, "spotify:track:2TpxZ7JUBn3uw46aR7qd6V", 0, 0);
    }

    @Override
    public void onLoggedOut() {
        Log.v("MapActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.v("MapActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.v("MapActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.v("MapActivity", "Received connection message: " + message);
    }
}
