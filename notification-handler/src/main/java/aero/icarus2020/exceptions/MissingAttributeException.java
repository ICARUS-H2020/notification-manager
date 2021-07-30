package aero.icarus2020.exceptions;

public class MissingAttributeException extends Exception {

    public MissingAttributeException(String attribute) {
        System.out.println("The event has missing attribute: " + attribute);
    }
}
