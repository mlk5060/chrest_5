package jchrest.lib;

/**
 * Class to handle visual-spatial field exceptions.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class VisualSpatialFieldException extends Exception{
  public VisualSpatialFieldException(String errorMsg){
    super(errorMsg);
  }
  
  public VisualSpatialFieldException(String errorMsg, Throwable cause){
    super(errorMsg, cause);
  }
}
