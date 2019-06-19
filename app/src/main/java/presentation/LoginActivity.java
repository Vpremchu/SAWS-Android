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
import android.util.Base64;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import domain.OnAuthListener;
import domain.OnLoginListener;
import liveVideoBroadcaster.LiveVideoBroadcasterActivity;
import liveVideoBroadcaster.R;
import logic.AuthManager;
import logic.CryptoManager;
import logic.LoginManager;

import static android.provider.Settings.Secure.ANDROID_ID;

public class LoginActivity extends AppCompatActivity {

    private static final int PERMISSION_READ_STATE = 0;
    private CryptoManager cryptoManager;
    private LoginManager loginManager;
    private AuthManager authManager;
    private OnLoginListener onLoginListener;
    private String globalUsername;
    private String password;
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

        this.authManager = new AuthManager(getSharedPreferences("localStorage", MODE_PRIVATE));
        this.authManager.setAuthDetails("Rick", "24e107dae7d9928f2f2dfdab627bb4abdee29bc4b1dc82cf67c0e5b8a0badffe");
        this.authManager.setOnAuthListener(new OnAuthListener() {
            @Override
            public void onSuccess(String certificate, String privateKey, String publicKey) {

            }

            @Override
            public void onFailure(int code, String message) {

            }
        });

        //TODO TEST
        byte[] key = Base64.decode("Fm/JVgmXDvR0yreNG4rFY0p1ft4JqGzsEO70M1+kDyg=", Base64.NO_WRAP);
        String testKey = "Fm/JVgmXDvR0yreNG4rFY0p1ft4JqGzsEO70M1+kDyg=";
        System.out.println((Base64.encodeToString(key, Base64.NO_WRAP)).equals(testKey));
        JSONObject test = new JSONObject();
        try {
            test.put("test", "aronboi");
            System.out.println(CryptoManager.createHMAC(test.toString(), key));
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        this.authManager.authenticate();

        this.queue = startQueue();

        //getUserByUUID(); TODO uncomment

        startLoginListener();

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
                MyData.put("uuid", getUUID());
                return MyData;
            }
        };

        this.queue.add(MyStringRequest);
    }

    public void loginUser(String username, String password) {
        loginManager
                .setLoginDetails(username, password)
                .setOnLoginListener(onLoginListener)
                .login();
    }

    public void startLoginListener() {
        this.loginManager = new LoginManager();
        this.onLoginListener = new OnLoginListener() {
            @Override
            public void onSuccess(String token, String username) {
                Intent intent = new Intent(getApplicationContext(), LiveVideoBroadcasterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("UUID", getUUID());
                intent.putExtra("username", globalUsername);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), "Welkom, " + username, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int code, String message) {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        };
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
                        intent.putExtra("UUID", getUUID());
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
                MyData.put("uuid", getUUID());
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
            }
    }
}

