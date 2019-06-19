package logic;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.MessageFormat;
import java.util.List;

import domain.MessageIO;
import liveVideoBroadcaster.R;


public class ChatBoxAdapter  extends RecyclerView.Adapter<ChatBoxAdapter.MyViewHolder> {
    private List<MessageIO> MessageIOList;

    public  class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView username;
        public TextView message;


        public MyViewHolder(View view) {
            super(view);

            username = view.findViewById(R.id.username);
            message = view.findViewById(R.id.message);
        }
    }
    public ChatBoxAdapter(List<MessageIO>MessagesList) {
        this.MessageIOList = MessagesList;
    }

    @Override
    public int getItemCount() {
        return MessageIOList.size();
    }

    @Override
    public ChatBoxAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item, parent, false);
        return new ChatBoxAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ChatBoxAdapter.MyViewHolder holder, final int position) {
        MessageIO m = MessageIOList.get(position);
        holder.username.setText(MessageFormat.format("{0} : ", m.getUsername()));
        holder.username.setTextColor(m.getColor());

        holder.message.setText(m.getMessage());

    }



}
