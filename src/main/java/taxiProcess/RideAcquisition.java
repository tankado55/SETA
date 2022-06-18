package taxiProcess;

import SETA.RideRequest;

public class RideAcquisition {
    public int getAckToReceive() {
        return ackToReceive;
    }

    private int ackToReceive;
    private RideRequest ride;

    public RideAcquisition (int ackToReceive, RideRequest ride) {
        this.ackToReceive = ackToReceive;
        this.ride = ride;
    }

    public void acked() {
        --ackToReceive;
    }
}
