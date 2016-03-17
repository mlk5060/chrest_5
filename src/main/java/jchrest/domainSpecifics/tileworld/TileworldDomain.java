package jchrest.domainSpecifics.tileworld;

import jchrest.domainSpecifics.DomainSpecifics;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import jchrest.architecture.Chrest;
import jchrest.domainSpecifics.Fixation;
import jchrest.lib.ExecutionHistoryOperations;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.Modality;
import jchrest.lib.PrimitivePattern;
import jchrest.domainSpecifics.Scene;
import jchrest.lib.VisualSpatialFieldObject;

/**
 * Used for Tileworld modelling.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class TileworldDomain extends DomainSpecifics{
  
  //These variables should not be changed during run-time since problems with 
  //"Scene" instances will ensue.
  private static final String TILE_TOKEN = "T";
  private static final String HOLE_TOKEN = "H"; 
  private static final String OPPONENT_TOKEN = "O";
  private final int _verticalFieldOfView;
  private final int _horizontalFieldOfView;
  
  /**
   * 
   * @param model
   * @param verticalFieldOfView How many squares ahead can be seen.
   * @param horizontalFieldOfView How many squares to the side can be seen.
   * @param maxFixationsInSet Used as input to {@link 
   * jchrest.domainSpecifics.DomainSpecifics#DomainSpecifics(
   * jchrest.architecture.Chrest, java.lang.Integer)}.
   */
  public TileworldDomain(Chrest model, Integer verticalFieldOfView, Integer horizontalFieldOfView, Integer maxFixationsInSet) {
    super(model, maxFixationsInSet);
    this._verticalFieldOfView = verticalFieldOfView;
    this._horizontalFieldOfView = horizontalFieldOfView;
  }

  /** 
   * @param pattern
   * @return A {@link jchrest.lib.ListPattern} stripped of {@link 
   * jchrest.lib.ItemSquarePattern}s that:
   * 
   * <ol type="1">
   *  <li>
   *    Represent the CHREST model or the agent equipped with the CHREST model.
   *  </li>
   *  <li> 
   *    Blind, empty and unknown {@link jchrest.lib.ItemSquarePattern}s.
   *  </li>
   *  <li> 
   *    Are duplicated in the {@link jchrest.lib.ListPattern} passed.
   *  </li>
   * </ol>
   */
  @Override
  public ListPattern normalise(ListPattern pattern) {
    ListPattern result = new ListPattern(pattern.getModality());
    
    for(PrimitivePattern prim : pattern){
      ItemSquarePattern itemDetails = (ItemSquarePattern)prim;
      String item = itemDetails.getItem();
      if(
        !item.equals(Scene.getBlindSquareToken()) &&
        !item.equals(Scene.getEmptySquareToken()) &&
        !item.equalsIgnoreCase(Scene.getCreatorToken()) && 
        !item.equalsIgnoreCase(VisualSpatialFieldObject.getUnknownSquareToken()) &&
        !result.contains(prim)
      ){
        result.add(itemDetails);
      }
    }
    
    if(this._associatedModel != null){
      HashMap<String, Object> historyRowToInsert = new HashMap<>();
      
      //Generic operation name setter for current method.  Ensures for the row to 
      //be added that, if this method's name is changed, the entry for the 
      //"Operation" column in the execution history table will be updated without 
      //manual intervention and "Filter By Operation" queries run on the execution 
      //history DB table will still work.
      class Local{};
      historyRowToInsert.put(Chrest._executionHistoryTableOperationColumnName, 
        ExecutionHistoryOperations.getOperationString(this.getClass(), Local.class.getEnclosingMethod())
      );
      historyRowToInsert.put(Chrest._executionHistoryTableInputColumnName, pattern.toString() + "(" + pattern.getModalityString() + ")");
      historyRowToInsert.put(Chrest._executionHistoryTableOutputColumnName, result.toString() + "(" + result.getModalityString() + ")");
      this._associatedModel.addEpisodeToExecutionHistory(historyRowToInsert);
    }
    
    return result;
  }
  
