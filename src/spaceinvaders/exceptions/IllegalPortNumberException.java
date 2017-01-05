package spaceinvaders.exceptions;

/**
 * Thrown when the port number is not in the standard range.
 */
@SuppressWarnings("serial")
public class IllegalPortNumberException extends IllegalArgumentException {
  public IllegalPortNumberException(Throwable cause) {
    super("The port number must be a number between 0 and 65535 inclusive!",cause);
  }
}
