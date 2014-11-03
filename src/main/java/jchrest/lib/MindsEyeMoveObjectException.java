package jchrest.lib;

/**
 *
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class MindsEyeMoveObjectException extends Exception{
  public MindsEyeMoveObjectException(String errorMsg){
    super(errorMsg);
  }
  
  public MindsEyeMoveObjectException(String errorMsg, Throwable cause){
    super(errorMsg, cause);
  }
}
