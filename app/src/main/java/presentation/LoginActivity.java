/*
    LoginActivity.java - Login page functionality
 */
package presentation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import domain.OnLoginListener;
import liveVideoBroadcaster.LiveVideoBroadcasterActivity;
import liveVideoBroadcaster.R;

import static android.provider.Settings.Secure.ANDROID_ID;

public class LoginActivity extends AppCompatActivity {

    private static final int PERMISSION_READ_STATE = 0;
    private OnLoginListener onLoginListener;
    private String globalUsername;
    private String password;
    private String UUID;
    private RequestQueue queue = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        //Change the action bar name
        try {
            getSupportActionBar().setTitle("Seechange Login");
        } catch (Exception e) {
            System.out.println(e);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null && !extras.isEmpty()) {
            UUID = extras.getString("UUID");
        }

        UUID = getUUID();

        this.queue = startQueue();

        getUserByUUID();

        final EditText usernameEditText = findViewById(R.id.username);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                globalUsername = usernameEditText.getText().toString().trim();
                password = passwordEditText.getText().toString();

                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                Objects.requireNonNull(inputManager).hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                LoginUserWithUUID(globalUsername, password);
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

    @SuppressLint("HardwareIds")
    public String getUUID() {
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;

        if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_READ_STATE);

        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        return deviceUuid.toString();
    }

    public void getUserByUUID() {
        // Create new request
        String url = "http://saws-api.herokuapp.com/api/usersuuid";
        StringRequest MyStringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    if (response == null || response.equals("null")) {
                        Toast.makeText(LoginActivity.this, "User not known, please login using credentials", Toast.LENGTH_LONG).show();
                    } else {
                        JSONObject obj = new JSONObject(response);
                        globalUsername = obj.getString("username");
                        loginUser(obj.getString("username"), obj.getString("password"));
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "error: " + e, Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(LoginActivity.this, "error: " + error, Toast.LENGTH_LONG).show();
            }
        }) {
            protected Map<String, String> getParams() {
                Map<String, String> MyData = new HashMap<String, String>();
                MyData.put("uuid", UUID);
                return MyData;
            }
        };

        this.queue.add(MyStringRequest);
    }

    public void loginUser(final String username, final String password) {
        // Create new request
        String url = "http://saws-api.herokuapp.com/api/loginhash";
        StringRequest MyStringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    if (response == null || response.equals("null")) {
                        Toast.makeText(LoginActivity.this, "User not known, please login using credentials", Toast.LENGTH_LONG).show();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), LiveVideoBroadcasterActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("UUID", UUID);
                        intent.putExtra("username", username);
                        startActivity(intent);
                        Toast.makeText(getApplicationContext(), "Welkom, " + username, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "error: " + e, Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(LoginActivity.this, "error: " + error, Toast.LENGTH_LONG).show();
            }
        }) {
            protected Map<String, String> getParams() {
                Map<String, String> MyData = new HashMap<String, String>();
                MyData.put("username", username);
                MyData.put("password", password);
                return MyData;
            }
        };

        this.queue.add(MyStringRequest);
    }

    public void LoginUserWithUUID(final String name, final String pass) {
        String url = "http://saws-api.herokuapp.com/api/loginuuid";
        StringRequest MyStringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    if (response == null || response.equals("null")) {
                        Toast.makeText(LoginActivity.this, "User not known, please check the credentials", Toast.LENGTH_LONG).show();
                    } else {
                        String username = obj.getString("username");
                        globalUsername = username;
                        Toast.makeText(getApplicationContext(), "Welkom, " + username, Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(getApplicationContext(), LiveVideoBroadcasterActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("uuid", UUID);
                        intent.putExtra("username", globalUsername);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "error: " + e, Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(LoginActivity.this, "error: " + error, Toast.LENGTH_LONG).show();
            }
        }) {
            protected Map<String, String> getParams() {
                Map<String, String> MyData = new HashMap<String, String>();
                MyData.put("username", name);
                MyData.put("password", pass);
                MyData.put("uuid", UUID);
                return MyData;
            }
        };
        this.queue.add(MyStringRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_READ_STATE)
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(LoginActivity.this, "Permission denied to read your Phone state", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(LoginActivity.this, "Permission given!", Toast.LENGTH_LONG).show();
            }
    }
}

