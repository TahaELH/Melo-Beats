package com.example.melobeats;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AlbumFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlbumFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ArrayList<AlbumModal> albumExist;

    public AlbumFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment albumFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AlbumFragment newInstance(String param1, String param2) {
        AlbumFragment fragment = new AlbumFragment();
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
        albumExist = new ArrayList<>();
        String[] folderNames = {"snaptube", "Music", "Download"};
        albumExist = getAudioFileTitles(folderNames);
    }

    private ArrayList<AlbumModal> getAudioFileTitles(String[] folderPaths) {
        ArrayList<AlbumModal> albumExist = new ArrayList<>();
        StringBuilder selectionBuilder = new StringBuilder();
        String[] projection = {
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
        };
        selectionBuilder.append(MediaStore.Audio.Media.IS_MUSIC).append(" != 0 AND (");
        for (int i = 0; i < folderPaths.length; i++) {
            if (i > 0) {
                selectionBuilder.append(" OR ");
            }
            selectionBuilder.append(MediaStore.Audio.Media.DATA)
                    .append(" LIKE '%").append(folderPaths[i]).append("%'");
        }
        selectionBuilder.append(")");

        String selection = selectionBuilder.toString();
        String[] selectionArgs = null;

        ContentResolver contentResolver = getContext().getContentResolver();
        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                MediaStore.Audio.Media.TITLE + " DESC"
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String albumId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                @SuppressLint("Range") String albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));

                // Check if album ID exists in albumExist list
                boolean albumIdExists = false;
                for (AlbumModal albumModal : albumExist) {
                    if (albumModal.getIdalbum().equals(albumId)) {
                        albumIdExists = true;
                        break;
                    }
                }

                // If album ID does not exist, add it to albumExist list
                if (!albumIdExists) {
                    AlbumModal songData = new AlbumModal(albumId, albumName);
                    albumExist.add(songData);
                }
            }
            cursor.close();
        }

        return albumExist;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_album, container, false);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) RecyclerView recyclerView = view.findViewById(R.id.recycler_view_albums);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create the adapter and set it for the RecyclerView
        AlbumAdapter albumAdapter = new AlbumAdapter(getContext(), albumExist);
        recyclerView.setAdapter(albumAdapter);
        LinearLayout loadingIndicator = ((MainActivity) requireActivity()).findViewById(R.id.loadingIndicator);
        ((MainActivity) requireActivity()).hideLoadingIndicator(loadingIndicator);

        return view;
    }
}