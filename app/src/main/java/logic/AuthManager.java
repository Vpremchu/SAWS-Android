/*
    LoginManager.java - Manager for login functionality
 */
package logic;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Base64;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;

import domain.AuthResult;
import domain.LoginResult;
import domain.OnAuthListener;
import domain.OnLoginListener;

public class AuthManager {

    private SharedPreferences sharedPreferences;
    private OnAuthListener onAuthListener = null;
    private String username = null;
    private String password = null;

    public AuthManager(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean hasLocalCertificate() {
        if (sharedPreferences.contains("certificate")) {
            return true;
        } else {
            return false;
        }
    }

    public AuthManager setAuthDetails(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    public AuthManager setOnAuthListener(OnAuthListener onAuthListener) {
        this.onAuthListener = onAuthListener;
        return this;
    }

    public void authenticate() {
        if (this.onAuthListener != null) {
            if (this.username != null & this.password != null) {
                AuthTask task = new AuthTask(this);
                task.execute(this.username, this.password);
            } else {
                throw new NullPointerException("No auth details defined.");
            }
        } else {
            throw new NullPointerException("No callback method defined.");
        }
    }

    private static class AuthTask extends AsyncTask<String, Void, AuthResult> {

        private WeakReference<AuthManager> manager;

        AuthTask(AuthManager manager) {
            this.manager = new WeakReference<>(manager);


        }

        @Override
        protected AuthResult doInBackground(String... strings) {
            String username = strings[0];
            String password = strings[1];

            AuthResult result = null;
            HttpURLConnection connection = null;
            DataOutputStream output = null;

            try {
                URL url = new URL("http://saws-api.herokuapp.com/api/auth");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoInput(true);
                connection.setDoOutput(true);

                byte[] key = CryptoManager.generateAESKey();
                byte[] iv = CryptoManager.generateAESIV();
                String UUID = "UUID"; //TODO remove - serverside

                JSONObject body = new JSONObject();
                JSONObject payload = new JSONObject();
                RSAPublicKey publicKey = CryptoManager.getPublicKeyFromString(CryptoManager.PUBLIC_KEY_PEM);
                payload.put("username", CryptoManager.encrypt(username, publicKey));
                payload.put("password", CryptoManager.encrypt(password, publicKey));
                payload.put("uuid", CryptoManager.encrypt(UUID, publicKey));
                payload.put("key", CryptoManager.encrypt(key, publicKey));
                payload.put("iv", CryptoManager.encrypt(iv, publicKey));
                body.put("payload", payload);

                System.out.println(Base64.encodeToString(key, Base64.NO_WRAP));  //TODO REMOVE

                System.out.println(payload.toString()); //TODO REMOVE
                String correctedPayload = payload.toString().replace("\\", "");
                System.out.println("CORRECTED PAYLOAD " + correctedPayload);

                String signature = CryptoManager.createHMAC(correctedPayload.trim(), key);
                body.put("signature", signature);

                System.out.println(signature); //TODO REMOVE

                output = new DataOutputStream(connection.getOutputStream());
                output.writeBytes(body.toString());
                output.flush();

                result = new AuthResult(connection.getResponseCode());
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String response = Utils.getStringFromInputStream(connection.getInputStream());

                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject jsonPayload = jsonObject.getJSONObject("payload");
                    String encryptedCertificate = jsonPayload.getString("certificate");
                    String encryptedPrivateKey = jsonPayload.getString("privateKey");
                    String encryptedPublicKey = jsonPayload.getString("publicKey");
                    //result.setToken(token).setUsername(username);
                    connection.getInputStream().close();
                } else {
                    String response = Utils.getStringFromInputStream(connection.getErrorStream());

                    JSONObject jsonObject = new JSONObject(response);
                    String message = jsonObject.getString("message");
                    System.out.println(message); // TODO Remove
                    result.setMessage(message);
                    connection.getErrorStream().close();
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (output != null) {
                    try {
                        output.close();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(AuthResult authResult) {
            if (this.manager.get() != null) {
                if (authResult != null) {
                    if (authResult.getCode() == 200) {
                        System.out.println("noice");
                        this.manager.get().onAuthListener.onSuccess(authResult.getCertificate(), authResult.getPrivateKey(), authResult.getPublicKey());
                    } else {
                        this.manager.get().onAuthListener.onFailure(authResult.getCode(), authResult.getMessage());
                    }
                } else {
                    this.manager.get().onAuthListener.onFailure(500, "Er is een interne fout opgetreden. Probeer het later opnieuw.");
                }
            }
        }
    }
}

