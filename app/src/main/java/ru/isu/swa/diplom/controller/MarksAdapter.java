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
import ru.isu.swa.diplom.model.Mark;
import ru.isu.swa.diplom.view.MarkActivity;

/**
 * Created by swa on 05.03.2018.
 */

//отображение списка меток на главном Activity - адаптер для RecyclerView
public class MarksAdapter extends RecyclerView.Adapter<MarksAdapter.MarkViewHolder> {
    private List<Mark> marks;
    private int row;
    private Context context;


    public static class MarkViewHolder extends RecyclerView.ViewHolder {
        LinearLayout mainLayout;
        TextView author;
        TextView message;


        public MarkViewHolder(View v) {
            super(v);
            mainLayout = (LinearLayout) v.findViewById(R.id.mainLinearLayout);
            author = (TextView) v.findViewById(R.id.author);
            message = (TextView) v.findViewById(R.id.message);
        }
    }

    public MarksAdapter(List<Mark> marks, int rowLayout, Context context) {
        this.marks = marks;
        this.row = rowLayout;
        this.context = context;
    }

    @Override
    public MarksAdapter.MarkViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(row, parent, false);
        return new MarkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MarkViewHolder holder, final int position) {
        final Mark mark = marks.get(position);
        if(mark.isEncrypted()){
         holder.message.setTextColor(context.getResources().getColor(R.color.md_blue_grey_300));
         holder.message.setText("Нажмите, чтобы прочитать");
        }else {
            holder.message.setText(mark.getMessage());
        }
        if (mark.getAuthor() == null) {
            holder.author.setText("Аноним");
        } else {
            holder.author.setText(mark.getAuthor().getFirstName() + " " + mark.getAuthor().getLastName());
        }

        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MarkActivity.class);
                intent.putExtra("mark_id", mark.getId());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        if (marks == null) return 0;
        return marks.size();
    }
}