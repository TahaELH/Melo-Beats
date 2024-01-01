package com.example.melobeats;

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
import android.media.RemoteControlClient;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
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

// Import statements...

public class PlaybackService extends Service {

    public static final String CHANNEL_ID = "MusicPlaybackChannel";
    public static final int NOTIFICATION_ID = 1;

    public String currentSongTitle, currentSongArtist;
    public String currentSongBackground;

    public MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
    public PowerManager.WakeLock wakeLock;
    public MediaSessionCompat mediaSession;


    @Override
    public void onCreate() {
        super.onCreate();
        initializeMediaSession();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);

        if ("PLAY_PAUSE_NOTIFICATION".equals(intent.getAction())) {
            togglePlayback();
        } else if ("CANCEL_NOTIFICATION".equals(intent.getAction())) {
            stopService();
        } else if ("PREVIOUS_NOTIFICATION".equals(intent.getAction())) {
            Intent playPreviousIntent = new Intent("PLAY_PREVIOUS");
            sendBroadcast(playPreviousIntent);
        } else if ("NEXT_NOTIFICATION".equals(intent.getAction())) {
            Intent playNextIntent = new Intent("PLAY_NEXT");
            sendBroadcast(playNextIntent);
        }

        return START_STICKY;
    }



    private void initializeMediaSession() {
        mediaSession = new MediaSessionCompat(this, "MusicPlaybackSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setCallback(new MediaSessionCallback());

        // Initialize the RemoteControlClient for API 21 and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            @SuppressLint("UnspecifiedImmutableFlag") RemoteControlClient remoteControlClient = new RemoteControlClient(
                    PendingIntent.getBroadcast(
                            this,
                            0,
                            new Intent(Intent.ACTION_MEDIA_BUTTON),
                            0
                    )
            );
            remoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                            RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_STOP |
                            RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                            RemoteControlClient.FLAG_KEY_MEDIA_NEXT
            );
            audioManager.registerRemoteControlClient(remoteControlClient);
        }
    }



    private void handleIntent(Intent intent) {
        if (intent != null) {
            String title = intent.getStringExtra("SONG_TITLE");
            String artist = intent.getStringExtra("SONG_ARTIST");
            String background = intent.getStringExtra("SONG_BACKGROUND");

            if (title != null && artist != null && background != null) {
                currentSongTitle = title;
                currentSongArtist = artist;
                currentSongBackground = background;

                // Add this line to force an update when handling the intent
                updateNotification();
            }
        }
    }


    private void togglePlayback() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
        updateNotification();
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

        Intent previousIntent = new Intent(this, PlaybackService.class);
        previousIntent.setAction("PREVIOUS_NOTIFICATION");
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 1, previousIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, PlaybackService.class);
        playPauseIntent.setAction("PLAY_PAUSE_NOTIFICATION");
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, PlaybackService.class);
        nextIntent.setAction("NEXT_NOTIFICATION");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent cancelIntent = new Intent(this, PlaybackService.class);
        cancelIntent.setAction("CANCEL_NOTIFICATION");
        PendingIntent cancelPendingIntent = PendingIntent.getService(this, 4, cancelIntent, PendingIntent.FLAG_IMMUTABLE);

        String folderName = "Music/.thumbnails";
        String imageName = currentSongBackground + ".jpg";
        File folder = new File(Environment.getExternalStorageDirectory(), folderName);

        if (folder.exists()) {
            File imageFile = new File(folder, imageName);

            int maxProgress = mediaPlayer.getDuration();
            int currentProgress = mediaPlayer.getCurrentPosition();


            @SuppressLint("WrongConstant") NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.vector)
                    .setContentIntent(pendingIntent)
                    .addAction(R.drawable.previous, "PREVIOUS", previousPendingIntent)
                    .addAction(mediaPlayer.isPlaying() ? R.drawable.pause_fill : R.drawable.play_fill, "Play/Pause", playPausePendingIntent)
                    .addAction(R.drawable.next, "NEXT", nextPendingIntent)
                    .addAction(R.drawable.delete_track, "Cancel", cancelPendingIntent)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(1)
                    )
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

    private void updateNotification() {
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback Channel",
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

    public class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
            }
        }

        @Override
        public void onPause() {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
            }
        }



        public void updatePlaybackState(int state) {
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(state, mediaPlayer.getCurrentPosition(), 1.0f)
                    .build());
        }
    }
}