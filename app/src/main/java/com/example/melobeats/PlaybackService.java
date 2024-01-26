package com.example.melobeats;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;

// Import statements...

public class PlaybackService extends Service implements AudioManager.OnAudioFocusChangeListener {

    PlaybackStateCompat.Builder stateBuilder;

    private static final String FAVORITE = "com.example.FAVORITE";

    File file;


    public static final String CHANNEL_ID = "MusicPlaybackChannel";
    public static final int NOTIFICATION_ID = 1;

    public String currentSongTitle, currentSongArtist, currentSongID;

    public int songSizeList;
    public String currentSongBackground;

    public MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
    public PowerManager.WakeLock wakeLock;
    public MediaSessionCompat mediaSession;

    private testCallback mediaSessionCallback;

    boolean isRandom;

    private AudioManager audioManager;



    private static final int AUDIO_FOCUS_GAIN = AudioManager.AUDIOFOCUS_GAIN;
    private static final int AUDIO_FOCUS_LOSS_TRANSIENT = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
    private static final int AUDIO_FOCUS_LOSS_TRANSIENT_CAN_DUCK = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;





    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    // Add this method to update the notification
    public void updateNotification() {
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Notification notification = createNotification();
        if (notification != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        if ("CANCEL_NOTIFICATION".equals(intent.getAction())) {
            abandonAudioFocus();
            stopService();
        } else {
            initializeMediaSession();
            startForeground(NOTIFICATION_ID, createNotification());
            requestAudioFocus();
        }
        return START_STICKY;
    }



    @SuppressLint("WrongConstant")
    private void initializeMediaSession() {
        file = new File(this.getFilesDir(), "likeSongs.txt");
        mediaSession = new MediaSessionCompat(this, "MusicPlaybackSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSessionCallback = new testCallback(mediaSession, file, isRandom, songSizeList);
        stateBuilder = new PlaybackStateCompat.Builder();
        androidx.media.app.NotificationCompat.MediaStyle style = new androidx.media.app.NotificationCompat.MediaStyle();

        mediaPlayer = MyMediaPlayer.getInstance();
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration());
        mediaSession.setMetadata(builder.build());

        PlaybackStateCompat.CustomAction favoriteAction = new PlaybackStateCompat.CustomAction.Builder(
                "com.example.FAVORITE",
                "Favorite",
                mediaSessionCallback.isCurrentSongFavorite(currentSongID) ? R.drawable.favorite_icon_fill : R.drawable.favorite_icon
        )
                .build();

        // Add the custom action to the state builder
        stateBuilder.addCustomAction(favoriteAction);

        mediaSession.getController().getTransportControls().sendCustomAction("com.example.FAVORITE", createFavoriteActionExtras(currentSongID, currentSongTitle));

        // Set the playback state
        int state = mediaPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        long position = mediaPlayer.getCurrentPosition();
        float playbackSpeed = 1f;
        stateBuilder.setState(state, position, playbackSpeed);

        // Set the actions for the media session, including the custom action
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SEEK_TO |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS); // Include the custom action

        // Set the media session
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setCallback(mediaSessionCallback);
        mediaSession.setActive(true);

        // Set the media session token to the notification
        style.setMediaSession(mediaSession.getSessionToken());

        // Set the media session token to the notification
        mediaSession.setMediaButtonReceiver(null);
        mediaSession.setActive(true);
    }

    private Bundle createFavoriteActionExtras(String currentSongId, String currentSongName) {
        Bundle extras = new Bundle();
        extras.putString("currentSongId", currentSongId);
        extras.putString("currentSongName", currentSongName);
        return extras;
    }





    private void handleIntent(Intent intent) {
        if (intent != null) {
            String title = intent.getStringExtra("SONG_TITLE");
            String artist = intent.getStringExtra("SONG_ARTIST");
            String background = intent.getStringExtra("SONG_BACKGROUND");
            int size = intent.getIntExtra("SIZE_LIST", 0);
            String id = intent.getStringExtra("SONG_ID");
            boolean isR = intent.getBooleanExtra("isRandom", false);

            if (title != null && artist != null && background != null && id != null) {
                currentSongTitle = title;
                currentSongArtist = artist;
                currentSongBackground = background;
                songSizeList = size;
                currentSongID = id;
                isRandom = isR;

                initializeMediaSession(); // Move the call here

                // Add this line to force an update when handling the intent
                updateNotification();
            }
        }
    }




