package informationaesthetics.powerhawkrealtime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoadFromDatabase extends AppCompatActivity {

    //Prefs
    private static final String SHARED_PREFS = "POWERHAWK_URL_SAVED_PREFS";

    String[] stat_array = new String[20];

    //get database objects
    FirebaseDatabase database;

    //objects for temporary data collection
    String result = "";
    int index = 0;
    Boolean hasNextStat = false;

    //User stats
    String userId;

    //shared preferences
    SharedPreferences sharedPref;

    //Text view for showing load percent
    TextView loadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_from_database);

        //Loading text
        loadingText = (TextView)findViewById(R.id.loading_text);

        //get prefs
        sharedPref = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);

        // Get database instance
        database = FirebaseDatabase.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //todo: figure out which stats to put in database
        // add them to stat array - also make stat array the correct length

        collectAllData();

    }

    public void collectAllData(){
        //display % of download completed
        loadingText.setText(String.format("%1$.2f",(Double.valueOf(index)/Double.valueOf(stat_array.length)) *100) + "%");

        //breaks if we've collected all the data
        if(index == stat_array.length){
            finishLoading();
            return;
        }

        //placeStatInPrefs
        if (hasNextStat){
            SharedPreferences.Editor editor = sharedPref.edit();
            //puts the read value in shared pref
            editor.putString(stat_array[index], result);
            editor.apply();
            index++;
            hasNextStat = false;
        }

        //retrieves each stat value from database
        if(index < stat_array.length) {
            getStat(stat_array[index]);
        } else {
            finishLoading();
        }
    }

    public void finishLoading(){
        Intent battle = new Intent(getBaseContext(), ServerActivity.class);
        startActivity(battle);
    }

    public void getStat(String stat){
        //sets result to the current read data
        // sets a specific statistic for a user
        DatabaseReference statData = database.getReference("/users/" + userId + "/" + stat);

        // Attach a listener to read the data at our posts reference
        statData.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                result = value;
                //calls to collect data after previous data is read
                hasNextStat = true;
                collectAllData();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });

    }

}
