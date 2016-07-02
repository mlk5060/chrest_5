// Copyright (c) 2014, Martyn Lloyd-Kelly
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import java.lang.reflect.Method;

/**
 * Declares generic operations that can be performed by CHREST.  Allows for
 * consistent execution history modification and querying.
 * 
 * @author Martyn Lloyd-Kelly {@code <martynlk@liverpool.ac.uk>}
 */
public class ExecutionHistoryOperations {
  
  public static String getOperationString(Class clazz, Method method){
    return clazz.getSimpleName() + "." + method.getName();
  }
}
