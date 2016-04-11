package jchrest.domainSpecifics;

import java.util.UUID;

/**
 * Class that represents objects in a {@link jchrest.lib.Scene}.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class SceneObject {
  protected final String _identifier;
  protected final String _objectType;
  
  /**
   * Constructor.
   * 
   * @param objectType Can not be {@code null} or empty: specifies the generic 
   * "type" of {@link #this}, i.e. if {@link #this} is to represent a car, 
   * {@code objectType} may be "Toyota".
   * 
   * @throws IllegalArgumentException If the {@code objectType} specified is 
   * equal to {@code null} or is empty.
   */
  public SceneObject(String objectType){
    this(UUID.randomUUID().toString(), objectType);
  }
  
  /**
   * Should only be used by a {@link 
   * jchrest.lib.VisualSpatialFieldObject} since the identifier for
   * a {@link jchrest.lib.VisualSpatialFieldObject} should match
   * its {@link jchrest.domainSpecifics.SceneObject} representation in certain
   * cases.
   * 
   * @param identifier
   * @param objectType 
   */
  public SceneObject(String identifier, String objectType){
    if(identifier == null || objectType == null || identifier.isEmpty() || objectType.isEmpty()){
      throw new IllegalArgumentException(
        "SceneObject identifier/type can not be null or empty." +
        "\n- SceneObject identifier specified: " + (identifier == null ? "null" : identifier) +
        "\n- SceneObject type specified: " + (objectType == null ? "null" : objectType)
      );
    }
    
    this._identifier = identifier;
    this._objectType = objectType;
  }

  public String getIdentifier() {
    return this._identifier;
  }

  public String getObjectType() {
    return _objectType;
  }
  
  @Override
  public String toString(){
    return "Identifier: " + this._identifier + ", type: " + this._objectType;
  }
}
