// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import java.util.ArrayList;

/**
 * Represents a 2D external environment that a CHREST model can "see" as a 3D
 * {@link java.util.ArrayList>.  The data structure is organised as below:
 * 
 * <ul>
 *  <li>
 *    First-dimension elements represent columns (x-axis) in the external 
 *    environment.
 *  </li>
 *  <li>
 *    Second-dimension elements represent rows (y-axis) in the external 
 *    environment.
 *  </li>
 *  <li>
 *    Third-dimension elements represent objects in the external environment as 
 *    {@link jchrest.lib.SceneObject}s, allowing multiple object's to occupy the 
 *    same coordinates, if required.
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
  
  //The string used to identify "blind-spots" i.e. squares that can't be seen in
  //a Scene instance.
  private static final String BLIND_SQUARE_IDENTIFIER = "null";
  
  //The string used to identify empty squares
  private static final String EMPTY_SQUARE_IDENTIFIER = ".";
  
  //The string used to identify the creator of the Scene instance.
  private static final String SELF_IDENTIFIER = "SELF";
  
  //The actual scene.
  private final ArrayList<ArrayList<ArrayList<SceneObject>>> _scene;

  /**
   * Constructor: the instance created is initially "blind", empty squares and
   * items must be added using the appropriate methods from this class.
   * 
   * @param name Human-readable identifier for the Scene instance created.
   * @param width Represents the maximum number of indivisible x-coordinates 
   * that can be "seen" in an external vision.
   * @param height Represents the maximum number of indivisible y-coordinates 
   * that can be "seen" in an external vision.
   */
  public Scene (String name, int width, int height) {
    this._name = name;
    this._height = height;
    this._width = width;
    
    //Instantiate scene with blind squares at first (empty squares must be 
    //encoded explicitly).  This allows for "blind-spots" to be distinguished 
    //from empty squares.  Note that the identifier for blind objects is set to
    //null since they can not be moved in a visual-spatial field if they are 
    //converted to MindsEyeObject instances and the purpose of the identifier is
    //to allow MindsEyeObject instances to be precisely moved.
    this._scene = new ArrayList<>();
    for(int col = 0; col < width; col++){
      this._scene.add(new ArrayList<>());
      for(int row = 0; row < height; row++){
        this._scene.get(col).add(new ArrayList<>());
        this._scene.get(col).get(row).add(new SceneObject(null, Scene.getBlindSquareIdentifier()));
      }
    }
    
  }
  
  /**
   * Adds the item identifier to the specified square in the scene.  
   * <ul>
   *  <li>
   *    If the square is currently considered as being a blind spot, add the
   *    item.  This may seem nonsensical but if addition is blocked on blind
   *    squares, no Scene would ever contain anything other than blind squares
   *    since Scene instances are completely blind after initial instantiation.
   *  </li>
   *  <li>
   *    If the specified square is currently empty, the item specified replaces 
   *    the "empty" identifier.
   *  </li>
   *  <li>
   *    If the specified square is not currently empty and the item to be added
   *    is not empty, then the item is appended to the current contents of the 
   *    square with a comma prefix i.e. if the specified square contains "A" and 
   *    "B" is to be added, the contents of the specified square will equal 
   *    "A,B".
   *  </li>
   *  <li>
   *    If the specified square is not currently empty and the item to be added
   *    is empty, then the square becomes empty i.e. if the specified square 
   *    contains "A,B" and the square is to become empty, the contents of the 
   *    specified square will contain the empty square identifier only.
   *  </li>
   * </ul>
   * 
   * @param row
   * @param col
   * @param identifier
   * @param objectClass
   */
  public void addItemToSquare (int col, int row, Integer identifier, String objectClass) {
    assert (row >= 0 && row < _height && col >= 0 && col < _width);
    
    ArrayList<SceneObject> squareContents = _scene.get(col).get(row);
    ArrayList<String> objectClassesOnSquare = new ArrayList<>(); 
    for(SceneObject object : squareContents){
      objectClassesOnSquare.add(object.getObjectClass());
    }
      
    //If the square is currently considered as "blind", add the item.
    if(objectClassesOnSquare.contains(Scene.BLIND_SQUARE_IDENTIFIER)){

      //Remove the blind square SceneObject so the square is no longer blind.
      _scene.get(col).get(row).clear();

      //Add the new item using the information provided.
      _scene.get(col).get(row).add(new SceneObject(identifier, objectClass));
    }
    //Else, if the square is empty and the item to be added isn't empty, or
    //the square is not empty and the item to be added is empty, overwrite the
    //existing square content (square is always "wiped clean" if not empty and
    //empty identifier is added).
    else if(
      (objectClassesOnSquare.contains(Scene.EMPTY_SQUARE_IDENTIFIER) && objectClass.equals(Scene.EMPTY_SQUARE_IDENTIFIER)) ||
      (!objectClassesOnSquare.contains(Scene.EMPTY_SQUARE_IDENTIFIER) && objectClass.equals(Scene.EMPTY_SQUARE_IDENTIFIER))
    ){
      _scene.get(col).get(row).clear();
      _scene.get(col).get(row).add(new SceneObject(null, objectClass));
    }
    //If the square is empty and the item is empty, do nothing.
    else if(objectClassesOnSquare.contains(Scene.EMPTY_SQUARE_IDENTIFIER) && objectClass.equals(Scene.EMPTY_SQUARE_IDENTIFIER)){}
    //Otherwise, the square isn't empty and neither is the item so append the 
    //item to the current contents.
    else{
      _scene.get(col).get(row).add(new SceneObject(identifier, objectClass));
    }
  }
  
  /**
   * Adds items to columns in the specified row from column 0 incrementally.  
   * For example, if a scene's row is 3 squares wide and the number of items to
   * be added is 5 then the first item in the "items" parameter will be added at
   * column 0 in the row specified, the second item in the "items" parameter 
   * will be added at column 1 in the row specified and so on until all items
   * have been processed.  If the number of items specified is greater than the
   * number of columns in the row, the extra items are ignored.
   * 
   * If a row's square already contains an item, the new item is appended to the
   * square using a comma i.e. if "B" is to be added to a square containing "A"
   * the result would be "A,B".
   * 
   * @param row The row to be modified.
   * @param items The items to added to the row in column order as 
   * {@link jchrest.lib.SceneObject} instances.
   */
  public void addItemsToRow (int row, ArrayList<SceneObject> items) {
    
    //If the square is not meant to be left as a blind-spot then add the item to 
    //the square specified accordingly.
    for (int i = 0; i < items.size(); ++i) {
      SceneObject item = items.get(i);
      if(!item.getObjectClass().equals(Scene.BLIND_SQUARE_IDENTIFIER)){
        this.addItemToSquare(i, row, item.getIdentifier(), item.getObjectClass());
      }
    }
  }
  
  /**
   * Compute errors of commission of given scene against this one.
   * Commission is the number of pieces which are in the given scene but not in 
   * this one.
   * 
   * @param sceneToCompareAgainst
   * 
   * @return 
   */
  public int computeErrorsOfCommission (Scene sceneToCompareAgainst) {
    return sceneToCompareAgainst.getEntireSceneAsListPattern(false, true).size() - this.getEntireSceneAsListPattern(false, true).size();
  }
  
  /**
   * Compute errors of omission of given scene against this one.
   * Omission is the number of pieces which are in this scene but not in the 
   * given one.
   * 
   * @param sceneToCompareAgainst
   * 
   * @return 
   */
  public int computeErrorsOfOmission (Scene sceneToCompareAgainst) {
    return this.getEntireSceneAsListPattern(false, true).size() - sceneToCompareAgainst.getEntireSceneAsListPattern(false, true).size();
  }
  
  /**
   * Compute precision of this scene against a given scene i.e. the proportion 
   * of pieces in the scene that are correct in their placement when compared 
   * to the other.
   * 
   * @param sceneToCompareAgainst
   * @param itemsIdentifiedByObjectClass Set to true to specify that the items
   * in this {@link jchrest.lib.Scene} and the {@link jchrest.lib.Scene} 
   * compared against should be identified and compared according to their 
   * object classes.  Set to false to specify that the items should be 
   * identified and compared by their unique identifiers.
   * 
   * 
   * @return 
   */
  public float computePrecision (Scene sceneToCompareAgainst, boolean itemsIdentifiedByObjectClass) {
    if(
      this.getEntireSceneAsListPattern(false, itemsIdentifiedByObjectClass).isEmpty() || 
      sceneToCompareAgainst.getEntireSceneAsListPattern(false, itemsIdentifiedByObjectClass).isEmpty()
    ){
      return 0.0f;
    }
    else{
      int numberOfCorrectlyPlacedItems = 0;
      for(int row = 0; row < this._height; row++){
        for(int col = 0; col < this._width; col++){
          ListPattern itemsOnSquareInThisScene = this.getSquareContentsAsListPattern(col, row, false, itemsIdentifiedByObjectClass);
          ListPattern itemsOnSquareInOtherScene = sceneToCompareAgainst.getSquareContentsAsListPattern(col, row, false, itemsIdentifiedByObjectClass);

          ArrayList<String> itemsOnSquareInThisSceneAsStrings = new ArrayList<>();
          ArrayList<String> itemsOnSquareInOtherSceneAsStrings = new ArrayList<>();
          
          for(PrimitivePattern item : itemsOnSquareInThisScene){
            itemsOnSquareInThisSceneAsStrings.add( ((ItemSquarePattern)item).toString() );
          }
          
          for(PrimitivePattern item : itemsOnSquareInOtherScene){
            itemsOnSquareInOtherSceneAsStrings.add( ((ItemSquarePattern)item).toString() );
          }
          
          for(String itemOnSquareInOtherSceneAsString : itemsOnSquareInOtherSceneAsStrings){
            if(itemsOnSquareInThisSceneAsStrings.contains(itemOnSquareInOtherSceneAsString)){
              numberOfCorrectlyPlacedItems++;
            }
          }
        }
      }

      return (float)numberOfCorrectlyPlacedItems / (float)sceneToCompareAgainst.getEntireSceneAsListPattern(false, itemsIdentifiedByObjectClass).size();
    }
  }
  
  /**
   * Compute recall of given scene against this one.
   * Recall is the proportion of pieces in this scene which have been correctly 
   * recalled, irrespective of correct placement.
   * 
   * @param sceneToCompareAgainst
   * 
   * @return 
   */
  public float computeRecall (Scene sceneToCompareAgainst) {
    
    //The parameters passed to the "getEntireSceneAsListPattern" are of no 
    //consequence here.
    float numberOfItemsInThisScene = (float)this.getEntireSceneAsListPattern(true, true).size();
    float numberOfItemsInOtherScene = (float)sceneToCompareAgainst.getEntireSceneAsListPattern(true, true).size();
    
    if(numberOfItemsInThisScene == 0 || numberOfItemsInOtherScene == 0){
      return 0.0f;
    }
    else{
      return numberOfItemsInThisScene / numberOfItemsInOtherScene;
    }
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
   * Returns this {@link jchrest.lib.Scene} instance as-is.
   * 
   * @return 
   */
  public ArrayList<ArrayList<ArrayList<SceneObject>>> getEntireScene(){
    return this._scene;
  }
  
  /**
   * Returns the scene (including blind and empty squares) from east -> west 
   * then south -> north as a {@link jchrest.lib.ListPattern} instance composed
   * of {@link jchrest.lib.ItemSquarePattern} instances representing the items
   * in the scene.
   * 
   * @param selfRelativeCoordinates Set to false to force the function to return 
   * Scene specific coordinates even when the Scene's creator is identified in 
   * the Scene.
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
  public ListPattern getEntireSceneAsListPattern(boolean selfRelativeCoordinates, boolean identifyItemsByObjectClass){
    ListPattern scene = new ListPattern();
    
    for(int row = 0; row < _height; row++){
      for(int col = 0; col < _width; col++){
        scene.append(this.getSquareContentsAsListPattern(col, row, selfRelativeCoordinates, identifyItemsByObjectClass));
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
   * Retrieve all items within given row +/- size, column +/- size (blind 
   * and empty squares are not returned).
   * 
   * @param startCol
   * @param startRow
   * @param colScope
   * @param rowScope
   * @param selfRelativeCoordinates Set to true to return square coordinates 
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
  public ListPattern getItemsInScopeAsListPattern (int startCol, int startRow, int colScope, int rowScope, boolean selfRelativeCoordinates, boolean identifyItemsByObjectClass) {
    ListPattern itemsInScope = new ListPattern ();

    for (int row = startRow - rowScope; row <= startRow + rowScope; row++) {
      if (row >= 0 && row < _height) {
        for (int col = startCol - colScope; col <= startCol + colScope; col++) {
          if (col >= 0 && col < _width) {
            itemsInScope = itemsInScope.append(this.getSquareContentsAsListPattern(col, row, selfRelativeCoordinates, identifyItemsByObjectClass));
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
  public ArrayList<SceneObject> getSquareContents(int col, int row){
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
   * @param selfRelativeCoordinates Set to false to force the column and row
   * coordinates specified in the {@link jchrest.lib.ItemSquarePattern}s 
   * returned to not be relative to the creator of this 
   * {@link jchrest.lib.Scene} if they are present in it.
   * @param identifyItemsByObjectClass
   * 
   * @return 
   */
  public ListPattern getSquareContentsAsListPattern (int col, int row, boolean selfRelativeCoordinates, boolean identifyItemsByObjectClass) {
    ListPattern squareContentsAsListPattern = new ListPattern();
    
    if (row >= 0 && row < _height && col >= 0 && col < _width) {
      Square locationOfSelf = this.getLocationOfSelf();
      ArrayList<SceneObject> squareContents = this._scene.get(col).get(row);
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
      if(locationOfSelf != null && selfRelativeCoordinates){
        itemSquarePatternCol = col - locationOfSelf.getColumn();
        itemSquarePatternRow = row - locationOfSelf.getRow();
      }   
      
      for(SceneObject objectOnSquare : squareContents){
        
        //By default, assume that the item identifier for the ItemSquarePattern
        //representation of the object on square will have the object's class
        //as the item identifier.
        String itemIdentifier = objectOnSquare.getObjectClass();
        
        //If items are not to be identified by their class, i.e. they should be
        //identified by their unique identifier, overwrite the item identifier
        //set above only if the item does not indicate a blind square or an 
        //empty square since these items should not have unique identifiers.
        if(
          !identifyItemsByObjectClass && 
          !itemIdentifier.equals(Scene.BLIND_SQUARE_IDENTIFIER) &&
          !itemIdentifier.equals(Scene.EMPTY_SQUARE_IDENTIFIER)
        ){
          itemIdentifier = String.valueOf(objectOnSquare.getIdentifier());
        }
        
        squareContentsAsListPattern.add(new ItemSquarePattern(
          itemIdentifier,
          itemSquarePatternCol,
          itemSquarePatternRow
        ));
      }
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
  public Square getLocationOfSelf(){
    for(int row = 0; row < this._height; row++){
      for(int col = 0; col < this._width; col++){
        ArrayList<SceneObject> squareContents = this._scene.get(col).get(row);
        for(SceneObject object : squareContents){
          if(object.getObjectClass().equals(Scene.SELF_IDENTIFIER)){
            return new Square(col, row);
          }
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
  public static String getSelfIdentifier(){
    return Scene.SELF_IDENTIFIER;
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
      ArrayList<SceneObject> squareContents = _scene.get(col).get(row);
      ArrayList<String> objectClasses = new ArrayList<>();
      for(SceneObject object : squareContents){
        objectClasses.add(object.getObjectClass());
      }
      
      return objectClasses.contains(Scene.BLIND_SQUARE_IDENTIFIER);
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
      ArrayList<SceneObject> squareContents = _scene.get(col).get(row);
      ArrayList<String> objectClasses = new ArrayList<>();
      for(SceneObject object : squareContents){
        objectClasses.add(object.getObjectClass());
      }
      
      return objectClasses.contains(Scene.EMPTY_SQUARE_IDENTIFIER);
    } else {
      return true; // no item off scene (!)
    }
  }
}

