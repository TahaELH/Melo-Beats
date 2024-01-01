package com.example.melobeats;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class AudioFileAdapter extends RecyclerView.Adapter<AudioFileAdapter.ViewHolder> {
    ArrayList<AudioModel> songsList;
    Context context;

    Boolean isBlock;

    View viewMain;

    public AudioFileAdapter(ArrayList<AudioModel> songsList, Context context, Boolean isBlock) {
        this.songsList = songsList;
        this.context = context;
        this.isBlock = isBlock;

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(isBlock != true ? R.layout.list_item_audio_file : R.layout.list_item_audio_file_block, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        AudioModel songData = songsList.get(position);
        if(MyMediaPlayer.AudioId == Integer.parseInt(songData.getID())){
            holder.titleTextView.setTextColor(Color.parseColor("#201bff"));
        }else{
            holder.titleTextView.setTextColor(Color.parseColor("#FFFFFF"));
        }
        holder.titleTextView.setText(songData.getTitle());
        holder.artistTextView.setText(songData.getArtist());
        String folderName = "Music/.thumbnails";
        String imageName = songData.getID() + ".jpg";
        File folder = new File(Environment.getExternalStorageDirectory(), folderName);
        if (folder.exists()) {
            File imageFile = new File(folder, imageName);
            if (imageFile.exists()) {
                Glide.with(context)
                        .load(Uri.fromFile(imageFile))
                        .into(holder.trackImageView);
            }
        }



//        holder.detailsTrack.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                BottomSheetDialog detailDialog = new BottomSheetDialog(context);
//                detailDialog.setContentView(R.layout.bottom_dialog_details_track);
//                TextView trackNameDetail = detailDialog.findViewById(R.id.trackNameDetail);
//                trackNameDetail.setText(songData.getTitle());
//                detailDialog.show();
//            }
//        });


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyMediaPlayer.getInstance().reset();
                MyMediaPlayer.currentIndex = position;
                MyMediaPlayer.AudioId = Integer.parseInt(songData.getID());
                File file = new File(context.getFilesDir(), "sonList.txt");

                try (FileOutputStream fos = new FileOutputStream(file);
                     ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                    oos.writeObject(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(context, MusicPlayerActivity.class);
                intent.putExtra("SIZE", songsList.size());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, artistTextView;
        ImageView trackImageView, detailsTrack;
        public ViewHolder(View itemView) {
            super(itemView);
            viewMain = itemView;
            titleTextView = itemView.findViewById(R.id.titleTextView);
            artistTextView = itemView.findViewById(R.id.artistTextView);
            trackImageView = itemView.findViewById(R.id.imagetrackH);
            detailsTrack = itemView.findViewById(R.id.detailsTrack);
        }
    }
}


