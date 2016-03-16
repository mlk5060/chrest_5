package jchrest.domainSpecifics;

/**
 * Class that represents objects in a {@link jchrest.lib.Scene}.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class SceneObject {
  private final String _identifier;
  private final String _objectType;
  
  /**
   * Constructor.
   * 
   * @param identifier Can not be {@code null}: a unique identifier for {@link 
   * #this} (in context of the {@link jchrest.lib.Scene} it is to be placed in),
   * i.e. if {@link #this} is to represent a car, {@code identifier} may be set 
   * to "0000".  If {@link #this} is to represent a blind/empty {@link 
   * jchrest.lib.Square}, whatever is specified here will be overwritten with
   * the result of {@link jchrest.lib.Scene#getBlindSquareToken()} or {@link 
   * jchrest.lib.Scene#getEmptySquareToken()} accordingly since the {@code 
   * identifier} for {@link #this} intuitively implies that {@link #this} 
   * represents a non blind/empty  {@link jchrest.lib.Square}, i.e. an 
   * <i>actual</i> object in a {@link jchrest.lib.Scene}.
   * 
   * @param objectType Can not be {@code null}: specifies the generic "type" of
   * {@link #this}, i.e. if {@link #this} is to represent a car, {@code 
   * objectType} may be "Toyota".  If {@link #this} is to represent a 
   * blind/empty {@link jchrest.lib.Square}, this parameter should be set to the
   * result of {@link jchrest.lib.Scene#getBlindSquareToken()} or {@link 
   * jchrest.lib.Scene#getEmptySquareToken()}, accordingly.
   * 
   * @throws IllegalArgumentException If the {@code identifier} or {@code 
   * objectType} specified are equal to {@code null}.
   */
  public SceneObject(String identifier, String objectType){
    
    if(identifier == null || objectType == null){
      throw new IllegalArgumentException(
        "SceneObject identifier and object class can not be null." +
        "\n   - Identifier specified: " + (identifier == null ? "null" : identifier) +
        "\n   - Obj. class specified: " + (objectType == null ? "null" : objectType)
      );
    }
    
    if(objectType.equals(Scene.getBlindSquareToken())){
      identifier = Scene.getBlindSquareToken();
    }
    else if(objectType.equals(Scene.getEmptySquareToken())){
      identifier = Scene.getEmptySquareToken();
    }
    
    this._identifier =  identifier;
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
