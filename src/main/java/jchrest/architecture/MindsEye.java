package jchrest.architecture;

import java.util.ArrayList;
import java.util.Iterator;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.MindsEyeMoveObjectException;
import jchrest.lib.MindsEyeObject;
import jchrest.lib.PrimitivePattern;
import jchrest.lib.Scene;
import jchrest.lib.Square;

/**
 * Class that implements the "Mind's Eye", specifically one that handles
 * <i>attention-based imagery</i> (see page 301 of "Image and Brain" by Stephen
 * Kosslyn).
 * 
 * The mind's eye contains a visual-spatial field whose coordinates maintain
 * a history of objects that have been placed on them.  The visual-spatial field
 * is a 2D ArrayList whose size is finite after creation (consistent with the 
 * view proposed by Kosslyn on page 305 of "Image and Brain)".
 * 
 * Information in the mind's eye can be manipulated independently of the
 * environment that the observer is currently situated in to test outcomes of 
 * actions without incurring these outcomes in "reality".
 * 
 * TODO: After instantiation, the access time may decrease depending upon how
 *       many times the image has been re-encoded (see page 307 of "Image and
 *       Brain" by Kosslyn).
 * 
 * TODO: The size of the visual-spatial field may be finite before creation 
 *       according to Kosslyn (proposed that reliable encoding of object 
 *       locations occurs when the matrix is 3 x 3, anything larger causes 
 *       object encoding to become unreliable and subject to error).
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class MindsEye {
  
  //The CHREST model that the mind's eye instance is associated with.
  private final Chrest _model;
  
  //The visual spatial field of the mind's eye (a 3D ArrayList).  First 
  //dimension elements represent the columns (x-coordinates) of a scene, second
  //dimension elements represent the rows (y-coordinates) of a scene, third
  //dimension elements represent the objects on the square (there may be 
  //multiple objects on one square).
  private ArrayList<ArrayList<ArrayList<MindsEyeObject>>> _visualSpatialField = new ArrayList<>();
  
  //The scene that was transposed into the mind's eye originally.
  private final Scene _sceneTransposed;
  
  //Time taken (ms) to access the mind's eye.
  private final int _accessTime;
  
  //The time taken (in milliseconds) to move an object in the mind's eye.
  private final int _objectMovementTime;
  
  //The time taken (in milliseconds) to place an object in the mind's eye during
  //instantiation.
  private final int _objectPlacementTime;
  
  //The length of time (in milliseconds) that an object will exist in the mind's
  //eye for if it is part of a chunk (_lifespanForRecognisedObjects) or if it
  //isn't (_lifespanForUnrecognisedObjects).
  private final int _lifespanForRecognisedObjects;
  private final int _lifespanForUnrecognisedObjects;
  
  /**
   * Constructor for "MindsEye" object.  The constructor creates 
   * {@link jchrest.lib.MindsEyeObject} instances that represent objects in the 
   * {@link jchrest.lib.Scene} passed in the visual-spatial field of the mind's 
   * eye.
   * 
   * The {@link jchrest.lib.Scene} to be transposed is first scanned using
   * {@link jchrest.architecture.Chrest#scanScene(jchrest.lib.Scene, int, int)}
   * and objects that are recognised are transposed first in the visual-spatial 
   * field.  The rest of the {@link jchrest.lib.Scene} is then processed from 
   * south-west to north-east first along the x-axis, then the y-axis.
   * 
   * When transposing objects in a chunk, the attention clock of the
   * {@link jchrest.architecture.Chrest} instance associated with this mind's 
   * eye is advanced by the time specified to encode one object.  In other 
   * words, a chunk is considered as one object rather than multiple objects 
   * with respect to mind's eye transposition time.
   * 
   * If an object is recognised more than once, its terminus value will be set
   * to the time at which the object was last seen during mind's eye 
   * instantiation plus the lifespan specified for recognised objects 
   * (implements refreshment of objects on visual-spatial field).
   * 
   * @param model The CHREST model instance that the mind's eye instance is 
   * associated with.
   * 
   * @param sceneToTranspose The {@link jchrest.lib.Scene} instance that is to 
   * be transposed into the visual-spatial field.
   * 
   * @param objectEncodingTime The length of time (in milliseconds) that it 
   * takes to transpose an object into the visual-spatial field during 
   * instantiation.
   * 
   * @param emptySquareEncodingTime The length of time (in milliseconds) that it 
   * takes to transpose an empty square into the visual-spatial field during 
   * instantiation.
   * 
   * @param accessTime The time taken (in milliseconds) to access the mind's 
   * eye at any time.
   * 
   * @param objectMovementTime The time taken (in milliseconds) to move an 
   * object in the visual-spatial field when the 
   * {@link jchrest.architecture.MindsEye#moveObjects(java.util.ArrayList, int)  
   * function is used.
   * 
   * @param lifespanForRecognisedObjects The length of time (in milliseconds) 
   * that a recognised object will exist in the visual-spatial field for when it 
   * is transposed or interacted with after transposition.
   * 
   * @param lifespanForUnrecognisedObjects The length of time (in milliseconds) 
   * that an unrecognised object will exist in the visual-spatial field for when 
   * it is transposed or interacted with after transposition.
   * 
   * @param numberFixations The number of fixations that should be used when 
   * scanning the {@link jchrest.lib.Scene} that is to be transposed into the
   * visual-spatial field.
   * 
   * @param domainTime The current time (in milliseconds) in the domain where 
   * the CHREST model associated with the mind's eye instance is located.
   * 
   */
  public MindsEye(Chrest model, Scene sceneToTranspose, int objectEncodingTime, int emptySquareEncodingTime, int accessTime, int objectMovementTime, int lifespanForRecognisedObjects, int lifespanForUnrecognisedObjects, int numberFixations, int domainTime){   
    
    this._model = model;
    this._sceneTransposed = sceneToTranspose;
    this._accessTime = accessTime;
    this._objectMovementTime = objectMovementTime;
    this._objectPlacementTime = objectEncodingTime;
    this._lifespanForRecognisedObjects = lifespanForRecognisedObjects;
    this._lifespanForUnrecognisedObjects = lifespanForUnrecognisedObjects;
    
    //Set a local "time" variable to be equal to the time that the constructor 
    //was called in the domain plus mind's eye access time.  This variable will
    //be used to set the attention clock of the CHREST model associated with 
    //the mind's eye later.
    int time = domainTime + this._accessTime;
    
    //Create the visual-spatial field using the scene to transpose as a basis.  
    //The visual spatial field will consist entirely of blind squares at first.
    this._visualSpatialField = new ArrayList<>();
    for(int col = 0; col < this._sceneTransposed.getWidth(); col++){
      this._visualSpatialField.add(new ArrayList<>());
      for(int row = 0; row < this._sceneTransposed.getHeight(); row++){
        this._visualSpatialField.get(col).add(new ArrayList<>());
        this._visualSpatialField.get(col).get(row).add(new MindsEyeObject(this, Scene.getBlindSquareIdentifier(), time));
      }
    }
    
    //Check for an entirely blind scene, if this is the case, stop the procedure
    //at this point otherwise the function will hang when the scene to transpose
    //is scanned below.
    boolean sceneToTransposeIsEntirelyBlind = true;
    Iterator<PrimitivePattern> sceneItems = sceneToTranspose.getEntireScene(true).iterator();
    while(sceneItems.hasNext()){
      PrimitivePattern sceneItem = sceneItems.next();
      if(sceneItem instanceof ItemSquarePattern){
        ItemSquarePattern sceneContent = (ItemSquarePattern)sceneItem;
        if(!sceneContent.getItem().equals(Scene.getBlindSquareIdentifier())){
          sceneToTransposeIsEntirelyBlind = false;
          break;
        }
      }
    }
    if(!sceneToTransposeIsEntirelyBlind){
    
      //Get the location of the creator in the Scene being transposed so that
      //it can be determined whether object coordinates in the scene to be 
      //transposed need to be translated from creator-relative to non 
      //creator-relative in the next step.  This will happen if chunks in LTM
      //have creator-relative coordinates.  Translation is necessary since 
      //recognised objects can not be placed correctly in the visual-spatial 
      //field otherwise.
      Square locationOfSelf = sceneToTranspose.getLocationOfSelf();

      //Create a data structure that will contain items that have been 
      //recognised on the scene to transpose.  This allows for the filtering of 
      //unrecognised objects in the scene to be transposed, so that mind's eye 
      //object terminus values can be set correctly. 
      ArrayList<String> recognisedPatterns = new ArrayList<>();

      //Scan the scene to be transposed to populate visual STM so that 
      //recognised objects can be identified.
      this._model.scanScene(sceneToTranspose, numberFixations, time);
      Stm visualStm = this._model.getVisualStm();

      //Process visual STM items that aren't empty and aren't root nodes from 
      //oldest to newest (since STM is a FIFO list we need to go from back to 
      //front and STM is zero-indexed).
      for(int visualStmItem = (visualStm.getCount() -1); visualStmItem >= 0; visualStmItem--){
        Node stmItem = visualStm.getItem(visualStmItem);

        if(
          !stmItem.getImage().isEmpty() && //If the item isn't empty
          !stmItem.equals(this._model.getVisualLtm()) //If the item isn't the root node for visual LTM
        ){

          //Advance the local "time" variable by the time it takes to place one 
          //object in the mind's eye.  Thus, when terminus values for chunk 
          //items are set, they'll be set to the time after encoding the chunk 
          //plus the specified lifespan for recognised objects (encoding the 
          //chunk won't prematurely age the objects as it would if the attention 
          //clock were advanced after terminus values are set).
          time += this._objectPlacementTime;

          //Get the contents (patterns) of the next visual STM chunk (may have 
          //relative coordinates) and process each one.
          Iterator<PrimitivePattern> chunkPatternsWithPossibleRelativeCoordinates = visualStm.getItem(visualStmItem).getImage().iterator();
          while(chunkPatternsWithPossibleRelativeCoordinates.hasNext()){          
            PrimitivePattern patternWithPossibleRelativeCoordinates = chunkPatternsWithPossibleRelativeCoordinates.next();
            if(patternWithPossibleRelativeCoordinates instanceof ItemSquarePattern){
              ItemSquarePattern patternToProcess = (ItemSquarePattern)patternWithPossibleRelativeCoordinates;

              //Translate coordinates if necessary.
              if(locationOfSelf != null){
                patternToProcess = new ItemSquarePattern(
                  patternToProcess.getItem(),
                  patternToProcess.getColumn() + locationOfSelf.getColumn(),
                  patternToProcess.getRow() + locationOfSelf.getRow()
                );
              }

              //Get the current contents of the mind's eye coordinates specified 
              //by the pattern to process.  If there are objects here already, 
              //check each one to see if its identifier matches the identifier 
              //for the pattern being processed.  If this is the case, set the 
              //terminus for the corresponding minds eye object to be equal to 
              //the current attention clock plus the lifespan for recognised 
              //objects (thus implementing refreshment).  If there are other,
              //non-blind objects here, set their terminus accordingly since
              //they have been "looked-at" too.
              ArrayList<MindsEyeObject> mindsEyeObjectsAtPatternLocation = this._visualSpatialField.get(patternToProcess.getColumn()).get(patternToProcess.getRow());
              boolean objectAlreadyAtCoordinates = false;
              for(MindsEyeObject mindsEyeObjectAtPatternLocation : mindsEyeObjectsAtPatternLocation){
                String objectIdentifier = mindsEyeObjectAtPatternLocation.getIdentifier();

                if(objectIdentifier.equals(patternToProcess.getItem())){
                  mindsEyeObjectAtPatternLocation.setTerminus(time, false);
                  objectAlreadyAtCoordinates = true;
                }
                else if(!objectIdentifier.equals(Scene.getBlindSquareIdentifier())){
                  mindsEyeObjectAtPatternLocation.setTerminus(time, false);
                }
              }

              //If the pattern isn't already on the coordinates, add it.
              if(!objectAlreadyAtCoordinates){

                //If the first blind spot object on these coordinates is set to
                //null then this is the first object to be placed on the 
                //coordinates.  Consequently, set the terminus of the first 
                //blind-spot object to the current time to indicate that the
                //coordinates are no longer considered blind.
                if(mindsEyeObjectsAtPatternLocation.get(0).getTerminus() == null){
                  mindsEyeObjectsAtPatternLocation.get(0).setTerminus(time, true);
                }

                //Now, add the recognised object.
                MindsEyeObject mindsEyeObject = new MindsEyeObject(this, patternToProcess.getItem(), time);
                mindsEyeObject.setRecognised(time);
                this._visualSpatialField.get(patternToProcess.getColumn()).get(patternToProcess.getRow()).add(mindsEyeObject);
              }

              //Add the pattern with its non creator-specific coordinates to the 
              //recognised patterns data structure so that it will be ignored 
              //when unrecognised objects are transposed into the visual-spatial 
              //field below.
              recognisedPatterns.add(patternToProcess.toString());
            }
          }
        }
      }//Process next STM chunk (if there is one)

      //Transpose the remainder of the scene that isn't recognised.  Encoding 
      //each unrecognised object incurs a time cost unless the square is blind.
      Iterator<PrimitivePattern> patternsInSceneToTranspose = sceneToTranspose.getEntireScene(true).iterator();
      while(patternsInSceneToTranspose.hasNext()){
        ItemSquarePattern patternToTranspose = (ItemSquarePattern)patternsInSceneToTranspose.next();

        //Check that the pattern to transpose hasn't already been recognised and
        //transposed earlier.  If so, don't transpose it now.
        if( !recognisedPatterns.contains(patternToTranspose.toString()) ){
          String objectIdentifier = patternToTranspose.getItem();

          //If the square to transpose is empty, set the terminus of the object 
          //already on these coordinates (a blind-square) to the current time.  
          //The attention clock of the CHREST model associated with this mind's 
          //eye should also be incremented by the time taken to encode an empty 
          //square.  Note that there is no "empty square object" added here.
          if(objectIdentifier.equals(Scene.getEmptySquareIdentifier())){
            time += emptySquareEncodingTime;
            for(MindsEyeObject blindSpot : this._visualSpatialField.get(patternToTranspose.getColumn()).get(patternToTranspose.getRow())){
              blindSpot.setTerminus(time, true);
            }
          }
          //Otherwise, if the square to transpose isn't empty advance the 
          //attention clock of the CHREST model associated with this mind's 
          //eye by the time taken to encode an object.  Now, cycle through the
          //objects already on these coordinates and set their terminus 
          //appropriately:
          //
          // 1) If the object is a blind square identifier and its terminus
          //    currently isn't set, set its terminus to the current time since
          //    the square is no longer blind given that there's an object seen
          //    on these coordinates in the scene being transposed.
          // 2) If the object isn't a blind square, set its terminus according 
          //    to the current time (thus implementing refreshment).
          //
          //After this, add the object to the coordinates.
          else if(!objectIdentifier.equals(Scene.getBlindSquareIdentifier())){
            time += this._objectPlacementTime;
            ArrayList<MindsEyeObject> objectsOnSquare = this._visualSpatialField.get(patternToTranspose.getColumn()).get(patternToTranspose.getRow());
            for(MindsEyeObject object : objectsOnSquare){
              String identifier = object.getIdentifier();
              if(identifier.equals(Scene.getBlindSquareIdentifier()) && object.getTerminus() == null){
                object.setTerminus(time, true);
              }
              else if(!identifier.equals(Scene.getBlindSquareIdentifier())){
                object.setTerminus(time, false);
              }
            }
            
            this._visualSpatialField.get(patternToTranspose.getColumn()).get(patternToTranspose.getRow()).add(new MindsEyeObject(this, patternToTranspose.getItem(), time));
          }
        }
      }
    } //End entirely blind check
    
    //Finally, set the attention clock of the CHREST model associated with this 
    //mind's eye to the time calculated for instantiation.
    this._model.setAttentionClock(time);
  }
  
  /**
   * Returns all objects on the visual-spatial field at the coordinates 
   * specified.
   * 
   * @param col
   * @param row
   * @return All {@link jchrest.lib.MindsEyeObject} instances on the square 
   * specified in the visual-spatial field.
   */
  public ArrayList<MindsEyeObject> getObjectsOnVisualSpatialSquare(int col, int row){
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
   * Returns the {@link jchrest.lib.Scene} that was transposed into the 
   * visual-spatial field of this mind's eye originally.
   * 
   * @return 
   */
  public Scene getSceneTransposed(){
    return this._sceneTransposed;
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
   * Returns the state of the visual-spatial field of the mind's eye at the time
   * specified as a {@link jchrest.lib.Scene} instance.
   * 
   * TODO: should this incur an attentional time cost?
   * TODO: should this incur an access time cost?
   * 
   * @param time The time in the domain when this function was invoked.
   * 
   * @return An instance of {@link jchrest.lib.Scene} representing the current
   * state of the visual-spatial field of the mind's eye at the time specified.
   */
  public Scene getVisualSpatialFieldAsScene(int time){
      
    //Create a new Scene instance based on the current dimensions of the 
    //visual-spatial field and associate this mind's eye instance with it.
    Scene mindsEyeScene = new Scene(
      "Mind's eye scene", 
      this._sceneTransposed.getWidth(), 
      this._sceneTransposed.getHeight()
    );

    for(int row = 0; row < this._sceneTransposed.getHeight(); row++){
      for(int col = 0; col < this._sceneTransposed.getWidth(); col++){

        //Get objects on square.
        ArrayList<MindsEyeObject> objects = this._visualSpatialField.get(col).get(row);
        ArrayList<MindsEyeObject> itemsThatExistAtTime = new ArrayList<>();
        
        //If any objects on square have a terminus of null then this is a blind
        //square so should be added to the list of items that exist on this
        //square at this time.  Otherwise, if the terminus for the object is not
        //null check that its creation time is earlier than or equal to the time 
        //specified and its terminus is later than the time specified.  If this 
        //is the case, add it to the list of items that exist on this square at 
        //this time.
        for(MindsEyeObject object : objects){
          if(object.getTerminus() == null){
            itemsThatExistAtTime.add(object);
          }
          else if(
            time >= object.getTimeCreated() &&
            time < object.getTerminus()
          ){
            itemsThatExistAtTime.add(object);
          }
        }

        //If no items exist on the square at the time passed then add an empty
        //square to the Scene.  Otherwise, add each of the items that exist on
        //the square at the time specified to the Scene.
        if(itemsThatExistAtTime.isEmpty()){
          mindsEyeScene.addItemToSquare(col, row, Scene.getEmptySquareIdentifier());
        }
        else{
          for(MindsEyeObject object : itemsThatExistAtTime){
            mindsEyeScene.addItemToSquare(col, row, object.getIdentifier());
          }
        }
      }
    }
    
    return mindsEyeScene;
  }
  
  /**
   * Moves objects in the mind's eye according to the sequence of moves 
   * specified.  Object movement can only occur if the attention of the CHREST 
   * model associated with this mind's eye instance is free.  
   * 
   * If all moves are successful, the attention clock of the CHREST model 
   * associated with this mind's eye will be set to the product of the time 
   * taken to access the mind's eye plus the number of moves performed 
   * multiplied by the time specified to move an object in the mind's eye.
   * 
   * This method does not constrain the number of squares moved by an object in
   * the visual-spatial field.  In other words, according to this method, it 
   * takes the same amount of time to move an object across 5 squares as it does
   * to move it across one-square.  Any movement constraints like this should be 
   * implemented by the function that accesses this one.
   * 
   * Note that if an an object is moved to mind's eye coordinates that are
   * already occupied then the two objects will co-exist on the coordinates; the 
   * new object does not overwrite the old object.
   * 
   * @param objectMoves A 2D ArrayList whose first dimension elements 
   * should contain ArrayLists of {@link jchrest.lib.ItemSquarePattern} 
   * instances that prescribe a sequence of moves for one object in the domain
   * using visual-spatial field specific coordinates, i.e. the minimum x and y
   * coordinate should be 0.  For example, if two objects, A and B, are to be 
   * moved the ArrayList passed should contain: 
   * [[A sourceX sourceY], [A destinationX desitinationY]], 
   * [[B sourceX sourceY], [B desitinationX destinationY]].
   * 
   * @param time The current time (in milliseconds) in the domain when object
   * movement is requested.
   * 
   * @throws jchrest.lib.MindsEyeMoveObjectException If any of the moves passed
   * cause any of the following statements to be evaluated as true:
   * <ol type="1">
   *  <li>
   *    More than one object is moved at once; object movement should be 
   *    strictly serial.
   *  </li>
   *  <li>
   *    An object's first "move" does not correctly identify where the object 
   *    is currently located in the mind's eye.  If the object has previously 
   *    been moved in the mind's eye but not in physical space, the initial 
   *    location passed for the object should be its current coordinates in the 
   *    mind's eye.
   *  </li>
   *  <li>
   *    Only the initial location of an object is specified.
   *  </li>
   * </ol>
   */
  public void moveObjects(ArrayList<ArrayList<ItemSquarePattern>> objectMoves, int time) throws MindsEyeMoveObjectException {
    
    //Check that attention is free, if so, continue.
    if(this._model.attentionFree(time)){
      
      //Clone the current contents of "_visualSpatialField" and the current 
      //value of the mind's eye terminus so that if any moves are illegal, all 
      //changes made up until the illegal move can be reversed.
      ArrayList<ArrayList<ArrayList<MindsEyeObject>>> visualSpatialFieldBeforeMovesApplied = new ArrayList<>();
      for(int col = 0; col < this._visualSpatialField.size(); col++){
        visualSpatialFieldBeforeMovesApplied.add(new ArrayList<>());
        for(int row = 0; row < this._visualSpatialField.get(col).size(); row++){
          visualSpatialFieldBeforeMovesApplied.get(col).add(new ArrayList<>());
          for(int object = 0; object < this._visualSpatialField.get(col).get(row).size(); object++){
            MindsEyeObject original = this._visualSpatialField.get(col).get(row).get(object);
            MindsEyeObject clone = original.createClone();
            visualSpatialFieldBeforeMovesApplied.get(col).get(row).add(clone);
          }
        }
      }
      
      //Tracks the time taken so far to process the object moves.  Used to 
      //assign terminus values for moved objects and the time that the attention
      //of the CHREST model associated with this mind's eye will be free.
      int timeTakenToMoveObjects = time + this._accessTime;
      
      //Process each object move sequence.
      try{
        for(int objectMoveSequence = 0; objectMoveSequence < objectMoves.size(); objectMoveSequence++){

          //Get the first move sequence for an object and check to see if at 
          //least one movement has been specified for it.
          ArrayList<ItemSquarePattern> moveSequence = objectMoves.get(objectMoveSequence);
          if(moveSequence.size() >= 2){

            //Extract the source information for the object to move.
            ItemSquarePattern currentObjectLocation = moveSequence.get(0);

            //Process each move for this object starting from the first element of 
            //the current second dimension array.
            for(int movement = 1; movement < moveSequence.size(); movement++){
              
              //Get the destination info for the object.
              ItemSquarePattern destinationInfo = moveSequence.get(movement);
              
              //Check to see if the object in the destination info is the object 
              //originally specified in the first element of the move sequence.
              //If it isn't, serial movement is not implemented so the entire
              //move sequence should fail.
              if( currentObjectLocation.getItem().equals(destinationInfo.getItem()) ){

                //Check to see if the object to be moved is currently at the
                //location specified.  If the previous move caused the object to 
                //be placed on a blind spot or the item is on the square but its
                //terminus has passed, this check will return false.
                ListPattern currentObjectCoordinateContents = this.getVisualSpatialFieldAsScene(timeTakenToMoveObjects).getItemsOnSquare(currentObjectLocation.getColumn(), currentObjectLocation.getRow(), false, false);
                if(currentObjectCoordinateContents.contains(currentObjectLocation)){

                  //Remove the object from its current coordinates in
                  //"_visualSpatialField" by setting its terminus to the current 
                  //time.  Update the terminus values of any objects on this
                  //location since they will have also been "looked at" so long
                  //as they aeren't blind squares that already have their 
                  //terminus set and the object's terminus hasn't already been 
                  //reached.
                  ArrayList<MindsEyeObject> currentObjectLocationContents = this._visualSpatialField.get(currentObjectLocation.getColumn()).get(currentObjectLocation.getRow());
                  for(MindsEyeObject object : currentObjectLocationContents){
                    if(object.getIdentifier().equals(currentObjectLocation.getItem())){
                      object.setTerminus(timeTakenToMoveObjects, true);
                    }
                    else if(
                      (
                        !object.getIdentifier().equals(Scene.getBlindSquareIdentifier()) && 
                        object.getTerminus() != null
                      ) &&
                      object.getTerminus() > timeTakenToMoveObjects
                    ){
                      object.setTerminus(timeTakenToMoveObjects, false);
                    }
                  }

                  //Increment the time tracker variable by the time taken to 
                  //move the object.  Do this now since it should still take 
                  //time to move an object even if it is moved to a blind spot.
                  timeTakenToMoveObjects += this._objectMovementTime;

                  //Check to see if the mind's eye coordinates that were 
                  //resolved above are both represented in the visual spatial 
                  //field and aren't a blind spot.  If both conditions are true, 
                  //add the object identifier to the new mind's eye coordinates.  
                  //Otherwise, don't.
                  if(
                    (destinationInfo.getColumn() < this._sceneTransposed.getWidth() && destinationInfo.getRow() < this._sceneTransposed.getHeight() ) &&
                    !this.getVisualSpatialFieldAsScene(timeTakenToMoveObjects).isSquareBlind(destinationInfo.getColumn(), destinationInfo.getRow())
                  ){
                    
                    //Update the termini of objects on the destination square
                    //since they have been "looked" at.  Do not do this for 
                    //blind squares whose termini are already set and "dead" 
                    //objects (objects whose termini have already expired).
                    Iterator<MindsEyeObject> destinationSquareContents = this._visualSpatialField.get(destinationInfo.getColumn()).get(destinationInfo.getRow()).iterator();
                    while(destinationSquareContents.hasNext()){
                      MindsEyeObject object = destinationSquareContents.next();
                      if(
                        (
                          !object.getIdentifier().equals(Scene.getBlindSquareIdentifier()) && 
                          object.getTerminus() != null
                        ) &&
                        object.getTerminus() > timeTakenToMoveObjects
                      ){
                        object.setTerminus(timeTakenToMoveObjects, false);
                      }
                    }
                    
                    //Now, "move" the object to be moved to its destination 
                    //coordinates.
                    this._visualSpatialField.get(destinationInfo.getColumn()).get(destinationInfo.getRow()).add(new MindsEyeObject(this, destinationInfo.getItem(), timeTakenToMoveObjects));
                  }
                  
                  //Set the current location of the object to be its destination 
                  //so that the next move can be processed correctly.
                  currentObjectLocation = destinationInfo;
                }
                //The object is not at the location specified.
                else{
                  
                  //If this is the first movement then the actual object 
                  //location specification is incorrect so throw an exception 
                  //since this may indicate an issue with coordinate translation
                  //or experiment code.
                  if(movement == 1){
                    throw new MindsEyeMoveObjectException(
                      "The initial location specified for object " + currentObjectLocation.getItem() + " (" + currentObjectLocation.toString() + ") is incorrect.\n"
                        + "This may be because domain-specific coordinates have been used to specify the location.");                
                  }
                  //Otherwise, the object has decayed since a number of other
                  //moves have been performed or, it has been moved to a blind
                  //square in a previous move so start to process the next 
                  //object move set.
                  else{
                    break;
                  }
                }
              }
              //The object being moved is not the original object specified.
              else {
                throw new MindsEyeMoveObjectException("Object " + destinationInfo.getItem() + " is not the object initially specified for this move sequence: " + destinationInfo.getItem() + ".");
              }
            }//End move for an object.
          }//End check for number of object moves being greater than or equal to 2.
          else{
            throw new MindsEyeMoveObjectException("The move sequence " + moveSequence.toString() + " does not contain any moves after the current location of the object is specified.");
          }
        }//End entire movement sequence for all objects.

        this._model.setAttentionClock(timeTakenToMoveObjects);
      } 
      catch (MindsEyeMoveObjectException e){
        this._visualSpatialField = visualSpatialFieldBeforeMovesApplied;
        throw e;
      }
    }
  }
}
