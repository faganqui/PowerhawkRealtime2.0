package informationaesthetics.com.powerhawkrealtime.Objects;

import java.util.ArrayList;
import java.util.HashMap;

import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

/**
 * Created by quinnfagan on 2018-02-08.
 */

public class Meter {

    public HashMap<String, String> getStats() {
        return stats;
    }

    public void setStats(HashMap<String, String> stats) {
        this.stats = stats;
        buildMeterPoints();
    }

    private HashMap<String, String> stats;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    private int index;

    public Meter(){
    }

    public String getName(){
        return stats.get("name");
    }

    private int rowId;

    private String[] digits = {"1","2","3","5","6","7","8","9","0"};

    public ArrayList<HashMap<String, String>> getMeterPoints() {
        return meterPoints;
    }

    public void setMeterPoints(ArrayList<HashMap<String, String>> meterPoints) {
        this.meterPoints = meterPoints;
    }

    private ArrayList<HashMap<String,String>> meterPoints = new ArrayList<>();

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public int getRowId(){
        return rowId;
    }

    public float getMeterTotalWatts() {
        float mWatts = 0;
        try {
            mWatts = Float.valueOf(stats.get("watts"));
        }catch (Exception e){
            mWatts = Float.valueOf(stats.get("w"));
        }
        return mWatts;
    }

    public float getMeterTotalVar() {
        return Float.valueOf(stats.get("var"));
    }

    public float getMeterTotalVa() {
        return Float.valueOf(stats.get("va"));
    }

    public float getMeterTotalWattsReceived() {
        return Float.valueOf(stats.get("kwhreceived"));
    }

    public float getMeterTotalVarReceived() {
        return Float.valueOf(stats.get("kvarhreceived"));
    }

    public void buildMeterPoints(){
        HashMap<Integer, ArrayList<String>> indexedKeys = new HashMap<>();
        for (Object meterPointKey : (Object[]) this.getStats().keySet().toArray()) {
            // if the key contains a digit then its a meter point
            if (Utilities.getInstance().doesStringContainObjectInArray((String)meterPointKey, digits)){
                ArrayList<String> x;
                if (indexedKeys.get(Integer.parseInt(meterPointKey.toString().replaceAll("[\\D]", ""))) == null){
                    x = new ArrayList<>();
                } else {
                    int index = Integer.parseInt(meterPointKey.toString().replaceAll("[\\D]", ""));
                    x = indexedKeys.get(index);
                }
                x.add(meterPointKey.toString());
                indexedKeys.put(Integer.parseInt(meterPointKey.toString().replaceAll("[\\D]", "")), x);
            } else {
                // otherwise its a total for the meter so we add it to 0 index
                ArrayList<String> x;
                if(indexedKeys.get(0) == null){
                    x = new ArrayList<>();
                } else {
                    x = indexedKeys.get(0);
                }
                x.add(meterPointKey.toString());
                indexedKeys.put(0, x);
            }
        }
        for (Object key : indexedKeys.keySet().toArray()){
            HashMap<String, String> returnSublist = new HashMap<>();
            for (String stringKey : indexedKeys.get(key)){
                returnSublist.put(stringKey, this.getStats().get(stringKey));
            }
            meterPoints.add(returnSublist);
        }
    }

    public float getMeterTotalVaH() {
        return Float.valueOf(stats.get("kvah"));
    }
}
