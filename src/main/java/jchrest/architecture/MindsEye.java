package jchrest.architecture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.MindsEyeMoveObjectException;
import jchrest.lib.MindsEyeObject;
import jchrest.lib.Modality;
import jchrest.lib.PrimitivePattern;
import jchrest.lib.Scene;

/**
 * Class that implements the "Mind's Eye", specifically one that handles
 * <i>attention-based imagery</i> (see page 301 of "Image and Brain" by Stephen
 * Kosslyn).
 * 
 * Package-access only.
 * 
 * The mind's eye is implemented as a 2D ArrayList whose size is finite after
 * creation (consistent with the view proposed by Kosslyn on page 305 of "Image 
 * and Brain)".  The minds eye contains a visual-spatial field that represents 
 * the vision of the observer in the domain whose CHREST model is associated 
 * with a mind's eye instance.
 * 
 * Information in the mind's eye can be manipulated independently of the
 * environment that the observer is currently situated in to test outcomes of 
 * actions without incurring these outcomes in "reality".
 * 
 * TODO: After instantiation, the access time may decrease depending upon how
 *       many times the image has been re-encoded (see page 307 of "Image and
 *       Brain" by Kosslyn).
 * TODO: The size of the visual-spatial field may be finite before creation 
 *       according to Kosslyn (proposed that reliable encoding of object 
 *       locations occurs when the matrix is 3 x 3, anything larger causes 
 *       object encoding to become unreliable and subject to error).
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
class MindsEye {
  
  //Time taken (ms) to access the mind's eye.
  private final int _accessTime;
  
  //The length of time (in milliseconds) that the minds eye exists for after
  //it is created/accessed.
  private final int _lifespan;
  
  //The domain time (in milliseconds) when the minds eye will be cleared.
  private int _terminus;
  
   //The CHREST model that the mind's eye instance is associated with.
  private final Chrest _model;
  
  //The time taken (in milliseconds) to move an object in the mind's eye.
  private final int _objectMovementTime;
  
  //The time taken (in milliseconds) to place an object in the mind's eye during
  //instantiation.
  private final int _objectPlacementTime; //Look in Fernand's paper with Waters for time of this.
  
  //The visual spatial field of the mind's eye (a 2D ArrayList).  First 
  //dimension elements represent the rows (y-coordinates) of a scene, second
  //dimension elements represent the columns (x-coordinates) of a scene.  Thus,
  //this instance variable should be able to transpose Scene instance contents
  //easily.
  private ArrayList<ArrayList<MindsEyeObject>> _visualSpatialField = new ArrayList<>();
  
  
  /**
   * Constructor for "MindsEye" object.
   * 
   * @param model The CHREST model instance that the mind's eye instance is 
   * associated with.
   * 
   * @param vision An instance of {@link jchrest.lib.Scene}, usually the one 
   * last used by the 
   * {@link jchrest.architecture.Chrest#learnScene(jchrest.lib.Scene, int)},
   * {@link jchrest.architecture.Chrest#learnSceneAndMove(jchrest.lib.Scene, jchrest.lib.Move, int)} or
   * {@link jchrest.architecture.Chrest#scanScene(jchrest.lib.Scene, int)}
   * functions.
   * 
   * @param lifespan The length of time (in milliseconds) that the mind's eye
   * exists for after creation/access.
   * 
   * @param objectPlacementTime The length of time (in milliseconds) that it 
   * takes to place an object in the mind's eye during instantiation.
   * 
   * @param accessTime The time taken (in milliseconds) to access the mind's eye 
   * when the 
   * {@link jchrest.architecture.MindsEye#moveObjects(java.util.ArrayList, int)  
   * function is used.
   * 
   * @param objectMovementTime The time taken (in milliseconds) to move an 
   * object in the mind's eye when the 
   * {@link jchrest.architecture.MindsEye#moveObjects(java.util.ArrayList, int)  
   * function is used.
   * 
   * @param domainTime The current time (in milliseconds) in the domain where 
   * the CHREST model associated with the mind's eye instance is located.
   * 
   * @param terminusForRecognisedObject The length of time (in milliseconds) 
   * that an object will exist in the mind's eye for when it is created or 
   * interacted with if it is committed to LTM.
   * 
   * @param terminusForUnrecognisedObject The length of time (in milliseconds) 
   * that an object will exist in the mind's eye for when it is created or 
   * interacted with if it is not committed to LTM.
   */
  
  //TODO: I wonder if this should be placed in jchrest.architecture.Perceiver
  //since the MindsEye should only be created/modified after scanning a scene.
  //This reduces the possibility of "hangovers" when identifying what objects
  //should have improved terminus times in the mind's eye.
  public MindsEye(Chrest model, Scene currentScene, int lifespan, int objectPlacementTime, int accessTime, int objectMovementTime, int domainTime, int terminusForRecognisedObject, int terminusForUnrecognisedObject){
//  public MindsEye(Chrest model, TwoDimensionalMindsEyeObject [] vision, int lifespan, int objectPlacementTime, int accessTime, int objectMovementTime, int domainTime){
    
    this._model = model;
    this._accessTime = accessTime;
    this._lifespan = lifespan;
    this._objectMovementTime = objectMovementTime;
    this._objectPlacementTime = objectPlacementTime;
    
    //Create the visual-spatial field using currentScene as a basis.  The field
    //will be empty at first.
    this._visualSpatialField = new ArrayList<>();
    for(int row = 0; row < currentScene.getHeight(); row++){
      this._visualSpatialField.add(new ArrayList<>());
      for(int col = 0; col < currentScene.getWidth(); col++){
        this._visualSpatialField.get(row).add(null);
      }
    }
    
    //Get visual STM so that we know what objects should have superior terminus
    //times.
    Iterator<Node> visualStm = this._model.getVisualStm().iterator();
    ListPattern scene = currentScene.getScene();
    ArrayList<PrimitivePattern> patternsInScenePresentInVisualStm = new ArrayList<>();
    int multiplierForObjectPlacementTime = 0;
    
    //Populate visual spatial field using visual STM chunks.
    while(visualStm.hasNext()){
      
      //If this chunk contains one or more patterns that are present in the 
      //current scene, only 1 should be added to the multiplier for object 
      //placement time. So, to determine whether this condition should apply, 
      //the boolean flag below is used since it may be the case that none of the
      //chunks in visual STM apply to the current scene.
      boolean patternInChunkPresentInScene = false;
      
      //Process the chunk.
      Iterator<PrimitivePattern> chunkPatterns = visualStm.next().getImage().iterator();
      while(chunkPatterns.hasNext()){
        PrimitivePattern chunkPattern = chunkPatterns.next();
        if(chunkPattern instanceof ItemSquarePattern){
          ItemSquarePattern pattern = (ItemSquarePattern)chunkPattern;
          if(scene.contains(pattern)){
            //Add a new MindsEyeObject to the visual spatial field with a superior 
            //terminus value.
            this._visualSpatialField.get(pattern.getRow()).set(pattern.getColumn(), new MindsEyeObject(pattern.getItem(), terminusForRecognisedObject));
            patternsInScenePresentInVisualStm.add(pattern);
            patternInChunkPresentInScene = true;
          }
        }
      }
      
      if(patternInChunkPresentInScene){
        multiplierForObjectPlacementTime++;
      }
    }
    
    //Populate visual spatial field using information from the current scene 
    //that isn't present in visual STM.  Placing such objects incurs a time cost
    //for each object so multiplierForObjectPlacementTime is incremented by 1
    //after each object is placed.
    Iterator<PrimitivePattern> sceneContents = scene.iterator();
    while(sceneContents.hasNext()){
      ItemSquarePattern sceneObject = (ItemSquarePattern)sceneContents.next();
      if(!patternsInScenePresentInVisualStm.contains(sceneObject)){
        this._visualSpatialField.get(sceneObject.getRow()).set(sceneObject.getColumn(), new MindsEyeObject(sceneObject.getItem(), terminusForUnrecognisedObject));
        multiplierForObjectPlacementTime++;
      }
    }
    
    this._model.setAttentionClock(domainTime + (objectPlacementTime * multiplierForObjectPlacementTime) );
    this._terminus = (this._model.getAttentionClock() + lifespan);
    //TODO: may have to set terminus values for visual-spatial objects here 
    //instead of the exact moment when they are placed in the visual-spatial 
    //field. 
    
    /**************************************************************************/
    /**************************************************************************/
    /******************************* OLD METHOD *******************************/
    /**************************************************************************/
    /**************************************************************************/
//    //Used to determine sizes of ArrayList dimensions that consitute the 
//    //"_visualSpatialField" data structure.
//    ArrayList<Integer> domainSpecificXCoordinates = new ArrayList<>();
//    ArrayList<Integer> domainSpecificYCoordinates = new ArrayList<>();
//    
//    //Extract all domain-specific x and y coordinates so that the visual spatial
//    //field can be instantiated.
////    for (String visionUnit : vision) {
//////    for (TwoDimensionalMindsEyeObject object: vision){
//////      int domainSpecificXCor = object.getDomainSpecificXCor();
//////      int domainSpecificYCor = object.getDomainSpecificYCor();
////      String[] visionUnitInfo = processObjectInfo(visionUnit);
////      int domainSpecificXCor = Integer.valueOf(visionUnitInfo[1]);
////      int domainSpecificYCor = Integer.valueOf(visionUnitInfo[2]);
//////      
////      if( !domainSpecificXCoordinates.contains(domainSpecificXCor) ){
////        domainSpecificXCoordinates.add(domainSpecificXCor);
////      }
////      
////      if( !domainSpecificYCoordinates.contains(domainSpecificYCor) ){
////        domainSpecificYCoordinates.add(domainSpecificYCor);
////      }
////    }
////    }
//    
//    //Sort x and y coordinate lists into ascending order, this ensures that the
//    //minimum domain-specific x/y-coordinates are found at coordinates 0,0 in 
//    //the mind's eye and all subsequent domain-specific coordinates are found
//    //in ascending order in the mind's eye.
//    Collections.sort(domainSpecificXCoordinates);
//    Collections.sort(domainSpecificYCoordinates);
//    
//    //Instantiate the "_visualSpatialField" with null values and generate the 
//    //domain-specific to/from mind's eye coordinate mappings.
//    for(int mindsEyeXCor = 0; mindsEyeXCor < domainSpecificXCoordinates.size(); mindsEyeXCor++){
//      ArrayList<ArrayList<MindsEyeObject>> yCorSpace = new ArrayList<>();
//      this._visualSpatialField.add(yCorSpace);
//
//      for(int mindsEyeYCor = 0; mindsEyeYCor < domainSpecificYCoordinates.size(); mindsEyeYCor++){  
//        this._visualSpatialField.get(mindsEyeXCor).add(null);  
//        
//        //Generate domain -> minds eye/minds eye -> domain coordinate mappings.
//        String domainXAndYCor = String.valueOf(domainSpecificXCoordinates.get(mindsEyeXCor)) + "," + String.valueOf(domainSpecificYCoordinates.get(mindsEyeYCor));
//        String mindsEyeXandYCor = String.valueOf(mindsEyeXCor) + "," + String.valueOf(mindsEyeYCor);
//        this._domainSpecificToMindsEyeCoordMappings.put(domainXAndYCor, mindsEyeXandYCor);
//        this._mindsEyeToDomainSpecificCoordMappings.put(mindsEyeXandYCor, domainXAndYCor);
//      }
//    }
//    
//    //Populate "_visualSpatialField" with the objects at each of the coordinates 
//    //specified in the "vision" string array passed, create the vision pattern
//    //to be recognised by LTM and count how many objects are present in the 
//    //vision in total (coordinates that do not contain any objects are not 
//    //included in this count).  This value will be used to calculate the time 
//    //cost of placing objects in the mind's eye.
//    int numberOfObjects = 0;
//    //ListPattern visionPattern = new ListPattern(Modality.VISUAL);
//    
//    for(String visionUnit : vision){
////    for (TwoDimensionalMindsEyeObject objectBeingProcessed: vision){
//      String[] visionUnitInfo = this.processObjectInfo(visionUnit);
//      String objectInfo = visionUnitInfo[0];
//      int domainXCor = Integer.parseInt(visionUnitInfo[1]);
//      int domainYCor = Integer.parseInt(visionUnitInfo[2]);
////      String objectInfo = objectBeingProcessed.getIdentifier();
////      int domainXCor = objectBeingProcessed.getDomainSpecificXCor();
////      int domainYCor = objectBeingProcessed.getDomainSpecificYCor();
//      
//      //Check to see if "objectInfo" is not empty, if not, the number of objects
//      //should be counted and the object information should be added to the
//      //visual pattern to be recognised by LTM.
//      if(!objectInfo.isEmpty()){
//        
//        ArrayList<MindsEyeObject> mindsEyeObjects = new ArrayList<>();
//        
//        //Multiple objects have been specified.
//        if(objectInfo.contains(",")){
//           
//          //Add all objects and their respective coordinates to the pattern that
//          //will be passed to LTM.
//          for(String object : objectInfo.split(",")){
//            
//            //Try to recognise the object so that the terminus value for its
//            //MindsEyeObject representation can be set correctly.
//            ListPattern visionPattern = new ListPattern(Modality.VISUAL);
//            visionPattern.add(new ItemSquarePattern(object, domainXCor, domainYCor));
//            Node resultOfRecognise = this._model.recognise(visionPattern);
//            
//            //If a root node has been returned then the object has not been 
//            //committed to LTM.
//            if(resultOfRecognise == this._model.getLtmByModality (visionPattern)){
//              MindsEyeObject mindsEyeObject = new MindsEyeObject(objectInfo, domainTime + 100); //TODO: Determine correct terminus value.
//              mindsEyeObjects.add(mindsEyeObject);
//            }
//            else{
//              MindsEyeObject mindsEyeObject = new MindsEyeObject(objectInfo, domainTime + 1000); //TODO: Determine correct terminus value.
//              mindsEyeObjects.add(mindsEyeObject);
//            }
//            
//            
//            //Count the object
//            numberOfObjects++;  
//            
//            //visionPattern.add( new ItemSquarePattern(object, domainXCor, domainYCor) );
//          }
//          
//          //Place the objects in the mind's eye.
//          int[] mindsEyeXYCoords = resolveDomainSpecificCoord(domainXCor, domainYCor);
//          this._visualSpatialField.get( mindsEyeXYCoords[0] ).set(mindsEyeXYCoords[1], mindsEyeObjects);
//        }
//        //Only one object specified.
//        else{
//          
//          MindsEyeObject object = new MindsEyeObject(objectInfo, domainTime);
//          
//          //Place the object in the mind's eye.
//          int[] mindsEyeXYCoords = resolveDomainSpecificCoord(domainXCor, domainYCor);
//          //this._visualSpatialField.get( mindsEyeXYCoords[0] ).set(mindsEyeXYCoords[1], object);
//          
//          //Count the object
//          numberOfObjects++;
//          
//          //Add the object along with its respective coordinates to the pattern
//          //that will be passed to LTM.
//          //visionPattern.add( new ItemSquarePattern( objectInfo, domainXCor , domainYCor ) );
//        }
//      }
//    }
//    
//    //Pass the pattern constructed above to LTM, attempt to recognise it, 
//    //extract the chunk in the retrieved node and count how many patterns are 
//    //contained within it.  This value will be used to determine if a reduction 
//    //in time cost associated with object placement can be applied.
//    //int numberOfRecognisedPatterns = this._model.recognise(visionPattern).getImage().size();
//    
//    //Set the multiplier for object placement time to the total number of 
//    //objects placed since, if there is no time cost discount to be applied as
//    //a result of recognising chunks in the visual information used to 
//    //instantiate the minds eye, the total time cost will be equal to the total 
//    //number of objects placed multiplied by the value of the 
//    //"_objectPlacementTime" instance variable.
//    int multiplierForObjectPlacementTime = numberOfObjects;
//    
//    //Check to see if the number of recognised patterns is greater than 1.  If
//    //this is the case, the time it takes to place the objects in question is
//    //reduced since placing a chunk containing 3 objects only incurs a time 
//    //cost equal to the value specified in the "_objectPlacementTime" instance
//    //variable.  This reduction of time is only valid if the number of patterns
//    //recognised in a chunk of vision is greater than 1 since, if zero patterns
//    //are recognised or if one pattern is recognised, the total time taken to 
//    //place all objects from the vision in the mind's eye does not change.
//    //However, if there are 5 objects in total that need to be placed but a 
//    //chunk is recognised that contains 3 of these objects then the total time
//    //cost incurred is 3 * _objectPlacementTime since placement of a chunk of 
//    //recognised objects is treated as one object in addition to the two objects 
//    //that are unrecognised (hence the addition of 1 below too).
////    if(numberOfRecognisedPatterns > 1){
////      multiplierForObjectPlacementTime = (multiplierForObjectPlacementTime - numberOfRecognisedPatterns) + 1;
////    }
//    
//    //Set the attention clock of the CHREST model
//    //this._model.setAttentionClock(domainTime + (this._objectPlacementTime * multiplierForObjectPlacementTime));
//    
//    //Set the initial "_mindsEyeTerminus" instance variable value to the sum of 
//    //the current value of the "_attentionClock" of the CHREST model associated 
//    //with the mind's eye instance and the value of the "_mindsEyeLifespan"
//    //instance variable value since the mind's eye should initially expire after
//    //it has been instantiated and its lifespan has passed.
//    //this.setTerminus(this._model.getAttentionClock());
//    //this.terminusForRecognisedObject = terminusForRecognisedObject;
  }
  
  /**
   * Checks the value of the "domainTime" parameter passed against the value of
   * the "_mindsEyeTerminus" instance variable value.
   * 
   * @param domainTime The current time (in milliseconds) in the domain where 
   * the CHREST model associated with the mind's eye instance is located.
   * 
   * @return True if the "_mindsEyeTerminus" value is greater than the value 
   * of the "domainTime" parameter passed, false if not.
   */
  public boolean exists(int domainTime){
    return this.getTerminus() > domainTime;
  }
  
  /**
   * Accessor for the "_mindsEyeTerminus" instance variable.
   * 
   * @return The current value of the "_mindsEyeTerminus" instance variable.
   */
  public int getTerminus(){
    return this._terminus;
  }
  
  /**
   * Sets the mind's eye terminus value to the (domain) time specified.
   * 
   * @param time The (domain) time that the "_mindsEyeLifespan" should be added 
   * to.
   */
  private void setTerminus(int time){
    int newMindsEyeTerminus = time + this._lifespan;
    if( newMindsEyeTerminus > this._terminus ){
      this._terminus = newMindsEyeTerminus;
    }
  }
  
  private Scene getMindsEyeScene(int time){
    Scene mindsEyeScene = new Scene("Mind's eye scene", this._visualSpatialField.size(), this._visualSpatialField.get(0).size());
    if(this.exists(time)){
    
      for(int row = 0; row < this._visualSpatialField.size(); row++){
        for(int col = 0; col < this._visualSpatialField.get(row).size(); col++){
          MindsEyeObject object = this._visualSpatialField.get(row).get(col);
          if(object != null && object.getTerminus() < time){
            mindsEyeScene.setItem(row, col, object.getIdentifier());
          }
        }
      }
    }
    
    return mindsEyeScene;
  }
  
  public void scanMindsEyeScene(int time, int numberFixations){
    if(this.exists(time)){
      this._model.scanScene(this.getMindsEyeScene(time), numberFixations);
    }
  }
  
  public void learnMindsEyeScene(int time, int numberFixations){
    if(this.exists(time)){
      this._model.learnScene(this.getMindsEyeScene(time), numberFixations);
    }
  }
  
  /**
   * Retrieves the entire contents of the mind's eye along in relation to domain
   * specific coordinates. This operation does not incur a time cost.
   * 
   * @param domainTime The current time (in milliseconds) in the domain where 
   * the CHREST model associated with the mind's eye instance is located.
   * 
   * @return Null if the mind's eye does not exist, otherwise, an ArrayList of 
   * strings containing the contents of each mind's eye coordinate along with 
   * the relevant domain-specific x and y coordinates.  Object, x-cor and y-cor 
   * information are separated by a semi-colon (;) and so can be used as a 
   * delimiter to split information.  Commas (,) are used to separate object 
   * identifiers in mind's eye content so can be used as a 
   * delimiter to extract object information.
   * 
   * TODO: should this incur a time cost and if so, should it be longer than 
   *       the time cost incurred by getting content from specific coordinates?
   * TODO: should coordinates further from the object thatthe mind's eye belongs
   *       to take longer to retrieve (see chapter 9 of "Image and Brain" by 
   *       Kosslyn).
   */
