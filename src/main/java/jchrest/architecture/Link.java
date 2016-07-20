// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.architecture;

import java.io.Serializable;
import jchrest.lib.ListPattern;

/**
 * Represents a test link within the model's long-term memory.
 * The link has a test, which must be passed when sorting a pattern 
 * through to the child node.
 */
public class Link implements Serializable {
  
  private final ListPattern _test;
  private final Node _child;
  
  private final transient int _creationTime;
  private final String _createdInExperiment; //Used for drawing LTM.

  /**
   * Constructor.
   * 
   * @param test The test that must be passed for the child {@link 
   * jchrest.architecture.Node} of {@link #this} to be reached during LTM
   * retrieval.
   * @param child 
   * @param currentExperimentName 
   * @param time
   */
  public Link (ListPattern test, Node child, int time, String currentExperimentName) {
    _test = test;
    _child = child;
    _creationTime = time;
    _createdInExperiment = currentExperimentName;
  }

  public Node getChildNode () {
    return _child;
  }

  public ListPattern getTest () {
    return _test;
  }
  
  public int getCreationTime(){
    return _creationTime;
  }
  
  public String getExperimentCreatedIn(){
    return this._createdInExperiment;
  }

  /**
   * Check if the {@link jchrest.lib.ListPattern} specified {@link 
   * jchrest.lib.ListPattern#matches(jchrest.lib.Pattern)} the {@link 
   * jchrest.lib.ListPattern}
   * 
   * @param pattern
   * @return 
   */
  public boolean passes (ListPattern pattern) {
    return _test.matches (pattern);
  }
}

