package com.example.melobeats;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlaylistFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlaylistFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    LinearLayout likes, recents;


    RecyclerView recyclerViewAlbum;

    BottomSheetDialog dialog;

    ArrayList<AudioModel> songsList;

    public PlaylistFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment playlistFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PlaylistFragment newInstance(String param1, String param2) {
        PlaylistFragment fragment = new PlaylistFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        songsList = SongsHelper.getSongsList(getContext());
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        likes = view.findViewById(R.id.all_item_likes);
        recents = view.findViewById(R.id.all_item_recent);
        setSettingDialog(likes, "likeSongs.txt");
        setSettingDialog(recents, "recentSongs.txt");
        LinearLayout loadingIndicator = ((MainActivity) requireActivity()).findViewById(R.id.loadingIndicator);
        ((MainActivity) requireActivity()).hideLoadingIndicator(loadingIndicator);
        return view;
    }


    private void setSettingDialog(LinearLayout type, String fileName) {
        dialog = new BottomSheetDialog(getContext());
        createDialog();
        type.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
                if (songsList.size() != 0) {
                    recyclerViewAlbum.setLayoutManager(new LinearLayoutManager(getContext()));
                    AudioFileAdapterIdSongs adapterAlbumSongs = new AudioFileAdapterIdSongs(
                            songsList, getContext(), getSongList(fileName), recyclerViewAlbum);
                    recyclerViewAlbum.setAdapter(adapterAlbumSongs);
                }
            }
        });
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        dialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog.getWindow().setStatusBarColor(Color.BLACK);
        }
        recyclerViewAlbum = dialog.findViewById(R.id.recycler_view_likes);
    }

    private void createDialog() {
        View view = getLayoutInflater().inflate(R.layout.bottom_dialog_likes, null, false);
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

    private ArrayList<String> getSongList(String fileName) {
        File file = new File(getContext().getFilesDir(), fileName);
        return readSongIDs(file);
    }

    private ArrayList<String> readSongIDs(File file) {
        ArrayList<String> songIDs = new ArrayList<>();
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                songIDs = (ArrayList<String>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return songIDs;
    }
}