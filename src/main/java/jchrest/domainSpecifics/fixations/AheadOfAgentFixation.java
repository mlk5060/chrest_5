package jchrest.domainSpecifics.fixations;

import jchrest.domainSpecifics.Fixation;
import jchrest.domainSpecifics.Scene;
import jchrest.lib.Square;

/**
 * A {@link jchrest.domainSpecifics.Fixation} that, when constructed, will 
 * return the {@link jchrest.lib.Square} that is 1 {@link jchrest.lib.Square}
 * north of the result of invoking {@link 
 * jchrest.domainSpecifics.Scene#getLocationOfCreator()}.
 * 
 * <b>NOTE:</b> {@link jchrest.domainSpecifics.fixations.AheadOfAgentFixation
 * AheadOfAgentFixations} do not require any {@link 
 * jchrest.domainSpecifics.Fixation Fixations} to have been added by the {@link 
 * jchrest.architecture.Perceiver} associated with the {@link 
 * jchrest.architecture.Chrest} model that invokes {@link 
 * #this#make(jchrest.domainSpecifics.Scene, int)} prior to this invocation in 
 * order to propose a {@link jchrest.lib.Square} to fixate on.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class AheadOfAgentFixation extends Fixation{

  /**
  * Constructor.
  * 
  * @param timeThatDecidingUponThisStarts The time (in milliseconds) that it 
   * will be in the domain when {@link #this} starts to be decided upon.
   * @param timeTakenToDecideUponThis The time (in milliseconds) that it takes 
   * to decide upon the {@link jchrest.lib.Square} that {@link #this} will 
   * fixate on after starting deliberation on it.
  */
  public AheadOfAgentFixation(int timeThatDecidingUponThisStarts, int timeTakenToDecideUponThis){
    super(timeThatDecidingUponThisStarts, timeTakenToDecideUponThis);
  }
  
  /**
   * 
   * @param scene
   * @param time
   * 
   * @return A {@link jchrest.lib.Square} whose {@link 
   * jchrest.lib.Square#getRow()} is equal to <i>s</i> (the result of adding 1 
   * to {@link jchrest.lib.Square#getRow()} in context of the result of {@link 
   * jchrest.domainSpecifics.Scene#getLocationOfCreator()} in context of {@code 
   * scene}) if {@link #this#_performanceTime} is less than or equal to {@code 
   * time} and <i>s</i> is less than the result of invoking {@link 
   * jchrest.domainSpecifics.Scene#getHeight()} in context of {@code 
   * scene}.  Otherwise, {@code null} is returned.
   * 
   * @throws IllegalStateException If the creator of the {@code scene} is not 
   * specified in {@code scene}.
   */
  @Override
  public Square make(Scene scene, int time) {
    Square fixation = null;
  
    if(this._performanceTime <= time){
      Square locationOfCreator = scene.getLocationOfCreator();
      if(locationOfCreator != null){
        int rowAheadOfCreator = locationOfCreator.getRow() + 1;
        if(rowAheadOfCreator < scene.getHeight()){
          fixation = new Square(locationOfCreator.getColumn(), rowAheadOfCreator);
        }
      }
      else{
        throw new IllegalStateException(
          "Agent equipped with CHREST not found in Scene with name " + 
          scene.getName()
        );
      }
    }
    
    return fixation;
  }
  
}
