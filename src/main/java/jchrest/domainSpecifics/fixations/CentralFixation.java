package jchrest.domainSpecifics.fixations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import jchrest.domainSpecifics.Fixation;
import jchrest.domainSpecifics.Scene;
import jchrest.lib.Square;

/**
 * A {@link jchrest.domainSpecifics.Fixation} that, when constructed, will 
 * suggest either the absolute centre {@link jchrest.lib.Square} of a {@link 
 * jchrest.domainSpecifics.Scene} to fixate on or, if there is no absolute 
 * centre in the {@link jchrest.domainSpecifics.Scene} passed to {@link 
 * #this#make(jchrest.domainSpecifics.Scene, int)} (either {@link 
 * jchrest.domainSpecifics.Scene#getHeight()} or {@link 
 * jchrest.domainSpecifics.Scene#getWidth()} for the {@link 
 * jchrest.domainSpecifics.Scene} passed to {@link #this#make(
 * jchrest.domainSpecifics.Scene, int)} returns an even number), a {@link 
 * jchrest.lib.Square} around the absolute centre is returned.
 * 
 * <b>NOTE:</b> {@link jchrest.domainSpecifics.fixations.CentralFixation 
 * CentralFixations} do not require any {@link jchrest.domainSpecifics.Fixation 
 * Fixations} to have been added by the {@link jchrest.architecture.Perceiver}
 * associated with the {@link jchrest.architecture.Chrest} model that invokes
 * {@link #this#make(jchrest.domainSpecifics.Scene, int)} prior to this 
 * invocation in order to propose a {@link jchrest.lib.Square} to fixate on.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class CentralFixation extends Fixation{
  
 /**
  * Constructor.
  * 
  * @param timeThatDecidingUponThisStarts The time (in milliseconds) that it 
   * will be in the domain when {@link #this} starts to be decided upon.
   * @param timeTakenToDecideUponThis The time (in milliseconds) that it takes 
   * to decide upon the {@link jchrest.lib.Square} that {@link #this} will 
   * fixate on after starting deliberation on it.
  */
  public CentralFixation(int timeThatDecidingUponThisStarts, int timeTakenToDecideUponThis){
    super(timeThatDecidingUponThisStarts, timeTakenToDecideUponThis);
  }

  /**
   * @param scene
   * @param time
   * 
   * @return A non-blind {@link jchrest.lib.Square} either at the absolute 
   * centre or around the absolute centre of the {@link 
   * jchrest.domainSpecifics.Scene} specified or {@code null} if
   * {@link jchrest.domainSpecifics.Scene#isBlind()} evaluates to
   * {@link java.lang.Boolean#TRUE} (may happen if the {@link 
   * jchrest.domainSpecifics.Scene} has been generated using {@link 
   * jchrest.architecture.VisualSpatialField#getSceneEncoded()} or {@link 
   * #this#getPerformanceTime()} is later than the {@code time} specified.
   * 
   * If invoking {@link jchrest.lib.Scene#getWidth()} and {@link 
   * jchrest.lib.Scene#getHeight()} on the {@link 
   * jchrest.domainSpecifics.Scene} returned by {@link 
   * jchrest.architecture.Perceiver#getCurrentScene} produces odd numbers then 
   * the {@link jchrest.lib.Square} returned will be the absolute centre of the 
   * {@link jchrest.domainSpecifics.Scene}. Even numbers for {@link 
   * jchrest.lib.Scene#getWidth()} and {@link jchrest.lib.Scene#getHeight()} 
   * will result in a random choice between {@link jchrest.lib.Square Squares} 
   * that are around the absolute centre of the {@link 
   * jchrest.domainSpecifics.Scene} with equal probability.
   */
  @Override
  public Square make(Scene scene, int time) {
    Square fixation = null;
    
    if(!scene.isBlind() && this.getPerformanceTime() <= time){
      if(scene.getWidth() % 2 != 0 && scene.getHeight() % 2 != 0){
        int col = scene.getWidth()/2;
        int row = scene.getHeight()/2;
        if(!scene.isSquareBlind(col, row)){
          return new Square(col, row);
        }
      }
      else{
        //Set used to ensure no duplicate cooridnates are added so the random 
        //selection of a coordinate later has equal probability.
        Set<Square> potentialFixationsSet = new HashSet();
        int minXcor = (scene.getWidth() / 2);
        int maxXcor = (scene.getWidth() / 2);
        int minYcor = (scene.getHeight() / 2);
        int maxYcor = (scene.getHeight() / 2);
        
        if(scene.getWidth() % 2 == 0){
          minXcor--;
        }
        
        if(scene.getHeight() % 2 == 0){
          minYcor--;
        }
        
        if(scene.getSquareContents(minXcor, minYcor) != null && !scene.isSquareBlind(minXcor, minYcor)) potentialFixationsSet.add(new Square(minXcor, minYcor));
        if(scene.getSquareContents(minXcor, maxYcor) != null && !scene.isSquareBlind(minXcor, maxYcor)) potentialFixationsSet.add(new Square(minXcor, maxYcor));
        if(scene.getSquareContents(maxXcor, minYcor) != null && !scene.isSquareBlind(maxXcor, minYcor)) potentialFixationsSet.add(new Square(maxXcor, minYcor));
        if(scene.getSquareContents(maxXcor, maxYcor) != null && !scene.isSquareBlind(maxXcor, maxYcor)) potentialFixationsSet.add(new Square(maxXcor, maxYcor));
        
        //Convert set to a list so that a coordinate can be randomly selected.
        List<Square> potentialFixationsList = new ArrayList();
        potentialFixationsList.addAll(potentialFixationsSet);
        
        return potentialFixationsList.isEmpty() ? null : potentialFixationsList.get(new Random().nextInt(potentialFixationsList.size()));
      }
    }
    
    return fixation;
  }
  
}
