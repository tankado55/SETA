package beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.concurrent.ThreadLocalRandom;

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

    public static Position generateRandomPosition(int maxX, int maxY){
        int randomX = ThreadLocalRandom.current().nextInt(0, maxX);
        int randomY = ThreadLocalRandom.current().nextInt(0, maxY);
        return new Position(randomX, randomY);
    }

    public int getX(){
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY(){
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
