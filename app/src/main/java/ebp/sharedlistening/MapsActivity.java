package ebp.sharedlistening;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.tasks.OnSuccessListener;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.spotify.sdk.android.player.Error;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    private static String apiUrl = "http://192.168.178.22:3000/users";

    private GoogleMap mMap;

    //Spotify Web API
    private SpotifyApi spotWebApi = new SpotifyApi();
    private SpotifyService spotWebService;

    //Spotify Events send from the App, probably a good idea to always check against null or if updatetime >0
    private SpotifyAppReceivedSingleton spotifyApp = SpotifyAppReceivedSingleton.getInstance();

    //Spotify API Credentials
    private static final String SPOTIFY_CLIENT_ID = "c85909acec5348d1adba2e032cd9561e";
    private static final String SPOTIFY_REDIRECT_URI = "sharedListening-spotify://callback";
    //Spotify Request Code for Verification - Any Integer
    private static final int SPOTIFY_REQUEST_CODE = 1337;
    //Spotify Player
    private Player mPlayer;
    private FusedLocationProviderClient mFusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

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
        spotAuthBuilder.setScopes(new String[]{"user-read-private", "streaming", "user-follow-modify"});
        AuthenticationRequest spotAuthReq = spotAuthBuilder.build();
        AuthenticationClient.openLoginActivity(this, SPOTIFY_REQUEST_CODE, spotAuthReq);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);



    }

    //Get Results vom Lauched Activities through intents
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        //SpotifyLoginActivity
        if (requestCode == SPOTIFY_REQUEST_CODE) {
            AuthenticationResponse spotAuthRes = AuthenticationClient.getResponse(resultCode, intent);
            if (spotAuthRes.getType() == AuthenticationResponse.Type.TOKEN) {
                //WebApiToken
                spotWebApi.setAccessToken(spotAuthRes.getAccessToken());
                spotWebService = spotWebApi.getService();
                //fetch credentials for backend storage
                new fetchCredentials().execute("");
                //PlayerToken
                Config spotPlayerConfig = new Config(this, spotAuthRes.getAccessToken(), SPOTIFY_CLIENT_ID);
                Spotify.getPlayer(spotPlayerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MapsActivity.this);
                        mPlayer.addNotificationCallback(MapsActivity.this);
                    }

                    @Override
                    public void onError(Throwable error) {
                        Log.e("MapsActivity", "Could not initialize Spotify Player: " + error.getMessage());
                    }
                });
            }
        }
    }

    //Destroy the Player when the Activity is closed - Probably wont be able to run in the background with this
    @Override
    protected void onDestroy() {
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
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        /*
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.v("Wichtig", "returned");
            return;
        }
        */
            checkPermission();
            mMap.setMyLocationEnabled(true);
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                LatLng home = new LatLng(location.getLatitude(), location.getLongitude());
                               // mMap.addMarker(new MarkerOptions().position(home).title("Your location"));
                                mMap.setMinZoomPreference(15);
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(home));
                            }
                        }
                    });

            }

    public void getSongs(View view){
        Log.v("SONGS","GOTEM");

        Log.v("SONGS", "id: " + spotifyApp.getUserid());
        //Follow user (for now because backendcode is not finished cant be on main thread)
        /*
        String userid = spotWebService.getMe().id;
        spotWebService.followUsers(userid, new Callback<Object>() {

            @Override
            public void success(Object o, retrofit.client.Response response) {
                //follow successful
            }

            @Override
            public void failure(RetrofitError error) {
                //follow failed
            }
        });
        */

        //get Metadata - Prioritizing the Spotify app, with app player as fallback for now, because thats more important for our use cases
        //fetched uris are the same no matter the acquisition method
        String curArtist = "";
        String curTitle = "";
        String curAlbum = "";
        String curUri = "";

        //currentPlayerPlayedSong
        Metadata.Track currentPlayerTrack = mPlayer.getMetadata().currentTrack;
        if(!(currentPlayerTrack == null)){
            Log.v("SONGS", "current Song: " + currentPlayerTrack.artistName + " - " + currentPlayerTrack.name);
            curArtist = currentPlayerTrack.artistName;
            curTitle = currentPlayerTrack.name;
            curAlbum = currentPlayerTrack.albumName;
            curUri = currentPlayerTrack.uri;
        }

        //Spotify App playing Song
        //Check if it fetched at least once: value is time in milis between runtime and jan 1st 1970
        if(spotifyApp.getLastMetadataUpdate() > 0){
            SpotifyMetadata songData = spotifyApp.getMetadata();
            Log.v("SONGS", "current Song: " + songData.getArtistName() + " - " + songData.getTrackName());
            curArtist = songData.getArtistName();
            curTitle = songData.getTrackName();
            curAlbum = songData.getAlbumName();
            curUri = songData.getTrackId();
            Log.v("SONGS", "spotify App URI: " + curUri);
        }

        //TODO: check a songfield for an empty string to see if something is playing

        //new BackendTask().execute();
        checkPermission();
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.v("Request", location.toString());
                            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

                            // Initialize a new JsonArrayRequest instance
                            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                                    Request.Method.GET,
                                    apiUrl + "/?latitude=" +location.getLatitude()+ "&longitude=" + location.getLongitude() + "&distance=1",
                                    //"http://pastebin.com/raw/Em972E5s",
                                    null,
                                    new Response.Listener<JSONArray>() {
                                        @Override
                                        public void onResponse(JSONArray response) {
                                            try{
                                                for(int i=0;i<response.length();i++){
                                                    JSONObject obj = response.getJSONObject(i);
                                                    JSONObject loc = obj.getJSONObject("location");
                                                    JSONArray coords = loc.getJSONArray("coordinates");
                                                    JSONObject song = obj.getJSONObject("song");
                                                    LatLng user = new LatLng(coords.getDouble(1), coords.getDouble(0));

                                                    String title = "Song : " + song.getString("titel") + "\n";
                                                    if(song.has("album")){
                                                      //  title += "Album : " + song.getString("album")  + "\n";
                                                    }
                                                    if(song.has("interpret")){
                                                        //title += "Interpret : " + song.getString("interpret")  + "\n";
                                                    }


                                                    mMap.addMarker(new MarkerOptions()
                                                            .position(user)
                                                            .title(obj.getString("username"))
                                                            .snippet(title));


                                                }
                                            }catch (JSONException e){
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    new Response.ErrorListener(){
                                        @Override
                                        public void onErrorResponse(VolleyError error){
                                            // Do something when error occurred
                                            Log.v("Volley",error.toString());
                                        }
                                    }
                            );

                            // Add JsonArrayRequest to the RequestQueue
                            requestQueue.add(jsonArrayRequest);
                        }
                    }
                });





    }

    private void enableMyLocation() {
        checkPermission();
        if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (permissions.length == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermission();
                mMap.setMyLocationEnabled(true);
            } else {
                // Permission was denied. Display an error message.
            }
        }
    }
    public void checkPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ){//Can add more as per requirement
            Log.v("Check", "requestPerm");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
                    123);
        }
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

        mPlayer.playUri(null, "spotify:track:0oOvCyoDVfZ5FOQzR6hxoR", 0, 0);
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

    //because no network stuff in mainthread
    private class fetchCredentials extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] params) {
            spotifyApp.setUserid(spotWebService.getMe().id);
            return null;
        }
    }

    private class BackendTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            try {
                URL url = new URL(apiUrl + "/test");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    Log.v("String",stringBuilder.toString());
                    return stringBuilder.toString();
                }
                finally{
                    urlConnection.disconnect();
                }
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return e.getMessage();
            }
        }
    }



}
