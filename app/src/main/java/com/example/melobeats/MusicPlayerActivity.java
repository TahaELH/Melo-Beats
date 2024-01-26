package com.example.melobeats;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {

    private static final int DELAY_MILLIS = 100;
    private static final int SEEK_FORWARD_MILLIS = 5000;

    // UI elements
    private TextView titleTextView, currentTimeTextView, totalTimeTextView, artistTextView, albumTextView, LyricsButtonText;
    private SeekBar seekBar;
    private ImageButton pausePlayButton, previousButton, nextButton, repeatButton, shuffleButton, returnButton, likeButton;
    private LinearLayout primaryColorLayout;
    private ImageView albumArtImageView, detailsTrackImageView;

    // Other variables
    private int imageNumber = 1;
    private int primaryColor;

    private boolean isRepeatEnabled;
    private boolean isRandomEnabled = false;

    private AudioModel currentSong;

    private Uri urlImage;

    private int sizeList;

    private BroadcastReceiver playPauseReceiver;
    private BroadcastReceiver playNextReceiver;
    private BroadcastReceiver playPreviousReceiver;


    int currentMediaIndex = 0;


    ArrayList<AudioModel> songsList;

    ArrayList<AudioModel> retrievedAlbumList;

    private MediaPlayer mediaPlayer;

    private MediaSessionCompat mediaSession;
    private MusicPlayerSessionCallback mediaSessionCallback;

    private TextView lyricsTextView;


    @SuppressLint({"MissingInflatedId", "ResourceType"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        sizeList = getIntent().getIntExtra("SIZE", 0);




        playPauseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                pausePlay();
            }
        };

        playNextReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                playNextSong();
            }
        };

        playPreviousReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                playPreviousSong();
            }
        };

        IntentFilter playPauseFilter = new IntentFilter("PLAY_PAUSE");
        IntentFilter nextFilter = new IntentFilter("PLAY_NEXT");
        IntentFilter previousFilter = new IntentFilter("PLAY_PREVIOUS");

        registerReceiver(playPauseReceiver, playPauseFilter);
        registerReceiver(playNextReceiver, nextFilter);
        registerReceiver(playPreviousReceiver, previousFilter);


        setContentView(R.layout.activity_music_player);

        initializeUI(); // Ensure that UI elements are initialized first
        mediaSession = new MediaSessionCompat(this, "MusicPlayerSession");
        setResourcesWithMusic(); // Now you can safely use UI elements
        setListeners();
        updateUI();

        runOnUiThread(() -> {
            updateSeekBarAndTime();
            new Handler().postDelayed(() -> updateUI(), DELAY_MILLIS);
        });
    }



    private void initializeUI() {
        // Initialize UI elements
        titleTextView = findViewById(R.id.trackName);
        currentTimeTextView = findViewById(R.id.currentTime);
        artistTextView = findViewById(R.id.artistName);
        albumTextView = findViewById(R.id.albumName);
        totalTimeTextView = findViewById(R.id.totalTime);
        seekBar = findViewById(R.id.seekBar);
        pausePlayButton = findViewById(R.id.playPauseButton);
        previousButton = findViewById(R.id.previousButton);
        detailsTrackImageView = findViewById(R.id.detailsTrack);
        nextButton = findViewById(R.id.nextButton);
        repeatButton = findViewById(R.id.repeatImageButton);
        shuffleButton = findViewById(R.id.aleatoireImageButton);
        returnButton = findViewById(R.id.submit);
        albumArtImageView = findViewById(R.id.imageView);
        primaryColorLayout = findViewById(R.id.changeprimarycolor);
        likeButton = findViewById(R.id.likeCurrentTrack);

        // Initialize lyrics TextView
        lyricsTextView = findViewById(R.id.lyricsTextView);


        LyricsButtonText = findViewById(R.id.LyricsButtonText);


    }




    private void setListeners() {
        // Set listeners for UI elements
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress);

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mediaPlayer.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.start();
                updateNotification();
            }
        });

        // Set click listeners for buttons
        pausePlayButton.setOnClickListener(v -> pausePlay());
        nextButton.setOnClickListener(v -> playNextSong());
        nextButton.setOnFocusChangeListener((View v, boolean hasFocus) -> addSeconds());
        previousButton.setOnClickListener(v -> playPreviousSong());
        repeatButton.setOnClickListener(v -> {
            toggleRepeat(repeatButton, isRepeatEnabled);
            isRepeatEnabled = !isRepeatEnabled;
        });
        shuffleButton.setOnClickListener(v -> {
            isRandomEnabled = !isRandomEnabled;
            toggleRandom(shuffleButton, isRandomEnabled);
        });

        returnButton.setOnClickListener(view -> onBackPressed());
        detailsTrackImageView.setOnClickListener(view -> showDetailsBottomSheet());
        LyricsButtonText.setOnClickListener(view -> showDetailsBottomSheetLyrics());
        likeButton.setOnClickListener(view -> addRemoveLikeSong());
    }

    public void toggleRepeat(ImageButton repeatButton, boolean isRepeatEnabled) {
        if (isRepeatEnabled) {
            mediaPlayer.setLooping(false); // Toggle repeat on
            repeatButton.setImageResource(R.drawable.repeate_all);
        } else {
            mediaPlayer.setLooping(true); // Toggle repeat off
            repeatButton.setImageResource(R.drawable.repeate_one);
        }
        updateNotification();
    }


    public void toggleRandom(ImageButton shuffleButton, boolean isRandomEnable) {

        if (isRandomEnable) {
            shuffleButton.setImageResource(R.drawable.shuffle_true);
        } else {
            shuffleButton.setImageResource(R.drawable.shuffle);
        }
        Log.e("NOTIFICATION", "" + isRandomEnable);
        updateNotification();
    }

    private void setResourcesWithMusic() {
        File albumFile = new File(getBaseContext().getFilesDir(), "sonList.txt");
        retrievedAlbumList = null;
        try (FileInputStream fis = new FileInputStream(albumFile);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            retrievedAlbumList = (ArrayList<AudioModel>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        currentMediaIndex = 0;
        File file = new File(getBaseContext().getFilesDir(), "likeSongs.txt");
        songsList = null;
        if (retrievedAlbumList != null){
            songsList = retrievedAlbumList;
        }else {
            songsList = SongsHelper.getSongsList(getBaseContext());
        }
        if( MyMediaPlayer.currentIndex <= songsList.size()){
            currentSong = songsList.get(MyMediaPlayer.currentIndex);
        }else {
            MyMediaPlayer.currentIndex = songsList.size() - 1;
            currentSong = songsList.get(MyMediaPlayer.currentIndex);
        }
        mediaPlayer = MyMediaPlayer.getInstance();
        isRepeatEnabled = mediaPlayer.isLooping();

        updateNotification();

        setContent(currentSong);


        mediaSessionCallback = new MusicPlayerSessionCallback(mediaSession, file);
        mediaSession.setCallback(mediaSessionCallback);
        mediaSession.setActive(true);

        if(mediaSessionCallback.isCurrentSongFavorite(currentSong.getID())){
            likeButton.setImageResource(R.drawable.favorite_icon_fill);
        }else{
            likeButton.setImageResource(R.drawable.favorite_icon);
        }

        if(mediaPlayer.isPlaying() && mediaPlayer.getCurrentPosition() != 0){
            if (isRepeatEnabled) {
                mediaPlayer.setLooping(true);
                repeatButton.setImageResource(R.drawable.repeate_one);
            } else {
                mediaPlayer.setLooping(false);
                repeatButton.setImageResource(R.drawable.repeate_all);
            }

            if (isRandomEnabled) {
                shuffleButton.setImageResource(R.drawable.shuffle_true);
            } else {
                shuffleButton.setImageResource(R.drawable.shuffle);
            }
        }else {
            setContent(currentSong);
            playMusic();
        }

    }

    void setContent(AudioModel crtsong) {
        if (crtsong != null) {
            titleTextView.setText(crtsong.getTitle());
            albumTextView.setText(crtsong.getAlbum());
            artistTextView.setText(crtsong.getArtist());

            String folderName = "Music/.thumbnails";
            String imageName = crtsong.getID() + ".jpg";
            File folder = new File(Environment.getExternalStorageDirectory(), folderName);

            if (folder.exists()) {
                File imageFile = new File(folder, imageName);

                if (imageFile.exists()) {
                    urlImage = Uri.fromFile(imageFile);
                    albumArtImageView.setImageURI(urlImage);

                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            if (palette != null && palette.getDominantSwatch() != null) {
                                int color = palette.getDominantSwatch().getRgb();
                                int darkerColor = darkenColor(color, 0.5f);

                                Drawable backgroundDrawable = primaryColorLayout.getBackground();
                                GradientDrawable gradientDrawable;

                                if (backgroundDrawable instanceof GradientDrawable) {
                                    gradientDrawable = (GradientDrawable) backgroundDrawable;
                                } else {
                                    gradientDrawable = new GradientDrawable();
                                    gradientDrawable.setColor(((ColorDrawable) backgroundDrawable).getColor());
                                }

                                gradientDrawable.setColors(new int[]{darkerColor, Color.parseColor("#80000000")});
                                primaryColorLayout.setBackground(gradientDrawable);
                                primaryColor = darkerColor;
                            }
                        }
                    });

                } else {
                    albumArtImageView.setImageResource(R.drawable.logo1);
                    primaryColorLayout.setBackgroundColor(0xFF000000);
                    primaryColor = 0xFF000000;
                }
            }

            totalTimeTextView.setText(convertToMMSS(crtsong.getDuration()));
        }
    }


    private void updateUI() {
        runOnUiThread(() -> {
            if (mediaPlayer != null) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                currentTimeTextView.setText(convertToMMSS(String.valueOf(mediaPlayer.getCurrentPosition())));

                if (mediaPlayer.isPlaying()) {
                    pausePlayButton.setImageResource(R.drawable.baseline_pause_circle_24);
                } else {
                    pausePlayButton.setImageResource(R.drawable.baseline_play_circle_24);
                }
            }
            new Handler().postDelayed(() -> updateUI(), DELAY_MILLIS);
        });
    }

    private void updateSeekBarAndTime() {
        seekBar.setMax(mediaPlayer.getDuration());
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
//        mediaSessionCallback.onCompletion(mediaPlayer);
        currentTimeTextView.setText(convertToMMSS(String.valueOf(mediaPlayer.getCurrentPosition())));

        if (mediaPlayer.isPlaying()) {
            pausePlayButton.setImageResource(R.drawable.baseline_pause_circle_24);
        } else {
            pausePlayButton.setImageResource(R.drawable.baseline_play_circle_24);
        }

        new Handler().postDelayed(this::updateSeekBarAndTime, DELAY_MILLIS);
    }

    private void playMusic() {
        mediaPlayer.setOnCompletionListener(mediaPlayer -> {
            currentMediaIndex +=1;
            if (isRepeatEnabled) {
                mediaPlayer.seekTo(0);
                mediaSessionCallback.onPlay();
            }else {
                if (currentMediaIndex > 0){
                    playNextSong();
                    currentMediaIndex -=1;
                }
            }
        });

        try {
            mediaPlayer.reset(); // Reset the MediaPlayer to bring it to the Idle state
            mediaPlayer.setDataSource(currentSong.getPath());
            mediaPlayer.prepare();
            mediaSessionCallback.onPlay();
            updateRecentSongs();

            seekBar.setProgress(0);
            seekBar.setMax(mediaPlayer.getDuration());

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("MediaPlayer", "Error setting data source: " + e.getMessage());
            // You may want to show an error message to the user here
        }


        if (isRepeatEnabled) {
            mediaPlayer.setLooping(true);
            repeatButton.setImageResource(R.drawable.repeate_one);
        } else {
            mediaPlayer.setLooping(false);
            repeatButton.setImageResource(R.drawable.repeate_all);
        }

        if (isRandomEnabled) {
            shuffleButton.setImageResource(R.drawable.shuffle_true);
        } else {
            shuffleButton.setImageResource(R.drawable.shuffle);
        }
    }



    private void updateRecentSongs() {
        File file = new File(getBaseContext().getFilesDir(), "recentSongs.txt");

        ArrayList<String> recentSongIDs = readRecentSongIDs(file);

        String currentSongID = currentSong.getID();

        if (recentSongIDs.contains(currentSongID)) {
            recentSongIDs.remove(currentSongID);
            recentSongIDs.add(0, currentSongID);
        } else {
            recentSongIDs.add(0, currentSongID);

            if (recentSongIDs.size() > 100) {
                recentSongIDs.remove(recentSongIDs.size() - 1);
            }
        }
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(recentSongIDs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> readRecentSongIDs(File file) {
        ArrayList<String> recentSongIDs = new ArrayList<>();

        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                Object obj = ois.readObject();
                if (obj instanceof ArrayList) {
                    recentSongIDs = (ArrayList<String>) obj;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return recentSongIDs;
    }


    public void playNextSong() {
        mediaSessionCallback.onSkip(isRandomEnabled, sizeList);
        mediaSession = new MediaSessionCompat(this, "MusicPlayerSession");
        setResourcesWithMusic();
        playMusic();
    }

    private void addSeconds() {
        int currentPosition = mediaPlayer.getCurrentPosition();
        int newPosition = currentPosition + SEEK_FORWARD_MILLIS;

        int duration = mediaPlayer.getDuration();
        if (newPosition > duration) {
            newPosition = duration;
        }

        mediaPlayer.seekTo(newPosition);
//        mediaSessionCallback.useUpdateSeekBar();
    }

    public void playPreviousSong() {
        mediaSessionCallback.onPrevious(sizeList);
        mediaSession = new MediaSessionCompat(this, "MusicPlayerSession");
        setResourcesWithMusic();
        playMusic();
    }



    public void pausePlay() {
        mediaSessionCallback.onPause();
        updateNotification();
    }

    private int darkenColor(int color, float darknessLevel) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= darknessLevel;
        return Color.HSVToColor(hsv);
    }

    private static String convertToMMSS(String duration) {
        long millis = Long.parseLong(duration);
        return String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }

    private void updateNotification() {
        Intent serviceIntent = new Intent(MusicPlayerActivity.this, PlaybackService.class);
        serviceIntent.putExtra("SONG_ID", currentSong.getID());
        serviceIntent.putExtra("SONG_TITLE", currentSong.getTitle());
        serviceIntent.putExtra("SONG_ARTIST", currentSong.getArtist());
        serviceIntent.putExtra("SONG_BACKGROUND", currentSong.getID());
        serviceIntent.putExtra("SIZE_LIST", sizeList);
        serviceIntent.putExtra("isRandom", isRandomEnabled);
        ContextCompat.startForegroundService(MusicPlayerActivity.this, serviceIntent);
    }

    private void addRemoveLikeSong() {
        mediaSession.getController().getTransportControls().sendCustomAction("com.example.FAVORITE", createFavoriteActionExtras(currentSong.getID(), currentSong.getTitle()));
        if(!mediaSessionCallback.isCurrentSongFavorite(currentSong.getID())){
            likeButton.setImageResource(R.drawable.favorite_icon_fill);
        }else{
            likeButton.setImageResource(R.drawable.favorite_icon);
        }
        updateNotification();
    }
    private Bundle createFavoriteActionExtras(String currentSongId, String currentSongName) {
        Bundle extras = new Bundle();
        extras.putString("currentSongId", currentSongId);
        extras.putString("currentSongName", currentSongName);
        return extras;
    }



    private void showDetailsBottomSheet() {
        BottomSheetDialog detailDialog = new BottomSheetDialog(MusicPlayerActivity.this);
        detailDialog.setContentView(R.layout.bottom_dialog_details_track);
        TextView trackNameDetail = detailDialog.findViewById(R.id.trackNameDetail);
        trackNameDetail.setText(currentSong.getTitle());
        detailDialog.show();
    }
    private void showDetailsBottomSheetLyrics() {
        // Assuming you have a function to fetch lyrics based on the current media
        String lyrics = fetchLyricsForCurrentMedia(); // Implement this function

        BottomSheetDialog detailDialog = new BottomSheetDialog(MusicPlayerActivity.this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_dialog_details_track_lyrics, null);
        TextView lyricsTextView = bottomSheetView.findViewById(R.id.lyricsTextView);

        // Check if lyrics are available and set them
        if (lyrics != null && !lyrics.isEmpty()) {
            lyricsTextView.setText(lyrics);
        } else {
            lyricsTextView.setText("Lyrics not available");
        }

        detailDialog.setContentView(bottomSheetView);
        detailDialog.show();
    }

    // Function to fetch lyrics based on the current media (replace this with your logic)
    private String fetchLyricsForCurrentMedia() {
        // Implement your logic to fetch lyrics for the currently playing media
        // For example, you might retrieve lyrics from a database or server
        // Return the lyrics as a string
        return "These are the lyrics for the current media.";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (playNextReceiver != null) {
            unregisterReceiver(playNextReceiver);
        }
        if (playPreviousReceiver != null) {
            unregisterReceiver(playPreviousReceiver);
        }

        mediaSession.setCallback(null);
        mediaSession.setActive(false);
        mediaSession.release();
    }
}
