package com.example.storia.adapter;

import static com.example.storia.MainActivity.createInitialsBitmap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storia.model.CommentModel;
import com.example.storia.R;
import com.example.storia.model.UserModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.Vh> {
    Context context;
    List<CommentModel> list;
    DatabaseReference dbRefUsers;

    public CommentsAdapter(Context context, List<CommentModel> list, DatabaseReference dbRefUsers) {
        this.context = context;
        this.list = list;
        this.dbRefUsers = dbRefUsers;
    }

    @NonNull
    @Override
    public Vh onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new Vh(view);
    }

    @SuppressLint({"SetTextI18n", "SimpleDateFormat"})
    @Override
    public void onBindViewHolder(@NonNull Vh holder, @SuppressLint("RecyclerView") int position) {
        CommentModel model = list.get(position);
        holder.tvComment.setText(model.getComment());
        holder.tvDate.setText(model.getDate());
        dbRefUsers.child(model.getMyUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserModel user = dataSnapshot.getValue(UserModel.class);
                holder.tvUser.setText(user.getName());
                Bitmap initialsBitmap = createInitialsBitmap(user.getName(), 200);
                holder.ivUser.setImageBitmap(initialsBitmap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class Vh extends RecyclerView.ViewHolder {
        ImageView ivUser;
        TextView tvComment, tvDate, tvUser;

        public Vh(@NonNull View itemView) {
            super(itemView);
            ivUser = itemView.findViewById(R.id.ivUser);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvUser = itemView.findViewById(R.id.tvUser);
        }
    }
}
