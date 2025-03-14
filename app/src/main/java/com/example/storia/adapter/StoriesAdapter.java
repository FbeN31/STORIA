package com.example.storia.adapter;

import static com.example.storia.MainActivity.calculateAge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.storia.utils.OnItemClick;
import com.example.storia.R;
import com.example.storia.model.UserModel;

import java.util.List;

public class StoriesAdapter extends RecyclerView.Adapter<StoriesAdapter.Vh> {
    Context context;
    List<UserModel> list;
    OnItemClick onItemClick;

    public StoriesAdapter(Context context, List<UserModel> list, OnItemClick click) {
        this.context = context;
        this.list = list;
        this.onItemClick = click;
    }

    @NonNull
    @Override
    public Vh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false);
        return new Vh(view);
    }

    @SuppressLint({"SetTextI18n", "SimpleDateFormat"})
    @Override
    public void onBindViewHolder(@NonNull Vh holder, @SuppressLint("RecyclerView") int position) {
        UserModel model = list.get(position);
        holder.tvStoryTitle.setText(model.getPostTitle());
        String title = model.getName() + ", " + calculateAge(model.getDateOfBirth()) + " years old, " + model.getCity() + " " + model.getCountry();
        holder.tvDetails.setText(title);
        Glide.with(holder.ivStory).load(model.getPostImage()).into(holder.ivStory);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClick.onClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class Vh extends RecyclerView.ViewHolder {
        ImageView ivStory;
        TextView tvDetails, tvStoryTitle;

        public Vh(@NonNull View itemView) {
            super(itemView);
            ivStory = itemView.findViewById(R.id.ivStory);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvStoryTitle = itemView.findViewById(R.id.tvStoryTitle);
        }
    }
}
