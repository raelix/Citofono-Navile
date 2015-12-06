package citofono.navile.com.citofono;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import citofono.navile.com.citofono.data.Preferences;

/**
 * Created by raeli on 08/11/2015.
 */
public class Services extends Service implements OnBellRingListener,OnMotionListener,OnCloseMotionListener,View.OnTouchListener, OnConfigurationChangeListener {
   public static final int minBrightness = 10;
    public static final int maxBrightness = 255;
    public static final int autoDisableMills = 90000;
    //NOTE 3 Positions
    public static final String noteDev = "SM-N9005";
    public static final String removeErrorDev ="/system/bin/input tap 252 477\n";
    public static final String startAppDev ="/system/bin/input tap 600 500\n";
    public static final String openAppDev ="/system/bin/input tap 584 880\n";
    public static final String closeAppHorizontalDev ="/system/bin/input tap 360 980\n";
    public static final String closeAppVerticalDev ="/system/bin/input tap 255 1226\n";
    public static final int openWidth = 230;
    public static final int openHeight = 220;
    public static final int closeWidth = 230;
    public static final int closeHeight = 220;
    public static final int borderWidth = 250;
    public static final int borderHeight = 150;
    //Tablet Positions
    public static final String startApp ="/system/bin/input tap 262 142\n";
    public static final String openApp ="/system/bin/input tap 246 297\n";
    public static final String closeAppHorizontal ="/system/bin/input tap 181 441\n";
    public static final String closeAppVertical ="/system/bin/input tap 125 488\n";
    public static final String removeError ="/system/bin/input tap 252 477\n";
    long timeElapsed = -1;
    private PowerManager.WakeLock wl;
    private Handler handler;
    private AudioAnalyzer audioAnalyzer;
    private Detector detector;
    private Delayer delayer;
    private boolean bellRing;
    boolean closeComm;
    boolean inCall;
    boolean manualStart;
    boolean manualStop;
    boolean inMove;
    boolean motions;
    //  Button moveButton;

    CustomBorderSquare squareButton;
    Button openButton;
    Button closeButton;
    private WindowManager.LayoutParams openParams;
    private WindowManager.LayoutParams closeParams;
    // private WindowManager.LayoutParams moveParams;
    private WindowManager.LayoutParams borderParams;
    private WindowManager windowManager;
    private int millsBeforeShutdownNumber;
    private int thresholdNumber;
    private boolean sensors;
    boolean detectorStarted;

