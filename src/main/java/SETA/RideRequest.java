package SETA;

import beans.Position;
import it.ewlab.ride.RideRequestMsgOuterClass.*;

public class RideRequest {
    String id;
    Position startingPosition;
    Position destinationPosition;

    RideRequest(String id, Position start, Position dest){
        this.id = id;
        startingPosition = start;
        destinationPosition = dest;
    }

    RideRequestMsg toMsg (){
        return RideRequestMsg.newBuilder().setId(id)
                .setStart(RideRequestMsg.PositionMsg.newBuilder()
                        .setX(startingPosition.getX())
                        .setY(startingPosition.getY()))
                .setDestination(RideRequestMsg.PositionMsg.newBuilder()
                        .setX(startingPosition.getX())
                        .setY(destinationPosition.getY()))
                .build();
    }
}
