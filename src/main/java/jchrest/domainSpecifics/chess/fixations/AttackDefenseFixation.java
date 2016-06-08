package jchrest.domainSpecifics.chess.fixations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jchrest.architecture.Chrest;
import jchrest.domainSpecifics.Fixation;
import jchrest.domainSpecifics.chess.ChessBoard;
import jchrest.domainSpecifics.chess.ChessDomain;
import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.SceneObject;
import jchrest.domainSpecifics.chess.ChessObject;
import jchrest.lib.Square;

/**
 * Represents the type of {@link jchrest.lib.Fixation} made using the 
 * "attack-defense" chess heuristic (see "Perception and Memory in Chess" by de 
 * Groot and Gobet, section 8.7.6).
 * 
 * <b>NOTE:</b> {@link 
 * jchrest.domainSpecifics.chess.fixations.AttackDefenseFixation
 * AttackDefenseFixations} require a {@link jchrest.domainSpecifics.Fixation} 
 * focused on a {@link jchrest.lib.Square} containing a non-blind and non-empty 
 * {@link jchrest.domainSpecifics.SceneObject} to have been added by the {@link 
 * jchrest.architecture.Perceiver} associated with the {@link 
 * jchrest.architecture.Chrest} model that invokes {@link 
 * #this#make(jchrest.domainSpecifics.Scene, int)} to have been added prior to 
 * this invocation to propose a {@link jchrest.lib.Square} to fixate on.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class AttackDefenseFixation extends Fixation{
  
  private final ChessBoard _board;
  private Square _squareToFixateOn;
  
  /**
   * Constructor.
   * 
   * Initially sets the time that {@link #this} will be decided upon to be the 
   * {@code time} specified.  The constructor will then retrieve the most recent 
   * {@link jchrest.domainSpecifics.Fixation} performed.  If this retrieval is 
   * successful, the chess piece fixated on is determined and used to generate 
   * potential {@link jchrest.lib.Square Squares} to make {@link #this}
   * on.  If there are suitable {@link jchrest.lib.Square Squares} to make 
   * {@link #this} on in context of the {@code board} specified, one is randomly 
   * selected and assigned to be the {@link jchrest.lib.Square} to return when 
   * {@link #this#make(jchrest.lib.Scene, int)} is invoked and the time {@link 
   * #this} is decided upon is updated to the {@code time} specified plus the 
   * value of the {@code model} specified's {@link 
   * jchrest.architecture.Chrest#getTimeToAccessVisualSpatialField()} value
   * plus the value of the {@code model} specified's {@link 
   * jchrest.architecture.Chrest#getTimeToMoveVisualSpatialFieldObject()} 
   * multiplied by the number of {@link jchrest.lib.Square Squares} considered 
   * for movement to by the chess piece fixated on.
   * 
   * Thus, the constructor sets the time {@link #this} is decided upon according
   * to the values for the "Base time to generate a move" and "Time to traverse
   * a square" entries in table 8.2 found in "Perception and Memory in Chess" by 
   * de Groot and Gobet).
   * 
   * @param model
   * @param board
   * @param timeThatDecidingUponThisStarts The time (in milliseconds) that it 
   * will be in the domain when {@link #this} starts to be decided upon.
   */
  public AttackDefenseFixation(Chrest model, ChessBoard board, int timeThatDecidingUponThisStarts){

    super(timeThatDecidingUponThisStarts, model.getTimeToAccessVisualSpatialField());
    
    if(timeThatDecidingUponThisStarts < model.getCreationTime()){
      throw new IllegalArgumentException(
        "The time that the AttackDefenseFixation constructor was invoked (" + 
        timeThatDecidingUponThisStarts + ") is earlier than the time the " +
        "associated CHREST model was created (" + model.getCreationTime() + ")"
       );
    }
    
    //Set the board variable now since this can not be null otherwise the "make"
    //function will throw a NullPointerException since there is a conditional 
    //required that makes use of this instance variable.
    this._board = board;
    
    if(!board.isBlind()){
      
      //Get the most recent fixations performed and check that a Fixation of this
      //type can be performed.  Note that the performance time check of this
      //Fixation is performed AFTER the check for Fixations performed prior to
      //this function being invoked existing.  This ensures chronological 
      //continuity: if the CHREST model making this fixation does not exist or 
      //hasn't made any Fixations at the time specified, the function will return
      //null.  Otherwise, it can be reasonably assumed that the performance time
      //for this Fixation is AFTER the model has been created and made previous
      //Fixations.
      Fixation mostRecentFixationPerformed = model.getPerceiver().getMostRecentFixationPerformed(timeThatDecidingUponThisStarts);
      if(mostRecentFixationPerformed != null){
        
        //Get most recent fixation performed info and check that its all OK.
        Integer mostRecentFixationPerformedCol = mostRecentFixationPerformed.getColFixatedOn();
        Integer mostRecentFixationPerformedRow = mostRecentFixationPerformed.getRowFixatedOn();
        SceneObject mostRecentFixationPerformedObjectSeen = mostRecentFixationPerformed.getObjectSeen();

        if(
          mostRecentFixationPerformedCol != null &&
          mostRecentFixationPerformedRow != null &&
          mostRecentFixationPerformedObjectSeen != null &&
          mostRecentFixationPerformedObjectSeen.getClass().equals(ChessObject.class)
        ){
          Square squareMostRecentlyFixatedOn = new Square(mostRecentFixationPerformedCol, mostRecentFixationPerformedRow);
          String classOfObjectMostRecentlyFixatedOn = mostRecentFixationPerformedObjectSeen.getObjectType();

          if(
            !classOfObjectMostRecentlyFixatedOn.equals(Scene.getBlindSquareToken()) &&
            !classOfObjectMostRecentlyFixatedOn.equals(Scene.getEmptySquareToken())
          ){
            Object[] potentialFixationsAndSquaresConsidered = 
              classOfObjectMostRecentlyFixatedOn.equalsIgnoreCase("P") ?
              ChessDomain.getPawnMoves(board, squareMostRecentlyFixatedOn) :
                classOfObjectMostRecentlyFixatedOn.equalsIgnoreCase("N") ?
                  ChessDomain.getKnightMoves(board, squareMostRecentlyFixatedOn) :
                  classOfObjectMostRecentlyFixatedOn.equalsIgnoreCase("K") ?
                    ChessDomain.getKingMoves(board, squareMostRecentlyFixatedOn) :
                    classOfObjectMostRecentlyFixatedOn.equalsIgnoreCase("Q") ?
                      ChessDomain.getQueenMoves(board, squareMostRecentlyFixatedOn) :
                      classOfObjectMostRecentlyFixatedOn.equalsIgnoreCase("R") ?
                        ChessDomain.getRookMoves(board, squareMostRecentlyFixatedOn) :
                        classOfObjectMostRecentlyFixatedOn.equalsIgnoreCase("B") ?
                          ChessDomain.getBishopMoves(board, squareMostRecentlyFixatedOn):
                          new Object[]{new ArrayList<>(), 0}
            ;

            //Set time decided upon, irrespective of whether a suitable Square was
            //found to make this fixation on.
            this.setTimeDecidedUpon(
              this.getTimeDecidedUpon() + model.getTimeToMoveVisualSpatialFieldObject() * (int)potentialFixationsAndSquaresConsidered[1]
            );

            //Set the Square to fixate on when this Fixation is made, if there are
            //candidates.
            List<Square> potentialFixations = (List<Square>)potentialFixationsAndSquaresConsidered[0];
            if(!potentialFixations.isEmpty()){
              this._squareToFixateOn = potentialFixations.get(new Random().nextInt(potentialFixations.size()));
            }
          }
        }
      }
    }
  }

  /**
   * 
   * @param scene
   * @param time
   * 
   * @return The {@link jchrest.lib.Square} decided upon when {@link 
   * #this#AttackDefenseFixation(jchrest.architecture.Chrest, 
   * jchrest.domainSpecifics.Scene, int) was invoked or {@code null} if any of 
   * the following conditions evaluate to {@link java.lang.Boolean#TRUE}:
   * <ul>
   *    <li>
   *      The result of {@link #this#getPerformanceTime()} is greater than 
   *      (later than) the {@code time} specified.
   *    </li>
   *    <li>
   *      {@link java.lang.Boolean#TRUE} is returned when {@link 
   *      jchrest.domainSpecifics.Scene#isBlind()} is invoked in context
   *      of the {@code scene} specified.
   *    </li>
   *    <li>
   *      {@link jchrest.domainSpecifics.Scene#equals(java.lang.Object)} returns
   *      {@link java.lang.Boolean#FALSE} when the {@link 
   *      jchrest.domainSpecifics.chess.ChessBoard} that was used to generate 
   *      the {@link jchrest.lib.Square} to fixate upon in {@link 
   *      #this#AttackDefenseFixation(jchrest.architecture.Chrest, 
   *      jchrest.domainSpecifics.Scene, int)} is compared with the {@code 
   *      scene} specified.
   *    </li>
   *    <li>
   *      No {@link jchrest.lib.Square} was suitable when {@link 
   *      #this#AttackDefenseFixation(jchrest.architecture.Chrest, 
   *      jchrest.domainSpecifics.Scene, int)} was called, i.e. the {@link 
   *      jchrest.lib.Square} to fixate on is equal to {@code null}.
   *    </li>
   *    <li>
   *      The {@link jchrest.domainSpecifics.SceneObject} on the {@link 
   *      jchrest.lib.Square} to fixate on indicates a blind {@link 
   *      jchrest.lib.Square} in context of the {@code scene} specified.
   *    </li>
   * </ul>
   */
  @Override
  public Square make(Scene scene, int time) {
    return 
      time < this.getPerformanceTime() ||
      scene.isBlind() ||
      !this._board.equals(scene) ||
      this._squareToFixateOn == null ||
      scene.getSquareContents(this._squareToFixateOn.getColumn(), this._squareToFixateOn.getRow()).getObjectType().equals(Scene.getBlindSquareToken()) ? 
        null : 
        this._squareToFixateOn
    ;
  }
}
