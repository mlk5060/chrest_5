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
   * Retrieves a list of Strings produced after calling the 
   * {@link java.lang.Class#getName()} on all classes that extend the 
   * {@link jchrest.lib.DomainSpecifics} class from the {@link jchrest.lib} 
   * package.
   * 
   * @return 
   */
  public static ArrayList getDeclaredDomains(){
    ArrayList listOfDeclaredDomains = new ArrayList();
    Reflections reflections = new Reflections("jchrest.lib");
    Set<Class<? extends DomainSpecifics>> declaredDomains = reflections.getSubTypesOf(DomainSpecifics.class);
    for(Class<? extends DomainSpecifics> declaredDomain : declaredDomains){
      listOfDeclaredDomains.add(declaredDomain.getName());
    }
    return listOfDeclaredDomains;
  }
  
  public abstract int getCurrentTime();
  
  //Used to modify chunks in a domain-specific manner before they are input to a
  //CHREST proper.  For example, item-square patterns that denote empty space 
  //may need to be ignored in specific domains; this method allows you to 
  //implement such functionality.
  public abstract ListPattern normalise (ListPattern pattern);
  
  /**
   * Used to translate coordinates in {@link jchrest.lib.ItemSquarePattern}s
   * to zero-indexed coordinates so information can be used in a 
   * {@link jchrest.lib.Scene}.
   * 
   * @param listPattern
   * @param scene Can be used if the concrete implementation of this method 
   * needs to take into account whether some object (like the {@link 
   * jchrest.lib.Scene} creator) is present in a {@link jchrest.lib.Scene} to
   * convert coordinates differently.  For example, if the {@link 
   * jchrest.lib.Scene} creator is present, coordinates of objects may be 
   * translated so they are relative to the location of the {@link 
   * jchrest.lib.Scene} creator.  If the {@link jchrest.lib.Scene} creator is 
   * not present then coordinates may be absolute.
   * 
   * @return 
   */
  public abstract ListPattern convertDomainSpecificCoordinatesToSceneSpecificCoordinates(ListPattern listPattern, Scene scene);
  
  /**
   * Used to translate zero-indexed {@link jchrest.lib.Scene} coordinates in 
   * {@link jchrest.lib.ItemSquarePattern}s to domain-specific coordinates.
   * 
   * @param listPattern
   * @param scene Can be used if the concrete implementation of this method 
   * needs to take into account whether some object (like the {@link 
   * jchrest.lib.Scene} creator) is present in a {@link jchrest.lib.Scene} to
   * convert coordinates differently.  For example, if the {@link 
   * jchrest.lib.Scene} creator is present, coordinates of objects may be 
   * translated so they are relative to the location of the {@link 
   * jchrest.lib.Scene} creator.  If the {@link jchrest.lib.Scene} creator is 
   * not present then coordinates may be absolute.
   * 
   * @return 
   */
  public abstract ListPattern convertSceneSpecificCoordinatesToDomainSpecificCoordinates(ListPattern listPattern, Scene scene);
  
  public abstract List<Square> proposeMovementFixations (Scene scene, Square square);
  public abstract Set<Square> proposeSalientSquareFixations (Scene scene, Chrest model);
}

