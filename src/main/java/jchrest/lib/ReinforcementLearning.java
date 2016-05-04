package jchrest.lib;

/**
 * Represents reinforcement learning theories currently supported in CHREST.
 * 
 * @author Martyn Lloyd-Kelly <mlk5060@liverpool.ac.uk>
 */
public class ReinforcementLearning{
  
  /**
   * Defines all reinforcement learning theories implemented in CHREST as enum
   * values.
   */
  public enum Theory{
    PROFIT_SHARING_WITH_DISCOUNT_RATE(4){
      
      /**
       * 
       * @param variables First element should be the reward, second element 
       * should be a discount rate, third element should be the time the reward
       * was awarded and the fourth element should be the time an action was 
       * performed.  See Arai, S., Sycara, K.P., Payne, T.R.: 
       * <i>Experience-based reinforcement learning to acquire effective 
       * behavior in a multi-agent domain.</i> In: Proceedings of the 6th 
       * Pacific Rim International Conference on Artificial Intelligence, pp. 
       * 125–135 (2000).
       * @return 
       */
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
    Theory (int numberOfVariablesExpected) {
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
  public static Theory[] getReinforcementLearningTheories(){
    return Theory.values();
  }
}
