package com.example.melobeats;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {

    private static final int DELAY_MILLIS = 100;
    private static final int SEEK_FORWARD_MILLIS = 5000;

    // UI elements
    private TextView titleTextView, currentTimeTextView, totalTimeTextView, artistTextView, albumTextView;
    private SeekBar seekBar;
    private ImageButton pausePlayButton, previousButton, nextButton, repeatButton, shuffleButton, returnButton, imageConvertButton, likeButton;
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

    private BroadcastReceiver playNextReceiver;
    private BroadcastReceiver playPreviousReceiver;


    ArrayList<AudioModel> songsList;

    ArrayList<AudioModel> retrievedAlbumList;

    private MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();

    @SuppressLint({"MissingInflatedId", "ResourceType"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );


        File albumFile = new File(getBaseContext().getFilesDir(), "sonList.txt");
        retrievedAlbumList = null;
        try (FileInputStream fis = new FileInputStream(albumFile);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            retrievedAlbumList = (ArrayList<AudioModel>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }




        isRepeatEnabled = mediaPlayer.isLooping();

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

        IntentFilter nextFilter = new IntentFilter("PLAY_NEXT");
        IntentFilter previousFilter = new IntentFilter("PLAY_PREVIOUS");

        registerReceiver(playNextReceiver, nextFilter);
        registerReceiver(playPreviousReceiver, previousFilter);


        setContentView(R.layout.activity_music_player);

        initializeUI(); // Ensure that UI elements are initialized first
        setListeners();
        setResourcesWithMusic(); // Now you can safely use UI elements
        updateUI();

        runOnUiThread(() -> {
            updateSeekBarAndTime();
            new Handler().postDelayed(() -> updateUI(), DELAY_MILLIS);
        });

        sizeList = getIntent().getIntExtra("SIZE", 0);



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
        imageConvertButton = findViewById(R.id.imageConvert);
        primaryColorLayout = findViewById(R.id.changeprimarycolor);
        likeButton = findViewById(R.id.likeCurrentTrack);

        // Get the song list from the intent
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
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });

        // Set click listeners for buttons
        pausePlayButton.setOnClickListener(v -> pausePlay());
        nextButton.setOnClickListener(v -> playNextSong());
        nextButton.setOnFocusChangeListener((View v, boolean hasFocus) -> addSeconds());
        previousButton.setOnClickListener(v -> playPreviousSong());
        repeatButton.setOnClickListener(v -> toggleRepeat());
        shuffleButton.setOnClickListener(v -> toggleRandom());
        returnButton.setOnClickListener(view -> onBackPressed());
        imageConvertButton.setOnClickListener(view -> changeAlbumArt());
        detailsTrackImageView.setOnClickListener(view -> showDetailsBottomSheet());
        likeButton.setOnClickListener(view -> addRemoveLikeSong());
    }

    private void setResourcesWithMusic() {
//        (ArrayList<AudioModel>) getIntent().getSerializableExtra("LIST")
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

        updateNotification();

        setContent(currentSong);

        if(isSongLiked(currentSong.getID())){
            likeButton.setImageResource(R.drawable.heartliked);
        }else{
            likeButton.setImageResource(R.drawable.heart);
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

    void setContent(AudioModel crtsong){
        if(crtsong != null){
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
                                primaryColorLayout.setBackgroundColor(darkerColor);
                                primaryColor = darkerColor;
                            }
                        }
                    });

                    imageConvertButton.setVisibility(View.GONE);
                } else {
                    albumArtImageView.setImageResource(R.drawable.logo1);
                    imageConvertButton.setVisibility(View.VISIBLE);
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
                    pausePlayButton.setImageResource(R.drawable.stop_current);
                } else {
                    pausePlayButton.setImageResource(R.drawable.play_current);
                }
            }
            new Handler().postDelayed(() -> updateUI(), DELAY_MILLIS);
        });
    }

    private void updateSeekBarAndTime() {
        seekBar.setMax(mediaPlayer.getDuration());
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
        currentTimeTextView.setText(convertToMMSS(String.valueOf(mediaPlayer.getCurrentPosition())));

        if (mediaPlayer.isPlaying()) {
            pausePlayButton.setImageResource(R.drawable.stop_current);
        } else {
            pausePlayButton.setImageResource(R.drawable.play_current);
        }

        new Handler().postDelayed(this::updateSeekBarAndTime, DELAY_MILLIS);
    }

    private void playMusic() {
        mediaPlayer.setOnCompletionListener(mediaPlayer -> {
            if (isRepeatEnabled) {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            } else {
                playNextSong();
            }
        });

        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(currentSong.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            updateRecentSongs();


            seekBar.setProgress(0);
            seekBar.setMax(mediaPlayer.getDuration());

        } catch (IOException e) {
            e.printStackTrace();
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


    void playNextSong() {
        if (MyMediaPlayer.currentIndex == sizeList - 1) {
            if (isRepeatEnabled) {
                MyMediaPlayer.currentIndex = 0;
            } else {
                return;
            }
        }else {
            if (isRandomEnabled) {
                Random random = new Random();
                int randomIndex;

                if (sizeList > 0) {
                    do {
                        randomIndex = random.nextInt(sizeList);
                    } while (randomIndex == MyMediaPlayer.currentIndex);
                    MyMediaPlayer.currentIndex = randomIndex;
                } else {
                    // Handle the case when sizeList is not positive
                    MyMediaPlayer.currentIndex = random.nextInt(songsList.size() - 1);
                    // You might want to show a message or take other appropriate action.
                }
            } else {
                MyMediaPlayer.currentIndex += 1;
            }
        }

        setResourcesWithMusic();
        mediaPlayer.reset();
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
    }

    void playPreviousSong() {
        if (MyMediaPlayer.currentIndex == 0) {
            if (isRepeatEnabled) {
                MyMediaPlayer.currentIndex = sizeList - 1;
            } else {
                return;
            }
        } else {
            MyMediaPlayer.currentIndex -= 1;
        }
        setResourcesWithMusic();
        mediaPlayer.reset();
        playMusic();
    }

    private void pausePlay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
        updateNotification();
    }

    private void toggleRepeat() {
        isRepeatEnabled = !isRepeatEnabled;

        if (isRepeatEnabled) {
            mediaPlayer.setLooping(true);
            repeatButton.setImageResource(R.drawable.repeate_one);
        } else {
            mediaPlayer.setLooping(false);
            repeatButton.setImageResource(R.drawable.repeate_all);
        }

        if (isRandomEnabled && sizeList > 0) {
            shuffleButton.setImageResource(R.drawable.shuffle_true);
            Random random = new Random();
            int randomIndex;
            do {
                randomIndex = random.nextInt(sizeList );
            } while (randomIndex == MyMediaPlayer.currentIndex);
            MyMediaPlayer.currentIndex = randomIndex;
        } else {
            shuffleButton.setImageResource(R.drawable.shuffle);
        }
    }

    private void toggleRandom() {
        isRandomEnabled = !isRandomEnabled;

        if (isRandomEnabled) {
            shuffleButton.setImageResource(R.drawable.shuffle_true);
        } else {
            shuffleButton.setImageResource(R.drawable.shuffle);
        }
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
        serviceIntent.putExtra("SONG_TITLE", currentSong.getTitle());
        serviceIntent.putExtra("SONG_ARTIST", currentSong.getArtist());
        serviceIntent.putExtra("SONG_BACKGROUND", currentSong.getID());
        ContextCompat.startForegroundService(MusicPlayerActivity.this, serviceIntent);
    }

    private void changeAlbumArt() {
        imageNumber = (imageNumber % 5) + 1;
        int drawableId = getResources().getIdentifier("logo" + imageNumber, "drawable", getPackageName());
        albumArtImageView.setImageResource(drawableId);
    }

    private void addRemoveLikeSong() {
        File file = new File(getBaseContext().getFilesDir(), "likeSongs.txt");

        // Read the existing liked song IDs from the file
        ArrayList<String> likedSongIDs = readLikedSongIDs(file);

        // Get the ID of the current song
        String currentSongID = currentSong.getID();

        // Check if the current song ID is in the liked list
        if (likedSongIDs.contains(currentSongID)) {
            // If present, remove it
            likedSongIDs.remove(currentSongID);
        } else {
            // If not present, add it
            likedSongIDs.add(0, currentSongID);
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(likedSongIDs);
            if(isSongLiked(currentSong.getID())){
                likeButton.setImageResource(R.drawable.heartliked);
            }else{
                likeButton.setImageResource(R.drawable.heart);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private boolean isSongLiked(String songID) {
        File file = new File(getBaseContext().getFilesDir(), "likeSongs.txt");

        // Read the existing liked song IDs from the file
        ArrayList<String> likedSongIDs = readLikedSongIDs(file);

        // Check if the provided song ID is in the liked list
        return likedSongIDs.contains(songID);
    }

    private ArrayList<String> readLikedSongIDs(File file) {
        ArrayList<String> likedSongIDs = new ArrayList<>();

        // Check if the file exists
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                // Read the existing liked song IDs from the file
                likedSongIDs = (ArrayList<String>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return likedSongIDs;
    }


    private void showDetailsBottomSheet() {
        BottomSheetDialog detailDialog = new BottomSheetDialog(MusicPlayerActivity.this);
        detailDialog.setContentView(R.layout.bottom_dialog_details_track);
        TextView trackNameDetail = detailDialog.findViewById(R.id.trackNameDetail);
        trackNameDetail.setText(currentSong.getTitle());
        detailDialog.show();
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
    }


}