//  @Override
//  public void makeOrScheduleFixation(Scene scene, int time){
//    Perceiver perceiver = this._associatedModel.getPerceiver();
//    Object[] nextScheduledFixation = this._associatedModel.getNextScheduledFixation();
//    
//    //Attempt to make a fixation if one is scheduled at the current time.
//    if((int)nextScheduledFixation[0] == time){
//      
//      switch((FixationType)nextScheduledFixation[1]){
//        
//        case first:
//          perceiver.makeFirstFixation(scene, time);
//          this._associatedModel.setNextScheduledFixation(new Object[]{
//            time + this._salientObjectSelectionTime,
//            FixationType.salientObject
//          });
//          
//        case salientObject:
//          if(
//            !perceiver.makeFixationUsingSalientObjectHeuristic(scene, time) ||
//            !perceiver.doingInitialFixations()
//          ){
//            this._nextScheduledFixation = new Object[]{
//              time + this._timeToRetrieveItemFromStm,
//              FixationType.hypothesisDiscrimination
//            };
//          }
//        
//        case hypothesisDiscrimination:
//          if(!perceiver.makeFixationUsingHypothesisDiscriminationHeuristic(scene, time)){
//            double r = Math.random ();
//            
//            if(r < 0.3333){
//              this._nextScheduledFixation = new Object[]{
//                time + this._movementSquareSelectionTime,
//                FixationType.objectMovement
//              };
//            }
//            else if (r >= 0.3333 && r < 0.6667) {
//              this._nextScheduledFixation = new Object[]{
//                time + this._randomSquareSelectionTime,
//                FixationType.peripheralObject
//              };
//            }
//            else if(this.isExperienced(time)){
//              this._nextScheduledFixation = new Object[]{
//                time + this.getDomainSpecifics().getTimeToUseGlobalStrategy(),
//                FixationType.globalStrategy
//              };
//            }
//            else{
//              this._nextScheduledFixation = new Object[]{
//                time + this._randomSquareSelectionTime,
//                FixationType.peripheralSquare
//              };
//            }
//          }
//        
//        case objectMovement:
//          perceiver.makeFixationUsingObjectMovementHeuristic(scene, time);
//          
//        case peripheralSquare:
//          perceiver.makeFixationUsingPeripheralLocationHeuristic(scene, time);
//          
//        case globalStrategy:
//          
//        case peripheralObject:
//          perceiver.makeFixationUsingPeripheralObjectHeuristic(scene, time);
//          
//        //TODO: learn fixation after it is made, could put the following in
//        //      Perceiver.addFixation():
//        //      ListPattern listPatternToRecognise = _model.getDomainSpecifics().normalise (
////          _model.getDomainSpecifics().convertSceneSpecificCoordinatesToDomainSpecificCoordinates(
////            _currentScene.getItemsInScopeAsListPattern (_fixationX, _fixationY, this.getFieldOfView(), true),
////            this._currentScene
////          )
////        );
//      }
//      
//    }
//    //No fixation is to be made so check if the perceiver resource is free.  If
//    //it is, schedule a new fixation.
//    else if(this.perceiverFree(time)){
//      
//      
//      //If no fixations have yet been made, this is the start of a new fixation
//      //set so make the initial fixation, this incurs no time cost.
//      if(perceiver.getFixations(time).isEmpty()){
//        
//        //If there has been a fixation previously scheduled and the required
//        //amount of time to move the eye back to the "default" position hasn't
//        //elapsed, schedule the initial fixation.  Otherwise, make it now.
//        if(
//          this._nextScheduledFixation[0] != null &&
//          (int)this._nextScheduledFixation[0] + this._saccadeTime < time 
//        ){
//         this._nextScheduledFixation = new Object[]{
//           (int)this._nextScheduledFixation[0] + this._saccadeTime,
//           FixationType.first
//         };
//        }
//        else{
//          perceiver.makeFirstFixation(scene, time);
//          this._nextScheduledFixation = new Object[]{
//            time + this._salientObjectSelectionTime, 
//            FixationType.salientObject
//          };
//        }
//      }
//    }
//  }
  
  /** 
   * Make the first fixation in a set (see {@link 
   * jchrest.lib.DomainSpecifics#getFirstFixation(jchrest.lib.Scene)} for the
   * result of invoking {@link jchrest.architecture.Chrest#getDomainSpecifics()}
   * on the {@link jchrest.architecture.Chrest} model associated with {@link 
   * #this}.
   * 
   * <b>NOTE:</b> Since this is a domain-specific heuristic, it is assumed that
   * {@link jchrest.lib.Square Squares} proposed for {@link jchrest.domainSpecifics.chess.fixationTypes.Fixation
   * Fixations} to make are not blind.
   * 
   * @param scene The scene to make the {@link jchrest.domainSpecifics.chess.fixationTypes.Fixation} in context
   * of.  If this contains only blind {@link jchrest.lib.SceneObject 
   * SceneObjects} then no fixation will be made.
   * 
   * @param time
   */
