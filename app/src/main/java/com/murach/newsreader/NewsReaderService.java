package com.murach.newsreader;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class NewsReaderService extends Service {

    public static final String TAG = "News reader";
    private NewsReaderApp app;
    private Timer timer;
    private FileIO io;
    
    @Override
    public void onCreate() {
        Log.d(TAG, "Service created");
        app = (NewsReaderApp) getApplication();
        io = new FileIO(getApplicationContext());
        startTimer();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound - not used!");
        return null;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        stopTimer();
    }
    
    private void startTimer() {
        TimerTask task = new TimerTask() {
            
            @Override
            public void run() {
                Log.d(TAG, "Timer task started");
                
                io.downloadFile();
                Log.d(TAG, "File downloaded");
                
                RSSFeed newFeed = io.readFile();
                Log.d(TAG, "File read");
                
                // if new feed is newer than old feed
                if (newFeed.getPubDateMillis() > app.getFeedMillis()) {
                    Log.d(TAG, "Updated feed available.");
                    
                    // update app object
                    app.setFeedMillis(newFeed.getPubDateMillis());
                    
                    // display notification
                    sendNotification("Select to view updated feed.");
                }
                else {
                    Log.d(TAG, "Updated feed NOT available.");

                    //display notification
                    sendNotification("Updated feed NOT available.");
                }
                
            }
        };
        
        timer = new Timer(true);
        int delay = 1000 * 5;      // 5 seconds
        int interval = 1000 * 60 * 60;   // 1 hour
        timer.schedule(task, delay, interval);
    }
    
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }
     
    private void sendNotification(String text) {
        // create the intent for the notification
        Intent notificationIntent = new Intent(this, ItemsActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // create the pending intent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = 
                PendingIntent.getActivity(this, 0, notificationIntent, flags);
        
        // create the variables for the notification
        int icon = R.drawable.ic_launcher;
        CharSequence tickerText = "Updated news feed is available";
        CharSequence contentTitle = getText(R.string.app_name);
        CharSequence contentText = text;

        // create the notification and set its data
        Notification notification = 
                new NotificationCompat.Builder(this)
            .setSmallIcon(icon)
            .setTicker(tickerText)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build();
        
        // display the notification
        NotificationManager manager = (NotificationManager) 
                getSystemService(NOTIFICATION_SERVICE);
        final int NOTIFICATION_ID = 1;
        manager.notify(NOTIFICATION_ID, notification);
    }
}