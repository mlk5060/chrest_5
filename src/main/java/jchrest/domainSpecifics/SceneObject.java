package jchrest.lib;

/**
 * Class that represents objects in a {@link jchrest.lib.Scene}.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class SceneObject {
  private final String _identifier;
  private final String _objectClass;
  
  /**
   * Constructor.  If the object class is equal to 
   * {@link jchrest.lib.Scene#getBlindSquareToken()} or
   * {@link jchrest.lib.Scene#getEmptySquareToken()} then, the identifier 
   * passed is always overwritten to the result of 
   * {@link jchrest.lib.Scene#getBlindSquareToken()} or
   * {@link jchrest.lib.Scene#getEmptySquareToken()} accordingly.  
   * Otherwise, if the {@link jchrest.lib.Scene} that this {@link #this} is a 
   * part of is rendered as a {@link jchrest.lib.ListPattern} and objects are 
   * encoded using their unique identifiers, there may be some
   * confusion since unique identifiers intuitively imply that {@link #this} 
   * represents an actual object.
   * 
   * @param identifier A unique identifier for the object (in context of the 
   * {@link jchrest.lib.Scene} it is to be placed in: used to provide
   * information to the {@link jchrest.lib.VisualSpatialFieldObject} constructor 
   * if the {@link jchrest.lib.Scene} that this {@link #this} is present in is 
   * used to construct a {@link jchrest.architecture.VisualSpatialField} 
   * instance.
   * 
   * @param objectClass The class of object: used to provide information to the 
   * {@link jchrest.lib.VisualSpatialFieldObject} constructor if the
   * {@link jchrest.lib.Scene} that this {@link #this} is present in is used to 
   * construct a {@link jchrest.architecture.VisualSpatialField} instance.
   */
  public SceneObject(String identifier, String objectClass){
    
    if(objectClass.equals(Scene.getBlindSquareToken())){
      identifier = Scene.getBlindSquareToken();
    }
    else if(objectClass.equals(Scene.getEmptySquareToken())){
      identifier = Scene.getEmptySquareToken();
    }
    
    this._identifier =  identifier;
    this._objectClass = objectClass;
  }

  /**
   * Returns the identifier for this object.
   * 
   * @return 
   */
  public String getIdentifier() {
    return this._identifier;
  }

  /**
   * Returns the object class for this object.
   * 
   * @return 
   */
  public String getObjectClass() {
    return _objectClass;
  }
}
