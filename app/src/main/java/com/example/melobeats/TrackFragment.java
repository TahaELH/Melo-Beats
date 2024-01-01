package com.example.melobeats;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class TrackFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final String FOLDER_NAME = "Melo-Beats-Music";

    LinearLayout nosonglinear;




    Boolean clickBlock = false;



    private String mParam1;
    private String mParam2;

    private RecyclerView recyclerView;


    ImageButton BlockBtn;

    ArrayList<AudioModel> songsList = new ArrayList<>();

    private ContentResolver contentResolver;

    public TrackFragment() {
        // Required empty public constructor

    }

    public static TrackFragment newInstance(String param1, String param2) {
        TrackFragment fragment = new TrackFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        contentResolver = context.getContentResolver();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    private void createAppFolder() {
        File folder = new File(Environment.getExternalStorageDirectory(), FOLDER_NAME);
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                Toast.makeText(getContext(), "Folder 'Melo-Beats-Music' created successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to create folder", Toast.LENGTH_SHORT).show();
            }
        }

    }




    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        nosonglinear = view.findViewById(R.id.no_any_song);
        BlockBtn = view.findViewById(R.id.changeStyle);
        if (!checkPermission()) {
            requestPermission();
        }
        createAppFolder();
        songsList = SongsHelper.getSongsList(getContext());

        BlockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickBlock = !clickBlock;
                if(songsList.size()!=0){
                    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                    recyclerView.setLayoutManager(layoutManager);
                    AudioFileAdapter adapter = new AudioFileAdapter(songsList, requireContext().getApplicationContext(), clickBlock);
                    recyclerView.setAdapter(adapter);
                    nosonglinear.setVisibility(View.GONE);
                }else{
                    nosonglinear.setVisibility(View.VISIBLE);
                }
            }
        });

        if(songsList.size()!=0){
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            AudioFileAdapter adapter = new AudioFileAdapter(songsList, requireContext().getApplicationContext(), clickBlock);
            recyclerView.setAdapter(adapter);
            nosonglinear.setVisibility(View.GONE);
        }else{
            nosonglinear.setVisibility(View.VISIBLE);
        }

        LinearLayout loadingIndicator = ((MainActivity) requireActivity()).findViewById(R.id.loadingIndicator);
        ((MainActivity) requireActivity()).hideLoadingIndicator(loadingIndicator);
        return view;
    }


    boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    void requestPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(getActivity(), "ALLOW FROM SETTINGS", Toast.LENGTH_SHORT).show();
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(recyclerView!=null){
            recyclerView.setAdapter(new AudioFileAdapter(songsList,getContext(), clickBlock));
        }
    }
}