//  public ArrayList<String> getAllContent(int domainTime){
//    ArrayList<String> mindsEyeContent = null;
//    
//    if( this.exists(domainTime) && this._model.attentionFree(domainTime) ){
//      mindsEyeContent = new ArrayList<>();
//      for(int mindsEyeXCor = 0; mindsEyeXCor < this._visualSpatialField.size(); mindsEyeXCor++){
//        ArrayList rowArray = this._visualSpatialField.get(mindsEyeXCor);
//        for(int mindsEyeYCor = 0; mindsEyeYCor < rowArray.size(); mindsEyeYCor++){
//          String[] domainSpecificXAndYCoordinates = this._mindsEyeToDomainSpecificCoordMappings.get(Integer.toString(mindsEyeXCor) + "," + Integer.toString(mindsEyeYCor)).split(",");
//          String domainSpecificXCorString = domainSpecificXAndYCoordinates[0];
//          String domainSpecificYCorString = domainSpecificXAndYCoordinates[1];
//          String contents = this.getSpecificContent(Integer.valueOf(domainSpecificXCorString), Integer.valueOf(domainSpecificYCorString), domainTime);
//          mindsEyeContent.add(contents + ";" + domainSpecificXCorString + ";" + domainSpecificYCorString);
//        }
//      }
//      
//      //The "_mindsEyeTerminus" value is not modified in this action since the
//      //call to the "getMindsEyeContentUsingDomainSpecificCoords()" method 
//      //performs this task.
//    }
//    
//    return mindsEyeContent;
//  }
  
  /**
   * Takes domain-specific x and y coordinates and returns the object identifier 
   * found at the resolved mind's eye x and y coordinates.  This operation does
   * not incur a time cost.
   * 
   * @param domainSpecificXCor
   * 
   * @param domainSpecificYCor
   * 
   * @param domainTime The current time (in milliseconds) in the domain where 
   * the CHREST model associated with the mind's eye instance is located.
   * 
   * @return Null if the mind's eye does not exist, otherwise, a string 
   * containing the current contents of the minds eye coordinates that are 
   * equivalent to the domain coordinates specified.  Commas (,) are used to 
   * separate object identifiers so can be used as a delimiter to extract object 
   * information.
   */
