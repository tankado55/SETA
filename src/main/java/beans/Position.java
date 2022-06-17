package beans;

import it.ewlab.ride.RideRequestMsgOuterClass;

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

    public Position(RideRequestMsgOuterClass.RideRequestMsg.PositionMsg msg) {
        this.x = msg.getX();
        this.y = msg.getY();
    }

    public static Position generateRandomPosition(int maxX, int maxY){
        int randomX = ThreadLocalRandom.current().nextInt(0, maxX);
        int randomY = ThreadLocalRandom.current().nextInt(0, maxY);
        return new Position(randomX, randomY);
    }

    @Override
    public boolean equals(Object o){
        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof Position)) {
            return false;
        }

        Position p = (Position) o;

        return p.x == this.x && p.y == this.y;
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

    public int getDistrict(){
        if (x < 5){
            if (y < 5)
                return 1;
            else
                return 4;
        }
        else{
            if (y < 5)
                return 2;
            else
                return 3;
        }
    }
}
