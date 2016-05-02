package jchrest.domainSpecifics.chess.fixations;

import java.util.List;
import jchrest.architecture.Chrest;
import jchrest.domainSpecifics.Fixation;
import jchrest.domainSpecifics.chess.ChessBoard;
import jchrest.domainSpecifics.chess.ChessDomain;
import jchrest.lib.ItemSquarePattern;
import jchrest.domainSpecifics.Scene;
import jchrest.lib.Square;

/**
 * Represents the type of {@link jchrest.lib.Fixation} made using the 
 * "salient-man" chess heuristic (see "Perception and Memory in Chess" by de 
 * Groot and Gobet, section 8.7.6).
 * 
 * <b>NOTE:</b> {@link 
 * jchrest.domainSpecifics.chess.fixations.SalientManFixation
 * SalientManFixations} do not require any {@link 
 * jchrest.domainSpecifics.Fixation Fixations} to have been added by the {@link
 * jchrest.architecture.Perceiver} associated with the {@link 
 * jchrest.architecture.Chrest} model that invokes {@link #this#make(
 * jchrest.domainSpecifics.Scene, int)} prior to this invocation in order to 
 * propose a {@link jchrest.lib.Square} to be fixated on.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class SalientManFixation extends Fixation{
  
  private final Chrest _model;
  
  /**
  * Constructor.
  * 
  * Sets the time that {@link #this} will be decided upon to be the {@code time}
  * specified plus 150ms (the value for the "Time to select a salient square" 
  * entry in table 8.2 found in "Perception and Memory in Chess" by de Groot and 
  * Gobet).
  * 
  * @param model The {@link jchrest.architecture.Chrest} model constructing 
  * {@link #this}.
  * @param time The current time in the domain (in milliseconds).
  */
  public SalientManFixation(Chrest model, int time){
    super(time + 150);
    this._model = model;
  }

  /**
   * @param time
   * 
   * @return Either a {@link jchrest.lib.Square} or {@code null}.  If any of
   * the following statements evaluate to {@link java.lang.Boolean#TRUE}, {@code 
   * null} is returned, otherwise {@link 
   * jchrest.architecture.Chrest#isExperienced(int)} is invoked on the {@link 
   * jchrest.architecture.Chrest} model that is making {@link #this} and the 
   * result is used to guide how to proceed (if {@link java.lang.Boolean#TRUE},
   * {@link jchrest.domainSpecifics.chess.ChessDomain#getOffensivePieces(
   * jchrest.lib.Scene)} is invoked, otherwise {@link 
   * jchrest.domainSpecifics.chess.ChessDomain#getBigPieces(jchrest.lib.Scene)}
   * is invoked).
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
   *      No suitable {@link jchrest.lib.Square} can be found by the method 
   *      called after checking the result of {@link 
   *      jchrest.architecture.Chrest#isExperienced(int)} in context of the 
   *      {@link jchrest.architecture.Chrest} model invoking this function. 
   *    </li>
   * </ul>
   */
  @Override
  public Square make(Scene scene, int time) {
    if(
      this.getPerformanceTime() <= time && 
      !scene.isBlind() && 
      scene.getClass().equals(ChessBoard.class)
    ){
      
      List<ItemSquarePattern> potentialFixations = this._model.isExperienced(time) ? 
        ChessDomain.getOffensivePieces((ChessBoard)scene) : 
        ChessDomain.getBigPieces((ChessBoard)scene)
      ;
      
      if(!potentialFixations.isEmpty()) {
        ItemSquarePattern fixationToMake = potentialFixations.get((new java.util.Random()).nextInt(potentialFixations.size()));
        return new Square(fixationToMake.getColumn(), fixationToMake.getRow());
      }
    }
    
    return null;
  }
}