//  public String getSpecificContent(int domainSpecificXCor, int domainSpecificYCor, int domainTime){
//    String mindsEyeContent = null;
//    
//    if( this.exists(domainTime) && this._model.attentionFree(domainTime) ){
//      int[] mindsEyeXAndYCoords = this.resolveDomainSpecificCoord(domainSpecificXCor, domainSpecificYCor);
//      
//      //The "String.valueOf()" call ensures that, if the domain-specific 
//      //coordinates are not represented in the mind's eye and null is returned,
//      //this null value is converted to "null" i.e. a string.  This 
//      //differentiates between the possible conflation of non-string and string
//      //nulls in the local "mindsEyeContent" variable.
//      mindsEyeContent = String.valueOf(this._visualSpatialField.get( mindsEyeXAndYCoords[0] ).get( mindsEyeXAndYCoords[1] ));
//      
//      this.setTerminus(domainTime);
//    }
//    
//    return mindsEyeContent;
//  }
  
  /**
   * Moves objects in the mind's eye according to the sequence of moves passed
   * in as a parameter to this function if the domain time at which this method
   * is called is earlier than the "_terminus" value of the mind's eye and the
   * attention of the CHREST model associated with this mind's eye instance is
   * free.  
   * 
   * If all moves are successful, the clock of the CHREST model associated with 
   * this mind's eye will be advanced by the product of: 
   * this._accessTime + (this._movementTime * total number of moves).
   * 
   * Note that if an an object is moved to mind's eye coordinates that are
   * already occupied then the two objects will co-exist on the coordinates; the 
   * new object does not overwrite the old object.
   * 
   * @param domainSpecificMoves A 2D ArrayList whose first dimension elements 
   * should contain ArrayLists whose elements should be strings that prescribe a 
   * sequence of moves for one object in the domain by specifying 
   * domain-specific coordinates.  For example, if two objects, A and B, are to 
   * be moved from domain specific x/y coordinates 0/1 and 0/2 to 1/1 and 1/2 
   * respectively, the ArrayList passed should contain: 
   * [ ["A;0;1", "A;1;1"], ["B;0;2","B;1;2"] ].  See the discussion of the 
   * function's caveats above for further implementation and usage details of 
   * this parameter.
   * 
   * @param domainTime The current time (in milliseconds) in the domain where 
   * the CHREST model associated with the mind's eye instance is located.
   * 
   * @return Boolean False if the minds eye does not exist or the attention of
   * the CHREST model isn't free when this function is called according to the 
   * value of the domainTime parameter passed.  Boolean true if mind's eye 
   * exists, the attention of the CHREST model is free and all moves specified 
   * are legal.
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
   *    is currently located in the mind's eye (using domain-specific 
   *    coordinates).  If the object has previously been moved in the mind's eye 
   *    then the initial location passed for the object should be relative to 
   *    its current coordinates in the mind's eye.
   *  </li>
   *  <li>
   *    Only the initial location of an object is specified.
   *  </li>
   *  <li>
   *    After moving an object to coordinates not represented in the mind's eye,
   *    further moves are attempted with this object.
   *  </li>
   * </ol>
   */
