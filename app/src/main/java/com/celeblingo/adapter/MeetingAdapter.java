package com.celeblingo.adapter;

import static com.celeblingo.MainActivity.extractVideoId;
import static com.celeblingo.MainActivity.extractYTId;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.celeblingo.R;
import com.celeblingo.model.Meetings;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;

public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.ViewHolder> {
    Context context;
    ArrayList<Meetings> meetingsArrayList;
    Dialog dialog;
    String youtubeVideoId;

    public MeetingAdapter(Context context, ArrayList<Meetings> meetingsArrayList, Dialog dialog) {
        this.context = context;
        this.meetingsArrayList = meetingsArrayList;
        this.dialog = dialog;
    }

    @NonNull
    @Override
    public MeetingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.layout_meetings, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MeetingAdapter.ViewHolder holder, int position) {
        Meetings meetings = meetingsArrayList.get(position);
        holder.summaryTtx.setText(meetings.getSummary());
        holder.startTimeTxt.setText(meetings.getStartTime());
        holder.endTimeTxt.setText(meetings.getEndTime());
        youtubeVideoId  = extractVideoId(meetings.getVideoUrl());
        if (youtubeVideoId.equals("NoId")){
            youtubeVideoId = extractYTId(meetings.getVideoUrl());
        }
        holder.youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer youTubePlayer) {
                youTubePlayer.loadVideo(youtubeVideoId, 0);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                super.onStateChange(youTubePlayer, state);
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                holder.youTubePlayerView.release();
            }
        });
    }

    @Override
    public int getItemCount() {
        return meetingsArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView organizerNameTxt, summaryTtx, descriptionTxt,
                startTimeTxt, endTimeTxt, attendeesTxt;
        YouTubePlayerView youTubePlayerView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            organizerNameTxt = itemView.findViewById(R.id.organizer_display_name_txt);
            summaryTtx = itemView.findViewById(R.id.summary_txt);
            descriptionTxt = itemView.findViewById(R.id.description_txt);
            startTimeTxt = itemView.findViewById(R.id.start_time_txt);
            endTimeTxt = itemView.findViewById(R.id.end_time_txt);
            attendeesTxt = itemView.findViewById(R.id.attendee_email_txt);
            youTubePlayerView = itemView.findViewById(R.id.youtube_view);
        }
    }
}
