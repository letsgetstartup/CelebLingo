package com.celeblingo.adapter;

import static com.celeblingo.MainActivity.extractUrl;
import static com.celeblingo.MainActivity.extractVideoId;
import static com.celeblingo.MainActivity.extractYTId;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.celeblingo.MainActivity;
import com.celeblingo.R;
import com.celeblingo.WebViewActivity;
import com.celeblingo.model.Meetings;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.ViewHolder> {
    Context context;
    ArrayList<Meetings> meetingsArrayList;
    String youtubeVideoId;
    String type;

    public MeetingAdapter(Context context, ArrayList<Meetings> meetingsArrayList, String type) {
        this.context = context;
        this.meetingsArrayList = meetingsArrayList;
        this.type = type;
    }

    @NonNull
    @Override
    public MeetingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.layout_meetings, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MeetingAdapter.ViewHolder holder, int position) {
        Meetings meetings = meetingsArrayList.get(position);
        holder.summaryTtx.setText(meetings.getSummary());
        holder.startTimeTxt.setText(extractDateTime(meetings.getStartTime())
                + " - " + extractTime(meetings.getEndTime()));
        holder.endTimeTxt.setText(extractDateTime(meetings.getEndTime()));
        youtubeVideoId = extractVideoId(meetings.getVideoUrl());
        if (youtubeVideoId.equals("NoId")) {
            youtubeVideoId = extractYTId(meetings.getVideoUrl());
        }
        holder.joinBtn.setOnClickListener(view -> {
            String url = extractUrl(meetings.getGptUrl());
            Log.d("==url", url + " ");
            context.startActivity(new Intent(context, WebViewActivity.class)
                    .putExtra("url", url)
                    .putExtra("type", "meeting")
                    .putExtra("meetingId", meetings.getId()));
        });

//        holder.youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
//            @Override
//            public void onReady(@NonNull com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer youTubePlayer) {
//                youTubePlayer.loadVideo(youtubeVideoId, 0);
//            }
//
//            @Override
//            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
//                super.onStateChange(youTubePlayer, state);
//            }
//        });
    }

    private String extractDateTime(String date) {
        OffsetDateTime odt = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            odt = OffsetDateTime.parse(date);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E,dd MMM yyyy HH:mm");
            return odt.format(formatter);
        }
        return date;
    }

    private String extractTime(String date) {
        OffsetDateTime odt = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            odt = OffsetDateTime.parse(date);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            return odt.format(formatter);
        }
        return date;
    }

    @Override
    public int getItemCount() {
        return meetingsArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView organizerNameTxt, summaryTtx, descriptionTxt,
                startTimeTxt, endTimeTxt, attendeesTxt;
        AppCompatButton joinBtn;

        //        YouTubePlayerView youTubePlayerView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            organizerNameTxt = itemView.findViewById(R.id.organizer_display_name_txt);
            summaryTtx = itemView.findViewById(R.id.summary_txt);
            descriptionTxt = itemView.findViewById(R.id.description_txt);
            startTimeTxt = itemView.findViewById(R.id.start_time_txt);
            endTimeTxt = itemView.findViewById(R.id.end_time_txt);
            attendeesTxt = itemView.findViewById(R.id.attendee_email_txt);
//            youTubePlayerView = itemView.findViewById(R.id.youtube_view);
            joinBtn = itemView.findViewById(R.id.join_meeting_btn);
        }
    }
}