//  public boolean moveObjects(ArrayList<ArrayList<String>> domainSpecificMoves, int domainTime) throws MindsEyeMoveObjectException {
//    
//    //Indicates whether objects have all been moved successfully.
//    boolean moveObjectsSuccessful = false;
//    
//    if(this.exists(domainTime) && this._model.attentionFree(domainTime)){
//      
//      //Copy the current contents of "_visualSpatialField" before any moves are
//      //applied so that if any object's move is illegal, all changes made to 
//      //"_visualSpatialField" up until the illegal move can be reversed.
//      ArrayList<ArrayList<String>> visualSpatialFieldBeforeMovesApplied = new ArrayList<>();
//      for(int mindsEyeXCor = 0; mindsEyeXCor < this._visualSpatialField.size(); mindsEyeXCor++){
//        visualSpatialFieldBeforeMovesApplied.add(new ArrayList<>());
//        for(int mindsEyeYCor = 0; mindsEyeYCor < this._visualSpatialField.get(mindsEyeXCor).size(); mindsEyeYCor++){
//          visualSpatialFieldBeforeMovesApplied.get(mindsEyeXCor).add( String.valueOf( this._visualSpatialField.get(mindsEyeXCor).get(mindsEyeYCor) ) );
//        }
//      }
//
//      int mindsEyeTerminusBeforeMovesApplied = this.getTerminus();
//      
//      try{
//
//        //Counter for how many moves have been applied - acts as a multiplier for 
//        //the "_movementTime" parameter multiplicand so that the clock of the CHREST
//        //model associated with this mind's eye can be incremented correctly.
//        int movesApplied = 0;
//
//        //Process each object's move sequence.
//        for(int objectMoveSequence = 0; objectMoveSequence < domainSpecificMoves.size(); objectMoveSequence++){
//
//          ArrayList<String> objectMoves = domainSpecificMoves.get(objectMoveSequence);
//          //Check to see if at least one move has been specified for an object along
//          //with information regarding its current location in the domain.
//          if(objectMoves.size() >= 2){
//
//            //Extract the initial information for the object
//            String[] initialDomainSpecificObjectInformation = this.processObjectInfo(objectMoves.get(0));
//            String initialObjectIdentifier = initialDomainSpecificObjectInformation[0];
//
//            //Check that only one object has been specified to be moved.
//            if(initialObjectIdentifier.split(",").length == 1){
//
//              //Set the "currentMindsEyeXCor" and "currentMindsEyeYCor" values to
//              //the relevant minds eye xcor and ycor values after translating
//              //domain-specific xcor and ycor values.
//              int currentDomainSpecificXCor = Integer.valueOf(initialDomainSpecificObjectInformation[1]);
//              int currentDomainSpecificYCor = Integer.valueOf(initialDomainSpecificObjectInformation[2]);
//              int[] initialMindsEyeCoords = this.resolveDomainSpecificCoord(currentDomainSpecificXCor, currentDomainSpecificYCor);
//              int currentMindsEyeXCor = initialMindsEyeCoords[0];
//              int currentMindsEyeYCor = initialMindsEyeCoords[1];
//
//              //Process each move for this object starting from the first element of 
//              //the current second dimension array.
//              for(int move = 1; move < objectMoves.size(); move++){
//
//                String objectMove = objectMoves.get(move);
//
//                //Extract domain specific move information.
//                String[] domainSpecificMoveInformation = this.processObjectInfo(objectMove);
//                String objectToBeMoved = domainSpecificMoveInformation[0];
//
//                //Check to see if the object being moved is the object originally 
//                //specified in the first element of the move sequence.
//                if( initialObjectIdentifier.equals(objectToBeMoved) ){
//
//                  //Check to see if the object is currently located at the current 
//                  //mind's eye x/ycor specified by the move.
//                  String currentMindsEyeCoordinateContents = String.valueOf(this._visualSpatialField.get(currentMindsEyeXCor).get(currentMindsEyeYCor));
//
//                  //Check that the current minds eye coordinate contents contains 
//                  //the object specified initially. If the previous move caused 
//                  //the object to be placed on a blind spot, then this check will 
//                  //return false since the current minds eye coordinates will be
//                  //set to the coordinates specified before the move to the blind
//                  //spot and the object will no longer be at these coordinates.
//                  if(currentMindsEyeCoordinateContents.contains(initialObjectIdentifier)){
//
//                    //Extract domain specific row/col to move to.
//                    int domainXCorToMoveObjectTo = Integer.valueOf(domainSpecificMoveInformation[1]);
//                    int domainYCorToMoveObjectTo = Integer.valueOf(domainSpecificMoveInformation[2]);
//
//                    //Convert domain-specific coordinates to move to into their 
//                    //relevant mind's eye coordinates.
//                    int[] mindsEyeCoordsToMoveTo = this.resolveDomainSpecificCoord( domainXCorToMoveObjectTo, domainYCorToMoveObjectTo);
//
//                    //Remove the object from its current coordinates in 
//                    //"_visualSpatialField" and tidy up any double/leading/trailing
//                    //commas in the contents of the mind's eye row/column that the
//                    //object has been moved from.
//                    
//                    //TODO: Sort this out, just commented to keep compiler happy.
//                    //String mindsEyeCurrentCoordsContentAfterObjectRemoval = this._visualSpatialField.get( currentMindsEyeXCor ).get( currentMindsEyeYCor ).replaceFirst(initialObjectIdentifier, "").replaceAll(",,", ",").replaceAll("^,\\s*|,\\s*$", "");
//                    //this._visualSpatialField.get(currentMindsEyeXCor).set(currentMindsEyeYCor, mindsEyeCurrentCoordsContentAfterObjectRemoval);
//
//                    //Check to see if the mind's eye coordinates that were resolved 
//                    //above are represented in the visual spatial field.  If they 
//                    //are, add the object identifier to the new mind's eye 
//                    //coordinates.
//                    if(mindsEyeCoordsToMoveTo != null){
//
//                      //Extract mind's eye coordinates to move to using 
//                      //domain-specific coordinates to move to.
//                      int mindsEyeXCorToMoveTo = mindsEyeCoordsToMoveTo[0];
//                      int mindsEyeYCorToMoveTo = mindsEyeCoordsToMoveTo[1];
//
//                      //Get the current content of the mind's eye coordinates that 
//                      //the object will be moved to.
//                      String mindsEyeCoordsToMoveToContentBeforeObjectMovement = this.getSpecificContent(domainXCorToMoveObjectTo, domainYCorToMoveObjectTo, domainTime);
//
//                      //Create a blank string to hold the new mind's eye coordinate
//                      //contents that the object will be moved to.
//                      String mindsEyeCoordsToMoveToContentAfterObjectMovement;
//
//                      //Check to see if the mind's eye coordinates to move to 
//                      //content is empty.  If so, simply overwrite the content with
//                      //the object identifier in question otherwise, append the 
//                      //object identifier in question to the current content of the 
//                      //mind's eye coordinates to move to preceeded by a comma.
//                      if(mindsEyeCoordsToMoveToContentBeforeObjectMovement.isEmpty()){
//                        mindsEyeCoordsToMoveToContentAfterObjectMovement = objectToBeMoved;
//                      }
//                      else{
//                        mindsEyeCoordsToMoveToContentAfterObjectMovement = mindsEyeCoordsToMoveToContentBeforeObjectMovement + "," + objectToBeMoved;
//                      }
//
//                      //Set the content of the mind's eye row/col to move to to the
//                      //content specified above.
//                      
//                      //TODO: Sort this out, just commented to keep compiler happy.
//                      //this._visualSpatialField.get(mindsEyeXCorToMoveTo).set(mindsEyeYCorToMoveTo, mindsEyeCoordsToMoveToContentAfterObjectMovement);
//
//                      //Set the values of "currentMindsEyeRow" and 
//                      //"currentMindsEyeCol" to the values of "mindsEyeRowToMoveTo"
//                      //and "mindsEyeColToMoveTo" so that any subsequent moves for
//                      //the object in this sequence will remove the object from the
//                      //correct coordinates in the mind's eye.
//                      currentMindsEyeXCor = mindsEyeXCorToMoveTo;
//                      currentMindsEyeYCor = mindsEyeYCorToMoveTo;  
//                    }
//
//                    //Increment the "movesApplied" counter value by 1 since the
//                    //object to be moved will have been moved from its current 
//                    //coordinates.
//                    movesApplied++;
//                  }
//                  else{
//                    throw new MindsEyeMoveObjectException("For move " + move + " of object " + objectToBeMoved + ", object " + objectToBeMoved + " is not present at the coordinates specified: " + domainSpecificMoveInformation[1] + ", " + domainSpecificMoveInformation[2] + ".  This is either because the object has been moved out of mind's eye range or because the object's specified location is incorrect.");
//                  }
//                }
//                //The object being moved is not the original object specified.
//                else {
//                  throw new MindsEyeMoveObjectException("Object " + objectToBeMoved + " is not the object initially specified for this move sequence: " + initialObjectIdentifier + ".");
//                }
//              }//End second dimension loop
//            }
//            else{
//              throw new MindsEyeMoveObjectException("More than one object has been specified to be moved for coordinates " + initialDomainSpecificObjectInformation[1] + ", " + initialDomainSpecificObjectInformation[1] + ": " + initialObjectIdentifier + "."); 
//            }
//          }//End check for number of object moves being greater than or equal to 2.
//          else{
//            throw new MindsEyeMoveObjectException("The move sequence " + domainSpecificMoves.get(objectMoveSequence) + " does not contain any moves after the current location of the object is specified.");
//          }
//        }//End first dimension loop
//
//        moveObjectsSuccessful = true;
//        this._model.setAttentionClock(domainTime + ( this._accessTime + (movesApplied * this._movementTime) ) );
//        this.setTerminus(this._model.getAttentionClock());
//      } 
//      catch (MindsEyeMoveObjectException e){
//        this.resetTerminusAndVisualSpatialField(visualSpatialFieldBeforeMovesApplied, mindsEyeTerminusBeforeMovesApplied);
//        throw e;
//      }
//    }
//    
//    return moveObjectsSuccessful;
//  }
//  
//  private void resetTerminusAndVisualSpatialField(ArrayList<ArrayList<String>>visualSpatialField, int terminus){
//    //TODO: Sort this out, just commented to keep compiler happy.
//    //this._visualSpatialField = visualSpatialField;
//    this._mindsEyeTerminus = terminus;
//  }
}
