package SETA;

import beans.Position;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SETA {

    static final int GRIDXDIM = 10;
    static final int GRIDYDIM = 10;


    public static void main(String[] args) throws InterruptedException {

        List<String> idList = new ArrayList<>();
        int idCounter = 0;

        // generate two random rides
        while (true){
            RideRequest ride1 = generateRandomRide(Integer.toString(idCounter));
            ++idCounter;
            RideRequest ride2 = generateRandomRide(Integer.toString(idCounter));
            ++idCounter;

            Thread.sleep(5000);
        }

    }

    private static RideRequest generateRandomRide(String id){

        while (true){
            Position start = Position.generateRandomPosition(GRIDXDIM, GRIDYDIM);
            Position dest = Position.generateRandomPosition(GRIDXDIM, GRIDYDIM);

            if (start != dest){ // TODO implementare il confronto
                return new RideRequest(id, start, dest);
            }
        }

    }
}
