package presentation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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

import domain.Profile;
import domain.ProfileListener;
import liveVideoBroadcaster.LiveVideoBroadcasterActivity;
import liveVideoBroadcaster.R;
import logic.ImageLoader;

public class ProfileActivity extends AppCompatActivity {
    private RequestQueue queue;
    private ProfileListener listener;
    private Profile profile;
    private String userName;
    private String imageUrl;
    private int satoshiCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        this.queue = startQueue();
        getProfile();

        TextView userName = findViewById(R.id.userName);
        TextView satoshiCount = findViewById(R.id.satoshiCount);
        ImageView imageUrl = findViewById(R.id.avatar);

        userName.setText(profile.getUserName());
        satoshiCount.setText(profile.getSatoshiCount());
        new ImageLoader(imageUrl).execute(profile.getImageUrl());

        Button backButton = findViewById(R.id.backToStream);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LiveVideoBroadcasterActivity.class);
                startActivity(intent);
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
        String url = "http://saws-api.herokuapp.com/api/user=username?";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                JSONObject obj = null;
                try {
                    obj = new JSONObject(response);
                    String userName = obj.getString("firstname") + " " +  obj.getString("lastname");
                    int satoshiCount = obj.getInt("satoshiAmount");
                    String imageUrl = obj.getString("iconurl");

                    Profile profile = new Profile(userName, imageUrl, satoshiCount);

                    listener.onProfileListener(profile);
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
}
