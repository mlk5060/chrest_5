// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import java.util.ArrayList;
import jchrest.architecture.VisualSpatialField;

/**
 * Represents a 2D external environment that a CHREST model can "see" as a 2D
 * {@link java.util.ArrayList}.  Currently, only one object can be encoded per
 * coordinate in the {@link jchrest.lib.Scene}.  The {@link java.util.ArrayList}
 * created has the following structure when created:
 * 
 * <ul>
 *  <li>
 *    First-dimension elements represent columns (x-axis) in the external 
 *    environment.
 *  </li>
 *  <li>
 *    Second-dimension elements represent rows (y-axis) in the external 
 *    environment and contain, at most, one {@link jchrest.lib.SceneObject}.
 *  </li>
 * </ul>
 * 
 * Constructing the data structure in this way means that coordinate 
 * specification must follow the form of x-coordinate <b>then</b> y-coordinate.
 * Thus, coordinate specification in a {@link jchrest.lib.Scene} instance is
 * congruent with the "along the corridor, up the stairs" approach to 2D grid
 * reading.
 * 
 * Rows and columns are zero-indexed and therefore, identifying coordinates in
 * a {@link jchrest.lib.Scene} should not use coordinates specific to the
 * external environment (unless coordinates for the external environment are
 * also zero-indexed).
 * 
 * @author Peter C. R. Lane <p.c.lane@herts.ac.uk>
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class Scene {
  
  //Human-readable identifier for the scene.
  private final String _name;
  
  //The maximimum height and width of the scene.
  private final int _height;
  private final int _width;
  
  private final VisualSpatialField _visualSpatialFieldGeneratedFrom;
  
  //The string used to identify "blind-spots" i.e. squares that can't be seen in
  //a Scene instance.
  private static final String BLIND_SQUARE_IDENTIFIER = "null";
  
  //The string used to identify empty squares
  private static final String EMPTY_SQUARE_IDENTIFIER = ".";
  
  //The string used to identify the creator of the Scene instance.
  private static final String CREATOR_TOKEN = "SELF";
  
  //The actual scene.
  private final ArrayList<ArrayList<SceneObject>> _scene;

  /**
   * Constructor: the instance created is initially "blind", empty squares and
   * items must be added using the appropriate methods from this class.
   * 
   * @param name Human-readable identifier for the Scene instance created.
   * @param width Represents the maximum number of indivisible x-coordinates 
   * that can be "seen" in an external vision.
   * @param height Represents the maximum number of indivisible y-coordinates 
   * that can be "seen" in an external vision.
   * @param visualSpatialFieldGeneratedFrom Set to null if this {@link #this} is
   * not being generated from a {@link jchrest.architecture.VisualSpatialField}
   */
  public Scene (String name, int width, int height, VisualSpatialField visualSpatialFieldGeneratedFrom) {
    this._name = name;
    this._height = height;
    this._width = width;
    this._visualSpatialFieldGeneratedFrom = visualSpatialFieldGeneratedFrom;
    
    //Instantiate Scene with ScenObjects representing blind squares at first 
    //(empty squares must be encoded explicitly).  Note that identifiers for 
    //blind SceneObjects are not unique since blind squares can not be moved in 
    //a visual-spatial field if they are converted to VisualSpatialFieldObject 
    //instances (the purpose of a SceneObject identifier is to allow 
    //VisualSpatialFieldObject instances to be precisely identified so they may 
    //be moved).
    this._scene = new ArrayList<>();
    for(int col = 0; col < width; col++){
      this._scene.add(new ArrayList<>());
      for(int row = 0; row < height; row++){
        this._scene.get(col).add(new SceneObject("", Scene.getBlindSquareIdentifier()));
      }
    }
  }
  
  /**
   * Creates a new {@link jchrest.lib.SceneObject} representing the item 
   * specified and inserts this {@link jchrest.lib.SceneObject} into the 
   * specified coordinate in this {@link #this}.  If an item already exists on 
   * the coordinate specified, the new item overwrites the old one.
   * 
   * @param row
   * @param col
   * @param itemIdentifier
   * @param objectClass
   */
  public void addItemToSquare (int col, int row, String itemIdentifier, String objectClass) {
    assert (row >= 0 && row < _height && col >= 0 && col < _width);
    _scene.get(col).set(row, new SceneObject(itemIdentifier, objectClass));
  }
  
  /**
   * Adds the items specified along the x-axis of the y-axis (row) specified 
   * from the minimum x-axis coordinate incrementally.  If the number of items 
   * specified is greater than the maximum x-axis value of the {@link #this},
   * the extra items are ignored.
   * 
   * If a coordinate already contains an item, the new item overwrites the old
   * item.
   * 
   * @param row The row to be modified.
   * @param items The items to added to the row as 
   * {@link jchrest.lib.SceneObject} instances.
   */
  public void addItemsToRow (int row, ArrayList<SceneObject> items) {
    for (int i = 0; i < this._width; i++) {
      SceneObject item = items.get(i);
      if(!item.getObjectClass().equals(Scene.BLIND_SQUARE_IDENTIFIER)){
        this.addItemToSquare(i, row, item.getIdentifier(), item.getObjectClass());
      }
    }
  }
  
  /**
   * Compute the errors of commission in this scene compared to another.
   * 
   * @param sceneToCompareAgainst
   * 
   * @return 
   * <ul>
   *  <li>
   *    The number of items that are in this scene but not another.  For 
   *    example, if this {@link #this} were to have 4 items and another 
   *    {@link jchrest.lib.Scene} were to have 3, the output of this function 
   *    would be 1 (blind and empty "items" are not included in the calculation).  
   *  </li>
   *  <li>
   *    If the number of squares in the scenes to be used in the calculation are 
   *    not equal, an error is thrown since a fair calculation can not be made 
   *    in these circumstances.  
   *  </li>
   *  <li>
   *    If the number of items in a scene is less than the number of items in 
   *    the scene compared against, 0 is returned.
   *  </li>
   * </ul>
   */
  public int computeErrorsOfCommission (Scene sceneToCompareAgainst) {
    if(this.getHeight() != sceneToCompareAgainst.getHeight() || this.getWidth() != sceneToCompareAgainst.getWidth()){
      throw new IllegalArgumentException("Dimensions of scenes to compare are not equal: "
        + "height and width of scene whose recall is to be calculated = " + this.getHeight() + ", " + this.getWidth()
        + "height and width of scene compared against = " + sceneToCompareAgainst.getHeight() + ", " + sceneToCompareAgainst.getWidth() 
        + "."
      );
    }
    
    int numberItemsInThisScene = this.getAsListPattern(false, true).removeBlindAndEmptyItems().size();
    int numberItemsInOtherScene = sceneToCompareAgainst.getAsListPattern(false, true).removeBlindAndEmptyItems().size();
    
    if(numberItemsInThisScene <= numberItemsInOtherScene){
      return 0;
    }

    return numberItemsInThisScene - numberItemsInOtherScene;
  }
  
  /**
   * Compute the errors of ommission in this scene compared to another.
   * 
   * @param sceneToCompareAgainst
   * 
   * @return 
   * <ul>
   *  <li>
   *    The number of non-blind and non-empty squares that aren't in this 
   *    scene but are in the other.  For example, if this {@link #this} were to 
   *    have 3 non blind/empty squares the other {@link jchrest.lib.Scene} were 
   *    to have 4, the output of this function would be 1.  
   *  </li>
   *  <li>
   *    If the number of squares in the scenes to be used in the calculation are 
   *    not equal, an error is thrown since a fair calculation can not be made 
   *    in these circumstances.  
   *  </li>
   *  <li>
   *    If the number of non blind/empty squares in the base scene is greater 
   *    than the number of non blind/empty squares in the scene compared 
   *    against, 0 is returned.
   *  </li>
   * </ul>
   */
  public int computeErrorsOfOmission (Scene sceneToCompareAgainst) {
    if(this.getHeight() != sceneToCompareAgainst.getHeight() || this.getWidth() != sceneToCompareAgainst.getWidth()){
      throw new IllegalArgumentException("Dimensions of scenes to compare are not equal: "
        + "height and width of scene whose recall is to be calculated = " + this.getHeight() + ", " + this.getWidth()
        + "height and width of scene compared against = " + sceneToCompareAgainst.getHeight() + ", " + sceneToCompareAgainst.getWidth() 
        + "."
      );
    }
    
    int numberItemsInThisScene = this.getAsListPattern(false, true).removeBlindAndEmptyItems().size();
    int numberItemsInOtherScene = sceneToCompareAgainst.getAsListPattern(false, true).removeBlindAndEmptyItems().size();

    if(numberItemsInThisScene >= numberItemsInOtherScene){
      return 0;
    }

    return numberItemsInOtherScene - numberItemsInThisScene;
  }
  
  /**
   * Compute precision of this scene against a given scene i.e. the percentage 
   * of non blind/empty squares in the scene, p (0 &lt;= p &lt;= 1), that are
   * correct in their placement when compared to another scene.
   * 
   * @param sceneToCompareAgainst
   * @param itemsIdentifiedByObjectClass Set to true to specify that the items
   * in this {@link jchrest.lib.Scene} and the {@link jchrest.lib.Scene} 
   * compared against should be identified and compared according to their 
   * object classes.  Set to false to specify that the items should be 
   * identified and compared by their unique identifiers.
   * 
   * @return 
   */
  public float computePrecision (Scene sceneToCompareAgainst, boolean itemsIdentifiedByObjectClass) {
    if(this.getHeight() != sceneToCompareAgainst.getHeight() || this.getWidth() != sceneToCompareAgainst.getWidth()){
      throw new IllegalArgumentException("Dimensions of scenes to compare are not equal: "
        + "height and width of scene whose recall is to be calculated = " + this.getHeight() + ", " + this.getWidth()
        + "height and width of scene compared against = " + sceneToCompareAgainst.getHeight() + ", " + sceneToCompareAgainst.getWidth() 
        + "."
      );
    }
      
    ListPattern itemsInThisScene = this.getAsListPattern(false, itemsIdentifiedByObjectClass).removeBlindAndEmptyItems();
    ListPattern itemsInOtherScene = sceneToCompareAgainst.getAsListPattern(false, itemsIdentifiedByObjectClass).removeBlindAndEmptyItems();

    //Check for potential to divide by 0.
    if( itemsInThisScene.isEmpty() || itemsInOtherScene.isEmpty() ){
      return 0.0f;
    }

    int numberOfCorrectlyPlacedItems = 0;

    for(int i = 0; i < itemsInThisScene.size(); i++){
      ItemSquarePattern itemInThisScene = (ItemSquarePattern)itemsInThisScene.getItem(i);

      if(itemsInOtherScene.contains(itemInThisScene)){
        numberOfCorrectlyPlacedItems++;
      }
    }

    return (float)numberOfCorrectlyPlacedItems / (float)itemsInOtherScene.size();
  }
  
  /**
   * Compute recall of given scene against this one, i.e. the proportion of 
   * items in this scene which have been correctly recalled, irrespective of 
   * their placement.
   * 
   * @param sceneToCompareAgainst
   * @param itemsIdentifiedByObjectClass
   * 
   * @return 
   */
  public float computeRecall (Scene sceneToCompareAgainst, boolean itemsIdentifiedByObjectClass) {
    if(this.getHeight() != sceneToCompareAgainst.getHeight() || this.getWidth() != sceneToCompareAgainst.getWidth()){
      throw new IllegalArgumentException("Dimensions of scenes to compare are not equal: "
        + "height and width of scene whose recall is to be calculated = " + this.getHeight() + ", " + this.getWidth()
        + "height and width of scene compared against = " + sceneToCompareAgainst.getHeight() + ", " + sceneToCompareAgainst.getWidth() 
        + "."
      );
    }
    
    ListPattern itemsInThisScene = this.getAsListPattern(false, itemsIdentifiedByObjectClass).removeBlindAndEmptyItems();
    ListPattern itemsInOtherScene = sceneToCompareAgainst.getAsListPattern(false, itemsIdentifiedByObjectClass).removeBlindAndEmptyItems();

    if(itemsInThisScene.isEmpty() || itemsInOtherScene.isEmpty()){
      return 0.0f;
    }

    ArrayList<String> itemIdentifiersInThisScene = new ArrayList();
    ArrayList<String> itemIdentifiersInOtherScene = new ArrayList();

    for(PrimitivePattern itemInThisScene: itemsInThisScene){
      itemIdentifiersInThisScene.add( ((ItemSquarePattern)itemInThisScene).getItem() );
    }

    for(PrimitivePattern itemInOtherScene: itemsInOtherScene){
      itemIdentifiersInOtherScene.add( ((ItemSquarePattern)itemInOtherScene).getItem() );
    }

    ArrayList<String> recalledItems = new ArrayList();
    float numberItemsInOtherScene = itemIdentifiersInOtherScene.size();

    for(String itemIdentifierInThisScene : itemIdentifiersInThisScene){
      if(itemIdentifiersInOtherScene.contains(itemIdentifierInThisScene)){
        recalledItems.add(itemIdentifierInThisScene);
        itemIdentifiersInOtherScene.remove(itemIdentifierInThisScene);
      }
    }
    
    return (float)recalledItems.size() / numberItemsInOtherScene;
  }
  
  /**
   * Returns the string used to denote blind squares in the scene.
   * 
   * @return 
   */
  public static String getBlindSquareIdentifier(){
    return Scene.BLIND_SQUARE_IDENTIFIER;
  }
  
  /**
   * Returns the string used to denote empty squares in the scene.
   * 
   * @return 
   */
  public static String getEmptySquareIdentifier(){
    return Scene.EMPTY_SQUARE_IDENTIFIER;
  }
  
  /**
   * Returns the scene (including blind and empty squares) from east -> west 
   * then south -> north as a {@link jchrest.lib.ListPattern} instance composed
   * of {@link jchrest.lib.ItemSquarePattern} instances representing the items
   * in the scene.
   * 
   * @param creatorRelativeCoordinates Set to false to force the function to 
   * return Scene specific coordinates for squares comprising the Scene even 
   * when the Scene's creator is identified in the Scene.  Set to true to have 
   * coordinates for squares comprising the Scene relative to the Scene 
   * creator's location.
   * 
   * @param identifyItemsByObjectClass Set to true to have the identifiers for 
   * the {@link jchrest.lib.ItemSquarePattern} instances make up the {@link 
   * jchrest.lib.ListPattern} returned set to the result of calling
   * {@link jchrest.lib.SceneObject#getObjectClass()} on each 
   * {@link jchrest.lib.SceneObject} in this {@link jchrest.lib.Scene}.  Set to
   * false to have the identifiers set to the result of calling 
   * {@link jchrest.lib.SceneObject#getIdentifier()} on each 
   * {@link jchrest.lib.SceneObject} in this {@link jchrest.lib.Scene} instead.
   * 
   * @return
   */
  public ListPattern getAsListPattern(boolean creatorRelativeCoordinates, boolean identifyItemsByObjectClass){
    ListPattern scene = new ListPattern();
    
    for(int row = 0; row < _height; row++){
      for(int col = 0; col < _width; col++){
        scene = scene.append(this.getSquareContentsAsListPattern(col, row, creatorRelativeCoordinates, identifyItemsByObjectClass));
      }
    }
    
    return scene;
  }
  
  /**
   * Returns the maximum height of the scene.
   * 
   * @return 
   */
  public int getHeight () {
    return _height;
  }
  
  /**
   * Retrieve all items within given row and column +/- model's current field of 
   * view (see {@link jchrest.architecture.Perceiver#getFieldOfView() and @link
   * jchrest.architecture.Perceiver#setFieldOfView()}). Blind and empty squares
   * are not returned).
   * 
   * @param startCol
   * @param startRow
   * @param fieldOfView
   * @param creatorRelativeCoordinates Set to true to return square coordinates 
   * relative to the creator of the scene if the creator is identified in the 
   * scene itself.
   * 
   * @param identifyItemsByObjectClass Set to true to have the identifiers for 
   * the {@link jchrest.lib.ItemSquarePattern} instances make up the {@link 
   * jchrest.lib.ListPattern} returned set to the result of calling
   * {@link jchrest.lib.SceneObject#getObjectClass()} on each 
   * {@link jchrest.lib.SceneObject} in this {@link jchrest.lib.Scene}.  Set to
   * false to have the identifiers set to the result of calling 
   * {@link jchrest.lib.SceneObject#getIdentifier()} on each 
   * {@link jchrest.lib.SceneObject} in this {@link jchrest.lib.Scene} instead.
   * 
   * @return 
   */
  public ListPattern getItemsInScopeAsListPattern (int startCol, int startRow, int fieldOfView, boolean creatorRelativeCoordinates, boolean identifyItemsByObjectClass) {
    ListPattern itemsInScope = new ListPattern ();
    
    for (int row = startRow - fieldOfView; row <= startRow + fieldOfView; row++) {
      if (row >= 0 && row < _height) {
        for (int col = startCol - fieldOfView; col <= startCol + fieldOfView; col++) {
          if (col >= 0 && col < _width) {
            itemsInScope = itemsInScope.append(this.getSquareContentsAsListPattern(col, row, creatorRelativeCoordinates, identifyItemsByObjectClass));
          }
        }
      }
    }
    return itemsInScope;
  }
   
  /**
   * Returns the contents of the square identified in this 
   * {@link jchrest.lib.Scene}.
   * 
   * @param col
   * @param row
   * @return 
   */
  public SceneObject getSquareContents(int col, int row){
    return this._scene.get(col).get(row);
  }
  
  /**
   * Returns all items on a square in this {@link jchrest.lib.Scene} as 
   * {@link jchrest.lib.ItemSquarePattern}s with coordinates relative to this 
   * {@link jchrest.lib.Scene}'s creator (if the creator is identified in this 
   * {@link jchrest.lib.Scene}) contained within a {@link 
   * jchrest.lib.ListPattern}.
   * 
   * @param col
   * @param row
   * @param creatorRelativeCoordinates Set to false to force the column and row
   * coordinates specified in the {@link jchrest.lib.ItemSquarePattern}s 
   * returned to not be relative to the creator of this 
   * {@link jchrest.lib.Scene} if they are present in it.
   * @param identifyItemsByObjectClass
   * 
   * @return 
   */
  public ListPattern getSquareContentsAsListPattern (int col, int row, boolean creatorRelativeCoordinates, boolean identifyItemsByObjectClass) {
    ListPattern squareContentsAsListPattern = new ListPattern();
    
    if (row >= 0 && row < _height && col >= 0 && col < _width) {
      Square locationOfSelf = this.getLocationOfCreator();
      SceneObject squareContents = this.getSquareContents(col, row);
      int itemSquarePatternCol = col;
      int itemSquarePatternRow = row;
        
      //Get self relative coordinates only if the self is present and self
      //relative coordinates have been requested.
      //
      // |---------------|----------------------------|----------------------|
      // | Self present? | Relative coords requested? | Result               |
      // |---------------|----------------------------|----------------------|
      // | Yes           | Yes                        | Self-relative coords |
      // | Yes           | No                         | Scene-relative coords|
      // | No            | Yes                        | Scene-relative coords|
      // | No            | No                         | Scene-relative coords|
      // |---------------|----------------------------|----------------------|
      if(locationOfSelf != null && creatorRelativeCoordinates){
        itemSquarePatternCol = col - locationOfSelf.getColumn();
        itemSquarePatternRow = row - locationOfSelf.getRow();
      }   
      
      //By default, assume that the item identifier for the ItemSquarePattern
      //representation of the object on square will have the object's class
      //as the item identifier.
      String itemIdentifier = squareContents.getObjectClass();

      
      if(!identifyItemsByObjectClass){
        itemIdentifier = squareContents.getIdentifier();
      }

      squareContentsAsListPattern.add(new ItemSquarePattern(
        itemIdentifier,
        itemSquarePatternCol,
        itemSquarePatternRow
      ));
    }
    
    return squareContentsAsListPattern;
  }
  
  /**
   * Returns a {@link jchrest.lib.Square} specifying the location of the entity 
   * that constructed this {@link jchrest.lib.Scene} (if it identified itself) 
   * in this {@link jchrest.lib.Scene}.
   * 
   * @return
   */
  public Square getLocationOfCreator(){
    for(int row = 0; row < this._height; row++){
      for(int col = 0; col < this._width; col++){
        String squareContents = this._scene.get(col).get(row).getObjectClass();
        if(squareContents.equals(Scene.CREATOR_TOKEN)){
          return new Square(col, row);
        }
      }
    }
    
    return null;
  }

  /**
   * Returns the name of the scene.
   * 
   * @return 
   */
  public String getName () {
    return _name;
  }
  
   /**
   * Returns the string used to denote the creator of a scene in the scene.
   * @return 
   */
  public static String getCreatorToken(){
    return Scene.CREATOR_TOKEN;
  }

  /**
   * Returns the maximum width of the scene.
   * 
   * @return 
   */
  public int getWidth () {
    return _width;
  }
  
  /**
   * Determines whether the coordinates specified are a blind spot in the scene.
   * 
   * @param col
   * @param row
   * @return True if the coordinate is a blind spot, false if not.
   */
  public boolean isSquareBlind(int col, int row){
    if (
      row >= 0 && 
      row < _height && 
      col >= 0 && 
      col < _width 
    ) {
      return _scene.get(col).get(row).getObjectClass().equals(Scene.BLIND_SQUARE_IDENTIFIER);
    } else {
      return true;
    }
  }

  /**
   * Determines whether the coordinate specified in the scene contains an item
   * or not.  
   * 
   * @param row
   * @param col
   * @return False if there is an item on the coordinate specified in this
   * scene, true if not or the item is a blind-spot.  If the row or col 
   * specified is less than 0 or greater than/equal to the max
   * height/width of this scene then the coordinate specified is considered to
   * be a blind spot.  Consequently, true is returned.
   */
  public boolean isSquareEmpty (int col, int row) {
    if (
      row >= 0 && 
      row < _height && 
      col >= 0 && 
      col < _width 
    ) {
      return _scene.get(col).get(row).getObjectClass().equals(Scene.EMPTY_SQUARE_IDENTIFIER);
    } else {
      return true; // no item off scene (!)
    }
  }
  
  public VisualSpatialField getVisualSpatialFieldGeneratedFrom(){
    return this._visualSpatialFieldGeneratedFrom;
  }
}