    public Services() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }



    @Override
    public void onCreate() {
        Toast.makeText(this, "Il Servizio e' stato avviato", Toast.LENGTH_LONG).show();
        loadConfiguration();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        handler = new Handler();
        handler.postDelayed(executor, 150);

        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNjfdhotDimScreen");
        wl.acquire();
        delayer = new Delayer(this, millsBeforeShutdownNumber);
        if(motions) {
            detector = new Detector();
        }
        if(sensors) {
            audioAnalyzer = new AudioAnalyzer(this, thresholdNumber);
            audioAnalyzer.measureStart();
        }
        initView();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Toast.makeText(this, " Servizio Avviato", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Servizio Distrutto", Toast.LENGTH_LONG).show();
        wl.release();
        if (closeButton != null)
            windowManager.removeView(closeButton);
        if (openButton != null)
            windowManager.removeView(openButton);
        // if (moveButton != null)
        //     windowManager.removeView(moveButton);
        if(detectorStarted)
            stopDetector();
    }

    public void loadConfiguration(){
        SharedPreferences preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        millsBeforeShutdownNumber = preferences.getInt(Preferences.millsBeforeShutdown, -1);
        thresholdNumber = preferences.getInt(Preferences.threshold, -1);
        sensors = preferences.getBoolean(Preferences.sensors, false);
        motions = preferences.getBoolean(Preferences.motions, false);
        System.out.println("readed mills " + millsBeforeShutdownNumber);
        System.out.println("readed threshold " + thresholdNumber);
        if(millsBeforeShutdownNumber == -1 && thresholdNumber == -1){
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(Preferences.millsBeforeShutdown,15000);
            editor.putInt(Preferences.threshold, 13);
            editor.commit();
        }
    }

    public void setConfiguration(String param, int value){
        SharedPreferences preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(param, value);
        editor.commit();
    }

    private void initView(){
        openButton = new Button(this);
        squareButton = new CustomBorderSquare(this, 0, 0, borderWidth, borderHeight);
        //moveButton = new Button(this);
        closeButton = new Button(this);
        openButton.setText("APRI");
        closeButton.setText("CHIUDI");
        //moveButton.setText("MUOVI");
        openButton.setBackgroundColor(Color.GREEN);
        openButton.setTextColor(Color.WHITE);
        closeButton.setBackgroundColor(Color.RED);
        closeButton.setTextColor(Color.WHITE);
        openButton.setOnTouchListener(this);
        closeButton.setOnTouchListener(this);

        // moveButton.setOnTouchListener(this);
        GradientDrawable gdDefaultOpen = new GradientDrawable();
        gdDefaultOpen.setColor(Color.GREEN);
        gdDefaultOpen.setCornerRadius(360);
        gdDefaultOpen.setStroke(10, Color.parseColor("#BDBDBD"));
        GradientDrawable gdDefaultClose = new GradientDrawable();
        gdDefaultClose.setColor(Color.RED);
        gdDefaultClose.setCornerRadius(360);
        gdDefaultClose.setStroke(10, Color.parseColor("#BDBDBD"));
        openButton.setBackgroundDrawable(gdDefaultOpen);
        closeButton.setBackgroundDrawable(gdDefaultClose);
        openParams= new WindowManager.LayoutParams(
                openWidth,
                openHeight,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        closeParams = new WindowManager.LayoutParams(
                closeWidth,
                closeHeight,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        borderParams = new WindowManager.LayoutParams(
                borderWidth,
                borderHeight,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);

      /*  moveParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);*/


        borderParams.gravity =  Gravity.RIGHT;
        openParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
        closeParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        //moveParams.gravity = Gravity.TOP | Gravity.CENTER;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        System.out.println("Size " + point);
        borderParams.y = (int) (point.y / 8.2);
        squareButton.setOnTouchListener(this);

        windowManager.addView(openButton, openParams);
        windowManager.addView(squareButton, borderParams);
        windowManager.addView(closeButton, closeParams);
        squareButton.setVisibility(View.GONE);
        //  windowManager.addView(moveButton, moveParams);

    }

    public void executeCommand(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd);
            os.writeBytes("exit\n");
            os.flush();
            os.close();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startApplication(String packageName){
        try
        {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            List<ResolveInfo> resolveInfoList = getPackageManager().queryIntentActivities(intent, 0);

            for(ResolveInfo info : resolveInfoList)
                if(info.activityInfo.packageName.equalsIgnoreCase(packageName))
                {
                    launchComponent(info.activityInfo.packageName, info.activityInfo.name);
                    return;
                }

            // No match, so application is not installed
            showInMarket(packageName);
        }
        catch (Exception e)
        {
            showInMarket(packageName);
        }
    }

    private void launchComponent(String packageName, String name) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setComponent(new ComponentName(packageName, name));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }

    private void showInMarket(String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void bellRing() {
        bellRing = true;
    }

    @Override
    public void onMotionFound() {
        System.out.println("Motion Found!");
        if(inCall)
            refreshTimer();
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        String phrase = "";
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase += Character.toUpperCase(c);
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase += c;
        }
        return phrase;
    }
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    public boolean isInDevMode(){
        return getDeviceName().contains(noteDev);
    }

    final Runnable executor = new Runnable() {
        public void run() {
            if(bellRing || manualStart){

                manualStop = false;
                android.provider.Settings.System.putInt(getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS, maxBrightness);

                if(sensors)
                    audioAnalyzer.measureStop();

                if (!isSkyBellApplicationOpen()) {
                    startSkyBellApplication();
                    waitMills(10000);
                } else {
                    //startSkyBellApplication();
                    //waitMills(1000);
                }
                if(!isInDevMode()) {
                    executeCommand(removeError);
                    waitMills(300);
                    executeCommand(startApp);
                    waitMills(500);
                    executeCommand(openApp);
                }
                else{
                    executeCommand(startAppDev);
                    waitMills(500);
                    executeCommand(openAppDev);
                }
                inCall = true;
                squareButton.setVisibility(View.VISIBLE);
                if (!detectorStarted && motions) {
                    detector.onStart(Services.this, Services.this);
                    detectorStarted = true;
                }
                // else
                //    resumeDetector();
                if(motions)
                    refreshTimer();
                else {
                    delayer.setDefaultTimeBeforeFireEvent(autoDisableMills);
                    refreshTimer();
                }
                bellRing = false;
                manualStart = false;
            }

            if(!inCall && sensors){
                audioAnalyzer.doUpdate();
            }

            if((manualStop || closeComm) && inCall){
                inCall = false;
                closeComm = false;
                manualStop = false;
                manualStart = false;
                if(!isInDevMode())
                    executeCommand(closeAppVertical);
                else
                    executeCommand(closeAppVerticalDev);
                //  stopDetector();
                squareButton.setVisibility(View.GONE);
                if(sensors)
                    audioAnalyzer.measureStart();

                android.provider.Settings.System.putInt(getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS, minBrightness);
            }

            handler.postDelayed(this, 250); // amount of delay between every cycle of volume level detection + sending the data
        }
    };




    private void startSkyBellApplication(){
        startApplication("com.skybell");
    }

    private boolean isSkyBellApplicationOpen(){
        ActivityManager activityManager = (ActivityManager) Services.this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        boolean isOpen = false;
        for(int i = 0; i < procInfos.size(); i++){
            System.out.println("process analizzo "+procInfos.get(i).processName);
            if(procInfos.get(i).processName.equals("com.skybell")){
                isOpen = true;
            }
        }
        return isOpen;
    }

    private void waitMills(int mills){
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }






    public void stopDetector(){
        detector.onPause();
    }

    public void resumeDetector(){
        detector.onResume();
    }


    private void refreshTimer(){
        delayer.restartTimer();
    }

    @Override
    public void closeCommunication() {
        if(inCall)
            closeComm = true;
    }

    @Override
    public void incrementThreshold() {
        audioAnalyzer.incrementThreshold();
        setConfiguration(Preferences.threshold, audioAnalyzer.getThreshold());
        //Toast.makeText(this.getApplicationContext(), "Threshold incremented", Toast.LENGTH_SHORT).show();
    }

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Button view = null;
        WindowManager.LayoutParams params = null;
    if(v == squareButton)
        return true;

        if (v == openButton) {
            view = openButton;
            params = openParams;
        }
        else if (v == closeButton){
            view = closeButton;
            params = closeParams;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(view == null || params == null)
                    return false;
                if (inMove){
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (v == openButton) {
                    if(!inMove)
                        manualStart = true;
                }
                else if (v == closeButton) {
                    if(!inMove)
                        manualStop = true;
                }
             /*   if (v == moveButton) {
                    if (inMove) {
                        System.out.println("inMove false");
                        inMove = false;
                    } else {
                        System.out.println("inMove true");
                        inMove = true;
                    }
                    return false;
                }*/
                return true;
            case MotionEvent.ACTION_MOVE:
                if(view == null || params == null)
                    return false;
                if (inMove) {
                    params.x = initialX
                            + (int) (event.getRawX() - initialTouchX);

                    params.y = initialY
                            + (int) (event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(view, params);
                }
        }
        return true;
    }

    @Override
    public void configurationChanged() {
        loadConfiguration();
        delayer.setDefaultTimeBeforeFireEvent(millsBeforeShutdownNumber);
        audioAnalyzer.changeThreshold(thresholdNumber);
    }
}
