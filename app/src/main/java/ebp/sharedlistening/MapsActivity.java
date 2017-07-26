package ebp.sharedlistening;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SpotifyPlayer.NotificationCallback, ConnectionStateCallback, GoogleMap.OnInfoWindowCloseListener, GoogleMap.OnMarkerClickListener {

    private static String apiUrl = "http://192.168.178.22:3000/users";

    private GoogleMap mMap;
    private Button playButton;
    private Button followButton;
    private Marker activeMarker;

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
    private SpotifyBroadcastReceiver receiver;

    private String ownSpotToken;
    //Google Maps --Currently Sets Marker and Position to Sydney

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        playButton = (Button) findViewById(R.id.playBtn);
        followButton = (Button) findViewById(R.id.followBtn);

        playButton.setVisibility(View.GONE);
        followButton.setVisibility(View.GONE);

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

        /*
        receiver = new SpotifyBroadcastReceiver();
        Log.v("META",SpotifyBroadcastReceiver.BroadcastTypes.METADATA_CHANGED);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(SpotifyBroadcastReceiver.BroadcastTypes.PLAYBACK_STATE_CHANGED);
        filter.addAction(SpotifyBroadcastReceiver.BroadcastTypes.METADATA_CHANGED);
        filter.addAction(SpotifyBroadcastReceiver.BroadcastTypes.QUEUE_CHANGED);
        this.registerReceiver(receiver, filter);
        */
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
        //this.unregisterReceiver(receiver);
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
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowCloseListener(this);

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                Context context = getApplicationContext(); //or getActivity(), YourActivity.this, etc.

                LinearLayout info = new LinearLayout(context);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(context);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(context);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });


        ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                requestSongs();
            }
        }, 0, 15, TimeUnit.SECONDS);


        checkPermission();
        mMap.setMyLocationEnabled(true);
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            LatLng home = new LatLng(location.getLatitude(), location.getLongitude());
                            /*
                            Marker marker =  mMap.addMarker(new MarkerOptions()
                                    .position(home)
                                    .title("Your location")
                                    .snippet("bunch of information" + "\n" + "which should result in new lines you know!" + "\n" +"The end"
                                    ));
                            marker.setTag(new Infos("",""));
                            */
                            //.icon(BitmapDescriptorFactory.fromAsset("assets/headphones.png")));
                            mMap.setMinZoomPreference(15);
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(home));
                        }
                    }
                });

    }

    public void listenToSong(View view) {
        Infos info = (Infos) activeMarker.getTag();
        mPlayer.playUri(null, info.songURI, 0, 0);
        Log.v("Listen", "song aktiv");
    }

    public void followUser(View view) {
        Infos info = (Infos) activeMarker.getTag();
        new FollowUser().execute(info.userId);
        //Log.v("Marker", info.userId);
    }


    public void getSongs(View view) {
        requestSongs();
    }


    public void setSong(View view) {
        //get Metadata - Prioritizing the Spotify app, with app player as fallback for now, because thats more important for our use cases
        //fetched uris are the same no matter the acquisition method
        SpotifyMetadata currentSong = null;

        //currentPlayerPlayedSong
        Metadata.Track currentPlayerTrack = mPlayer.getMetadata().currentTrack;
        if (!(currentPlayerTrack == null)) {
            Log.v("SONGS", "current Song: " + currentPlayerTrack.artistName + " - " + currentPlayerTrack.name);
            currentSong = new SpotifyMetadata(currentPlayerTrack.uri,currentPlayerTrack.artistName,currentPlayerTrack.albumName,currentPlayerTrack.name);
        }

        
        //Spotify App playing Song
        //Check if it fetched at least once: value is time in milis between runtime and jan 1st 1970
        Log.v("SONGS", "" + spotifyApp.getLastMetadataUpdate());
        if (spotifyApp.getLastMetadataUpdate() > 0) {
            currentSong = spotifyApp.getMetadata();
            Log.v("SONGS", "spotify App URI: ");
        }


        if(currentSong != null){
            sendCurrentSong(currentSong);
        }else{
            Toast toast = Toast.makeText(getApplicationContext(), "You aren't listening to any music!", Toast.LENGTH_LONG);
            toast.show();
        }

        //TODO: check a songfield for an empty string to see if something is playing

    }

    public void sendCurrentSong(final SpotifyMetadata song) {
        checkPermission();
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(final Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
                            uploadSong(requestQueue,location,song);
                        }
                    }
                });
    }

    public void uploadSong(RequestQueue requestQueue,final Location location,final SpotifyMetadata song) {
        String url = apiUrl + "/" + spotifyApp.getUserid();
        StringRequest putRequest = new StringRequest(Request.Method.PUT, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("response", response);
                        Toast toast = Toast.makeText(getApplicationContext(), "You are now sharing your music", Toast.LENGTH_LONG);
                        toast.show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("PUT_ERROR", "" + error.getMessage() + "," + error.toString());
                        Toast toast = Toast.makeText(getApplicationContext(), "There was an connection issue", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
        ) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                params.put("longitude", "" + location.getLongitude());
                params.put("latitude", "" + location.getLatitude());
                params.put("titel", "" + song.getTrackName());
                params.put("interpret", "" + song.getArtistName());
                params.put("album", "" + song.getAlbumName());
                params.put("spotifyURI", song.getTrackId());
                return params;
            }

            @Override
            protected VolleyError parseNetworkError(VolleyError volleyError) {
                return volleyError;
            }
        };
        requestQueue.add(putRequest);
    }

    public void getOwnSong(RequestQueue requestQueue) {

        String spotifyURL = "https://api.spotify.com/v1/me/player/currently-playing";
        StringRequest songRequest = new StringRequest(Request.Method.GET, spotifyURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("GOTSONG", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("CURRENT_ERROR", "" + error.getCause() + "," + error.toString());

                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                Log.v("TOKEN", ownSpotToken);
                params.put("Authorization", ownSpotToken);
                return params;
            }

            @Override
            protected VolleyError parseNetworkError(VolleyError volleyError) {
                return volleyError;
            }
        };
        requestQueue.add(songRequest);
    }

    public void requestSongs() {
        Log.v("SONGS", "GOTEM");

        checkPermission();
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            //clear all previous markers from the map
                            mMap.clear();
                            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

                            // Initialize a new JsonArrayRequest instance
                            Log.v("URL", apiUrl + "/?latitude=" + location.getLatitude() + "&longitude=" + location.getLongitude());
                            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                                    Request.Method.GET,
                                    apiUrl + "/?latitude=" + location.getLatitude() + "&longitude=" + location.getLongitude(),
                                    //"http://pastebin.com/raw/Em972E5s",
                                    null,
                                    new Response.Listener<JSONArray>() {
                                        @Override
                                        public void onResponse(JSONArray response) {
                                            try {
                                                for (int i = 0; i < response.length(); i++) {
                                                    JSONObject obj = response.getJSONObject(i);
                                                    JSONObject loc = obj.getJSONObject("location");
                                                    JSONArray coords = loc.getJSONArray("coordinates");
                                                    JSONObject song = obj.getJSONObject("song");
                                                    LatLng user = new LatLng(coords.getDouble(1), coords.getDouble(0));
                                                    String title = "Song : " + song.getString("titel") + "\n";
                                                    if (song.has("album")) {
                                                        title += "Album : " + song.getString("album") + "\n";
                                                    }
                                                    if (song.has("interpret")) {
                                                        title += "Interpret : " + song.getString("interpret") + "\n";
                                                    }
                                                    if (obj.has("userID") && song.has("spotifyURI")) {
                                                        Marker marker = mMap.addMarker(new MarkerOptions()
                                                                .position(user)
                                                                .title(obj.getString("userID"))
                                                                .snippet(title));

                                                        marker.setTag(new Infos(obj.getString("userID"), song.getString("spotifyURI")));
                                                        marker.setIcon((BitmapDescriptorFactory.fromAsset("headphones.png")));
                                                    }


                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            // Do something when error occurred
                                            Toast toast = Toast.makeText(getApplicationContext(), "There was an connection issue", Toast.LENGTH_LONG);
                                            toast.show();
                                            Log.v("Volley", error.toString());
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

    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {//Can add more as per requirement
            Log.v("Check", "requestPerm");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
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
        //mPlayer.playUri(null, "spotify:track:0oOvCyoDVfZ5FOQzR6hxoR", 0, 0);
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

    @Override
    public boolean onMarkerClick(Marker marker) {

        //Infos test = (Infos)marker.getTag();
        //Log.v("marker",test.userId);
        playButton.setVisibility(View.VISIBLE);
        followButton.setVisibility(View.VISIBLE);
        activeMarker = marker;
        return false;
    }

    @Override
    public void onInfoWindowClose(Marker marker) {
        Log.v("Info", "Closed");
        playButton.setVisibility(View.GONE);
        followButton.setVisibility(View.GONE);
        activeMarker = null;
    }


    //because no network stuff in mainthread
    private class fetchCredentials extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            spotifyApp.setUserid(spotWebService.getMe().id);
            return null;
        }
    }

    private class FollowUser extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] params) {
            Log.v("FollowUserAsync", "param: " + params[0]);
            final String userId = (String)params[0];
            spotWebService.followUsers(userId, new Callback<Object>() {
                @Override
                public void success(Object o, retrofit.client.Response response) {
                    //follow successful

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(getApplicationContext(), "You are now following " + userId, Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                }

                @Override
                public void failure(final RetrofitError error) {
                    //follow failed
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("Follow",error.getLocalizedMessage(), error.getCause());
                            Toast toast = Toast.makeText(getApplicationContext(), "Following " + userId + " failed", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                }
            });
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
                    Log.v("String", stringBuilder.toString());
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return e.getMessage();
            }
        }
    }


}
