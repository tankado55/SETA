package beans;

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

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }
}