//  public void makeFirstFixation(Scene scene, int time) {
//    //_recognisedNodes.clear();
//    
//    if(!scene.isEntirelyBlind()){
//      Square firstFixation = _associatedChrestModel.getDomainSpecifics().getFirstFixation(scene);    
//      this.addFixation(
//        new Fixation(jchrest.domainSpecifics.chess.FixationType.first, firstFixation.getColumn(), firstFixation.getRow(), time),
//        scene,
//        time
//      );
//    }
//  }
  
  /**
   * @param scene
   * @return The {@link jchrest.lib.Square} immediately ahead of a player along 
   * their current heading.  Note that {@code null} may be returned if {@link 
   * #this#setMethodToDetermineSquareAheadOfPlayer(java.lang.reflect.Method, 
   * java.lang.Object, java.lang.Object...)} has not been used to set the 
   * parameters required to determine what the {@link jchrest.lib.Square} 
   * immediately ahead of a player along their current heading is.
   */
//  @Override
//  public Square getFirstFixation(Scene scene){
//    Square initialFixation = null;
//      
//    try {
//      initialFixation = (Square)this._getSquareAheadOfPlayerMethod.invoke(this._player, this._getSquareAheadOfPlayerMethodArguments);
//    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
//      Logger.getLogger(TileworldDomain.class.getName()).log(Level.SEVERE, null, ex);
//    }
//    
//    return initialFixation;
//  }

  /**
   * @param scene
   * @param model
   * @return A {@link jchrest.lib.Square} in the {@link jchrest.lib.Scene} 
   * passed that isn't blind, empty or occupied by the creator of the {@link 
   * jchrest.lib.Scene}.
   */
//  @Override
//  public Set<Square> getSalientObjectFixations(Scene scene, Chrest model, int time) {
//    Set<Square> result = new HashSet<> ();
//    
//    int randomCol = new java.util.Random().nextInt(scene.getWidth ());
//    int randomRow = new java.util.Random().nextInt(scene.getHeight ());
//
//    String objectOnSquare = scene.getSquareContents(randomCol, randomRow).getObjectClass();
//    while( 
//      objectOnSquare.equals(Scene.getBlindSquareToken()) ||
//      objectOnSquare.equals(Scene.getCreatorToken()) ||
//      objectOnSquare.equals(Scene.getEmptySquareToken())
//    ){
//      randomCol = new java.util.Random().nextInt(scene.getWidth ());
//      randomRow = new java.util.Random().nextInt(scene.getHeight ());
//      objectOnSquare = scene.getSquareContents(randomCol, randomRow).getObjectClass();
//    }
//
//    result.add (new Square(randomCol, randomRow));
//    return result;
//  }

  /**
   * If the {@link jchrest.lib.Square} passed on the {@link jchrest.domainSpecifics.Scene} 
   * passed contains a tile, opponent or the scene creator, then the 
   * {@link jchrest.lib.Square}s that are 1 square north, east, south and west 
   * of the passed {@link jchrest.lib.Square} will be added to the 
   * {@link java.util.List} of {@link jchrest.lib.Square}s returned.
   * 
   * @param scene
   * @param square
   * @return 
   */
//  @Override
//  public List<Square> proposeMovementFixations(Scene scene, Square square) {
//    ArrayList<Square> movementFixations = new ArrayList<>();
//    int col = square.getColumn();
//    int row = square.getRow();
//    
//    SceneObject objectOnSquare = scene.getSquareContents(col, row);
//    
//    if(objectOnSquare != null){
//      String objectOnSquareClass = objectOnSquare.getObjectClass();
//      if(
//        objectOnSquareClass.equals(TILE_TOKEN) ||
//        objectOnSquareClass.equals(Scene.getCreatorToken()) ||
//        objectOnSquareClass.equals(OPPONENT_TOKEN)
//      ){
//        if ((row + 1 >= 0) && (row + 1 < scene.getHeight())) movementFixations.add(new Square(col, row + 1)); //North
//        if ((col + 1 >= 0) && (col + 1 < scene.getWidth())) movementFixations.add(new Square(col + 1, row));//East
//        if ((row - 1 >= 0) && (row - 1 < scene.getHeight())) movementFixations.add(new Square(col, row - 1));//South
//        if ((col - 1 >= 0) && (col - 1 < scene.getWidth())) movementFixations.add(new Square(col - 1, row));//West
//      }
//    }
//    
//    return movementFixations;
//  }
  
  public static String getHoleIdentifier(){
    return HOLE_TOKEN;
  }
  
  public static String getOpponentIdentifier(){
    return OPPONENT_TOKEN;
  }
  
  public static String getTileIdentifier(){
    return TILE_TOKEN;
  }

  @Override
  public Fixation getInitialFixationInSet(int time) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Fixation getNonInitialFixationInSet(int time) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean shouldLearnFromNewFixations(int time) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean isFixationSetComplete(int time) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean shouldAddNewFixation(int time) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
