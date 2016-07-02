// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.domainSpecifics;

import jchrest.architecture.Chrest;
import jchrest.lib.ListPattern;

/**
  * An interface for defining domain-specific methods.
  * 
  * @author Martyn Lloyd-Kelly {@code <martynlk@liverpool.ac.uk>}
  */
public abstract class DomainSpecifics {
  
  protected final Chrest _associatedModel;
  protected Integer _maxFixationsInSet;
  
  /**
   * 
   * @param model
   * @param maxFixationsInSet The maximum number of {@link 
   * jchrest.domainSpecifics.Fixation Fixations} that can be made in a set.
   * Pass {@code null} if this does not apply in the domain being modelled.
   */
  public DomainSpecifics(Chrest model, Integer maxFixationsInSet){
    this._associatedModel = model;
    this._maxFixationsInSet = maxFixationsInSet;
  }
  
  /////////////////////////////
  ///// CONCRETE METHODS //////
  /////////////////////////////
  
  public Chrest getAssociatedModel(){
    return this._associatedModel;
  }
  
  /**
   * Used by {@link jchrest.architecture.Chrest#scheduleOrMakeNextFixation(jchrest.domainSpecifics.Scene, 
   * boolean, boolean, int)}.
   * 
   * @return The maximum number of fixations that can be made in a set.  May
   * return {@code null}.
   */
  public Integer getMaximumFixationsInSet(){
    return this._maxFixationsInSet;
  }
  
  /**
   * 
   * @param maxFixationsInSet 
   */
  public void setMaximumFixationsInSet(int maxFixationsInSet){
    this._maxFixationsInSet = maxFixationsInSet;
  }
  
  ////////////////////////////
  ///// ABSTRACT METHODS /////
  ////////////////////////////
  
  /**
   * Used to prepare {@link jchrest.lib.ListPattern ListPatterns} for input to 
   * long-term memory by a {@link jchrest.architecture.Chrest} model.
   * 
   * Implementing this enables {@link jchrest.lib.Modality#VISUAL} 
   * {@link jchrest.lib.ListPattern ListPatterns} to be stripped of {@link 
   * jchrest.lib.ItemSquarePattern ItemSquarePatterns} that denote empty {@link 
   * jchrest.lib.Square Squares} in a {@link jchrest.domainSpecifics.Scene}, for
   * example.
   * 
   * @param pattern
   * @return 
   */
  public abstract ListPattern normalise(ListPattern pattern);
  
  /**
   * Used by {@link jchrest.architecture.Chrest#scheduleOrMakeNextFixation(jchrest.domainSpecifics.Scene, 
   * boolean, boolean, int)}.
   * 
   * @param time
   * 
   * @return Retrieves the initial {@link jchrest.domainSpecifics.Fixation} in 
   * a new set.
   */
  public abstract Fixation getInitialFixationInSet(int time);
  
  /**
   * Used by {@link jchrest.architecture.Chrest#scheduleOrMakeNextFixation(jchrest.domainSpecifics.Scene, 
   * boolean, boolean, int)}.
   * 
   * @param time
   * 
   * @return Retrieves a {@link jchrest.domainSpecifics.Fixation} in a set that
   * shouldn't be the initial {@link jchrest.domainSpecifics.Fixation} made.
   */
  public abstract Fixation getNonInitialFixationInSet(int time);
 
  /**
   * Used by {@link jchrest.architecture.Chrest#scheduleOrMakeNextFixation(jchrest.domainSpecifics.Scene, 
   * boolean, boolean, int)}.
   * 
   * @param time
   * 
   * @return 
   */
  public abstract boolean shouldAddNewFixation(int time);
  
  /**
   * Used by {@link jchrest.architecture.Chrest#scheduleOrMakeNextFixation(jchrest.domainSpecifics.Scene, 
   * boolean, boolean, int)} to determine if any {@link 
   * jchrest.domainSpecifics.Fixation Fixations} successfully performed by the 
   * {@link jchrest.architecture.Perceiver} associated with the {@link 
   * jchrest.architecture.Chrest} model invoking {@link 
   * jchrest.architecture.Chrest#scheduleOrMakeNextFixation(jchrest.domainSpecifics.Scene, 
   * boolean, boolean, int)} that have not been learned yet, should
   * be.
   * 
   * <b>NOTE:</b> {@link jchrest.architecture.Chrest#scheduleOrMakeNextFixation(jchrest.domainSpecifics.Scene, 
   * boolean, boolean, int)} already checks for a {@link 
   * jchrest.domainSpecifics.SceneObject} or {@link jchrest.lib.Square} being 
   * focused on again since the last time {@link 
   * jchrest.architecture.Chrest#scheduleOrMakeNextFixation(jchrest.domainSpecifics.Scene, 
   * boolean, boolean, int)} was invoked.
   * 
   * @param time
   * @return 
   */
  public abstract boolean shouldLearnFromNewFixations(int time);
  
  /**
   * Used by {@link jchrest.architecture.Chrest#scheduleOrMakeNextFixation(jchrest.domainSpecifics.Scene, 
   * boolean, boolean, int)} to determine if there are any 
   * conditions other than the maximum number of {@link 
   * jchrest.domainSpecifics.Fixation Fixations} in a set having been attempted 
   * that should signal the end of a {@link jchrest.domainSpecifics.Fixation} 
   * set.
   * 
   * @param time
   * 
   * @return
   */
  public abstract boolean isFixationSetComplete(int time);
}

