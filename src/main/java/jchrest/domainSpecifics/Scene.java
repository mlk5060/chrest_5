// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.domainSpecifics;

import java.util.ArrayList;
import java.util.Objects;
import jchrest.architecture.VisualSpatialField;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.PrimitivePattern;
import jchrest.lib.Square;

/**
 * Represents a 2D external environment that a CHREST model can "see" as a 2D
 * {@link java.util.ArrayList} containing one {@link 
 * jchrest.domainSpecifics.SceneObject} per coordinate.  The {@link 
 * java.util.ArrayList} created has the following structure:
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
 * </ul>
 * 
 * Constructing the data structure in this way means that coordinate 
 * specification must follow the form of x-coordinate <b>then</b> y-coordinate.
 * Thus, coordinate specification in a {@link jchrest.domainSpecifics.Scene} is
 * congruent with the <a href="http://www.bbc.co.uk/schools/teachers/
 * ks2_lessonplans/maths/grids.shtml"> along the corridor, up the stairs</a> 
 * approach to 2D grid reading. 
 * <p>
 * Rows and columns are zero-indexed and therefore, identifying coordinates in
 * a {@link jchrest.domainSpecifics.Scene} should not use coordinates specific 
 * to the external environment (unless coordinates for the external environment 
 * are also zero-indexed). However, {@link jchrest.domainSpecifics.Scene Scenes} 
 * are capable of returning the domain-specific coordinates of any coordinate 
 * they represent since the minimum x/y-coordinate that the {@link 
 * jchrest.domainSpecifics.Scene} represents is required when {@link 
 * #this#Scene(java.lang.String, int, int, int, int, 
 * jchrest.architecture.VisualSpatialField)} is invoked.
 * 
 * @author Peter C. R. Lane <p.c.lane@herts.ac.uk>
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class Scene {
  //TODO: Remove "get" methods for static fields now that they've been made 
  //      public.  Should also move these to jchrest.domainSpecifics.SceneObject
  //      since it makes more sense to have them there.
  public static final String BLIND_SQUARE_TOKEN = "*";
  public static final String EMPTY_SQUARE_TOKEN = ".";
  public static final String CREATOR_TOKEN = "SELF";
  
  protected final String _name;
  protected final int _height;
  protected final int _width;
  protected final int _minimumDomainSpecificColumn;
  protected final int _minimumDomainSpecificRow;
  protected ArrayList<ArrayList<SceneObject>> _scene;
  private final VisualSpatialField _visualSpatialFieldRepresented;

  /**
   * Constructor.
   * 
   * {@link #this} is initially "blind".
   * 
   * @param name Human-readable identifier for {@link #this}.
   * @param minDomainSpecificCol The minimum column in the domain that {@link 
   * #this} is intended to represent.  Note that this value must represent an 
   * absolute coordinate value in the domain.
   * @param minDomainSpecificRow The minimum row in the domain that {@link 
   * #this} is intended to represent.  Note that this value must represent an 
   * absolute coordinate value in the domain.
   * @param width Represents the maximum number of indivisible columns that can 
   * be "seen".
   * @param height Represents the maximum number of indivisible rows that can 
   * be "seen".
   * @param visualSpatialFieldRepresented Set to {@code null} if {@link #this} 
   * does not represent a {@link jchrest.architecture.VisualSpatialField}.
   */
  public Scene(
    String name, 
    int width, 
    int height, 
    int minDomainSpecificCol, 
    int minDomainSpecificRow, 
    VisualSpatialField visualSpatialFieldRepresented
  ){
    if(width <= 0 || height <= 0){
      throw new IllegalArgumentException(
        "The width (" + width + ") or height (" + height + ") specified for a " +
        "new Scene is <= 0."
      );
    }
    
    this._name = name;
    this._height = height;
    this._width = width;
    this._minimumDomainSpecificColumn = minDomainSpecificCol;
    this._minimumDomainSpecificRow = minDomainSpecificRow;
    this._visualSpatialFieldRepresented = visualSpatialFieldRepresented;
    this._scene = new ArrayList<>();
    
    for(int col = 0; col < width; col++){
      this._scene.add(new ArrayList<>());
      for(int row = 0; row < height; row++){
        this._scene.get(col).add(new SceneObject(Scene.getBlindSquareToken()));
      }
    }
  }
  
  /**
   * 
   * @param col Should be specific to {@link #this}.
   * @param row Should be specific to {@link #this}.
   * @param object 
   */
  public void addObjectToSquare(int col, int row, SceneObject object){
    if(row < 0 || row > this._height || col < 0 || col > this._width){
      throw new IllegalArgumentException(
        "The column or row to add a SceneObject to (" + col + "," + row + ") " +
        "is < 0 or greater than the maximum width/height of the scene with " +
        "name '" + this._name + "' (" + this._width + " and " + this._height + 
        ", respectively)."
      );
    }
    _scene.get(col).set(row, object);
  }
  
  /**
   * Wrapper for {@link #this#addItemToSquare(int, int, 
   * jchrest.domainSpecifics.SceneObject)} that creates a new {@link 
   * jchrest.domainSpecifics.SceneObject} that will have a randomly assigned 
   * identifier but the {@code objectType} specified.
   * 
   * @param row
   * @param col
   * @param objectType
   */
  public void addObjectToSquare(int col, int row, String objectType) {
    this.addObjectToSquare(col, row, new SceneObject(objectType));
  }
  
  /**
   * Adds the {@link jchrest.domainSpecifics.SceneObject SceneObjects} specified 
   * along the columns of the row specified from the minimum column in the row 
   * incrementally.  If the number of {@link jchrest.lib.SceneObject 
   * SceneObjects} specified is greater than the maximum number of columns in 
   * {@link #this}, the extra {@link jchrest.lib.SceneObject SceneObjects} are 
   * ignored.
   * <p>
   * If a coordinate already contains a {@link jchrest.lib.SceneObject}, it is
   * overwritten.
   * 
   * @param row 
   * @param objects
   */
  public void addObjectsToRow (int row, ArrayList<SceneObject> objects) {
    for (int i = 0; i < this._width; i++) {
      SceneObject item = objects.get(i);
      this._scene.get(i).set(row, item);
    }
  }
  
  /**
   * @param domainSpecificCol
   * @param domainSpecificRow
   * 
   * @return Whether or not the {@code domainSpecificCol} and {@code 
   * domainSpecificRow} specified are represented in {@link #this}. 
   */
  public boolean areDomainSpecificCoordinatesRepresented(int domainSpecificCol, int domainSpecificRow){
    for(int col = 0; col < this.getWidth(); col++){
      for(int row = 0; row < this.getHeight(); row++){
        if(
          this._minimumDomainSpecificColumn + col == domainSpecificCol &&
          this._minimumDomainSpecificRow + row == domainSpecificRow
        ){
          return true;
        }
      }
    }
    
    return false;
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
   *    jchrest.lib.SceneObject SceneObjects} and another {@link 
   *    jchrest.domainSpecifics.Scene} were to have 3, the output of this 
   *    function would be 1 (blind and empty {@link jchrest.lib.SceneObject 
   *    SceneObjects} are not included in the calculation).  
   *  </li>
   *  <li>
   *    If the number of squares in {@link #this} that are to be used in the 
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
    
    int numberItemsInThisScene = this.getAsListPattern().removeBlindEmptyAndUnknownItems().size();
    int numberItemsInOtherScene = sceneToCompareAgainst.getAsListPattern().removeBlindEmptyAndUnknownItems().size();
    
    if(numberItemsInThisScene <= numberItemsInOtherScene){
      return 0;
    }

    return numberItemsInThisScene - numberItemsInOtherScene;
  }
  
  /**
   * Compute the errors of ommission in {@link #this} compared to the {@code 
   * sceneToCompareAgainst} specified.
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
   *    jchrest.lib.SceneObject}s and another {@link jchrest.domainSpecifics.Scene} were to 
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
    
    int numberItemsInThisScene = this.getAsListPattern().removeBlindEmptyAndUnknownItems().size();
    int numberItemsInOtherScene = sceneToCompareAgainst.getAsListPattern().removeBlindEmptyAndUnknownItems().size();

    if(numberItemsInThisScene >= numberItemsInOtherScene){
      return 0;
    }

    return numberItemsInOtherScene - numberItemsInThisScene;
  }
  
  /**
   * Compute precision of this {@link #this} against the {@code 
   * sceneToCompareAgainst} specified, i.e. the percentage of non blind/empty 
   * {@link jchrest.lib.SceneObject SceneObjects} in {@link #this}, that are 
   * placed on the same coordinates in both {@link jchrest.domainSpecifics.Scene 
   * Scenes}.
   * <p>
   * To determine correct placement the result of invoking {@link 
   * jchrest.domainSpecifics.SceneObject#getObjectType()} on each {@link 
   * jchrest.domainSpecifics.SceneObject} in {@link #this} and the {@code 
   * sceneToCompareAgainst} are compared.
   * 
   * @param sceneToCompareAgainst
   * 
   * @return 
   */
  public float computePrecision (Scene sceneToCompareAgainst) {
    if(this.getHeight() != sceneToCompareAgainst.getHeight() || this.getWidth() != sceneToCompareAgainst.getWidth()){
      throw new IllegalArgumentException("Dimensions of scenes to compare are not equal: "
        + "height and width of scene whose recall is to be calculated = " + this.getHeight() + ", " + this.getWidth()
        + "height and width of scene compared against = " + sceneToCompareAgainst.getHeight() + ", " + sceneToCompareAgainst.getWidth() 
        + "."
      );
    }
      
    ListPattern itemsInThisScene = this.getAsListPattern().removeBlindEmptyAndUnknownItems();
    ListPattern itemsInOtherScene = sceneToCompareAgainst.getAsListPattern().removeBlindEmptyAndUnknownItems();

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
   * <p>
   * To determine recall the result of invoking {@link 
   * jchrest.domainSpecifics.SceneObject#getObjectType()} on each {@link 
   * jchrest.domainSpecifics.SceneObject} in {@link #this} and the {@code 
   * sceneToCompareAgainst} are compared.
   * 
   * @param sceneToCompareAgainst
   * 
   * @return 
   */
  public float computeRecall (Scene sceneToCompareAgainst) {
    if(this.getHeight() != sceneToCompareAgainst.getHeight() || this.getWidth() != sceneToCompareAgainst.getWidth()){
      throw new IllegalArgumentException("Dimensions of scenes to compare are not equal: "
        + "height and width of scene whose recall is to be calculated = " + this.getHeight() + ", " + this.getWidth()
        + "height and width of scene compared against = " + sceneToCompareAgainst.getHeight() + ", " + sceneToCompareAgainst.getWidth() 
        + "."
      );
    }
    
    ListPattern itemsInThisScene = this.getAsListPattern().removeBlindEmptyAndUnknownItems();
    ListPattern itemsInOtherScene = sceneToCompareAgainst.getAsListPattern().removeBlindEmptyAndUnknownItems();

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
   * @param scene
   * 
   * @return {@link java.lang.Boolean#TRUE} if the following conditions all 
   * evaluate to {@link java.lang.Boolean#TRUE}, {@link java.lang.Boolean#FALSE}
   * otherwise:
   * <ul>
   *    <li>
   *      {@code scene} is not {@code null}.
   *    </li>
   *    <li>
   *      The class of {@link #this} (see {@link #this#getClass()} and class of
   *      {@code scene} are equal.
   *    </li>
   *    <li>
   *      {@link #this#sameDomainSpace(jchrest.domainSpecifics.Scene)} evaluates 
   *      to {@link java.lang.Boolean#TRUE} when {@code scene} is passed as a 
   *      parameter.
   *    </li>
   *    <li>
   *      All {@link jchrest.domainSpecifics.SceneObject SceneObjects} on the 
   *      {@link jchrest.lib.Square Squares} in {@link #this} are located on the 
   *      same {@link jchrest.lib.Square Squares} in {@code scene} ({@link 
   *      jchrest.domainSpecifics.SceneObject SceneObjects} are identified using
   *      the result of invoking {@link 
   *      jchrest.domainSpecifics.SceneObject#getObjectType()}).
   *    </li>
   * </ul>
   */
  @Override
  public boolean equals(Object scene){
    if(scene != null && this.getClass().equals(scene.getClass())){
      
      //The scene passed must be a Scene instance at this point so cast it so 
      //that it can be used in Scene functions.
      Scene sceneAsScene = (Scene)scene;
      if(this.sameDomainSpace(sceneAsScene)){
        for(int col = 0; col < this.getWidth(); col++){
          for(int row = 0; row < this.getHeight(); row++){
            if(!this._scene.get(col).get(row).getObjectType().equals(sceneAsScene._scene.get(col).get(row).getObjectType())) return false;
          }
        }

        return true;
      }
    }
    return false;
  }
  
  /**
   * @return The {@link jchrest.lib.SceneObject SceneObjects} in {@link #this} 
   * (including {@link jchrest.lib.SceneObject SceneObjects} representing blind 
   * and empty squares) in order from east -> west then south -> north as a 
   * {@link jchrest.lib.ListPattern} composed of {@link 
   * jchrest.lib.ItemSquarePattern ItemSquarePatterns}.
   * <p>
   * The "item" for each {@link jchrest.lib.ItemSquarePattern} will be the 
   * result of invoking {@link 
   * jchrest.domainSpecifics.SceneObject#getObjectType()} on each {@link 
   * jchrest.domainSpecifics.SceneObject} present in {@link #this}.
   */
  public ListPattern getAsListPattern(){
    ListPattern scene = new ListPattern();
    
    for(int row = 0; row < _height; row++){
      for(int col = 0; col < _width; col++){
        scene = scene.append(this.getSquareContentsAsListPattern(col, row));
      }
    }
    
    return scene;
  }
  
  /**
   * @return The token used to denote blind squares in a {@link #this}.
   */
  public static String getBlindSquareToken(){
    return Scene.BLIND_SQUARE_TOKEN;
  }
  
  /**
   * @return The string used to denote the creator of the {@link #this} in the
   * {@link #this}.
   */
  public static String getCreatorToken(){
    return Scene.CREATOR_TOKEN;
  }
  
  /**
   * @param sceneSpecificCol Should be zero-indexed.
   * 
   * @return The absolute domain-specific column value in context of {@link 
   * #this} given {@code sceneSpecificCol} relative to the coordinates of {@link 
   * #this}.
   */
  public int getDomainSpecificColFromSceneSpecificCol(int sceneSpecificCol){
    return this._minimumDomainSpecificColumn + sceneSpecificCol;
  }
  
  /**
   * @param sceneSpecificRow Should be zero-indexed.
   * 
   * @return The absolute domain-specific row value in context of {@link #this} 
   * given {@code sceneSpecificRow} relative to the coordinates of {@link 
   * #this}.
   */
  public int getDomainSpecificRowFromSceneSpecificRow(int sceneSpecificRow){
    return this._minimumDomainSpecificRow + sceneSpecificRow;
  }
  
  /**
   * @return The token used to denote empty squares in a {@link #this}.
   */
  public static String getEmptySquareToken(){
    return Scene.EMPTY_SQUARE_TOKEN;
  }
  
  /**
   * @return The number of rows in this {@link #this}.
   */
  public int getHeight() {
    return _height;
  }
  
  /**
   * @param col
   * @param row
   * @param scope
   * 
   * @return All {@link jchrest.lib.SceneObject SceneObjects} on coordinates
   * +/- the specified {@code scope} from the coordinate specified by {@code 
   * col} and {@code row} from the most south-easterly coordinate to the most 
   * north-westerly coordinate (all columns in a row are processed before the 
   * next row is processed) as a {@link jchrest.lib.ListPattern} composed of 
   * {@link jchrest.lib.ItemSquarePattern ItemSquarePatterns} ({@link 
   * jchrest.lib.SceneObject SceneObjects} representing blind and empty 
   * coordinates are not included).
   * <p>
   * The "item" for each {@link jchrest.lib.ItemSquarePattern} will be the 
   * result of invoking {@link 
   * jchrest.domainSpecifics.SceneObject#getObjectType()} on each {@link 
   * jchrest.domainSpecifics.SceneObject} present on the applicable coordinates.
   */
  public ListPattern getItemsInScopeAsListPattern (int col, int row, int scope) {
    ListPattern itemsInScope = new ListPattern ();
    
    for (int r = row - scope; r <= row + scope; r++) {
      if (r >= 0 && r < _height) {
        for (int c = col - scope; c <= col + scope; c++) {
          if (c >= 0 && c < _width) {
            itemsInScope = itemsInScope.append(this.getSquareContentsAsListPattern(c, r));
          }
        }
      }
    }
    return itemsInScope;
  }
  
  /**
   * @return The location of this {@link #this}'s creator (if it identified 
   * itself) in this {@link jchrest.domainSpecifics.Scene}.  If it didn't, 
   * {@code null} is returned.
   */
  public Square getLocationOfCreator(){
    for(int row = 0; row < this._height; row++){
      for(int col = 0; col < this._width; col++){
        String squareContents = this._scene.get(col).get(row).getObjectType();
        if(squareContents.equals(Scene.CREATOR_TOKEN)){
          return new Square(col, row);
        }
      }
    }
    
    return null;
  }
  
  /**
   * @return The minimum domain-specific column coordinate that {@link #this}
   * represents.
   */
  public int getMinimumDomainSpecificColumn(){
    return this._minimumDomainSpecificColumn;
  }
  
  /**
   * @return The minimum domain-specific row coordinate that {@link #this}
   * represents.
   */
  public int getMinimumDomainSpecificRow(){
    return this._minimumDomainSpecificRow;
  }
  
  /**
   * @return The name of this {@link #this}.
   */
  public String getName () {
    return _name;
  }
  
  public Integer getSceneSpecificColFromDomainSpecificCol(int col){
    return col >= this._minimumDomainSpecificColumn && col <= ((this._minimumDomainSpecificColumn + this._width) - 1) ? 
      col - this._minimumDomainSpecificColumn :
      null;
  }
  
  public Integer getSceneSpecificRowFromDomainSpecificRow(int row){
    return row >= this._minimumDomainSpecificRow && row <= (this._minimumDomainSpecificRow + this._height) - 1 ? 
      row - this._minimumDomainSpecificRow :
      null;
  }
   
  /**
   * @param col
   * @param row
   * @return The contents of the coordinate specified by {@code col} and {@code 
   * row} in this {@link #this} or {@code null} if the coordinate specified does
   * not exist.
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
   * @return The {@link jchrest.lib.SceneObject SceneObject} on the 
   * coordinate specified by {@code col} and {@code row} in this {@link #this} 
   * (including {@link jchrest.lib.SceneObject SceneObjects} representing blind 
   * and empty squares) as a {@link jchrest.lib.ListPattern} composed of a 
   * {@link jchrest.lib.ItemSquarePattern ItemSquarePatterns.
   * <p>
   * The "item" for the {@link jchrest.lib.ItemSquarePattern} will be the 
   * result of invoking {@link 
   * jchrest.domainSpecifics.SceneObject#getObjectType()} on the {@link 
   * jchrest.domainSpecifics.SceneObject} present on the coordinate specified in 
   * {@link #this}.
   * <p>
   * If the {@code col} and {@code row} specified are not represented in {@link 
   * #this}, an empty {@link jchrest.lib.ListPattern} is returned.
   */
  public ListPattern getSquareContentsAsListPattern (int col, int row) {
    ListPattern squareContentsAsListPattern = new ListPattern();
    
    if (row >= 0 && row < _height && col >= 0 && col < _width) {
      SceneObject squareContents = this.getSquareContents(col, row);
      squareContentsAsListPattern.add(new ItemSquarePattern(squareContents.getObjectType(), col, row));
    }
    
    return squareContentsAsListPattern;
  }
  
  /**
   * 
   * @return The {@link jchrest.architecture.VisualSpatialField} represented by
   * {@link #this} or {@code null} if {@link #this} does not represent a {@link 
   * jchrest.architecture.VisualSpatialField}.
   */
  public VisualSpatialField getVisualSpatialFieldRepresented(){
    return this._visualSpatialFieldRepresented;
  }

  /**
   * @return The number of columns in this {@link #this}
   */
  public int getWidth(){
    return _width;
  }
  
  /**
   * 
   * @return {@link java.lang.Boolean#TRUE} if {@link #this} consists entirely
   * of blind {@link jchrest.lib.SceneObject SceneObjects}, {@link 
   * java.lang.Boolean#FALSE} if not.
   */
  public boolean isEntirelyBlind(){
    for(int col = 0; col < this._width; col++){
      for(int row = 0; row < this._height; row++){
        if(!this.isSquareBlind(col, row)) return false;
      }
    }
    
    return true;
  }
  
  /**
   * @param col
   * @param row
   * @return True if the coordinate specified contains a {@link 
   * jchrest.lib.SceneObject} representing a blind square.
   */
  public Boolean isSquareBlind(int col, int row){
    return _scene.get(col).get(row).getObjectType().equals(Scene.BLIND_SQUARE_TOKEN);
  }

  /**
   * @param row
   * @param col
   * @return True if the coordinate specified contains a {@link 
   * jchrest.lib.SceneObject} representing an empty square.
   */
  public boolean isSquareEmpty (int col, int row) {
    return _scene.get(col).get(row).getObjectType().equals(Scene.EMPTY_SQUARE_TOKEN);
  }
 
  /**
   * 
   * @param scene
   * 
   * @return {@link java.lang.Boolean#TRUE} if {@link #this} represents the
   * <i>exact</i> same domain-specific coordinates as the {@code scene} passed
   * as a parameter.
   */
  public boolean sameDomainSpace(Scene scene){
    return 
      this._minimumDomainSpecificColumn == scene._minimumDomainSpecificColumn &&
      this._minimumDomainSpecificRow == scene._minimumDomainSpecificRow &&
      this.getWidth() == scene.getWidth() &&
      this.getHeight() == scene.getHeight()
    ;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 97 * hash + this._height;
    hash = 97 * hash + this._width;
    hash = 97 * hash + this._minimumDomainSpecificColumn;
    hash = 97 * hash + this._minimumDomainSpecificRow;
    hash = 97 * hash + Objects.hashCode(this._scene);
    return hash;
  }
}

