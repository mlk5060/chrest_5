package jchrest.domainSpecifics.tileworld;

import jchrest.domainSpecifics.DomainSpecifics;
import java.util.List;
import jchrest.architecture.Chrest;
import jchrest.domainSpecifics.Fixation;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.PrimitivePattern;
import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.fixations.AheadOfAgentFixation;
import jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation;
import jchrest.domainSpecifics.fixations.PeripheralItemFixation;
import jchrest.domainSpecifics.fixations.PeripheralSquareFixation;
import jchrest.domainSpecifics.tileworld.fixations.MovementFixation;
import jchrest.domainSpecifics.tileworld.fixations.SalientObjectFixation;
import jchrest.lib.Modality;

/**
 * Used for Tileworld modelling.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class TileworldDomain extends DomainSpecifics{
  
  public static final String HOLE_SCENE_OBJECT_TYPE_TOKEN = "H"; 
  public static final String OPPONENT_SCENE_OBJECT_TYPE_TOKEN = "O";
  public static final String TILE_SCENE_OBJECT_TYPE_TOKEN = "T";
  
  private final int _initialFixationThreshold;
  private final int _peripheralItemFixationMaxAttempts;
  
  private int _timeTakenToDecideUponMovementFixations;
  private int _timeTakenToDecideUponSalientObjectFixations;
  
  /**
   * 
   * @param model
   * 
   * @param maxFixationsInSet See parameters for {@link 
   * jchrest.domainSpecifics.DomainSpecifics#DomainSpecifics(
   * jchrest.architecture.Chrest, java.lang.Integer)}.
   * 
   * @param initialFixationThreshold The number of {@link 
   * jchrest.domainSpecifics.Fixation Fixations} that must be attempted by the
   * {@link jchrest.architecture.Perceiver} associated with the {@link 
   * jchrest.architecture.Chrest} model using {@link #this} before non-initial
   * {@link jchrest.domainSpecifics.Fixation Fixations} are proposed by {@link 
   * #this#getNonInitialFixationInSet(int)}.
   * 
   * @param peripheralItemFixationMaxAttempts The maximum number of attempts 
   * that will be made if a {@link 
   * jchrest.domainSpecifics.fixations.PeripheralItemFixation} is to be made
   * when {@link #this#getNonInitialFixationInSet(int)} (see parameters for 
   * {@link jchrest.domainSpecifics.fixations.PeripheralItemFixation#PeripheralItemFixation(
   * jchrest.architecture.Chrest, int, int)}
   * 
   * @param timeTakenToDecideUponMovementFixations
   * @param timeTakenToDecideUponSalientObjectFixations
   */
  public TileworldDomain(
    Chrest model, 
    Integer maxFixationsInSet, 
    Integer initialFixationThreshold, 
    Integer peripheralItemFixationMaxAttempts,
    Integer timeTakenToDecideUponMovementFixations,
    Integer timeTakenToDecideUponSalientObjectFixations
  ) {
    super(model, maxFixationsInSet);
    
    //Check for CHREST model learning object locations realtive to agent
    if(!model.isLearningObjectLocationsRelativeToAgent()){
      throw new IllegalStateException(
        "To use a TileworldDomain, a CHREST model must be learning object " +
        "locations relative to an agent however, the CHREST model specified " +
        "is not."
      );
    }
    
    //Set initial fixation threshold.
    if(initialFixationThreshold > 0){
      if(maxFixationsInSet < initialFixationThreshold){
        throw new IllegalArgumentException(
          "The maximum number of fixations to make in a set specified as a " +
          "parameter to the " + this.getClass().getCanonicalName() + " " +
          "constructor (" + maxFixationsInSet + ") is < the initial fixation " +
          "threshold specified (" + initialFixationThreshold + ")."
        );
      }
      else{
        this._initialFixationThreshold = initialFixationThreshold;
      }
    }
    else{
      throw new IllegalArgumentException(
        "The initial fixation threshold specified as a parameter to the " + 
        this.getClass().getCanonicalName() + " constructor (" + 
        initialFixationThreshold + ") is <= 0."
      );
    }
    
    //Set peripheral item fixation max attempts.
    if(peripheralItemFixationMaxAttempts > 0){
      this._peripheralItemFixationMaxAttempts = peripheralItemFixationMaxAttempts;
    }
    else{
      throw new IllegalArgumentException(
        "The maximum number of attempts to make a fixation on an item " +
        "in the periphery specified as a parameter to the " + 
        this.getClass().getCanonicalName() + " constructor (" + 
        peripheralItemFixationMaxAttempts + ") is <= 0."
      );
    }
    
    //Set time taken to decide upon movement/salient object fixations
    if(timeTakenToDecideUponMovementFixations < 0 || timeTakenToDecideUponSalientObjectFixations < 0){
      throw new IllegalArgumentException(
        "One or both of the times taken to decide on movement or salient " +
        "object fixations is < 0 (time specified to decide on movement fixations: " +
        timeTakenToDecideUponMovementFixations + ", time specified to decide on " +
        "salient object fixations: " + timeTakenToDecideUponSalientObjectFixations +
        ")."
      );
    }
    else{
      this._timeTakenToDecideUponMovementFixations = timeTakenToDecideUponMovementFixations;
      this._timeTakenToDecideUponSalientObjectFixations = timeTakenToDecideUponSalientObjectFixations;
    }
  }

  /** 
   * @param pattern
   * 
   * @return A {@link jchrest.lib.ListPattern} stripped of {@link 
   * jchrest.lib.ItemSquarePattern ItemSquarePatterns} where {@link 
   * jchrest.lib.ItemSquarePattern#getItem()} returns {@link 
   * jchrest.domainSpecifics.Scene#getBlindSquareToken()}, {@link 
   * jchrest.domainSpecifics.Scene#getEmptySquareToken()} or {@link 
   * jchrest.domainSpecifics.Scene#getCreatorToken()}.
   * <br/>
   * If the {@code pattern} has {@link jchrest.lib.Modality#VISUAL}, any 
   * duplicate {@link jchrest.lib.ItemSquarePattern ItemSquarePatterns} are 
   * removed.
   */
  @Override
  public ListPattern normalise(ListPattern pattern) {
    ListPattern result = new ListPattern(pattern.getModality());
    
    for(PrimitivePattern prim : pattern){
      ItemSquarePattern itemDetails = (ItemSquarePattern)prim;
      String item = itemDetails.getItem();
      if(
        !item.equals(Scene.getBlindSquareToken()) &&
        !item.equals(Scene.getEmptySquareToken()) &&
        !item.equalsIgnoreCase(Scene.getCreatorToken())
      ){
        result.add(itemDetails);
      }
    }
    
    if(pattern.getModality() == Modality.VISUAL){
      ListPattern resultWithoutDuplicates = new ListPattern(result.getModality());
      for(PrimitivePattern prim : result){
        ItemSquarePattern isp = (ItemSquarePattern)prim;
        if(!resultWithoutDuplicates.contains(isp)){
          resultWithoutDuplicates.add(isp);
        }
      }
      
      result = resultWithoutDuplicates;
    }
    
    if(pattern.isFinished()){
      result.setFinished();
    } 
    else{
      result.setNotFinished();
    }
    
    return result;
  }

  @Override
  public Fixation getInitialFixationInSet(int time) {
    return new AheadOfAgentFixation(time, this._associatedModel.getTimeTakenToDecideUponAheadOfAgentFixations());
  }

  /**
   * 
   * @param time
   * 
   * @return A new {@link jchrest.domainSpecifics.Fixation} whose type is 
   * determined by comparing <i>n</i> (the sum of the number of {@link 
   * jchrest.domainSpecifics.Fixation Fixations} to make and the number of {@link 
   * jchrest.domainSpecifics.Fixation Fixations} attempted at the {@code time} 
   * specified) to <i>t</i> (the initial {@link 
   * jchrest.domainSpecifics.Fixation} threshold specified as a parameter to 
   * {@link #this#TileworldDomain(jchrest.architecture.Chrest, int, int, int)}):
   * 
   * <ol type="1">
   *  <li>
   *    If <i>n</i> &lt; <i>t</i> a {@link 
   *    jchrest.domainSpecifics.tileworld.fixations.SalientObjectFixation} is
   *    returned.
   *  </li>
   *  <li>
   *    If <i>n</i> &gt;&#61; <i>t</i>, the {@link 
   *    jchrest.domainSpecifics.Fixation Fixations} scheduled to be performed 
   *    and the most recent {@link jchrest.domainSpecifics.Fixation} attempted
   *    by the {@link jchrest.architecture.Chrest} model using {@link #this} are
   *    retrieved in context of the {@code time} specified. 
   *    <ol type="1">
   *      <li>
   *        If there is no {@link 
   *        jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} 
   *        scheduled to be performed or the most recently attempted {@link 
   *        jchrest.domainSpecifics.Fixation} was a {@link 
   *        jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} 
   *        whose performance was unsuccessful, a {@link 
   *        jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} 
   *        is returned.
   *      </li>
   *      <li>
   *        If there is a {@link 
   *        jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} 
   *        scheduled to be performed or the most recently attempted {@link 
   *        jchrest.domainSpecifics.Fixation} was a {@link 
   *        jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} 
   *        that was performed successfully, one of the following {@link 
   *        jchrest.domainSpecifics.Fixation Fixations} are returned with equal
   *        probability:
   *        <ul>
   *          <li>{@link jchrest.domainSpecifics.tileworld.fixations.SalientObjectFixation}</li>
   *          <li>{@link jchrest.domainSpecifics.tileworld.fixations.MovementFixation}</li>
   *          <li>{@link jchrest.domainSpecifics.fixations.PeripheralItemFixation}</li>
   *          <li>{@link jchrest.domainSpecifics.fixations.PeripheralSquareFixation}</li>
   *        </ul>
   *      </li>
   *    </ol>
   *  </li>
   * </ol>
   */
  @Override
  public Fixation getNonInitialFixationInSet(int time) {
    List<Fixation> fixationsScheduled = this._associatedModel.getScheduledFixations(time);
    List<Fixation> fixationsAttempted = this._associatedModel.getPerceiver().getFixations(time);
    int numberFixationsToMake = (fixationsScheduled == null ? 0 : fixationsScheduled.size());
    int numberFixationsAttempted = (fixationsAttempted == null ? 0 : fixationsAttempted.size());
    
    if((numberFixationsToMake + numberFixationsAttempted) < this._initialFixationThreshold){
      return new SalientObjectFixation(time, this._timeTakenToDecideUponSalientObjectFixations);
    }
    else{
      
      //In this case, a HypothesisDiscriminationFixation should always be 
      //attempted unless:
      //
      // 1. There is such a Fixation already being deliberated on but hasn't 
      //    been performed yet.
      // 2. The most recent Fixation attempted was such a Fixation but wasn't
      //    performed.
      //
      //In the first case, the outcome of attempting to make the 
      //HypothesisDiscriminationFixation is unknown so instead of generating 
      //another which may fail again (essentially wasting a Fixation), generate
      //another type of Fixation.
      //
      //In the second case, the outcome of attempting to make a 
      //HypothesisDiscriminationFixation is known and the attempt was 
      //unsuccessful so other Fixations need to be made to try and replace the
      //current visual STM hypothesis since its information is not useful in the
      //current Scene.
      //
      //To perform these checks, get the Fixations currently being deliberated
      //on and the Fixations performed up until the time specified by the 
      //CHREST model associated with this domain.  NOTE: there is no need to 
      //check for whether the Lists returned are null or empty since this will 
      //have been checked when doneInitialFixations() is called in the "if" part 
      //of the conditional surrounding this block.
      
      //Check for a HypothesisDiscriminationFixation currently being decided 
      //upon.
      boolean hypothesisDiscriminationFixationBeingDeliberatedOn = false;
      for(Fixation fixation : fixationsScheduled){
        if(fixation.getClass().equals(HypothesisDiscriminationFixation.class)){
          hypothesisDiscriminationFixationBeingDeliberatedOn = true;
          break;
        }
      }
      
      //Check for a recent attempt at a HypothesisDiscriminationFixation that
      //failed.
      boolean mostRecentFixationAttemptedFailedAndWasHDF = false;
      if(fixationsAttempted != null){
        Fixation mostRecentFixationAttempted = fixationsAttempted.get(fixationsAttempted.size() - 1);
        mostRecentFixationAttemptedFailedAndWasHDF = (
          !mostRecentFixationAttempted.hasBeenPerformed() && 
          mostRecentFixationAttempted.getClass().equals(HypothesisDiscriminationFixation.class)
        );
      }
      
      if(
        hypothesisDiscriminationFixationBeingDeliberatedOn ||
        (
          !hypothesisDiscriminationFixationBeingDeliberatedOn &&
          mostRecentFixationAttemptedFailedAndWasHDF
        )
      ){
        
        Fixation fixation = null;
        while(fixation == null){
          double r = Math.random();
          
          if(r < 0.25){
            fixation = new SalientObjectFixation(time, this._timeTakenToDecideUponSalientObjectFixations);
          }
          else if(r >= 0.25 && r < 0.5) {
            fixation = new MovementFixation(this._associatedModel, time, this._timeTakenToDecideUponMovementFixations);
          }
          else if(r >= 0.5 && r < 0.75){
            fixation = new PeripheralItemFixation(this._associatedModel, this._peripheralItemFixationMaxAttempts, time, this._associatedModel.getTimeTakenToDecideUponPeripheralItemFixations());
          }
          else{
            fixation = new PeripheralSquareFixation(this._associatedModel, time, this._associatedModel.getTimeTakenToDecideUponPeripheralSquareFixations());
          }
        }
        
        return fixation;
      }
      else{
        return new HypothesisDiscriminationFixation(this._associatedModel, time);
      }
    }
  }

  /**
   * 
   * @param time
   * 
   * @return {@link java.lang.Boolean#FALSE} since there are no extra conditions
   * to consider when a {@link jchrest.architecture.Chrest} model using {@link 
   * #this} determines whether the {@link jchrest.domainSpecifics.Fixation 
   * Fixations} it has performed should be learned from.
   */
  @Override
  public boolean shouldLearnFromNewFixations(int time) {
    return false;
  }

  /**
   * @param time
   * 
   * @return {@link java.lang.Boolean#FALSE} since the only reason a {@link 
   * jchrest.domainSpecifics.Fixation} set should end in Tileworld is if the 
   * maximum number of {@link jchrest.domainSpecifics.Fixation Fixations} have 
   * been attempted.
   */
  @Override
  public boolean isFixationSetComplete(int time) {
    return false;
  }

  /**
   *
   * @param time
   * 
   * @return {@link java.lang.Boolean#TRUE} since there are no additional checks
   * to be made when adding a new {@link jchrest.domainSpecifics.Fixation} in
   * {@link jchrest.architecture.Chrest#scheduleOrMakeNextFixation(
   * jchrest.domainSpecifics.Scene, boolean, int)}.
   */
  @Override
  public boolean shouldAddNewFixation(int time) {
    return true;
  }
  
  public int getTimeTakenToDecideUponMovementFixations(){
    return this._timeTakenToDecideUponMovementFixations;
  }
  
  public void setTimeTakenToDecideUponMovementFixations(int time){
    if(time < 0){
      throw new IllegalArgumentException(
        "The time specified to decide on a movement fixation is < 0 (" + time + ")."
      );
    }
    else{
      this._timeTakenToDecideUponMovementFixations = time;
    }
  }
  
  public int getTimeTakenToDecideUponSalientObjectFixations(){
    return this._timeTakenToDecideUponSalientObjectFixations;
  }
  
  public void setTimeTakenToDecideUponSalientObjectFixations(int time){
    if(time < 0){
      throw new IllegalArgumentException(
        "The time specified to decide on a salient object fixation is < 0 (" + 
        time + ")."
      );
    }
    else{
      this._timeTakenToDecideUponSalientObjectFixations = time;
    }
  }
}
