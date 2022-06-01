package adminServer;

import beans.TaxiInfo;
import exceptions.taxi.IdAlreadyPresentException;
import exceptions.taxi.TaxiNotPresentException;

import java.util.ArrayList;
import java.util.List;


public class TaxisInfoManager {

    private List<TaxiInfo> taxiInfoList;

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
        if (taxiInfoList.contains(id))
            taxiInfoList.remove(id);
        else{
            throw new TaxiNotPresentException();
        }
    }

}
