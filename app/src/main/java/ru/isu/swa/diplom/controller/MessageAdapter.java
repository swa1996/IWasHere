package ru.isu.swa.diplom.controller;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import ru.isu.swa.diplom.R;
import ru.isu.swa.diplom.model.Group;
import ru.isu.swa.diplom.model.User;
import ru.isu.swa.diplom.view.ChatActivity;

//Отображение списка собеседников в Сообщениях - адаптер для RecyclerView
// При нажатии на собеседника - переход в соответствующий чат.
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>  {
    private List<Object> authors;
    private List<String> messages;
    private int row;
    private Context context;

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout mainLayout;
        TextView author;
        TextView message;

        public MessageViewHolder(View v) {
            super(v);
            mainLayout = (LinearLayout) v.findViewById(R.id.messageLinearLayout);
            author = (TextView) v.findViewById(R.id.authorMessage);
            message = (TextView) v.findViewById(R.id.messageText);
        }
    }

    public MessageAdapter(List<Object> authors, List<String> mes, int rowLayout, Context context) {
        this.authors = authors;
        this.messages = mes;
        this.row = rowLayout;
        this.context = context;
    }

    @Override
    public MessageAdapter.MessageViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(row, parent, false);
        return new MessageAdapter.MessageViewHolder(view);
    }


    @Override
    public void onBindViewHolder(final MessageAdapter.MessageViewHolder holder, final int position) {
        final Object author = authors.get(position);
        final String message = messages.get(position);
        if(author.getClass().equals(User.class)){
            User user = (User) author;
            holder.author.setText(user.getFirstName()+" "+user.getLastName());
        }
        if(author.getClass().equals(Group.class)){
            Group group = (Group) author;
            holder.author.setText(group.getName());
        }
        holder.message.setText(message);

        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ChatActivity.class);
                        if(author.getClass().equals(User.class)){
                            intent.putExtra("sub", "user");
                            intent.putExtra("sub_id", ((User) author).getId());
                        }
                        if(author.getClass().equals(Group.class)){
                            intent.putExtra("sub", "group");
                            intent.putExtra("sub_id", ((Group) author).getId());
                        }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        if (authors == null) return 0;
        return authors.size();
    }
}
