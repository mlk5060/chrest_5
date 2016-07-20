package jchrest.domainSpecifics.chess.fixations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jchrest.architecture.Chrest;
import jchrest.domainSpecifics.Fixation;
import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.chess.ChessBoard;
import jchrest.lib.Square;

/**
 * Represents a "global" strategy in chess (see the "Global strategies" fixation
 * heuristic description in section 8.7.6 of "Perception and Memory in Chess" by 
 * de Groot and Gobet).
 * 
 * <b>NOTE:</b> {@link 
 * jchrest.domainSpecifics.chess.fixations.GlobalStrategyFixation
 * GlobalStrategyFixations} require a {@link jchrest.domainSpecifics.Fixation} 
 * focused on a {@link jchrest.lib.Square} containing a non-blind {@link 
 * jchrest.domainSpecifics.SceneObject} to have been added by the {@link 
 * jchrest.architecture.Perceiver} associated with the {@link 
 * jchrest.architecture.Chrest} model that invokes {@link 
 * #this#make(jchrest.domainSpecifics.Scene, int)} prior to this invocation in 
 * order to propose a {@link jchrest.lib.Square} to fixate on.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class GlobalStrategyFixation extends Fixation{
  
  private final Chrest _model;
  
  public GlobalStrategyFixation(Chrest model, int time){
    super(time + 150);
    this._model = model;
  }

  /**
   * @param time
   * 
   * @return Either a {@link jchrest.lib.Square} on a quadrant of the {@code 
   * scene} that has not yet been fixated on in context of the current set of 
   * {@link jchrest.domainSpecifics.Fixation Fixations} being made by the {@link
   * jchrest.architecture.Chrest} model invoking this function or {@code null} 
   * if any of the following statements evaluate to {@link 
   * java.lang.Boolean#TRUE}:
   * 
   * <ul>
   *    <li>
   *      The result of {@link #this#getPerformanceTime()} is greater than the
   *      {@code time} specified.
   *    </li>
   *    <li>
   *      The result of invoking {@link 
   *      jchrest.domainSpecifics.Scene#isBlind()} on the {@code scene}
   *      specified returns {@link java.lang.Boolean#TRUE}.
   *    </li>
   *    <li>
   *      The result of invoking {@link java.lang.Object#getClass()} on the 
   *      {@code scene} specified causes {@link java.lang.Object#equals(
   *      java.lang.Object)} to return {@link java.lang.Boolean#FALSE} when
   *      compared against {@link jchrest.domainSpecifics.chess.ChessBoard}.
   *    </li>
   *    <li>
   *      The result of invoking {@link 
   *      jchrest.architecture.Perceiver#getFixationsPerformed(int)} in context
   *      of the {@link jchrest.architecture.Perceiver} associated with the
   *      {@link jchrest.architecture.Chrest} model invoking this function and
   *      the {@code time} specified returns {@code null} or an empty {@link
   *      java.util.List}
   *    </li>
   *    <li>
   *      No suitable {@link jchrest.lib.Square} can be found.
   *    </li>
   * </ul>
   * 
   * The "quadrant checking" mentioned above works by calculating if the 
   * white-side, black-side, queen-side and king-side of the {@link 
   * jchrest.domainSpecifics.chess.ChessBoard} represented by the {@code scene}
   * specified have been fixated on yet.  If any quadrant has not been, all 
   * {@link jchrest.lib.Square Squares} within the quadrant are added to a 
   * {@link java.util.List} and a {@link jchrest.lib.Square} is selected at 
   * random from this {@link java.util.List}.
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
    List<Fixation> mostRecentFixationsPerformed = this._model.getPerceiver().getFixationsPerformed(time);
    
    if(
      mostRecentFixationsPerformed != null &&
      !mostRecentFixationsPerformed.isEmpty() &&
      scene.getClass().equals(ChessBoard.class) &&
      !scene.isBlind() && 
      this.getPerformanceTime() <= time
    ){
        
      List<Integer> colsFixatedOn = new ArrayList();
      List<Integer> rowsFixatedOn = new ArrayList();

      for(Fixation fixation : mostRecentFixationsPerformed){

        //Get the x and y coordinates of the fixation.  Note that the data 
        //type of these values can be safely set to "int" rather than 
        //"Integer" since the fixation should be performed and therefore, the
        //values returned should not be null.
        int x = fixation.getColFixatedOn();
        int y = fixation.getRowFixatedOn();
        if(!colsFixatedOn.contains(x)) colsFixatedOn.add(x);
        if(!rowsFixatedOn.contains(y)) rowsFixatedOn.add(y);
      }

      //Get board quadrants to potentially fixate on based upon col and rows
      //already fixated on.
      List<Integer> colsToPotentiallyFixateOn = new ArrayList();
      List<Integer> rowsToPotentiallyFixateOn = new ArrayList();

      //If the queen-side of the board has not been fixated on, add all 
      //queen-side coordinates to the columns to potentially fixate on.
      if(
        !colsFixatedOn.contains(0) &&
        !colsFixatedOn.contains(1) &&
        !colsFixatedOn.contains(2) &&
        !colsFixatedOn.contains(3) 
      ){
        for(int col = 0; col < 4; col++){
          colsToPotentiallyFixateOn.add(col);
        }
      }

      //If the king-side of the board has not been fixated on, add all 
      //king-side coordinates to the columns to potentially fixate on.
      if(
        !colsFixatedOn.contains(4) &&
        !colsFixatedOn.contains(5) &&
        !colsFixatedOn.contains(6) &&
        !colsFixatedOn.contains(7) 
      ){
        for(int col = 4; col < 8; col++){
          colsToPotentiallyFixateOn.add(col);
        }
      }

      //If the white-side of the board has not been fixated on, add all 
      //white-side coordinates to the rows to potentially fixate on.
      if(
        !rowsFixatedOn.contains(0) &&
        !rowsFixatedOn.contains(1) &&
        !rowsFixatedOn.contains(2) &&
        !rowsFixatedOn.contains(3) 
      ){
        for(int row = 0; row < 4; row++){
          rowsToPotentiallyFixateOn.add(row);
        }
      }

      //If the black-side of the board has not been fixated on, add all 
      //black-side coordinates to the rows to potentially fixate on.
      if(
        !rowsFixatedOn.contains(4) &&
        !rowsFixatedOn.contains(5) &&
        !rowsFixatedOn.contains(6) &&
        !rowsFixatedOn.contains(7) 
      ){
        for(int row = 4; row < 7; row++){
          rowsToPotentiallyFixateOn.add(row);
        }
      }

      //Generate a list of squares to potentially fixate on now.  Note that 
      //it should always be possible to fixate on the centre of the board.
      List<Square> potentialFixations = new ArrayList();

      //Add centre squares of board.
      potentialFixations.add(new Square(3, 3));
      potentialFixations.add(new Square(3, 4));
      potentialFixations.add(new Square(4, 3));
      potentialFixations.add(new Square(4, 4));

      //Add all combinations of rows and columns not fixated on yet to the list
      //of potential fixations
      if(!colsToPotentiallyFixateOn.isEmpty() && !rowsToPotentiallyFixateOn.isEmpty()){
        for(Integer col : colsToPotentiallyFixateOn){
          for(Integer row : rowsToPotentiallyFixateOn){
            Square potentialFixation = new Square(col, row);
            if(!potentialFixations.contains(potentialFixation)){
              potentialFixations.add(potentialFixation);
            }
          }
        }
      }

      return potentialFixations.get(new Random().nextInt(potentialFixations.size()));
    }
    
    return null;
  }
}
