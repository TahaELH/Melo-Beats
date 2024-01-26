package com.example.melobeats;

import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;

public class MusicPlayerSessionCallback extends MediaSessionCompat.Callback {

    private static final String FAVORITE = "com.example.FAVORITE";


    private MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();

    private MediaSessionCompat mediaSession;


    private File file;






    public MusicPlayerSessionCallback(MediaSessionCompat mediaSession, File file) {
        this.mediaSession = mediaSession;
        this.file = file;
    }


    @Override
    public void onSeekTo(long pos) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo((int) pos);

            // Update the playback state to reflect the new position
            updatePlaybackState();

            // Update the seek bar UI with the current position on the main thread
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    updateSeekBarUI(mediaPlayer.getCurrentPosition());
                }
            });
        }
    }



    @Override
    public void onPlay() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            // Start a handler to update the seek bar
            useUpdateSeekBar();
            updatePlaybackState();
        } else {
            onPause(); // If already playing, pause
        }
    }


    // Method to start the handler
    private void useUpdateSeekBar() {
        // Start the handler only if it's not already running
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!handler.hasCallbacks(updateSeekBar)) {
                handler.post(updateSeekBar);
            }
        }
    }



    @Override
    public void onPause() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            handler.removeCallbacks(updateSeekBar);
            updatePlaybackState();
        }else {
            onPlay();
        }
    }





    public final Handler handler = new Handler();

    public final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                // Update the seek bar progress periodically
                long currentPosition = mediaPlayer.getCurrentPosition();
                // Notify the UI to update the seek bar with the current position
                // (you may use a callback or broadcast an intent for this purpose)
                updateSeekBarUI(currentPosition);
                // Schedule the handler to run again after a delay
                handler.postDelayed(this, 100); // You can adjust the delay as needed
            } else {
                // If media is not playing, remove callbacks to stop the handler
                handler.removeCallbacks(this);
            }
        }
    };


    // Method to update the UI with the current position for the seek bar
    private void updateSeekBarUI(long currentPosition) {
        // Notify the UI to update the seek bar with the current position
        // (you may use a callback or broadcast an intent for this purpose)
        // For example, if you have a seek bar in your UI:
        PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder();
        playbackStateBuilder.setState(
                PlaybackStateCompat.STATE_PLAYING,
                currentPosition,
                1.0f
        );
        mediaSession.setPlaybackState(playbackStateBuilder.build());
    }


    public void onSkip(boolean isRandom, int sizeList) {
        if (MyMediaPlayer.currentIndex >= sizeList - 1) {
            MyMediaPlayer.currentIndex = 0;
        } else {
            if (isRandom) {
                Random random = new Random();
                int randomIndex;

                if (sizeList > 0) {
                    do {
                        randomIndex = random.nextInt(sizeList);
                    } while (randomIndex == MyMediaPlayer.currentIndex);
                    MyMediaPlayer.currentIndex = randomIndex;
                } else {
                    MyMediaPlayer.currentIndex = random.nextInt(sizeList - 1);
                    // Handle the case when sizeList is not positive
                }
            } else {
                MyMediaPlayer.currentIndex += 1;
            }
        }
        mediaPlayer = MyMediaPlayer.getInstance(); // Update the MediaPlayer instance
        updatePlaybackState();
    }

    public void onPrevious(int sizeList) {
        if (!mediaPlayer.isLooping()) {
            if (MyMediaPlayer.currentIndex == 0 || MyMediaPlayer.currentIndex < 0) {
                Log.e("NOTIFICATION", "   " + MyMediaPlayer.currentIndex);
                MyMediaPlayer.currentIndex = sizeList - 2;
            } else {
                MyMediaPlayer.currentIndex -= 1;
            }
        } else {
            mediaPlayer.seekTo(0);
        }
        mediaPlayer = MyMediaPlayer.getInstance(); // Update the MediaPlayer instance
        updatePlaybackState();
    }


    public void updatePlaybackState() {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();

        int state = mediaPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        long position = mediaPlayer.isPlaying() ? mediaPlayer.getCurrentPosition() : PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        float playbackSpeed = 1f;

        stateBuilder.setState(state, position, playbackSpeed);

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration());
        mediaSession.setMetadata(builder.build());

        // Set the supported actions based on the current state
        long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE;

        if (mediaPlayer.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }

        // Add other supported actions based on your requirements
        actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;

        stateBuilder.setActions(actions);

        // Set the playback state to the MediaSessionCompat
        mediaSession.setPlaybackState(stateBuilder.build());

        // Notify any relevant components, like the MediaSessionCompat.Callback or your UI
        mediaSession.setActive(true);
    }




    public boolean isCurrentSongFavorite(String currentSongId) {
        // Implement your logic to determine if the current song is marked as a favorite
        // You may need to keep track of favorites in your data model or database
        // For demonstration purposes, let's assume a hypothetical method isFavoriteSong(index)
        return isFavoriteSong(currentSongId);
    }

    public boolean isFavoriteSong(String index) {

        // Read the existing liked song IDs from the file
        ArrayList<String> likedSongIDs = readLikedSongIDs(file);

        // Check if the provided song ID is in the liked list
        return likedSongIDs.contains(index);
    }

    public ArrayList<String> readLikedSongIDs(File file) {
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

    public void addRemoveLikeSong(String currentSongId) {
        // Read the existing liked song IDs from the file
        ArrayList<String> likedSongIDs = readLikedSongIDs(file);

        // Get the ID of the current song
        String currentSongID = currentSongId;

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCustomAction(String action, Bundle extras) {
        if (action.equals(FAVORITE)) {
            String currentSongId = extras.getString("currentSongId");
            addRemoveLikeSong(currentSongId);
        }
    }
}
