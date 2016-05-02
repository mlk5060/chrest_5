package jchrest.domainSpecifics.fixations;

import jchrest.domainSpecifics.Fixation;
import java.util.List;
import jchrest.architecture.Chrest;
import jchrest.architecture.Link;
import jchrest.architecture.Stm;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.Modality;
import jchrest.lib.Pattern;
import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.SceneObject;
import jchrest.lib.Square;

/**
 * Represents a {@link jchrest.domainSpecifics.Fixation} made using the 
 * "hypothesis-discrimination" heuristic (see "Perception and Memory in Chess"
 * by de Groot and Gobet, section 8.7.6).
 * 
 * <b>NOTE:</b> {@link 
 * jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation
 * HypothesisDiscriminationFixations} do not require any {@link 
 * jchrest.domainSpecifics.Fixation Fixations} to have been added by the {@link 
 * jchrest.architecture.Perceiver} associated with the {@link 
 * jchrest.architecture.Chrest} model that invokes {@link #this#make(
 * jchrest.domainSpecifics.Scene, int)} prior to this invocation in order to 
 * propose a {@link jchrest.lib.Square} to fixate on.  However, the {@link 
 * jchrest.architecture.Chrest} model that invokes {@link #this#make(
 * jchrest.domainSpecifics.Scene, int)} must have at least 1 {@link 
 * jchrest.architecture.Node} in its {@link jchrest.lib.Modality#VISUAL} {@link 
 * jchrest.architecture.Stm} at the {@code time} specified when {@link 
 * #this#make(jchrest.domainSpecifics.Scene, int)} is invoked.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class HypothesisDiscriminationFixation extends Fixation{
  
  private final Chrest _model;
  
  /**
   * Constructor.
   * 
   * Sets the time {@link #this} will be decided upon to be the {@code time}
   * specified plus the value of invoking {@link 
   * jchrest.architecture.Chrest#getTimeToRetrieveItemFromStm()} on the {@code 
   * model} specified.
   * 
   * @param model The {@link jchrest.architecture.Chrest} model constructing 
   * {@link #this}.
   * @param time The current time in the domain (in milliseconds).
   */
  public HypothesisDiscriminationFixation(Chrest model, int time){
    super(time + model.getTimeToRetrieveItemFromStm());
    this._model = model;
  }

  /**
   * Implements the procedure of making {@link #this} according to the fixation
   * 1 description provided in section 8.7.6 of "Perception and Memory in Chess" 
   * by de Groot and Gobet.
   * 
   * @param scene
   * @param time
   * 
   * @return The {@link jchrest.lib.Square} represented by invoking {@link 
   * jchrest.lib.ItemSquarePattern#getColumn()} and {@link 
   * jchrest.lib.ItemSquarePattern#getRow()} on the first {@link 
   * jchrest.lib.ItemSquarePattern} of the image of the first child of the 
   * current {@link jchrest.lib.Modality#VISUAL} {@link 
   * jchrest.architecture.Stm} hypothesis of the {@link 
   * jchrest.architecture.Chrest} model invoking this method at the {@code time} 
   * specified.  However, if any of the following statements evaluate to {@link 
   * java.lang.Boolean#TRUE}, {@code null} is returned:
   * 
   * <ol type="1">
   *    <li>The {@code scene} specified is entirely blind.</li>
   *    <li>
   *      The result of invoking {@link jchrest.architecture.Stm#getCount(int)}
   *      on the {@link jchrest.lib.Modality#VISUAL} {@link 
   *      jchrest.architecture.Stm} of the {@link jchrest.architecture.Chrest} 
   *      model making {@link #this} at the time specified is equal to {@code 
   *      null} or 0.
   *    </li>
   *    <li>
   *      The result of invoking {@link jchrest.architecture.Node#getChildren(
   *      int)} on the {@link jchrest.lib.Modality#VISUAL} {@link 
   *      jchrest.architecture.Stm} hypothesis of the {@link 
   *      jchrest.architecture.Chrest} model making {@link #this} at the time 
   *      specified is equal to {@code null} or is empty.
   *    </li>
   *    <li>
   *      The result of invoking {@link jchrest.architecture.Link#getTest()} on
   *      the {@link jchrest.architecture.Link} of the first child of the {@link 
   *      jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Stm} 
   *      hypothesis of the {@link jchrest.architecture.Chrest} model making 
   *      {@link #this} at the time specified is equal to {@code null} or is not
   *      an instance of {@link jchrest.lib.ItemSquarePattern}.
   *    </li>
   *    <li>
   *      The {@link jchrest.lib.Square} specified by the first {@link 
   *      jchrest.lib.ItemSquarePattern} in the result of invoking {@link 
   *      jchrest.architecture.Link#getTest()} on the {@link 
   *      jchrest.architecture.Link} of the first child of the {@link 
   *      jchrest.lib.Modality#VISUAL} {@link jchrest.architecture.Stm} 
   *      hypothesis of the {@link jchrest.architecture.Chrest} model making 
   *      {@link #this} at the time specified is:
   *      <ul>
   *        <li>
   *          Equal to {@code null} or is not an instance of {@link 
   *          jchrest.lib.ItemSquarePattern}.
   *        </li>
   *        <li>
   *          The same {@link jchrest.lib.Square} that was fixated on in the
   *          {@link jchrest.domainSpecifics.Fixation} previous to this.
   *        </li>
   *        <li>
   *          Is not a valid coordinate in context of the result of invoking 
   *          {@link jchrest.architecture.Perceiver#getCurrentScene()} on the
   *          {@link jchrest.architecture.Perceiver} associated with the
   *          {@link jchrest.architecture.Chrest} model that is making {@link 
   *          #this}.
   *        </li>
   *        <li>
   *          Is blind in context of the result of invoking {@link 
   *          jchrest.architecture.Perceiver#getCurrentScene()} on the {@link 
   *          jchrest.architecture.Perceiver} associated with the {@link 
   *          jchrest.architecture.Chrest} model that is making {@link #this}.
   *        </li>
   *      </ul>
   *    </li>
   *    <li>
   *      The {@code time} specified is earlier than the result of invoking
   *      {@link #this#getPerformanceTime()}.
   *    </li>
   * </ol>
   */
  @Override
  public Square make(Scene scene, int time) {
    if(!scene.isBlind()){
      this._model.printDebugStatement("- Scene is not entirely blind");
      
      Stm visualStm = this._model.getStm(Modality.VISUAL);
      Integer numberNodesInStmAtTime = visualStm.getCount(time);
      
      if(numberNodesInStmAtTime != null && numberNodesInStmAtTime >= 1) {
        this._model.printDebugStatement("- Number of Nodes in visual STM at time " + time + " is >= 1");
        
        List<Link> hypothesisChildren = visualStm.getItem(0, time).getChildren(time);
        if(hypothesisChildren == null || hypothesisChildren.isEmpty()) return null;
        this._model.printDebugStatement("- Visual STM hypothesis has children at time " + time);

        //for(int i = 0; i < hypothesisChildren.size () && i < 1; ++i) { // *** i == 0 only
          ListPattern firstHypothesisChildTest = hypothesisChildren.get(0).getTest();
          if (firstHypothesisChildTest.isEmpty ()) return null;
          this._model.printDebugStatement("- Test for first child of visual STM hypothesis is not empty");
          
          Pattern firstItemOfFirstHypothesisChildTest = firstHypothesisChildTest.getItem(0);
          if(firstItemOfFirstHypothesisChildTest.getClass().equals(ItemSquarePattern.class)) {
            this._model.printDebugStatement("- First pattern in test for first child of visual STM hypothesis is an ItemSquarePattern");
            
            ItemSquarePattern potentialFixationWithAgentRelativeOrDomainSpecificCoordinates = (ItemSquarePattern)firstItemOfFirstHypothesisChildTest;
            this._model.printDebugStatement("- Potential fixation (domain-specific): " + potentialFixationWithAgentRelativeOrDomainSpecificCoordinates.toString());
            
            //////////////////////////////////////////////////////////////
            ///// CONVERT DOMAIN-SPECIFIC FIXATION TO SCENE-SPECIFIC /////
            //////////////////////////////////////////////////////////////
            
            //The STM node test will contain domain-specific coordinates so, to
            //make this Fixation, Scene-specific (zero-indexed) coordinates are
            //required.  Translation is dependent on whether or not CHREST is
            //currently learning object locations relative to the agent equipped
            //with CHREST.
            Square potentialFixation = null;
            if(this._model.isLearningObjectLocationsRelativeToAgent()){
              this._model.printDebugStatement("- CHREST model is learning object locations relative to agent equipped with CHREST");
              Square locationOfAgentInScene = scene.getLocationOfCreator();
              
              if(locationOfAgentInScene != null){
                this._model.printDebugStatement("- Location of agent in scene (scene-specific): " + locationOfAgentInScene.toString());
                int sceneSpecificCol = locationOfAgentInScene.getColumn() + potentialFixationWithAgentRelativeOrDomainSpecificCoordinates.getColumn();
                int sceneSpecificRow = locationOfAgentInScene.getRow() + potentialFixationWithAgentRelativeOrDomainSpecificCoordinates.getRow();
                
                if(
                  sceneSpecificCol >= 0 && 
                  sceneSpecificCol < scene.getWidth() &&
                  sceneSpecificRow >= 0 &&
                  sceneSpecificRow < scene.getHeight()
                ){
                  potentialFixation = new Square(sceneSpecificCol, sceneSpecificRow);
                }
              }
              else{
                throw new IllegalStateException(
                  "The CHREST model is learning object loctaions relative to " +
                  "the agent equipped with it but its location has not been " +
                  "specified in the scene to fixate on (" + scene.toString() + ")"
                );
              }
            }
            else{
              Integer col = scene.getSceneSpecificColFromDomainSpecificCol(potentialFixationWithAgentRelativeOrDomainSpecificCoordinates.getColumn());
              Integer row = scene.getSceneSpecificRowFromDomainSpecificRow(potentialFixationWithAgentRelativeOrDomainSpecificCoordinates.getRow());
              if(col != null && row != null) potentialFixation = new Square(col, row);
            }
            
            if(potentialFixation != null){
              this._model.printDebugStatement("- Square to attempt fixation on (scene-specific): " + potentialFixation.toString());

              //Check if the fixation should be made.  If all of the following
              //statements are true, perform the fixation:
              //
              // 1. The square to fixate on is present in the current Scene.
              // 2. The square to fixate on is not blind.
              // 3. No Fixation has been performed prior to this Fixation's 
              //    attempt to be made OR the most recently performed Fixation 
              //    did not focus on the same Square in the domain as this 
              //    Fixation is proposing (this Fixation, if made would then 
              //    look at the same square again; no point in doing this).  
              //    Since Scenes can look at different areas of the external 
              //    domain, the absolue domain coordinates proposed to be 
              //    fixated on now and those fixated on in the previous Fixation 
              //    are checked.
              // 4. The time that this Fixation has been requested at is equal 
              //    to or after its performance time.  Otherwise, an attempt is 
              //    being made to make this Fixation before its performance time 
              //    and this should not be permitted.
            
              //Get the most recent Fixation performed since it will be used in 
              //a few places below (more efficient).
              Fixation mostRecentFixationPerformed = this._model.getPerceiver().getMostRecentFixationPerformed(time);
              SceneObject contentsOfPotentialFixation = scene.getSquareContents(potentialFixation.getColumn(), potentialFixation.getRow());
              
              Square domainSquareToPotentiallyFixateOn = new Square(
                scene.getDomainSpecificColFromSceneSpecificCol(potentialFixation.getColumn()),
                scene.getDomainSpecificRowFromSceneSpecificRow(potentialFixation.getRow())
              );
              
              Square domainSquareFixatedOnInMostRecentFixationPerformed = null;
              if(mostRecentFixationPerformed != null){
                domainSquareFixatedOnInMostRecentFixationPerformed = new Square(
                  mostRecentFixationPerformed.getScene().getDomainSpecificColFromSceneSpecificCol(mostRecentFixationPerformed.getColFixatedOn()),
                  mostRecentFixationPerformed.getScene().getDomainSpecificRowFromSceneSpecificRow(mostRecentFixationPerformed.getRowFixatedOn())
                );
              }
              this._model.printDebugStatement(
                "- Checking if Fixation can be made (all statements must evaluate to true):" +
                "\n   ~ Potential Fixation on Square represented in Scene: " + (contentsOfPotentialFixation != null) +
                "\n   ~ Potential Fixation on non-blind Square: " + !(contentsOfPotentialFixation.getObjectType().equals(Scene.getBlindSquareToken())) +
                "\n   ~ Potential Fixation on different Square to previous Fixation: " + !(domainSquareToPotentiallyFixateOn.equals(domainSquareFixatedOnInMostRecentFixationPerformed)) +
                "\n      + Potential Fixation (domain-specific coords): " + domainSquareToPotentiallyFixateOn.toString() + 
                "\n      + Most recent Fixation performed (domain-specific coords): " + 
                  (domainSquareFixatedOnInMostRecentFixationPerformed == null ? 
                    "null" : 
                    domainSquareFixatedOnInMostRecentFixationPerformed.toString()
                  ) +
                "\n   ~ Fixation performance time reached: " + (this.getPerformanceTime() <= time)
              );

              if(
                //Check statement 1
                (contentsOfPotentialFixation != null) &&
                //Check statement 2
                !(contentsOfPotentialFixation.getObjectType().equals(Scene.getBlindSquareToken())) &&
                //Check statement 3 (if no Fixation has previously been made, 
                //the domain square prev. fixated on variable will be set to 
                //null so the test will evaluate to true since potentialFixation
                //is not null since it was asserted so above).
                (!domainSquareToPotentiallyFixateOn.equals(domainSquareFixatedOnInMostRecentFixationPerformed)) &&
                //Check statement 4
                (this.getPerformanceTime() <= time)
              ) {
                this._model.printDebugStatement("- Making Fixation");
              
                /////////////////////////
                ///// MAKE FIXATION /////
                /////////////////////////

                // Update hypothesis by looking at the Square indicated by the 
                // first test link of each hypothesis child.  This will cause a
                // different Square to be looked at if this function is invoked
                // again in the same fixation set (see the conditional above about
                // looking at the same Square).
                for (Link hypothesisChildLink : hypothesisChildren) {
                  ListPattern hypothesisChildTest = hypothesisChildLink.getTest();

                  // Note: using first test created gives more uses of LTM 
                  //       heuristic (Martyn: this is a comment left by Peter, 
                  //       and I'm unsure of the logic behind it).
                  if(
                    hypothesisChildTest.size() == 1 && 
                    hypothesisChildTest.getItem(0) instanceof ItemSquarePattern
                  ){  
                    ItemSquarePattern testIos = (ItemSquarePattern)hypothesisChildTest.getItem(0);

                    // If the item in the test exists in the Scene, update the 
                    //hypothesis.
                    if(scene.getSquareContents(potentialFixation.getColumn(), potentialFixation.getRow()).getObjectType().equals(testIos.getItem())){
                      this._model.replaceStmHypothesis(hypothesisChildLink.getChildNode(), time);
                    }
                  }
                }

                //"Make" the fixation.
                return potentialFixation;
              }
            }
          }
        }
      //}
    }
    return null;
  }
}
