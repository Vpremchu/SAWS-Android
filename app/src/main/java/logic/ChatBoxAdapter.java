package logic;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import domain.MessageIO;
import liveVideoBroadcaster.R;

public class ChatBoxAdapter  extends RecyclerView.Adapter<ChatBoxAdapter.MyViewHolder> {
    private List<MessageIO> messageIOList;

    public  class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView nickname;
        public TextView message;


        public MyViewHolder(View view) {
            super(view);

            nickname = (TextView) view.findViewById(R.id.username);
            message = (TextView) view.findViewById(R.id.message);
        }
    }
    public ChatBoxAdapter(List<MessageIO> messagesList) {

        this.messageIOList = messagesList;


    }

    @Override
    public int getItemCount() {
        return messageIOList.size();
    }
    @Override
    public ChatBoxAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item, parent, false);



        return new ChatBoxAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ChatBoxAdapter.MyViewHolder holder, final int position) {

        //binding the data from our ArrayList of object to the item.xml using the viewholder



        MessageIO m = messageIOList.get(position);
        holder.nickname.setText(m.getNickname());

        holder.message.setText(m.getMessage() );




    }



}
