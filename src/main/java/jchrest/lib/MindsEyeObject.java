package jchrest.lib;

import java.util.Map.Entry;
import java.util.TreeMap;
import jchrest.architecture.MindsEye;

/**
 * Represents an object in the mind's eye.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class MindsEyeObject {
  
  //Stores the mind's eye associated with this object.
  private final MindsEye _associatedMindsEye;
  
  //Stores a human-readable identifier for the object.
  private final String _identifier;
  
  //Maintains a history of the object's "recognised state" so that its 
  //recognised status at any time can be determined.  A TreeMap is used for ease
  //of status retrieval since values can be selected using keys that are close
  //to a time but may not be exact.
  private final TreeMap<Integer,Boolean> _recognisedHistory = new TreeMap<>();
  
  //Stores the time that the object will fully decay on its coordinates in its 
  //visual-spatial field.
  private Integer _terminus = null;
  
  //Stores the time that the object was created on its coordinates in its
  //visual-spatial field.
  private final int _timeCreated;
  
  /**
   * Constructor.  If the identifier passed is equal to 
   * {@link jchrest.lib.Scene#EMPTY_SQUARE_IDENTIFIER}, the terminus for this 
   * object is set to null since empty squares do not have terminus values.
   * 
   * @param associatedMindsEye
   * @param identifier
   * @param timeCreated 
   */
  public MindsEyeObject(MindsEye associatedMindsEye, String identifier, int timeCreated){
    this._associatedMindsEye = associatedMindsEye;
    this._identifier = identifier;
    this._timeCreated = timeCreated;
    this._recognisedHistory.put(timeCreated, Boolean.FALSE);
    
    if(!this._identifier.equals(Scene.getBlindSquareIdentifier())){
      this._terminus = timeCreated + this._associatedMindsEye.getUnrecognisedObjectLifespan();
    }
  }
  
  /**
   * Returns whether the object is alive at the time specified.
   * 
   * @param time
   * @return True if the terminus for this object is null and the time specified
   * is greater than or equal to the time the object was created.  If the 
   * object's terminus is not null then true is returned if the object's
   * terminus is greater than the time specified and the time specified is 
   * greater than or equal to the object's time of creation.
   */
  public boolean alive(int time){
    if(this._terminus == null){
      if(time >= this._timeCreated){
        return true;
      }
      else{
        return false;
      }
    }
    else{
      if(this._terminus > time && time >= this._timeCreated){
        return true;
      }
      else{
        return false;
      }
    }
  }
  
  /**
   * Creates an exact clone of this {@link jchrest.lib.MindsEyeObject} instance.
   * 
   * @return 
   */
  public MindsEyeObject createClone(){
    MindsEyeObject clone = new MindsEyeObject(this._associatedMindsEye, this._identifier, this._timeCreated);
    
    //Rebuild the object's "Recognised Status" history.
    for(Entry<Integer, Boolean> history : this._recognisedHistory.entrySet()){
      
      //If object was recognised in this entry
      if(history.getValue()){
        clone.setRecognised(history.getKey());
      }
      //Object was unrecognised in this entry
      else{
        clone.setUnrecognised(history.getKey());
      }
    }
    
    //Set the terminus for the clone explicitly here since the recognised status 
    //history rebuild will have set it incorrectly.
    clone._terminus = this._terminus;
    
    return clone;
  }
  
  /**
   * Returns the {@link jchrest.architecture.MindsEye} instance that this object 
   * is associated with.
   * 
   * @return 
   */
  public MindsEye getAssociatedMindsEye(){
    return this._associatedMindsEye;
  }
  
  /**
   * Returns the object's identifier.
   * 
   * @return 
   */
  public String getIdentifier(){
    return this._identifier;
  }
  
  /**
   * Returns the object's terminus.
   * 
   * @return 
   */
  public Integer getTerminus(){
    return this._terminus;
  }
  
  /**
   * Returns the time that the object was placed in its current coordinates in
   * the visual-spatial field.
   * 
   * @return 
   */
  public int getTimeCreated(){
    return this._timeCreated;
  }
  
  /**
   * Returns the recognised status of the object at the time specified.  If the
   * time specified is before the creation time of the object, false is 
   * returned.
   * 
   * @param time
   * @return 
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
   * Sets the recognised status of the object to true if the object has not
   * decayed at the time specified.
   * 
   * @param time 
   */
  public void setRecognised(int time){
    if(this.alive(time)){
      this._recognisedHistory.put(time, Boolean.TRUE);
      this.setTerminus(time, false);
    }
  }
  
  /**
   * Sets the terminus of this object based upon the time specified if this 
   * object has not decayed at the time specified.
   * <ul>
   *  <li>
   *    If the object denotes a blind square, its terminus is set to the time
   *    passed since this means that the square the object occupies is no longer 
   *    blind (setting the second parameter to this method has no effect on 
   *    this).
   *  </li>
   *  <li>
   *    If the object does not denote a blind square its terminus is set to the
   *    time passed plus the lifespan for a recognised or unrecognised object
   *    (according to the "recognised" status of this object) specified in the 
   *    mind's eye that this mind's eye object is located in.
   *  </li>
   * </ul>
   * 
   * @param time
   * 
   * @param setToTime Set to true to force the terminus of this object to be set
   * to the time specified.  Set to false to have the method set the object's
   * terminus normally i.e. if the object has not decayed (terminus is less than
   * or equal to the time passed) calculate a new terminus by having the object 
   * determine if it is currently recognised or not and then adding the lifespan 
   * specified for recognised/unrecognised objects in the mind's eye associated 
   * with this object to the time specified.
   */
  public void setTerminus(int time, boolean setToTime){
    if(this.getIdentifier().equals(Scene.getBlindSquareIdentifier()) || setToTime){
      this._terminus = time;
    }
    else if(this.alive(time)){
      this._terminus = time + (this.recognised(time) ? 
        this._associatedMindsEye.getRecognisedObjectLifespan() : 
        this._associatedMindsEye.getUnrecognisedObjectLifespan() 
      );
    }
  }
  
  /**
   * Sets the recognised status of the object to false if the object has not
   * decayed at the time specified.
   * 
   * @param time 
   */
  public void setUnrecognised(int time){
    if(this.alive(time)){
      this._recognisedHistory.put(time, Boolean.FALSE);
      this.setTerminus(time, false);
    }
  }
}
