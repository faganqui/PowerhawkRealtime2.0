package informationaesthetics.powerhawkrt;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FirebaseDatabaseCollection extends Service {

    public FirebaseDatabaseCollection() {
    }

    public void onStartCommand(){

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }



}
