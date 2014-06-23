package jchrest.lib;

/**
 * Represents reinforcement learning theories currently supported in CHEREST.
 * 
 * @author Martyn Lloyd-Kelly <mlk5060@liverpool.ac.uk>
 */
public class ReinforcementLearning{
  
  /**
   * Defines all reinforcement learning theories implemented in CHREST as enum
   * values.
   */
  public enum ReinforcementLearningTheories{
    PROFIT_SHARING_WITH_DISCOUNT_RATE;
  }
  
  /**
   * Returns an array of all reinforcement learning theory enum values.
   * 
   * @return 
   */
  public ReinforcementLearningTheories[] getReinforcementLearningTheories(){
    return ReinforcementLearningTheories.values();
  }
  
  /**
   * Calculates reinforcement values according to the reinforcement learning 
   * theory that is passed.  If the reinforcement learning theory passed does 
   * not have a method defined to calculate a reinforcement value a runtime
   * exception is thrown.
   * 
   * @param theory A reinforcement learning theory enum value.  Use 
   * {@link #getReinforcementLearningTheories()} to return all current 
   * reinforcement learning theory enum values.
   * 
   * @param variables The variables required in order for the calculation to be
   * performed.
   * 
   * @return 
   */
  public static Double calculateReinforcementValue(ReinforcementLearningTheories theory, Double[] variables){
    Double reinforcementValue = 0.00;
    boolean correctNumberOfVariables = true;
    int numberOfVariablesExpected = 0;
    
    switch(theory){
      case PROFIT_SHARING_WITH_DISCOUNT_RATE:
        numberOfVariablesExpected = 4;
        
        if(variables.length == numberOfVariablesExpected){
          reinforcementValue = variables[0] * Math.pow(variables[1], (variables[2] - variables[3]));
        }
        else{
          correctNumberOfVariables = false;
        }
        
        break;
      default:
        throw new RuntimeException("The specified reinforcement learning theory: " + theory.toString() + ", has no method defined to calculate reinforcement values!");
    }
    
    if(!correctNumberOfVariables){
      throw new RuntimeException("The " + theory.toString() + " reinforcement learning theory requires " + numberOfVariablesExpected + " variables but " + variables.length + " have been specified.");
    }
    
    return reinforcementValue;
  }
}
