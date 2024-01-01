package com.example.melobeats;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {

    private ArrayList<ArtistModal> artistList;
    LinearLayout show;
    BottomSheetDialog dialog;
    private Context context;

    String NameArtist;

    RecyclerView recyclerViewArtist;

    ArrayList<AudioModel> songsList;

    public ArtistAdapter(Context context, ArrayList<ArtistModal> artistList) {
        this.context = context;
        this.artistList = artistList;
        songsList = SongsHelper.getSongsList(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ArtistModal artist = artistList.get(position);
        holder.artistNameTextView.setText(artist.getNameartist());
        // Set other artist details if needed
        dialog = new BottomSheetDialog(context);
        createDialog();

        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        dialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog.getWindow().setStatusBarColor(Color.BLACK);
        }
        recyclerViewArtist = dialog.findViewById(R.id.recycler_view_artist);
    }

    @SuppressLint("MissingInflatedId")
    private void createDialog() {
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_dialog_artist, null, false);
        view.setMinimumHeight(Resources.getSystem().getDisplayMetrics().heightPixels);
        ImageButton submit = view.findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);
    }

    @Override
    public int getItemCount() {
        return artistList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView artistNameTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            artistNameTextView = itemView.findViewById(R.id.artistNameTextView);
            show = itemView.findViewById(R.id.all_item);
            show.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ArtistModal artist = artistList.get(position);
                        NameArtist = artist.getNameartist();
                    }
                    dialog.show();
                    if (songsList.size() != 0) {
                        recyclerViewArtist.setLayoutManager(new LinearLayoutManager(context));
                        AudioFileAdapterArtistSongs adapterArtistSongs = new AudioFileAdapterArtistSongs(songsList, context, NameArtist, recyclerViewArtist);
                        recyclerViewArtist.setAdapter(adapterArtistSongs);
                    }
                }
            });
        }
    }

}