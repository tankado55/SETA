package adminServer;

import beans.TaxiInfo;
import exceptions.taxi.IdAlreadyPresentException;
import exceptions.taxi.TaxiNotPresentException;

import java.util.ArrayList;
import java.util.List;


public class TaxisInfoManager {

    private ArrayList<TaxiInfo> taxiInfoList;

    private static TaxisInfoManager instance;

    private TaxisInfoManager() {
        taxiInfoList = new ArrayList<TaxiInfo>();}

    //singleton
    public synchronized static TaxisInfoManager getInstance(){
        if(instance==null)
            instance = new TaxisInfoManager();
        return instance;
    }

    public synchronized List<TaxiInfo> add(TaxiInfo taxiInfo) throws IdAlreadyPresentException {
        // check if taxi id is already present
        for (TaxiInfo t : taxiInfoList) {
            if (t.getId().equals(taxiInfo.getId())){
                throw new IdAlreadyPresentException();
            }
        }
        // not present
        List<TaxiInfo> oldTaxiInfoList = new ArrayList<TaxiInfo>(taxiInfoList);
        taxiInfoList.add(taxiInfo);
        return oldTaxiInfoList;
    }

    public synchronized void delete(String id) throws TaxiNotPresentException {
        TaxiInfo toRemove = null;
        for (TaxiInfo info : taxiInfoList){
            if (info.getId().equals(id))
                toRemove = info;
        }
        if (toRemove != null)
            taxiInfoList.remove(toRemove);
        else{
            throw new TaxiNotPresentException();
        }
    }

    public ArrayList<TaxiInfo> getTaxiInfoList() {
        return taxiInfoList;
    }
}
