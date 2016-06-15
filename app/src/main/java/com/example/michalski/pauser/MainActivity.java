package com.example.michalski.pauser;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class
            .getSimpleName();

    private Cast.Listener mCastClientListener;

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private MediaRouteButton mMediaRouteButton;
    private CastDevice mSelectedDevice;
    private int mRouteCount = 0;
    private GoogleApiClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSelectedDevice = null; //new CastDevice();

        mCastClientListener = new Cast.Listener();

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());

        mMediaRouterCallback = new MyMediaRouterCallback();


        mMediaRouteButton = (MediaRouteButton) findViewById(R.id.media_route_button);
        mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK).build();
        mMediaRouteButton.setRouteSelector(mMediaRouteSelector);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                RemoteMediaPlayer player = new RemoteMediaPlayer();

                try {
                    PendingResult<RemoteMediaPlayer.MediaChannelResult> result = player.pause(mClient);
                    String resultString = result.await().toString();
                    Snackbar.make(view, "Result: " + resultString, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "failed with : " + e.toString());
                }
                Log.e(TAG, Cast.CastApi.getApplicationStatus(mClient));
                Log.e(TAG, Cast.CastApi.getApplicationMetadata(mClient).getApplicationId());
                Log.e(TAG, Cast.CastApi.getApplicationMetadata(mClient).getSupportedNamespaces().toString());



                Cast.CastApi.joinApplication(mClient, "233637DE").setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
                    @Override
                    public void onResult(@NonNull Cast.ApplicationConnectionResult applicationConnectionResult) {
                        Log.e(TAG, "Connection: " + applicationConnectionResult.toString());
                        Log.e(TAG, applicationConnectionResult.getApplicationMetadata().getApplicationId());
                        Log.e(TAG, applicationConnectionResult.getSessionId());
                        Log.e(TAG, applicationConnectionResult.getApplicationStatus());
                        String message = "{\"type\": \"PAUSE\", \"requestId\": 9345}";
                        Log.e(TAG, message);
                        try {
                            Cast.CastApi.setMessageReceivedCallbacks(mClient, "urn:x-cast:com.google.cast.media", new Cast.MessageReceivedCallback() {
                                @Override
                                public void onMessageReceived(CastDevice castDevice, String s, String s1) {
                                    Log.e(TAG, "Got " + s + " : " + s1);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Cast.CastApi.sendMessage(mClient, "urn:x-cast:com.google.cast.media", message).setResultCallback(
                                new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(@NonNull Status status) {
                                        Log.e(TAG, status.toString());
                                    }
                                }
                        );

                    }
                });

                /*String message = "{\"type\": \"PAUSE\"," +
                        "\"requestId\": 123," +
                        "\"mediaSessionId\": 11}";*/
                /*
                String message = "{ \"type\": \"GET_STATUS\"," +
                        "\"requestId\": 9345 }";
                */
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("aaa", "Failed to connect.");

    }

    void InitClient() {
        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                .builder(mSelectedDevice, mCastClientListener);
        mClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this)
                .addApi(Cast.API, apiOptionsBuilder.build()).build();
        mClient.connect();
    }


    private class MyMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            super.onRouteSelected(router, route);
            Log.d(TAG, "onRouteSelected");
            mSelectedDevice = CastDevice.getFromBundle(route.getExtras());
            Log.d(TAG, route.getExtras().toString());
            InitClient();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            super.onRouteUnselected(router, route);
            mSelectedDevice = null;
        }
    }
}
