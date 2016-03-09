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
  
  //The string used to denote blind squares in a Scene, i.e. squares that can't 
  //be seen in a Scene instance.
  private static final String BLIND_SQUARE_TOKEN = "null";
  
  //The string used to denote empty squares.
  private static final String EMPTY_SQUARE_TOKEN = ".";
  
  //The string used to identify the creator of the Scene instance.
  private static final String CREATOR_TOKEN = "SELF";
  
  //The actual scene.
  private final ArrayList<ArrayList<SceneObject>> _scene;

  /**
   * Constructor: the {@link #this} created is initially "blind"; empty squares
   * and items must be added using the appropriate methods from this class.
   * 
   * @param name Human-readable identifier for the Scene instance created.
   * @param width Represents the maximum number of indivisible x-coordinates 
   * that can be "seen".
   * @param height Represents the maximum number of indivisible y-coordinates 
   * that can be "seen".
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
        this._scene.get(col).add(new SceneObject("", Scene.getBlindSquareToken()));
      }
    }
  }
  
  /**
   * Creates a new {@link jchrest.lib.SceneObject} and inserts it into the 
   * specified coordinate in this {@link #this}.  If a {@link 
   * jchrest.lib.SceneObject} already exists on the coordinates specified, the 
   * new {@link jchrest.lib.SceneObject} overwrites the old one.
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
   * Adds the {@link jchrest.lib.SceneObject}s specified along the columns of 
   * the row specified from the minimum column in the row incrementally.  If the 
   * number of {@link jchrest.lib.SceneObject}s specified is greater than the 
   * maximum number of columns in this {@link #this},  the extra {@link 
   * jchrest.lib.SceneObject}s are ignored.
   * 
   * If a coordinate already contains a {@link jchrest.lib.SceneObject}, the new 
   * {@link jchrest.lib.SceneObject} overwrites the old one.
   * 
   * @param row The row to be modified.
   * @param items The {@link jchrest.lib.SceneObject}s to added to the row.
   */
  public void addItemsToRow (int row, ArrayList<SceneObject> items) {
    for (int i = 0; i < this._width; i++) {
      SceneObject item = items.get(i);
      if(!item.getObjectClass().equals(Scene.BLIND_SQUARE_TOKEN)){
        this.addItemToSquare(i, row, item.getIdentifier(), item.getObjectClass());
      }
    }
  }
  
  /**
   * Compute the errors of commission in this {@link #this} compared to another.
   * 
   * @param sceneToCompareAgainst
   * 
   * @return 
   * <ul>
   *  <li>
   *    If the number of squares in the {@link #this}s to be used in the 
   *    calculation are equal, the number of {@link jchrest.lib.SceneObject}s 
   *    that are in this {@link #this} but not the other is returned.  For 
   *    example, if this {@link #this} were to have 4 {@link 
   *    jchrest.lib.SceneObject}s and another {@link jchrest.lib.Scene} were to 
   *    have 3, the output of this function would be 1 (blind and empty 
   *    {@link jchrest.lib.SceneObject}s are not included in the calculation).  
   *  </li>
   *  <li>
   *    If the number of squares in the {@link #this}s to be used in the 
   *    calculation are not equal, an error is thrown since a fair calculation 
   *    can not be made in these circumstances.  
   *  </li>
   *  <li>
   *    If the number of {@link jchrest.lib.SceneObject}s in this {@link #this} 
   *    is less than the number of {@link jchrest.lib.SceneObject}s in the 
   *    {@link #this} compared against, 0 is returned.
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
    
    int numberItemsInThisScene = this.getAsListPattern(true).removeBlindEmptyAndUnknownItems().size();
    int numberItemsInOtherScene = sceneToCompareAgainst.getAsListPattern(true).removeBlindEmptyAndUnknownItems().size();
    
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
   *    If the number of squares in the {@link #this}s to be used in the 
   *    calculation are equal, the number of {@link jchrest.lib.SceneObject}s 
   *    that aren't in this {@link #this} but are in the other is returned.  For 
   *    example, if this {@link #this} were to have 3 {@link 
   *    jchrest.lib.SceneObject}s and another {@link jchrest.lib.Scene} were to 
   *    have 4, the output of this function would be 1 (blind and empty 
   *    {@link jchrest.lib.SceneObject}s are not included in the calculation).  
   *  </li>
   *  <li>
   *    If the number of squares in the {@link #this}s to be used in the 
   *    calculation are not equal, an error is thrown since a fair calculation 
   *    can not be made in these circumstances.  
   *  </li>
   *  <li>
   *    If the number of {@link jchrest.lib.SceneObject}s in this {@link #this} 
   *    is greater than the number of {@link jchrest.lib.SceneObject}s in the 
   *    {@link #this} compared against, 0 is returned.
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
    
    int numberItemsInThisScene = this.getAsListPattern(true).removeBlindEmptyAndUnknownItems().size();
    int numberItemsInOtherScene = sceneToCompareAgainst.getAsListPattern(true).removeBlindEmptyAndUnknownItems().size();

    if(numberItemsInThisScene >= numberItemsInOtherScene){
      return 0;
    }

    return numberItemsInOtherScene - numberItemsInThisScene;
  }
  
  /**
   * Compute precision of this {@link #this} against another, i.e. the 
   * percentage of non blind/empty {@link jchrest.lib.SceneObject}s in this 
   * {@link #this}, p (0 &lt;= p &lt;= 1), that are correct in their placement 
   * when compared to another.
   * 
   * @param sceneToCompareAgainst
   * @param objectsIdentifiedByObjectClass Set to true to specify that the 
   * {@link jchrest.lib.SceneObject}s in the {@link #this}s to compare should be 
   * identified and compared according to their object classes.  Set to false to 
   * specify that the items should be identified and compared by their unique 
   * identifiers.
   * 
   * @return 
   */
  public float computePrecision (Scene sceneToCompareAgainst, boolean objectsIdentifiedByObjectClass) {
    if(this.getHeight() != sceneToCompareAgainst.getHeight() || this.getWidth() != sceneToCompareAgainst.getWidth()){
      throw new IllegalArgumentException("Dimensions of scenes to compare are not equal: "
        + "height and width of scene whose recall is to be calculated = " + this.getHeight() + ", " + this.getWidth()
        + "height and width of scene compared against = " + sceneToCompareAgainst.getHeight() + ", " + sceneToCompareAgainst.getWidth() 
        + "."
      );
    }
      
    ListPattern itemsInThisScene = this.getAsListPattern(objectsIdentifiedByObjectClass).removeBlindEmptyAndUnknownItems();
    ListPattern itemsInOtherScene = sceneToCompareAgainst.getAsListPattern(objectsIdentifiedByObjectClass).removeBlindEmptyAndUnknownItems();

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
   * Compute recall of given {@link #this} against this one, i.e. the proportion 
   * of {@link jchrest.lib.SceneObject}s in this {@link #this} which have been 
   * correctly recalled, irrespective of their placement.
   * 
   * @param sceneToCompareAgainst
   * @param objectsIdentifiedByObjectClass
   * 
   * @return 
   */
  public float computeRecall (Scene sceneToCompareAgainst, boolean objectsIdentifiedByObjectClass) {
    if(this.getHeight() != sceneToCompareAgainst.getHeight() || this.getWidth() != sceneToCompareAgainst.getWidth()){
      throw new IllegalArgumentException("Dimensions of scenes to compare are not equal: "
        + "height and width of scene whose recall is to be calculated = " + this.getHeight() + ", " + this.getWidth()
        + "height and width of scene compared against = " + sceneToCompareAgainst.getHeight() + ", " + sceneToCompareAgainst.getWidth() 
        + "."
      );
    }
    
    ListPattern itemsInThisScene = this.getAsListPattern(objectsIdentifiedByObjectClass).removeBlindEmptyAndUnknownItems();
    ListPattern itemsInOtherScene = sceneToCompareAgainst.getAsListPattern(objectsIdentifiedByObjectClass).removeBlindEmptyAndUnknownItems();

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
   * @return The token used to denote blind squares in a {@link #this}.
   */
  public static String getBlindSquareToken(){
    return Scene.BLIND_SQUARE_TOKEN;
  }
  
  /**
   * @return The token used to denote empty squares in a {@link #this}.
   */
  public static String getEmptySquareToken(){
    return Scene.EMPTY_SQUARE_TOKEN;
  }
  
  /**
   * @param objectsIdentifiedByObjectClass Set to true to identify {@link 
   * jchrest.lib.SceneObject}s by their object class, set to false to identify 
   * {@link jchrest.lib.SceneObject}s by their unique identifier.
   * 
   * @return The {@link jchrest.lib.SceneObject}s in this {@link #this} 
   * (including blind and empty {@link jchrest.lib.SceneObject}s) in order from 
   * east -> west then south -> north as a {@link jchrest.lib.ListPattern} 
   * composed of {@link jchrest.lib.ItemSquarePattern}s.
   */
  public ListPattern getAsListPattern (boolean objectsIdentifiedByObjectClass){
    ListPattern scene = new ListPattern();
    
    for(int row = 0; row < _height; row++){
      for(int col = 0; col < _width; col++){
        scene = scene.append(this.getSquareContentsAsListPattern(col, row, objectsIdentifiedByObjectClass));
      }
    }
    
    return scene;
  }
  
  /**
   * @return The number of rows in this {@link #this}.
   */
  public int getHeight () {
    return _height;
  }
  
  /**
   * @param startCol
   * @param startRow
   * @param scope
   * 
   * @param objectsIdentifiedByObjectClass Set to true to identify {@link 
   * jchrest.lib.SceneObject}s by their object class, set to false to identify 
   * {@link jchrest.lib.SceneObject}s by their unique identifier.
   * 
   * @return All {@link jchrest.lib.SceneObject}s within given row and column 
   * +/- the specified field of view in order from east -> west then south -> 
   * north as a {@link jchrest.lib.ListPattern} composed of {@link 
   * jchrest.lib.ItemSquarePattern}s. Blind and empty {@link 
   * jchrest.lib.SceneObject}s are not returned.
   */
  public ListPattern getItemsInScopeAsListPattern (int startCol, int startRow, int scope, boolean objectsIdentifiedByObjectClass) {
    ListPattern itemsInScope = new ListPattern ();
    
    for (int row = startRow - scope; row <= startRow + scope; row++) {
      if (row >= 0 && row < _height) {
        for (int col = startCol - scope; col <= startCol + scope; col++) {
          if (col >= 0 && col < _width) {
            itemsInScope = itemsInScope.append(this.getSquareContentsAsListPattern(col, row, objectsIdentifiedByObjectClass));
          }
        }
      }
    }
    return itemsInScope;
  }
   
  /**
   * @param col
   * @param row
   * @return The contents of the coordinate specified in this {@link #this}.
   */
  public SceneObject getSquareContents(int col, int row){
    if(
      (col >= 0 && col < this.getWidth()) && 
      (row >= 0 && row < this.getHeight())
    ){
      return this._scene.get(col).get(row);
    }
    else{
      return null;
    }
  }
  
  /**
   * @param col
   * @param row
   * 
   * @param objectsIdentifiedByObjectClass Set to true to identify {@link 
   * jchrest.lib.SceneObject}s by their object class, set to false to identify 
   * {@link jchrest.lib.SceneObject}s by their unique identifier.
   * 
   * @return The contents of the coordinate specified in this {@link #this}.
   */
  public ListPattern getSquareContentsAsListPattern (int col, int row, boolean objectsIdentifiedByObjectClass) {
    ListPattern squareContentsAsListPattern = new ListPattern();
    
    if (row >= 0 && row < _height && col >= 0 && col < _width) {
      SceneObject squareContents = this.getSquareContents(col, row);
      int itemSquarePatternCol = col;
      int itemSquarePatternRow = row;
      
      //By default, assume that the item identifier for the ItemSquarePattern
      //representation of the object on square will have the object's class
      //as the item identifier.
      String itemIdentifier = squareContents.getObjectClass();
      
      if(!objectsIdentifiedByObjectClass){
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
   * @return The location of this {@link #this}'s creator (if it identified 
   * itself) in this {@link jchrest.lib.Scene}.  If it didn't, null is returned.
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

  /**git 
   * @return The name of this {@link #this}.
   */
  public String getName () {
    return _name;
  }
  
   /**
   * @return The string used to denote the creator of the {@link #this} in the
   * {@link #this}.
   */
  public static String getCreatorToken(){
    return Scene.CREATOR_TOKEN;
  }

  /**
   * @return The number of columns in this {@link #this}
   */
  public int getWidth () {
    return _width;
  }
  
  /**
   * @param col
   * @param row
   * @return True if the coordinate specified contains a {@link 
   * jchrest.lib.SceneObject} representing a blind square.
   */
  public Boolean isSquareBlind(int col, int row){
    return _scene.get(col).get(row).getObjectClass().equals(Scene.BLIND_SQUARE_TOKEN);
  }

  /**
   * @param row
   * @param col
   * @return True if the coordinate specified contains a {@link 
   * jchrest.lib.SceneObject} representing an empty square.
   */
  public boolean isSquareEmpty (int col, int row) {
    return _scene.get(col).get(row).getObjectClass().equals(Scene.EMPTY_SQUARE_TOKEN);
  }
  
  /**
   * @return The {@link jchrest.architecture.VisualSpatialField} this {@link 
   * #this} was created from, if applicable.
   */
  public VisualSpatialField getVisualSpatialFieldGeneratedFrom(){
    return this._visualSpatialFieldGeneratedFrom;
  }
}

