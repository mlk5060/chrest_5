package jchrest.architecture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.SceneObject;
import jchrest.lib.VisualSpatialFieldObject;
import jchrest.lib.Square;
import jchrest.lib.VisualSpatialFieldException;

/**
 * Implements a visual-spatial field, specifically one that handles 
 * <i>attention-based imagery</i> (see page 301 of "Image and Brain" by Stephen
 * Kosslyn).
 * <p>
 * Visual-spatial fields are 3D {@link java.util.List Lists} whose first two
 * dimensions represent the columns and rows of the visual-spatial field and 
 * whose third dimension maintains a history of {@link 
 * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} that have 
 * been placed on the coordinates denoted by the first and second dimension 
 * elements containing the third, i.e. [2][0][1] would return the second 
 * {@link jchrest.lib.VisualSpatialFieldObject} on coordinates (2, 0) in {@link 
 * #this}.  The visual-spatial field's size is finite after creation (consistent 
 * with the view proposed by Kosslyn on page 305 of "Image and Brain)".
 * <p>
 * Information in the visual-spatial field can be manipulated independently of 
 * the environment that the creator is currently situated in to test outcomes 
 * of actions without incurring these outcomes in "reality".
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
// TODO: After instantiation, the access time may decrease depending upon how
//       many times reality has been re-encoded (see page 307 of "Image and
//       Brain" by Kosslyn).
//  
// TODO: The size of the visual-spatial field may be finite before creation 
//       according to Kosslyn (proposed that reliable encoding of object 
//       locations occurs when the matrix is 3 x 3, anything larger causes 
//       object encoding to become unreliable and subject to error).
public class VisualSpatialField {
  
  private final ArrayList<ArrayList<TreeMap<Integer, ArrayList<VisualSpatialFieldObject>>>> _visualSpatialField = new ArrayList<>();
  private final int _creationTime;
  private final String _name;
  private final int _width;
  private final int _height;
  private final int _minDomainSpecificCol;
  private final int _minDomainSpecificRow;
  private final Chrest _associatedModel;
  
  /**
   * Constructor 
   * 
   * @param name
   * @param width
   * @param height
   * @param minDomainSpecificCol
   * @param minDomainSpecificRow 
   * @param associatedModel
   * @param creatorDetails A two element {@link java.util.List} that can be set
   * to {@code null} if the creator of {@link #this}, i.e. the agent equipped 
   * with the {@code associatedModel} specified, does not need to be encoded.  
   * Otherwise, if this is not {@code null}, the first element should be a 
   * {@link java.lang.String} specifying the identifier for the agent equipped 
   * with the {@code associatedModel} specified (see {@link 
   * jchrest.domainSpecifics.SceneObject#_identifier}) and whose second element
   * should be a {@link jchrest.lib.Square} specifying the location of the
   * agent equipped with the {@code associatedModel} specified in coordinates
   * specific to {@link #this}, i.e. zero-indexed.
   * @param time
   */
  public VisualSpatialField(
    String name, 
    int width, 
    int height, 
    int minDomainSpecificCol, 
    int minDomainSpecificRow, 
    Chrest associatedModel,
    List<Object> creatorDetails,
    int time
  ){
    if(associatedModel == null){
      throw new IllegalArgumentException(
        "The CHREST model that is to be associated with the VisualSpatialField " +
        "under construction can not be set to null"
      );
    }
    
    if(creatorDetails != null){
      Square creatorLocation = ((Square)creatorDetails.get(1));
      int creatorLocationCol = creatorLocation.getColumn();
      int creatorLocationRow = creatorLocation.getRow();
      if(
        creatorLocationCol < 0 || creatorLocationCol >= width ||
        creatorLocationRow < 0 || creatorLocationRow >= height
      ){
        throw new IllegalArgumentException(
          "Creator loctaion specified is outside the dimensions specified for " +
          "the VisualSpatialField to be consructed, i.e. column coordinate (" + 
          creatorLocationCol + ") is < 0 or greater than the maximum width " +
          "specified (" + width + ") or row coordinate (" + creatorLocationRow +
          ") is < 0 or greater than the maximum height specfied (" + height + ")."
        );
      }
    }
    
    this._creationTime = time;
    this._name = name;
    this._width = width;
    this._height = height;
    this._minDomainSpecificCol = minDomainSpecificCol;
    this._minDomainSpecificRow = minDomainSpecificRow;
    this._associatedModel = associatedModel;
    
    for(int col = 0; col < width; col++){
      this._visualSpatialField.add(new ArrayList());
      for(int row = 0; row < height; row++){
        ArrayList<VisualSpatialFieldObject> coordinateContents = new ArrayList();
        TreeMap<Integer, ArrayList<VisualSpatialFieldObject>> coordinateContentsHistory = new TreeMap();
        coordinateContentsHistory.put(time, coordinateContents);
        this._visualSpatialField.get(col).add(coordinateContentsHistory);
      }
    }
    
    if( creatorDetails != null){
      Square creatorLocation = (Square)creatorDetails.get(1);
      try {
        this.addObjectToCoordinates(
          creatorLocation.getColumn(),
          creatorLocation.getRow(),
          new VisualSpatialFieldObject(
            (String)creatorDetails.get(0),
            Scene.getCreatorToken(),
            this._associatedModel,
            this,
            time,
            false,
            false
          ),
          time
        );
      } catch (VisualSpatialFieldException ex) {
        Logger.getLogger(VisualSpatialField.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
  
  /**************************/
  /***** Simple Getters *****/
  /**************************/
  
  public Chrest getAssociatedModel(){
    return this._associatedModel;
  }
  
  public int getHeight(){
    return this._height;
  }
  
  public int getMinimumDomainSpecificCol(){
    return this._minDomainSpecificCol;
  }
  
  public int getMinimumDomainSpecificRow(){
    return this._minDomainSpecificRow;
  }
  
  public final String getName(){
    return this._name;
  }
  
  public int getWidth(){
    return this._width;
  }
  
  /**********************************/
  /***** Advanced Functionality *****/
  /**********************************/
  
  /**
   * Adds {@code object} to the {@code col} and {@code row} specified in {@link 
   * #this} if it doesn't already exist on this field.
   * <p>
   * If the coordinates specified represents an empty {@link jchrest.lib.Square}
   * at the {@code time} specified and the {@code object} specified represents 
   * a non-empty square, the {@code object} will occupy the coordinates and the
   * {@link jchrest.lib.VisualSpatialFieldObject} representing the empty {@link 
   * jchrest.lib.Square} will have its terminus set to {@code time}.  In other 
   * words, empty coordinates become non-empty, if applicable.
   * <p>
   * If the coordinates specified do not represent an empty {@link 
   * jchrest.lib.Square}, the {@code object} specified will co-exist with any 
   * other {@link jchrest.lib.VisualSpatialFieldObject 
   * VisualSpatialFieldObjects} that are alive on the coordinates at the 
   * {@code time} specified (see {@link 
   * jchrest.lib.VisualSpatialFieldObject#isAlive(int)}).  However, if the 
   * {@code object} specified represents an empty {@link jchrest.lib.Square}, 
   * all {@link jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} 
   * on the coordinates will have their terminus set to the {@code time} 
   * specified.
   * 
   * @param col
   * @param row
   * @param object
   * @param time 
   * 
   * @throws VisualSpatialFieldException If any of the following statements 
   * evaluate to {@link java.lang.Boolean#TRUE}:
   * <ol type="1">
   *  <li>
   *    The {@code object} to add represents a blind square (invoking {@link 
   *    jchrest.lib.VisualSpatialFieldObject#getObjectType()} returns the result
   *    of invoking {@link jchrest.domainSpecifics.Scene#getBlindSquareToken()}).
   *    This is because blind squares should be encoded as unknown squares in
   *    {@link #this}.
   *  </li>
   *  <li>
   *    The {@code col} and {@code row} specified are not represented in {@link 
   *    #this}.
   *  </li>
   *  <li>
   *    The {@code object} specified represents the agent equipped with {@link 
   *    jchrest.architecture.Chrest} but there is already a {@link 
   *    jchrest.lib.VisualSpatialFieldObject} that represents this agent on 
   *    {@link #this} at the {@code time} specified.
   *  </li>
   * </ol>
   */
  public final boolean addObjectToCoordinates(int col, int row, VisualSpatialFieldObject object, int time) throws VisualSpatialFieldException{
    if(!object.getObjectType().equals(Scene.getBlindSquareToken())){
      if(col >= 0 && col < this._width && row >= 0 && row < this._height){

        //////////////////////////////////////////////////////////////////
        ///// CHECK IF OBJECT ALREADY EXISTS ON VISUAL-SPATIAL FIELD /////
        //////////////////////////////////////////////////////////////////
        
        boolean objectAlreadyExists = false;
        for(int colToCheck = 0; colToCheck < this._width; colToCheck++){
          for(int rowToCheck = 0; rowToCheck < this._height; rowToCheck++){
            for(VisualSpatialFieldObject vsfo : this._visualSpatialField.get(colToCheck).get(rowToCheck).floorEntry(time).getValue()){
              if(vsfo.isAlive(time)){

                //Check if the creator is being added when another creator is 
                //still "alive".
                if(vsfo.getObjectType().equals(Scene.getCreatorToken()) && object.getObjectType().equals(Scene.getCreatorToken())){
                  throw new VisualSpatialFieldException(
                    "A VisualSpatialFieldObject representing the agent equipped " +
                    "with CHREST has been requested to be added to " + 
                    "VisualSpatialField coordinates (" + col + ", " + row + ") " +
                    "but such a VisualSpatialFieldObject currently exists on the " +
                    "same VisualSpatialField at coordinates (" + colToCheck + ", " +
                    rowToCheck + ").  VisualSpatialFieldObject to add details:" + 
                    object.toString() + "\nVisualSpatialFieldObject found details:" +
                    vsfo.toString()
                  );
                }

                //Check if a duplicate VisualSpatialFieldObject is being added.
                if(vsfo.getIdentifier().equals(object.getIdentifier())){
//                  throw new VisualSpatialFieldException(
//                    "A VisualSpatialFieldObject representing the " +
//                    "VisualSpatialFieldObject requested to be added to " + 
//                    "VisualSpatialField coordinates (" + col + ", " + row + ") " +
//                    "currently exists on the same VisualSpatialField at " +
//                    "coordinates (" + colToCheck + ", " + rowToCheck + ").  " +
//                    "VisualSpatialFieldObject to add details:" + object.toString() +
//                    "\nVisualSpatialFieldObject found details:" + vsfo.toString()
//                  );
                  objectAlreadyExists = true;
                }
              }
            }
          }
        }
        
        //////////////////////////////////////////////////////
        ///// ADD NEW OBJECT AND HANDLE EXISTING OBJECTS /////
        //////////////////////////////////////////////////////

        if(!objectAlreadyExists){
          
          //Overwrite empty squares or non-empty squares if the 
          //VisualSpatialFieldObject being added is a non-empty square or an empty
          //square, respectively.
          ArrayList<VisualSpatialFieldObject> currentCoordinateContents = this._visualSpatialField.get(col).get(row).floorEntry(time).getValue();
          for(VisualSpatialFieldObject vsfo : currentCoordinateContents){
            if(
              (vsfo.getObjectType().equals(Scene.getEmptySquareToken()) && vsfo.isAlive(time)) ||
              (object.getObjectType().equals(Scene.getEmptySquareToken()) && vsfo.isAlive(time))
            ){
              vsfo.setTerminus(time, true);
            }
          }

          ArrayList<VisualSpatialFieldObject> newCoordinateContents = new ArrayList();
          newCoordinateContents.addAll(currentCoordinateContents);
          newCoordinateContents.add(object);
          this._visualSpatialField.get(col).get(row).put(time, newCoordinateContents);
          return true;
        }
        
        return false;
      }
      else{
        throw new IllegalArgumentException(
          "The column (" + col + ") or row (" + row +") to add the " +
          "VisualSpatialFieldObject specified to is < 0 or >= the width (" + 
          this._width + ") or height " + "(" + this._height + ") of the " + 
          "VisualSpatialField to add it to.\nVisualSpatialField name: " + 
          this._name + "\nVisualSpatialFieldObject to add details:" + 
          object.toString()
        );
      }
    }
    else{
      throw new IllegalArgumentException(
        "The VisualSpatialFieldObject to add to the VisualSpatialField with " +
        "name: '" + this._name + "' represents a blind square and such " +
        "VisualSpatialFieldObjects should not be encoded.  " +
        "\nVisualSpatialFieldObject to add details:" + object.toString()
      );
    }
  }
  
  /**
   * @param time
   * 
   * @param unknownProbabilities If a coordinate on {@link #this} does not 
   * contain any {@link jchrest.lib.VisualSpatialFieldObject 
   * VisualSpatialFieldObjects} that are alive at the {@code time} specified
   * (see {@link jchrest.lib.VisualSpatialFieldObject#isAlive(int)}), the {@link 
   * java.util.TreeMap} passed here will be used to determine what {@link 
   * jchrest.domainSpecifics.SceneObject} will be placed on the corresponding 
   * coordinates in the {@link jchrest.domainSpecifics.Scene} generated.  
   * <p>
   * Keys <b>must</b> be between 0.0 and 1.0 (inclusive) and should always 
   * contain a key for 1.0. Values should be {@link
   * jchrest.domainSpecifics.SceneObject} types; the {@link
   * jchrest.domainSpecifics.SceneObject} constructed will have a randomly 
   * generated identifier (see {@link 
   * jchrest.domainSpecifics.SceneObject#SceneObject(java.lang.String)}). So, 
   * an entry of [0.5, "A"] means that, if a coordinate in {@link #this} has no
   * {@link jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} 
   * alive on it at the {@code time} specified, there is a 50% chance that a 
   * {@link jchrest.domainSpecifics.SceneObject} with object type "A" will be 
   * encoded on such coordinates in the {@link jchrest.domainSpecifics.Scene}
   * returned.
   * 
   * @return {@link #this} as a {@link jchrest.domainSpecifics.Scene} 
   * containing {@link jchrest.domainSpecifics.SceneObject} representations of 
   * {@link jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} that 
   * are alive on {@link #this} at the {@code time} specified.
   * <p>
   * If a coordinate is occupied by more than one {@link 
   * jchrest.lib.VisualSpatialFieldObject} that is alive at the {@code time} 
   * specified, the one created most recently will be encoded as a {@link 
   * jchrest.domainSpecifics.SceneObject}.
   */
  //TODO: should this incur an attentional time cost?
  //TODO: should this incur an access time cost?
  public Scene getAsScene(int time, TreeMap<Double, String> unknownProbabilities){
    
    for(Double probability : unknownProbabilities.keySet()){
      if(probability > 1.0){
        throw new IllegalArgumentException(
          "A probability specified in the data structure defining what " +
          "SceneObject types may be used if a VisualSpatialField coordinate " +
          "is empty when constructing a Scene from the current state of the " +
          "VisualSpatialField is greater than 1.0."
        );
      }
    }
    
    if(unknownProbabilities.containsKey(1.0)){
      if(!unknownProbabilities.containsValue(VisualSpatialFieldObject.getUnknownSquareToken())){
      
        //Create a new Scene instance based on the current dimensions of this
        //visual-spatial field.
        Scene visualSpatialFieldScene = new Scene(
          "Visual-spatial-field @ time " + time, 
          this.getWidth(), 
          this.getHeight(),
          this._minDomainSpecificCol,
          this._minDomainSpecificRow,
          this
        );

        for(int row = 0; row < this.getHeight(); row++){
          for(int col = 0; col < this.getWidth(); col++){

            ArrayList<VisualSpatialFieldObject> coordinateContents = new ArrayList();
            for(VisualSpatialFieldObject object : this.getCoordinateContents(col, row, time, false)){
              if(object.isAlive(time)){
                coordinateContents.add(object);
              }
            }

            if(coordinateContents.isEmpty()){
              Random random = new Random();
              visualSpatialFieldScene.addObjectToSquare(
                col, 
                row, 
                new SceneObject(unknownProbabilities.ceilingEntry(random.nextDouble()).getValue())
              );
            }
            else{
              VisualSpatialFieldObject mostRecentObjectOnCoordinates = coordinateContents.get(coordinateContents.size() - 1);
              visualSpatialFieldScene.addObjectToSquare(
                col, 
                row, 
                new SceneObject(mostRecentObjectOnCoordinates.getIdentifier(), mostRecentObjectOnCoordinates.getObjectType())
              );
            }
          }
        }

        return visualSpatialFieldScene;      
      }
      else{
        throw new IllegalArgumentException(
          "The data structure defining what SceneObject types may be used if a " +
          "VisualSpatialField coordinate is empty when constructing a Scene from " +
          "the current state of the VisualSpatialField contains the unknown " +
          "VisualSpatialFieldObject status token.  This is not permitted since " +
          "there is no equivalent SceneObject."
        );
      }
    }
    else{
      throw new IllegalArgumentException(
        "The data structure defining what SceneObject types may be used if a " +
        "VisualSpatialField coordinate is empty when constructing a Scene from " +
        "the current state of the VisualSpatialField does not contain the key " +
        "1.0.  This means that, if the maximum probability specified is 0.5 and " +
        "0.9 is generated randomly, null will be returned using this data " +
        "structure and this is not permitted; some value MUST be returned."
      );
    }
  }
  
  /**
   * @param col
   * @param row
   * @param time
   * @param returnUnknownIfEmpty If no {@link 
   * jchrest.lib.VisualSpatialFieldObject VisualSpatialFieldObjects} on the 
   * coordinates specified by {@code col} and {@code row} are alive at the 
   * {@code time} specified, an empty {@link java.util.List} will be returned if 
   * this parameter is set to {@link java.lang.Boolean#FALSE}.  However, if this 
   * parameter is set to {@link java.lang.Boolean#TRUE} a {@link 
   * jchrest.lib.VisualSpatialFieldObject} representing an unknown coordinate 
   * status is returned.
   * 
   * @return All {@link jchrest.lib.VisualSpatialFieldObject 
   * VisualSpatialFieldObjects} on the coordinates specified by {@code col} and
   * {@code row} that are "alive" at the {@code time} specified (see {@link 
   * jchrest.lib.VisualSpatialFieldObject#isAlive(int)}.  
   * <p>
   * If the {@code col} and {@code row} specified are represented by {@link 
   * #this}, {@code null} is returned.
   * <p>
   * <b>NOTE</b>: Modifying the {@link java.util.List} returned will not modify
   * the actual contents of the coordinates in {@link #this}.
   */
  public List<VisualSpatialFieldObject> getCoordinateContents(int col, int row, int time, boolean returnUnknownIfEmpty){
    ArrayList<VisualSpatialFieldObject> coordinateContents = null;
    
    if(col >= 0 && col < this.getWidth() && row >= 0 && row < this.getHeight()){
      coordinateContents = new ArrayList<>();
      ArrayList<VisualSpatialFieldObject> contents = this._visualSpatialField.get(col).get(row).floorEntry(time).getValue();
      for(VisualSpatialFieldObject object : contents){
        if(object.isAlive(time)){
          coordinateContents.add(object);
        }
      }

      if(coordinateContents.isEmpty() && returnUnknownIfEmpty){
        coordinateContents.add(new VisualSpatialFieldObject(
          VisualSpatialFieldObject.getUnknownSquareToken(),
          VisualSpatialFieldObject.getUnknownSquareToken(),
          this._associatedModel,
          this,
          (contents.isEmpty() || contents.get(contents.size() - 1).getTerminus() == null ? 
            this._creationTime : 
            contents.get(contents.size() - 1).getTerminus()
          ),
          false,
          false
        ));
      }
    }
    
    return coordinateContents;
  }
  
  /**
   * @param col
   * @param row
   * @return All {@link jchrest.lib.VisualSpatialFieldObject 
   * VisualSpatialFieldObjects} on the {@code col} and {@code row} specified.
   * <p>
   * <b>NOTE</b>: Modifying the {@link java.util.List} returned will not modify
   * the actual contents of the coordinates in {@link #this}.
   */
  List<VisualSpatialFieldObject> getCoordinateContents(int col, int row){
    ArrayList<VisualSpatialFieldObject> coordinateContents = null;
    
    if(col >= 0 && col < this.getWidth() && row >= 0 && row < this.getHeight()){
      coordinateContents = new ArrayList<>();
      for(Map.Entry<Integer, ArrayList<VisualSpatialFieldObject>> coordinateContentsEntry : this._visualSpatialField.get(col).get(row).entrySet()){
        coordinateContents.addAll(coordinateContentsEntry.getValue());
      }
    }
    
    return coordinateContents;
  }
  
  /**
   * @param time The time at which to search {@link #this} for a {@link 
   * jchrest.lib.VisualSpatialFieldObject} representing the agent equipped with
   * the {@link jchrest.architecture.Chrest} model associated with {@link #this}
   * (the creator).
   * 
   * @return Either {@code null} if the creator is not present on {@link #this} 
   * at the {@code time} specified or, if it is, a two element {@link 
   * java.util.List} whose first element is the result of invoking {@link 
   * jchrest.domainSpecifics.VisualSpatialFieldObject#getIdentifier()} on the
   * {@link jchrest.domainSpecifics.VisualSpatialFieldObject} that represents 
   * the creator and whose second element is a {@link jchrest.lib.Square} 
   * specifying the location of the creator in coordinates specific to {@link 
   * #this}.
   */
  public List<Object> getCreatorDetails(int time){
    List<Object> creatorDetails = null;
    for(int col = 0; col < this.getWidth(); col++){
      for(int row = 0; row < this.getHeight(); row++){
        List<VisualSpatialFieldObject> coordinateContents = this._visualSpatialField.get(col).get(row).floorEntry(time).getValue();
        for(VisualSpatialFieldObject visualSpatialFieldObject : coordinateContents){
          if(
            visualSpatialFieldObject.getObjectType().equals(Scene.getCreatorToken()) &&
            visualSpatialFieldObject.isAlive(time)
          ){
            creatorDetails = new ArrayList();
            creatorDetails.add(visualSpatialFieldObject.getIdentifier());
            creatorDetails.add(new Square(col, row));
            return creatorDetails;
          }
        }
      }
    }
    
    return creatorDetails;
  }
  
  /**
   * 
   * @param col
   * @param row
   * @return 
   */
  public final boolean areDomainSpecificCoordinatesRepresented(int col, int row){
    return col >= this._minDomainSpecificCol && 
      col < (this._minDomainSpecificCol + this._width) &&
      row >= this._minDomainSpecificRow && 
      row < (this._minDomainSpecificRow + this._height)
    ;
  }
  
  /**
   * 
   * @param col
   * @return The domain-specific column coordinate represented by the 
   * visual-spatial field coordinate specified ({@code col}) or {@code null} if 
   * {@code col} is not represented by {@link #this}.
   */
  public final int getDomainSpecificColFromVisualSpatialFieldCol(int col){
    if(col >= 0 && col < this._width){
      return this._minDomainSpecificCol + col;
    }
    else{
      throw new IllegalArgumentException(
        "The column specified (" + col + ") is either < 0 or is greater than " +
        "the maximum width of the visual-spatial field (" + this._width + ")"
      );
    }
  }
  
  /**
   * 
   * @param row
   * @return The domain-specific column coordinate represented by the 
   * visual-spatial field coordinate specified ({@code row}) or {@code null} if 
   * {@code row} is not represented by {@link #this}.
   */
  public final int getDomainSpecificRowFromVisualSpatialFieldRow(int row){
    if(row >= 0 && row < this._height){
      return this._minDomainSpecificRow + row;
    }
    else{
      throw new IllegalArgumentException(
        "The row specified (" + row + ") is either < 0 or is greater than " +
        "the maximum height of the visual-spatial field (" + this._height + ")"
      );
    }
  }
  
  /**
   * 
   * @param col
   * @return The visual-spatial field column coordinate represented by the 
   * domain-specific coordinate specified ({@code col}) or {@code null} if 
   * {@code col} is not represented by {@link #this}.
   */
  public final int getVisualSpatialFieldColFromDomainSpecificCol(int col){
    if(col >= this._minDomainSpecificCol && col < (this._minDomainSpecificCol + this._width)){ 
      return col - this._minDomainSpecificCol;
    }
    else{
      throw new IllegalArgumentException(
        "The column specified (" + col + ") is either < the minimum domain-specific " +
        "column represented in the visual-spatial field (" + this._minDomainSpecificCol +
        ") or is greater than the maximum domain-specific col represented in the " +
        "visual-spatial field (" + (this._minDomainSpecificCol + this._width + ")")
      );
    }
  }
  
  /**
   * 
   * @param row
   * @return The visual-spatial field row coordinate represented by the 
   * domain-specific coordinate specified ({@code row}) or {@code null} if 
   * {@code row} is not represented by {@link #this}.
   */
  public final int getVisualSpatialFieldRowFromDomainSpecificRow(int row){
    if(row >= this._minDomainSpecificRow && row < (this._minDomainSpecificRow + this._height)){
      return row - this._minDomainSpecificRow;
    }
    else{
      throw new IllegalArgumentException(
        "The row specified (" + row + ") is either < the minimum domain-specific " +
        "row represented in the visual-spatial field (" + this._minDomainSpecificRow +
        ") or is greater than the maximum domain-specific row represented in the " +
        "visual-spatial field (" + (this._minDomainSpecificRow + this._height) + ")"
      );
    }
  }
}
