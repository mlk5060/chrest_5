package jchrest.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Random;
import javax.swing.JOptionPane;
import jchrest.architecture.Chrest;

/**
 *
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class PairedAssociateExperiment extends Observable{
  
  //The CHREST model used in the experiment.
  private final Chrest _model;
  
  //The auditory loop that filters stimulus-response pairs input to CHREST.  The 
  //auditory loop is to become a part of the main CHREST architecture but for 
  //now its implementation is experiment-specific.  In this experiment, the 
  //auditory loop is a finite-sized priority queue and is used to store whole 
  //stimulus-response pairs to mimic "rehearsal" of these pairs.  The size of 
  //the loop is controlled by a JSpinner and each stimulus-response pair in the 
  //experiment has a priority that denotes where the pair should be placed in 
  //the loop when presented.  Pairs with a higher priority (1 being the highest)
  //are placed closer to the front of the list so, a pair with priority 1 will
  //occupy the first space in the list.  Only the first stimulus-response pair 
  //in the loop is processed at any time.  The model may use the auditory loop 
  //to "cheat" when providing responses i.e if its response using LTM isn't of
  //the length expected, it will simply use the information in the auditory loop
  //to provide a response to a stimulus (provided the stimulus-response pair is
  //present in the audioty loop when the stimulus that generated the incomplete
  //response is presented).  Only the first item is removed from the loop and
  //this occurs when the correct response is provided by the model using its LTM
  //rather than "cheating".  Attempts to add pairs occur when a response is not
  //generated using LTM.
  private final List<PairedPattern> _auditoryLoop = new ArrayList<>();
  
  //Stores the current experiment time.
  private int _exptClock = 0;
  
  //Stores how many stimulus-response pairs have been presented in a trial.
  //Zero-indexed to enable retrieval of stimulus-response pairs from data 
  //structures containing them.
  private int _stimulusResponseNumber = 0;
  
  //Stores the current trial number.  Zero-indexed to enable retrieval of 
  //stimulus-response pairs from data structures containing them.
  private int _trialNumber = 0;
  
  //Stores the current and original stimulus-response pairs and their priorities
  //so that they may be retrieved throughout the course of the experiment.  
  //Whereas the mappings in the current stimulus-response pairs data structure
  //may be shuffled (if requested), the original stimulus-response pair and 
  //priority mappings remain unchanged throughout the course of the experiment
  //unless the restart button is selected and new mappings are defined in the
  //stimulus-response-priority table GUI.
  private final Map<PairedPattern, Integer> _currentStimulusResponsePairsAndPriorities;
  private final Map<PairedPattern, Integer> _originalStimulusResponsePairsAndPriorities;
  
  //Stores whether the stimulus-response pairs have been shuffled for a trial 
  //and if set to false means that pairs will be shuffled if requested.  Pairs 
  //will not be shuffled if this value is set to true and shuffling is 
  //requested.  When set to true, this value won't be reset to false until the 
  //start of a new trial.
  private boolean _stimulusResponsePairsShuffledForTrial = false;
  
  //The maximum size of the auditory loop.  Changes made to this value will be
  //refelected in the auditory loop table (see above).
  private int _auditoryLoopMaxSize;
  
  //How long a stimulus-response pair is presented for after a test (ms).
  private int _presentationTime;
  
  //How long after a stimulus-response pair has been presented before the next
  //pair is presented.
  private int _interItemTime;
  
  //How long after a trial has finished before a new trial begins.
  private int _interTrialTime;
  
  //Stores the responses given to stimuli over trials in a two-dimensional data
  //structure.  The first dimension is an ordered list whose keys correspond to
  //trial numbers.  The second dimension is a map whose keys are stimuli in the 
  //experiment and whose values are responses.  The use of stimuli as keys in 
  //this second-dimension map enables responses in a trial to be unambiguously 
  //retrieved following a shuffle of stimulus-response pair mappings (which may 
  //occur on more than one trial).  This is important for rendering the 
  //"Response" table in the GUI correctly.
  private final List<Map<ListPattern, ListPattern>> _responses = new ArrayList<>();
  
  //Stores whether the model gave an erroneous response to stimuli over trials 
  //in a two-dimensional data structure.  The first dimension is an ordered list 
  //whose keys correspond to trial numbers.  The second dimension is a map whose 
  //keys are stimuli in the experiment and whose values are either 0 or 1.  The 
  //use of stimuli as keys in this second-dimension map enables responses in a 
  //trial to be unambiguously retrieved following a shuffle of stimulus-response 
  //pair mappings (which may occur on more than one trial).  This is important 
  //for rendering the "Response" table in the GUI correctly.  A value of 0 in
  //the second-dimension map indicates that the response given for the stimulus
  //indicated by the key was correct, a value of 1 means it was incorrect.
  //integers rather than booleans are used as values since an average 
  //correctness of the model over each trial needs to be calculated.
  private final List<HashMap<ListPattern, Integer>> _errors = new ArrayList<>();
  
  //Stores whether the model "cheated" to provide a respone to stimuli over 
  //trials in a two-dimensional data structure.  The first dimension is an 
  //ordered list whose keys correspond to trial numbers.  The second dimension 
  //is a map whose keys are stimuli in the experiment and whose values are 
  //boolean values indicating whether the model "cheated" to produce the 
  //response given the stimuli indicated by the key.  The use of stimuli as keys 
  //in this second-dimension map enables responses in a trial to be 
  //unambiguously retrieved following a shuffle of stimulus-response pair 
  //mappings (which may occur on more than one trial).  This is important for 
  //rendering the "Response" and "Errors" tables in the GUI correctly.  The 
  //values of the second-dimension map are used to flag "cheat" responses to the
  //user in the "Responses" and "Error" tables in the GUI.
  private final List<HashMap<ListPattern, Boolean>> _cheats = new ArrayList<>();
  
  public PairedAssociateExperiment(Chrest model, List<PairedPattern> patterns){
    
    //Assign the model instance and reset the model's attention clock so that the 
    //model's state and execution history can be rendered and updated correctly.
    this._model = model;
    this._model.resetAttentionClock();
    
    //Set the stimulus-response pair and priority variables as elements of a 
    //LinkedHashMap so that insertion order is retained.  This means that the
    //stimulus-response pair orderings will match their order in the original
    //data file.  Prevents user-confusion.
    this._currentStimulusResponsePairsAndPriorities = new LinkedHashMap<>();
    this._originalStimulusResponsePairsAndPriorities = new LinkedHashMap<>();
    for(int i = 0; i < patterns.size(); i++){
      this._currentStimulusResponsePairsAndPriorities.put(patterns.get(i), i + 1);
      this._originalStimulusResponsePairsAndPriorities.put(patterns.get(i), i + 1);
    }
  }
  
  /**
   * Checks that all priorities specified for stimulus-response pairs are 
   * greater than 0 and unique.  If neither condition is true, an error message
   * is thrown.
   * 
   * @return 
   */
  private boolean checkPriorities(){
    List<Integer> prioritiesDeclared = new ArrayList<>();
    
    for(Entry<PairedPattern, Integer> stimulusResponsePairAndPriority : this._currentStimulusResponsePairsAndPriorities.entrySet()){
      
      PairedPattern stimulusResponsePair = stimulusResponsePairAndPriority.getKey();
      Integer priorityDeclared = stimulusResponsePairAndPriority.getValue();
      
      if(priorityDeclared <= 0 ){
        JOptionPane.showMessageDialog(
          null,
          "The priority for stimulus-response pair " + stimulusResponsePair.getFirst().toString() + stimulusResponsePair.getSecond().toString()
            + " is less than or equal to 0.\n\n"
            + "Please rectify so that it is greater than 0.",
          "Stimulus-Response Priority Specification Error",
          JOptionPane.ERROR_MESSAGE
        );
        return false;
      }
      
      int currentNumberOfStimulusResponsePairsAndPriorities = this._currentStimulusResponsePairsAndPriorities.size();
      if(priorityDeclared > currentNumberOfStimulusResponsePairsAndPriorities){
        JOptionPane.showMessageDialog(
          null,
          "The priority for stimulus-response pair " + stimulusResponsePair.getFirst().toString() + stimulusResponsePair.getSecond().toString()
            + ") is greater than the number of stimulus-response pairs declared "
            + "(" + currentNumberOfStimulusResponsePairsAndPriorities + ").\n\n"
            + "Please rectify so that this priority is less than or equal to "
            + currentNumberOfStimulusResponsePairsAndPriorities + ".",
          "Stimulus-Response Priority Specification Error",
          JOptionPane.ERROR_MESSAGE
        );
        return false;
      }
      
      if(prioritiesDeclared.contains(priorityDeclared)){
        JOptionPane.showMessageDialog(
          null,
          "The priority for stimulus-response pair " + stimulusResponsePair.getFirst().toString() + stimulusResponsePair.getSecond().toString()
            + ") has already been used.\n\n"
            + "Please rectify so that the priority is unique.",
          "Stimulus-Response Priority Specification Error",
          JOptionPane.ERROR_MESSAGE
        );
        return false;
      }
      
      prioritiesDeclared.add(priorityDeclared);
    }
    
    return true;
  }
  
  /**
   * Converts a list of ListPatterns into a list of paired associate experiment
   * stimulus-response pattern pairs.
   */
  public static List<PairedPattern> makePairs (List<ListPattern> patterns) {
    List<PairedPattern> pairs = new ArrayList<PairedPattern> ();
    
    for (int i = 1; i < patterns.size (); ++i) {
      pairs.add (new PairedPattern (patterns.get(i-1), patterns.get(i)));
    }

    return pairs;
  }
  
  /**
   * Attempts to associate the second pattern with the first pattern of a 
   * specified pattern if they are both learned or attempts to learn and 
   * associate them every millisecond until the presentation time specified is 
   * reached.
   * 
   * @param shufflePresentationOrder Set to true if the presentation 
   * order of stimulus response pair presentation should be shuffled or false if
   * not.
   */
  public void processNextPattern(boolean shufflePresentationOrder){
    
    //If all priorities declared conform to specification then process the
    //current stimulus-response pair.
    if(this.checkPriorities()){
      
      _model.setEngagedInExperiment();
      _model.freeze (); // save all gui updates to the end
      
      if(shufflePresentationOrder){
        this.shuffleStimulusResponsePairs();
      }
      
      //If this is the first stimulus-response pair in a trial, add a new "row"
      //to the responses, cheats and errors data structures.
      if(this._stimulusResponseNumber == 0){
        this._responses.add( new HashMap<>() );
        this._cheats.add( new HashMap<>() );
        this._errors.add( new HashMap<>() ); 
      }

      //Set the presentation finish time and retrieve the stimulus-response pair
      //that is to be presented.
      int nextStimulusResponsePairPresentedTime = 
        this._exptClock + 
        this._presentationTime + 
        this._interItemTime;
      
      //On the last trial, the next stimulus-response presentation will occur
      //after the presentation time, inter-item time AND inter-trial time has 
      //elapsed so set the local "nextStimulusResponsePairPresentedTime" 
      //accordingly.
      if(this._stimulusResponseNumber == (this._originalStimulusResponsePairsAndPriorities.size() - 1) ){
        nextStimulusResponsePairPresentedTime += this._interTrialTime;
      }
      
      //First, test the model using the presented stimulus-response pair and 
      //record the outcome.  If the test returns false, this indicates that an 
      //incorrect response was given or the model *cheated* so add the currently 
      //presented item to the auditory loop so that it can be learned properly.
      PairedPattern presentedStimulusResponsePair = this.getCurrentStimulusResponsePair();
      if( !this.test(presentedStimulusResponsePair) ){
        
        //Now, update the auditory loop accordingly.
        //If the auditory loop is empty just add the current stimulus response pair.
        if(this._auditoryLoop.isEmpty()){
          this._auditoryLoop.add(presentedStimulusResponsePair);
        }
        //Auditory loop isn't empty so the current stimulus response pair will be 
        //inserted in the auditory loop according to its priority and the priority 
        //of existing stimulus-response pairs in the auditory loop.  This addition
        //should only occur if the presented stimulus-response pair isn't already
        //contained in the auditory loop.
        //
        //Functionality that checks for equal probabilities is superflous here 
        //since paired associate experiments should have disctinct priorities for
        //each stimulus-response pair.  However, this has been retained for when
        //the auditory loop becomes a fully-fleged part of the CHREST architecture.
        else if(!this._auditoryLoop.contains(presentedStimulusResponsePair)){
          
          Integer priorityOfPresentedStimulusResponsePair = this._currentStimulusResponsePairsAndPriorities.get(presentedStimulusResponsePair);
          
          //Create a boolean variable to enable appending the presented 
          //stimulus-response pair to the end of the auditory loop if all items 
          //in the auditory loop are currently of a higher or equal probability.
          boolean allItemsOfEqualOrHigherPriority = true;

          for(int i = 0; i < this._auditoryLoop.size(); i++){
            Integer priorityOfCurrentAuditoryLoopItem = this._currentStimulusResponsePairsAndPriorities.get( this._auditoryLoop.get(i) );

            //This conditional should be read as "if the priority of the presented 
            //stimulus-response pair is greater than the current auditory loop 
            //item".  If this is the case, add it at this point and stop searching
            //through the auditory loop so that the presented stimulus-response
            //pair is placed as close to the front of the auditory loop as 
            //possible and all items of a lower priority are "demoted".
            if(priorityOfPresentedStimulusResponsePair < priorityOfCurrentAuditoryLoopItem ){
              this._auditoryLoop.add(i, presentedStimulusResponsePair);
              allItemsOfEqualOrHigherPriority = false;
              break;
            }
          }

          //If all items in the auditory loop have priorities that are greater 
          //than or equal to the presented stimulus-response pair, append the
          //presented stimulus-response pair to the end of the auditory loop.
          if(allItemsOfEqualOrHigherPriority){
            this._auditoryLoop.add(presentedStimulusResponsePair);
          }

          //Trim off the excess from the auditory loop, if neccessary.
          while(this._auditoryLoop.size() > this._auditoryLoopMaxSize){
            this._auditoryLoop.remove(this._auditoryLoop.size() - 1);
          }
        }
      }
      
      //Until the time comes for the next stimulus response pair to be 
      //presented, associate and learn the first item in the auditory loop.
      while(_exptClock < nextStimulusResponsePairPresentedTime){
        if(!this._auditoryLoop.isEmpty()){
          PairedPattern stimulusResponseToLearn = this._auditoryLoop.get(0);
          ListPattern stimulus = stimulusResponseToLearn.getFirst();
          ListPattern response = stimulusResponseToLearn.getSecond();
          this._model.associateAndLearn(stimulus, response, _exptClock);
        }
        _exptClock += 1;
      }
      
      _stimulusResponseNumber++;
      _model.unfreeze();
      this.setChanged();
      
      if(this.isEndOfTrial()){
        _stimulusResponseNumber = 0;
        _trialNumber += 1;
        _stimulusResponsePairsShuffledForTrial = false;
        this.notifyObservers(Boolean.TRUE);
      }
      else{
        this.notifyObservers(Boolean.FALSE);
      }
    }
  }
  
  //TODO: Check that everything is reset correctly!
  public void restart(){
    String lastExperimentLocatedInName = _model.getExperimentsLocatedInNames().get(_model.getExperimentsLocatedInNames().size() - 1);
    lastExperimentLocatedInName = lastExperimentLocatedInName.substring(0, lastExperimentLocatedInName.lastIndexOf("-"));
    _model.addExperimentsLocatedInName(lastExperimentLocatedInName);
    
    _auditoryLoop.clear();
    _cheats.clear();
    _errors.clear();
    _exptClock = 0;
    _responses.clear ();
    _stimulusResponseNumber = 0;
    _trialNumber = 0;
    
    _stimulusResponsePairsShuffledForTrial = false;
    this.unshuffleStimulusResponsePairs();
    this.setChanged();
    this.notifyObservers(Boolean.TRUE);
  }
  
  
  public void runTrial(boolean shufflePresentationOrder){
    int currentTrial = _trialNumber;
    
    //When the last stimulus-response pair is presented, the trial number will
    //be incremented by 1 so at this point, presentation of stimulus-response
    //pairs should stop.
    while(currentTrial == _trialNumber){
      this.processNextPattern(shufflePresentationOrder);
    }
  }
  
  /**
   * Shuffle the stimulus-response pairs if this is the first pattern to be run 
   * in a new trial and patterns have not already been shuffled in this trial.
   */
  public void shuffleStimulusResponsePairs(){
    if(_stimulusResponseNumber == 0 && !_stimulusResponsePairsShuffledForTrial){
      
      //Since we can't shuffle a Map data structure, we need to recreate the
      //current stimulus-response-priority Map with a different order.  So:
      // 1) Make a copy of this Map's current contents so that the priority for 
      //    a stimulus-response pair can be assigned to the correct 
      //    stimulus-response pair after the shuffle.
      // 2) Create an ArrayList using the Map's keys since ArrayList elements 
      //    can be randomly accessed using random numbers. 
      // 3) Clear the current stimulus-response-priorities Map for repopulation.
      LinkedHashMap<PairedPattern, Integer> stimulusResponsePrioritiesBeforeShuffle = new LinkedHashMap<>(_currentStimulusResponsePairsAndPriorities);
      ArrayList<PairedPattern> stimulusResponsePairs = new ArrayList<>(stimulusResponsePrioritiesBeforeShuffle.keySet());
      _currentStimulusResponsePairsAndPriorities.clear();
      
      //Now, randomly select stimulus-response pairs from the ArrayList 
      //generated above and use this to put the stimulus-response pair and its
      //current priority into the now empty current stimulus-response-priority 
      //Map.
      Random random = new Random();
      random.setSeed(System.currentTimeMillis()); //Important: ensures that different series of random numbers are produced everytime (hopefully!)
      List<Integer> stimulusResponsePairsSelected = new ArrayList<>();
      Integer stimulusResponsePairSelected = random.nextInt(stimulusResponsePrioritiesBeforeShuffle.size());
      
      //Whilst the size of the shuffled stimulus-response-priorities data 
      //structure is less than the size of the previous 
      //stimulus-response-priorities data structure, add a randomly selected 
      //stimulus-response-priority entry to the shuffled structure.
      while( _currentStimulusResponsePairsAndPriorities.size() < stimulusResponsePrioritiesBeforeShuffle.size() ){
        
        //While the random number generated has already been used (indicating 
        //that a stimulus-response pair has already been reassigned or shuffled)
        //generate a new random number.
        while(stimulusResponsePairsSelected.contains(stimulusResponsePairSelected)){
          random.setSeed(System.currentTimeMillis()); //Important: ensures that different series of random numbers are produced everytime (hopefully!)
          stimulusResponsePairSelected = random.nextInt(stimulusResponsePrioritiesBeforeShuffle.size());
        }
        
        PairedPattern stimulusResponsePair = stimulusResponsePairs.get(stimulusResponsePairSelected);
        _currentStimulusResponsePairsAndPriorities.put(stimulusResponsePair, stimulusResponsePrioritiesBeforeShuffle.get(stimulusResponsePair));
        stimulusResponsePairsSelected.add(stimulusResponsePairSelected);
      }
      
      _stimulusResponsePairsShuffledForTrial = true;
      this.setChanged();
      this.notifyObservers(Boolean.FALSE);
    }
  }
  
  public void unshuffleStimulusResponsePairs(){
    if(_stimulusResponseNumber == 0 && _stimulusResponsePairsShuffledForTrial){
      this._stimulusResponsePairsShuffledForTrial = false;
      _currentStimulusResponsePairsAndPriorities.clear();
      _currentStimulusResponsePairsAndPriorities.putAll(_originalStimulusResponsePairsAndPriorities);
      this.setChanged();
      this.notifyObservers(Boolean.FALSE);
    }
  }
  
  /**
   * Tests the model by asking it to give the response associated with the 
   * stimulus passed as a parameter to this function.  The model may *cheat*
   * here i.e. if the stimulus-response pair it is being tested on is currently 
   * in its auditory loop, it will simply "recite" the response using this 
   * information.  Otherwise, the model's LTM is consulted.  Therefore, it may 
   * be that while a correct response is given in an earlier trial, the correct 
   * response may not be produced in a later one.
   * 
   * @param stimulusResponsePair The stimulus-response pair that the model 
   * should be tested on.
   */
  private boolean test (PairedPattern stimulusResponsePair) {
    
    boolean cheated = false;

    //Ostensibly, test the model.  This is actually the model testing itself to
    //determine if it knows the correct response i.e. it isn't saying to the
    //experimentor "I think the response is...", yet.
    ListPattern response = _model.associatedPattern (stimulusResponsePair.getFirst (), _exptClock);
    
    if(response == null || response.isEmpty() ){
      response = Pattern.makeVerbalList (new String[]{"NONE"});
    }
    else {
      //If the current response doesn't contain the same amount of information 
      //expected then the model can *cheat* and use the auditory loop to produce 
      //a response (if the stimulus-response pair is in the auditory loop).
      if(
        response.size() != stimulusResponsePair.getSecond().size() &&
        this._auditoryLoop.contains(stimulusResponsePair)
      ){
        for(int i = 0; i < this._auditoryLoop.size(); i++){
          PairedPattern stimulusResponseInLoop = this._auditoryLoop.get(i);

          if(stimulusResponseInLoop.equals(stimulusResponsePair)){
            response = stimulusResponseInLoop.getSecond();
            cheated = true;
            break;
          }
        }
      }
    }
    
    //At this point, the model has some response (other than nothing) and says 
    //"I think the response is..."
    response.setFinished();
    this._responses.get(this._trialNumber).put(stimulusResponsePair.getFirst(), response);
    this._cheats.get(this._trialNumber).put(stimulusResponsePair.getFirst(), cheated);
    
    //The experimenter reveals to the model what the expected response is. 
    if( response.equals(stimulusResponsePair.getSecond()) ){
      
      //If the response is correct set the error in this trial for the pattern 
      //to 0.
      _errors.get(_trialNumber).put( stimulusResponsePair.getFirst(), 0 );
       
      //Also, if the model cheated then false should be returned indicating 
      //that a correct response produced by LTM was not provided and therefore,
      //more learning of the stimulus-response pair should occur.
      if(cheated){
        return false;
      }
      //Otherwise, the model produced the correct response and didn't cheat so
      //no further learning of the presented stimulus-response pair needs to
      //occur.  Consequently, remove the prsented stimulus-response pair from 
      //the auditory loop (if it exists), update the auditory loop table model
      //and return true.
      else{
        this._auditoryLoop.remove(stimulusResponsePair);
        this.setChanged();
        this.notifyObservers(Boolean.FALSE);
        return true;
      }
    }
    else{
      //In this case, the model did not produce a correct response so, record an
      //error and return false, leaving the auditory loop as it is (hopefully
      //learning the presented stimulus-response).
      _errors.get(_trialNumber).put( stimulusResponsePair.getFirst(), 1 );
      return false;
    }
  }
  
  public boolean isEndOfTrial(){
    return _stimulusResponseNumber == _currentStimulusResponsePairsAndPriorities.size();
  }
  
  /****************************************************************************/
  /****************************************************************************/
  /**************************** GETTERS AND SETTERS ***************************/
  /****************************************************************************/
  /****************************************************************************/

  public Chrest getModel() {
    return _model;
  }

  public List<PairedPattern> getAuditoryLoop() {
    return _auditoryLoop;
  }
  
  /**
   * Retrieves the current stimulus-response pair to be learned and its priority.
   * 
   * @return 
   */
  public PairedPattern getCurrentStimulusResponsePair(){
    return this.getStimulusResponsePairsArrayFromKeysInMap(this._currentStimulusResponsePairsAndPriorities).get(this._stimulusResponseNumber);
  }
  
  /**
   * Convenience function that converts stimulus-response (PairedPattern) keys 
   * in a Map to a PairedPattern ArrayList and retains ordering of keys in Map.
   * 
   * @param hashMapToProcess
   * @return 
   */
  public ArrayList<PairedPattern> getStimulusResponsePairsArrayFromKeysInMap(Map<PairedPattern,?> mapToProcess){
    ArrayList<PairedPattern> stimulusResponsePairs = new ArrayList<>();
    Iterator<PairedPattern> iterator = mapToProcess.keySet().iterator();
    while(iterator.hasNext()){
      stimulusResponsePairs.add(iterator.next());
    }
    
    return stimulusResponsePairs;
  }
  
  /**
   * Retrieves the previous stimulus-response pair to be learned and its priority.
   * 
   * @return 
   */
  public Map<PairedPattern,Integer> getPreviousStimulusResponsePairAndPriority(){
    Map<PairedPattern,Integer> pairAndPriority = new HashMap<>();
    PairedPattern stimulusResponsePair = getStimulusResponsePairsArrayFromKeysInMap(this._currentStimulusResponsePairsAndPriorities).get(_stimulusResponseNumber - 1);
    pairAndPriority.put( stimulusResponsePair, _currentStimulusResponsePairsAndPriorities.get(stimulusResponsePair) );
    return pairAndPriority;
  }

  public int getExptClock() {
    return _exptClock;
  }

  public int getStimulusResponseNumber() {
    return _stimulusResponseNumber;
  }

  public int getTrialNumber() {
    return _trialNumber;
  }

  public Map<PairedPattern, Integer> getCurrentStimulusResponsePairsAndPriorities() {
    return _currentStimulusResponsePairsAndPriorities;
  }

  public Map<PairedPattern, Integer> getOriginalStimulusResponsePairsAndPriorities() {
    return _originalStimulusResponsePairsAndPriorities;
  }

  public boolean isStimulusResponsePairsShuffledForTrial() {
    return _stimulusResponsePairsShuffledForTrial;
  }

  public int getAuditoryLoopMaxSize() {
    return _auditoryLoopMaxSize;
  }

  public int getPresentationTime() {
    return _presentationTime;
  }

  public int getInterItemTime() {
    return _interItemTime;
  }

  public int getInterTrialTime() {
    return _interTrialTime;
  }

  public List<Map<ListPattern, ListPattern>> getResponses() {
    return _responses;
  }

  public List<HashMap<ListPattern, Integer>> getErrors() {
    return _errors;
  }

  public List<HashMap<ListPattern, Boolean>> getCheats() {
    return _cheats;
  }

  public void setExptClock(int _exptClock) {
    this._exptClock = _exptClock;
  }

  public void setStimulusResponseNumber(int _stimulusResponseNumber) {
    this._stimulusResponseNumber = _stimulusResponseNumber;
  }

  public void setTrialNumber(int _trialNumber) {
    this._trialNumber = _trialNumber;
  }

  public void setStimulusResponsePairsShuffledForTrial(boolean _stimulusResponsePairsShuffledForTrial) {
    this._stimulusResponsePairsShuffledForTrial = _stimulusResponsePairsShuffledForTrial;
  }
  
  public void setStimulusResponsePriority(PairedPattern stimulusResponse, int newPriority, boolean overwriteOriginalPriority){
    this._currentStimulusResponsePairsAndPriorities.put(stimulusResponse, newPriority);
    if(overwriteOriginalPriority){
      this._originalStimulusResponsePairsAndPriorities.put(stimulusResponse, newPriority);
    }
  }

  public void setAuditoryLoopMaxSize(int _auditoryLoopMaxSize) {
    this._auditoryLoopMaxSize = _auditoryLoopMaxSize;
  }

  public void setPresentationTime(int _presentationTime) {
    this._presentationTime = _presentationTime;
  }

  public void setInterItemTime(int _interItemTime) {
    this._interItemTime = _interItemTime;
  }

  public void setInterTrialTime(int _interTrialTime) {
    this._interTrialTime = _interTrialTime;
  }
}
