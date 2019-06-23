package domain;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class RequestSingleton {
    private static RequestSingleton ourInstance;
    private RequestQueue requestQueue;

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public static RequestSingleton getInstance(Context context) {
        if(ourInstance == null) {
            ourInstance = new RequestSingleton(context);
        }
        return ourInstance;
    }

    private RequestSingleton(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public<T> void addToRequestQueue(Request<T> request){
        // Add the specified request to the request queue
        getRequestQueue().add(request);
    }
}
