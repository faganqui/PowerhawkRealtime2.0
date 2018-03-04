package informationaesthetics.com.powerhawkrealtime.Data;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import informationaesthetics.com.powerhawkrealtime.Interfaces.DataLoded;

/**
 * Created by quinnfagan on 2018-02-07.
 */

public class FirebaseDatabaseData {

    private static volatile FirebaseDatabaseData mInstance;
    private static FirebaseDatabase database;
    private Object receivedObject;

    private FirebaseDatabaseData() {
        database = FirebaseDatabase.getInstance();
    }

    public static FirebaseDatabaseData getInstance() {
        return mInstance;
    }

    public static final void init() {
        mInstance = new FirebaseDatabaseData();
    }

    public void setObjectValue(String referencekey, Object object) {
        if(object == null) database.getReference(UserData.getInstance().getUser().getUid()+referencekey).removeValue();
        database.getReference(UserData.getInstance().getUser().getUid()+referencekey).setValue(object);
    }

    public void getObject(String referenceKey, final DataLoded loader) {

        database.getReference(referenceKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                receivedObject = dataSnapshot.getValue();
                loader.dataLoaded(receivedObject);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                loader.dataLoaded(null);
            }
        });
    }

    public void getObjects(String referenceKey, final DataLoded loader) {

        database.getReference(referenceKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                receivedObject = dataSnapshot.getValue();
                loader.dataLoaded(receivedObject);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                loader.dataLoaded(null);
            }
        });
    }

    public Object getLoadedData(){
        return receivedObject;
    }
}
