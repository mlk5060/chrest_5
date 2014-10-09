/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jchrest.lib;

/**
 *
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class MindsEyeMoveObjectException extends Exception{
  public MindsEyeMoveObjectException(String errorMsg){
    super(errorMsg);
  }
  
  public MindsEyeMoveObjectException(String errorMsg, Throwable cause){
    super(errorMsg, cause);
  }
}
