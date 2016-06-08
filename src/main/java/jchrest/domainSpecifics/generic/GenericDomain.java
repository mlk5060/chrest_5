// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.domainSpecifics.generic;

import jchrest.domainSpecifics.DomainSpecifics;
import java.util.HashMap;
import java.util.List;
import jchrest.architecture.Chrest;
import jchrest.domainSpecifics.Fixation;
import jchrest.lib.ExecutionHistoryOperations;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.PrimitivePattern;
import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.fixations.AheadOfAgentFixation;
import jchrest.domainSpecifics.fixations.CentralFixation;
import jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation;
import jchrest.domainSpecifics.fixations.PeripheralItemFixation;
import jchrest.domainSpecifics.fixations.PeripheralSquareFixation;

/**
  * Default {@link jchrest.domainSpecifics.DomainSpecifics} used by a {@link 
  * jchrest.architecture.Chrest} instance.
  * 
  * @author Peter C. R. Lane <p.c.lane@herts.ac.uk>
  * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
  */
public class GenericDomain extends DomainSpecifics {
  
  private final int _peripheralItemFixationMaxAttempts;
  
  /**
   * 
   * @param model
   * @param maxFixationsInSet
   * @param peripheralItemFixationMaxAttempts See second parameter for {@link 
   * jchrest.domainSpecifics.fixations.PeripheralItemFixation#PeripheralItemFixation(
   * jchrest.architecture.Chrest, int, int)}.
   */
  public GenericDomain(Chrest model, Integer maxFixationsInSet, int peripheralItemFixationMaxAttempts) {
    super(model, maxFixationsInSet);
    this._peripheralItemFixationMaxAttempts = peripheralItemFixationMaxAttempts;
  }
  
  /**
   * @param pattern
   * @return A {@link jchrest.lib.ListPattern} stripped of {@link 
   * jchrest.lib.ItemSquarePattern}s that:
   * 
   * <ol type="1">
   *  <li>
   *    Represent the agent equipped with the {@link 
   *    jchrest.architecture.Chrest} model associated with {@link #this}.
   *  </li>
   *  <li> 
   *    Represent blind/empty {@link jchrest.lib.Square Squares}.
   *  </li>
   *  <li> 
   *    Are duplicated in the {@link jchrest.lib.ListPattern} passed.
   *  </li>
   * </ol>
   */
  @Override
  public ListPattern normalise (ListPattern pattern) {
    ListPattern result = new ListPattern(pattern.getModality());
    
    for(PrimitivePattern prim : pattern){
      if(prim instanceof ItemSquarePattern){
        ItemSquarePattern itemSquarePattern = (ItemSquarePattern)prim;
        String objectType = itemSquarePattern.getItem();
        if( 
          !objectType.equals(Scene.getCreatorToken()) &&
          !objectType.equals(Scene.getEmptySquareToken()) &&
          !objectType.equals(Scene.getBlindSquareToken()) &&
          !result.contains(prim)
        ){
          result.add(prim);
        } 
      }
      else{
        result.add(prim);
      }
    }
    
    if(this._associatedModel != null){
      HashMap<String, Object> historyRowToInsert = new HashMap<>();
      
      //Generic operation name setter for current method.  Ensures for the row to 
      //be added that, if this method's name is changed, the entry for the 
      //"Operation" column in the execution history table will be updated without 
      //manual intervention and "Filter By Operation" queries run on the execution 
      //history DB table will still work.
      class Local{};
      historyRowToInsert.put(Chrest._executionHistoryTableOperationColumnName, 
        ExecutionHistoryOperations.getOperationString(this.getClass(), Local.class.getEnclosingMethod())
      );
      historyRowToInsert.put(Chrest._executionHistoryTableInputColumnName, pattern.toString() + "(" + pattern.getModalityString() + ")");
      historyRowToInsert.put(Chrest._executionHistoryTableOutputColumnName, result.toString() + "(" + result.getModalityString() + ")");
      this._associatedModel.addEpisodeToExecutionHistory(historyRowToInsert);
    }
    
    return result;
  }

  /**
   * 
   * @param time
   * 
   * @return If {@link 
   * jchrest.architecture.Chrest#isLearningObjectLocationsRelativeToAgent()} 
   * returns {@link java.lang.Boolean#TRUE} when invoked in context of {@link 
   * #this#getAssociatedModel()}, a {@link 
   * jchrest.domainSpecifics.fixations.AheadOfAgentFixation} is returned.  
   * Otherwise, a {@link jchrest.domainSpecifics.fixations.CentralFixation} is
   * returned.
   */
  @Override
  public Fixation getInitialFixationInSet(int time) {
    if(this._associatedModel.isLearningObjectLocationsRelativeToAgent()){
      return new AheadOfAgentFixation(time, this._associatedModel.getTimeTakenToDecideUponAheadOfAgentFixation());
    }
    else{
      return new CentralFixation(time, this._associatedModel.getTimeTakenToDecideUponCentralFixation());
    }
  }

