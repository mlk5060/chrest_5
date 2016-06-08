package jchrest.domainSpecifics.fixations;

import java.util.ArrayList;
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
   * @param timeThatDecidingUponThisStarts The time (in milliseconds) that it 
   * will be in the domain when {@link #this} starts to be decided upon.
   * @param timeTakenToDecideUponThis The time (in milliseconds) that it takes 
   * to decide upon the {@link jchrest.lib.Square} that {@link #this} will 
   * fixate on after starting deliberation on it.
   */
  public PeripheralSquareFixation(Chrest model, int timeThatDecidingUponThisStarts, int timeTakenToDecideUponThis){
    super(timeThatDecidingUponThisStarts, timeTakenToDecideUponThis);
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
   *          jchrest.domainSpecifics.Scene#isBlind()} is invoked upon 
   *          it.
   *        </li>
   *        <li>
   *          The result of invoking {@link 
   *          jchrest.domainSpecifics.Scene#isBlind()} on {@code scene} 
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
    this._model.printDebugStatement("===== PeripheralSquareFixation.make() =====");
    this._model.printDebugStatement("- Requested at time " + time);
    
    Square squareToFixateOn = null;
    
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
      this._model.printDebugStatement("- A Fixation has previously been performed: " + mostRecentFixationPerformed.toString());

      //Get the fixation field of view for the associated Perceiver.  Check that 
      //the value is greater than 0 otherwise the column and row displacement 
      //will always equal 0 resulting in the new Square to fixate on being the 
      //same as the Square fixated on in the previous Fixation.
      int fixationFieldOfView = perceiver.getFixationFieldOfView();
      if(fixationFieldOfView > 0){
        this._model.printDebugStatement("- Perceiver's Fixation field of view (" + fixationFieldOfView + ") is greater than 0 so a peripheral Fixation is possible");
        
        //Get the Scene and column/row coordinates that the most recent fixation 
        //performed fixated on.
        Scene sceneMostRecentFixationPerformedOn = mostRecentFixationPerformed.getScene();
        Integer colFixatedOnByMostRecentFixationPerformed = mostRecentFixationPerformed.getColFixatedOn();
        Integer rowFixatedOnByMostRecentFixationPerformed = mostRecentFixationPerformed.getRowFixatedOn();

        //Assert that the Scene in the Fixation most recently performed exists 
        //and that the column/row coordinates fixated on are set (ensures that 
        //the required information is available to suggest a Square for this 
        //Fixation).
        //
        //Also, assert that the Scene previously fixated on refers to the same 
        //space in the external domain as the one passed as a parameter to 
        //this function.  This ensures that this Fixation is only ever made in 
        //context of the same space that the last Fixation performed was made in 
        //context of but allows for the locations of SceneObjects in these two
        //Scenes to differ. Also, ensure that neither Scene are entirely blind 
        //otherwise, there's no point continuing with trying to make this 
        //Fixation.
        //
        //Finally, ensure that the time this function is being invoked is 
        //greater than or equal to the performance time of this Fixation.
        if(this._model.debug()){
          this._model.printDebugStatement("- Checking if the following statements all evaluate to true:");
          this._model.printDebugStatement("  ~ Scene fixated on by the most recent Fixation performed is not null: " + (sceneMostRecentFixationPerformedOn != null));
          this._model.printDebugStatement("  ~ Column fixated on by the most recent Fixation performed is not null: " + (colFixatedOnByMostRecentFixationPerformed != null));
          this._model.printDebugStatement("  ~ Row fixated on by the most recent Fixation performed is not null: " + (rowFixatedOnByMostRecentFixationPerformed != null));
          this._model.printDebugStatement("  ~ Scene fixated on by the most recent Fixation performed is the same domain space as the Scene being fixated on by this Fixation: " + (sceneMostRecentFixationPerformedOn.sameDomainSpace(scene)));
          this._model.printDebugStatement("  ~ Scene fixated on by the most recent Fixation is not entirely blind: " + (!sceneMostRecentFixationPerformedOn.isBlind()));
          this._model.printDebugStatement("  ~ Scene being fixated on by this Fixation is not entirely blind: " + (!scene.isBlind()));
          this._model.printDebugStatement("  ~ Performance time of this Fixation is <= the time this method was requested: " + (this.getPerformanceTime() <= time));
        }
        if(
          sceneMostRecentFixationPerformedOn != null &&
          colFixatedOnByMostRecentFixationPerformed != null &&
          rowFixatedOnByMostRecentFixationPerformed != null &&
          sceneMostRecentFixationPerformedOn.sameDomainSpace(scene) &&
          !sceneMostRecentFixationPerformedOn.isBlind() &&
          !scene.isBlind() &&
          this.getPerformanceTime() <= time
        ){
          this._model.printDebugStatement("- All statements evaluate to true, continuing");

          //Note that it should always be possible to find a suitable Square
          //given the conditionals checked up until this point. However, to 
          //ensure that infinite loops do not occur (which only occurs under 
          //bizarre circumstances e.g. the scene to make this Fixation on 
          //contains creator objects only), only attempt to find a suitable 
          //Square for the number of Squares that can be fixated on.
          //
          //To calculate the number of Squares that can be fixated on, suppose
          //the current Fixation field of view parameter is set to 2.  25
          //Squares in total should be seen when a Fixation is made. To 
          //calculate this for any given Fixation field of view parameter, add 
          //the current Fixation field of view parameter to itself (2 + 2) since 
          //this parameter only stipulates how many Squares along 1 ordinal 
          //compass direction can be seen FROM THE PREVIOUS SQUARE FIXATED ON.  
          //Add 1 to this value now since the column or row the previous 
          //Fixation was made on is not included, this gives 5; the total number 
          //of Squares along one spatial dimension, either the x or y axis of a 
          //Scene that falls within a Fixation's field of view.  Since this is 
          //just the value for one spatial dimension (x/y) raise this number by 
          //2 (5 ^ 2) to get the total number of Squares that can be seen along 
          //both spatial dimensions, i.e. the total number of Squares in the 
          //Fixation's field of view.
          int singleSpatialDimension = (fixationFieldOfView + fixationFieldOfView) + 1;
          int numberSquaresThatCanBeFixatedOn = (int)(Math.pow((double)singleSpatialDimension, (double)2));
          this._model.printDebugStatement("- Number of Squares that could be fixated on = " + numberSquaresThatCanBeFixatedOn);
          
          //Initialise the column and row coordinate displacement values i.e. 
          //the number of Squares along the x and y axis of the Scene from the 
          //last Fixation performed's coordinates that should be potentially 
          //fixated on; otherwise known as the fixationFieldOfView parameter.
          //Initialise these to 0 so that the "fixation attempt" while loop 
          //below is entered.  This can be thought of as starting to make this
          //Fixation from the Square previously performed.
          Random r = new Random();
          int colDisplacement = 0;
          int rowDisplacement = 0;
          SceneObject potentialFixationContents = scene.getSquareContents(
            colFixatedOnByMostRecentFixationPerformed + colDisplacement, 
            rowFixatedOnByMostRecentFixationPerformed + rowDisplacement
          );
          
          //Due to the randomness of displacement value generation, it may be 
          //that the Square selected:
          //
          // 1. Is the Square fixated on by the most recent Fixation performed 
          //    again (column and row displacement may both equal 0)
          // 2. Is a square that isn't in scope of the Scene being fixated on.  
          // 3. Is blind.
          // 4. Is occupied by the agent making this Fixation.
          //
          //If any of these statements are true, generate new column/row
          //displacement variables until either all statements evaluate to false 
          //or every displacement combination has been tried (all Squares in the
          //Fixation field of view have been fixated on).
          ArrayList<Square> displacementsTried = new ArrayList();
          while(
            ( 
              (colDisplacement == 0 && rowDisplacement == 0) || //Statement 1
              (potentialFixationContents == null) || //Statement 2
              (potentialFixationContents.getObjectType().equals(Scene.getBlindSquareToken())) || //Statement 3
              (potentialFixationContents.getObjectType().equals(Scene.getCreatorToken())) //Statement 4
            ) &&
            displacementsTried.size() < numberSquaresThatCanBeFixatedOn
          ){
            Square displacementTried = new Square(colDisplacement, rowDisplacement);
            if (!displacementsTried.contains(displacementTried)) displacementsTried.add(displacementTried);
            
            //Add 1 to the random number generator bound since it is exclusive. 
            //So if the fixationFieldOfView parameter is set to 2, integers 0 -> 
            //2 should be able to be generated rather than just 0 or 1.
            colDisplacement = r.nextInt(fixationFieldOfView + 1);
            rowDisplacement = r.nextInt(fixationFieldOfView + 1);
            
            //The col and row displacement generation above would only ever 
            //allow a Fixation to be made to the east or north of the previous
            //Fixation, displacements should also be possible to the west and
            //south.  To enable this, "flip a coin", if "heads", displace to
            //the north/east, if "tails", displace to the south/west.
            colDisplacement = (r.nextDouble() < 0.5 ? colDisplacement : colDisplacement * -1);
            rowDisplacement = (r.nextDouble() < 0.5 ? rowDisplacement : rowDisplacement * -1);
            
            //Add the col/row displacement to the col/row fixated on previously
            //(negative displacements will negatively affect the col/row 
            //previously fixated on).
            potentialFixationContents = scene.getSquareContents(
              colFixatedOnByMostRecentFixationPerformed + colDisplacement,
              rowFixatedOnByMostRecentFixationPerformed + rowDisplacement
            );
          }
          
          this._model.printDebugStatement("- The following displacements failed: " + displacementsTried.toString());
          this._model.printDebugStatement(
            "- Last displacement tried was (" + colDisplacement + ", " + 
            rowDisplacement + ")" + " and " +
            (potentialFixationContents == null ? 
              "was on a Square not represented in the Scene" : 
              "was on a Square represented in the Scene that contained the " +
              "following SceneObject: " + potentialFixationContents.toString()
            )
          );

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
            squareToFixateOn = new Square(colFixatedOnByMostRecentFixationPerformed + colDisplacement, rowFixatedOnByMostRecentFixationPerformed + rowDisplacement);
            this._model.printDebugStatement(
              "- Since the last displacement tried was on a Square represented " +
              "in the Scene and this Square was not blind or occupied by myself, " +
              "this Fixation will be made on this Square (scene-specific coordinates: " +
              squareToFixateOn + ")"
            );
          }
          else{
            this._model.printDebugStatement(
              "- Since the last displacement tried was not on a Square represented " +
              "in the Scene or it was but the Square was blind or occupied by myself, " +
              "this Fixation has failed"
            );
          }
        }
        else{
          this._model.printDebugStatement("  ~ Some statements evaluated to false, exiting");
        }
      }
      else{
        this._model.printDebugStatement("- Perceiver's Fixation field of view is not greater than 0 so a peripheral Fixation is not possible, exiting");
      }
    }
    else{
      this._model.printDebugStatement("- A Fixation has not been performed previously, exiting");
    }
    
    this._model.printDebugStatement("- Returning " + (squareToFixateOn == null ? "null" : squareToFixateOn.toString()));
    this._model.printDebugStatement("===== RETURN PeripheralSquareFixation.make() =====");
    return squareToFixateOn;
  }
}
