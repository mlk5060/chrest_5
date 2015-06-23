package jchrest.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jchrest.architecture.Chrest;

/**
 * Used for Tileworld modelling.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class TileworldDomain extends DomainSpecifics{
  
  private final String _tileIdentifier = "T";
  private final String _holeIdentifier = "H"; 
  private final String _opponentIdentifier = "O";
  
  public TileworldDomain(Chrest model) {
    super(model);
  }

  @Override
  public ListPattern normalise(ListPattern pattern) {
    ListPattern result = new ListPattern(pattern.getModality());
    
    //Remove self from pattern since the location of self doesn't need to be
    //learned and remove duplicates that may have been added due to random 
    //fixations.
    for(PrimitivePattern prim : pattern){
      ItemSquarePattern item = (ItemSquarePattern)prim;
      if(!item.getItem().equalsIgnoreCase(Scene.getSelfIdentifier()) && !result.contains(prim)){
        result.add(item);
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

  /**
   * In Tileworld, salient squares are those that aren't occupied solely by the 
   * scene creator, aren't blind spots and aren't empty.
   * 
   * @param scene
   * @param model
   * @return 
   */
  @Override
  public Set<Square> proposeSalientSquareFixations(Scene scene, Chrest model) {
    Set<Square> salientSquareFixations = new HashSet<>();
    for(int col = 0; col < scene.getWidth(); col++){
      for(int row = 0; row < scene.getHeight(); row++){
        
        ListPattern squareContents = scene.getItemsOnSquare(col, row, false, false);
        boolean onlyCreatorOnSquare = false;
        
        if(squareContents.size() > 1){
          for(PrimitivePattern object  : squareContents){
            if(object instanceof ItemSquarePattern){
              ItemSquarePattern obj = (ItemSquarePattern)object;
              if(obj.getItem().equals(Scene.getSelfIdentifier())){
                onlyCreatorOnSquare = true;
              }
            }
          }
        }
        
        if( !squareContents.isEmpty() && !onlyCreatorOnSquare ){
          salientSquareFixations.add(new Square(col, row));
        }
      }
    }
    return salientSquareFixations;
  }

  @Override
  public List<Square> proposeMovementFixations(Scene scene, Square square) {
    ArrayList<Square> movementFixations = new ArrayList<>();
    int col = square.getColumn();
    int row = square.getRow();
    
    ListPattern squareContents = scene.getItemsOnSquare(col, row, false, false);
    if( !squareContents.isEmpty() ){
      for(PrimitivePattern squareContent : squareContents){
        String item = ((ItemSquarePattern)squareContent).getItem();
        
        if(item.equals(this._tileIdentifier)){
          movementFixations.addAll(this.findTileMoves(scene, square));
        } 
        else if(
          item.equals(Scene.getSelfIdentifier()) ||
          item.equals(this._opponentIdentifier)
        ){
          movementFixations.addAll(this.findAgentMoves(scene, square));
        }
      }
    }
    
    return movementFixations;
  }
  
  /**
   * Determines if an agent can move to the squares immediately north, 
   * east, south and west of its current location.
   * 
   * @param scene
   * @param agentLocation
   * @return 
   */
  private List<Square> findAgentMoves(Scene scene, Square agentLocation){
    ArrayList<Square> squaresAgentCanMoveTo = new ArrayList<>();
    
    for(int direction = 0; direction < 4; direction++){
      
      //Default square to check is north.
      Square squareToCheck = new Square(agentLocation.getColumn(), agentLocation.getRow() + 1);
      
      if(direction == 1){ 
        squareToCheck = new Square(agentLocation.getColumn() + 1, agentLocation.getRow()); //East
      } else if(direction == 2){ 
        squareToCheck = new Square(agentLocation.getColumn(), agentLocation.getRow() - 1); //South
      } else if(direction == 3){
        squareToCheck = new Square(agentLocation.getColumn() - 1, agentLocation.getRow()); //West
      }
      
      if(this.canAgentMoveToSquare(squareToCheck, scene)){
        squaresAgentCanMoveTo.add(squareToCheck);
      }
    }
    
    return squaresAgentCanMoveTo;
  }
  
  /**
   * Determines if an agent can move to a given square.  An agent can only move
   * to a square if it is empty or has no immovable objects upon it, i.e. a 
   * tile that isn't blocked, a hole or an agent.
   * 
   * @param to
   * @param scene
   * @return 
   */
  private boolean canAgentMoveToSquare(Square to, Scene scene){
    
    ListPattern objectsOnSquareToMoveTo = scene.getItemsOnSquare(to.getColumn(), to.getRow(), false, false);
    
    for(PrimitivePattern objectOnSquareToMoveTo : objectsOnSquareToMoveTo){
      ItemSquarePattern ios = (ItemSquarePattern)objectOnSquareToMoveTo;
      String itemIdentifier = ios.getItem();
      
      if( 
        (itemIdentifier.equals(this._tileIdentifier) && this.findTileMoves(scene, to).isEmpty()) ||
        itemIdentifier.equals(this._holeIdentifier) ||
        itemIdentifier.equals(this._opponentIdentifier) ||
        itemIdentifier.equals(Scene.getSelfIdentifier())
      ){
        return false;
      }
    }
    
    return true;
  }
  
  /**
   * Determines if a tile can move to the squares immediately north, 
   * east, south and west of its current location.
   * 
   * @param scene
   * @param square The current location of the tile to find moves for.
   * @return 
   */
  private List<Square> findTileMoves(Scene scene, Square tileLocation){
    
    ArrayList<Square> squaresTileCanMoveTo = new ArrayList<>();
    int tileLocationCol = tileLocation.getColumn();
    int tileLocationRow = tileLocation.getRow();
    
    for(int direction = 0; direction < 4; direction++){
      
      //Default square to check is north.
      Square squareToCheck = new Square(tileLocationCol, tileLocationRow + 1);
      
      if(direction == 1){ 
        squareToCheck =  new Square(tileLocationCol + 1, tileLocationRow); //East
      } else if(direction == 2){ 
        squareToCheck =  new Square(tileLocationCol, tileLocationRow - 1); //South
      } else if(direction == 3){
        squareToCheck =  new Square(tileLocationCol - 1, tileLocationRow); //West
      }
      
      if(this.canTileMoveToSquare(tileLocation, squareToCheck, scene)){
        squaresTileCanMoveTo.add(squareToCheck);
      }
    }
    
    return squaresTileCanMoveTo;
  }
  
  /**
   * Determines if a tile can be moved from its current location to a new 
   * location specified in a given {@link jchrest.lib.Scene}.  A tile can only 
   * be moved if all of the following conditions are true:
   * 
   * <ol>
   *  <li>
   *    It is not already on the same square as a hole.
   *  </li>
   *  <li>
   *    There isn't a blocking object (anything except a hole) on the new 
   *    location specified.
   *    
   *  </li>
   *  <li>
   *    There is a "mover" agent, i.e. the creator of the 
   *    {@link jchrest.lib.Scene} being considered or an opponent on a square 
   *    behind the tile's current location along the heading required for the
   *    tile to move to its new location.
   *  </li>
   * </ol>
   * 
   * @param tileLocation
   * @param scene
   * @return 
   */
  private boolean canTileMoveToSquare(Square currentTileLocation, Square newTileLocation, Scene scene){
    
    int currentTileLocationCol = currentTileLocation.getColumn();
    int currentTileLocationRow = currentTileLocation.getRow();
    
    /////////////////////////////////////////
    ///// Check for hole on same square /////
    /////////////////////////////////////////

    ListPattern itemsOnCurrentLocation = scene.getItemsOnSquare(currentTileLocationCol, currentTileLocationRow, false, false);
    for(PrimitivePattern itemOnCurrentLocation : itemsOnCurrentLocation){
      if(itemOnCurrentLocation instanceof ItemSquarePattern){
        ItemSquarePattern ios = (ItemSquarePattern)itemOnCurrentLocation;
        if(ios.getItem().equals(this._holeIdentifier)){
          return false;
        }
      }
    }
    
    //////////////////////////////////////////////////////
    ///// Check for blocking objects on new location /////
    //////////////////////////////////////////////////////
    
    int newTileLocationCol = newTileLocation.getColumn();
    int newTileLocationRow = newTileLocation.getRow();
    ListPattern objectsOnNewLocation = scene.getItemsOnSquare(newTileLocationCol, newTileLocationRow, false, false);
    
    if(!objectsOnNewLocation.isEmpty()){
      for(PrimitivePattern objectOnNewLocation : objectsOnNewLocation){
        if(objectOnNewLocation instanceof ItemSquarePattern){
          ItemSquarePattern ios = (ItemSquarePattern)objectOnNewLocation;
          String itemIdentifier = ios.getItem();
          if( !itemIdentifier.equals(this._holeIdentifier) ){
            return false;
          }
        }
      }
    }
    
    ////////////////////////////////////////////////////
    ///// Check for a correctly positioned "mover" /////
    ////////////////////////////////////////////////////
    
    ListPattern objectsOnSquareBehind = new ListPattern(Modality.VISUAL);
    
    //New location is north of current location.
    if(
      newTileLocationCol == currentTileLocationCol &&
      newTileLocationRow == currentTileLocationRow + 1
    ){
      objectsOnSquareBehind = scene.getItemsOnSquare(currentTileLocationCol, currentTileLocationRow - 1, false, false);
    }
    
    //New location is east of current location.
    if(
      newTileLocationCol == currentTileLocationCol + 1 &&
      newTileLocationRow == currentTileLocationRow
    ){
      objectsOnSquareBehind = scene.getItemsOnSquare(currentTileLocationCol - 1, currentTileLocationRow, false, false);
    }
    
    //New location is south of current location.
    if(
      newTileLocationCol == currentTileLocationCol &&
      newTileLocationRow == currentTileLocationRow - 1
    ){
      objectsOnSquareBehind = scene.getItemsOnSquare(currentTileLocationCol, currentTileLocationRow + 1, false, false);
    }
    
    //New location is west of current location.
    if(
      newTileLocationCol == currentTileLocationCol -1 &&
      newTileLocationRow == currentTileLocationRow
    ){
      objectsOnSquareBehind = scene.getItemsOnSquare(currentTileLocationCol + 1, currentTileLocationRow, false, false);
    }
    
    //Is there a "mover" in the correct location?
    if(!objectsOnSquareBehind.isEmpty()){
      for(PrimitivePattern objectOnSquareBehind : objectsOnSquareBehind){
        if(objectOnSquareBehind instanceof ItemSquarePattern){
          ItemSquarePattern ios = (ItemSquarePattern)objectOnSquareBehind;
          String itemIdentifier = ios.getItem();
          if(itemIdentifier.equals(this._opponentIdentifier) || itemIdentifier.equals(Scene.getSelfIdentifier())){
            return true;
          }
        }
      }
    }

    return false;
  }

  @Override
  public int getCurrentTime() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
