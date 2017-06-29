package ebp.sharedlistening;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.spotify.sdk.android.player.Error;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    private GoogleMap mMap;

    //Spotify API Credentials
    private static final String SPOTIFY_CLIENT_ID = "c85909acec5348d1adba2e032cd9561e";
    private static final String SPOTIFY_REDIRECT_URI = "sharedListening-spotify://callback";
    //Spotify Request Code for Verification - Any Integer
    private static final int SPOTIFY_REQUEST_CODE = 1337;
    //Spotify Player
    private Player mPlayer;

    //Google Maps --Currently Sets Marker and Position to Sydney

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Authenticate Spotify User on Startup
        AuthenticationRequest.Builder spotAuthBuilder = new AuthenticationRequest.Builder(SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, SPOTIFY_REDIRECT_URI);
        spotAuthBuilder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest spotAuthReq = spotAuthBuilder.build();
        AuthenticationClient.openLoginActivity(this, SPOTIFY_REQUEST_CODE, spotAuthReq);
    }

    //Get Results vom Lauched Activities through intents
    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);

        //SpotifyLoginActivity
        if(resultCode == SPOTIFY_REQUEST_CODE){
            AuthenticationResponse spotAuthRes = AuthenticationClient.getResponse(resultCode, intent);
            if(spotAuthRes.getType() == AuthenticationResponse.Type.TOKEN){
                Config spotPlayerConfig = new Config(this, spotAuthRes.getAccessToken(), SPOTIFY_CLIENT_ID);
                Spotify.getPlayer(spotPlayerConfig, this, new SpotifyPlayer.InitializationObserver(){
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer){
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MapsActivity.this);
                        mPlayer.addNotificationCallback(MapsActivity.this);
                    }

                    @Override
                    public void onError(Throwable error){
                        Log.e("MapsActivity", "Could not initialize Spotify Player: " + error.getMessage());
                    }
                });
            }
        }
    }

    //Destroy the Player when the Activity is closed - Probably wont be able to run in the background with this
    @Override
    protected void onDestroy(){
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    //Map Overrides
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    //Spotify Overrides
    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MapActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MapActivity", "User logged in");

        mPlayer.playUri(null, "spotify:track:2TpxZ7JUBn3uw46aR7qd6V", 0, 0);
    }

    @Override
    public void onLoggedOut() {
        Log.d("MapActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.d("MapActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MapActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MapActivity", "Received connection message: " + message);
    }
}
