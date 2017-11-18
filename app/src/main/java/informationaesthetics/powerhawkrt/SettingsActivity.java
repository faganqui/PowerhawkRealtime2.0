package informationaesthetics.powerhawkrt;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static final String SHARED_PREFS = "POWERHAWK_URL_SAVED_PREFS";

    //collected from shared prefs
    CharSequence[] meters_list;
    CharSequence[] stats_list;
    String the_url;
    String the_user_id;
    String server_ip;

    //collected from server
    ArrayList<String> rule_list = new ArrayList<>();

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        getSharedPrefs();
    }

    @Override
    protected void onResume() {
        new getRuleList().execute(server_ip);
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        new getRuleList().execute(server_ip);
        super.onBackPressed();
    }

    @Override
    public void onHeaderClick(Header header, int position) {


        // Here's an example

        if(header.fragmentArguments == null)
        {
            header.fragmentArguments = new Bundle();
        }

        header.fragmentArguments.putCharSequenceArray("meterst", meters_list);
        header.fragmentArguments.putCharSequenceArray("statst", stats_list);
        header.fragmentArguments.putString("user", the_user_id);
        header.fragmentArguments.putString("url", the_url);
        header.fragmentArguments.putString("serverIP", server_ip);
        header.fragmentArguments.putStringArray("rules", rule_list.toArray(new String[rule_list.size()]));

        super.onHeaderClick(header, position);

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName)
                || AddNewRulePreferenceFragment.class.getName().equals(fragmentName)
                || RemoveRulePreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("example_list"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AddNewRulePreferenceFragment extends PreferenceFragment {

        String userId;
        String cur_url;
        String cur_server;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_addnewrule);
            setHasOptionsMenu(true);

            /*
            THIS SETS UP THE LIST PREFERENCES FOR METERS, STATS, and CURRENT RULES
            this is because they must be set dynamically as the content of the list changes
            */
            Bundle bundle1 = getArguments();

            cur_url = bundle1.getString("url");
            userId = bundle1.getString("user");
            cur_server = bundle1.getString("serverIP");

            //sets up the button controls
            Preference button = findPreference(getString(R.string.add_rule_button));
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SharedPreferences preferencemng = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    String sel_meter = preferencemng.getString("meters_list_pref", "");
                    String sel_stat = preferencemng.getString("stats_list_pref", "");
                    String sel_min = preferencemng.getString("range_min_text", "");
                    String sel_max = preferencemng.getString("range_max_text", "");
                    String sel_label = preferencemng.getString("label_text", "");
                    String out_text = String.format("%1$s-%2$s;%3$s;%4$s^^%5$s", sel_min, sel_max, sel_meter, sel_stat,sel_label);
                    new connectToServer().execute(cur_server,"alarm", out_text);
                    getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                    getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
                    return true;
                }
            });

            //sets up list preferences
            final ListPreference listPreference = (ListPreference) findPreference("meters_list_pref");
            final ListPreference listPreferenceStats = (ListPreference) findPreference("stats_list_pref");

            CharSequence[] meter_titles = bundle1.getCharSequenceArray("meterst");
            CharSequence[] stat_titles = bundle1.getCharSequenceArray("statst");

            listPreference.setEntries(meter_titles);
            listPreferenceStats.setEntries(stat_titles);
            //make meter values array
            ArrayList<String> values = new ArrayList<>();
            ArrayList<String> valuestats = new ArrayList<>();
            for(int i = 0; i < meter_titles.length; i++){
                values.add(String.valueOf(i));
            }
            //make stat values array
            for(int i = 0; i < stat_titles.length; i++){
                valuestats.add(String.valueOf(i));
            }
            listPreference.setEntryValues(values.toArray(new CharSequence[meter_titles.length]));
            listPreferenceStats.setEntryValues(valuestats.toArray(new CharSequence[stat_titles.length]));


            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("range_max_text"));
            bindPreferenceSummaryToValue(findPreference("range_min_text"));
            bindPreferenceSummaryToValue(findPreference("label_text"));
            bindPreferenceSummaryToValue(listPreference);
            bindPreferenceSummaryToValue(listPreferenceStats);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        private class connectToServer extends AsyncTask<String,Void,String> {

            @Override
            protected String doInBackground(String... strings) {
                String dataString = "";

                try {
                    //connection refused because its not local host its an emulator, try connecting via my IP
                    Socket soc = new Socket(strings[0], 9002);

                    if(strings[1] == "alarm") {

                        DataOutputStream dout = new DataOutputStream(soc.getOutputStream());
                        //sends all the things needed too keep track of alarm
                        dout.writeUTF("alarm_"  + userId + "_" + strings[2] + "_" + cur_url);
                        dout.flush();

                        BufferedReader in = new BufferedReader(new InputStreamReader(
                                soc.getInputStream()));

                        BufferedReader stdIn =
                                new BufferedReader(
                                        new InputStreamReader(System.in));

                        String userInput;

                        StringBuilder sb = new StringBuilder();
                        while ((userInput = in.readLine()) != null) {
                            sb.append(userInput);
                        }
                        userInput = sb.toString();

                        dataString += userInput;

                        dout.close();
                        in.close();
                        soc.close();
                    }
                } catch (Exception e) {
                    dataString += e;
                    e.printStackTrace();
                }

                //collectedString = dataString;
                return ("MESSAGE: " + dataString);
            }


        }

    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class RemoveRulePreferenceFragment extends PreferenceFragment {

        String[] rule_list;
        String theserver;
        String url;
        String UserId;
        Bundle bundle1;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_removerule);
            setHasOptionsMenu(true);

            bundle1 = getArguments();
            theserver = bundle1.getString("serverIP");
            url = bundle1.getString("url");
            UserId = bundle1.getString("user");

            //sets up the button controls
            Preference button = findPreference(getString(R.string.remove_rule_button));
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    SharedPreferences preferencemng = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    String sel_label = preferencemng.getString("remove_list_pref", "");

                    CharSequence[] temp = bundle1.getCharSequenceArray("rules");

                    new connectToServer().execute(theserver,String.valueOf(temp[Integer.valueOf(sel_label)]));

                    getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                    getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
                    return true;
                }
            });

            //sets up list preferences
            final ListPreference listPreference = (ListPreference) findPreference("remove_list_pref");

            CharSequence[] meter_rules = bundle1.getCharSequenceArray("rules");

            listPreference.setEntries(meter_rules);
            //make meter values array
            ArrayList<String> values = new ArrayList<>();
            for(int i = 0; i < meter_rules.length; i++){
                values.add(String.valueOf(i));
            }
            listPreference.setEntryValues(values.toArray(new CharSequence[meter_rules.length]));


            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("remove_list_pref"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        private class connectToServer extends AsyncTask<String,Void,String> {

            @Override
            protected String doInBackground(String... strings) {
                String dataString = "";

                try {
                    //connection refused because its not local host its an emulator, try connecting via my IP
                    Socket soc = new Socket(strings[0], 9002);


                    DataOutputStream dout = new DataOutputStream(soc.getOutputStream());
                    //sends all the things needed too keep track of alarm
                    dout.writeUTF("removealarm_"  + strings[1] + "_" + url + "_" + UserId );
                    dout.flush();

                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            soc.getInputStream()));

                    BufferedReader stdIn =
                            new BufferedReader(
                                    new InputStreamReader(System.in));

                    String userInput;

                    StringBuilder sb = new StringBuilder();
                    while ((userInput = in.readLine()) != null) {
                        sb.append(userInput);
                    }
                    userInput = sb.toString();

                    dataString += userInput;

                    dout.close();
                    in.close();
                    soc.close();

                } catch (Exception e) {
                    dataString += e;
                    e.printStackTrace();
                }

                //collectedString = dataString;
                return ("MESSAGE: " + dataString);
            }
        }

    }

    public void getSharedPrefs(){
        SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFS,Context.MODE_PRIVATE);
        the_user_id = sharedPref.getString("userid", "guest");
        the_url = sharedPref.getString("cururl", "");
        server_ip = sharedPref.getString("curserver", "");
        String temp_meters_list = sharedPref.getString(the_user_id +the_url+"m","");
        String temp_stats_list = sharedPref.getString(the_user_id+the_url+"s","");
        ArrayList<String> MeterTitles = new ArrayList<>();
        ArrayList<String> StatTitles = new ArrayList<>();

        for(String element : temp_meters_list.split(";")){
            if(element!=""){
                MeterTitles.add(element);
            }
        }
        for(String element : temp_stats_list.split(";")){
            if(element!=""){
                StatTitles.add(element);
            }
        }

        meters_list = MeterTitles.toArray(new CharSequence[MeterTitles.size()]);
        stats_list = StatTitles.toArray(new CharSequence[StatTitles.size()]);
    }

    public void savePrefs(String key, String value){
        SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFS,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
        return;
    }

    private class getRuleList extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            String dataString = "";

            try {
                //connection refused because its not local host its an emulator, try connecting via my IP
                Socket soc = new Socket(strings[0], 9002);

                DataOutputStream dout = new DataOutputStream(soc.getOutputStream());
                //sends all the things needed too keep track of alarm
                dout.writeUTF("getlabel_"  + the_user_id + "_" + the_url);
                dout.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        soc.getInputStream()));

                BufferedReader stdIn =
                        new BufferedReader(
                                new InputStreamReader(System.in));

                String userInput;

                StringBuilder sb = new StringBuilder();
                while ((userInput = in.readLine()) != null) {
                    sb.append(userInput);
                }
                userInput = sb.toString();

                dataString += userInput;

                rule_list = new ArrayList<>();
                String[] temp_list = dataString.split("\\^");

                for(String label : temp_list){
                    rule_list.add(label);
                }

                dout.close();
                in.close();
                soc.close();

            } catch (Exception e) {
                dataString += e;
                e.printStackTrace();
            }

            //collectedString = dataString;
            return ("MESSAGE: " + dataString);
        }


    }
}
