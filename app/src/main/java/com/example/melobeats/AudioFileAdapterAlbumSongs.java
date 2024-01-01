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

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AudioFileAdapterAlbumSongs extends RecyclerView.Adapter<AudioFileAdapterAlbumSongs.ViewHolder> {
    private ArrayList<AudioModel> songsList;
    private ArrayList<AudioModel> filteredSongsList;
    private Context context;
    private String NameAlbum;
    private RecyclerView recyclerViewAlbum;


    public AudioFileAdapterAlbumSongs(ArrayList<AudioModel> songsList, Context context, String NameAlbum, RecyclerView recyclerViewAlbum) {
        this.songsList = songsList;
        this.filteredSongsList = new ArrayList<>(songsList);
        this.context = context;
        this.NameAlbum = NameAlbum;
        this.recyclerViewAlbum = recyclerViewAlbum;

        filterSongs(NameAlbum);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_audio_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        AudioModel songData = filteredSongsList.get(position);
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
                holder.trackImageView.setImageURI(Uri.fromFile(imageFile));
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyMediaPlayer.getInstance().reset();
                MyMediaPlayer.currentIndex = position;
                MyMediaPlayer.AudioId = Integer.parseInt(songData.getID());
                File file = new File(context.getFilesDir(), "sonList.txt");

                try (FileOutputStream fos = new FileOutputStream(file);
                     ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                    oos.writeObject(filteredSongsList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(context, MusicPlayerActivity.class);
                intent.putExtra("LIST", filteredSongsList);
                intent.putExtra("CURRENT", filteredSongsList.get(MyMediaPlayer.currentIndex));
                intent.putExtra("SIZE", filteredSongsList.size());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        holder.detailsTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BottomSheetDialog detailDialog = new BottomSheetDialog(context);
                detailDialog.setContentView(R.layout.bottom_dialog_details_track);
                TextView trackNameDetail = detailDialog.findViewById(R.id.trackNameDetail);
                trackNameDetail.setText(songData.getTitle());
                detailDialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredSongsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, artistTextView;
        ImageView trackImageView, detailsTrack;

        public ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            artistTextView = itemView.findViewById(R.id.artistTextView);
            trackImageView = itemView.findViewById(R.id.imagetrackH);
            detailsTrack = itemView.findViewById(R.id.detailsTrack);
        }
    }

    private void filterSongs(String searchText) {
        filteredSongsList.clear();
        String searchQuery = searchText.toLowerCase(Locale.getDefault());
        for (AudioModel song : songsList) {
            if (song.getAlbum().toLowerCase(Locale.getDefault()).contains(searchQuery)) {
                filteredSongsList.add(song);
            }
        }
        notifyDataSetChanged();
    }
    public static String convertToMMSS(String duration) {
        long millis = Long.parseLong(duration);
        String formattedDuration = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));

        return formattedDuration;
    }
}
