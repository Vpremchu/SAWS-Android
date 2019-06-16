package presentation;

import android.content.Intent;
import android.support.v4.app.Fragment;

import io.antmedia.android.broadcaster.LiveVideoBroadcaster;

public class BroadcasterFragment extends Fragment {

    public void stratIntent() {
        Intent intent = new Intent(getActivity(), LiveVideoBroadcaster.class);
        startActivity(intent);
    }
}
