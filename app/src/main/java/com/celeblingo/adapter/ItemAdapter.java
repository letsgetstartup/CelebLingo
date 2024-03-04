package com.celeblingo.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.celeblingo.R;
import com.celeblingo.WebViewActivity;
import com.celeblingo.model.BGImages;
import com.celeblingo.model.GPTURL;
import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.AnimationTypes;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.interfaces.ItemClickListener;
import com.denzcoskun.imageslider.models.SlideModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    Activity context;
    ArrayList<GPTURL> gpturlArrayList;

    public ItemAdapter(Activity context, ArrayList<GPTURL> gpturlArrayList) {
        this.context = context;
        this.gpturlArrayList = gpturlArrayList;
    }

    @NonNull
    @Override
    public ItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.layout_items, parent, false);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams((int) (width / 2.0),
                RecyclerView.LayoutParams.MATCH_PARENT);
        params.setMargins(8, 8, 8, 8);
        v.setLayoutParams(params);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.titleTxt.setText(gpturlArrayList.get(position).getTitle());
        holder.bodyTxt.setText(gpturlArrayList.get(position).getText());
        holder.itemView.setOnClickListener(view -> {
            context.startActivity(new Intent(context, WebViewActivity.class)
                    .putExtra("url", gpturlArrayList.get(position).getUrl())
                    .putExtra("type", "card"));
        });
        holder.itemContentLyt.setOnClickListener(view -> {
            context.startActivity(new Intent(context, WebViewActivity.class)
                    .putExtra("url", gpturlArrayList.get(position).getUrl())
                    .putExtra("type", "card"));
        });

        //getCardItemBGImages(position + 1, holder.slider);
        Glide.with(context).load(gpturlArrayList.get(position).getImage())
                .into(holder.itemImage);
    }

    private void getCardItemBGImages(int id, ImageSlider slider) {
        ArrayList<SlideModel> imageList = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("GPTURL")
                .child(id + "").child("BGImages");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        BGImages bgImages = dataSnapshot.getValue(BGImages.class);
                        assert bgImages != null;
                        imageList.add(new SlideModel(bgImages.getImage(), ScaleTypes.FIT));
                    }
                    slider.setImageList(imageList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return gpturlArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTxt, bodyTxt;
        ImageView itemImage;
        ImageSlider slider;
        LinearLayout itemContentLyt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTxt = itemView.findViewById(R.id.item_title);
            bodyTxt = itemView.findViewById(R.id.item_text);
            itemImage = itemView.findViewById(R.id.item_image);
            slider = itemView.findViewById(R.id.image_slider);
            itemContentLyt = itemView.findViewById(R.id.item_content_lyt);
        }
    }
}
