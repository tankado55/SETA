package beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TaxiList {

    private ArrayList<TaxiInfo> taxiInfoList;

    public TaxiList() {
        taxiInfoList = new ArrayList<>();
    }

    public TaxiList(ArrayList<TaxiInfo> list){
        taxiInfoList = list;
    }

    public List<TaxiInfo> getTaxiInfoList() {
        return taxiInfoList;
    }

    public void setTaxiInfoList(ArrayList<TaxiInfo> taxiInfoList) {
        this.taxiInfoList = taxiInfoList;
    }
}
