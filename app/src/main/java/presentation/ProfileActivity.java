package presentation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import domain.Profile;
import domain.ProfileListener;
import liveVideoBroadcaster.LiveVideoBroadcasterActivity;
import liveVideoBroadcaster.R;
import logic.ImageLoader;

import static android.provider.Settings.Secure.ANDROID_ID;

public class ProfileActivity extends AppCompatActivity {
    private RequestQueue queue;
    private ProfileListener listener;
    private Profile profile;
    private String userName;
    private String imageUrl;
    private int satoshiCount;
    private TextView userNameView;
    private TextView satoshiCountView;
    private ImageView imageUrlView;
    private String globalUsername;
    private String UUID;
    private static final int PERMISSION_READ_STATE = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        Bundle extras = getIntent().getExtras();
        if (extras != null || !extras.isEmpty()) {
            globalUsername = extras.getString("username");
        }

        UUID = getUUID();

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        this.queue = startQueue();
        getProfile();

        userNameView = findViewById(R.id.userName);
        satoshiCountView = findViewById(R.id.satoshiCount);
        imageUrlView = findViewById(R.id.avatar);

        Button backButton = findViewById(R.id.backToStream);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LiveVideoBroadcasterActivity.class);
                intent.putExtra("UUID", UUID);
                intent.putExtra("username", globalUsername);
                startActivity(intent);
            }
        });

        Button logoutButton = findViewById(R.id.removeAccount);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logoutUser();
            }
        });
    }

    public RequestQueue startQueue() {
        RequestQueue requestQueue;
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);
        Network network = new BasicNetwork(new HurlStack());
        requestQueue = new RequestQueue(cache, network);
        requestQueue.start();
        return requestQueue;
    }

    public void getProfile() {
        String url = "http://saws-api.herokuapp.com/api/user?username=" + globalUsername;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    userName = obj.getString("firstname") + " " +  obj.getString("lastname");
                    satoshiCount = obj.getInt("satoshiAmount");
                    imageUrl = obj.getString("iconurl");

                    profile = new Profile(userName, imageUrl, satoshiCount);

                    System.out.println(profile);
                    System.out.println(userName+satoshiCount+imageUrl);

                    userNameView.setText("Name: \n" + userName);
                    satoshiCountView.setText("Satochi Amount: \n" + Integer.toString(satoshiCount));
                    new ImageLoader(imageUrlView).execute(imageUrl);

                    //listener.onProfileListener(profile);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void logoutUser() {
        String url = "http://saws-api.herokuapp.com/api/user";
        StringRequest MyStringRequest = new StringRequest(Request.Method.PUT, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    if (response == null || response.equals("null")) {
                        Toast.makeText(ProfileActivity.this, "Could not logout!", Toast.LENGTH_LONG).show();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        intent.putExtra("UUID", UUID);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Toast.makeText(ProfileActivity.this, "error: " + e, Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(ProfileActivity.this, "error: " + error, Toast.LENGTH_LONG).show();
            }
        }) {
            protected Map<String, String> getParams() {
                Map<String, String> MyData = new HashMap<String, String>();
                MyData.put("uuid", UUID);
                return MyData;
            }
        };

        queue.add(MyStringRequest);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), LiveVideoBroadcasterActivity.class);
        intent.putExtra("UUID", UUID);
        intent.putExtra("username", globalUsername);
        startActivity(intent);
    }



    @SuppressLint("HardwareIds")
    public String getUUID() {
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;

        if (ContextCompat.checkSelfPermission(ProfileActivity.this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(ProfileActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_READ_STATE);

        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), ANDROID_ID);

        java.util.UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        return deviceUuid.toString();
    }
}
