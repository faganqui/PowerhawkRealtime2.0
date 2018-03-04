package informationaesthetics.com.powerhawkrealtime.Data;

import java.util.ArrayList;
import java.util.HashMap;

import informationaesthetics.com.powerhawkrealtime.Objects.Meter;
import informationaesthetics.com.powerhawkrealtime.Objects.Unit;

/**
 * Created by quinnfagan on 2018-02-08.
 */

public class UnitData {

    private static volatile UnitData mInstance;
    private static Unit[] mUnits;

    private UnitData(){}

    public static void init(){
        mInstance = new UnitData();
    }

    public static UnitData getInstance(){
        return mInstance;
    }

    public Unit[] getUnits(){
        return mUnits;
    }

    public void setUnits(Unit[] units){
        mUnits = units;
    }

    public void initilizeData(HashMap<String, HashMap<String,Object>> unitArray) {
        if(unitArray == null){
            setUnits(null);
            return;
        }
        ArrayList<Unit> units = new ArrayList<>();
        for (Object unitKey : (Object [])unitArray.keySet().toArray()){
            Unit unitUnit = new Unit();
            unitUnit.setKeyName((String)unitKey);
            ArrayList<Meter> meters = new ArrayList<>();
            int meterIndex = 0;
            for (Object meterKey : (unitArray.get(unitKey)).keySet().toArray()){
                if(meterKey.equals("date")){
                    unitUnit.setLastUpdated((String)(unitArray.get(unitKey)).get(meterKey));
                }else{
                    Meter meter = new Meter();
                    meter.setStats((HashMap<String, String>)(unitArray.get(unitKey)).get(meterKey));
                    meter.setIndex(meterIndex);
                    meters.add(meter);
                    meterIndex++;
                }
            }
            unitUnit.setMeters(meters.toArray(new Meter[meters.size()]));
            String unitDatabaseKey = UserData.getInstance().getSelectedServer().getsIP().replace(".","") + "/" + unitUnit.getKeyName();
            FirebaseDatabaseData.getInstance().getObjects(unitDatabaseKey, unitUnit);
            units.add(unitUnit);
        }
        setUnits(units.toArray(new Unit[units.size()]));
    }

    public float getTotalWatts(){
        float total = 0;
        for (Unit unit : mUnits){
            total += unit.getUnitTotalWatts();
        }
        return total;
    }

    public float getTotalVar(){
        float total = 0;
        for (Unit unit : mUnits){
            total += unit.getUnitTotalVar();
        }
        return total;
    }

    public float getTotalVa(){
        float total = 0;
        for (Unit unit : mUnits){
            total += unit.getUnitTotalVa();
        }
        return total;
    }

    public float getTotalWattsReceived(){
        float total = 0;
        for (Unit unit : mUnits){
            total += unit.getUnitTotalWattsReceived();
        }
        return total;
    }

    public float getTotalVarReceived(){
        float total = 0;
        for (Unit unit : mUnits){
            total += unit.getUnitTotalVarReceived();
        }
        return total;
    }

    public float getTotalVaH(){
        float total = 0;
        for (Unit unit : mUnits){
            total += unit.getUnitTotalVaH();
        }
        return total;
    }

    private int statType;

    public void setStatType(int statType) {
        this.statType = statType;
    }

    public int getStatType(){
        return statType;
    }
}
