package beans;

import it.ewlab.ride.GreetingsServiceOuterClass;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TaxiInfo {
    private String id;
    private String ip;
    private String port;

    public TaxiInfo() {}

    public TaxiInfo(String id, String ip, String port){
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public TaxiInfo(GreetingsServiceOuterClass.GreetingsRequest.TaxiInfoMsg taxiInfoMsg) {
        id = taxiInfoMsg.getId();
        ip = taxiInfoMsg.getIp();
        port = String.valueOf(taxiInfoMsg.getPort());
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }
    public void setPort(String port) {
        this.port = port;
    }
}
