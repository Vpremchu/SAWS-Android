package presentation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import domain.MessageIO;
import io.antmedia.android.broadcaster.ILiveVideoBroadcaster;
import io.antmedia.android.broadcaster.LiveVideoBroadcaster;
import io.antmedia.android.broadcaster.utils.Resolution;
import logic.CameraResolutionsFragment;
import liveVideoBroadcaster.R;
import logic.AuthManager;
import logic.ChatBoxAdapter;
import logic.CryptoManager;

import static android.provider.Settings.Secure.ANDROID_ID;


public class LiveVideoBroadcasterActivity extends AppCompatActivity {

    private static final String RTMP_BASE_URL = "rtmp://141.138.142.48/live/";

    private static final String TAG = LiveVideoBroadcasterActivity.class.getSimpleName();
    private ViewGroup mRootView;
    boolean mIsRecording = false;
    private Timer mTimer;
    private long mElapsedTime;
    public TimerHandler mTimerHandler;
    private ImageButton mSettingsButton;
    private CameraResolutionsFragment mCameraResolutionsDialog;
    private Intent mLiveVideoBroadcasterServiceIntent;
    private TextView mStreamLiveStatus;
    private GLSurfaceView mGLView;
    private ILiveVideoBroadcaster mLiveVideoBroadcaster;
    private Button mBroadcastControlButton;

    private AuthManager authManager;

    private Socket mSocket;
    private LinearLayout chatView;
    private RequestQueue requestQueue;
    private String globalUsername;
    private String UUID;
    private Button toggleProfileButton;

    public RecyclerView myRecyclerView;
    public List<MessageIO> MessageIOList;
    public ChatBoxAdapter chatBoxAdapter;
    public  EditText messagetxt;
    public  Button send;
    private static final int PERMISSION_READ_STATE = 0;


