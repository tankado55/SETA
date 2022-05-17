package beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TaxisRegistrationInfo {

    @XmlElement(name="initial_position")
    private Position myStartingPosition;

    @XmlElement(name="taxi_list")
    private List<TaxiInfo> taxiInfoList;

    public TaxisRegistrationInfo() {
        myStartingPosition = new Position();
        taxiInfoList = new ArrayList<>();
    }

    public TaxisRegistrationInfo(Position start, List<TaxiInfo> ts){
        myStartingPosition = start;
        taxiInfoList = ts;
    }

    // getters e Setters
    public Position getMyStartingPosition() {
        return myStartingPosition;
    }

    public void setMyStartingPosition(Position myStartingPosition) {
        this.myStartingPosition = myStartingPosition;
    }

    public List<TaxiInfo> getTaxiInfoList() {
        return taxiInfoList;
    }

    public void setTaxiInfoList(List<TaxiInfo> taxiInfoList) {
        this.taxiInfoList = taxiInfoList;
    }
}
