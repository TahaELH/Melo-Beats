package com.example.melobeats;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 123;


    ImageView show;
    BottomSheetDialog dialog;

    RecyclerView recyclerViewS;

    ArrayList<AudioModel> songsList;

    private PowerManager.WakeLock wakeLock;

    private MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();

    LinearLayout loadingIndicator;

    private static final int NOTIFICATION_ID = 1;


    private void openSystemNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }




    private void animateView(View view) {
        Animation animation = new TranslateAnimation(0, 0, 1, -1);
        animation.setDuration(100);
        animation.setRepeatMode(Animation.REVERSE);
        animation.setRepeatCount(1);
        view.startAnimation(animation);
    }

    private int selectedTab = 1;
    private ActivityResultLauncher<Intent> activityResultLauncher;


    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
        }
        setContentView(R.layout.activity_main);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (wakeLock == null || !wakeLock.isHeld()) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            wakeLock.acquire();
        }
        show = findViewById(R.id.tracksearch);
        loadingIndicator = findViewById(R.id.loadingIndicator);



        songsList = SongsHelper.getSongsList(getApplicationContext());


        boolean notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled();

        if (!notificationsEnabled) {
            // Create the BottomSheetDialog
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
            bottomSheetDialog.setContentView(R.layout.bottom_dialog_notification);

            // Find the buttons in the layout
            Button cancelButton = bottomSheetDialog.findViewById(R.id.cancel);
            Button confirmButton = bottomSheetDialog.findViewById(R.id.confirm);

            // Set click listeners for the buttons
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomSheetDialog.dismiss();
                }
            });

            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSystemNotificationSettings();
                    bottomSheetDialog.dismiss();
                }
            });

            // Show the BottomSheetDialog
            bottomSheetDialog.show();
        }





        if (!checkPermission()) {
            requestPermission();
        }

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult( ActivityResult result ) {

            }
        });
        dialog = new BottomSheetDialog(this);
        createDialog();
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        dialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog.getWindow().setStatusBarColor(Color.BLACK);
        }
        EditText searchEditText = dialog.findViewById(R.id.searchEditText);
        recyclerViewS = dialog.findViewById(R.id.recycler_view_search);
        LinearLayout FSearch = dialog.findViewById(R.id.search__song);
        LinearLayout NSearch = dialog.findViewById(R.id.no__song);
        if (songsList.size() != 0) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
            recyclerViewS.setLayoutManager(layoutManager);
            AudioFileAdapterSearch adapterS = new AudioFileAdapterSearch(songsList, getApplicationContext(), searchEditText, FSearch, NSearch, recyclerViewS);
            recyclerViewS.setAdapter(adapterS);
        }








        final LinearLayout playlistLayout = findViewById(R.id.playlistLayout);
        final LinearLayout albumLayout = findViewById(R.id.albumLayout);
        final LinearLayout trackLayout = findViewById(R.id.trackLayout);
        final LinearLayout artistLayout = findViewById(R.id.artistLayout);

        final ImageView playlistImage = findViewById(R.id.playlistImage);
        final ImageView albumImage = findViewById(R.id.albumImage);
        final ImageView trackImage = findViewById(R.id.trackImage);
        final ImageView artistImage = findViewById(R.id.artistImage);

        final TextView playlistTxt = findViewById(R.id.playlistTxt);
        final TextView albumTxt = findViewById(R.id.albumTxt);
        final TextView trackTxt = findViewById(R.id.trackTxt);
        final TextView artistTxt = findViewById(R.id.artistTxt);



        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragmentContainer, TrackFragment.class, null)
                .commit();

        trackLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingIndicator(loadingIndicator);
                if (selectedTab != 1) {
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                            .replace(R.id.fragmentContainer, TrackFragment.class, null)
                            .commit();

                    albumTxt.setTextColor(getResources().getColor(R.color.album_20));
                    playlistTxt.setTextColor(getResources().getColor(R.color.playlist_20));
                    artistTxt.setTextColor(getResources().getColor(R.color.artist_20));

                    albumImage.setBackgroundResource(R.drawable.album);
                    playlistImage.setBackgroundResource(R.drawable.playlist);
                    artistImage.setBackgroundResource(R.drawable.artists);

                    albumLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    playlistLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    artistLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                    trackTxt.setTextColor(getResources().getColor(R.color.track));
                    trackImage.setBackgroundResource(R.drawable.tracks_selected);
                    animateView(trackLayout);
                    selectedTab = 1;
                }
            }
        });

        playlistLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingIndicator(loadingIndicator);
                if (selectedTab != 2) {
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                            .replace(R.id.fragmentContainer, PlaylistFragment.class, null)
                            .commit();

                    albumTxt.setTextColor(getResources().getColor(R.color.album_20));
                    trackTxt.setTextColor(getResources().getColor(R.color.track_20));
                    artistTxt.setTextColor(getResources().getColor(R.color.artist_20));

                    albumImage.setBackgroundResource(R.drawable.album);
                    trackImage.setBackgroundResource(R.drawable.tracks);
                    artistImage.setBackgroundResource(R.drawable.artists);

                    albumLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    trackLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    artistLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                    playlistTxt.setTextColor(getResources().getColor(R.color.playlist));
                    playlistImage.setBackgroundResource(R.drawable.playlist_selected);

                    animateView(playlistLayout);
                    selectedTab = 2;
                }
            }
        });

        albumLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingIndicator(loadingIndicator);
                if (selectedTab != 3) {
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                            .replace(R.id.fragmentContainer, AlbumFragment.class, null)
                            .commit();

                    trackTxt.setTextColor(getResources().getColor(R.color.track_20));
                    playlistTxt.setTextColor(getResources().getColor(R.color.playlist_20));
                    artistTxt.setTextColor(getResources().getColor(R.color.artist_20));

                    playlistImage.setBackgroundResource(R.drawable.playlist);
                    trackImage.setBackgroundResource(R.drawable.tracks);
                    artistImage.setBackgroundResource(R.drawable.artists);

                    playlistLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    trackLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    artistLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                    albumTxt.setTextColor(getResources().getColor(R.color.album));
                    albumImage.setBackgroundResource(R.drawable.album_selected);

                    animateView(albumLayout);
                    selectedTab = 3;
                }
            }
        });

        artistLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoadingIndicator(loadingIndicator);
                if (selectedTab != 4) {
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                            .replace(R.id.fragmentContainer, ArtistFragment.class, null)
                            .commit();

                    trackTxt.setTextColor(getResources().getColor(R.color.track_20));
                    playlistTxt.setTextColor(getResources().getColor(R.color.playlist_20));
                    albumTxt.setTextColor(getResources().getColor(R.color.album_20));

                    playlistImage.setBackgroundResource(R.drawable.playlist);
                    trackImage.setBackgroundResource(R.drawable.tracks);
                    albumImage.setBackgroundResource(R.drawable.album);

                    playlistLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    trackLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    albumLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                    artistTxt.setTextColor(getResources().getColor(R.color.artist));
                    artistImage.setBackgroundResource(R.drawable.artists_selected);

                    animateView(artistLayout);
                    selectedTab = 4;
                }
            }
        });
    }
    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int readCheck = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
            int writeCheck = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
            return readCheck == PackageManager.PERMISSION_GRANTED && writeCheck == PackageManager.PERMISSION_GRANTED;
        }
    }

    void showLoadingIndicator(LinearLayout loading) {
        loading.setVisibility(View.VISIBLE);
    }

    void hideLoadingIndicator(LinearLayout loading) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                loading.setVisibility(View.GONE);
            }
        }, 200);
    }



    private String[] permissions = {READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE};
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Permission")
                    .setMessage("Please give the Storage permission")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick( DialogInterface dialog, int which ) {
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                intent.addCategory("android.intent.category.DEFAULT");
                                intent.setData(Uri.parse(String.format("package:%s", new Object[]{getApplicationContext().getPackageName()})));
                                activityResultLauncher.launch(intent);
                            } catch (Exception e) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                activityResultLauncher.launch(intent);
                            }
                        }
                    })
                    .setCancelable(false)
                    .show();

        } else {

            ActivityCompat.requestPermissions(MainActivity.this, permissions, 30);
        }
    }




    private List<File> getSortedAudioFiles() {
        List<File> audioFiles = new ArrayList<>();
        Collections.sort(audioFiles, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                String title1 = null;
                try {
                    title1 = getAudioFileTitle(file1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String title2 = null;
                try {
                    title2 = getAudioFileTitle(file2);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return title1.compareToIgnoreCase(title2);
            }
        });

        return audioFiles;
    }

    private String getAudioFileTitle(File audioFile) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(audioFile.getAbsolutePath());
        String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        retriever.release();
        return title;
    }
    private void createDialog() {
        View view = getLayoutInflater().inflate(R.layout.bottom_dialog, null, false);
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
    protected void onDestroy() {
        super.onDestroy();

        // Pause the media player if it is playing
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }

        // Stop the service
        Intent stopServiceIntent = new Intent(this, PlaybackService.class);
        stopService(stopServiceIntent);

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

}