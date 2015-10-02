package jchrest.architecture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.VisualSpatialFieldException;
import jchrest.lib.VisualSpatialFieldObject;
import jchrest.lib.PrimitivePattern;
import jchrest.lib.Scene;
import jchrest.lib.SceneObject;
import jchrest.lib.Square;

/**
 * Class that implements a visual-spatial field, specifically one that handles
 * <i>attention-based imagery</i> (see page 301 of "Image and Brain" by Stephen
 * Kosslyn).
 * 
 * The visual-spatial field's coordinates maintain a history of objects that 
 * have been placed on them.  The visual-spatial field is a 2D ArrayList whose 
 * size is finite after creation (consistent with the view proposed by Kosslyn 
 * on page 305 of "Image and Brain)".
 * 
 * Information in the visual-spatial field can be manipulated independently of 
 * the environment that the observer is currently situated in to test outcomes 
 * of actions without incurring these outcomes in "reality".
 * 
 * TODO: After instantiation, the access time may decrease depending upon how
 *       many times reality has been re-encoded (see page 307 of "Image and
 *       Brain" by Kosslyn).
 * 
 * TODO: The size of the visual-spatial field may be finite before creation 
 *       according to Kosslyn (proposed that reliable encoding of object 
 *       locations occurs when the matrix is 3 x 3, anything larger causes 
 *       object encoding to become unreliable and subject to error).
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class VisualSpatialField {
  
  //The CHREST model that this instance is associated with.
  private final Chrest _model;
  
  //The Scene instance that was used to construct this visual-spatial field.
  private final Scene _sceneEncoded;
  
  //The actual visual-spatial field.  First dimension elements represent the 
  //columns (x-coordinates) of the scene encoded, second dimension elements 
  //represent the rows (y-coordinates) of the scene encoded, third dimension 
  //elements represent the objects that have existed or currently exist on the 
  //square indicated by the column and row.
  private ArrayList<ArrayList<ArrayList<VisualSpatialFieldObject>>> _visualSpatialField = new ArrayList<>();
  
  //Time taken (ms) to access the visual-spatial field.
  private final int _accessTime;
  
  //The time taken (in milliseconds) to place an object on the visual-spatial
  //field.
  private final int _objectPlacementTime;
  
  //The time taken (in milliseconds) to move an object on the visual-spatial
  //field.
  private final int _objectMovementTime;
  
  //The length of time (in milliseconds) that an object will exist on the 
  //visual-spatial field for if it is recognised after being "looked-at".
  private final int _lifespanForRecognisedObjects;
  
  //The length of time (in milliseconds) that an object will exist on the 
  //visual-spatial field for if it is not recognised after being "looked-at".
  private final int _lifespanForUnrecognisedObjects;
  
  //The last identifier used for a ghost object; used in the 
  //"assignGhostObjectId" method in this class.  Since it is only an object's 
  //class and location that is recorded in LTM, it is not possible to specify 
  //unique identifiers for "ghost" items (items recognised when constructing the 
  //visual-spatial field but aren't present in the secne encoded as a 
  //SceneObject).  This allows for ghost objects to be uniquely identified 
  //automatically.
  private int _ghostObjectIdentifier = 0;
  
  //The height and width of the visual-spatial field.
  private int _height = 0;
  private int _width = 0;
  
  /**
   * The constructor creates {@link jchrest.lib.VisualSpatialFieldObject}s that 
   * correspond to {@link jchrest.lib.SceneObject}s in the 
   * {@link jchrest.lib.Scene} to encode that is passed as a parameter.  If the 
   * <i>encodeGhostObjects</i> parameter is set to true, <i>ghost</i> objects 
   * (objects that have no {@link jchrest.lib.SceneObject} representation in the 
   * {@link jchrest.lib.Scene} to encode but are present in chunks retrieved 
   * after the {@link jchrest.lib.Scene} to encode is scanned using standard 
   * perceptual mechanisms, see 
   * {@link jchrest.architecture.Chrest#scanScene(jchrest.lib.Scene, int, int)}) 
   * are also encoded as {@link jchrest.lib.VisualSpatialFieldObject}s.  
   * Encoding {@link jchrest.lib.VisualSpatialFieldObject} representations of 
   * recognised/unrecognised objects and empty squares incurs attentional time 
   * costs; specified by the <i>objectEncodingTime</i> and 
   * <i>emptySquareEncodingTime</i> parameters (provided these are &062= 1).
   * Encoding blind objects or the location of the scene creator does not incur 
   * any attentional time costs.
   * 
   * Most {@link jchrest.lib.VisualSpatialFieldObject}s encoded will have 
   * terminus values set when the instance is created with the exceptions of 
   * {@link jchrest.lib.VisualSpatialFieldObject}s that represent the creator of 
   * the scene being encoded and blind squares.
   * 
   * The constructor ensures that only one 
   * {@link jchrest.lib.VisualSpatialFieldObject} will be seen to occupy a 
   * single visual-spatial field coordinate at any time.  Note that, in 
   * actuality and for the purposes of visualising CHREST's operation history, 
   * there may be more than one {@link jchrest.lib.VisualSpatialFieldObject} per 
   * coordinate in the visual-spatial field.  However, the creation and terminus 
   * times of {@link jchrest.lib.VisualSpatialFieldObject}s are set so that 
   * CHREST only considers there to be one 
   * {@link jchrest.lib.VisualSpatialFieldObject} on the coordinates in question.
   * 
   * The constructor proceeds as follows:
   * <ol type="1">
   *  <li>
   *    The visual-spatial field is instantiated as a completely blind field 
   *    whose width and height is equal to those of the 
   *    {@link jchrest.lib.Scene} to encode.  If the {@link jchrest.lib.Scene} 
   *    to encode isn't entirely blind then visual-spatial field construction 
   *    will continue and an attentional time cost specified by the 
   *    <i>accessTime</i> parameter will be incurred.
   *  </li>
   *  <li>
   *    If the creator of the {@link jchrest.lib.Scene} to encode is present in
   *    the {@link jchrest.lib.Scene}, its 
   *    {@link jchrest.lib.VisualSpatialFieldObject} representation is encoded 
   *    in the visual-spatial field without incurring any attentional time 
   *    costs.  Note also that the terminus of the creator's avatar isn't set 
   *    since it should never decay in the visual-spatial field.
   *  </li>
   *  <li>
   *    The {@link jchrest.lib.Scene} to encode is then scanned using standard 
   *    perceptual methods (as described above) and recognised 
   *    {@link jchrest.lib.SceneObject}s have equivalent 
   *    {@link jchrest.lib.VisualSpatialFieldObject}s created.  These
   *    {@link jchrest.lib.VisualSpatialFieldObject}s are encoded in the order 
   *    of which they were recognised (first -> last), e.g. if an object, A, 
   *    occurs in a chunk that was recognised before a chunk containing another 
   *    object, B, A's {@link jchrest.lib.VisualSpatialFieldObject} 
   *    representation will be encoded before B's.  So, if the 
   *    <i>objectEncodingTime</i> parameter is &062= 1, the creation and 
   *    terminus values for A's {@link jchrest.lib.VisualSpatialFieldObject} 
   *    representation will be &060 B's.  Encoding all objects in a chunk incurs 
   *    an attentional time cost of the value specified by the 
   *    <i>objectEncodingTime</i>.  So, if two chunks are recognised, 
   *    irrespective of the number of objects identified in these chunks, the 
   *    attentional cost incurred will be equal to <i>objectEncodingTime</i> * 
   *    2.  The terminus of a recognised object will be set to the time that its 
   *    containing chunk is encoded plus the value specified for the
   *    <i>lifespanForRecognisedObjects</i> parameter.
   *  </li>
   *  <li>
   *    If the <i>encodeGhostObjects</i> parameter is set to true, ghost objects
   *    will be encoded along with real recognised objects.  There are a number 
   *    of caveats to be aware of if ghost objects are to be encoded, these are 
   *    discussed below.
   *  </li>
   *  <li>
   *    Finally, empty squares and unrecognised objects from the 
   *    {@link jchrest.lib.Scene} to encode are encoded as 
   *    {@link jchrest.lib.VisualSpatialFieldObject}s in order of their
   *    location in the {@link jchrest.lib.Scene} to encode from east -> west 
   *    then south-> north, e.g. the most south-easterly unrecognised object or 
   *    empty square is encoded first and the most north-westerly unrecognised 
   *    object or empty square is encoded last.  Encoding an empty square incurs 
   *    an attentional time cost of the value specified by the 
   *    <i>emptySquareEncodingTime</i> parameter whereas encoding an 
   *    unrecognised object incurs an attentional time cost of the value 
   *    specified by the <i>objectEncodingTime</i> parameter.  So, if there are
   *    two empty squares and two unrecognised objects present in the 
   *    {@link jchrest.lib.Scene} to encode, the attentional time cost incurred
   *    would be equal to (<i>emptySquareEncodingTime</i> * 2) + 
   *    (<i>objectEncodingTime</i> * 2).  The terminus for unrecognised objects
   *    (both empty squares and actual objects) is set to the time that the
   *    object is encoded plus the value specified for the 
   *    <i>lifespanForUnrecognisedObjects</i> parameter.
   *  <li>
   * </ol>
   * 
   * As mentioned above, if the <i>encodeGhostObjects</i> parameter is set to 
   * true, there is a strict preference ordering to be aware of when 
   * {@link jchrest.lib.VisualSpatialFieldObject}s are encoded: real objects 
   * (empty squares, actual objects and the scene creator) &062 ghost objects 
   * &062 blind squares.  So, there are a number of scenarios that need to be 
   * taken into consideration when understanding why a visual-spatial field is 
   * constructed in the way it is when ghost objects can be encoded. These
   * scenarios are delineated below along with their outcomes:
   * 
   *  <table>
   *    <tr><th>Scenario</th><th>Example</th><th>Outcome</th><tr/>
   *    <tr>
   *      <td>
   *        Same non-empty, real object occurs in different recognised chunks on
   *        same coordinates.
   *      <td>
   *      <td>
   *        &060[A, 1, 2][B, 1, 3]&062&060[D, 2, 2][A, 1, 2]&062 (object A is of
   *        interest here)
   *      </td>
   *      <td>
   *        The creation time of the object is set to the time when the 
   *        recognised chunk that the object first occurs in is encoded.  If the 
   *        object's original terminus has not been reached when the next chunk 
   *        it occurs in is encoded, the object's terminus is <i>refreshed</i>, 
   *        i.e. it is set to the time the second chunk is encoded plus the 
   *        value specified for the <i>lifespanForRecognisedObjects</i> 
   *        parameter.  Otherwise, the object will be recreated.
   *      </td>
   *    </tr>
   *    <tr>
   *      <td>
   *        A non-empty, real object and a ghost object are located on the same 
   *        coordinates but the real object occurs in a chunk that is recognised 
   *        before the chunk containing the ghost object.
   *      </td>
   *      <td>
   *        &060[A, 1, 2][b, 1, 3]&062&060[D, 2, 2][c, 1, 2]&062 (objects A and 
   *        c are of interest here)
   *      </td>
   *      <td>
   *        If the real object's terminus has not been reached when the chunk 
   *        containing the ghost object is encoded, the ghost object is not 
   *        encoded and the real object's terminus is <i>refreshed</i>, i.e. it
   *        is set to the time the chunk containing the ghost object is encoded 
   *        plus the value specified for the <i>lifespanForRecognisedObjects</i> 
   *        parameter.  This occurs because attention has been focused on the 
   *        coordinates occupied by the real object.  Otherwise, if the real 
   *        object's original terminus has been reached when the chunk 
   *        containing the ghost object is encoded, the ghost object will be 
   *        encoded.
   *      </td>
   *    </tr>
   *    <tr>
   *      <td>
   *        A non-empty, real object and a ghost object are located on the same 
   *        coordinates but the ghost object occurs in a chunk that is 
   *        recognised before the chunk containing the real object.
   *      </td>
   *      <td>
   *        &060[A, 1, 2][b, 1, 3]&062&060[B, 3, 2][D, 1, 3]&062 (objects b and 
   *        D are of interest here)
   *      </td>
   *      <td>
   *        The real object will always overwrite the ghost object.
   *      </td>
   *    </tr>
   *    <tr>
   *      <td>
   *        Same ghost object occurs in different recognised chunks on same 
   *        coordinates.
   *      </td>
   *      <td>
   *        &060[A, 1, 2][b, 1, 3]&062&060[D, 2, 2][b, 1, 3]&062 (object b is of 
   *        interest here)
   *      </td>
   *      <td>
   *        The creation time of the object is set to the time when the 
   *        recognised chunk that the object first occurs in is encoded.  If the 
   *        object's original terminus has not been reached when the next chunk 
   *        it occurs in is processed, the object's terminus is <i>refreshed</i>, 
   *        i.e. it is set to the time the second chunk is encoded plus the 
   *        value specified for the <i>lifespanForRecognisedObjects</i> 
   *        parameter.  Otherwise, the object will be recreated.
   *      </td>
   *    </tr>
   *    <tr>
   *      <td>
   *        Two distinct ghost objects in different chunks are located on the 
   *        same coordinates.
   *      </td>
   *      <td>
   *        &060[A, 1, 2][b, 1, 3]&062&060[D, 2, 2][c, 1, 3]&062 (objects b and 
   *        c are of interest here)
   *      </td>
   *      <td>
   *        The most recently recognised ghost object overwrites the ghost 
   *        object recognised earlier.
   *      </td>
   *    </tr>
   *    <tr>
   *      <td>
   *        A ghost object and blind square occupy the same coordinates.
   *      </td>
   *      <td>
   *      </td>
   *      <td>
   *        The ghost object overwrites the blind square.
   *      </td>
   *    </tr>
   *    <tr>
   *      <td>
   *        A ghost object and an empty square occupy the same coordinates.
   *      </td>
   *      <td>
   *      </td>
   *      <td>
   *        The empty square overwrites the ghost object (empty squares are
   *        encoded after recognised objects).
   *      </td>
   *    </tr>
   *    <tr>
   *      <td>
   *        A ghost object and an unrecognised real object occupy the same
   *        coordinates.
   *      </td>
   *      <td>
   *      </td>
   *      <td>
   *        The real object overwrites the ghost object (unrecognised real 
   *        objects are encoded after recognised objects).
   *      </td>
   *    </tr>
   *    <tr>
   *      <td>
   *        A ghost object occupies the same coordinates as the scene creator.
   *      </td>
   *      <td>
   *      </td>
   *      <td>
   *        The ghost object is not encoded.
   *      </td>
   *    </tr>
   * </table>
   * 
   * @param model The {@link jchrest.architecture.Chrest} model that this 
   * {@link #this} is associated with.
   * 
   * @param sceneToEncode The {@link jchrest.lib.Scene} instance representing 
   * that is to be encoded into the visual-spatial field.
   * 
   * @param objectEncodingTime The length of time (in milliseconds) that it 
   * takes to encode a {@link jchrest.lib.VisualSpatialFieldObject} representing
   * the result of invoking {@link jchrest.architecture.Node#getImage()} on a 
   * {@link jchrest.architecture.Node} that is retrieved from visual long-term 
   * memory after scanning the {@link jchrest.lib.Scene} to encode or an 
   * unrecognised {@link jchrest.lib.SceneObject}.
   * 
   * @param emptySquareEncodingTime The length of time (in milliseconds) that it 
   * takes to encode a {@link jchrest.lib.VisualSpatialFieldObject} representing
   * an empty square.
   * 
   * @param accessTime The time taken (in milliseconds) to access this 
   * {@link #this} at any time.
   * 
   * @param objectMovementTime The time taken (in milliseconds) to move a
   * {@link jchrest.lib.VisualSpatialFieldObject} in this {@link #this}.
   * 
   * @param lifespanForRecognisedObjects The length of time (in milliseconds) 
   * that a recognised {@link jchrest.lib.VisualSpatialFieldObject} will exist 
   * for in this {@link #this} after attention has been focused on it.
   * 
   * @param lifespanForUnrecognisedObjects The length of time (in milliseconds) 
   * that an unrecognised {@link jchrest.lib.VisualSpatialFieldObject} will 
   * exist for in this {@link #this} after attention has been focused on it.
   * 
   * @param numberFixations The number of fixations that should be used when 
   * scanning the {@link jchrest.lib.Scene} to encode.
   * 
   * @param domainTime The time (in milliseconds) in the domain where 
   * the {@link jchrest.architecture.Chrest} model associated with this 
   * {@link #this} is located when this constructor is invoked.
   * 
   * @param encodeGhostObjects Set to true to encode ghost objects during 
   * construction of this {@link #this}.
   * 
   * @param debug Set to true to output debug messages to 
   * {@link java.lang.System#out}.
   * 
   * @throws jchrest.lib.VisualSpatialFieldException Thrown if two 
   * {@link jchrest.lib.SceneObject}s in the {@link jchrest.lib.Scene} to 
   * encode have the same identifier.
   */
  public VisualSpatialField(
    Chrest model, 
    Scene sceneToEncode, 
    int objectEncodingTime, 
    int emptySquareEncodingTime, 
    int accessTime, 
    int objectMovementTime, 
    int lifespanForRecognisedObjects, 
    int lifespanForUnrecognisedObjects, 
    int numberFixations, 
    int domainTime,
    boolean encodeGhostObjects,
    boolean debug
  ) throws VisualSpatialFieldException{   
    
    if (debug) System.out.println("=== VisualSpatialField Constructor ===");
    
    this._model = model;
    this._sceneEncoded = sceneToEncode;
    this._accessTime = accessTime;
    this._objectMovementTime = objectMovementTime;
    this._objectPlacementTime = objectEncodingTime;
    this._lifespanForRecognisedObjects = lifespanForRecognisedObjects;
    this._lifespanForUnrecognisedObjects = lifespanForUnrecognisedObjects;
    
    
    //Set a local "time" variable to be equal to the time that the constructor 
    //was called in the domain.  This will be used throughout visual-spatial 
    //field construction.
    int time = domainTime;
    if(debug) System.out.println("- Construction began at time " + time);
    
    /******************************************/
    /***** CHECK FOR ENTIRELY BLIND SCENE *****/
    /******************************************/
    
    //If the scene to encode is entirely blind, the constructor will hang when 
    //the scene to encode is scanned for recognised chunks below so this check 
    //prevents this from happening.
    if(debug) System.out.println("- Checking if the scene to encode is entirely blind...");
    
    //Create a boolean variable that is only set to true if a non-blind object
    //exists in the scene to encode.
    boolean realityIsBlind = true;
    
    //Check sceneToEncode for a non-blind object that is not the scene creator.
    for(int col = 0; col < this._sceneEncoded.getWidth() && realityIsBlind; col++){
      for(int row = 0; row < this._sceneEncoded.getHeight() && realityIsBlind; row++){
        String objectClass = this._sceneEncoded.getSquareContents(col, row).getObjectClass();
        if(
          !objectClass.equals(Scene.getBlindSquareIdentifier()) &&
          !objectClass.equals(Scene.getCreatorToken())
        ){
          if(debug) System.out.println("   - Col " + col + ", row " + row + " contains an object with class " + objectClass + ".");
          realityIsBlind = false;
          break;
        }
      }
    }
    
    if(!realityIsBlind){
      if(debug) if(debug) System.out.println("- Scene to encode isn't entirely blind");
      
      this._height = sceneToEncode.getHeight();
      this._width = sceneToEncode.getWidth();
      if(debug) System.out.println("- Height and width of visual-spatial field set to "
        + this._height + " and " + this._width + "respectively");
      
      time += this._accessTime;
      if(debug) System.out.println("- Visual-spatial field accessed at time " + time);
    
      /********************************************/
      /***** INSTANTIATE VISUAL-SPATIAL FIELD *****/
      /********************************************/
      if(debug) System.out.println("- Instantiating visual-spatial field as a blind 'canvas'");
      this._visualSpatialField = new ArrayList<>();
      for(int col = 0; col < this._width; col++){
        this._visualSpatialField.add(new ArrayList<>());
        
        for(int row = 0; row < this._height; row++){
          this._visualSpatialField.get(col).add(new ArrayList<>());

          this._visualSpatialField.get(col).get(row).add(new VisualSpatialFieldObject(
            this, 
            Scene.getBlindSquareIdentifier(), 
            Scene.getBlindSquareIdentifier(), 
            time,
            false,
            false
          ));
        }
      }
      
      //Encode the scene creator, if present in sceneToEncode.
      if(debug) System.out.println("- Checking for scene creator in scene to encode...");
      Square creatorLocation = sceneToEncode.getLocationOfCreator();
      if(creatorLocation != null){
        
        if(debug) System.out.println("   - Scene creator is present in scene to encode at coordinates " + creatorLocation.toString());
        if(debug) System.out.println("   - Encoding scene creator at same location in visual-spatial field");
        
        ArrayList<VisualSpatialFieldObject> creatorLocationInVisualSpatialField = this._visualSpatialField.get(creatorLocation.getColumn()).get(creatorLocation.getRow());
        creatorLocationInVisualSpatialField.clear();
        creatorLocationInVisualSpatialField.add(new VisualSpatialFieldObject(
          this,
          sceneToEncode.getSquareContents(creatorLocation.getColumn(), creatorLocation.getRow()).getIdentifier(),
          Scene.getCreatorToken(),
          time,
          false,
          false
        ));
      }
      else{
        if (debug) System.out.println("   - Scene creator not present in scene to encode so will not be encoded in visual-spatial field");
      }
    
      //Scan sceneToEncode to populate visual STM enabling recognised objects 
      //to be encoded.
      if(debug) System.out.println("- Scanning for recognised chunks in scene to encode...");
      this._model.scanScene(sceneToEncode, numberFixations, time);
      ArrayList<ListPattern> recognisedChunks = new ArrayList<>();
      this._model.getVisualStm().iterator().forEachRemaining(chunk -> {
        if(!chunk.equals(this._model.getVisualLtm()) && !chunk.getImage().isEmpty()){
          recognisedChunks.add(chunk.getImage());
        }
      });
      
      /*************************************/
      /***** PROCESS RECOGNISED CHUNKS *****/
      /*************************************/
      if(!recognisedChunks.isEmpty()){
        
        //Reverse STM chunk order so that objects that are recognised most 
        //recently will be at the back of the list.  This is done for two 
        //reasons:
        // 1) The debug statement will print out the contents in an intuitive 
        //    order (chunks recognised first are printed first).
        // 2) It is possible to iterate through the chunks using a for loop that
        //    doesn't rely on setting a loop counter manually (reduces 
        //    possiblity of errors).
        Collections.reverse(recognisedChunks);
        if(debug) System.out.println("   - Recognised chunks (oldest first):"); recognisedChunks.forEach(chunk -> { if(debug) System.out.println("      " + chunk.toString()); });
        
        /***************************************************************/
        /***** TRANSLATE CREATOR-SPECIFIC -> SCENE-SPECIFIC COORDS *****/
        /***************************************************************/
        
        //To process recognised objects in the visual-spatial field, these 
        //objects must be capable of being identified in the visual-spatial 
        //field itself.  The best way to do this would be to use object 
        //identifiers however, chunks learned from Scene instances only encode
        //an object's class and location.  Since an object's class can not 
        //uniquely identify an object in the visual-spatial field, the chunked 
        //object's class AND location information must be used instead.  This 
        //makes it possible to identify equivalent objects in the scene to 
        //encode and the visual-spatial field since the scene to encode's 
        //coordinates and the visual-spatial field's coordinates are equal.  
        //However, this approach can still be problematic since the location 
        //information in a chunk may be relative to the agent that created the 
        //chunk, i.e. the scene-creator.  Therefore, to identify a recognised 
        //object in the visual-spatial field, it must be ensured that a chunk 
        //pattern's location information is non-creator relative.
        if(creatorLocation != null){
          if(debug) System.out.println("   - Translating recognised object coordinates to be relative to scene creator...");
          
          for(int i = 0; i < recognisedChunks.size(); i++){
            ListPattern chunkWithCreatorSpecificCoords = recognisedChunks.get(i);
            ListPattern chunkWithSceneSpecificCoords = new ListPattern(chunkWithCreatorSpecificCoords.getModality());

            chunkWithCreatorSpecificCoords.iterator().forEachRemaining(patternWithCreatorSpecificCoords -> {
              ItemSquarePattern iosWithCreatorSpecificCoords = (ItemSquarePattern)patternWithCreatorSpecificCoords;
              chunkWithSceneSpecificCoords.add(new ItemSquarePattern(
                iosWithCreatorSpecificCoords.getItem(),
                iosWithCreatorSpecificCoords.getColumn() + creatorLocation.getColumn(),
                iosWithCreatorSpecificCoords.getRow() + creatorLocation.getRow()
              ));
            });
            
            recognisedChunks.set(i, chunkWithSceneSpecificCoords);
          }
          
          if(debug) System.out.println("   - Recognised chunk contents with creator-relative coordinates:"); recognisedChunks.forEach(chunk -> { if(debug) System.out.println("      " + chunk.toString()); });
        }
        
        /*************************************************/
        /***** PLACE OBJECTS IN VISUAL-SPATIAL FIELD *****/
        /*************************************************/
        for(ListPattern chunk : recognisedChunks){
          if(debug) System.out.println("   - Processing chunk " + chunk.toString());
        
          //Advance the local "time" variable by the time it takes to encode one 
          //object.  Thus, when terminus values for the objects in this chunk 
          //are set, they'll be set to the time after encoding the chunk plus 
          //the specified lifespan for recognised objects (encoding the chunk 
          //won't prematurely age the objects as it would if the attention clock 
          //were advanced after terminus values are set).
          if(debug) System.out.println("      - Time before encoding chunk: " + time);
          time += this._objectPlacementTime;
          
          for(PrimitivePattern recognisedObject : chunk){
            
            //Cast the recognised object to an instance of an ItemSquarePattern 
            //and store its details for efficiency since each detail will be 
            //used multiple times below (code adheres to DRY principle).
            ItemSquarePattern recObj = (ItemSquarePattern)recognisedObject;
            String recObjClass = recObj.getItem();
            int recObjCol = recObj.getColumn();
            int recObjRow = recObj.getRow();
            
            if(debug) System.out.println("      - Encoding object " + recObj.toString());
              
            //Using the coordinates specified by the recognised object, get the 
            //object found on these coordinates in sceneToEncode.  This 
            //information will be used to determine if the recognised object is 
            //a real object or a ghost.
            SceneObject objectOnRecObjCoordsInReality = this._sceneEncoded.getSquareContents(recObjCol, recObjRow);
            
            //Also, retrieve the contents of the coordinates specified by the 
            //recognised object in the visual-spatial field.  These contents 
            //will need to be processed no matter if the recognised object 
            //corresponds to a real object or a ghost object (code adheres to 
            //DRY principle).
            ArrayList<VisualSpatialFieldObject> objectsOnVisualSpatialCoordsSpecifiedByRecObj = this._visualSpatialField.get(recObjCol).get(recObjRow);
            VisualSpatialFieldObject mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj = objectsOnVisualSpatialCoordsSpecifiedByRecObj.get(objectsOnVisualSpatialCoordsSpecifiedByRecObj.size() - 1);
            
            /********************************/
            /***** REAL OBJECT ENCODING *****/
            /********************************/
            if(debug) System.out.println("         - Object on coords in reality: " + objectOnRecObjCoordsInReality.getObjectClass());
            if(objectOnRecObjCoordsInReality.getObjectClass().equals(recObjClass)){
              if(debug) System.out.println("         - Recognised object represents a real object");
              
              //Set a boolean flag that will be used to determine if the 
              //recognised object needs to be encoded on the visual-spatial
              //coordinates specified.
              boolean recognisedObjectAlreadyEncodedHere = false;
              
              //Process the latest VisualSpatialFieldObject on the visual-spatial field 
              //coordinates specified by the object in the chunk.
              if(debug) System.out.println("         - Processing latest object on visual-spatial coords indicated: " + mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.getObjectClass());
              String mostRecentObjClassOnVisualSpatialCoordsSpecifiedByRecObj = mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.getObjectClass();

              //If the most recent object is a blind object, set its terminus.
              if(mostRecentObjClassOnVisualSpatialCoordsSpecifiedByRecObj.equals(Scene.getBlindSquareIdentifier())){
                if(debug) System.out.println("         - This is a blind object and will be overwritten.");
                mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.setTerminus(time, true);
              }
              //If this VisualSpatialFieldObject has the same class as the 
              //recognised
              //object, consider it as the same object.
              else if(mostRecentObjClassOnVisualSpatialCoordsSpecifiedByRecObj.equals(recObjClass)){
                if(debug) System.out.println("         - This is the same object so its terminus will be refreshed if it is alive.");
                
                //If the object is currently "alive" (it may not be depending
                //upon time parameters and number of objects recognised...),
                //refresh its terminus and set the boolean flag to indicate 
                //whether the recognised object is already encoded here to true.
                if(mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.alive(time)){
                  if(debug) System.out.println("         - Object is alive so its terminus will be refreshed");
                  mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.setTerminus(time, false);
                  recognisedObjectAlreadyEncodedHere = true;
                }
                else{
                  if(debug) System.out.println("         - Object's terminus has been reached so it will be recreated");
                }
              }
              //Otherwise, this is a different object and should be 
              //overwritten since recency is preferred.
              else{
                if(debug) System.out.println("            - This is a different object and will be overwritten");
                mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.setTerminus(time, true);
              }
              
              if(debug) System.out.println("         - Terminus of latest object on visual-spatial coords indicated by recognised object now equals: " + mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.getTerminus());
              
              //Encode the recognised object here if it isn't already.
              if(!recognisedObjectAlreadyEncodedHere){
                if(debug) System.out.println("         - Encoding recognised object");
                VisualSpatialFieldObject objectToAdd = new VisualSpatialFieldObject(
                  this,
                  objectOnRecObjCoordsInReality.getIdentifier(),
                  objectOnRecObjCoordsInReality.getObjectClass(),
                  time,
                  false,
                  false
                );
                objectToAdd.setRecognised(time, true);
                this._visualSpatialField.get(recObjCol).get(recObjRow).add(objectToAdd);
              }
            }
            /*********************************/
            /***** GHOST OBJECT ENCODING *****/
            /*********************************/
            else if(encodeGhostObjects){
              if(debug) System.out.println("         - Recognised object represents a ghost object");
              
              //Set a flag to control whether the ghost object should be 
              //encoded.
              boolean encodeGhostObject = false;
              
              //Process the latest VisualSpatialFieldObject on the visual-spatial field 
              //coordinates specified by the object in the chunk.
              if(debug) System.out.println("         - Processing latest object on visual-spatial coords indicated: " + mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.getObjectClass());
              String mostRecentObjClassOnVisualSpatialCoordsSpecifiedByRecObj = mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.getObjectClass();

              //If the most recent object is the scene creator, do nothing since
              //the scene creator's avatar should never be destroyed or 
              //overwritten.
              if(mostRecentObjClassOnVisualSpatialCoordsSpecifiedByRecObj.equals(Scene.getCreatorToken())){
                if(debug) System.out.println("         - This is the scene creator's avatar so will not be overwritten");
              }
              //If the most recent object is a blind square identifier then stop 
              //this square from being considered as blind (this will not be
              //done if the blind square's terminus has already been set by 
              //another ghost object being placed here previously, see the
              //VisualSpatialFieldObject.setTerminus() method).
              else if(mostRecentObjClassOnVisualSpatialCoordsSpecifiedByRecObj.equals(Scene.getBlindSquareIdentifier())){
                if(debug) System.out.println("         - This is a blind object and will be overwritten.");
                mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.setTerminus(time, true);
                encodeGhostObject = true;
              }
              //Otherwise, check that this object's class is different to the 
              //recognised object's.  If this is the case, the other object
              //may be a real object or another ghost object. 
              // - If it is a real object, its terminus should be updated 
              //   since attention will be focused on the location.
              // - If it is a ghost object, this object should overwrite it so
              //   kill the object and set the flag that controls ghost object
              //   encoding to true.
              else if(!mostRecentObjClassOnVisualSpatialCoordsSpecifiedByRecObj.equals(recObjClass)){
                if(debug) System.out.println("         - This is a different object to the ghost");
                if(!mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.isGhost()){
                  if(debug) System.out.println("         - The object is a real object so its terminus will be extended and the ghost won't overwrite it.");
                  mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.setTerminus(time, false);
                }
                else{
                  if(debug) System.out.println("         - The object is a ghost object so it will be overwritten by the recognised ghost.");
                  mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.setTerminus(time, true);
                  encodeGhostObject = true;
                }
              }
              //Otherwise, this must be the same ghost object recognised 
              //again.  In this case, extend the ghost's terminus.
              else {
                if(debug) System.out.println("         - This is the same object so its terminus will be refreshed if it is alive or will be recreated if not.");
                mostRecentObjOnVisualSpatialCoordsSpecifiedByRecObj.setTerminus(time, false);
              }

              //Finally, encode the ghost object if the flag indicates this 
              //should be done.
              if(encodeGhostObject){
                if(debug) System.out.println("         - Encoding the recognised ghost object.");
                VisualSpatialFieldObject ghostObject = new VisualSpatialFieldObject(
                  this, 
                  this.assignGhostObjectId(), 
                  recObjClass, 
                  time, 
                  true,
                  true
                );
                ghostObject.setRecognised(time, true);
                this._visualSpatialField.get(recObjCol).get(recObjRow).add(ghostObject);
              }
            }
          }
          if(debug) System.out.println("      - Time after encoding chunk = " + time);
        }//Process next STM chunk (if there is one)
      }
      else{
        if(debug) System.out.println("   - No chunks recognised");
      }
      
      /*********************************************************/
      /***** ENCODE EMPTY SQUARES AND UNRECOGNISED OBJECTS *****/
      /*********************************************************/
      
      if(debug) System.out.println("   - Finished encoding recognised chunks @ time: " + time);
      if(debug) System.out.println("- Start encoding unrecognised objects @ time: " + time);
      
      //Encode the remaining objects (unrecognised objects) in the scene to 
      //encode.  Encoding each unrecognised object incurs a time cost.
      for(int row = 0; row < this._sceneEncoded.getHeight(); row++){
        for(int col = 0; col < this._sceneEncoded.getWidth(); col++){
          
          //Get the object from the scene to encode that exists on these 
          //coordinates and check that it isn't a blind square or the scene 
          //creator, i.e. an empty square or an unrecognised object.  
          //If it isn't, encode it.
          SceneObject objectInReality = this._sceneEncoded.getSquareContents(col, row);
          String objectInRealityClass = objectInReality.getObjectClass();
          
          if(debug) System.out.println("   - Square " + col + ", " + row + " contains an object with class: " + objectInRealityClass);
          if( 
            !objectInRealityClass.equals(Scene.getBlindSquareIdentifier()) && 
            !objectInRealityClass.equals(Scene.getCreatorToken())
          ){
            if(debug) System.out.println("   - This isn't a blind square or the scene creator so will be processed.");
            boolean encodeObjectFromReality = false;
            
            if(debug) System.out.println("   - Time before encoding the object = " + time);
            
            //Get the most recent object on the coordinates specified from the
            //visual-spatial field.  This will either be a blind object or an 
            //actual object (empty squares not encoded yet).
            ArrayList<VisualSpatialFieldObject> objectsAtCoordinates = this._visualSpatialField.get(col).get(row);
            VisualSpatialFieldObject mostRecentObjectAtCoordinates = objectsAtCoordinates.get(objectsAtCoordinates.size() - 1);
            if(debug)System.out.println("   - Class of the most recent object on this square in the visual-spatial field: " + mostRecentObjectAtCoordinates.getObjectClass());
            
            //If the object in sceneToEncode is an empty square, the 
            //VisualSpatialFieldObject should be overwritten (ghost and blind 
            //squares killed).
            if(objectInRealityClass.equals(Scene.getEmptySquareIdentifier())){
              if(debug)System.out.println("   - Unrecognised object is an empty square.");
              encodeObjectFromReality = true;
              time += emptySquareEncodingTime;
            }
            //Otherwise, if its not an empty square, its a real object (blind 
            //squares were ignored earlier).  If the most recent 
            //VisualSpatialFieldObject is a blind square, encode the real object 
            //over this.  Otherwise, the most recent VisualSpatialFieldObject on 
            //these coordinates will be either the same object as that in 
            //reality or a ghost.  If the objects are the same, do nothing 
            //otherwise, overwrite the ghost.
            else if(
              mostRecentObjectAtCoordinates.getObjectClass().equals(Scene.getBlindSquareIdentifier()) ||
              mostRecentObjectAtCoordinates.isGhost()
            ){
              if(debug) System.out.println("   - Unrecognised object not an empty square");
              if(debug) System.out.println("   - The most recent object on this square in the visual-spatial field is either a blind square or a ghost.");
              encodeObjectFromReality = true;
              time += this._objectPlacementTime;
            }
            
            if(encodeObjectFromReality){
              if(debug) System.out.println("   - Encoding the unrecognised object" );
              mostRecentObjectAtCoordinates.setTerminus(time, true);
              this._visualSpatialField.get(col).get(row).add(new VisualSpatialFieldObject(
                this,
                objectInReality.getIdentifier(),
                objectInReality.getObjectClass(),
                time,
                true,
                false
              ));
              if(debug) System.out.println("   - Time after encoding the object = " + time);
            }
          } else{
            if(debug) System.out.println("   - This is a blind square or the scene creator so will be ignored.");
          }
        }
      }
      
      if(debug){
        System.out.println("- Visual-spatial field after construction");
        for(int row = 0; row < this._sceneEncoded.getHeight(); row++){
          for(int col = 0; col < this._sceneEncoded.getWidth(); col++){
            System.out.println("   - Col: " + col + ", row: " + row);
            ArrayList<VisualSpatialFieldObject> squareContents = this._visualSpatialField.get(col).get(row);
            for(int item = 0; item < squareContents.size(); item++){
              VisualSpatialFieldObject object = squareContents.get(item);
              System.out.println("      - Item " + item);
              System.out.println("         ID: " + object.getIdentifier());
              System.out.println("         Class: " + object.getObjectClass()); 
              System.out.println("         Time created: " + object.getTimeCreated());
              System.out.println("         Terminus: " + object.getTerminus());
              System.out.println("         Recognised: " + object.recognised(time));
              System.out.println("         Ghost: " + object.isGhost());
            }
          }
        };
      }
      
      //Check for duplicate identifiers in the visual-spatial field now that all 
      //objects have been placed.
      try {
        this.checkForDuplicateObjects();
      } catch (VisualSpatialFieldException ex) {
        throw ex;
      }

      //Finally, set the attention clock of the associated CHREST model to the 
      //time calculated for instantiation.
      this._model.setAttentionClock(time);
    }
    else{
      if(debug) System.out.println("- Scene to encode is entirely blind, exiting constructor");
    }
    
    if(debug) System.out.println("- Attention clock after constructing visual-spatial field: " + this._model.getAttentionClock());
  }
  
  /**
   * Checks for duplicate {@link jchrest.lib.VisualSpatialFieldObject}s on this
   * {@link #this}.  A {@link jchrest.lib.VisualSpatialFieldObject} is 
   * considered to be a duplicate if its identifier is shared by another
   * {@link jchrest.lib.VisualSpatialFieldObject} on this {@link #this}.
   * Since {@link jchrest.lib.VisualSpatialFieldObject}s that represent blind
   * and empty squares do not have unique identifiers, these objects are 
   * excluded from this check.
   */
  private void checkForDuplicateObjects() throws VisualSpatialFieldException{
    HashSet<String> objectIds = new HashSet<>();
    for(int col = 0; col < this.getWidth(); col++){
      for(int row = 0; row < this.getHeight(); row++){
        for(VisualSpatialFieldObject object : this.getSquareContents(col, row)){
          String identifier = object.getIdentifier();
          if(
            !identifier.equals(Scene.getBlindSquareIdentifier()) &&
            !identifier.equals(Scene.getEmptySquareIdentifier()) &&
            objectIds.add(object.getIdentifier()) == false
          ){
            throw new VisualSpatialFieldException("The identifer for the " + VisualSpatialFieldObject.class.getName() + " "
              + "on coordinates (" + col + ", " + row + ") in the " + VisualSpatialField.class.getName() + " "
              + "created from the " + Scene.class.getName() + " with name '" + this.getSceneEncoded().getName() + "' "
              + "is not unique (identifier: '" + identifier + "', object class: '" + object.getObjectClass() + "')."
            );
          }
        }
      }
    }
  }

  /**
   * @return A unique identifier in context of this {@link #this} for a ghost
   * {@link jchrest.lib.VisualSpatialFieldObject}.
   */
  private String assignGhostObjectId(){
    String ghostItemId = VisualSpatialField.getGhostObjectIdPrefix() + this._ghostObjectIdentifier;
    this._ghostObjectIdentifier++;
    return ghostItemId;
  }
  
  /**
   * @return The prefix for ghost object ID's.
   */
  public static String getGhostObjectIdPrefix(){
    return "g";
  }
  
  /**
   * @return The number of rows (height) of this {@link #this}.
   */
  public int getHeight(){
    return this._height;
  }
  
  /**
   * Returns all objects on the visual-spatial field at the coordinates 
   * specified.
   * 
   * @param col
   * @param row
   * @return All {@link jchrest.lib.VisualSpatialFieldObject} instances on the square 
   * specified in the visual-spatial field.
   */
  public ArrayList<VisualSpatialFieldObject> getSquareContents(int col, int row){
    return this._visualSpatialField.get(col).get(row);
  }
  
  /**
   * Returns the lifespan specified for recognised objects.
   * 
   * @return 
   */
  public int getRecognisedObjectLifespan(){
    return this._lifespanForRecognisedObjects;
  }
  
  /**
   * Returns the {@link jchrest.lib.Scene} that was encoded into this
   * {@link #this} originally.
   * 
   * @return 
   */
  public Scene getSceneEncoded(){
    return this._sceneEncoded;
  }
  
  /**
   * Returns the lifespan specified for unrecognised objects.
   * 
   * @return 
   */
  public int getUnrecognisedObjectLifespan(){
    return this._lifespanForUnrecognisedObjects;
  }
  
  /**
   * Returns the state of this {@link #this} at the time specified as a {@link 
   * jchrest.lib.Scene}.
   * 
   * TODO: should this incur an attentional time cost?
   * TODO: should this incur an access time cost?
   * 
   * @param time The time in the domain when this function was invoked.
   * @param encodeGhostObjects
   * 
   * @return The {@link jchrest.lib.Scene} returned will contain {@link 
   * jchrest.lib.SceneObject} representations of 
   * {@link jchrest.lib.VisualSpatialFieldObject}s that are alive (see 
   * {@link jchrest.lib.VisualSpatialFieldObject#alive(int)}) on this {@link 
   * #this} at the time specified.  If a coordinate on this {@link #this} does
   * not contain any {@link jchrest.lib.VisualSpatialFieldObject}s that are 
   * alive, a {@link jchrest.lib.SceneObject} that represents an empty square
   * will be found on the {@link jchrest.lib.Scene} coordinates.  Also, if there
   * are multiple {@link jchrest.lib.VisualSpatialFieldObject}s on a coordinate,
   * the corresponding coordinate in the {@link jchrest.lib.Scene} returned will
   * contain a {@link jchrest.lib.SceneObject} that represents the 
   * {@link jchrest.lib.VisualSpatialFieldObject} that was added to the 
   * coordinates most recently.
   */
  public Scene getAsScene(int time, boolean encodeGhostObjects){
      
    //Create a new Scene instance based on the current dimensions of this
    //visual-spatial field.
    Scene visualSpatialFieldScene = new Scene(
      "Visual-spatial-field @ time " + time, 
      this.getWidth(), 
      this.getHeight(),
      this
    );

    for(int row = 0; row < this.getHeight(); row++){
      for(int col = 0; col < this.getWidth(); col++){
        
        //Set a boolean flag that will be used to determine if the model has to
        //fall back on using the scene transposed in order to specify what 
        //object is present on the current coordinates in the scene returned.  
        //If this boolean flag is set to false after processing the objects on
        //the current coordinates then, this means that there are no objects
        //currently "alive" on the coordinates.  In this case, the model must 
        //either encode the square as a blind square or an empty square.  If the
        //coordinates in the scene originally transposed are blind, a blind 
        //square will be encoded on the scene to be returned.  Otherwise, an
        //empty square will be encoded.
        boolean objectEncodedOnCoordinates = false;
        
        for(VisualSpatialFieldObject object : this.getSquareContents(col, row)){
          
          //If an object is alive at the time specified, add it to the scene to
          //be returned.  Due to the fact that the for loop goes from the oldest
          //item to the most recent item on the coordinates, the most recent 
          //item that is alive will be present on the coordinates in the scene
          //returned.
          if(object.alive(time)){
            boolean encodeObject = false;
            if(object.isGhost()){
              if(encodeGhostObjects){
                encodeObject = true;
              }
            }
            else{
              encodeObject = true;
            }
            
            if(encodeObject){
              objectEncodedOnCoordinates = true;
              visualSpatialFieldScene.addItemToSquare(col, row, object.getIdentifier(), object.getObjectClass());
            }
          }
          
          if(!objectEncodedOnCoordinates){
            SceneObject squareContents = this.getSceneEncoded().getSquareContents(col, row);
            if(squareContents.getObjectClass().equals(Scene.getBlindSquareIdentifier())){
              visualSpatialFieldScene.addItemToSquare(col, row, Scene.getBlindSquareIdentifier(), Scene.getBlindSquareIdentifier());
            } else {
              visualSpatialFieldScene.addItemToSquare(col, row, Scene.getEmptySquareIdentifier(), Scene.getEmptySquareIdentifier());
            }
          }
        }
      }
    }
    
    return visualSpatialFieldScene;
  }
  
  public int getWidth(){
    return this._width;
  }
  
  /**
   * Moves objects on the visual-spatial field eye according to the sequence of 
   * moves specified.  Object movement can only occur if the attention of the 
   * CHREST model associated with this {@link #this} is free.  
   * 
   * If all moves are successful, the attention clock of the CHREST model 
   * associated with this {@link #this} will be set to the product of the time 
   * taken to access this {@link #this} plus the number of moves performed 
   * multiplied by the time specified to move an object in this {@link #this}.
   * 
   * This method does not constrain the number of squares moved by an object in
   * the visual-spatial field.  In other words, according to this method, it 
   * takes the same amount of time to move an object across 5 squares as it does
   * to move it across one.  Any movement constraints like this should be 
   * implemented by the calling function.
   * 
   * Note that if an an object is moved to coordinates in this {@link #this} 
   * that are already occupied then the two objects will co-exist on the 
   * coordinates; the new object does not overwrite the old object.
   * 
   * @param moveSequences A 2D ArrayList whose first dimension elements 
   * should contain ArrayLists of {@link jchrest.lib.ItemSquarePattern} 
   * instances that prescribe a sequence of moves for one 
   * {@link jchrest.lib.VisualSpatialFieldObject} using coordinates relative to 
   * this {@link #this} rather than coordinates used in the external domain or 
   * relative to the "mover" in this {@link #this}.  It is <b>imperative</b> 
   * that {@link jchrest.lib.VisualSpatialFieldObject}s to be moved are 
   * identified using their unique identifier (see 
   * {@link jchrest.lib.VisualSpatialFieldObject#getIdentifier()}) rather than 
   * their object class (see 
   * {@link jchrest.lib.VisualSpatialFieldObject#getObjectClass()}). For 
   * example, if two {@link jchrest.lib.VisualSpatialFieldObject} have the same
   * object class, A, but have unique identifiers, 0 and 1, and both are to be
   * moved, 0 before 1, then the ArrayList passed should specify: 
   * 
   * [[0 sourceX sourceY], [0 destinationX desitinationY]], 
   * [[1 sourceX sourceY], [1 desitinationX destinationY]].
   * 
   * @param time The current time (in milliseconds) in the domain when object
   * movement is requested.
   * 
   * @throws jchrest.lib.VisualSpatialFieldException If any of the 
   * moves specified cause any of the following statements to be evaluated as 
   * true:
   * <ol type="1">
   *  <li>
   *    More than one object is moved within the same sequence; object movement 
   *    should be strictly serial.
   *  </li>
   *  <li>
   *    The initial {@link jchrest.lib.ItemSquarePattern} in a move sequence 
   *    does not correctly identify where the 
   *    {@link jchrest.lib.VisualSpatialFieldObject} is located in this 
   *    {@link #this}.  If the {@link jchrest.lib.VisualSpatialFieldObject} has 
   *    previously been moved in this {@link #this}, the initial location should 
   *    be its current location in this {@link #this}.
   *  </li>
   *  <li>
   *    Only the initial location of a 
   *    {@link jchrest.lib.VisualSpatialFieldObject} is specified.
   *  </li>
   * </ol>
   */
  public void moveObjects(ArrayList<ArrayList<ItemSquarePattern>> moveSequences, int time, boolean debug) throws VisualSpatialFieldException {
    
    if(debug) System.out.println("== VisualSpatialField.moveObjects() ==");
    //Check that attention is free, if so, continue.
    if(this._model.attentionFree(time)){
      
      if(debug) System.out.println("- Attention is free");
      
      //Clone the current visual-spatial field so that if any moves are illegal, 
      //all moves performed up until the illegal move can be reversed.
      ArrayList<ArrayList<ArrayList<VisualSpatialFieldObject>>> visualSpatialFieldBeforeMovesApplied = new ArrayList<>();
      for(int col = 0; col < this._visualSpatialField.size(); col++){
        visualSpatialFieldBeforeMovesApplied.add(new ArrayList<>());
        for(int row = 0; row < this._visualSpatialField.get(col).size(); row++){
          visualSpatialFieldBeforeMovesApplied.get(col).add(new ArrayList<>());
          if(debug)System.out.println("~~~ Cloning items on square " + col + ", " + row + " ~~~");
          for(int object = 0; object < this._visualSpatialField.get(col).get(row).size(); object++){
            VisualSpatialFieldObject original = this._visualSpatialField.get(col).get(row).get(object);
            VisualSpatialFieldObject clone = original.createClone();
            visualSpatialFieldBeforeMovesApplied.get(col).get(row).add(clone);
          }
        }
      }
      
      //Track the time taken so far to process the object moves.  Used to 
      //assign terminus values for objects moved and update the attention clock
      //of the associated CHREST model.
      time += this._accessTime;
      int timeMoveSequenceBegins = time;
      if(debug) System.out.println("- Time moves begin: " + time);
      
      //Process each object move sequence.
      try{
        for(int objectMoveSequence = 0; objectMoveSequence < moveSequences.size(); objectMoveSequence++){

          //Get the first move sequence for an object and check to see if at 
          //least one movement has been specified for it.
          ArrayList<ItemSquarePattern> moveSequence = moveSequences.get(objectMoveSequence);
          if(debug) System.out.println("- Processing move sequence " + objectMoveSequence);
          
          if(moveSequence.size() >= 2){
            if(debug) System.out.println("   - Move sequence has more than 1 move");

            //Extract the information for the object to move.
            ItemSquarePattern moveFromDetails = moveSequence.get(0);
            String moveFromIdentifier = moveFromDetails.getItem();
            int colToMoveFrom = moveFromDetails.getColumn();
            int rowToMoveFrom = moveFromDetails.getRow();

            //Process each move for this object starting from the first element of 
            //the current second dimension array.
            for(int movement = 1; movement < moveSequence.size(); movement++){
              
              //Get the details of the object movement.
              ItemSquarePattern moveToDetails = moveSequence.get(movement);
              String moveToIdentifier = moveToDetails.getItem();
              int colToMoveTo = moveToDetails.getColumn();
              int rowToMoveTo = moveToDetails.getRow();
              
              if(debug) System.out.println("   - Move from details: " + moveFromDetails.toString());
              if(debug) System.out.println("   - Move to details: " + moveToDetails.toString());
              
              //Check to see if the identifier given for this move is the same
              //as that declared initially. If it isn't, serial movement is not
              //implemented so the entire move sequence should fail.
              if( moveFromIdentifier.equals(moveToIdentifier) ){
                if(debug) System.out.println("   - Move refers to the same object");

                //Cycle through the VisualSpatialFieldObjects on the square that
                //the VisualSpatialFieldObject to be moved is currently located
                //on and refresh their termini since they will be "looked
                //at".  Set the object to move's terminus to simulate the first 
                //part of the move ("picking" the object up).  After doing this,
                //it may be that the square which the object was located on 
                //should now be blind/empty again.  If this is the case, this 
                //must be done after checking the contents of the square that 
                //the object should be moved from otherwise, a 
                //java.util.ConcurrentModificationException error will be 
                //thrown.  To do this, initialise two boolean flags that, if set
                //to true, will encode a blind or empty square on the square the
                //object was moved from in the visual-spatial field.  Also, 
                //initialise a variable to store the time the object was moved
                //since the time tracker is incremented after the object is
                //"picked" up and therefore, without this variable, it would not
                //be possible to set the correct creation time for the 
                //blind/empty square that is to be added.
                ArrayList<VisualSpatialFieldObject> objectsOnSquareToMoveFrom = this.getSquareContents(colToMoveFrom, rowToMoveFrom);
                VisualSpatialFieldObject objectToMove = null;
                boolean makeSquareToMoveFromBlind = false;
                boolean makeSquareToMoveFromEmpty = false;
                int timeObjectMoved = 0;
                
                if(debug) System.out.println("   - Checking for object on visual-spatial coordinates to move from");
                for(VisualSpatialFieldObject objectOnSquareToMoveFrom : objectsOnSquareToMoveFrom){
                  
                  if(debug) System.out.println("      - Checking object " + objectOnSquareToMoveFrom.getIdentifier());
                  
                  //This object is the object to move.
                  if(
                    objectOnSquareToMoveFrom.getIdentifier().equals(moveFromIdentifier) &&
                    objectOnSquareToMoveFrom.alive(time)
                  ){
                    if(debug) System.out.println("         - This is the object to move and it is alive so it will be moved.");
                    
                    //Remove the object from the visual-spatial coordinates at
                    //this time.
                    objectOnSquareToMoveFrom.setTerminus(time, true);
                    if(debug) System.out.println("         - Terminus now equals " + objectOnSquareToMoveFrom.getTerminus());
                    
                    //Check to see if the square should be rencoded as blind or
                    //empty.  
                    //
                    //The square should be blind if it is blind in the Scene 
                    //originally transposed and the object being moved is not 
                    //co-habiting the square with other non-blind/empty objects 
                    //that are currently alive.
                    //
                    //The square should be empty if it isn't blind in the Scene 
                    //originally transposed and either: 
                    //
                    // 1) The object on the square before the object to be moved 
                    //    is an empty square.
                    // 2) The object being moved is not co-habiting the square 
                    //    with other non-empty objects that are currently alive.
                    if(debug) System.out.println("         - Checking if square should be made blind/empty again");
                    
                    boolean squareBlindInSceneTransposed = this.getSceneEncoded().getSquareContents(colToMoveFrom, rowToMoveFrom).getObjectClass().equals(Scene.getBlindSquareIdentifier());
                    VisualSpatialFieldObject previousObject = objectsOnSquareToMoveFrom.get( objectsOnSquareToMoveFrom.indexOf(objectOnSquareToMoveFrom) - 1);
                    boolean otherObjectAliveOnCoordinates = false;
                    
                    //Cycle through all objects on the square to see if any are
                    //non-blind/empty and currently alive.
                    for(VisualSpatialFieldObject objectToCheck : objectsOnSquareToMoveFrom){
                      String objectToCheckClass = objectToCheck.getObjectClass();
                      if(
                        !objectToCheckClass.equals(Scene.getBlindSquareIdentifier()) && 
                        !objectToCheckClass.equals(Scene.getEmptySquareIdentifier()) &&
                        objectToCheck.alive(time)
                      ){
                        otherObjectAliveOnCoordinates = true;
                      }
                    }
                    
                    if(!squareBlindInSceneTransposed){
                      if(debug) System.out.println("         - Square is not blind in scene transposed");
                      if(
                        previousObject.getObjectClass().equals(Scene.getEmptySquareIdentifier()) ||
                        !otherObjectAliveOnCoordinates
                      ){
                        if(debug) System.out.println("         - Setting the boolean flag to make the square empty again");
                        makeSquareToMoveFromEmpty = true;
                        timeObjectMoved = time;
                      }
                    }
                    else{
                      if(debug) System.out.println("         - Square is blind in scene transposed");
                      if( !otherObjectAliveOnCoordinates ){
                        if(debug) System.out.println("         - Setting the boolean flag to make the square blind again");
                        makeSquareToMoveFromBlind = true;
                        timeObjectMoved = time;
                      }
                    }
                    
                    //Increment the time tracker variable by the time taken to 
                    //move the object.  Do this now since it should still take 
                    //time to move an object even if it is moved to a blind 
                    //spot (the "putting-down" step of the move is not actually
                    //performed in this case).
                    time += this._objectMovementTime;
                    if(debug) System.out.println("         - Incrementing time taken to move object, now equal to " + time);
                    
                    //Create a new VisualSpatialFieldObject that represents the object 
                    //after the move.  
                    objectToMove = new VisualSpatialFieldObject(
                      this,
                      objectOnSquareToMoveFrom.getIdentifier(),
                      objectOnSquareToMoveFrom.getObjectClass(),
                      time,
                      true, 
                      objectOnSquareToMoveFrom.isGhost()
                    );
                    if(debug) System.out.println("         - Created the new object representation to be added to coordinates to move object to.");
                    if(debug) {
                      System.out.println("            ID: " + objectToMove.getIdentifier());
                      System.out.println("            Class: " + objectToMove.getObjectClass());
                      System.out.println("            Created at: " + objectToMove.getTimeCreated());
                      System.out.println("            Terminus:" + objectToMove.getTerminus());
                      System.out.println("            Recognised: " + objectToMove.recognised(time));
                      System.out.println("            Ghost: " + objectToMove.isGhost());
                    }
                  }
                  //If this is the creator of the visual-spatial field, do 
                  //nothing since its terminus should not be modified (the 
                  //creator's avatar should never die).
                  else if(objectOnSquareToMoveFrom.getObjectClass().equals(Scene.getCreatorToken())){}
                  //This isn't the object to move and isn't the creator of the
                  //visual-spatial field so extend its terminus.
                  else{
                    if(debug) System.out.println("         - This isn't the object to move but its terminus will be updated");
                    if(debug) System.out.println("            Current terminus: " + objectOnSquareToMoveFrom.getTerminus());
                    objectOnSquareToMoveFrom.setTerminus(time, false);
                    if(debug) System.out.println("            New terminus: " + objectOnSquareToMoveFrom.getTerminus());
                  }
                }
                
                //Make the coordinates the object was moved from blind.
                if(makeSquareToMoveFromBlind){
                  if(debug) System.out.println("         - Making the square blind again");
                  objectsOnSquareToMoveFrom.add(new VisualSpatialFieldObject(
                    this,
                    Scene.getBlindSquareIdentifier(),
                    Scene.getBlindSquareIdentifier(),
                    timeObjectMoved,
                    false,
                    false
                  ));
                }
                
                //Make the coordinates the object was moved from empty.
                if(makeSquareToMoveFromEmpty){
                  if(debug) System.out.println("         - Making the square empty again");
                  objectsOnSquareToMoveFrom.add(new VisualSpatialFieldObject(
                    this,
                    Scene.getEmptySquareIdentifier(),
                    Scene.getEmptySquareIdentifier(),
                    timeObjectMoved,
                    true,
                    false
                  ));
                }
                
                //Check to see if the object to move is currently on the
                //location specified in the visual-spatial field.  If a previous
                //move caused the object to be placed on a blind spot or the
                //item is on the square but its terminus has been reached, this 
                //check will return false.
                if(objectToMove != null){
                  
                  if(debug) System.out.println("      - Object on 'from' coordinates specified.");

                  //Check to see if the coordinates to move the object are both
                  //represented in the visual-spatial field and aren't a blind
                  //spot.  If both conditions are true, add the new
                  //VisualSpatialFieldObject to the intended visual-spatial 
                  //field coordinates. Otherwise, don't.
                  
                  //Determine if coordinates to move to are blind by checking to
                  //see if the coordinates in the scene transposed are blind 
                  //(too difficult to tell using the visual-spatial field square
                  //contents: there is only one object per square in the Scene
                  //transposed, however).
                  boolean squareToMoveToIsBlind = false;
                  if(this.getSceneEncoded().getSquareContents(colToMoveTo, rowToMoveTo).getObjectClass().equals(Scene.getBlindSquareIdentifier())){
                    squareToMoveToIsBlind = true;
                  }
                  
                  //Blind square and coordinates represented check.
                  if(
                    (colToMoveTo < this._sceneEncoded.getWidth() && rowToMoveTo < this._sceneEncoded.getHeight() ) &&
                    !squareToMoveToIsBlind
                  ){
                    
                    if(debug) System.out.println("      - Coordinates to move to are not blind and are represented so object will be moved there");
                    ArrayList<VisualSpatialFieldObject> objectsOnSquareToMoveTo = this.getSquareContents(colToMoveTo, rowToMoveTo);
                    
                    //Process the termini of objects on the square to be moved
                    //to.
                    if(debug) System.out.println("      - Updating termini of objects on cooridnates to move to");
                    for(VisualSpatialFieldObject objectOnSquareToMoveTo : objectsOnSquareToMoveTo){
                      if(debug) System.out.println("         - Processing object " + objectOnSquareToMoveTo.getIdentifier());
                      if(debug) System.out.println("            Current terminus: " + objectOnSquareToMoveTo.getTerminus());
                      
                      //If the object does not represent the creator of the 
                      //visual-spatial field (the avatar shouldn't ever die so 
                      //its terminus should always be null) and the object is 
                      //alive at the time of the move, process its terminus.
                      if(
                        !objectOnSquareToMoveTo.getObjectClass().equals(Scene.getCreatorToken()) && 
                        objectOnSquareToMoveTo.alive(time)
                      ){
                        
                        //If the object is an empty-square, kill it since the
                        //square should no longer be empty (there's an object 
                        //being moved onto it).
                        if(objectOnSquareToMoveTo.getObjectClass().equals(Scene.getEmptySquareIdentifier())){
                          objectOnSquareToMoveTo.setTerminus(time, true);
                        }
                        //If the object isn't an empty square identifier, extend
                        //its terminus since objects can co-habit visual-spatial
                        //coordinates and the location of the object has been
                        //focused on.
                        else{
                          objectOnSquareToMoveTo.setTerminus(time, false);
                        }
                      }
                      if(debug) System.out.println("            New terminus: " + objectOnSquareToMoveTo.getTerminus());
                    }
                    
                    //Now, "move" the object to be moved to its destination 
                    //coordinates.
                    objectsOnSquareToMoveTo.add(objectToMove);
                    if(debug){ 
                      System.out.println("      - Added object to coordinates.  Coordinate content:");
                      for(VisualSpatialFieldObject objectOnSquareToMoveTo : objectsOnSquareToMoveTo){
                        System.out.println("            ID: " + objectOnSquareToMoveTo.getIdentifier());
                        System.out.println("            Class: " + objectOnSquareToMoveTo.getObjectClass());
                        System.out.println("            Created at: " + objectOnSquareToMoveTo.getTimeCreated());
                        System.out.println("            Terminus:" + objectOnSquareToMoveTo.getTerminus());
                        System.out.println("            Recognised: " + objectOnSquareToMoveTo.recognised(time));
                        System.out.println("            Ghost: " + objectOnSquareToMoveTo.isGhost());
                      }
                    }
                    
                  }
                  else{
                    if(debug) System.out.println("      - Coordinates to move to are blind or not represented so object will not be placed");
                  }
                  
                  //Set the current location of the object to be its destination 
                  //so that the next move can be processed correctly.
                  moveFromDetails = moveToDetails;
                }
                //The object is not at the location specified.
                else{
                  if(debug) System.out.println("      - Object not on 'from' coordinates specified");
                  
                  //If this is the first movement then the actual object 
                  //location specification is incorrect so throw an exception 
                  //since this may indicate an issue with coordinate translation
                  //or experiment code.
                  if(movement == 1){
                    if(debug) System.out.println("      - This is the first move so the initial location must be incorrect, exiting");
                    throw new VisualSpatialFieldException("The initial location specified for object with ID " + moveFromIdentifier + " (" + moveFromDetails.toString() + ") does not contain this object.\n"
                        + "This may be because domain-specific coordinates have been used to specify the object's location.");                
                  }
                  //Otherwise, the object has decayed since a number of other
                  //moves have been performed or, it has been moved to a blind
                  //square in a previous move so start to process the next 
                  //object move set.
                  else{
                    if(debug) System.out.println("      - This isn't the first move so the object may have decayed or been moved to a blind square previously.");
                    if(debug) System.out.println("      - Skipping to next move sequence");
                    break;
                  }
                }
              }
              else{
                if(debug) System.out.println("   - Move does not refer to same object, exiting");
                throw new VisualSpatialFieldException(
                  "Sequence " + objectMoveSequence + " does not consistently " +
                  "refer to the same object (move " + movement + " refers to " +
                  moveToIdentifier + " so serial movement not implemented."
                );
              }
            }//End move for an object.
          }//End check for number of object moves being greater than or equal to 2.
          else{
            if(debug) System.out.println("   - Move sequence only contains 1 move, exiting");
            throw new VisualSpatialFieldException("The move sequence " + moveSequence.toString() + " does not contain any moves after the current location of the object is specified.");
          }
        }//End entire movement sequence for all objects.

        this._model.setAttentionClock(time);
      } 
      catch (VisualSpatialFieldException e){
        if(debug) System.out.println("   - VisualSpatialFieldObjectMoveException thrown, reverting visual-spatial field to its state before moves were processed.");
        this._visualSpatialField = visualSpatialFieldBeforeMovesApplied;
        if(debug){
          for(int row = 0; row < this._visualSpatialField.get(0).size(); row++){
            for(int col = 0; col < this._visualSpatialField.size(); col++){
              System.out.println("      - Col " + col + ", row " + row);
              for(VisualSpatialFieldObject object : this.getSquareContents(col, row)){
                System.out.println("         ID: " + object.getIdentifier());
                System.out.println("         Class: " + object.getObjectClass());
                System.out.println("         Created at: " + object.getTimeCreated());
                System.out.println("         Terminus:" + object.getTerminus());
                System.out.println("         Recognised: " + object.recognised(timeMoveSequenceBegins));
                System.out.println("         Ghost: " + object.isGhost());
              }
            }
          }
        }
        throw e;
      }
    }
    else{
      if(debug) System.out.println("- Attention is not free, exiting");
    }
  }
}
