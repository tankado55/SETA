package taxiProcess;

import beans.Position;

import java.util.HashMap;
import java.util.Map;

public class Utils {
    static Map<Integer, Position> rechargePosition = new HashMap<Integer, Position>(){{
       put(1, new Position(0,0));
       put(2, new Position(0,9));
       put(3, new Position(9,9));
       put(4, new Position(9,0));
    }};

    public static Position getRechargePosition(int district){
        return rechargePosition.get(district);
    }
}

