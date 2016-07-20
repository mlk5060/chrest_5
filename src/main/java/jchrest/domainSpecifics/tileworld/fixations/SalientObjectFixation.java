package jchrest.domainSpecifics.tileworld.fixations;

import java.util.ArrayList;
import java.util.Random;
import jchrest.domainSpecifics.Fixation;
import jchrest.domainSpecifics.Scene;
import jchrest.lib.Square;

/**
 * Represents the type of {@link jchrest.lib.Fixation} made by considering what 
 * Tileworld objects are salient (tiles, holes or opponents).
 * 
 * <b>NOTE:</b> {@link 
 * jchrest.domainSpecifics.tileworld.fixations.SalientObjectFixation 
 * SalientObjectFixations} do not require any previous {@link 
 * jchrest.domainSpecifics.Fixation Fixations} to have been attempted by the 
 * {@link jchrest.architecture.Perceiver} associated with the {@link 
 * jchrest.architecture.Chrest} model that invokes {@link #this#make(
 * jchrest.domainSpecifics.Scene, int)} prior to this invocation in order to 
 * propose a {@link jchrest.lib.Square} to be fixated on.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class SalientObjectFixation extends Fixation{

  /**
   * Constructor.
   * 
   * @param timeDecidedUpon The time {@link #this} should be decided upon (in 
   * milliseconds).
   */
  public SalientObjectFixation(int timeDecidedUpon) {
    super(timeDecidedUpon);
  }
  
  /**
   * 
   * @param scene
   * @param time
   * 
   * @return If {@link #this#_performanceTime} is less than or equal to the 
   * {@code time} specified, a random {@link jchrest.lib.Square} in the {@code 
   * scene} specified that doesn't contain a {@link 
   * jchrest.domainSpecifics.SceneObject} whose {@link 
   * jchrest.domainSpecifics.SceneObject#getObjectType()} doesn't return {@link 
   * jchrest.domainSpecifics.Scene#getCreatorToken()}, {@link 
   * jchrest.domainSpecifics.Scene#getBlindSquareToken()} or {@link 
   * jchrest.domainSpecifics.Scene#getEmptySquareToken()} will be returned with
   * equal probability. Otherwise, {@code null} will be returned.
   */
  @Override
  public Square make(Scene scene, int time) {
    Square fixation = null;
    if(this._performanceTime <= time){
      
      ArrayList<Square> potentialFixations = new ArrayList();
      for(int col = 0; col < scene.getWidth(); col++){
        for(int row = 0; row < scene.getHeight(); row++){
          String objectType = scene.getSquareContents(col, row).getObjectType();
          if(
            !objectType.equals(Scene.BLIND_SQUARE_TOKEN) && 
            !objectType.equals(Scene.CREATOR_TOKEN) &&
            !objectType.equals(Scene.EMPTY_SQUARE_TOKEN)
          ){
            potentialFixations.add(new Square(col, row));
          }
        }
      }
      
      if(!potentialFixations.isEmpty()){
        fixation = potentialFixations.get(new Random().nextInt(potentialFixations.size()));
      }
    }
    
    return fixation;
  }
  
}
