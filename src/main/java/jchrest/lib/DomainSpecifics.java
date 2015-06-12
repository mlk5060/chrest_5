// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jchrest.architecture.Chrest;
import org.reflections.Reflections;

/**
  * An interface for defining domain-specific methods.
  */
public abstract class DomainSpecifics {
  
  public Chrest _associatedModel;
  
  public DomainSpecifics(Chrest model){
    _associatedModel = model;
  }
  
  /**
   * Retrieves all declared domains that extend the {@link 
   * jchrest.lib.DomainSpecifics} class.
   * 
   * @return 
   */
  public static ArrayList getDeclaredDomains(){
    ArrayList listOfDeclaredDomains = new ArrayList();
    Reflections reflections = new Reflections("jchrest.lib");
    for(Class<? extends DomainSpecifics> declaredDomain : reflections.getSubTypesOf(DomainSpecifics.class)){
      listOfDeclaredDomains.add(declaredDomain.getSimpleName());
    }
    return listOfDeclaredDomains;
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

