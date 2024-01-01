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
 * Use the {@link ArtistFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ArtistFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;



    ArrayList<ArtistModal> artistExist;


    public ArtistFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ArtistFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ArtistFragment newInstance(String param1, String param2) {
        ArtistFragment fragment = new ArtistFragment();
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
        artistExist = new ArrayList<>();
        String[] folderNames = {"snaptube", "Music", "Download"};
        artistExist = getAudioFileTitles(folderNames);
    }
    private ArrayList<ArtistModal> getAudioFileTitles(String[] folderPaths) {
        ArrayList<ArtistModal> artistExist = new ArrayList<>();
        StringBuilder selectionBuilder = new StringBuilder();
        String[] projection = {
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
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
                @SuppressLint("Range") String artistId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));
                @SuppressLint("Range") String artistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                // Check if artist ID exists in artistExist list
                boolean artistIdExists = false;
                for (ArtistModal artistModal : artistExist) {
                    if (artistModal.getIdartist().equals(artistId)) {
                        artistIdExists = true;
                        break;
                    }
                }

                // If artist ID does not exist, add it to artistExist list
                if (!artistIdExists) {
                    ArtistModal songData = new ArtistModal(artistId, artistName);
                    artistExist.add(songData);
                }
            }
            cursor.close();
        }

        return artistExist;
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_artists);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create the adapter and set it for the RecyclerView
        ArtistAdapter artistAdapter = new ArtistAdapter(getContext(), artistExist);
        recyclerView.setAdapter(artistAdapter);

        LinearLayout loadingIndicator = ((MainActivity) requireActivity()).findViewById(R.id.loadingIndicator);
        ((MainActivity) requireActivity()).hideLoadingIndicator(loadingIndicator);

        return view;
    }

}