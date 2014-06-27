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
    PROFIT_SHARING_WITH_DISCOUNT_RATE(4){
      
      @Override
      public Double calculateReinforcementValue(Double[] variables){
        Double reinforcementValue = 0.00;
        if(this.correctNumberOfVariables(variables)){
          reinforcementValue = variables[0] * Math.pow(variables[1], (variables[2] - variables[3]));
        }
        return reinforcementValue;
      }
    };
    
    //Stores the number of variables expected by the reinforcement learning 
    //theory to calculate how much a node link should be reinforced by.
    private final int NUMBER_OF_VARIABLES_EXPECTED;

    /**
     * Constructor for ReinforcementLearningTheories enum values.
     * 
     * @param numberOfVariablesExpected The number of variables expected by the 
     * reinforcement learning theory to calculate how much a node link should be 
     * reinforced by.
     */
    ReinforcementLearningTheories (int numberOfVariablesExpected) {
      this.NUMBER_OF_VARIABLES_EXPECTED = numberOfVariablesExpected;
    }

    /**
     * Returns the current value of the ReinforcementLearningTheories' 
     * NUMBER_OF_VARIABLES_EXPECTED variable.
     * 
     * @return 
     */
    int getNumberOfVariablesExpected () {
      return NUMBER_OF_VARIABLES_EXPECTED;
    }
    
    /**
     * Checks to see if the length of "variables" is equal to the value of the
     * current ReinforcementLearningTheories' NUMBER_OF_VARIABLES_EXPECTED value.
     * Returns true if these values are equal and false if not.
     * 
     * @param variables The variables to be used to calculate how much the link
     * between two nodes should be reinforced by.
     * 
     * @return 
     */
    boolean correctNumberOfVariables (Double[] variables) {
      return variables.length == this.NUMBER_OF_VARIABLES_EXPECTED;
    }
    
    /**
     * Calculates a reinforcement value according to the implementation for a
     * particular reinforcement learning theory and returns the result.
     * 
     * @param variables The variables to be used to calculate how much the link
     * between two nodes should be reinforced by.
     * @return 
     */
    public abstract Double calculateReinforcementValue(Double[] variables);
  }
  
  /**
   * Returns an array of all reinforcement learning theory enum values.
   * 
   * @return 
   */
  public static ReinforcementLearningTheories[] getReinforcementLearningTheories(){
    return ReinforcementLearningTheories.values();
  }
}
