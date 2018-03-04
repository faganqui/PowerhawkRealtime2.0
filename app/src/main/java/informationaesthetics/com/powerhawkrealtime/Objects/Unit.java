package informationaesthetics.com.powerhawkrealtime.Objects;

import java.util.ArrayList;
import java.util.HashMap;

import informationaesthetics.com.powerhawkrealtime.Data.FirebaseDatabaseData;
import informationaesthetics.com.powerhawkrealtime.Interfaces.DataLoded;
import informationaesthetics.com.powerhawkrealtime.MainActivity;
import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

/**
 * Created by quinnfagan on 2018-02-08.
 */

public class Unit implements DataLoded {

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    private String keyName;

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    private String lastUpdated;

    public Meter[] getMeters() {
        return meters;
    }

    public void setMeters(Meter[] meters) {
        this.meters = meters;
    }

    private Meter[] meters;

    public ArrayList<Meter[]> getOldMeters() { return oldMeters; }

    public void setOldMeters(ArrayList<Meter[]> oldMeters) { this.oldMeters = oldMeters; }

    private ArrayList<Meter[]> oldMeters;

    public Unit(){}

    public int getUnitId() {
        return unitId;
    }

    public void setUnitId(int unitId) {
        this.unitId = unitId;
    }

    private int unitId;

    private int rowId;

    public int getRowId(){
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public float getUnitTotalWatts(){
        float watts = 0;
        for (Meter meter: meters){
            watts += meter.getMeterTotalWatts();
        }
        return watts;
    }
    public float getUnitTotalVar(){
        float var = 0;
        for (Meter meter: meters){
            var += meter.getMeterTotalVar();
        }
        return var;
    }
    public float getUnitTotalVa(){
        float va = 0;
        for (Meter meter: meters){
            va += meter.getMeterTotalVa();
        }
        return va;
    }
    public float getUnitTotalWattsReceived(){
        float watts = 0;
        for (Meter meter: meters){
            watts += meter.getMeterTotalWattsReceived();
        }
        return watts;
    }
    public float getUnitTotalVarReceived(){
        float var = 0;
        for (Meter meter: meters){
            var += meter.getMeterTotalVarReceived();
        }
        return var;
    }
    public float getUnitTotalVaH(){
        float vah = 0;
        for (Meter meter: meters){
            vah += meter.getMeterTotalVaH();
        }
        return vah;
    }

    @Override
    public void dataLoaded(Object lodedData) {
        if ( oldMeters == null ){ oldMeters = new ArrayList<>(); }
        Utilities.getInstance().getMainActivity().refreshPages();

        Object units = lodedData;
        HashMap<String, Object> unitArray = (HashMap<String, Object>) units;

        ArrayList<Meter> meters = new ArrayList<>();
        Meter[] oldMetersArray = getMeters();
        int index = 0;
        for (Object meterKey : (Object[])(unitArray).keySet().toArray()){
                if(meterKey.equals("date")){
                this.setLastUpdated((String) unitArray.get(meterKey));
            }else{
                Meter meter = new Meter();
                meter.setStats((HashMap<String, String>)unitArray.get(meterKey));
                meter.setIndex(index);
                meter.setRowId(oldMetersArray[index].getRowId());
                meters.add(meter);
                index ++;
            }
        }

        oldMeters.add(getMeters());
        setOldMeters(oldMeters);
        this.setMeters(meters.toArray(new Meter[meters.size()]));
    }
}
