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

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {

    private ArrayList<AlbumModal> albumList;
    LinearLayout show;
    BottomSheetDialog dialog;
    private Context context;

    String NameAlbum;

    RecyclerView recyclerViewAlbum;

    ArrayList<AudioModel> songsList;

    public AlbumAdapter(Context context, ArrayList<AlbumModal> albumList) {
        this.context = context;
        this.albumList = albumList;
        songsList = SongsHelper.getSongsList(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AlbumModal album = albumList.get(position);
        holder.albumNameTextView.setText(album.getNamealbum());
        // Set other album details if needed
        dialog = new BottomSheetDialog(context);
        createDialog();

        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        dialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog.getWindow().setStatusBarColor(Color.BLACK);
        }
        recyclerViewAlbum = dialog.findViewById(R.id.recycler_view_album);
    }

    @SuppressLint("MissingInflatedId")
    private void createDialog() {
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_dialog_album, null, false);
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
        return albumList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView albumNameTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            albumNameTextView = itemView.findViewById(R.id.albumNameTextView);
            show = itemView.findViewById(R.id.all_item);
            show.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        AlbumModal album = albumList.get(position);
                        NameAlbum = album.getNamealbum();
                    }
                    dialog.show();
                    if (songsList.size() != 0) {
                        recyclerViewAlbum.setLayoutManager(new LinearLayoutManager(context));
                        AudioFileAdapterAlbumSongs adapterAlbumSongs = new AudioFileAdapterAlbumSongs(songsList, context, NameAlbum, recyclerViewAlbum);
                        recyclerViewAlbum.setAdapter(adapterAlbumSongs);
                    }
                }
            });
        }
    }

}