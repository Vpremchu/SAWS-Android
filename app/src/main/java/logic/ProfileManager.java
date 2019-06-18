package logic;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import domain.Profile;
import domain.ProfileListener;

public class ProfileManager extends AsyncTask<String, Void, String> {
    private ProfileListener listener;
    private static final String TAG = ProfileManager.class.getSimpleName();

    @Override
    protected String doInBackground(String... params) {
        Log.i("AsyncTask", "API ophalen.");
        BufferedReader bufferedReader = null;
        String response = "";

        try {
            URL url = new URL(params[0]);
            URLConnection connection = url.openConnection();

            bufferedReader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );
            response = bufferedReader.readLine().toString();
            String line;
            while( (line = bufferedReader.readLine()) != null) {
                response += line;
            }

        } catch (Exception e) {
            return null;
        } finally {
            if( bufferedReader != null ) {
                try {
                    bufferedReader.close();
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return response;
    }

    protected void onPostExecute(String response) {
        Log.i("AsyncTask", "JSON parsen");
        try {
            JSONObject obj = new JSONObject(response);
            String userName = obj.getString("firstName") + " " + obj.getString("lastName");
            int satoshiCount = obj.getInt("satoshi");
            String imageUrl = obj.getString("imageUrl");

            Profile profile = new Profile(userName, imageUrl, satoshiCount);

            this.listener.onProfileListener(profile);
        } catch(Exception e){
            Log.e(TAG,"onPostExecute JSONException " + e.getLocalizedMessage());
        }
    }
}
