// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.architecture;

import jchrest.domainSpecifics.Fixation;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.Modality;
import jchrest.domainSpecifics.Scene;
import jchrest.lib.Square;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import jchrest.lib.HistoryTreeMap;
import jchrest.domainSpecifics.SceneObject;
import jchrest.lib.PrimitivePattern;

/**
 * Manages storage of {@link jchrest.domainSpecifics.Fixation Fixations} 
 * and some domain-agnostic functionality of {@link 
 * jchrest.domainSpecifics.Fixation} performance.
 * 
 * {@link jchrest.domainSpecifics.Fixation Fixations} are stored in 
 * chronological order of performance.
 * 
 * @author Peter C. R. Lane
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class Perceiver {
  
  private final Chrest _associatedChrestModel;
  private int _fixationFieldOfView = 2;
  
  private final HistoryTreeMap _fixations = new HistoryTreeMap();
  private int _fixationToLearnFrom = 0;

  /**
   * Constructor.
   * 
   * Associates the {@link jchrest.architecture.Chrest} model specified with
   * {@link #this} and initialises the data structure that will maintain the 
   * {@link jchrest.domainSpecifics.chess.fixationTypes.Fixation Fixations} made by {@link #this}. 
   * 
   * @param associatedChrestModel
   * @param time 
   */
  Perceiver (Chrest associatedChrestModel, int time) {
    this._associatedChrestModel = associatedChrestModel;
    
    //Set the first entry in the Fixation data structure to an empty set before
    //the time this constructor is invoked.  Due to the constraints on 
    //HistoryTreeMap data structures, if this Perceiver were to have a Fixation
    //added to the data structure at the same time this Perceiver were created,
    //the new Fixation wouldn't be added.
    this._fixations.put(time - 1, new ArrayList());
  }
  
  /**
   * Appends the specified {@link jchrest.domainSpecifics.Fixation} to the 
   * structure that maintains {@link jchrest.domainSpecifics.Fixation 
   * Fixations} in {@link #this}.
   * <br/><br/>
   * If the specified {@link jchrest.domainSpecifics.Fixation} was performed
   * successfully, the function constructs a {@link jchrest.lib.ListPattern} 
   * containing information about {@link jchrest.domainSpecifics.SceneObject 
   * SceneObjects} in its fixation field of view, normalises this {@link 
   * jchrest.lib.ListPattern} using {@link 
   * jchrest.domainSpecifics.DomainSpecifics#normalise(jchrest.lib.ListPattern)}
   * and attempts to perform the tasks outlined below with it. 
   * <br/><br/>
   * <b>NOTE:</b> when the
   * {@link jchrest.lib.ListPattern} is created, this function takes into 
   * account the result of invoking {@link 
   * jchrest.architecture.Chrest#isLearningObjectLocationsRelativeToAgent()} on 
   * the {@link jchrest.architecture.Chrest} model associated with {@link 
   * #this}.
   * 
   * <br/><br/><i><u>Tasks attempted</u></i>
   * <ol type="1">
   *  <li>
   *    If the {@link jchrest.lib.Modality#VISUAL} {@link 
   *    jchrest.architecture.Stm} hypothesis {@link jchrest.architecture.Node 
   *    Node} of the {@link jchrest.architecture.Chrest} model associated with
   *    {@link #this} is a template then an attempt is made to fill its slots 
   *    using the {@link jchrest.lib.ListPattern} created.
   *  </li>
   *  <li>
   *    The {@link jchrest.lib.ListPattern} created will be passed to {@link 
   *    jchrest.architecture.Chrest#recogniseAndLearn(
   *    jchrest.lib.ListPattern, java.lang.Integer)} for the {@link 
   *    jchrest.architecture.Chrest} model associated with {@link #this}.
   *  </li>
   * </ol>
   * 
   * <b>NOTE:</b> slot filling is attempted before learning since both consume
   * the attention resource of the {@link jchrest.architecture.Chrest} model
   * associated with {@link #this} however, use of existing information should
   * take priority over learning new information.
   * 
   * @param fixation 
   * 
   * @return The {@link jchrest.lib.ListPattern} created from the information
   * contained in the {@link jchrest.domainSpecifics.Fixation} to add's field of 
   * view.  This will be {@code null} if the result of invoking {@link 
   * jchrest.domainSpecifics.Fixation#getPerformanceTime()} on the {@link 
   * jchrest.domainSpecifics.Fixation} to add is {@code null} or earlier than
   * the creation time of {@link #this} or invoking {@link 
   * jchrest.domainSpecifics.Fixation#hasBeenPerformed()} on the {@link 
   * jchrest.domainSpecifics.Fixation} to add returns {@link 
   * java.lang.Boolean#FALSE}.
   * 
   * @throws IllegalStateException If either of the following evaluate to {@link 
   * java.lang.Boolean#TRUE}:
   * 
   * <ul>
   *    <li>
   *      If invoking {@link 
   *      jchrest.domainSpecifics.Fixation#getPerformanceTime()} in context of
   *      the {@link jchrest.domainSpecifics.Fixation} to add is not {@code 
   *      null}, is later than or equal to the creation time of {@link #this} 
   *      but {@link jchrest.domainSpecifics.Fixation#getScene()}, {@link 
   *      jchrest.domainSpecifics.Fixation#getColFixatedOn()} or {@link 
   *      jchrest.domainSpecifics.Fixation#getRowFixatedOn()} returns {@code 
   *      null}.
   *    </li>
   *    <li>
   *      Invoking {@link 
   *      jchrest.architecture.Chrest#isLearningObjectLocationsRelativeToAgent()}
   *      in context of the {@link jchrest.architecture.Chrest} model associated
   *      with {@link #this} returns {@link java.lang.Boolean#TRUE} but invoking 
   *      {@link jchrest.domainSpecifics.Scene#getLocationOfCreator()} returns 
   *      {@code null} when invoked in context of the {@link 
   *      jchrest.domainSpecifics.Scene} returned from invoking {@link 
   *      jchrest.domainSpecifics.Fixation#getScene()} on the {@link 
   *      jchrest.domainSpecifics.Fixation} to add.
   *    </li>
   * </ul>
   */
  ListPattern addFixation(Fixation fixation) {
    this._associatedChrestModel.printDebugStatement("===== Perceiver.addFixation() =====");
    ListPattern fixationFieldOfViewInformation = null;
    
    this._associatedChrestModel.printDebugStatement(
      "- Attempting to add the following fixation: " + (fixation == null ? "null" :
      fixation.toString())
    );
    
    ///////////////////////////////////
    ///// CHECK FOR NULL FIXATION /////
    ///////////////////////////////////
    
    if(fixation != null){
      
      this._associatedChrestModel.printDebugStatement("- Checking if the fixation performance time is >= the time the Perceiver was created");
      Integer fixationPerformanceTime = fixation.getPerformanceTime();
      List<Fixation> mostRecentFixations = this.getFixations(fixationPerformanceTime);
      if(mostRecentFixations != null){
        
        Scene fixationScene = fixation.getScene();
        Integer fixationXcor = fixation.getColFixatedOn();
        Integer fixationYcor = fixation.getRowFixatedOn();
        
        if(fixation.hasBeenPerformed()){
          this._associatedChrestModel.printDebugStatement(
            "- Fixation has been performed.  Checking:\n   ~ Whether the  " +
            "Fixation's performance time, Scene fixated on, column fixated on " +
            "and row fixated on variables are all set before adding it to the " +
            "Perceiver's Fixations data structure.\n   ~ Whether the CHREST " +
            "model associated with the Perceiver is learning SceneObject " +
            "loctaions relative to the agent equipped with the model and, if " +
            "so, whether this agent is denoted in the Scene fixated on."
          );
          
          if(fixationScene == null || fixationXcor == null || fixationYcor == null){
            throw new IllegalStateException(
              "The fixation to add has been performed but one or more of the " +
              "following variables have not been set:\n- Performance time\n- " +
              "Scene fixated on\n- Column of Scene coordinates fixated on\n- " +
              "Row of Scene coordinates fixated on.\n\nFixation details:" + 
              fixation.toString()
            );
          }
          
          if(
            this._associatedChrestModel.isLearningObjectLocationsRelativeToAgent() && 
            fixationScene.getLocationOfCreator() == null
          ){
            throw new IllegalStateException(
              "CHREST model is to learn object locations relevant to the agent " + 
              "equipped with CHREST however, the Fixation to add has not " +
              "identified the agent's location in the Scene fixated on.  " +
              "Fixation details:\n" + fixation.toString()
            );
          }
        }
   
        ////////////////////////
        ///// ADD FIXATION /////
        ////////////////////////

        this._associatedChrestModel.printDebugStatement("- Adding Fixation to Perceiver's Fixations data structure");

        //TODO: potentially add in trace decay here.
        List<Fixation> newFixations = new ArrayList();
        newFixations.addAll(mostRecentFixations);
        newFixations.add(fixation);
        this._fixations.put(fixationPerformanceTime, newFixations);

        this._associatedChrestModel.printDebugStatement("- Checking if the Fixation was performed");
        if(fixation.hasBeenPerformed()){
          this._associatedChrestModel.printDebugStatement("   ~ Fixation performed");

          /////////////////////////////////////////////////////
          ///// GET INFORMATION IN FIXATION FIELD OF VIEW /////
          /////////////////////////////////////////////////////

          fixationFieldOfViewInformation = this.getObjectsSeenInFixationFieldOfView(fixation, true);
          this._associatedChrestModel.printDebugStatement("- SceneObjects fixated on: " + fixationFieldOfViewInformation.toString());

          ////////////////////////////////////////////////
          ///// FILL OUT VISUAL STM HYPOTHESIS SLOTS /////
          ////////////////////////////////////////////////

          Stm visualStm = this._associatedChrestModel.getStm(Modality.VISUAL);
          if(visualStm.getCount(fixationPerformanceTime) >= 1) {
            this._associatedChrestModel.printDebugStatement("- Attempting to fill slots of hypothesis Node in visual STM");
            visualStm.getItem(0, fixationPerformanceTime).fillSlots(fixationFieldOfViewInformation, fixationPerformanceTime);
          }

          //////////////////////////////////////////////////
          ///// UPDATE VisualSpatialFieldObjectTermini /////
          //////////////////////////////////////////////////

          VisualSpatialField visualSpatialFieldRepresented = fixationScene.getVisualSpatialFieldRepresented();
          if(visualSpatialFieldRepresented != null){
            this._associatedChrestModel.printDebugStatement(
              "- Fixation was performed on a Scene representing a " +
              "VisualSpatialField. Updating relevant VisualSpatialFieldObject " +
              "termini"
            );
            this._associatedChrestModel.refreshVisualSpatialFieldObjectTermini(
              visualSpatialFieldRepresented, 
              fixation.getColFixatedOn(), 
              fixation.getRowFixatedOn(), 
              fixationPerformanceTime
            );
          }
          
          ///////////////////////////////////////////////////////
          ///// LEARN INFORMATION IN FIXATION FIELD OF VIEW /////
          ///////////////////////////////////////////////////////

          this._associatedChrestModel.printDebugStatement(
            "- Attempting to recognise and learn " + fixationFieldOfViewInformation.toString() +
            " at fixation performance time (" + fixationPerformanceTime + ")."
          );
          this._associatedChrestModel.recogniseAndLearn(fixationFieldOfViewInformation, fixationPerformanceTime);
        }
        else{
          this._associatedChrestModel.printDebugStatement("   ~ Fixation not performed successfully, exiting");
        }
      }
      else{
        throw new IllegalArgumentException(
          "Perceiver does not exist at the time the following Fixation was " +
          "performed:" + fixation.toString()
        );
      }
    }
    else{
       this._associatedChrestModel.printDebugStatement("- Fixation to add is null, exiting");
    }
      
    this._associatedChrestModel.printDebugStatement(
      "- Returning " + (fixationFieldOfViewInformation == null ? 
        "null" : fixationFieldOfViewInformation.toString())
    );
    this._associatedChrestModel.printDebugStatement("===== RETURN =====");
    return fixationFieldOfViewInformation;
  }
  
  /**
   * 
   * @param fixation
   * @param normaliseListPattern Set to {@link java.lang.Boolean#TRUE} if the
   * {@link jchrest.lib.ListPattern} is to be sent as inout to {@link 
   * jchrest.domainSpecifics.DomainSpecifics#normalise(jchrest.lib.ListPattern)}
   * before being returned.  Set to {@link java.lang.Boolean#FALSE} to return
   * the {@link jchrest.lib.ListPattern} as-is, i.e. with all {@link 
   * jchrest.domainSpecifics.SceneObject SceneObjects} fixated on present.
   * @return 
   */
  public ListPattern getObjectsSeenInFixationFieldOfView(Fixation fixation, boolean normaliseListPattern){
    ListPattern objectsSeenInFixationFieldOfView = null;
    Scene fixationScene = fixation.getScene();
    Integer fixationXcor = fixation.getColFixatedOn();
    Integer fixationYcor = fixation.getRowFixatedOn();
    
    if(fixationScene != null && fixationXcor != null && fixationYcor != null){
      objectsSeenInFixationFieldOfView = new ListPattern(Modality.VISUAL);
      ListPattern fixationFieldOfViewContent = fixationScene.getItemsInScopeAsListPattern(fixationXcor, fixationYcor, this._fixationFieldOfView);

      //Construct fixationFieldOfViewInformation according to 
      //agent-relative object locations.
      if(this._associatedChrestModel.isLearningObjectLocationsRelativeToAgent()){
        Square creatorLocationSceneSpecific = fixationScene.getLocationOfCreator();

        if(creatorLocationSceneSpecific != null){
          int absoluteDomainSpecificCreatorLocationCol = fixationScene.getDomainSpecificColFromSceneSpecificCol(creatorLocationSceneSpecific.getColumn());
          int absoluteDomainSpecificCreatorLocationRow = fixationScene.getDomainSpecificRowFromSceneSpecificRow(creatorLocationSceneSpecific.getRow());

          for(PrimitivePattern pattern : fixationFieldOfViewContent){
            if(pattern.getClass().equals(ItemSquarePattern.class)){
              ItemSquarePattern isp = (ItemSquarePattern)pattern;
              int absoluteDomainSpecificCol = fixationScene.getDomainSpecificColFromSceneSpecificCol(isp.getColumn());
              int absoluteDomainSpecificRow = fixationScene.getDomainSpecificRowFromSceneSpecificRow(isp.getRow());

              objectsSeenInFixationFieldOfView.add(new ItemSquarePattern(
                isp.getItem(),
                absoluteDomainSpecificCol - absoluteDomainSpecificCreatorLocationCol,
                absoluteDomainSpecificRow - absoluteDomainSpecificCreatorLocationRow
              ));
            }
          }
        }
        else{
          throw new IllegalStateException(
            "CHREST model is to learn object locations relevant to " +
            "agent equipped with CHREST however, a Fixation attempted " +
            "has not identified the agent's location in the Scene " +
            "fixated on.  Fixation details:\n" + fixation.toString()
          );
        }
      }
      //Construct fixationFieldOfViewInformation according to non 
      //agent-relative object locations
      else{
        for(PrimitivePattern pattern : fixationFieldOfViewContent){
          if(pattern.getClass().equals(ItemSquarePattern.class)){
            ItemSquarePattern isp = (ItemSquarePattern)pattern;
            int absoluteDomainSpecificCol = fixationScene.getDomainSpecificColFromSceneSpecificCol(isp.getColumn());
            int absoluteDomainSpecificRow = fixationScene.getDomainSpecificRowFromSceneSpecificRow(isp.getRow());

            objectsSeenInFixationFieldOfView.add(new ItemSquarePattern(
              isp.getItem(),
              absoluteDomainSpecificCol,
              absoluteDomainSpecificRow
            ));
          }
        }
      }

      //Finally, normalise fixationFieldOfViewInformation according to
      //domain-specifics.
      if(normaliseListPattern){
        objectsSeenInFixationFieldOfView = this._associatedChrestModel.getDomainSpecifics().normalise(objectsSeenInFixationFieldOfView);
      }
    }
    else{
      throw new IllegalArgumentException(
        "One or more of the following variables are not set for the fixation " +
        "to get SceneObjects fixated on in context of:\n- Scene fixated on " +
        "\n- Column of Scene coordinates fixated on\n- Row of Scene coordinates " +
        "fixated on.\n\nFixation details:" + fixation.toString()
      );
    }
    
    return objectsSeenInFixationFieldOfView;
  }
  
  /**
   * Clears all {@link jchrest.domainSpecifics.Fixation Fixations} in the
   * relevant data structure at the {@code time} specified and resets the
   * counter that tracks what {@link jchrest.domainSpecifics.Fixation Fixations}
   * have been learned from in the data structure to 0.
   * 
   * @param time 
   */
  public void clearFixations(int time) {
    this._fixations.put(time, new ArrayList());
    this._fixationToLearnFrom = 0;
  }
  
  /**
   * @param time
   * 
   * @return The most recent {@link jchrest.domainSpecifics.Fixation Fixations}
   * at the {@code time} specified or {@code null} if {@link #this} did not 
   * exist at the {@code time} specified.  Note that the most recent {@link 
   * jchrest.domainSpecifics.Fixation} added will be at the end of the {@link 
   * java.util.List} whereas the oldest will be at the front.
   */
  public List<Fixation> getFixations(int time) {
    //TODO: potentially add in trace decay here.
    Entry<Integer, Object> mostRecentFixations = this._fixations.floorEntry(time);
    return mostRecentFixations == null ? null : (ArrayList<Fixation>)mostRecentFixations.getValue();
  }
  
  /**
   * @param time
   * @return All {@link jchrest.domainSpecifics.Fixation Fixations} that have
   * been performed at the {@code time} specified or {@code null} if no {@link 
   * jchrest.domainSpecifics.Fixation Fixations} have been added before/at the 
   * {@code time} specified.
   */
  public List<Fixation> getFixationsPerformed(int time){
    List<Fixation> mostRecentFixationsPerformed = null;
    List<Fixation> mostRecentFixations = this.getFixations(time);
    if(mostRecentFixations != null){
      mostRecentFixationsPerformed = new ArrayList();
      for(Fixation fixation : mostRecentFixations){
        if(fixation.hasBeenPerformed()) mostRecentFixationsPerformed.add(fixation);
      }
    }
    
    return mostRecentFixationsPerformed;
  }
  
  /**
   * @param time
   * 
   * @return The most recent {@link jchrest.domainSpecifics.Fixation} performed
   * according to the {@code time} specified or {@code null} if there are no
   * {@link jchrest.domainSpecifics.Fixation Fixations} at the time specified or
   * no {@link jchrest.domainSpecifics.Fixation Fixations} stored have been
   * performed successfully.
   */
  public Fixation getMostRecentFixationPerformed(int time){
    List<Fixation> mostRecentFixations = this.getFixations(time);
    if(mostRecentFixations != null && !mostRecentFixations.isEmpty()){
      for(int i = mostRecentFixations.size() - 1; i >= 0; i--){
        Fixation fixation = mostRecentFixations.get(i);
        if(fixation.hasBeenPerformed()) return fixation;
      }
    }
    
    return null;
  }
  
  /**
   * Constructs a {@link jchrest.lib.ListPattern} from any new {@link 
   * jchrest.domainSpecifics.Fixation Fixations} that have been made and 
   * successfully performed by {@link #this} since the last invocation of this
   * function and passes this as an input parameter to {@link 
   * jchrest.architecture.Chrest#recogniseAndLearn(jchrest.lib.ListPattern, 
   * java.lang.Integer)}.
   * 
   * The {@link jchrest.lib.ListPattern} will not contain any duplicate {@link 
   * jchrest.domainSpecifics.SceneObject SceneObjects} or {@link 
   * jchrest.lib.Square Squares} fixated on or the location of the agent 
   * equipped with the {@link jchrest.architecture.Chrest} model associated with 
   * {@link #this}.  The {@link jchrest.lib.ListPattern} constructed will also
   * take into consideration the result of {@link 
   * jchrest.architecture.Chrest#isLearningObjectLocationsRelativeToAgent()}.
   * 
   * @param time 
   * 
   * @return The {@link jchrest.lib.ListPattern} generated or {@code null} if
   * any of the following statements apply:
   * <ul>
   *  <li>{@link #this} does not exist at the {@code time} specified</li>
   *  <li>
   *    {@link #this} has not attempted any {@link 
   *    jchrest.domainSpecifics.Fixation Fixations} at the {@code time} 
   *    specified.
   *  </li>
   *  <li>
   *    {@link #this} has not attempted any new {@link 
   *    jchrest.domainSpecifics.Fixation Fixations} at the {@code time} 
   *    specified since the last invocation of this function.
   *  </li>
   * </ul>
   * 
   * @throws IllegalStateException If any of the following conditions apply to
   * any {@link jchrest.domainSpecifics.Fixation} in the set of {@link 
   * jchrest.domainSpecifics.Fixation Fixations} returned when invoking this
   * function at the {@code time} specified.  These checks prevent situations
   * occurring where it is not possible to proceed without some domain-specific
   * decision being made which is not possible at this point.
   * <ul>
   *    <li>
   *      The same {@link jchrest.domainSpecifics.SceneObject} is present in two
   *      distinct {@link jchrest.domainSpecifics.Fixations}, i.e. {@link 
   *      jchrest.domainSpecifics.SceneObject#getIdentifier()} returns the same 
   *      {@link java.lang.String} for two {@link 
   *      jchrest.domainSpecifics.SceneObject SceneObjects} fixated on in two 
   *      different {@link jchrest.domainSpecifics.Fixation Fixations}.  Note
   *      that {@link jchrest.domainSpecifics.SceneObject SceneObjects} 
   *      representing blind/empty {@link jchrest.domainSpecifics.SceneObject 
   *      SceneObjects} are not included in this check since their identifiers
   *      are automatically set to be homogeneous.
   *    </li>
   *    <li>
   *      A {@link jchrest.lib.Square} has been fixated on twice (the absolute
   *      domain-specific coordinates of {@link jchrest.lib.Square Squares}
   *      fixated on are used in this calculation).
   *    </li>
   *    <li>
   *      Invoking {@link jchrest.domainSpecifics.Fixation#getColFixatedOn()}, 
   *      {@link jchrest.domainSpecifics.Fixation#getRowFixatedOn()}, {@link 
   *      jchrest.domainSpecifics.Fixation#getObjectSeen()} or {@link 
   *      jchrest.domainSpecifics.Fixation#getScene()} returns {@code null}.
   *    </li>
   *    <li>
   *      Invoking {@link jchrest.domainSpecifics.SceneObject#getObjectType()}
   *      on the result of {@link 
   *      jchrest.domainSpecifics.Fixation#getObjectSeen()} returns the result 
   *      of invoking {@link jchrest.domainSpecifics.Scene#getCreatorToken()}.
   *      Required since the agent that creates a {@link 
   *      jchrest.domainSpecifics.Scene} should not be fixated on.
   *    </li>
   * </ul>
   */
  ListPattern learnFromNewFixations(int time) {
    
    //Check that there are some recently performed Fixations and that an 
    //IndexOutOfBounds exception won't be thrown due to the _fixationToLearnFrom
    //being set in a previous invocation and no further Fixations being added
    //afterwards.
    List<Fixation> mostRecentFixations = this.getFixations(time);
    if(
      mostRecentFixations != null && 
      !mostRecentFixations.isEmpty() && 
      this._fixationToLearnFrom < mostRecentFixations.size()
    ){

      //Create the data structures that will hold the Fixations and ListPattern 
      //to learn from.  The former will be used to populate the latter.
      List<Fixation> fixationsToLearnFrom = new ArrayList();
      ListPattern patternToLearn = new ListPattern(Modality.VISUAL);
      
      //Starting from the Fixation last learned from in the current Fixation set
      //(if this is the first time this function has been called since starting 
      //a new Fixation set, this will be the first Fixation performed in the 
      //set otherwise, it will be the next Fixation along from the one last 
      //learned in the set), try to add each Fixation that has been performed
      //to the set of Fixations to learn from.
      for(int i = this._fixationToLearnFrom; i < mostRecentFixations.size(); i++){
        Fixation fixation = mostRecentFixations.get(i);
        if(fixation.hasBeenPerformed()){
      
          SceneObject objectFixatedOn = fixation.getObjectSeen();
          Integer colFixatedOn = fixation.getColFixatedOn();
          Integer rowFixatedOn = fixation.getRowFixatedOn();
          Scene scene = fixation.getScene();

          //Check for information required to continue and for a fixation on the
          //creator.
          if(
            objectFixatedOn != null && 
            colFixatedOn != null && 
            rowFixatedOn != null && 
            scene != null && 
            !objectFixatedOn.getObjectType().equals(Scene.getCreatorToken())
          ){
            String classOfObjectFixatedOn = objectFixatedOn.getObjectType();
            String identifierOfObjectFixatedOn = objectFixatedOn.getIdentifier();
            int domainSpecificCol = scene.getDomainSpecificColFromSceneSpecificCol(colFixatedOn);
            int domainSpecificRow = scene.getDomainSpecificRowFromSceneSpecificRow(rowFixatedOn);

            //Check if the SceneObject fixated on isn't a blind/empty square. If 
            //so, set a flag to prevent a duplicate SceneObject check.
            boolean nonBlindEmptyCreatorObject = false;
            if(
              !classOfObjectFixatedOn.equals(Scene.getBlindSquareToken()) &&
              !classOfObjectFixatedOn.equals(Scene.getEmptySquareToken())
            ){
              nonBlindEmptyCreatorObject = true;
            }

            //Check for duplicate SceneObjects/locations
            for(Fixation fixationToLearnFrom : fixationsToLearnFrom){
              String fixationToLearnFromObjectFixatedOnIdentifier = fixationToLearnFrom.getObjectSeen().getIdentifier();
              int fixationToLearnFromDomainSpecificCol = fixationToLearnFrom.getScene().getDomainSpecificColFromSceneSpecificCol(fixationToLearnFrom.getColFixatedOn());
              int fixationToLearnFromDomainSpecificRow = fixationToLearnFrom.getScene().getDomainSpecificRowFromSceneSpecificRow(fixationToLearnFrom.getRowFixatedOn());

              if(
                //Check for duplicate non-blind/empty objects.
                (
                  nonBlindEmptyCreatorObject && 
                  (fixationToLearnFromObjectFixatedOnIdentifier.equals(identifierOfObjectFixatedOn))
                ) 
                ||
                //Check for duplicate locations
                (
                  domainSpecificCol == fixationToLearnFromDomainSpecificCol &&
                  domainSpecificRow == fixationToLearnFromDomainSpecificRow
                )
              ){
                throw new IllegalStateException(
                  "Fixations to learn from contains a duplicate SceneObject " +
                  "or location.\nFixation that caused exception:\n" + fixation.toString() + 
                  "\nFixations to learn from: " + fixationsToLearnFrom.toString()
                );
              }
            }
          }
          else{
            throw new IllegalStateException(
              "Fixation to learn from does not have required variables set (" +
              "SceneObject, column, row and Scene) or the object fixated on is " +
              "the creator.  Fixation details:\n" + fixation.toString()
            );
          }
          
          fixationsToLearnFrom.add(fixation);
        }
      }
      
      //Check that there are Fixations to learn from.
      if(!fixationsToLearnFrom.isEmpty()){

        //Determine if the agent using CHREST is learning object locations
        //using agent-relative coordinates or not.  If they are, the coordinates 
        //of each item fixated on is calculated relative to the agent's last 
        //recorded location (obtainable from the most recent Fixation attempted) 
        //for each Fixation to be learned from.
        if(this._associatedChrestModel.isLearningObjectLocationsRelativeToAgent()){

          //Get agent's location in the last Fixation attempted.
          Scene sceneFromMostRecentFixation = mostRecentFixations.get(mostRecentFixations.size() - 1).getScene();
          Square agentLocationInMostRecentFixation = sceneFromMostRecentFixation.getLocationOfCreator();
          int domainSpecificColOfCreator = sceneFromMostRecentFixation.getDomainSpecificColFromSceneSpecificCol(agentLocationInMostRecentFixation.getColumn());
          int domainSpecificRowOfCreator = sceneFromMostRecentFixation.getDomainSpecificRowFromSceneSpecificRow(agentLocationInMostRecentFixation.getRow());

          //Calculate the location of each object fixated on in Fixations to be
          //learned, relative to the agent's last recorded location.
          for(Fixation fixation : fixationsToLearnFrom){
            Scene fixationScene = fixation.getScene();
            Integer fixationSceneSpecificCol = fixation.getColFixatedOn();
            Integer fixationSceneSpecificRow = fixation.getRowFixatedOn();
            SceneObject objectSeen = fixation.getObjectSeen();

            //Check all required information is available to perform the 
            //calculation (should be but, better to be safe than sorry!)
            if(
              fixationScene != null &&
              fixationSceneSpecificCol != null &&
              fixationSceneSpecificRow != null &&
              objectSeen != null
            ){
              int domainSpecificFixationCol = fixationScene.getDomainSpecificColFromSceneSpecificCol(fixationSceneSpecificCol);
              int domainSpecificFixationRow = fixationScene.getDomainSpecificRowFromSceneSpecificRow(fixationSceneSpecificRow);

              patternToLearn.add(new ItemSquarePattern(
                  objectSeen.getObjectType(),
                  domainSpecificFixationCol - domainSpecificColOfCreator,
                  domainSpecificFixationRow - domainSpecificRowOfCreator
                )
              );
            }
          }
        }
        //The agent is not learning object locations relative to its own 
        //location so just add the relevant information from each fixation 
        //as an ItemSquarePattern to the pattern to learn from.
        else{
          for(Fixation fixation : fixationsToLearnFrom){
            Integer fixationSceneSpecificCol = fixation.getColFixatedOn();
            Integer fixationSceneSpecificRow = fixation.getRowFixatedOn();
            SceneObject objectSeen = fixation.getObjectSeen();

            //Check all required information is available to perform the 
            //calculation (should be but, better to be safe than sorry!)
            if(
              fixationSceneSpecificCol != null &&
              fixationSceneSpecificRow != null &&
              objectSeen != null
            ){
              patternToLearn.add(new ItemSquarePattern(
                  objectSeen.getObjectType(),
                  fixationSceneSpecificCol,
                  fixationSceneSpecificRow
                )
              );
            }
          }
        }

        //Normalise the pattern to learn from according to domain specifics 
        //and learn it.
        patternToLearn = this._associatedChrestModel.getDomainSpecifics().normalise(patternToLearn);
        this._associatedChrestModel.recogniseAndLearn(patternToLearn, time);
      }

      //Increment counter to the end of the Fixation set in preparation for the
      //next invocation of this method (should start from the next new fixation
      //that's added to the set).
      this._fixationToLearnFrom = mostRecentFixations.size();
      
      return patternToLearn;
    }
    
    return null;
  }
  
  /**
   * 
   * @return The current index that {@link #this} is to learn from in relation 
   * to the current set of {@link jchrest.domainSpecifics.Fixation Fixations} 
   * (used primarily in {@link #this#learnFromNewFixations(int)}).
   */
  int getFixationToLearnFrom(){
    return this._fixationToLearnFrom;
  }

  /**
   * @return The number of {@link jchrest.lib.Square Squares} in each compass 
   * direction that can be seen when a {@link jchrest.domainSpecifics.Fixation}
   * is added using {@link #this#addFixation(jchrest.domainSpecifics.Fixation)}.  
   * The value of this parameter is set, by default, to 2 according to section 
   * 8.7.6 of "Perception and Memory in Chess" by deGroot and Gobet (see the 
   * discussion of the "Peripheral-Square" heuristic).
   */
  public int getFixationFieldOfView() {
    return _fixationFieldOfView;
  }

  /**
   * The value of this parameter is set, by default, to 2 according to section 
   * 8.7.6 of "Perception and Memory in Chess" by deGroot and Gobet (see the 
   * discussion of the "Peripheral-Square" heuristic).
   * 
   * @param fov The number of {@link jchrest.lib.Square Squares} in each compass 
   * direction that can be seen when a {@link jchrest.domainSpecifics.Fixation}
   * is added using {@link #this#addFixation(jchrest.domainSpecifics.Fixation)}.
   */
  public void setFixationFieldOfView(int fov) {
    this._fixationFieldOfView = fov;
  }
}

