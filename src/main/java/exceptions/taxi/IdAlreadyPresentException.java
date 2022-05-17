package exceptions.taxi;

public class IdAlreadyPresentException extends Exception{
    private String message;

    public IdAlreadyPresentException() {message = "Taxi id already present";}
}
