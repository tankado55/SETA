package SETA;

import beans.Position;
import it.ewlab.ride.RideRequestMsgOuterClass.*;

public class RideRequest {
    public String getId() {
        return id;
    }

    String id;
    Position startingPosition;
    Position destinationPosition;

    public Position getStartingPosition() {
        return startingPosition;
    }

    public Position getDestinationPosition() {
        return destinationPosition;
    }

    public RideRequest(String id, Position start, Position dest){
        this.id = id;
        startingPosition = start;
        destinationPosition = dest;
    }

    public RideRequest(RideRequestMsg msg){
        this.id = msg.getId();
        this.startingPosition = new Position(msg.getStart());
        this.destinationPosition = new Position(msg.getDestination());

    }

    public RideRequestMsg toMsg (){
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
