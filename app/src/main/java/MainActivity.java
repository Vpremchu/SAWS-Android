
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import liveVideoBroadcaster.LiveVideoBroadcasterActivity;
import liveVideoPlayer.LiveVideoPlayerActivity;

import liveVideoBroadcaster.R;

public class MainActivity extends AppCompatActivity {

    public static final String RTMP_BASE_URL = "rtmp://141.138.142.48/live/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openVideoBroadcaster(View view) {
        Intent i = new Intent(this, LiveVideoBroadcasterActivity.class);
        startActivity(i);
    }

    public void openVideoPlayer(View view) {
        Intent i = new Intent(this, LiveVideoPlayerActivity.class);
        startActivity(i);
    }
}
