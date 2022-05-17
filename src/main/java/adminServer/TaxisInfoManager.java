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

    // getters & setters
    public List<TaxiInfo> getTaxiList() {
        return taxiInfoList;
    }

    public synchronized void add(TaxiInfo taxiInfo) throws IdAlreadyPresentException {
        // check if taxi id is already present
        for (TaxiInfo t : taxiInfoList) {
            if (t.getId().equals(taxiInfo.getId())){
                throw new IdAlreadyPresentException();
            }
        }
        // not present
        taxiInfoList.add(taxiInfo);
    }

    public synchronized void delete(String id) throws TaxiNotPresentException {
        if (taxiInfoList.contains(id))
            taxiInfoList.remove(id);
        else{
            throw new TaxiNotPresentException();
        }
    }

}
