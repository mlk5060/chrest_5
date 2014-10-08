package jchrest.lib;

/**
 * Custom exception class for exceptions thrown when attempting to move objects
 * using the {@link jchrest.architecture.MindsEye#moveObjects(java.util.ArrayList, int)}
 * method.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class MindsEyeObjectMoveException extends Exception{
  public MindsEyeObjectMoveException(String message){
    super(message);
  }
}
