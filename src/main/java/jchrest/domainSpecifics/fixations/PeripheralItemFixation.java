package jchrest.domainSpecifics.fixations;

import java.util.Random;
import jchrest.architecture.Chrest;
import jchrest.architecture.Perceiver;
import jchrest.domainSpecifics.Fixation;
import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.SceneObject;
import jchrest.lib.Square;

/**
 * Represents a {@link jchrest.domainSpecifics.Fixation} that should be made on 
 * a peripheral {@link jchrest.lib.Square} in a {@link 
 * jchrest.domainSpecifics.Scene} that is not empty/blind/occupied by the agent 
 * that constructed the {@link jchrest.domainSpecifics.Scene}.  The "periphery"
 * referred to is determined by the {@link jchrest.lib.Square} fixated on by 
 * the most recent {@link jchrest.domainSpecifics.Fixation} that was 
 * successfully performed by the {@link jchrest.architecture.Chrest} model that
 * is to make {@link #this}.
 * 
 * A peripheral {@link jchrest.lib.Square} in this context is any {@link 
 * jchrest.lib.Square} except the {@link jchrest.lib.Square} last fixated on
 * within the field of view specified by the {@link 
 * jchrest.architecture.Perceiver} associated with the {@link 
 * jchrest.architecture.Chrest} model that is to make {@link #this}.  For 
 * example, if the most recent {@link jchrest.domainSpecifics.Fixation} was 
 * successfully made on a {@link jchrest.lib.Square} with coordinates [2, 2] in 
 * context of a {@link jchrest.domainSpecifics.Scene} that is 5 {@link 
 * jchrest.lib.Square Squares} by 5 {@link jchrest.lib.Square Squares}, and the
 * fixation field of view for the associated {@link 
 * jchrest.architecture.Perceiver} is set to 2, then the {@link 
 * jchrest.lib.Square} returned could be any of the {@link 
 * jchrest.lib.Square Squares} in the {@link jchrest.domainSpecifics.Scene} 
 * whose coordinates are not equal to [2, 2] and which are not blind, empty or
 * occupied by the agent that constructed the {@link 
 * jchrest.domainSpecifics.Scene}.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class PeripheralItemFixation extends Fixation{
  
  private final Chrest _model;
  private final int _maxAttempts;
  
  /**
   * @param model The {@link jchrest.architecture.Chrest} model constructing 
   * {@link #this}.
   * @param maxAttempts The maximum number of attempts that should be made to
   * find a non-blind and non-empty {@link jchrest.lib.Square} when {@link 
   * #this#make(jchrest.lib.Scene, int)} is invoked.  This value should be 
   * greater than or equal to 1.  Without this parameter, an infinite
   * loop may occur when {@link #this#make(jchrest.domainSpecifics.Scene, int)}
   * is invoked since there is no guarantee that the {@link 
   * jchrest.domainSpecifics.Scene} previously fixated on contains any non-blind
   * and non-empty {@link jchrest.domainSpecifics.SceneObject SceneObjects} in 
   * its periphery.
   * @param timeThatDecidingUponThisStarts The time (in milliseconds) that it 
   * will be in the domain when {@link #this} starts to be decided upon.
   * @param timeTakenToDecideUponThis The time (in milliseconds) that it takes 
   * to decide upon the {@link jchrest.lib.Square} that {@link #this} will 
   * fixate on after starting deliberation on it.
   */
  public PeripheralItemFixation(Chrest model, int maxAttempts, int timeThatDecidingUponThisStarts, int timeTakenToDecideUponThis){
    super(timeThatDecidingUponThisStarts, timeTakenToDecideUponThis);
    this._model = model;
    
    if(maxAttempts < 1){
      throw new IllegalArgumentException("The maximum number of attempts specified is < 1");
    }
    else{
      this._maxAttempts = maxAttempts;
    }
  }

  /**
   * @param scene
   * @param time
   * 
   * @return A {@link jchrest.lib.Square} in the periphery of the {@code scene} 
   * specified that is not blind/occupied by the agent that constructed {@code 
   * scene}. The "periphery" is any {@link jchrest.lib.Square} except the {@link
   * jchrest.lib.Square} last fixated on that falls within the fixation field of 
   * view specified by the {@link jchrest.architecture.Perceiver} associated 
   * with the {@link jchrest.architecture.Chrest} model that is to make {@link 
   * #this} (see {@link jchrest.architecture.Perceiver#getFixationFieldOfView()}.
   * However, if any of the following conditions evaluate to true, {@code null} 
   * is returned:
   * 
   * <ul>
   *    <li>
   *      There has not been any {@link jchrest.domainSpecifics.Fixation} 
   *      performed successfully by the {@link jchrest.architecture.Chrest} 
   *      model at the {@code time} this function is invoked.
   *    </li>
   *    <li>
   *      For the {@link jchrest.domainSpecifics.Fixation} performed most 
   *      recently by the {@link jchrest.architecture.Perceiver} associated with 
   *      the {@link jchrest.architecture.Chrest} model making {@link #this}:
   *      <ul>
   *        <li> 
   *          The result of invoking {@link 
   *          jchrest.domainSpecifics.Fixation#getScene()} returns {@code null}.
   *        </li>
   *        <li>
   *          The result of invoking {@link 
   *          jchrest.domainSpecifics.Fixation#getColFixatedOn()} returns {@code null}.
   *        </li>
   *        <li>
   *          The result of invoking {@link 
   *          jchrest.domainSpecifics.Fixation#getRowFixatedOn()} returns {@code null}.
   *        </li>
   *        <li>
   *          The result of invoking {@link 
   *          jchrest.domainSpecifics.Fixation#getScene()} yields a {@link 
   *          jchrest.domainSpecifics.Scene} that returns {@link 
   *          java.lang.Boolean#FALSE} when {@link 
   *          jchrest.domainSpecifics.Scene#sameDomainSpace(
   *          jchrest.domainSpecifics.Scene)} is invoked upon it and {@code 
   *          scene} is passed as a parameter.
   *        </li>
   *        <li>
   *          The result of invoking {@link 
   *          jchrest.domainSpecifics.Fixation#getScene()} yields a {@link 
   *          jchrest.domainSpecifics.Scene} that returns {@link 
   *          java.lang.Boolean#TRUE} when {@link 
   *          jchrest.domainSpecifics.Scene#isBlind()} is invoked upon 
   *          it.
   *        </li>
   *        <li>
   *          The result of invoking {@link 
   *          jchrest.domainSpecifics.Scene#isBlind()} on {@code scene} 
   *          returns {@link java.lang.Boolean#TRUE}.
   *        </li>
   *      </ul>
   *    <li>
   *      {@link #this} is to be performed after the {@code time} specified.
   *    </li>
   *    <li>
   *      After attempting to select a {@link jchrest.lib.Square} from {@code 
   *      scene} n times where n = the {@code maxAttempts} parameter supplied
   *      to {@link #this#PeripheralItemFixation(jchrest.architecture.Chrest, 
   *      int, int) no {@link jchrest.lib.Square} is returned that:
   *      <ul>
   *        <li>
   *          Is different to the {@link jchrest.lib.Square} selected in the 
   *          {@link jchrest.domainSpecifics.Fixation} performed most recently 
   *          by the {@link jchrest.architecture.Perceiver} associated with 
   *          the {@link jchrest.architecture.Chrest} model making {@link 
   *          #this}. 
   *        </li>
   *        <li>Is represented in the {@code scene} specified.</li>
   *        <li>
   *          Does not contain a blind {@link 
   *          jchrest.domainSpecifics.SceneObject}.
   *        </li>
   *        <li>
   *          Does not contain an empty {@link 
   *          jchrest.domainSpecifics.SceneObject}.
   *        </li>
   *        <li>
   *          Does not contain a {@link jchrest.domainSpecifics.SceneObject} 
   *          that denotes something other than the creator of {@code scene}.
   *        </li>
   *      </ul>
   *    </li>
   * </ul>
   */
  @Override
  public Square make(Scene scene, int time) {

    //Get the most recent fixations performed and check that a Fixation of this
    //type can be performed.  Note that the performance time check of this
    //Fixation is performed AFTER the check for Fixations performed prior to
    //this function being invoked existing.  This ensures chronological 
    //continuity: if the CHREST model making this fixation does not exist or 
    //hasn't made any Fixations at the time specified, the function will return
    //null.  Otherwise, it can be reasonably assumed that the performance time
    //for this Fixation is AFTER the model has been created and made previous
    //Fixations.
    Perceiver perceiver = this._model.getPerceiver();
    Fixation mostRecentFixationPerformed = perceiver.getMostRecentFixationPerformed(time);
    if(mostRecentFixationPerformed != null){

      //Get the Scene and x/y coordinates of the Square that the most recent 
      //fixation was performed in context of.
      Scene sceneMostRecentFixationPerformedOn = mostRecentFixationPerformed.getScene();
      Integer mostRecentFixationPerformedXcor = mostRecentFixationPerformed.getColFixatedOn();
      Integer mostRecentFixationPerformedYcor = mostRecentFixationPerformed.getRowFixatedOn();

      //Assert that the Scene in the Fixation most recently performed 
      //successfully exists and that the x/y coordinates of the Square fixated 
      //on in that Fixation are set (ensures that the required information is
      //available to try and suggest a Square for this Fixation).
      //
      //Also, assert that the Scene previously fixated on refers to the same 
      //space in the external domain as the one passed as a parameter to this 
      //function.  This ensures that this Fixation is only ever made in 
      //context of the same space that the last Fixation successfully made in
      //context of but allows for the location of objects in the Scene passed 
      //as a parameter to this function to have changed when compared to the 
      //Scene that the last successful Fixation was made in context of.
      //
      //Ensure that neither Scene (the one previously fixated on or the one 
      //passed to this function) are entirely blind otherwise, there's no point 
      //continuing with trying to make this Fixation (over-cautious but better 
      //to be safe than sorry!).
      //
      //Finally, ensure that the time this function is being invoked is greater
      //than or equal to the performance time of this.
      if(
        sceneMostRecentFixationPerformedOn != null &&
        mostRecentFixationPerformedXcor != null &&
        mostRecentFixationPerformedYcor != null &&
        sceneMostRecentFixationPerformedOn.sameDomainSpace(scene) &&
        !sceneMostRecentFixationPerformedOn.isBlind() &&
        !scene.isBlind() &&
        this.getPerformanceTime() <= time
      ){

        //Displace the x/y coordinates of the previous fixation randomly 
        //according to the Perceiver's fixation field of vision.  Due to the 
        //randomness of x and y coordinate displacement value generation, it 
        //may be that the x and y coordinate displacement values:
        //
        // 1. Both equal 0, resulting in the most recent Fixation being made
        //    again.
        // 2. Suggest a Square that isn't in scope of the Scene being 
        //    "looked-at".  
        // 3. Suggest a blind Square.
        // 4. Suggest an empty Square.
        // 5. Suggest a Square occupied by the agent making this Fixation.
        //
        //If all these statements are true, and the number of attempts made
        //to find a suitable Square are less than the maximum number of 
        //attempts specified, generate new x/y displacement variables.

        //Get the fixation field of view for the associated Perceiver and add 
        //1 since the bound provided to Random.nextInt() is exclusive so it 
        //should be possible to get the maximum x/y displacement as stipulated 
        //by the fixationFieldOfView parameter.
        int fixationFieldOfView = perceiver.getFixationFieldOfView() + 1;
        Random r = new Random();

        for(int attempt = 0; attempt < this._maxAttempts; attempt++){

          int xDisplacement = r.nextInt(fixationFieldOfView);
          int yDisplacement = r.nextInt(fixationFieldOfView);

          if(xDisplacement != 0 && yDisplacement != 0){ //Statement 1

            int fixationCol = (r.nextInt(2) == 0 ? 
              mostRecentFixationPerformedXcor + xDisplacement :
              mostRecentFixationPerformedXcor - xDisplacement
            );

            int fixationRow = (r.nextInt(2) == 0 ? 
              mostRecentFixationPerformedYcor + yDisplacement :
              mostRecentFixationPerformedYcor - yDisplacement
            );

            SceneObject potentialFixationContents = scene.getSquareContents(fixationCol, fixationRow);

            if(
              potentialFixationContents != null && //Statement 2
              (
                !potentialFixationContents.getObjectType().equals(Scene.getBlindSquareToken()) && //Statement 3
                !potentialFixationContents.getObjectType().equals(Scene.getEmptySquareToken()) && //Statement 4
                !potentialFixationContents.getObjectType().equals(Scene.getCreatorToken()) //Statement 5
              )
            ){
              return new Square(fixationCol, fixationRow);
            }
          }
        }
      }
    }
    
    return null;
  }
  
}
