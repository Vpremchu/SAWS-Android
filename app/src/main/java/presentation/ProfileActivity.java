package presentation;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import liveVideoBroadcaster.R;

public class ProfileActivity extends AppCompatActivity {
    private RequestQueue queue;
    private static final String TAG = ProfileActivity.class.getSimpleName();

    public RequestQueue startQueue() {
        RequestQueue requestQueue;
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);
        Network network = new BasicNetwork(new HurlStack());
        requestQueue = new RequestQueue(cache, network);
        requestQueue.start();
        return requestQueue;
    }

    public void getFollowersCount() {
        String url = "";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            JSONObject obj = new JSONObject(response);
                            int followersCount = obj.getInt("followers");
                            int satoshiCount = obj.getInt("satoshi");
                            String image = obj.getString("imageUrl");
                        }
                        catch(Exception e){
                            Log.e(TAG,"onPostExecute JSONException " + e.getLocalizedMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ProfileActivity.this, "error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
        this.queue.add(stringRequest);
    }
}