    private void stopService() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        stopForeground(true);
        stopSelf();
    }

    public Notification createNotification() {
        Intent notificationIntent = new Intent(this, MusicPlayerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);


        String folderName = "Music/.thumbnails";
        String imageName = currentSongBackground + ".jpg";
        File folder = new File(Environment.getExternalStorageDirectory(), folderName);

        if (folder.exists()) {
            File imageFile = new File(folder, imageName);



            @SuppressLint("WrongConstant") NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.vector)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowCancelButton(true)
                    )
                    .setOnlyAlertOnce(true)
                    .setContentText(currentSongArtist != null ? currentSongArtist : "none")
                    .setContentTitle(currentSongTitle != null ? currentSongTitle : "title none");
            Glide.with(this)
                    .asBitmap()
                    .load(imageFile)
                    .apply(new RequestOptions().override(320, 250))
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            builder.setLargeIcon(resource);
                            if (ActivityCompat.checkSelfPermission(PlaybackService.this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            NotificationManagerCompat.from(PlaybackService.this).notify(NOTIFICATION_ID, builder.build());
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Implementation if needed
                        }
                    });

            return builder.build();
        }

        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Melo Beats",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel for music playback");
            channel.setSound(null, null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                channel.setImportance(NotificationManager.IMPORTANCE_LOW);
            }

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void requestAudioFocus() {
        int result = audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
    }

    private void abandonAudioFocus() {
        audioManager.abandonAudioFocus(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AUDIO_FOCUS_GAIN:
                // Gradually increase the volume when audio focus is regained
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    fadeVolume(mediaPlayer, 0.1f, 1.0f, 500, false); // Adjust the duration as needed
                    initializeMediaSession();
                    updateNotification();
                    createNotification();

                }
                break;
            case AUDIO_FOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Gradually lower the volume when another app temporarily interrupts playback
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    fadeVolume(mediaPlayer, 1.0f, 0.1f, 500, false); // Adjust the duration as needed
                }
                break;
            case AUDIO_FOCUS_LOSS_TRANSIENT:
                // Gradually pause playback when another app interrupts playback
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    fadeVolume(mediaPlayer, 1.0f, 0.0f, 500, true); // Pause at the end of the animation
                }
                break;
        }
    }
    private void fadeVolume(MediaPlayer mediaPlayer, float startVolume, float endVolume, long duration, final boolean pauseAtEnd) {
        final float[] volume = {startVolume};
        ValueAnimator volumeAnimator = ValueAnimator.ofFloat(startVolume, endVolume);
        volumeAnimator.setDuration(duration);
        volumeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                volume[0] = (float) animation.getAnimatedValue();
                mediaPlayer.setVolume(volume[0], volume[0]);
            }
        });
        if (pauseAtEnd) {
            volumeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        initializeMediaSession();
                        updateNotification();
                        createNotification();
                    }
                }
            });
        }
        volumeAnimator.start();
    }






    //callback------------------------------------------------------------------------------------------------







    class testCallback extends MediaSessionCompat.Callback {
        private MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();

        private MediaSessionCompat mediaSession;


        private File file;


        private boolean isRandomEnabled;
        private int songSizeList1;
        public testCallback(MediaSessionCompat mediaSession, File file, boolean isRandomEnabled, int songSizeList1) {
            this.mediaSession = mediaSession;
            this.file = file;
            this.isRandomEnabled = isRandomEnabled;
            this.songSizeList1 = songSizeList1;
        }
        @Override
        public void onSeekTo(long pos) {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo((int) pos);

                // Check if looping is enabled and the playback position is at the end
                if (mediaPlayer.isLooping() && pos >= mediaPlayer.getDuration()) {
                    // If looping is enabled and seeking beyond the end, reset playback
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                } else {
                    // Update the seek bar UI with the current position on the main thread
                    new Handler(Looper.getMainLooper()).post(() -> updateSeekBarUI(mediaPlayer.getCurrentPosition()));
                }
            }
        }

        public void updatenot(){
            initializeMediaSession();
            updateNotification();
            createNotification();
        }



        @Override
        public void onPlay() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                // Start a handler to update the seek bar
                useUpdateSeekBar();
            } else {
                onPause(); // If already playing, pause
            }
            updatenot();
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
            }else {
                onPlay();
            }
            updatenot();
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

        public void onSkipToNext() {
            if (MyMediaPlayer.currentIndex >= songSizeList1 - 1) {
                MyMediaPlayer.currentIndex = 0;
            } else {
                if (isRandom && songSizeList1 > 1) {
                    Random random = new Random();
                    int randomIndex;

                    do {
                        randomIndex = random.nextInt(songSizeList1);
                    } while (randomIndex == MyMediaPlayer.currentIndex);

                    MyMediaPlayer.currentIndex = randomIndex;
                } else {
                    MyMediaPlayer.currentIndex++;
                }
            }
            mediaPlayer.reset();
        }

        public void onSkipToPrevious() {
            if (!mediaPlayer.isLooping()) {
                if (MyMediaPlayer.currentIndex == 0 || MyMediaPlayer.currentIndex < 0) {
                    MyMediaPlayer.currentIndex = songSizeList1 - 2;
                } else {
                    MyMediaPlayer.currentIndex -= 2;
                }
            }
            mediaPlayer.reset();
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
                if(currentSongId != null){
                    addRemoveLikeSong(currentSongId);
                }else {
                    addRemoveLikeSong(currentSongID);
                }
                updatenot();
            }
        }
    }
}