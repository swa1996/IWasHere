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
import ru.isu.swa.diplom.view.GroupActivity;

//отображение списка групп - адаптер для RecyclerView
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {
    private List<Group> groups;
    private int row;
    private Context context;

    @Override
    public GroupAdapter.GroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(row, parent, false);
        return new GroupAdapter.GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GroupAdapter.GroupViewHolder holder, int position) {
        final Group group = groups.get(position);
        holder.name.setText(group.getName());
        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, GroupActivity.class);
                intent.putExtra("group_id", group.getId());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        if(groups==null)return 0;
        else return groups.size();
    }


    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        LinearLayout mainLayout;
        TextView name;
        TextView info;

        public GroupViewHolder(View v) {
            super(v);
            mainLayout = (LinearLayout) v.findViewById(R.id.groupLinearLayout);
            name = (TextView) v.findViewById(R.id.groupName);
            info = (TextView) v.findViewById(R.id.groupInfo);
        }
    }

    public GroupAdapter(List<Group> groups, int row, Context context) {
        this.groups = groups;
        this.row = row;
        this.context = context;
    }
}
