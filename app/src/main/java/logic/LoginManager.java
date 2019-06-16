/*
    LoginManager.java - Manager for login functionality
 */
package logic;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import domain.LoginResult;
import domain.OnLoginListener;

public class LoginManager {

    private OnLoginListener onLoginListener = null;
    private String username = null;
    private String password = null;

    public LoginManager setLoginDetails(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    public LoginManager setOnLoginListener(OnLoginListener onLoginListener) {
        this.onLoginListener = onLoginListener;
        return this;
    }

    public void login() {
        if (this.onLoginListener != null) {
            if (this.username != null & this.password != null) {
                LoginTask task = new LoginTask(this);
                task.execute(this.username, this.password);
            } else {
                throw new NullPointerException("No login details defined.");
            }
        } else {
            throw new NullPointerException("No callback method defined.");
        }
    }

    private static class LoginTask extends AsyncTask<String, Void, LoginResult> {

        private WeakReference<LoginManager> manager;

        LoginTask(LoginManager manager) {
            this.manager = new WeakReference<>(manager);


        }

        @Override
        protected LoginResult doInBackground(String... strings) {
            String username = strings[0];
            String password = strings[1];

            LoginResult result = null;
            HttpURLConnection connection = null;
            DataOutputStream output = null;

            try {
                URL url = new URL("https://saws-api.herokuapp.com/api/login");
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
                json.put("uuid", "");

                output = new DataOutputStream(connection.getOutputStream());
                output.writeBytes(json.toString());
                output.flush();

                result = new LoginResult(connection.getResponseCode());
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String response = Utils.getStringFromInputStream(connection.getInputStream());

                    JSONObject jsonObject = new JSONObject(response);
                    String token = jsonObject.getString("token");
                    result.setToken(token).setUsername(username);
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
        protected void onPostExecute(LoginResult loginResult) {
            if(this.manager.get() != null){
                if (loginResult != null) {
                    if(loginResult.getCode() == 200){
                        this.manager.get().onLoginListener.onSuccess(loginResult.getToken(), loginResult.getUsername());
                    } else {
                        this.manager.get().onLoginListener.onFailure(loginResult.getCode(), loginResult.getMessage());
                    }
                } else {
                    this.manager.get().onLoginListener.onFailure(500, "Er is een interne fout opgetreden. Probeer het later opnieuw.");
                }
            }
        }
    }
}

