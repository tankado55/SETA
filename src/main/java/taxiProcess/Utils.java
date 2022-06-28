package taxiProcess;

import beans.Position;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    public static String toHumanDate(long millisTimestamp){
        Date date = new Date(millisTimestamp);
        Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    public static long toMachineDate(String dateString){
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = format.parse(dateString);
            return date.getTime();
        } catch (ParseException e) {throw new RuntimeException(e);}
    }
}

