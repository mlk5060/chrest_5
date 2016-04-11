package jchrest.lib;

import jchrest.architecture.VisualSpatialField;
import java.util.Map.Entry;
import jchrest.architecture.Chrest;
import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.SceneObject;

/**
 * Represents an object in a {@link jchrest.architecture.VisualSpatialField}.
 * <p>
 * <b>NOTE</b>: Visual-spatial field objects can not represent {@link 
 * jchrest.domainSpecifics.SceneObject SceneObjects} whose {@link 
 * jchrest.domainSpecifics.SceneObject#getObjectType()} returns {@link 
 * jchrest.domainSpecifics.Scene#getBlindSquareToken()} since these {@link 
 * jchrest.domainSpecifics.SceneObject SceneObjects} should be encoded as 
 * "unknown" {@link jchrest.lib.VisualSpatialFieldObject 
 * VisualSpatialFieldObjects} (see {@link 
 * jchrest.lib.VisualSpatialFieldObject#getUnknownSquareToken()}).
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class VisualSpatialFieldObject extends SceneObject{
  private static final String UNKNOWN_SQUARE_TOKEN = "-";
  
  private final Chrest _associatedModel;
  private final VisualSpatialField _associatedVisualSpatialField;
  
  //Maintains a history of the object's "recognised state" so that its 
  //recognised status at any time can be determined.
  private final HistoryTreeMap _recognisedHistory = new HistoryTreeMap();
  
  private Integer _terminus;
  private int _timeCreated;
  
  /**
   * @return The string used to denote a {@link 
   * jchrest.lib.VisualSpatialFieldObject} that represents a {@link 
   * jchrest.architecture.VisualSpatialField} coordinate whose {@link 
   * jchrest.lib.VisualSpatialFieldObject} status is currently unknown.
   */
  public static String getUnknownSquareToken(){
    return VisualSpatialFieldObject.UNKNOWN_SQUARE_TOKEN;
  }
  
  /**
   * Constructor (sets {@link #this#_identifier} implicitly, see {@link 
   * jchrest.domainSpecifics.SceneObject#SceneObject(java.lang.String)).
   * 
   * @param objectType See {@link 
   * jchrest.domainSpecifics.SceneObject#_objectType}.  Can not equal {@link 
   * jchrest.domainSpecifics.Scene#getBlindSquareToken()}.
   * 
   * @param associatedModel
   * 
   * @param associatedVisualSpatialField The {@link 
   * jchrest.architecture.VisualSpatialField} that {@link #this} exists on.
   * 
   * @param timeCreated
   * 
   * @param recognised Whether {@link #this} is recognised.
   * 
   * @param setTerminus Set to {@link java.lang.Boolean#TRUE} to set the 
   * terminus of {@link #this} automatically or {@link java.lang.Boolean#FALSE}
   * to set to {@code null} (can be set later using {@link #this#setTerminus(
   * int, boolean)).
   */
  public VisualSpatialFieldObject(
    String objectType,
    Chrest associatedModel,
    VisualSpatialField associatedVisualSpatialField, 
    int timeCreated, 
    boolean recognised,
    boolean setTerminus
  ){
    super(objectType);
    
    if(objectType.equals(Scene.getBlindSquareToken())){
      throw new IllegalArgumentException(
        "Blind squares can not be encoded as VisualSpatialFieldObjects"
      );
    }
    
    //Set instance variables.
    this._associatedModel = associatedModel;
    this._associatedVisualSpatialField = associatedVisualSpatialField;
    this._recognisedHistory.put(timeCreated, recognised);
    this._timeCreated = timeCreated;
    
    if(setTerminus){
      this._terminus = timeCreated + (recognised ? 
        associatedModel.getRecognisedVisualSpatialFieldObjectLifespan() :
        associatedModel.getUnrecognisedVisualSpatialFieldObjectLifespan()
      );
    }
  }
  
  /**
   * Constructor (sets {@link #this#_identifier} explicitly).
   * 
   * @param identifier See {@link 
   * jchrest.domainSpecifics.SceneObject#_identifier}.
   * 
   * @param objectType See {@link 
   * jchrest.domainSpecifics.SceneObject#_objectType}. Can not equal {@link 
   * jchrest.domainSpecifics.Scene#getBlindSquareToken()}.
   * 
   * @param associatedModel
   * 
   * @param associatedVisualSpatialField The {@link 
   * jchrest.architecture.VisualSpatialField} that {@link #this} exists on.
   * 
   * @param timeCreated
   * 
   * @param recognised Whether {@link #this} is recognised.
   * 
   * @param setTerminus Set to {@link java.lang.Boolean#TRUE} to set the 
   * terminus of {@link #this} automatically or {@link java.lang.Boolean#FALSE}
   * to set to {@code null} (can be set later using {@link #this#setTerminus(
   * int, boolean)).
   */
  public VisualSpatialFieldObject(
    String identifier, 
    String objectType,
    Chrest associatedModel,
    VisualSpatialField associatedVisualSpatialField, 
    int timeCreated, 
    boolean recognised,
    boolean setTerminus
  ){
    super(identifier, objectType);
    
    if(objectType.equals(Scene.getBlindSquareToken())){
      throw new IllegalArgumentException(
        "Blind squares can not be encoded as VisualSpatialFieldObjects"
      );
    }

    //Set instance variables.
    this._associatedModel = associatedModel;
    this._associatedVisualSpatialField = associatedVisualSpatialField;
    this._recognisedHistory.put(timeCreated, recognised);
    this._timeCreated = timeCreated;
    
    if(setTerminus){
      this._terminus = timeCreated + (recognised ? 
        associatedModel.getRecognisedVisualSpatialFieldObjectLifespan() :
        associatedModel.getUnrecognisedVisualSpatialFieldObjectLifespan()
      );
    }
  }
  
  /**************************/
  /***** Simple Getters *****/
  /**************************/
  
  public final Chrest getAssociatedModel(){
    return this._associatedModel;
  }
  
  /**
   * @return The {@link jchrest.architecture.VisualSpatialField} that this 
   * {@link #this} is associated with.
   */
  public VisualSpatialField getAssociatedVisualSpatialField(){
    return this._associatedVisualSpatialField;
  }
  
  /**
   * @return The current terminus for {@link #this}, i.e. the time that {@link 
   * #this} will decay in the {@link jchrest.architecture.VisualSpatialField} it
   * is located on.
   */
  public Integer getTerminus(){
    return this._terminus;
  }
  
  /**
   * @return The time that {@link #this} was placed in its associated 
   * {@link jchrest.architecture.VisualSpatialField}.
   */
  public Integer getTimeCreated(){
    return this._timeCreated;
  }
  
  /**********************************/
  /***** Advanced Functionality *****/
  /**********************************/
  
  /**
   * @return An exact clone of this {@link #this} with a different object 
   * reference, i.e. a change to the clone will not be reflected in the 
   * original.
   */
  public VisualSpatialFieldObject createClone(){
    
    VisualSpatialFieldObject clone = new VisualSpatialFieldObject(
      this._identifier, 
      this._objectType, 
      this._associatedModel,
      this._associatedVisualSpatialField, 
      this._timeCreated, 
      (boolean)this._recognisedHistory.get(this._timeCreated),
      false
    );
    
    clone._terminus = this._terminus;
    
    //Build the clone's "Recognised Status" history.
    for(Entry<Integer, Object> history : this._recognisedHistory.entrySet()){
      clone._recognisedHistory.put(history.getKey(), (boolean)history.getValue());
    }
    
    return clone;
  }
  
  /**
   * @param time
   * 
   * @return {@link java.lang.Boolean#TRUE} if the creation time of {@link 
   * #this} is greater than or equal to the {@code time} specified and the 
   * terminus for {@link #this} is either equal to {@code null} or is not {@code 
   * null} and is greater than the {@code time} specified. 
   */
  public boolean isAlive(int time){
    return time >= this._timeCreated && (this._terminus == null || (this._terminus != null && this._terminus > time));
  }
  
  /**
   * @param time The time to check the recognised status of this {@link #this}
   * against.
   * 
   * @return The recognised status of {@link #this} at the {@code time} 
   * specified.  If the {@code time} specified is before the creation time of 
   * {@link #this}, {@link java.lang.Boolean#FALSE} is returned.
   */
  public boolean isRecognised(int time){
    if(time >= this._timeCreated){
      return (boolean)this._recognisedHistory.floorEntry(time).getValue();
    }
    else{
      return false;
    }
  }
  
  /**
   * Sets {@link this} as being recognised at the {@code time} specified if 
   * {@link #this} is alive (see {@link #this#isAlive(int)} at the {@code time} 
   * specified.
   * 
   * @param time
   * @param updateTerminusAutomatically Set to {@link java.lang.Boolean#TRUE} 
   * to update the terminus of {@link #this} automatically to the value of the 
   * {@code time} specified plus the result of invoking {@link 
   * jchrest.architecture.Chrest#getRecognisedVisualSpatialFieldObjectLifespan()
   * } in context of the result of invoking {@link #this#getAssociatedModel()}.
   */
  public void setRecognised(int time, boolean updateTerminusAutomatically){
    if(this.isAlive(time)){
      this._recognisedHistory.put(time, Boolean.TRUE);
      
      if(updateTerminusAutomatically){
        this._terminus = (time + this.getAssociatedModel().getRecognisedVisualSpatialFieldObjectLifespan());
      }
    }
  }
  
  /**
   * 
   * @param time
   * @param setToTime Set to {@link java.lang.Boolean#TRUE} to force the 
   * terminus of {@link #this} to be set to the {@code time} specified.  Set to 
   * {@link java.lang.Boolean#FALSE} to have the method set the terminus 
   * according to the recognised status of {@link #this} at the {@code time} 
   * specified:
   * <ul>
   *  <li>
   *    If this {@link #this} is recognised, the terminus will be set to the 
   *    {@code time} specified plus the value of {@link 
   *    jchrest.architecture.Chrest#getRecognisedVisualSpatialFieldObjectLifespan()}.
   *  </li>
   *  <li>
   *    If this {@link #this} is unrecognised, the terminus will be set to the 
   *    {@code time} specified plus the value of {@link 
   *    jchrest.architecture.Chrest#getUnrecognisedVisualSpatialFieldObjectLifespan()}.
   *  </li>
   * </ul>
   */
  public void setTerminus(int time, boolean setToTime){
    if(setToTime){
      this._terminus = time;
    }
    else{
      this._terminus = time + (this.isRecognised(time) ? 
        this.getAssociatedModel().getRecognisedVisualSpatialFieldObjectLifespan() : 
        this.getAssociatedModel().getUnrecognisedVisualSpatialFieldObjectLifespan()
      );
    }
  }
  
  /**
   * Sets {@link this} as being unrecognised at the {@code time} specified if 
   * {@link #this} is alive (see {@link #this#isAlive(int)} at the {@code time} 
   * specified.
   * 
   * @param time
   * @param updateTerminusAutomatically Set to {@link java.lang.Boolean#TRUE} 
   * to update the terminus of {@link #this} automatically to the value of the 
   * {@code time} specified plus the result of invoking {@link 
   * jchrest.architecture.Chrest#getUnrecognisedVisualSpatialFieldObjectLifespan()
   * } in context of the result of invoking {@link #this#getAssociatedModel()}.
   */
  public void setUnrecognised(int time, boolean updateTerminusAutomatically){
    if(this.isAlive(time)){
      this._recognisedHistory.put(time, Boolean.FALSE);
      
      if(updateTerminusAutomatically){
        this._terminus = (time + this._associatedModel.getUnrecognisedVisualSpatialFieldObjectLifespan());
      }
    }
  }
  
  @Override
  public String toString(){
    String recognisedHistory = "";
    for(Entry entry : this._recognisedHistory.entrySet()){
      recognisedHistory += "\n      ~ " + entry.getKey() + ": " + entry.getValue();
    }
      
    return 
      "\n   - Identifier: " + this._identifier +
      "\n   - Type: " + this._objectType +
      "\n   - Creation time: " + this._timeCreated +
      "\n   - Terminus: " + this._terminus + 
      "\n   - Recognised history: " + recognisedHistory
      ;
  }
}
