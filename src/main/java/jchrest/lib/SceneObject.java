package jchrest.lib;

/**
 * Class that represents objects in a {@link jchrest.lib.Scene}.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class SceneObject {
  private final Integer _identifier;
  private final String _objectClass;
  
  /**
   * Constructor.
   * 
   * @param identifier The unique identifier for the object: used to provide
   * information to the {@link jchrest.lib.MindsEyeObject} constructor if the
   * {@link jchrest.lib.Scene} that this object is present in is used to 
   * construct a {@link jchrest.architecture.MindsEye} instance.
   * 
   * @param objectClass The class of object: used to provide information to the 
   * {@link jchrest.lib.MindsEyeObject} constructor if the
   * {@link jchrest.lib.Scene} that this object is present in is used to 
   * construct a {@link jchrest.architecture.MindsEye} instance.
   */
  public SceneObject(Integer identifier, String objectClass){
    this._identifier = identifier;
    this._objectClass = objectClass;
  }

  /**
   * Returns the identifier for this object.
   * 
   * @return 
   */
  public int getIdentifier() {
    return _identifier;
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
