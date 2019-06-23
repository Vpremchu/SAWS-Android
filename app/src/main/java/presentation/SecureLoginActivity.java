package presentation;


import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.net.CookieManager;
import java.net.CookieStore;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import domain.RequestSingleton;
import liveVideoBroadcaster.LiveVideoBroadcasterActivity;
import liveVideoBroadcaster.R;
import logic.AuthManager;
import logic.CryptoManager;

import static android.provider.Settings.Secure.ANDROID_ID;

public class SecureLoginActivity extends AppCompatActivity {

    private static final int PERMISSION_READ_STATE = 0;
    private TextView loadingLabel;
    private ImageView imageCircleView;
    private Button authButton;

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secure_login);

        System.out.println(getUUID());
        SharedPreferences sharedPreferences = getSharedPreferences("localStorage", MODE_PRIVATE);
        authManager = new AuthManager(sharedPreferences);

        imageCircleView = findViewById(R.id.imageView);
        authButton = findViewById(R.id.AuthButton);
        loadingLabel = findViewById(R.id.LoadingLabel);

        authManager.clearStoredCredentials();

        if(authManager.hasLocalCertificate()) {
            loadingLabel.setText("Logging in...");
            login();
        } else {
            animateViews();
            setupAuthenticate();
        }
    }

    private void setupAuthenticate() {
        authButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView usernameView = findViewById(R.id.UsernameTextView);
                TextView passwordView = findViewById(R.id.PasswordTextView);

                changeLoadingState("Authenticating device...", LoadingState.SHOW);

                final byte[] key = CryptoManager.generateAESKey();
                final byte[] iv = CryptoManager.generateAESIV();
                String UUID = getUUID();

                JSONObject body = new JSONObject();
                JSONObject payload = new JSONObject();
                try {
                    final String username = usernameView.getText().toString();
                    final String password = hashPassword(passwordView.getText().toString());

                    RSAPublicKey publicKey = CryptoManager.getPublicKeyFromString(CryptoManager.PUBLIC_KEY_PEM);
                    payload.put("username", CryptoManager.encrypt(username, publicKey));
                    payload.put("password", CryptoManager.encrypt(password, publicKey));
                    payload.put("uuid", CryptoManager.encrypt(UUID, publicKey));
                    payload.put("key", CryptoManager.encrypt(key, publicKey));
                    payload.put("iv", CryptoManager.encrypt(iv, publicKey));
                    body.put("payload", payload);
                    String correctedPayload = payload.toString().replace("\\", "");
                    String signature = CryptoManager.createHMAC(correctedPayload.trim(), key);
                    body.put("signature", signature);

                    JsonObjectRequest request = new JsonObjectRequest(
                            Request.Method.POST,
                            "http://saws-api-dev.herokuapp.com/api/auth",
                            body,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        if(CryptoManager.verifyHMAC(response, key)) {
                                            authManager.storeCredentials(username, response, key, iv);
                                            login();
                                        }
                                    } catch (NoSuchPaddingException e) {
                                        e.printStackTrace();
                                    } catch (InvalidKeyException e) {
                                        e.printStackTrace();
                                    } catch (NoSuchAlgorithmException e) {
                                        e.printStackTrace();
                                    } catch (IllegalBlockSizeException e) {
                                        e.printStackTrace();
                                    } catch (BadPaddingException e) {
                                        e.printStackTrace();
                                    } catch (InvalidAlgorithmParameterException e) {
                                        e.printStackTrace();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            changeLoadingState("", LoadingState.HIDE);
                            String response;
                            try {
                                JSONObject errorResponse = new JSONObject(new String(error.networkResponse.data));
                                System.out.println(errorResponse.get("message").toString());
                                response = "An error occurred during authentication";
                            } catch (Exception e) {
                                System.out.println(error.toString());
                                response = "A timeout occurred";
                            }
                            Toast.makeText(getApplicationContext(), response, Snackbar.LENGTH_LONG).show();
                        }
                    });
                    RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(request);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void login() {
        JSONObject payload = new JSONObject();
        try {
            final JSONObject credentials = authManager.getStoredCredentials();
            payload.put("username", credentials.getString("username"));
            payload.put("certificate", credentials.getString("certificate"));
            JSONObject body = CryptoManager.createResponseBody(payload, CryptoManager.getPrivateKeyFromString(credentials.getString("privateKey")));

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.PUT,
                    "http://saws-api-dev.herokuapp.com/api/auth",
                    body,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if(CryptoManager.verifySignature(response)) {
                                    authManager.storeToken(response);
                                    Intent intent = new Intent(getApplicationContext(), LiveVideoBroadcasterActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    intent.putExtra("UUID", getUUID());
                                    intent.putExtra("username", credentials.getString("username"));
                                    startActivity(intent);
                                    Toast.makeText(getApplicationContext(), "Welkom, " + credentials.getString("username"), Toast.LENGTH_SHORT).show();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (GeneralSecurityException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String response;
                    try {
                        JSONObject errorResponse = new JSONObject(new String(error.networkResponse.data));
                        System.out.println(errorResponse.get("message").toString());
                        response = "An error occurred during login";
                    } catch (Exception e) {
                        System.out.println(error.toString());
                        response = "A timeout occurred";
                    }
                    Toast.makeText(getApplicationContext(), response, Snackbar.LENGTH_LONG).show();
                }
            });
            RequestSingleton.getInstance(getApplicationContext()).addToRequestQueue(request);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void animateViews() {
        // Setup used arrays
        final ArrayList<View> viewsIn = new ArrayList<>();
        ArrayList<View> viewsOut = new ArrayList<>();

        // Add views for fade in animations
        viewsIn.add(findViewById(R.id.UsernameLabel));
        viewsIn.add(findViewById(R.id.PasswordLabel));
        viewsIn.add(findViewById(R.id.UsernameTextView));
        viewsIn.add(findViewById(R.id.PasswordTextView));
        viewsIn.add(authButton);

        // Add views for fade out animations
        viewsOut.add(findViewById(R.id.LoadingLabel));
        viewsOut.add(findViewById(R.id.progressBar));

        // Setup a move animation of the circle animation
        imageCircleView.animate().alpha(1.0f).setDuration(500).start();
        ObjectAnimator animator = ObjectAnimator.ofFloat(imageCircleView, "translationY", dpToPx(-180f, getApplicationContext()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Loop through all the views in the array and fade them in and move
                for(View v : viewsIn) {
                    // Move the view upwards
                    ObjectAnimator.ofFloat(v, "translationY", dpToPx(-180f, getApplicationContext())).setDuration(0).start();
                    // Fade them in
                    v.animate().alpha(1.0f).setDuration(500).start();
                }
                Toast.makeText(getApplicationContext(), "Device is not authenticated yet.\nPlease provide valid login details.", Toast.LENGTH_LONG).show();
            }
        });
        // Start the move animation
        animator.setDuration(750).start();

        // Loop through all the views in the array and fade them out
        for(View v : viewsOut) {
            v.animate().alpha(0.0f).setDuration(500).start();
        }
    }

    public void changeLoadingState(String label, LoadingState state) {
        // Setup used arrays
        final ArrayList<View> views = new ArrayList<>();
        float alpha;
        // Add views for fade animations
        views.add(findViewById(R.id.LoadingLabel));
        views.add(findViewById(R.id.progressBar));

        // Loop through all the views in the array and fade them out
        if(state == LoadingState.SHOW) {
            loadingLabel.setText(label);
            alpha = 1.0f;
        } else {
            alpha = 0.0f;
        }
        for(View v : views) {
            v.animate().alpha(alpha).setDuration(500).start();
        }

    }

    // Helper function to change DP
    public static int dpToPx(float dp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    // Helper function for hashing strings to SHA256
    private static String hashPassword(String payload) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance( "SHA-256" );
        // Change this to UTF-16 if needed
        md.update( payload.getBytes( StandardCharsets.UTF_8 ) );
        byte[] digest = md.digest();
        return String.format( "%064x", new BigInteger( 1, digest ) );
    }

    // Request UUID from device
    @SuppressLint("HardwareIds")
    public String getUUID() {
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;

        if (ContextCompat.checkSelfPermission(SecureLoginActivity.this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(SecureLoginActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_READ_STATE);

        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        return deviceUuid.toString();
    }

    private enum LoadingState {
        SHOW, HIDE
    }
}


