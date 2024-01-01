package com.example.melobeats;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AudioFileAdapterSearch extends RecyclerView.Adapter<AudioFileAdapterSearch.ViewHolder> {
    private final ArrayList<AudioModel> songsList;
    private final ArrayList<AudioModel> filteredSongsList;
    private final Context context;
    private EditText searchEditText;
    private final LinearLayout FSearch;
    private final LinearLayout NSearch;
    private final RecyclerView recyclerViewS;


    public AudioFileAdapterSearch(ArrayList<AudioModel> songsList, Context context, EditText searchEditText, LinearLayout FSearch, LinearLayout NSearch, RecyclerView recyclerViewS) {
        this.songsList = songsList;
        this.filteredSongsList = new ArrayList<>(songsList);
        this.context = context;
        this.searchEditText = searchEditText;
        this.FSearch = FSearch;
        this.NSearch = NSearch;
        this.recyclerViewS = recyclerViewS;

        recyclerViewS.setVisibility(View.GONE);

        // Add a TextWatcher to filter the songs list when the searchEditText value changes
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // No implementation needed
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                filterSongs(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
                Intent intent = new Intent(context, MusicPlayerActivity.class);
                File file = new File(context.getFilesDir(), "sonList.txt");

                try (FileOutputStream fos = new FileOutputStream(file);
                     ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                    oos.writeObject(filteredSongsList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                intent.putExtra("LIST", filteredSongsList);
                intent.putExtra("CURRENT", filteredSongsList.get(MyMediaPlayer.currentIndex));
                intent.putExtra("SIZE", filteredSongsList.size());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredSongsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, artistTextView;
        ImageView trackImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            artistTextView = itemView.findViewById(R.id.artistTextView);
            trackImageView = itemView.findViewById(R.id.imagetrackH);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void filterSongs(String searchText) {
        filteredSongsList.clear();
        if (searchText.isEmpty()) {
            recyclerViewS.setVisibility(View.GONE);
            NSearch.setVisibility(View.GONE);
            FSearch.setVisibility(View.VISIBLE);
        } else {
            recyclerViewS.setVisibility(View.VISIBLE);
            NSearch.setVisibility(View.GONE);
            FSearch.setVisibility(View.GONE);
            String searchQuery = searchText.toLowerCase(Locale.getDefault());
            for (AudioModel song : songsList) {
                if (song.getTitle().toLowerCase(Locale.getDefault()).contains(searchQuery) ||
                        song.getArtist().toLowerCase(Locale.getDefault()).contains(searchQuery)) {
                    filteredSongsList.add(song);
                }
            }
            if(filteredSongsList.size() == 0){
                NSearch.setVisibility(View.VISIBLE);
                FSearch.setVisibility(View.GONE);
                recyclerViewS.setVisibility(View.GONE);
            }
        }
        notifyDataSetChanged();
    }
    public static String convertToMMSS(String duration) {
        long millis = Long.parseLong(duration);
        @SuppressLint("DefaultLocale") String formattedDuration = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));

        return formattedDuration;
    }
}