  /**
   * @param time
   * 
   * @return A {@link 
   * jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} should 
   * always be returned unless:
   * 
   * <ol type="1">
   *  <li>
   *    There is such a {@link jchrest.domainSpecifics.Fixation} already being 
   *    deliberated on by {@link #this#getAssociatedModel()} but hasn't been 
   *    performed yet.
   *  </li>
   *  <li>
   *    The most recent {@link jchrest.domainSpecifics.Fixation} attempted was 
   *    a {@link jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} 
   *    but it wasn't performed successfully.
   *  </li>
   * </ol>
   * 
   * In the first case, the outcome of attempting to make the {@link 
   * jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} is 
   * unknown so instead of generating another which may fail again (essentially 
   * wasting a {@link jchrest.domainSpecifics.Fixation} since only a finite 
   * number can be attempted (see {@link #this#getMaximumFixationsInSet()}), 
   * generate another type of {@link jchrest.domainSpecifics.Fixation}.
   * <p>
   * In the second case, the outcome of attempting to make a {@link 
   * jchrest.domainSpecifics.fixations.HypothesisDiscriminationFixation} is 
   * known and the attempt was unsuccessful so other {@link 
   * jchrest.domainSpecifics.Fixation Fixations} need to be attempted to try and 
   * replace the hypothesis present in the {@link jchrest.lib.Modality#VISUAL} 
   * {@link jchrest.architecture.Stm} of {@link #this#getAssociatedModel()} 
   * since its information is not useful in the current {@link 
   * jchrest.domainSpecifics.Scene} being fixated on.
   */
  @Override
  public Fixation getNonInitialFixationInSet(int time) {
    this._associatedModel.printDebugStatement("===== GenericDomain.getNonInitialFixationInSet() =====");
    this._associatedModel.printDebugStatement("- Performing at time " + time);
    
    Fixation fixation = null;
    
    List<Fixation> fixationsScheduled = this._associatedModel.getScheduledFixations(time);
    List<Fixation> fixationsAttempted = this._associatedModel.getPerceiver().getFixations(time);
    
    //Check for a HypothesisDiscriminationFixation currently being decided 
    //upon.
    this._associatedModel.printDebugStatement("- Checking for a HypothesisDiscriminationFixation scheduled at this time:");
    boolean hypothesisDiscriminationFixationBeingDeliberatedOn = false;
    for(Fixation fixationScheduled : fixationsScheduled){
      this._associatedModel.printDebugStatement(fixationScheduled.toString());
      if(fixationScheduled.getClass().equals(HypothesisDiscriminationFixation.class)){
        this._associatedModel.printDebugStatement("  ~ This is a HypothesisDiscriminationFixation, halting check");
        hypothesisDiscriminationFixationBeingDeliberatedOn = true;
        break;
      }
    }
      
    //Check for a recent attempt at a HypothesisDiscriminationFixation that
    //failed.
    this._associatedModel.printDebugStatement(
      "- Checking if the most recent Fixation attempted was a " +
      "HypothesisDiscriminationFixation that failed"
    );
    boolean mostRecentFixationAttemptedFailedAndWasHDF = false;
    Fixation mostRecentFixationAttempted;
    if(fixationsAttempted != null && fixationsAttempted.size() > 0){
      mostRecentFixationAttempted = fixationsAttempted.get(fixationsAttempted.size() - 1);
      this._associatedModel.printDebugStatement("  ~ Most recent Fixation attempted:" + mostRecentFixationAttempted.toString());
      
      mostRecentFixationAttemptedFailedAndWasHDF = (
        !mostRecentFixationAttempted.hasBeenPerformed() && 
        mostRecentFixationAttempted.getClass().equals(HypothesisDiscriminationFixation.class)
      );
      
      this._associatedModel.printDebugStatement(
        "  ~ This Fixation was " + (mostRecentFixationAttemptedFailedAndWasHDF ? "" : " not ") +
        "a failed HypothesisDiscriminationFixation"
      );
    }
    else{
      this._associatedModel.printDebugStatement("  ~ No Fixations attempted at this time");
    }
      
    if(
      hypothesisDiscriminationFixationBeingDeliberatedOn ||
      (
        !hypothesisDiscriminationFixationBeingDeliberatedOn &&
        mostRecentFixationAttemptedFailedAndWasHDF
      )
    ){
      
      this._associatedModel.printDebugStatement(
        "- Either a HypothesisDiscriminationFixation is currently being " +
        "deliberated on or the most recent Fixation attempted was a " +
        "HypothesisDiscriminationFixation that failed so a PeripheralItemFixation " +
        "or PeripheralSquareFixation will be returned with equal probability"
      );

      while(fixation == null){
        double r = Math.random();

        if(r < 0.5){
          fixation = new PeripheralItemFixation(this._associatedModel, this._peripheralItemFixationMaxAttempts, time, this._associatedModel.getTimeTakenToDecideUponPeripheralItemFixation());
        }
        else{
          fixation = new PeripheralSquareFixation(this._associatedModel, time, this._associatedModel.getTimeTakenToDecideUponPeripheralSquareFixation());
        }
      }
    }
    else{
      this._associatedModel.printDebugStatement(
        "- Its not the case that a HypothesisDiscriminationFixation is currently being " +
        "deliberated on or the most recent Fixation attempted was a " +
        "HypothesisDiscriminationFixation that failed so a HypothesisDiscriminationFixation " +
        "will be returned"
      );
      fixation = new HypothesisDiscriminationFixation(this._associatedModel, time);
    }
    
    this._associatedModel.printDebugStatement("- Returning " + fixation.toString());
    this._associatedModel.printDebugStatement("===== RETURN GenericDomain.getNonInitialFixationInSet() =====");
    return fixation;
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
   * 
   * @param time
   * 
   * @return {@link java.lang.Boolean#FALSE} since the only reason a {@link 
   * jchrest.domainSpecifics.Fixation} set should end in general is if the 
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
}