    private Map<String, Integer> userColors = new HashMap<>();

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LiveVideoBroadcaster.LocalBinder binder = (LiveVideoBroadcaster.LocalBinder) service;
            if (mLiveVideoBroadcaster == null) {
                mLiveVideoBroadcaster = binder.getService();
                mLiveVideoBroadcaster.init(LiveVideoBroadcasterActivity.this, mGLView);
                mLiveVideoBroadcaster.setAdaptiveStreaming(true);
            }
            mLiveVideoBroadcaster.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mLiveVideoBroadcaster = null;
        }
    };


    public RequestQueue startQueue() {
        RequestQueue requestQueue;
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);
        Network network = new BasicNetwork(new HurlStack());
        requestQueue = new RequestQueue(cache, network);
        requestQueue.start();
        return requestQueue;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null && !extras.isEmpty()) {
            globalUsername = extras.getString("username");
        }

        UUID = getUUID();

        SharedPreferences sharedPreferences = getSharedPreferences("localStorage", MODE_PRIVATE);
        authManager = new AuthManager(sharedPreferences);

        // Hide title
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //binding on resume not to having leaked service connection
        mLiveVideoBroadcasterServiceIntent = new Intent(this, LiveVideoBroadcaster.class);
        //this makes service do its job until done
        startService(mLiveVideoBroadcasterServiceIntent);

        this.requestQueue = startQueue();

        setContentView(R.layout.activity_live_video_broadcaster);

        mTimerHandler = new TimerHandler();

        mRootView =findViewById(R.id.root_layout);
        mSettingsButton = findViewById(R.id.settings_button);
        mStreamLiveStatus = findViewById(R.id.stream_live_status);

        mBroadcastControlButton = findViewById(R.id.toggle_broadcasting);

        chatView = findViewById(R.id.chatView);

        // Configure the GLSurfaceView.  This will start the Renderer thread, with an
        // appropriate EGL activity.
        mGLView = findViewById(R.id.cameraPreview_surfaceView);
        if (mGLView != null) {
            mGLView.setEGLContextClientVersion(2);     // select GLES 2.0
        }


        messagetxt =  findViewById(R.id.message) ;
        send = findViewById(R.id.send);

        try {
            //Set up connection parameters
            String url = "http://saws-api-dev.herokuapp.com/chat";

            // Get stored credentials
            JSONObject credentials = authManager.getStoredCredentials();
            JSONObject payload = new JSONObject();
            payload.put("stream", globalUsername);
            payload.put("username", credentials.getString("username"));
            payload.put("certificate", credentials.getString("certificate").replace('+','#'));

            IO.Options mOptions = new IO.Options();

            RSAPrivateKey privateKey = CryptoManager.getPrivateKeyFromString(credentials.getString("privateKey"));
            String correctedPayload = payload.toString().replace("\\", "");
            String signature = CryptoManager.sign(privateKey, correctedPayload);

            mOptions.query = "stream=" + globalUsername + "&username=" + credentials.getString("username") + "&certificate=" + credentials.getString("certificate").replace('+','#') + "&signature=" + signature.replace('+','#');

            //Create the socket with these parameters
            mSocket = IO.socket(url, mOptions);
        } catch (Exception e) {
            System.out.println(e);
        }
        mSocket.connect();

        MessageIOList = new ArrayList<>();
        myRecyclerView = findViewById(R.id.messagelist);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        myRecyclerView.setLayoutManager(mLayoutManager);
        myRecyclerView.setItemAnimator(new DefaultItemAnimator());



        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!messagetxt.getText().toString().isEmpty()){
                    try {
                        JSONObject credentials = authManager.getStoredCredentials();
                        JSONObject chatMessage = new JSONObject();
                        chatMessage.put("username", credentials.getString("username"));
                        chatMessage.put("message", messagetxt.getText().toString());

                        mSocket.emit("new-message", CryptoManager.createResponseBody(chatMessage, CryptoManager.getPrivateKeyFromString(credentials.getString("privateKey"))));

                        messagetxt.setText(" ");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (SignatureException e) {
                        e.printStackTrace();
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mSocket.on("MESSAGE", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                            if(CryptoManager.verifySignature(data)) {
                                JSONObject payload = data.getJSONObject("payload");

                                String username = payload.getString("username");
                                String message = payload.getString("message");

                                if(!userColors.containsKey(username)) {
                                    Random rnd = new Random();
                                    int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

                                    userColors.put(username, color);
                                }

                                MessageIO m = new MessageIO(username, message, userColors.get(username));
                                MessageIOList.add(m);
                                chatBoxAdapter = new ChatBoxAdapter(MessageIOList);
                                chatBoxAdapter.notifyDataSetChanged();
                                myRecyclerView.setAdapter(chatBoxAdapter);
                                myRecyclerView.scrollToPosition(chatBoxAdapter.getItemCount() -1);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (GeneralSecurityException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        //Chat button - open chat activity
        Button toggleChatButton = findViewById(R.id.toggle_chat);
        toggleChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!chatView.isShown()) {
                    chatView.setVisibility(View.VISIBLE);
                } else {
                    chatView.setVisibility(View.GONE);
                }
            }
        });

        toggleProfileButton = findViewById(R.id.toggle_profile);
        toggleProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(LiveVideoBroadcasterActivity.this)
                        .setTitle("Move to profile")
                        .setMessage("Do you really want to go to profile?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                intent.putExtra("username", globalUsername);
                                intent.putExtra("UUID", UUID);
                                startActivity(intent);
                            }})
                        .setNegativeButton("No", null).show();
            }
        });
    }

    public void changeCamera(View v) {
        if (mLiveVideoBroadcaster != null) {
            mLiveVideoBroadcaster.changeCamera();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //this lets activity bind
        bindService(mLiveVideoBroadcasterServiceIntent, mConnection, 0);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LiveVideoBroadcaster.PERMISSIONS_REQUEST: {
                if (mLiveVideoBroadcaster.isPermissionGranted()) {
                    mLiveVideoBroadcaster.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
                }
                else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CAMERA) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.RECORD_AUDIO) ) {
                        mLiveVideoBroadcaster.requestPermission();
                    }
                    else {
                        new AlertDialog.Builder(LiveVideoBroadcasterActivity.this)
                                .setTitle(R.string.permission)
                                .setMessage(getString(R.string.app_doesnot_work_without_permissions))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        try {
                                            //Open the specific App Info page:
                                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                            startActivity(intent);

                                        } catch ( ActivityNotFoundException e ) {
                                            //e.printStackTrace();

                                            //Open the generic Apps page:
                                            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                            startActivity(intent);

                                        }
                                    }
                                })
                                .show();
                    }
                }
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");

        //hide dialog if visible not to create leaked window exception
        if (mCameraResolutionsDialog != null && mCameraResolutionsDialog.isVisible()) {
            mCameraResolutionsDialog.dismiss();
        }
        try {
            stopStreamRequest();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        triggerStopRecording();
        mLiveVideoBroadcaster.pause();
    }


    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mLiveVideoBroadcaster.setDisplayOrientation();
        }

    }

    public void showSetResolutionDialog(View v) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment fragmentDialog = getSupportFragmentManager().findFragmentByTag("dialog");
        if (fragmentDialog != null) {

            ft.remove(fragmentDialog);
        }

        ArrayList<Resolution> sizeList = mLiveVideoBroadcaster.getPreviewSizeList();


        if (sizeList != null && sizeList.size() > 0) {
            mCameraResolutionsDialog = new CameraResolutionsFragment();

            mCameraResolutionsDialog.setCameraResolutions(sizeList, mLiveVideoBroadcaster.getPreviewSize());
            mCameraResolutionsDialog.show(ft, "resolutiton_dialog");
        }
        else {
            Snackbar.make(mRootView, "No resolution available",Snackbar.LENGTH_LONG).show();
        }

    }

    @SuppressLint("StaticFieldLeak")
    public void toggleBroadcasting(View v) throws JSONException, IOException, GeneralSecurityException {
        if (!mIsRecording)
        {
            if (mLiveVideoBroadcaster != null) {
                if (!mLiveVideoBroadcaster.isConnected()) {

                    startStreamRequest();

                    new AsyncTask<String, String, Boolean>() {
                        ContentLoadingProgressBar
                                progressBar;
                        @Override
                        protected void onPreExecute() {
                            progressBar = new ContentLoadingProgressBar(LiveVideoBroadcasterActivity.this);
                            progressBar.show();
                        }

                        @Override
                        protected Boolean doInBackground(String... url) {
                            return mLiveVideoBroadcaster.startBroadcasting(url[0]);

                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            progressBar.hide();
                            mIsRecording = result;
                            if (result) {
                                mStreamLiveStatus.setVisibility(View.VISIBLE);

                                mBroadcastControlButton.setText(R.string.stop_broadcasting);
                                mSettingsButton.setVisibility(View.GONE);
                                toggleProfileButton.setVisibility(View.GONE);
                                startTimer();//start the recording duration
                            }
                            else {
                                Snackbar.make(mRootView, R.string.stream_not_started, Snackbar.LENGTH_LONG).show();

                                try {
                                    stopStreamRequest();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (GeneralSecurityException e) {
                                    e.printStackTrace();
                                }
                                triggerStopRecording();
                                toggleProfileButton.setVisibility(View.VISIBLE);
                            }
                        }
                    }.execute(RTMP_BASE_URL + globalUsername.toLowerCase());
                }
                else {
                    Snackbar.make(mRootView, R.string.streaming_not_finished, Snackbar.LENGTH_LONG).show();
                }
            }
            else {
                Snackbar.make(mRootView, R.string.oopps_shouldnt_happen, Snackbar.LENGTH_LONG).show();
            }
        }
        else
        {
            stopStreamRequest();
            triggerStopRecording();
            toggleProfileButton.setVisibility(View.VISIBLE);
        }

    }


    public void triggerStopRecording() {
        if (mIsRecording) {
            mBroadcastControlButton.setText(R.string.start_broadcasting);

            mStreamLiveStatus.setVisibility(View.GONE);
            mStreamLiveStatus.setText(R.string.live_indicator);
            mSettingsButton.setVisibility(View.VISIBLE);

            stopTimer();
            mLiveVideoBroadcaster.stopBroadcasting();
        }

        mIsRecording = false;
    }

    //This method starts a mTimer and updates the textview to show elapsed time for recording
    public void startTimer() {

        if(mTimer == null) {
            mTimer = new Timer();
        }

        mElapsedTime = 0;
        mTimer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                mElapsedTime += 1; //increase every sec
                mTimerHandler.obtainMessage(TimerHandler.INCREASE_TIMER).sendToTarget();

                if (mLiveVideoBroadcaster == null || !mLiveVideoBroadcaster.isConnected()) {
                    mTimerHandler.obtainMessage(TimerHandler.CONNECTION_LOST).sendToTarget();
                }
            }
        }, 0, 1000);
    }


    public void stopTimer()
    {
        if (mTimer != null) {
            this.mTimer.cancel();
        }
        this.mTimer = null;
        this.mElapsedTime = 0;
    }

    public void setResolution(Resolution size) {
        mLiveVideoBroadcaster.setResolution(size);
    }

    private class TimerHandler extends Handler {
        static final int CONNECTION_LOST = 2;
        static final int INCREASE_TIMER = 1;

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INCREASE_TIMER:
                    mStreamLiveStatus.setText(getString(R.string.live_indicator) + " - " + getDurationString((int) mElapsedTime));
                    break;
                case CONNECTION_LOST:
                    triggerStopRecording();
                    new AlertDialog.Builder(LiveVideoBroadcasterActivity.this)
                            .setMessage(R.string.broadcast_connection_lost)
                            .setPositiveButton(android.R.string.yes, null)
                            .show();

                    break;
            }
        }
    }

    public void startStreamRequest() throws JSONException, IOException, GeneralSecurityException {
        // Create new request
        String url = "http://saws-api-dev.herokuapp.com/api/stream";
        final JSONObject credentials = authManager.getStoredCredentials();
        JSONObject payload = new JSONObject();
        payload.put("uuid", getUUID());
        JSONObject body = CryptoManager.createResponseBody(payload, CryptoManager.getPrivateKeyFromString(credentials.getString("privateKey")));

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {}
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(LiveVideoBroadcasterActivity.this, "error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", authManager.getToken());
                return headers;
            }
        };
        /*JsonObjectRequest MyStringRequest = new JsonObjectRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    if (response == null || response.equals("null")) {
                        Toast.makeText(LiveVideoBroadcasterActivity.this, "values are not correct", Toast.LENGTH_LONG).show();
                    } else {
                        JSONObject obj = new JSONObject(response);
                    }
                } catch (Exception e) {
                    Toast.makeText(LiveVideoBroadcasterActivity.this, "error: " + e, Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(LiveVideoBroadcasterActivity.this, "error: " + error, Toast.LENGTH_LONG).show();
            }
        }) {
            protected Map<String, String> getParams() {
                Map<String, String> MyData = new HashMap<String, String>();
                MyData.put("uuid", UUID);
                return MyData;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", authManager.getToken());
                return headers;
            }
        };*/

        this.requestQueue.add(request);
    }

    public void stopStreamRequest() throws JSONException, IOException, GeneralSecurityException {
        // Create new request
        String url = "http://saws-api-dev.herokuapp.com/api/stream";
        final JSONObject credentials = authManager.getStoredCredentials();
        JSONObject payload = new JSONObject();
        payload.put("uuid", getUUID());
        JSONObject body = CryptoManager.createResponseBody(payload, CryptoManager.getPrivateKeyFromString(credentials.getString("privateKey")));

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {}
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(LiveVideoBroadcasterActivity.this, "error: " + error, Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-access-token", authManager.getToken());
                return headers;
            }
        };
        /*StringRequest MyStringRequest = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    if (response == null || response.equals("null")) {
                        Toast.makeText(LiveVideoBroadcasterActivity.this, "values are not correct", Toast.LENGTH_LONG).show();
                    } else {
                        JSONObject obj = new JSONObject(response);
                    }
                } catch (Exception e) {
                    Toast.makeText(LiveVideoBroadcasterActivity.this, "error: " + e, Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(LiveVideoBroadcasterActivity.this, "error: " + error, Toast.LENGTH_LONG).show();
            }
        }) {
            protected Map<String, String> getParams() {
                Map<String, String> MyData = new HashMap<String, String>();
                MyData.put("uuid", UUID);
                return MyData;
            }
        };*/

        this.requestQueue.add(request);
    }

    public static String getDurationString(int seconds) {

        if(seconds < 0 || seconds > 2000000)//there is an codec problem and duration is not set correctly,so display meaningfull string
            seconds = 0;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        if(hours == 0)
            return twoDigitString(minutes) + " : " + twoDigitString(seconds);
        else
            return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(seconds);
    }

    public static String twoDigitString(int number) {

        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }

    @Override
    public void onBackPressed() {
        if (chatView.isShown()) {
            chatView.setVisibility(View.GONE);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Close app")
                    .setMessage("Do you really want to close the app and stream?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            triggerStopRecording();
                            try {
                                stopStreamRequest();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (GeneralSecurityException e) {
                                e.printStackTrace();
                            }
                            System.exit(0);
                            finish();

                        }})
                    .setNegativeButton("No", null).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
    }

    @SuppressLint("HardwareIds")
    public String getUUID() {
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;

        if (ContextCompat.checkSelfPermission(LiveVideoBroadcasterActivity.this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(LiveVideoBroadcasterActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_READ_STATE);

        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), ANDROID_ID);

        java.util.UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        return deviceUuid.toString();
    }
}
