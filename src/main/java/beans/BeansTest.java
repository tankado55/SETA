package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class BeansTest {
    private String x;

    public BeansTest() {
    }

    public BeansTest(String x) {
        this.x = x;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }
}
