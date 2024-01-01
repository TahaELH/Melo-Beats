package com.example.melobeats;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MediaActions {

    private TextView titleTV;
    private TextView currentTimeTV;
    private TextView artistTV;
    private TextView albumTV;
    private TextView totalTimeTV;
    private SeekBar seekBar;
    private ImageButton pausePlay;
    private ImageButton previousBtn;
    private ImageView detailsTrack;
    private ImageButton nextBtn;
    private ImageButton repeatImageButton;
    private ImageButton aleatoireImageButton;
    private ImageButton returnBtn;
    private ImageView imageView;
    private ImageButton imageConvertBtn;
    private LinearLayout backpage;

    private MediaPlayer mediaPlayer;
    private ArrayList<AudioModel> songsList;
    private boolean isRepeatEnabled = false;
    private boolean isRandomEnabled = false;

    private Context context;

    AudioModel currentSong;

    MusicPlayerActivity musicplayeract = new MusicPlayerActivity();


    public MediaActions(Context context, MediaPlayer mediaPlayer, ArrayList<AudioModel> songsList,
                        TextView titleTV, TextView currentTimeTV, TextView artistTV,
                        TextView albumTV, TextView totalTimeTV, SeekBar seekBar,
                        ImageButton pausePlay, ImageButton previousBtn,
                        ImageView detailsTrack, ImageButton nextBtn,
                        ImageButton repeatImageButton, ImageButton aleatoireImageButton,
                        ImageButton returnBtn, ImageView imageView,
                        ImageButton imageConvertBtn, LinearLayout backpage) {
        this.context = context;
        this.mediaPlayer = mediaPlayer;
        this.songsList = songsList;
        this.titleTV = titleTV;
        this.currentTimeTV = currentTimeTV;
        this.artistTV = artistTV;
        this.albumTV = albumTV;
        this.totalTimeTV = totalTimeTV;
        this.seekBar = seekBar;
        this.pausePlay = pausePlay;
        this.previousBtn = previousBtn;
        this.detailsTrack = detailsTrack;
        this.nextBtn = nextBtn;
        this.repeatImageButton = repeatImageButton;
        this.aleatoireImageButton = aleatoireImageButton;
        this.returnBtn = returnBtn;
        this.imageView = imageView;
        this.imageConvertBtn = imageConvertBtn;
        this.backpage = backpage;
        currentSong = songsList.get(MyMediaPlayer.currentIndex);
    }


    public void playMusic() {
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (isRepeatEnabled) {
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();

                } else {
//                    MusicPlayerActivity.playNextSong();
                }
            }
        });
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(currentSong.getPath());
            mediaPlayer.prepare();

            mediaPlayer.start();
            seekBar.setProgress(0);
            seekBar.setMax(mediaPlayer.getDuration());

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Update UI based on random and repeat modes
        if (isRepeatEnabled) {
            mediaPlayer.setLooping(true);
            repeatImageButton.setImageResource(R.drawable.repeate_one);
        } else {
            mediaPlayer.setLooping(false);
            repeatImageButton.setImageResource(R.drawable.repeate_all);
        }

        if (isRandomEnabled) {
            aleatoireImageButton.setImageResource(R.drawable.shuffle_true);
        } else {
            aleatoireImageButton.setImageResource(R.drawable.shuffle);
        }
    }


    public void addSeconds() {
        int currentPosition = mediaPlayer.getCurrentPosition();
        int newPosition = currentPosition + 5000; // Add 5 seconds (5000 milliseconds)

        // Make sure the new position does not exceed the duration of the media
        int duration = mediaPlayer.getDuration();
        if (newPosition > duration) {
            newPosition = duration;
        }

        mediaPlayer.seekTo(newPosition);
    }



    public void pausePlay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
//        musicplayeract.updateNotification();
    }

    public void toggleRepeat() {
        isRepeatEnabled = !isRepeatEnabled;

        if (isRepeatEnabled) {
            mediaPlayer.setLooping(true);
            repeatImageButton.setImageResource(R.drawable.repeate_one);
        } else {
            mediaPlayer.setLooping(false);
            repeatImageButton.setImageResource(R.drawable.repeate_all);
        }

        if (isRandomEnabled) {
            aleatoireImageButton.setImageResource(R.drawable.shuffle_true);
            Random random = new Random();
            int randomIndex;
            do {
                randomIndex = random.nextInt(songsList.size());
            } while (randomIndex == MyMediaPlayer.currentIndex);
            MyMediaPlayer.currentIndex = randomIndex;
        } else {
            aleatoireImageButton.setImageResource(R.drawable.shuffle);
        }
    }

    // Corrected toggleRandom method
    public void toggleRandom() {
        isRandomEnabled = !isRandomEnabled;

        if (isRandomEnabled) {
            mediaPlayer.setLooping(true);
            aleatoireImageButton.setImageResource(R.drawable.shuffle_true);
            Random random = new Random();
            int randomIndex;
            do {
                randomIndex = random.nextInt(songsList.size());
            } while (randomIndex == MyMediaPlayer.currentIndex);
            MyMediaPlayer.currentIndex = randomIndex;
        } else {
            mediaPlayer.setLooping(false);
            aleatoireImageButton.setImageResource(R.drawable.shuffle);
        }
    }

    public void updateNotification(Context context) {
        if(context != null){
            Intent serviceIntent = new Intent(context, PlaybackService.class);
            if (serviceIntent != null) {
                serviceIntent.putExtra("SONG_TITLE", currentSong.getTitle());
                serviceIntent.putExtra("SONG_ARTIST", currentSong.getArtist());
                serviceIntent.putExtra("SONG_BACKGROUND", currentSong.getID());

                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
        Log.e("NOTIFICATION", currentSong.getTitle() + "  " + currentSong.getArtist() + "  " + currentSong.getID());
    }





    int darkenColor(int color, float darknessLevel) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= darknessLevel;
        return Color.HSVToColor(hsv);
    }



    public static String convertToMMSS(String duration) {
        long millis = Long.parseLong(duration);
        String formattedDuration = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));

        return formattedDuration;
    }

}
