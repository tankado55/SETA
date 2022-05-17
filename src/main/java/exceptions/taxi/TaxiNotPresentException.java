package exceptions.taxi;

public class TaxiNotPresentException extends Exception {
    private String message;

    public TaxiNotPresentException() {message = "Taxi id already present";}
}
