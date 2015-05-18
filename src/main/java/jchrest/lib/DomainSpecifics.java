// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import java.util.List;
import java.util.Set;
import jchrest.architecture.Chrest;

/**
  * An interface for defining domain-specific methods.
  */
public abstract class DomainSpecifics {
  
  public Chrest _associatedModel;
  
  public DomainSpecifics(Chrest model){
    _associatedModel = model;
  }
  
  public abstract int getCurrentTime();
  
  //Used to modify chunks in a domain-specific manner before they are input to a
  //CHREST proper.  For example, item-square patterns that denote empty space 
  //may need to be ignored in specific domains; this method allows you to 
  //implement such functionality.
  public abstract ListPattern normalise (ListPattern pattern);
  
  public abstract List<Square> proposeMovementFixations (Scene scene, Square square);
  public abstract Set<Square> proposeSalientSquareFixations (Scene scene, Chrest model);
}

