/*
    LoginManager.java - Manager for login functionality
 */
package logic;

import android.content.SharedPreferences;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import domain.AuthResult;
import domain.LoginResult;
import domain.OnAuthListener;
import domain.OnLoginListener;

public class AuthManager {

    private SharedPreferences sharedPreferences;
    private OnAuthListener onAuthListener = null;
    private String username = null;
    private String password = null;

    AuthManager(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
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
                URL url = new URL("https://saws-api.herokuapp.com/api/auth");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(2000);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoInput(true);
                connection.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("username", username);
                json.put("password", password);

                output = new DataOutputStream(connection.getOutputStream());
                output.writeBytes(json.toString());
                output.flush();

                result = new AuthResult(connection.getResponseCode());
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String response = Utils.getStringFromInputStream(connection.getInputStream());

                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject jsonPayload = jsonObject.getJSONObject("payload");
                    String encryptedCertificate = jsonPayload.getString("certificate");
                    String encryptedPrivateKey = jsonPayload.getString("privateKey");
                    String encryptedPublicKey = jsonPayload.getString("publicKey");
                    String token = jsonObject.getString("token");
                    //result.setToken(token).setUsername(username);
                    connection.getInputStream().close();
                } else {
                    String response = Utils.getStringFromInputStream(connection.getErrorStream());

                    JSONObject jsonObject = new JSONObject(response);
                    String message = jsonObject.getString("message");
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
            if(this.manager.get() != null){
                if (authResult != null) {
                    if(authResult.getCode() == 200){
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

