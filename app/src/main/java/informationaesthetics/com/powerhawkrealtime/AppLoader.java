package informationaesthetics.com.powerhawkrealtime;

import android.content.Intent;
import android.os.Build;
import android.os.StrictMode;

import com.google.firebase.database.FirebaseDatabase;

import java.lang.reflect.Method;

import informationaesthetics.com.powerhawkrealtime.Data.FirebaseDatabaseData;
import informationaesthetics.com.powerhawkrealtime.Data.UnitData;
import informationaesthetics.com.powerhawkrealtime.Data.UserData;
import informationaesthetics.com.powerhawkrealtime.Utilities.FragmentUtilities;
import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

/**
 * Created by quinnfagan on 2018-02-05.
 */

public class AppLoader extends android.app.Application {
    private static AppLoader instance;
    public static AppLoader get() { return instance; }

    @Override
    public void onCreate() {
        super.onCreate();
        initSingletons();
    }

    /**
     * Initializes singletons which will be tied to the class loader for the Application.
     */
    private void initSingletons() {
        UserData.init(getApplicationContext());
        Utilities.init(getApplicationContext());
        FirebaseDatabaseData.init();
        UnitData.init();
        FragmentUtilities.init();

        if(Build.VERSION.SDK_INT>=24){
            try{
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
