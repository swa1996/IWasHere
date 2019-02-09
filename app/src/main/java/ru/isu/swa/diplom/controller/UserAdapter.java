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
import ru.isu.swa.diplom.model.User;
import ru.isu.swa.diplom.view.GroupActivity;
import ru.isu.swa.diplom.view.UserActivity;


//Отображение списка пользователей - адаптер для RecyclerView
//Используется в поиске юзеров и списке друзей.
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> users;
    private int row;
    private Context context;

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(row, parent, false);
        return new UserAdapter.UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        final User user = users.get(position);
        holder.name.setText(user.toString());
        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, UserActivity.class);
                intent.putExtra("user_id", user.getId());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
        if(context.getClass().equals(GroupActivity.class)) {
            holder.mainLayout.setTag(user);
            holder.mainLayout.setOnLongClickListener((GroupActivity)context);
        }
    }

    @Override
    public int getItemCount() {
        if(users==null)return 0;
        else return users.size();
    }


    public static class UserViewHolder extends RecyclerView.ViewHolder {
        LinearLayout mainLayout;
        TextView name;

        public UserViewHolder(View v) {
            super(v);
            mainLayout = (LinearLayout) v.findViewById(R.id.userLinearLayout);
            name = (TextView) v.findViewById(R.id.userName);
        }
    }

    public UserAdapter(List<User> users, int row, Context context) {
        this.users = users;
        this.row = row;
        this.context = context;
    }
}
