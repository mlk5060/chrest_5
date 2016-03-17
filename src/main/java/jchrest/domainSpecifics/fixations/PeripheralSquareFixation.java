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
 * jchrest.domainSpecifics.Scene} that is not blind/occupied by the agent 
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
 * whose coordinates are not equal to [2, 2] and which are not blind or occupied
 * by the agent that constructed the {@link jchrest.domainSpecifics.Scene}.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class PeripheralSquareFixation extends Fixation{
  
  private final Chrest _model;
  
  /**
   * @param model The {@link jchrest.architecture.Chrest} model constructing 
   * {@link #this}.
   * @param timeDecidedUpon The time {@link #this} is scheduled to be decided
   * upon in context of the external domain, in milliseconds.
   */
  public PeripheralSquareFixation(Chrest model, int timeDecidedUpon){
    super(timeDecidedUpon);
    this._model = model;
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
   *          jchrest.domainSpecifics.Scene#isEntirelyBlind()} is invoked upon 
   *          it.
   *        </li>
   *        <li>
   *          The result of invoking {@link 
   *          jchrest.domainSpecifics.Scene#isEntirelyBlind()} on {@code scene} 
   *          returns {@link java.lang.Boolean#TRUE}.
   *        </li>
   *        <li>
   *          The result of invoking {@link 
   *          jchrest.architecture.Perceiver#getFixationFieldOfView()} on the
   *          {@link jchrest.architecture.Perceiver} associated with the {@link 
   *          jchrest.architecture.Chrest} model performing this function is 0
   *          (a different {@link jchrest.lib.Square} to the one fixated on in
   *          the previous {@link jchrest.domainSpecifics.Fixation} performed
   *          will never be proposed).
   *        </li>
   *      </ul>
   *    </li>
   *    <li>
   *      {@link #this} is to be performed after the {@code time} specified.
   *    </li>
   *    <li>
   *      After attempting to select a {@link jchrest.lib.Square} from {@code 
   *      scene} n times where n = the result of multiplying the total number of
   *      {@link jchrest.lib.Square Squares} in {@code scene} by 10, no {@link 
   *      jchrest.lib.Square} is returned that:
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

      //Get the fixation field of view for the associated Perceiver and add 
      //1 since the bound provided to Random.nextInt() below is exclusive so 
      //it should be possible to get the maximum x/y displacement as 
      //stipulated by the fixationFieldOfView parameter.  Check that the
      //resulting value is greater than 1 otherwise the while loop below will
      //run forever since the x and y discplacement will always equal 0 
      //resulting in the new Square to fixate on being the same as the Square
      //fixated on in the previous Fixation.
      int fixationFieldOfView = perceiver.getFixationFieldOfView() + 1;
      if(fixationFieldOfView > 1){
        
        //Get the Scene and x/y coordinates of the Square that the most recent 
        //fixation was performed in context of.
        Scene sceneMostRecentFixationPerformedOn = mostRecentFixationPerformed.getScene();
        Integer mostRecentFixationPerformedXcor = mostRecentFixationPerformed.getColFixatedOn();
        Integer mostRecentFixationPerformedYcor = mostRecentFixationPerformed.getRowFixatedOn();

        //Assert that the Scene in the Fixation most recently performed 
        //successfully exists and that the x/y coordinates of the Square 
        //fixated on in that Fixation are set (ensures that the required 
        //information is available to try and suggest a Square for this 
        //Fixation).
        //
        //Also, assert that the Scene previously fixated on refers to the same 
        //space in the external domain as the one passed as a parameter to 
        //this function.  This ensures that this Fixation is only ever made in 
        //context of the same space that the last Fixation successfully made 
        //in context of but allows for the location of objects in the Scene 
        //passed as a parameter to this function to have changed when compared 
        //to the Scene that the last successful Fixation was made in context 
        //of.
        //
        //Ensure that neither Scene (the one previously fixated on or the one 
        //passed to this function) are entirely blind otherwise, there's no 
        //point continuing with trying to make this Fixation (over-cautious but 
        //better to be safe than sorry!).
        //
        //Finally, ensure that the time this function is being invoked is greater
        //than or equal to the performance time of this.
        if(
          sceneMostRecentFixationPerformedOn != null &&
          mostRecentFixationPerformedXcor != null &&
          mostRecentFixationPerformedYcor != null &&
          sceneMostRecentFixationPerformedOn.sameDomainSpace(scene) &&
          !sceneMostRecentFixationPerformedOn.isEntirelyBlind() &&
          !scene.isEntirelyBlind() &&
          this.getPerformanceTime() <= time
        ){

          //Now set up the x, y coordinate displacement values i.e. the number 
          //of squares from the fixation's last x, y coordinates (should be 
          //near to the periphery).  Set these to 0 initially so the while 
          //loop body below is entered.
          int xDisplacement = 0;
          int yDisplacement = 0;
          SceneObject potentialFixationContents = scene.getSquareContents(
            mostRecentFixationPerformedXcor + xDisplacement, 
            mostRecentFixationPerformedYcor + yDisplacement
          );

          //Generate the x, y coordinate displacement values.  Note that due 
          //to the randomness of displacement value generation, it may be that 
          //the x and y coordinate displacement values:
          //
          // 1. Both equal 0, resulting in the most recent fixation being made
          //    again.
          // 2. Suggest a fixation on a square that isn't in scope of the 
          //    scene being "looked-at".  
          // 3. Suggest a fixation on a blind square.
          // 4. Suggest a fixation on a square occupied by the agent making 
          //    the fixation.
          //
          //If any of these statements are true, generate new x, y 
          //displacement variables until all statements evaluate to false.
          Random r = new Random();

          //Note that it should always be possible to find a suitable Square
          //given the conditionals checked up until this point. However, to 
          //ensure that infinite loops do not occur (which only occurs under 
          //bizarre circumstances: the scene to make this Fixation on contains
          //creator objects only), only attempt to find a suitable Square for 
          //a specified number of times, n.  This value of n = the number of 
          //Squares that constitute the Scene to make the fixation on 
          //multiplied by 10.  This should give enough of an opportunity for a 
          //suitable Square to be find but will also prevent infinite loops.
          int maxAttempts = (scene.getHeight() * scene.getWidth()) * 10;
          int attempt = 1;

          while(
            attempt < maxAttempts && 
            ( 
              (xDisplacement == 0 && yDisplacement == 0) || //Statement 1
              (potentialFixationContents == null) || //Statement 2
              (potentialFixationContents.getObjectType().equals(Scene.getBlindSquareToken())) || //Statement 3
              (potentialFixationContents.getObjectType().equals(Scene.getCreatorToken())) //Statement 4
            )
          ){
            xDisplacement = r.nextInt(fixationFieldOfView);
            yDisplacement = r.nextInt(fixationFieldOfView);
            potentialFixationContents = scene.getSquareContents(
              mostRecentFixationPerformedXcor + xDisplacement, 
              mostRecentFixationPerformedYcor + yDisplacement
            );
            attempt++;
          }

          //Double-check that the potential fixation is represented in the
          //scene and that it is not a blind square or the location of the 
          //creator since these conditions may be true if the maximum number
          //of attempts has been reached and a suitable square has not been
          //found.
          if(
            (potentialFixationContents != null) &&
            (!potentialFixationContents.getObjectType().equals(Scene.getBlindSquareToken())) &&
            (!potentialFixationContents.getObjectType().equals(Scene.getCreatorToken()))
          ){
            return new Square(mostRecentFixationPerformedXcor + xDisplacement, mostRecentFixationPerformedYcor + yDisplacement);
          }
        }
      }
    }
    
    return null;
  }
}
