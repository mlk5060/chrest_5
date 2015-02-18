// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.architecture;

import jchrest.lib.FileUtilities;
import jchrest.lib.ListPattern;

/**
 * Represents a test link within the model's long-term memory.
 * The link has a test, which must be passed when sorting a pattern 
 * through to the child node.
 */
public class Link {
  
  private final ListPattern _test;
  private final Node _child;
  private final int _creationTime;
  private final String _createdInExperiment; //Used for drawing LTM.

  /**
   * Constructor sets the link's test and child node.
   */
  public Link (ListPattern test, Node child, int domainTime, String currentExperimentName) {
    _test = test;
    _child = child;
    _creationTime = domainTime;
    _createdInExperiment = currentExperimentName;
  }

  /**
   * Accessor to the link's child node.
   */
  public Node getChildNode () {
    return _child;
  }

  /**
   * Accessor to the link's test.
   */
  public ListPattern getTest () {
    return _test;
  }
  
  /**
   * Accessor to the time this link was created.
   * @return 
   */
  public int getCreationTime(){
    return _creationTime;
  }
  
  /**
   * Accessor to the name of the experiment this link was created in.
   * @return 
   */
  public String getExperimentCreatedIn(){
    return this._createdInExperiment;
  }

  /**
   * Test if the given pattern can be sorted through this test link.
   * A test passes if the test matches the given pattern.
   */
  public boolean passes (ListPattern pattern) {
    return _test.matches (pattern);
  }
}

