package jchrest.domainSpecifics;

import java.util.UUID;
import jchrest.lib.Square;

/**
 * Represents the result of a {@link jchrest.architecture.Perceiver} associated 
 * with a {@link jchrest.architecture.Chrest} model focusing on a {@link 
 * jchrest.lib.Square} in a {@link jchrest.lib.Scene}.
 * 
 * A {@link jchrest.domainSpecifics.Fixation} has two time variables.  The first 
 * denotes when the {@link jchrest.lib.Square} that is to be focused on by a 
 * {@link jchrest.architecture.Perceiver} associated with a {@link 
 * jchrest.architecture.Chrest} model has been selected (time decided upon).  
 * The second denotes the time that {@link #this} is to be performed, i.e. the
 * time that the {@link jchrest.architecture.Perceiver} associated with a {@link 
 * jchrest.architecture.Chrest} model actually focuses on the {@link 
 * jchrest.lib.Square} selected.  These times can vary from one type of {@link 
 * jchrest.domainSpecifics.Fixation} to another and so should be set 
 * accordingly.
 * 
 * A {@link jchrest.domainSpecifics.Fixation} also stores the {@link 
 * jchrest.lib.Scene} that {@link #this} was performed in context of, the x and 
 * y coordinates of the {@link jchrest.lib.Square} fixated on in the {@link 
 * jchrest.lib.Scene} and the {@link jchrest.lib.SceneObject} present on the 
 * {@link jchrest.lib.Square} fixated on. The instance variables of {@link 
 * #this} that store this information should only be set when {@link #this} is 
 * actually performed. By default, these instance variables are all set to 
 * {@code null}.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public abstract class Fixation {
  
  protected final String _reference = UUID.randomUUID().toString();
  protected int _timeDecidedUpon;
  protected Integer _performanceTime = null;
  protected boolean _performed = false;
  protected Scene _scene = null;
  protected Integer _colFixatedOn = null;
  protected Integer _rowFixatedOn = null;
  protected SceneObject _objectSeen = null;
  
  /**
   * @param timeDecidedUpon The time (in milliseconds) that it will be in the
   * domain when the {@link jchrest.lib.Square} to perform {@link #this} on is 
   * selected.
   */
  public Fixation(int timeDecidedUpon){
    this._timeDecidedUpon = timeDecidedUpon;
  }
  
  /**
   * @return The {@link jchrest.lib.SceneObject} present on the {@link 
   * jchrest.lib.Square} selected from {@link #this#getScene()} when {@link 
   * #this} is performed or {@code null} if {@link #this} has not yet been 
   * performed.
   */
  public SceneObject getObjectSeen(){
    return this._objectSeen;
  }
  
  /**
   * @return The unique reference for {@link #this}; useful for debugging.
   */
  public String getReference(){
    return this._reference;
  }
  
  /**
   * @return The {@link jchrest.lib.Scene} that {@link #this} was performed in
   * context of or {@code null} if {@link #this} has not yet been performed.
   */
  public Scene getScene(){
    return this._scene;
  }
  
  /**
   * @return The time (in milliseconds) that it will be in the
   * domain when the {@link jchrest.lib.Square} to perform {@link #this} on is 
   * selected.
   */
  public int getTimeDecidedUpon(){
    return this._timeDecidedUpon;
  }
  
  /**
   * @return The time (in milliseconds) that {@link #this} is to be/was 
   * performed or {@code null} if {@link #this} has not been scheduled for 
   * performance yet.
   */
  public Integer getPerformanceTime(){
    return this._performanceTime;
  }
  
  /**
   * @return The result of invoking {@link jchrest.lib.Square#getColumn()} on 
   * the {@link jchrest.lib.Square} selected when {@link #this} is performed 
   * (coordinate returned is relative to the {@link 
   * jchrest.domainSpecifics.Scene} fixated on rather than the coordinates in 
   * the external domain) or {@code null} if {@link #this} has not yet been 
   * performed.
   */
  public Integer getColFixatedOn(){
    return this._colFixatedOn;
  }
  
  /**
   * @return The result of invoking {@link jchrest.lib.Square#getRow()} on the 
   * {@link jchrest.lib.Square} selected when {@link #this} is performed 
   * (coordinate returned is relative to the {@link 
   * jchrest.domainSpecifics.Scene} fixated on rather than the coordinates in 
   * the external domain) or {@code null} if {@link #this} has not yet been 
   * performed.
   */
  public Integer getRowFixatedOn(){
    return this._rowFixatedOn;
  }
  
  /**
   * @return {@link java.lang.Boolean#TRUE} if {@link #this} has been performed,
   * {@link java.lang.Boolean#FALSE} if not.
   */
  public boolean hasBeenPerformed(){
    return this._performed;
  }
  
  /**
   * @param time The time {@link #this} was decided upon (in milliseconds).
   */
  protected void setTimeDecidedUpon(int time){
    this._timeDecidedUpon = time;
  }
  
  /**
   * Sets the relevant variables to indicate that {@link #this} has been 
   * performed.  In practice, only the first call to {@link #this} should have
   * any effect on the variables in question.
   * 
   * @param scene
   * @param col
   * @param row
   * @param objectSeen 
   */
  private void setPerformed(int col, int row, SceneObject objectSeen){
    if(!this._performed){
      this._performed = true;
      if(this._colFixatedOn == null) this._colFixatedOn = col;
      if(this._rowFixatedOn == null) this._rowFixatedOn = row;
      if(this._objectSeen == null) this._objectSeen = objectSeen;
    }
  }
  
  /**
   * Sets the time {@link #this} should be performed; can only be set once and
   * can only be set to a value greater than or equal to the result of {@link 
   * #this#getTimeDecidedUpon()} since {@link #this} should be decided upon
   * before it is performed.
   * 
   * @param time Should be specified in milliseconds.
   */
  public void setPerformanceTime(int time){
    if(this._performanceTime == null && time >= this._timeDecidedUpon) this._performanceTime = time;
  }
  
  /**
   * @return A description of {@link #this} along with its instance variables
   * in a human-readable format.
   */
  @Override
  public String toString() {
    return this.getClass().getSimpleName() 
      + "\n   - Reference: " + this._reference
      + "\n   - Decided upon at time: " + this._timeDecidedUpon + "ms"
      + "\n   - Performed at time: " + (this._performanceTime == null ? "null" : this._performanceTime + "ms")
      + "\n   - Performance successful: " + (this._performanceTime == null ? "not performed yet" : this._performed)
      + "\n   - Made in context of scene: " + (this._scene == null ? "null" : "'" + this._scene.getName() + "'")
      + "\n   - Column fixated on (domain-coordinates): " + (this._colFixatedOn == null || this._scene == null ? "null" : this._scene.getDomainSpecificColFromSceneSpecificCol(this._colFixatedOn))
      + "\n   - Row fixated on (domain-coordinates): " + (this._rowFixatedOn == null || this._scene == null ? "null" : this._scene.getDomainSpecificRowFromSceneSpecificRow(this._rowFixatedOn))
      + "\n   - Object seen: " + (this._objectSeen == null ?
          "has not yet been set" :
          "ID = " + this._objectSeen.getIdentifier() + ", class = " + this._objectSeen.getObjectType()
        )
    ;
  } 
  
  /**
   * Used to perform the fixation that {@link #this} represents on the {@link 
   * jchrest.lib.Scene} specified using the concrete implementation of {@link 
   * #this#make(jchrest.lib.Scene, int)}.
   * 
   * If the fixation that {@link #this} represents is successfully made, then
   * the scene this fixation is made in context of will be set to the {@link 
   * jchrest.lib.Scene} specified as a parameter to this method, the x and y 
   * coordinates fixated on will be set to the result of invoking {@link 
   * jchrest.lib.Square#getColumn()} and {@link 
   * jchrest.lib.Square#getRow()} on the {@link jchrest.lib.Square} fixated on 
   * and the object seen will be set to the {@link jchrest.lib.SceneObject} that 
   * is present on the {@link jchrest.lib.Square} that {@link #this} is 
   * performed on.
   * 
   * @param scene The {@link jchrest.lib.Scene} that {@link #this} should be
   * performed in context of.
   * @param time
   * 
   * @return {@link java.lang.Boolean#TRUE} if all the following are true, 
   * otherwise {@link java.lang.Boolean#FALSE} is returned:
   * 
   * <ul>
   *    <li>
   *      The {@code time} specified as a parameter, i.e. the time that {@link 
   *      #this} is to be performed, is later than or equal to {@link 
   *      #this#getPerformanceTime().
   *    </li>
   *    <li>
   *      The concrete implementation of {@link #this#doFixation(
   *      jchrest.architecture.Chrest, jchrest.lib.Scene, int)} returns an 
   *      {@link jchrest.lib.ItemSquarePattern}, i.e. {@link #this} was 
   *      successfully performed.  
   *    </li>
   * </ul>
   */
  public boolean perform(Scene scene, int time){
    if(this._performanceTime <= time){
      this._scene = scene;
      Square resultOfMakingFixation = make(scene, time);

      if(resultOfMakingFixation != null){
        SceneObject objectSeen = scene.getSquareContents(resultOfMakingFixation.getColumn(), resultOfMakingFixation.getRow());
        this.setPerformed(resultOfMakingFixation.getColumn(), resultOfMakingFixation.getRow(), objectSeen);
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * This method should not be invoked directly to make {@link #this} since it 
   * will not be guaranteed that the instance variables of {@link #this} 
   * pertaining to its performance will be set by the concrete implementation.  
   * Neither can it be guaranteed that the {@link jchrest.lib.Scene} that {@link 
   * #this} is made in context of is the {@link jchrest.lib.Scene} that previous 
   * {@link jchrest.domainSpecifics.Fixation Fixations} in the same set have 
   * been made in context of. Use {@link #this#perform(jchrest.lib.Scene, int)} 
   * instead.
   * 
   * @param scene The {@link jchrest.lib.Scene} that {@link #this} is to be
   * made in context of.
   * @param time The time {@link #this} is to be made, in milliseconds.
   * 
   * @return A {@link jchrest.lib.Square} denoting where the {@link 
   * jchrest.architecture.Perceiver} associated with the {@link 
   * jchrest.architecture.Chrest} model making {@link #this} should focus its
   * attention on (coordinates specified should be relative to the coordinates 
   * of the {@code scene} specified rather than the domain that the {@code 
   * scene} represents.  If {@link #this} is not made successfully, {@code null} 
   * should be returned.
   */
  public abstract Square make(Scene scene, int time);
}
