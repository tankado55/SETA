package SETA;

import beans.Position;

public class RideRequest {
    String id;
    Position startingPosition;
    Position destinationPosition;

    RideRequest(String id, Position start, Position dest){
        this.id = id;
        startingPosition = start;
        destinationPosition = dest;
    }
}
