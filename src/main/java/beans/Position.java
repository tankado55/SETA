package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Position {

    private int x;
    private int y;

    public Position() {
    }

    public Position (int x, int y){
        this.x = x;
        this.y = y;
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }
}
