package jchrest.lib;

import java.util.Map.Entry;
import java.util.TreeMap;
import jchrest.architecture.VisualSpatialField;

/**
 * Represents an object in a {@link jchrest.architecture.VisualSpatialField}.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class VisualSpatialFieldObject {
  
  //Stores the VisualSpatialField associated with this object.
  private final VisualSpatialField _associatedVisualSpatialField;
  
  //Stores whether this VisualSpatialFieldObject is a "ghost" object or not.
  private final boolean _ghostObject;
  
  //Allows for the unambiguous identification and manipulation of specific
  //VisualSpatialFieldObjects in a VisualSpatialField.
  private final String _identifier;
  
  //Allows for CHREST to create chunks containing generalisable information.  
  //For example, if a chess player were to learn the position of bishops on a 
  //chess board at a given moment, the CHREST model representing the player 
  //could learn where the bishops in the current game are positioned and use 
  //this information in subsequent games.  This is only possible if each bishop 
  //isn't considered to be a unique entity when its location is learned (as they
  //would be if VisualSpatialObjects could only be represented using their 
  //unique identifier) otherwise, information about a bishop's location in the 
  //game would remain anchored to this particular bishop.  Thus, if another 
  //bishop occurred in the same location in a subsequent game, the location 
  //information learned regarding the previous bishop could not be used since 
  //CHREST would consider the two bishops to be different.
  private final String _objectClass;
  
  //Maintains a history of the object's "recognised state" so that its 
  //recognised status at any time can be determined.  Keys are times at which
  //the VisualSPatialObject was reocgnised or unrecognised.  A TreeMap is used 
  //for ease of value retrieval since it is possible to specify times that 
  //aren't keys in the data structure but the closest key to the time provided 
  //can be used. 
  private final TreeMap<Integer,Boolean> _recognisedHistory = new TreeMap<>();
  
  //Stores the time that the object will fully decay on its coordinates in the 
  //associated VisualSpatialField.
  private Integer _terminus = null;
  
  //Stores the time that the object was created on its coordinates in the
  //associated VisualSpatialField.  Can only be set once if it is set to -1 
  //after the value has been set by the constructor.  Note that it is not 
  //possible to set this variable to null since it is used as a key in the 
  //_recognisedHistory data structure and a null value causes the comparator 
  //function called using TreeMap.put() to error out.
  private Integer _timeCreated;
  
  /**
   * Constructor.
   * 
   * @param associatedVisualSpatialField The 
   * {@link jchrest.architecture.VisualSpatialField} that this {@link #this}
   * exists on.
   * 
   * @param identifier A unique identifier to allow for precise identification
   * of this {@link #this} when manipulating it in its associated 
   * {@link jchrest.architecture.VisualSpatialField}.
   * 
   * @param objectClass A class to allow for generalisable learning.  For 
   * example, if this {@link #this} is to represent a bishop on a chess board, 
   * this parameter should be set to "B" (or something similar) and all other
   * bishops should also be set to "B".
   * 
   * @param timeCreated Set to null or -1 to enable setting later.
   * 
   * @param setTerminus Set to true to set the terminus now or false to set it
   * later.  If this {@link #this}'s class equal to the result of
   * {@link jchrest.lib.Scene#getBlindSquareIdentifier()} or 
   * {@link jchrest.lib.Scene#getCreatorToken()} and this parameter is set to
   * true, the terminus for this object is not set (and is set to null) since 
   * blind squares and the creator's avatar should not decay like other 
   * {@link jchrest.lib.VisualSpatialFieldObject}s in a 
   * {@link jchrest.architecture.VisualSpatialField}.
   * 
   * @param ghostObject If this {@link #this} represents an object in the 
   * associated {@link jchrest.architecture.VisualSpatialField} that is not 
   * represented as a {@link jchrest.lib.SceneObject} in the 
   * {@link jchrest.lib.Scene} transposed into the associated 
   * {@link jchrest.architecture.VisualSpatialField} (the result of 
   * {@link jchrest.architecture.VisualSpatialField#getSceneEncoded()}), then
   * set this parameter to true.
   */
  public VisualSpatialFieldObject(
    VisualSpatialField associatedVisualSpatialField, 
    String identifier, 
    String objectClass, 
    Integer timeCreated, 
    boolean setTerminus,
    boolean ghostObject
  ){
    
    //Determine the identifier to use and set the identifier and object class.
    if(objectClass.equals(Scene.getBlindSquareIdentifier())){
      identifier = Scene.getBlindSquareIdentifier();
    }
    else if(objectClass.equals(Scene.getEmptySquareIdentifier())){
      identifier = Scene.getEmptySquareIdentifier();
    }
    
    //Determine the time for creation and set this plus the first entry in the
    //objects recognised history.
    if(timeCreated == null){
      timeCreated = -1;
    }

    //Set instance variables.
    this._associatedVisualSpatialField = associatedVisualSpatialField;
    this._identifier = identifier;
    this._objectClass = objectClass;
    this._ghostObject = ghostObject;
    this._recognisedHistory.put(timeCreated, Boolean.FALSE);
    this._timeCreated = timeCreated;

    //Set terminus.
    if( 
      ( !objectClass.equals(Scene.getBlindSquareIdentifier()) && !objectClass.equals(Scene.getCreatorToken()) ) || 
      setTerminus
    ){
      this._terminus = timeCreated + this._associatedVisualSpatialField.getUnrecognisedObjectLifespan();
    }
  }
  
  /**
   * @param time The time to check this {@link #this}s "alive" status against.
   * @return True if the terminus for this object is null or greater than the
   * time specified and the time specified is greater than or equal to the time 
   * the object was created.
   */
  public boolean alive(int time){
    if(
      (this._terminus == null || this._terminus > time) && 
      time >= this._timeCreated
    ){
      return true;
    }
    else{
      return false;
    }
  }
  
  /**
   * @return An exact clone of this {@link #this} but the 
   * {@link jchrest.lib.VisualSpatialFieldObject} will have a different 
   * reference in memory.
   */
  public VisualSpatialFieldObject createClone(){
    
    VisualSpatialFieldObject clone = new VisualSpatialFieldObject(
      this._associatedVisualSpatialField, 
      this._identifier, 
      this._objectClass, 
      this._timeCreated, 
      false, 
      this._ghostObject
    );
    
    //Only set the clone's terminus if the original's terminus has been set.
    if(this._terminus != null){
      clone.setTerminus(this._terminus, true);
    }
    
    //Build the clone's "Recognised Status" history.
    for(Entry<Integer, Boolean> history : this._recognisedHistory.entrySet()){
      
      //Object was recognised at the time specified by the key.
      if(history.getValue()){
        clone.setRecognised(history.getKey(), false);
      }
      //Object was unrecognised at the time specified by the key.
      else{
        clone.setUnrecognised(history.getKey(), false);
      }
    }
    
    return clone;
  }
  
  /**
   * @return True if this {@link #this} is a ghost object.
   */
  public boolean isGhost(){
    return this._ghostObject;
  }
  
  /**
   * @return The {@link jchrest.architecture.VisualSpatialField} that this 
   * {@link #this} is associated with.
   */
  public VisualSpatialField getAssociatedVisualSpatialField(){
    return this._associatedVisualSpatialField;
  }
  
  /**
   * @return This {@link #this}'s identifier.
   */
  public String getIdentifier(){
    return this._identifier;
  }
  
  /**
   * @return This {@link #this}'s class (different from its Java class).
   */
  public String getObjectClass(){
    return this._objectClass;
  }
  
  /**
   * @return This {@link #this}'s current terminus.
   */
  public Integer getTerminus(){
    return this._terminus;
  }
  
  /**
   * @return The time that this {@link #this} was placed in its associated 
   * {@link jchrest.architecture.VisualSpatialField}.
   */
  public Integer getTimeCreated(){
    return this._timeCreated;
  }
  
  /**
   * @param time The time to check the recognised status of this {@link #this}
   * against.
   * 
   * @return The recognised status of this {@link #this} at the time specified.  
   * If the time specified is before the creation time of the object, false is 
   * returned.
   */
  public boolean recognised(int time){
    if(time >= this._timeCreated){
      return this._recognisedHistory.floorEntry(time).getValue();
    }
    else{
      return false;
    }
  }
  
  /**
   * Sets the recognised status of this {@link #this} to true if this 
   * {@link #this} is alive at the time specified.
   * 
   * @param time
   * @param updateTerminusAutomatically Set to true to update this 
   * {@link #this}'s terminus automatically to the value of the time passed 
   * plus the value returned from invoking 
   * {@link jchrest.architecture.VisualSpatialField#getRecognisedObjectLifespan()}.
   */
  public void setRecognised(int time, boolean updateTerminusAutomatically){
    if(this.alive(time)){
      this._recognisedHistory.put(time, Boolean.TRUE);
      
      if(updateTerminusAutomatically){
        this.setTerminus(time, false);
      }
    }
  }
  
  /**
   * If this {@link #this} is alive at the time specified, this function sets 
   * the terminus of this {@link #this} based upon the time specified.
   * 
   * @param time
   * @param setToTime Set to true to force the terminus of this {@link #this} to 
   * be set to the time specified.  Set to false to have the method set the
   * terminus normally:
   * <ul>
   *  <li>
   *    If this {@link #this} is recognised at the time specified, the terminus 
   *    will be equal to the time specified plus the value of 
   *    {@link jchrest.architecture.VisualSpatialField#getRecognisedObjectLifespan()}.
   *  </li>
   *  <li>
   *    If this {@link #this} is unrecognised at the time specified, the 
   *    terminus will be equal to the time specified plus the value of 
   *    {@link jchrest.architecture.VisualSpatialField#getUnrecognisedObjectLifespan()}.
   *  </li>
   * </ul>
   * If this {@link #this} represents a blind square, setting this parameter has
   * no effect: the terminus will always be set to the time passed since blind
   * squares do not decay after a period of time.
   * 
   */
  public void setTerminus(int time, boolean setToTime){
    if(this.alive(time)){
      if(setToTime || this._objectClass.equals(Scene.getBlindSquareIdentifier())){
        this._terminus = time;
      }
      else{
        this._terminus = time + (this.recognised(time) ? 
          this._associatedVisualSpatialField.getRecognisedObjectLifespan() : 
          this._associatedVisualSpatialField.getUnrecognisedObjectLifespan() 
        );
      }
    }
  }
  
  /**
   * Sets the time that this {@link #this} was created to the time specified (if 
   * it has not already been set).
   * 
   * @param timeCreated 
   */
  public void setTimeCreated(int timeCreated){
    if(this._timeCreated == -1){
      this._timeCreated = timeCreated;
    }
  }
  
  /**
   * Sets the recognised status of this {@link #this} to false if this 
   * {@link #this} is alive at the time specified.
   * 
   * @param time
   * @param updateTerminusAutomatically Set to true to update this 
   * {@link #this}'s terminus automatically to the value of the time passed 
   * plus the value returned from invoking 
   * {@link jchrest.architecture.VisualSpatialField#getUnrecognisedObjectLifespan()}.
   */
  public void setUnrecognised(int time, boolean updateTerminusAutomatically){
    if(this.alive(time)){
      this._recognisedHistory.put(time, Boolean.FALSE);
      
      if(updateTerminusAutomatically){
        this.setTerminus(time, false);
      }
    }
  }
}
