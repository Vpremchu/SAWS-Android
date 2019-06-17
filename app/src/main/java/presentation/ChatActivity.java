package presentation;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

import liveVideoBroadcaster.R;

public class ChatActivity extends AppCompatActivity {

    private Socket mSocket;
    private TextView chatView;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
            try {
                //Set up connection parameters
                String url = "https://saws-api.herokuapp.com/chat";
                IO.Options mOptions = new IO.Options();
                mOptions.query = "stream=" + "Rick"; //TODO - Replace static username

                //Create the socket with these parameters
                mSocket = IO.socket(url, mOptions);
            } catch (Exception e) {
                System.out.println(e);
            }
        //Tell the socket to listen for incoming messages
        mSocket.on("MESSAGE", onNewMessage);
        //Link scrollView to the corresponding ScrollView
        scrollView = findViewById(R.id.scrollView);
        //Link chatView with the corresponding TextView
        chatView = findViewById(R.id.chatView);
        //Connect with the socket and its given parameters
        mSocket.connect();
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    String message;
                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                        //Update chatview with incoming messages
                        chatView.append(username + ": " + message + '\n');
                        //Scroll down with the new messages
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    } catch (JSONException e) {
                        return;
                    }

                    // add the message to view
                    //addMessage(username, message);
                }
            });
        }
    };

}
