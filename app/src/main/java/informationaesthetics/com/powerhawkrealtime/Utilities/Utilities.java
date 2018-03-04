package informationaesthetics.com.powerhawkrealtime.Utilities;

import android.app.AlarmManager;
import android.support.v4.app.FragmentManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import informationaesthetics.com.powerhawkrealtime.Fragments.ServerFragment;
import informationaesthetics.com.powerhawkrealtime.MainActivity;

/**
 * Created by quinnfagan on 2018-01-30.
 */

public class Utilities {

    private static final String TAG = "Utils";
    private static volatile Utilities mInstance;
    private static Context mContext;
    private static MainActivity mainActivity;
    private static FragmentManager fMan;

    public Utilities(){
    }

    public MainActivity getMainActivity() {
        return mainActivity;
    }

    public void setMainActivity(MainActivity mainActivity) {
        Utilities.mainActivity = mainActivity;
    }

    public ServerFragment getServerFragment(){
        return mainActivity.getServerFragment();
    }

    public static void init(Context context){
        mInstance = new Utilities();
        mContext = context;
    }

    public static Utilities getInstance(){
        if(mInstance == null) return null;
        return mInstance;
    }

    public FragmentManager getFragmentManager(){
        return fMan;
    }

    public void setFragmentManager(FragmentManager fMan){
        this.fMan = fMan;
    }

    public void makeToast(final MainActivity parent, final String text){
        parent.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(parent.getBaseContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

    public Context getApplicationContext(){
        return mContext;
    }

    public void Restart(){
        Intent mStartActivity = new Intent(mContext, MainActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(mContext, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.MILLISECONDS.sleep(175);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Interupt in restart");
                    e.printStackTrace();
                }
                System.exit(0);

            }
        });
        t.start();
    }

    public Boolean doesStringContainObjectInArray(String string, String[] array){
        for (String s : array){
            if (string.contains(s)){
                return true;
            }
        }
        return false;
    }

    public int getDp(float dp){
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        float fpixels = metrics.density * dp;
        return (int) (fpixels + 0.5f);
    }
}
