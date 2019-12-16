package mr777nick.smartgarbagemonitoring.activities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import mr777nick.smartgarbagemonitoring.R;
import mr777nick.smartgarbagemonitoring.helpers.MQTTHelper;

public class MonitorActivity extends AppCompatActivity {

    MQTTHelper mqttHelper;

    private int mInterval = 5000; // 5 seconds by default, can be changed later
    private Handler mHandler;

    String pesan;
    String[] separatedPesan;

    WebView webView;
    //SwipeRefreshLayout swipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

//        swipe = (SwipeRefreshLayout) findViewById(R.id.swipe);
//        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                LoadWeb();
//            }
//        });

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.bn_main);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //thread.start();

        LoadWeb();
//        swipe.setEnabled(false);

        startMqtt();

        mHandler = new Handler();
        startRepeatingTask();
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                if (separatedPesan != null){
                    Double doublePesan = Double.parseDouble(separatedPesan[1]);
                    if (doublePesan > 66) {
                        showNotification(getApplicationContext(), "Trash Can " + separatedPesan[0] + " is full!", "Contains " + separatedPesan[1] + "% capacity. Please transport the garbage.", getIntent());
                    }
                }
                //startMqtt();
                //CheckingForNotifications();
                //updateStatus(); //this function can change value of mInterval.
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment fragment;
            switch (item.getItemId()) {
                case R.id.refresh_menu:
                    LoadWeb();
                    return true;
                case R.id.logout_menu:
                    SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("currentlyLoggedIn", "false");
                    editor.apply();
                    Intent intentLogin = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intentLogin);
                    return true;
            }
            return false;
        }
    };

    private void startMqtt() {
        Log.w("Debug", "startMqtt() started!");
        mqttHelper = new MQTTHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Debug", mqttMessage.toString());
                pesan = mqttMessage.toString();
                Log.w("Testing data", pesan);

                separatedPesan = pesan.split(":");
                Log.w("Test Lagi", separatedPesan[1]);

                Double doublePesan = Double.parseDouble(separatedPesan[1]);
                if (doublePesan > 66) {
                    showNotification(getApplicationContext(), "Trash Can " + separatedPesan[0] + " is full!", "Contains " + separatedPesan[1] + "% capacity. Please transport the garbage.", getIntent());
                    Log.d("Debug notif", "Harusnya muncul notif");
                }


            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    private void showNotif(String judul, String isi) {
        NotificationManager notificationManager;

        Intent mIntent = new Intent(this, MonitorActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("fromnotif", "notif");
        mIntent.putExtras(bundle);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setColor(getResources().getColor(R.color.colorAccent));
        builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_launcher_background)
                //.setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher_background))
                .setTicker("notif starting")
                .setAutoCancel(true)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .setLights(Color.RED, 3000, 3000)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setContentTitle(judul)
                .setContentText(isi);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(115, builder.build());
    }

    public void showNotification(Context context, String title, String body, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 1;
        String channelId = "channel-01";
        String channelName = "Channel Name";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));


        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(notificationId, mBuilder.build());
    }





//    private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;
//
//    @Override
//    public void onStart() {
//        super.onStart();
//
//        mOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
//            @Override
//            public void onScrollChanged() {
//                int scrollY = webView.getScrollY();
//                if (scrollY == 0)
//                    swipe.setEnabled(true);
//                else
//                    swipe.setEnabled(false);
//
//            }
//        };
//        swipe.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener);
//    }
//
//    @Override
//    public void onStop() {
//        swipe.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
//        super.onStop();
//    }

    public void LoadWeb(){
        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.loadUrl("https://demo.thingsboard.io/dashboard/cbfb0fd0-c1c4-11e9-9890-a9cdbff27d94?publicId=e83557b0-bf11-11e9-9890-a9cdbff27d94");
        //swipe.setRefreshing(true);

//        webView.setWebViewClient(new WebViewClient(){
//            public void onReceivedError(WebView view, int errorCode,
//                                        String description, String failingUrl) {
//                webView.loadUrl("file:///android_asset/error.html");
//            }
//            public  void  onPageFinished(WebView view, String url){
//                //ketika loading selesai, ison loading akan hilang
//                swipe.setRefreshing(false);
//            }
//        });

//        webView.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public void onProgressChanged(WebView view, int newProgress) {
//                //loading akan jalan lagi ketika masuk link lain
//                // dan akan berhenti saat loading selesai
//                if(webView.getProgress()== 100){
//                    swipe.setRefreshing(false);
//                } else {
//                    swipe.setRefreshing(true);
//                }
//            }
//        });

    }

    @Override
    public void onBackPressed(){

        if (webView.canGoBack()){
            webView.goBack();
        }else {
            //finish();
        }
    }



}

