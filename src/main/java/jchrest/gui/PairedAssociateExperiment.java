// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;
import jchrest.architecture.Chrest;
import jchrest.lib.ListPattern;
import jchrest.lib.PairedPattern;
import jchrest.lib.Pattern;

/**
 * This panel provides an interface for running paired associate experiments.
 * 
 * @author Peter C. R. Lane
 * @author Martyn Lloyd-Kelly
 */
public class PairedAssociateExperiment extends JPanel {
  
  /****************************************************************************/
  /****************************************************************************/
  /********************** CHREST MODEL-RELATED VARIABLES **********************/
  /****************************************************************************/
  /****************************************************************************/
  
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
  
  /****************************************************************************/
  /****************************************************************************/
  /********************** EXPERIMENT OPERATION VARIABLES  *********************/
  /****************************************************************************/
  /****************************************************************************/
  
  //Stores the current experiment time.
  private int _exptClock;
  
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
  
  /****************************************************************************/
  /****************************************************************************/
  /******************************* GUI VARIABLES ******************************/
  /****************************************************************************/
  /****************************************************************************/
  
  //The window containing the experiment interface.  Used as an anchor point for 
  //error and save dialogs.
  private final Component _window;
  
  //Remaining variables in this section are organised according to their view
  //"sections" in the GUI from top-to-bottom then left-to-right.
  
  //////////////////////////////////////////////////////////////////////////////
  /////////////////////// Stimulus-response-priority view //////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  //Contains the stimulus-response-priority table.
  private JScrollPane _stimulusResponsePriorityView;
  
  //The data model that underpins the stimulus-response-priority table.  Changes
  //made here should be reflected in the stimulus-response-priority table.  This
  //model is itself underpinned by the current stimulus-response-priority data
  //structure (see above) so changes made to that structure should be reflected
  //in this model.
  private DefaultTableModel _stimulusResponsePriorityTableModel;
  
  //////////////////////////////////////////////////////////////////////////////
  ///////////////////////////// Auditory loop view /////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  //Contains the auditory loop table.
  private JScrollPane _auditoryLoopView;
  
  //The data model that underpins the auditory loop table.  Changes made here 
  //should be reflected in the auditory loop table.  This model is itself 
  //underpinned by the auditory loop data structure (see above) so changes made 
  //to that structure should be reflected in this model.
  private DefaultTableModel _auditoryLoopTableModel;
  
  //////////////////////////////////////////////////////////////////////////////
  ///////////////////////// Experiment information view ////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  //Contains experiment information (non-editable).
  private JPanel _experimentInformationView;
  
  //Displays the stimulus-response previously presented to the model in the 
  //current trial.
  private JLabel _previousStimulusResponseLabel;
  
  //Displays the stimulus-response that is to be presented to the model next in 
  //the current trial.
  private JLabel _nextStimulusResponseLabel;
  
  //Displays the current trial number.
  private JLabel _trialNumberLabel;
  
  //Displays the current experiment time.
  private JLabel _experimentTimeLabel;
  
  //////////////////////////////////////////////////////////////////////////////
  ////////////////////////// Experiment controls view //////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  //Contains experiment controls (all editable).
  private JPanel _experimentControlsView;
  
  //The maximum size of the auditory loop.  Changes made to this value will be
  //refelected in the auditory loop table (see above).
  private JSpinner _auditoryLoopMaxSize;
  
  //How long a stimulus-response pair is presented for after a test (ms).
  private JSpinner _presentationTime;
  
  //How long after a stimulus-response pair has been presented before the next
  //pair is presented.
  private JSpinner _interItemTime;
  
  //How long after a trial has finished before a new trial begins.
  private JSpinner _interTrialTime;
  
  //Controls whether stimulus-response pairs should be shuffled or not for a 
  //trial.
  private JCheckBox _randomOrder;
  
  //////////////////////////////////////////////////////////////////////////////
  /////////////////////////////// Responses view ///////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  //Contains the "Responses" table.
  private JScrollPane _responsesScrollPane;
  
  //Stored so that the user can see the latest responses for a trial 
  //automatically without having to scroll across themselves.
  private JScrollBar _responsesHorizontalBar;
  
  //The table that stores and displays responses over trials.  Stored here since
  //data table structures need to be fired on this table throughout the 
  //experiment.
  private JTable _responsesTable;
  
  //Should always be "Stimulus" and "Target" columns.
  private final int _numberDefaultColsResponsesTable = 2; 
  
  //Stores the responses given to stimuli over trials in a two-dimensional data
  //structure.  The first dimension is an ordered list whose keys correspond to
  //trial numbers.  The second dimension is a map whose keys are stimuli in the 
  //experiment and whose values are responses.  The use of stimuli as keys in 
  //this second-dimension map enables responses in a trial to be unambiguously 
  //retrieved following a shuffle of stimulus-response pair mappings (which may 
  //occur on more than one trial).  This is important for rendering the 
  //"Response" table in the GUI correctly.
  private final List<Map<ListPattern, ListPattern>> _responses = new ArrayList<>();
  
  //////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////// Errors view ////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  //Contains the "Errors" table.
  private JScrollPane _errorsScrollPane;
  
  //Stored so that the user can see the latest errors for a trial automatically 
  //without having to scroll across themselves.
  private JScrollBar _errorsHorizontalBar;
  
  //The table that stores and displays errors over trials.  Stored here since
  //data table structures need to be fired on this table throughout the 
  //experiment.
  private JTable _errorsTable;
  
  //Should always be "Stimulus" column.
  private final int _numberDefaultColsErrorsTable = 1;
  
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
  
  //////////////////////////////////////////////////////////////////////////////
  ////////////////////////// Response and errors views /////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
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
  
  /****************************************************************************/
  /****************************************************************************/
  /****************************************************************************/
  /****************************************************************************/
  /****************************************************************************/

  /**
   * Constructor for the experiment.
   * 
   * @param model The CHREST model to be used in the experiment.
   * @param patterns The stimulus-response pairs that are to be used in the 
   * experiment.
   */
  public PairedAssociateExperiment (Chrest model, List<PairedPattern> patterns) {
    super ();
    
    //Assign the model instance and reset the model's learning clock so that the 
    //model's state and execution history can be rendered and updated correctly.
    this._model = model;
    this._model.resetLearningClock();
    
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

    //Render the GUI.
    setLayout (new GridLayout (1, 1));
    JSplitPane jsp = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, this.renderInputView (), this.renderOutputView ());
    jsp.setOneTouchExpandable (true);
    this._window = add (jsp);
  }
  
  /****************************************************************************/
  /****************************************************************************/
  /******************************* GUI CREATION *******************************/
  /****************************************************************************/
  /****************************************************************************/
  
  //Code in this section is organised from the top-level to the bottom with 
  //regard to creation of components.

  /**
   * Renders a panel composed of the stimulus-response pattern list panel 
   * (created by {@link #renderStimulusResponsePairsPanel()}), the experiment 
   * information panel (created by {@link #renderExperimentInformation()} and 
   * the experiment control panel {@link #renderControlsView()}.  
   * 
   * The stimulus-target pattern list panel is aligned above the experiment 
   * control panel.
   * 
   * @return 
   */
  private JPanel renderInputView () {
    JPanel experimentInput = new JPanel ();
    experimentInput.setBorder(new TitledBorder ("Experiment Input"));
    experimentInput.setLayout (new BoxLayout(experimentInput, BoxLayout.PAGE_AXIS));
    
    //Render the components in order and add them afterwards since certain 
    //components depend on other component values being set.  However, laying
    //out the input view according to this dependency order may be confusing for
    //the user.
    renderStimulusResponsePrioritiesView();
    renderExperimentControlsView();
    renderAuditoryLoopView();
    renderExperimentInformationView();
    
    experimentInput.add(this._stimulusResponsePriorityView);
    experimentInput.add(this._auditoryLoopView);
    experimentInput.add(this._experimentInformationView);
    experimentInput.add(this._experimentControlsView);

    return experimentInput;
  }
  
  /**
   * Renders a panel composed of two panels that display the responses/ 
   * errors given/made by the CHREST model for each pattern over each trial.
   * Composed of the trials view {@link #renderResponsesView()} and errors view
   * {@link #renderErrorsView()}.
   * 
   * @return 
   */
  private JPanel renderOutputView () {
    JPanel experimentOutput = new JPanel ();
    experimentOutput.setBorder (new TitledBorder ("Experiment Output"));
    experimentOutput.setLayout (new GridLayout (2, 1));
    
    experimentOutput.add(renderResponsesView());
    experimentOutput.add(renderErrorsView());
    
    return experimentOutput;
  }
  
  
  /**
   * Creates a panel containing all stimulus-response pattern pairs to be used 
   * in the experiment (pairs created by {@link #makePairs(java.util.List)} 
   * and initialises them with empty priorities.
   * 
   * @return 
   */
  private void renderStimulusResponsePrioritiesView () {
    
    this._stimulusResponsePriorityTableModel = new DefaultTableModel(){
      
      @Override
      public boolean isCellEditable(int row, int column) {
        //Only the "Priority" column is editable before a trial has begun.
        return column == 2 && _stimulusResponseNumber == 0;
      }
    };
    
    this._stimulusResponsePriorityTableModel.addColumn("Stimulus");
    this._stimulusResponsePriorityTableModel.addColumn("Response");
    this._stimulusResponsePriorityTableModel.addColumn("Priority");
    
    this.populateStimulusResponsePriorityTableModel();
    
    JTable stimulusResponsePriorityJTable = new JTable(_stimulusResponsePriorityTableModel);
    stimulusResponsePriorityJTable.setBackground(Color.LIGHT_GRAY);
    stimulusResponsePriorityJTable.setShowHorizontalLines(false);
    stimulusResponsePriorityJTable.setShowVerticalLines(false);
    stimulusResponsePriorityJTable.setFillsViewportHeight(true);
    
    //Assign an action that retrieves the altered value for a priority in the 
    //stimulus-response-priority table displayed and assigns this to the 
    //current stimulus-response-pair-priority data structure.
    Action priorityReassignment = new AbstractAction() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        TableCellListener tcl = (TableCellListener) e.getSource();
        
        //First, flip the priority i.e. if the new priority assigned to the 
        //stimulus-response pair, P, is already a priority for another 
        //stimulus-response pair, P', then P' should be assigned the priority
        //that P had before the change since no two stimulus-response pairs may
        //have the same priority.  The casts here are required since data from
        //a JTable are String objects and can't be cast directly to Integer
        //objects.
        String oldPriorityString = (String)tcl.getOldValue();
        Integer oldPriorityInteger = Integer.valueOf(oldPriorityString);
        String newPriorityString = (String)tcl.getNewValue();
        Integer newPriorityInteger = Integer.valueOf(newPriorityString);
        
        //Loop through all rows in the stimulus-response-priority table model 
        //(except the row just edited, obviously) to determine what 
        //stimulus-response priority (if any) has the new priority already 
        //assigned.  If a row contains the new priority, this row number will be
        //used to update the data model that the stimulus-response-table model
        //is created from.
        for(int i = 0; i < PairedAssociateExperiment.this._stimulusResponsePriorityTableModel.getRowCount(); i++){
          if(i != tcl.getRow()){
            Integer priorityToCheck = Integer.valueOf((String)PairedAssociateExperiment.this._stimulusResponsePriorityTableModel.getValueAt(i, 2));
            if(Objects.equals(priorityToCheck, newPriorityInteger)){
              _currentStimulusResponsePairsAndPriorities.put( getStimulusResponsePairsArrayFromKeysInMap(_currentStimulusResponsePairsAndPriorities).get(i), oldPriorityInteger );
              break;
            }
          }
        }
        
        //Now, set the new priority for P in the data model that feeds the table
        //model.
        _currentStimulusResponsePairsAndPriorities.put( getStimulusResponsePairsArrayFromKeysInMap(_currentStimulusResponsePairsAndPriorities).get(tcl.getRow()), newPriorityInteger );
        
        //Finally, update the table model so that the table in the GUI displays
        //the updated priorities.
        PairedAssociateExperiment.this.populateStimulusResponsePriorityTableModel();
      }
    };
    TableCellListener tcl = new TableCellListener(stimulusResponsePriorityJTable, priorityReassignment);
    
    this._stimulusResponsePriorityView = new JScrollPane(stimulusResponsePriorityJTable);
    this._stimulusResponsePriorityView.setBorder (new TitledBorder ("Stimulus-Response Pairs and Priorities"));       
  }
  
  /**
   * Renders a panel containing useful experiment information that is updated
   * periodically.
   * 
   * @return 
   */
  private void renderExperimentInformationView(){
    
    //Define experiment information elements
    this._previousStimulusResponseLabel = new JLabel ("", SwingConstants.RIGHT); //Always empty when this function is called.
    this._nextStimulusResponseLabel = new JLabel (this.getNextStimulusResponseLabelContents(), SwingConstants.RIGHT);
    this._trialNumberLabel = new JLabel (Integer.toString(_trialNumber + 1), SwingConstants.RIGHT);
    this._experimentTimeLabel = new JLabel ("0", SwingConstants.RIGHT);
    
    //Create experiment interface element.
    this._experimentInformationView = new JPanel();
    this._experimentInformationView.setBorder(new TitledBorder("Experiment Information"));
    this._experimentInformationView.setLayout(new GridLayout(4, 2, 2, 2));
    
    this._experimentInformationView.add(new JLabel ("Prev. stimulus-response", SwingConstants.RIGHT));
    this._experimentInformationView.add(this._previousStimulusResponseLabel);
    
    this._experimentInformationView.add(new JLabel ("Next stimulus-response", SwingConstants.RIGHT));
    this._experimentInformationView.add(this._nextStimulusResponseLabel);
    
    this._experimentInformationView.add (new JLabel ("Trial #", SwingConstants.RIGHT));
    this._experimentInformationView.add(_trialNumberLabel);
    
    this._experimentInformationView.add (new JLabel ("Experiment time (ms)", SwingConstants.RIGHT));
    this._experimentInformationView.add(_experimentTimeLabel);
  }
  
  /**
   * Renders the JScrollPane that displays the auditory loop contents.
   */
  private void renderAuditoryLoopView(){
    this._auditoryLoopTableModel = new DefaultTableModel(){
      
      @Override
      public boolean isCellEditable(int row, int column) {
        //None of the table cells should be editable
        return false;
      }
    };
    
    _auditoryLoopTableModel.addColumn("Item");
    _auditoryLoopTableModel.addColumn("Content");
    this.populateAuditoryLoopTableModel();
    
    JTable auditoryLoopTable = new JTable(_auditoryLoopTableModel);
    auditoryLoopTable.setBackground(Color.WHITE);
    auditoryLoopTable.setShowHorizontalLines(false);
    auditoryLoopTable.setShowVerticalLines(false);
    auditoryLoopTable.setFillsViewportHeight(true);
    
    this._auditoryLoopView = new JScrollPane(auditoryLoopTable);
    this._auditoryLoopView.setBorder(new TitledBorder("Auditory Loop")); 
  }
  
  /**
   * Renders a panel containing all independent variables that can be set in the
   * experiment.
   * 
   * @return 
   */
  private void renderExperimentControlsView() {
    
    int numStimRespPairs = this._stimulusResponsePriorityTableModel.getRowCount();
    this._auditoryLoopMaxSize = new JSpinner (new SpinnerNumberModel (numStimRespPairs, 1, numStimRespPairs, 1));
    this._auditoryLoopMaxSize.setToolTipText("The number of stimulus-response pairs that may be held for learning by CHREST at any time");
    this._auditoryLoopMaxSize.addChangeListener((ChangeEvent e) -> {
      PairedAssociateExperiment.this.populateAuditoryLoopTableModel();
    });
    
    this._presentationTime = new JSpinner (new SpinnerNumberModel (2000, 1, 50000, 1));
    this._presentationTime.setToolTipText("The length of time each stimuli is presented for in a trial");
    
    this._interItemTime = new JSpinner (new SpinnerNumberModel (2000, 1, 50000, 1));
    this._interItemTime.setToolTipText("The length of time between presentation of each stimuli on each trial");
    
    this._interTrialTime = new JSpinner(new SpinnerNumberModel(2000, 1, 50000, 1));
    this. _interTrialTime.setToolTipText("The length of time between trials");
    
    this._randomOrder = new JCheckBox ();
    this._randomOrder.setToolTipText ("Check to present stimuli in a random order.  Shuffling of patterns only occurs at the start of a new trial.");
    this._randomOrder.addItemListener((ItemEvent e) -> {
      
      if (e.getStateChange() == ItemEvent.DESELECTED){
        
        //This check probably isn't necessary since the random order checkbox is
        //only enabled when pattern number is 0 but do it anyway.
        if(_stimulusResponseNumber == 0){ 
          this.resetStimulusResponsePriorities();
        }
      }
      else if(e.getStateChange() == ItemEvent.SELECTED){
        shuffleStimulusResponsePairs();
      }
    });
    
    JButton restart = new JButton (new RestartAction() );
    restart.setToolTipText ("Reset the experiment and clear the model");
    
    JButton learnPattern = new JButton (new LearnPatternAction() );
    learnPattern.setToolTipText ("Asks the model to learn a pair of stimulus-response patterns.  If this is the last pair, the model will generate output.");
    
    JButton runTrial = new JButton (new RunTrialAction() );
    runTrial.setToolTipText("Presents all remaining pattern pairs in a trial to the model and produces output.");
    
    JButton exportData = new JButton(new ExportDataAction());
    exportData.setToolTipText ("Export current experiment data as a CSV file to a specified location");
    
    //Set layout of the controls and add elements.
    this._experimentControlsView = new JPanel ();
    this._experimentControlsView.setBorder (new TitledBorder ("Experiment Controls"));
    this._experimentControlsView.setLayout (new GridLayout (7, 2, 2, 2));
    
    //Row 1
    this._experimentControlsView.add (new JLabel ("Size of Auditory Loop", SwingConstants.RIGHT));
    this._experimentControlsView.add(_auditoryLoopMaxSize);
    
    //Row 2
    this._experimentControlsView.add (new JLabel ("Presentation time (ms)", SwingConstants.RIGHT));
    this._experimentControlsView.add (_presentationTime);
    
    //Row 3
    this._experimentControlsView.add (new JLabel ("Inter item time (ms)", SwingConstants.RIGHT));
    this._experimentControlsView.add (_interItemTime);
    
    //Row 4
    this._experimentControlsView.add (new JLabel ("Inter trial time (ms)", SwingConstants.RIGHT));
    this._experimentControlsView.add (_interTrialTime);
    
    //Row 5
    this._experimentControlsView.add (new JLabel ("Randomise presentation", SwingConstants.RIGHT));
    this._experimentControlsView.add (_randomOrder);
    
    //Row 6
    this._experimentControlsView.add (restart);    this._experimentControlsView.add (learnPattern);
    
    //Row 7
    this._experimentControlsView.add (exportData); this._experimentControlsView.add (runTrial);
  }
  
  /**
   * Renders a panel containing a table (created by {@link #createResponsesTable()}
   * ) that tracks the responses given by the CHREST model for each 
   * stimulus-response pair when a trial has been completed.
   * 
   * @return 
   */
  private JPanel renderResponsesView(){
    createResponsesTable ();
    _responsesScrollPane = new JScrollPane (_responsesTable);
    _responsesHorizontalBar = _responsesScrollPane.getHorizontalScrollBar ();
    
    JPanel trialsView = new JPanel();
    trialsView.setBorder (new TitledBorder ("Responses"));
    trialsView.setLayout (new GridLayout ());
    trialsView.add(_responsesScrollPane);
    
    return trialsView;
  }
  
  /**
   * Renders a panel containing a table (created by {@link #createErrorsTable()}
   * ) that tracks the errors made by the CHREST model when providing a response
   * for each stimulus-response pair when a trial has been completed.
   * 
   * @return 
   */
  private JPanel renderErrorsView(){
    createErrorsTable();
    _errorsScrollPane = new JScrollPane (_errorsTable);
    _errorsHorizontalBar = _errorsScrollPane.getHorizontalScrollBar ();
    
    JPanel errorsView = new JPanel();
    errorsView.setBorder (new TitledBorder ("Errors"));
    errorsView.setLayout (new GridLayout ());
    errorsView.add(_errorsScrollPane);

    return errorsView;
  }
  
  /**
   * Returns the string to be displayed for the "Next Stimulus-Response" 
   * experiment information label.
   * 
   * @return 
   */
  private String getNextStimulusResponseLabelContents(){
    PairedPattern pair = this.getStimulusResponsePairsArrayFromKeysInMap(this._currentStimulusResponsePairsAndPriorities).get(this._stimulusResponseNumber);
    return pair.getFirst().toString() + pair.getSecond().toString();
  }
  
  /**
   * Returns the string to be displayed for the "Prev. Stimulus-Response" 
   * experiment information label.
   * 
   * @return 
   */
  private String getPreviousStimulusResponseLabelContents(){
    if(_stimulusResponseNumber == 0){
      return "";
    }
    else{
      PairedPattern pair = this.getStimulusResponsePairsArrayFromKeysInMap(this._currentStimulusResponsePairsAndPriorities).get(this._stimulusResponseNumber - 1);
      return pair.getFirst().toString() + pair.getSecond().toString();
    }
  }
  
  /**
   * Creates the responses table that tracks the responses given by the CHREST 
   * model for each stimulus presented over the course of multiple trials.
   */
  private void createResponsesTable () {
    
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        return _currentStimulusResponsePairsAndPriorities.size ();
      }
      
      @Override
      public int getColumnCount () {
        return _numberDefaultColsResponsesTable + PairedAssociateExperiment.this._responses.size(); 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        if (column == 0) {
          //See this.getNextStimulusResponseLabelContents() method for 
          //explanation of why this array conversion occurs.
          return getStimulusResponsePairsArrayFromKeysInMap(_originalStimulusResponsePairsAndPriorities).get(row).getFirst();
        } else if (column == 1) {
          //See this.getNextStimulusResponseLabelContents() method for 
          //explanation of why this array conversion occurs.
          return getStimulusResponsePairsArrayFromKeysInMap(_originalStimulusResponsePairsAndPriorities).get(row).getSecond();
        }
        else {
          String response = PairedAssociateExperiment.this._responses.get(column-2).get(getStimulusResponsePairsArrayFromKeysInMap(_originalStimulusResponsePairsAndPriorities).get(row).getFirst()).toString ();
          if(PairedAssociateExperiment.this._cheats.get(column-2).get(getStimulusResponsePairsArrayFromKeysInMap(_originalStimulusResponsePairsAndPriorities).get(row).getFirst())){
            
            //Replace the angled brackets in the "response" string with their
            //HTML encoded counterparts otherwise, some get lost in translation.
            response = "<html><b><font color=\"red\">" + response.replaceAll("\\<", "&#60;").replaceAll("\\>", "&#62;") + "</font></b></html>";
          }
          return response;
        }
      }
      
      @Override
      public String getColumnName (int column) {
        if (column == 0) {
          return "Stimulus";
        } else if (column == 1) {
          return "Target";
        } else {
          return "Trial " + (column - 1);
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        
        //Resizes the JTable columns so each one displays its widest cell 
        //content and scrolls the horizontal bar for the trials scroll pane to 
        //its maximum value (i.e. to the extreme right) to display the latest 
        //results.  The wait is required since the UI will update before a new 
        //maximum horizontal bar value can be set.  Also, the horizontal scroll
        //must occur after the column resize so that the widest point of the 
        //table is scrolled to.
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(_responsesTable);
        EventQueue.invokeLater (() -> {
          _responsesHorizontalBar.setValue (_responsesHorizontalBar.getMaximum ());
        });
      }
    };
    
    _responsesTable = new JTable (tm);
    _responsesTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(_responsesTable);
  }
  
  /**
   * Creates the errors table that tracks the errors made by the CHREST model 
   * for each response given a presented stimulus over the course of multiple 
   * trials.
   */
  private void createErrorsTable(){
    TableModel tm = new AbstractTableModel () {

      @Override
      public int getRowCount() {
        
        //Should only have rows for the patterns declared plus 1 for the "% 
        //correct" row at bottom of table.
        return _currentStimulusResponsePairsAndPriorities.size() + 1;
      }

      @Override
      public int getColumnCount() {
        return _numberDefaultColsErrorsTable + _responses.size();
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
        
        //First column
        if(columnIndex == 0){
          
          //If this isn't the bottom row, get the relevant stimulus ListPattern 
          //for the row.  Use the original stimulus-response-priorities data
          //structure for this since the patterns may have been shuffled.  This
          //renders use of the current stimulus-response-priorities data
          //structure problematic since rows would need to be re-drawn and this
          //may be confusing for the user.
          if(rowIndex < (getRowCount() - 1) ){
            return getStimulusResponsePairsArrayFromKeysInMap(_originalStimulusResponsePairsAndPriorities).get(rowIndex).getFirst();
          }
          //For the bottom row, return a "header" for % correct.
          else{
            return "<html><b>&#37; Correct</b></html>";
          }
        }
        //Second column onwards
        else {
          
          //If this isn't the bottom row, get the relevant error value for the
          //stimulus.
          if(rowIndex < (getRowCount() - 1) ){
            String value = String.valueOf(_errors.get(columnIndex - 1).get(getStimulusResponsePairsArrayFromKeysInMap(_originalStimulusResponsePairsAndPriorities).get(rowIndex).getFirst()));
            if(PairedAssociateExperiment.this._cheats.get(columnIndex - 1).get(getStimulusResponsePairsArrayFromKeysInMap(_originalStimulusResponsePairsAndPriorities).get(rowIndex).getFirst())){
              value = "<html><b><font color=\"red\"> " + value + " </font></b></html>";
            }
            return value;
          }
          //For the bottom row, calculate the percentage of correct responses
          //given over the whole trial to 2 decimal places.
          else{
            Collection<Integer> values = _errors.get(columnIndex - 1).values();
            int totalErrors = 0;
            for(Integer value: values){
              totalErrors += value;
            }
            
            double result = 100 - (
              ( ((double)totalErrors) / _errors.get(columnIndex - 1).size() ) 
              * 100 
            ); //Casting "totalErrors" to double will promote denominator to double too.
            String resultString = String.format("%.2f%n", result);
            resultString = "<html><b>" + resultString + "</b></html>";
            return resultString;
          }
          
        }
      }
      
      @Override
      public String getColumnName (int columnIndex) {
        if (columnIndex == 0) {
          return "Stimulus";
        } else {
          return "Trial " + columnIndex;
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        
        //Resizes the JTable columns so each one displays its widest cell 
        //content and scrolls the horizontal bar for the errors scroll pane to 
        //its maximum value (i.e. to the extreme right) to display the latest 
        //results.  The wait is required since the UI will update before a new 
        //maximum horizontal bar value can be set.  Also, the horizontal scroll
        //must occur after the column resize so that the widest point of the 
        //table is scrolled to.
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(_errorsTable);
        EventQueue.invokeLater (() -> {
          _errorsHorizontalBar.setValue (_errorsHorizontalBar.getMaximum ());
        });
      }
    };
    
    _errorsTable = new JTable (tm);
    _errorsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(_errorsTable);
  }

  /****************************************************************************/
  /****************************************************************************/
  /******************************* BUTTONS CODE *******************************/
  /****************************************************************************/
  /****************************************************************************/
  
  //Code in this section pertains to the operation of GUI buttons.
  
  class RestartAction extends AbstractAction implements ActionListener {
    
    RestartAction () {
      super ("Restart");
    }

    @Override
    //TODO: Check that everything is reset correctly!
    public void actionPerformed (ActionEvent e) {
      String lastExperimentLocatedInName = _model.getExperimentsLocatedInNames().get(_model.getExperimentsLocatedInNames().size() - 1);
      _model.setNotEngagedInExperiment();
      _model.addExperimentsLocatedInName(lastExperimentLocatedInName);
      
      _auditoryLoop.clear();
      _cheats.clear();
      _errors.clear();
      _exptClock = 0;
      _model.clear ();
      _responses.clear ();
      _stimulusResponseNumber = 0;
      _trialNumber = 0;
      
      ((AbstractTableModel)_responsesTable.getModel()).fireTableStructureChanged();
      ((AbstractTableModel)_errorsTable.getModel()).fireTableStructureChanged();
      PairedAssociateExperiment.this.populateAuditoryLoopTableModel();
      resetStimulusResponsePriorities();
    }
  }

  class LearnPatternAction extends AbstractAction implements ActionListener {
    
    LearnPatternAction () {
      super ("Present Next");
    }
    
    @Override
    public void actionPerformed (ActionEvent e) {
      _model.setEngagedInExperiment();
      shuffleStimulusResponsePairs();
      _model.freeze (); // save all gui updates to the end
      processPattern();
      checkEndTrial();
      updateExperimentInformation();

      _model.unfreeze();
    }  
  }
  
  class RunTrialAction extends AbstractAction implements ActionListener {
    
    RunTrialAction(){
      super ("Present Remaining");
    }
      
    @Override
    public void actionPerformed(ActionEvent e){
      
      _model.setEngagedInExperiment();
      shuffleStimulusResponsePairs();
      _model.freeze();
      while(_stimulusResponseNumber < _currentStimulusResponsePairsAndPriorities.size()){
        processPattern();
      }
      checkEndTrial();
      updateExperimentInformation();

      _model.unfreeze();
    }
  }
  
  class ExportDataAction extends AbstractAction implements ActionListener {
    
    ExportDataAction () {
      super ("Export Data");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      
      ArrayList<String> trialDataToSave = new ArrayList<>();
      ArrayList<String> errorDataToSave = new ArrayList<>();
      
      trialDataToSave.add(ExportData.extractJTableDataAsCsv(_responsesTable));
      trialDataToSave.add("trialData");
      trialDataToSave.add("csv");
      
      errorDataToSave.add(ExportData.extractJTableDataAsCsv(_errorsTable));
      errorDataToSave.add("errorData");
      errorDataToSave.add("csv");
      
      ArrayList<ArrayList<String>> dataToSave = new ArrayList<>();
      dataToSave.add(trialDataToSave);
      dataToSave.add(errorDataToSave);
      
      ExportData.saveFile(_window, "CHREST-paired-associate-experiment-data", dataToSave);
    }
  }
  
  /****************************************************************************/
  /****************************************************************************/
  /******************************* SHARED CODE ********************************/
  /****************************************************************************/
  /****************************************************************************/
  
  //Code in this section is either not associated with GUI components or more 
  //than one GUI component.
  
  /**
   * Check if a trial has ended (all patterns have been presented to CHREST) and
   * test the model, update experiment variables and GUI if so.
   */
  private void checkEndTrial(){
    if(_stimulusResponseNumber == _currentStimulusResponsePairsAndPriorities.size()){
      ((AbstractTableModel)_responsesTable.getModel()).fireTableStructureChanged();
      ((AbstractTableModel)_errorsTable.getModel()).fireTableStructureChanged();
      _stimulusResponseNumber = 0;
      _trialNumber += 1;
      _stimulusResponsePairsShuffledForTrial = false;
      shuffleStimulusResponsePairs();
      this._randomOrder.setEnabled(true);
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
    
    for(int i = 0; i < this._stimulusResponsePriorityTableModel.getRowCount(); i++){
      Integer priorityDeclared = Integer.valueOf( (String)this._stimulusResponsePriorityTableModel.getValueAt(i, 2) );
      
      if(priorityDeclared <= 0 ){
        String erroneousStimulusResponsePriority = (String)this._stimulusResponsePriorityTableModel.getValueAt(i, 0) + (String)this._stimulusResponsePriorityTableModel.getValueAt(i, 0);
        JOptionPane.showMessageDialog(
          this._window,
          "The priority for stimulus-response pair " + i + " (" + 
            erroneousStimulusResponsePriority
            + ") is less than or equal to 0.\n\n"
            + "Please rectify so that it is greater than 0.",
          "Stimulus-Response Priority Specification Error",
          JOptionPane.ERROR_MESSAGE
        );
        return false;
      }
      
      if(priorityDeclared > this._currentStimulusResponsePairsAndPriorities.size()){
        String erroneousStimulusResponsePriority = (String)this._stimulusResponsePriorityTableModel.getValueAt(i, 0) + (String)this._stimulusResponsePriorityTableModel.getValueAt(i, 0);
        JOptionPane.showMessageDialog(
          this._window,
          "The priority for stimulus-response pair " + i + " (" + 
            erroneousStimulusResponsePriority
            + ") is greater than the number of stimulus-response pairs declared "
            + "(" + this._currentStimulusResponsePairsAndPriorities.size() + ").\n\n"
            + "Please rectify so that this priority is less than or equal to "
            + this._currentStimulusResponsePairsAndPriorities.size() + ".",
          "Stimulus-Response Priority Specification Error",
          JOptionPane.ERROR_MESSAGE
        );
        return false;
      }
      
      if(prioritiesDeclared.contains(priorityDeclared)){
        String erroneousStimulusResponsePriority = (String)this._stimulusResponsePriorityTableModel.getValueAt(i, 0) + (String)this._stimulusResponsePriorityTableModel.getValueAt(i, 0);
        JOptionPane.showMessageDialog(
          this._window,
          "The priority for stimulus-response pair " + i + " (" + 
            erroneousStimulusResponsePriority
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
   * Convenience function that converts stimulus-response (PairedPattern) keys 
   * in a Map to a PairedPattern ArrayList and retains ordering of keys in Map.
   * 
   * @param hashMapToProcess
   * @return 
   */
  private ArrayList<PairedPattern> getStimulusResponsePairsArrayFromKeysInMap(Map<PairedPattern,?> mapToProcess){
    ArrayList<PairedPattern> stimulusResponsePairs = new ArrayList<>();
    Iterator<PairedPattern> iterator = mapToProcess.keySet().iterator();
    while(iterator.hasNext()){
      stimulusResponsePairs.add(iterator.next());
    }
    
    return stimulusResponsePairs;
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
  
  /**
   * Converts a list of ListPatterns into a list of stimulus-response pattern 
   * pairs.
   */
  public static List<PairedPattern> makePairs (List<ListPattern> patterns) {
    List<PairedPattern> pairs = new ArrayList<PairedPattern> ();
    for (int i = 1; i < patterns.size (); ++i) {
      pairs.add (new PairedPattern (patterns.get(i-1), patterns.get(i)));
    }

    return pairs;
  }
  
  /**
   * Populates the auditory loop table model according to the contents of the
   * auditory loop causing the auditory loop contents table to update.
   */
  private void populateAuditoryLoopTableModel(){
    
    //Check for a non-empty table model.
    if(this._auditoryLoopTableModel.getRowCount() > 0){
      
      //Since the DefaultTableModel.removeRow() function will cause the row 
      //count to alter dynamically, not all rows will be removed if the row 
      //index is incremented after every iteration.  Instead, the first row 
      //index to be removed should be the last row (rows are zero-indexed hence
      //the subtraction of 1 when initialising i) and decrement the row index 
      //until it is less than 0 (the first row).
      int numberOfRows = this._auditoryLoopTableModel.getRowCount();
      for(int i = (numberOfRows - 1); i >= 0; i--){
        this._auditoryLoopTableModel.removeRow(i);
      }
    }
    
    //Now re-populate the data model.
    for(int i = 0; i < (int)this._auditoryLoopMaxSize.getValue(); i++){
      String content = "";
      if(i < this._auditoryLoop.size()){
        PairedPattern stimulusResponse = this._auditoryLoop.get(i);
        content = stimulusResponse.getFirst().toString() + stimulusResponse.getSecond().toString();
      }
      
      Object[] rowData = {
        (i + 1),
        content
      };
      _auditoryLoopTableModel.addRow(rowData);
    }
  }
  
  /**
   * Populates the "_stimulusResponsePriorityTableModel" with information 
   * according to the current state of the 
   * "_currentStimulusResponsePairsAndPriorities" instance variable.  Since this
   * model is the information store for the stimulus-response-priority table, 
   * the table will also be updated accordingly.
   * 
   * If the model already has rows (other than headers), it is first cleared, 
   * then populated.
   */
  private void populateStimulusResponsePriorityTableModel(){
    
    //Check for a non-empty table model.
    if(this._stimulusResponsePriorityTableModel.getRowCount() > 0){
      
      //Since the DefaultTableModel.removeRow() function will cause the row 
      //count to alter dynamically, not all rows will be removed if the row 
      //index is incremented after every iteration.  Instead, the first row 
      //index to be removed should be the last row (rows are zero-indexed hence
      //the subtraction of 1 when initialising i) and decrement the row index 
      //until it is less than 0 (the first row).
      int numberOfRows = this._stimulusResponsePriorityTableModel.getRowCount();
      for(int i = (numberOfRows - 1); i >= 0; i--){
        this._stimulusResponsePriorityTableModel.removeRow(i);
      }
    }
    
    //Now re-populate the data model so that its order matches that of the 
    //current stimulus-response-priority model.
    for(Entry<PairedPattern, Integer> pairedPatternAndPriority : this._currentStimulusResponsePairsAndPriorities.entrySet()){
      PairedPattern pairedPattern = pairedPatternAndPriority.getKey();
      Object[] rowData = {
        pairedPattern.getFirst().toString (),
        pairedPattern.getSecond().toString (),
        pairedPatternAndPriority.getValue().toString()
      };
      _stimulusResponsePriorityTableModel.addRow(rowData);
    }
  }
  
  /**
   * Attempts to associate the second pattern with the first pattern of a 
   * specified pattern if they are both learned or attempts to learn and 
   * associate them every millisecond until the presentation time specified is 
   * reached.
   */
  private void processPattern(){
    
    //If all priorities declared conform to specification then process the
    //current stimulus-response pair.
    if(this.checkPriorities()){
    
      //Whenever this procedure is called, disable the randomised order checkbox
      //just to be sure that its always disabled until a trial is complete.
      this._randomOrder.setEnabled(false);

      //Set the presentation finish time and retrieve the stimulus-response pair
      //that is to be presented.
      int nextStimulusResponsePairPresentedTime = 
        ((SpinnerNumberModel)_presentationTime.getModel()).getNumber().intValue() + 
        ((SpinnerNumberModel)_interItemTime.getModel()).getNumber().intValue () + 
        _exptClock;
      
      //On the last trial, the next stimulus-response presentation will occur
      //after the presentation time, inter-item time AND inter-trial time has 
      //elapsed so set the local "nextStimulusResponsePairPresentedTime" 
      //accordingly.
      if(this._stimulusResponseNumber == (this._originalStimulusResponsePairsAndPriorities.size() - 1) ){
        nextStimulusResponsePairPresentedTime += (int)this._interTrialTime.getValue();
      }
      
      PairedPattern presentedStimulusResponsePair = this.getCurrentStimulusResponsePair();
      
      //First, test the model using the presented stimulus-response pair and 
      //record the outcome.  If the test returns false, this indicates that an 
      //incorrect response was given or the model *cheated* so add the currently 
      //presented item to the auditory loop so that it can be learned properly.
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
          if(this._auditoryLoop.size() > (int)this._auditoryLoopMaxSize.getValue()){
            this._auditoryLoop.remove(this._auditoryLoop.size() - 1);
          }
        }

        //Update the auditory loop table model.
        this.populateAuditoryLoopTableModel();
      }
      
      while(_exptClock < nextStimulusResponsePairPresentedTime){
        if(!this._auditoryLoop.isEmpty()){
          PairedPattern stimulusResponseToLearn = this._auditoryLoop.get(0);
          this._model.associateAndLearn(stimulusResponseToLearn.getFirst(), stimulusResponseToLearn.getSecond(), _exptClock);
        }
        _exptClock += 1;
      }
      _stimulusResponseNumber++;
    }
  }
  
  /**
   * Resets the "_currentStimulusResponsePairsAndPriorities" data structure to
   * its original state and update table view and experiment control view.
   */
  private void resetStimulusResponsePriorities(){
    this._stimulusResponsePairsShuffledForTrial = false;
    this._randomOrder.setEnabled(true);
    this._randomOrder.setSelected(false);
    this._currentStimulusResponsePairsAndPriorities.clear();
    this._currentStimulusResponsePairsAndPriorities.putAll(this._originalStimulusResponsePairsAndPriorities);
    this.populateStimulusResponsePriorityTableModel();
    this.updateExperimentInformation();
  }
  
  /**
   * Shuffle the stimulus-response pairs if this is the first pattern to be run 
   * in a new trial and patterns are to be presented randomly.
   */
  private void shuffleStimulusResponsePairs(){
    if(_stimulusResponseNumber == 0 && _randomOrder.isSelected () && !_stimulusResponsePairsShuffledForTrial){
      
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
      
      this.populateStimulusResponsePriorityTableModel();
      updateExperimentInformation();
      _stimulusResponsePairsShuffledForTrial = true;
    }
  }
  
  /**
   * Tests the model by asking it to give the response associated with the 
   * stimulus passed as a parameter to this function.  The model may *cheat*
   * here in that, if the stimulus-response pair it is being tested on is 
   * currently in its auditory loop, it will simply recite the response using
   * this information.  Otherwise, the model's LTM is consulted.  Therefore, it
   * may be that while a correct response is given in an earlier trial, the 
   * correct response may not be produced in a later one.
   * 
   * @param stimulusResponsePair The stimulus-response pair that the model 
   * should be tested on.
   */
  private boolean test (PairedPattern stimulusResponsePair) {
    
    //If this is the first time a test has been run in a trial, instantiate the 
    //data structures relating to output displays.
    if(this._stimulusResponseNumber == 0){
      this._responses.add( new HashMap<ListPattern, ListPattern>() );
      this._cheats.add( new HashMap<ListPattern, Boolean>() );
      this._errors.add( new HashMap<ListPattern, Integer>() ); 
    }
    
    boolean cheated = false;

    //Ostensibly, test the model.  This is actually the model testing itself to
    //determine if it knows the correct response i.e. it isn't saying to the
    //experimentor "I think the response is...", yet.
    ListPattern response = _model.associatedPattern (stimulusResponsePair.getFirst (), _exptClock);
    
    //If the model responds with nothing then the model states that this is its
    //response for this presented stimulus-response pair in this trial and 
    //data structures related to viewing experiment output are updated and the
    //function returns with false, indicating that the response provided was
    //incorrect.  Implicit in this situation is the fact that the experimenter 
    //has revealed the expected response.
    if(response == null || response.isEmpty() ){
      this._responses.get(this._trialNumber).put (stimulusResponsePair.getFirst(), Pattern.makeVerbalList (new String[]{"NONE"}));
      this._cheats.get(this._trialNumber).put(stimulusResponsePair.getFirst(), false);
      _errors.get(_trialNumber).put( stimulusResponsePair.getFirst(), 1 );
      return false;
    }
    else {
      //If the current response isn't null but doesn't contain the same amount 
      //of information expected then the model can *cheat* and use the auditory 
      //loop to produce a response (if the stimulus-response pair is in the 
      //auditory loop).  If this conditional is not passed then either the model 
      //can not cheat because the stimulus-response presented is not present in
      //the auditory loop or the amount of information in the tentative response 
      //is equal to the amount of information expected in the response (the 
      //model believes that it has a correct response).  In either circumstance, 
      //the model "sticks" with its tentative response rather than cheating.
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
    //"I think the response is...".  Data structures related to viewing 
    //output from the experiment are then updated.
    this._responses.get(this._trialNumber).put(stimulusResponsePair.getFirst(), response);
    this._cheats.get(this._trialNumber).put(stimulusResponsePair.getFirst(), cheated);
    
    //The experimenter reveals to the model what the expected response is. At 
    //this point, set the responses' "finished" property to true so that its
    //correctness can be compared.
    response.setFinished();

    if( response.matches(stimulusResponsePair.getSecond()) ){
      
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
        this.populateAuditoryLoopTableModel();
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
  
  /**
   * Updates various information in the experiment control panel created by
   * {@link #createControlPanel()}.
   */
  private void updateExperimentInformation () {
    this._previousStimulusResponseLabel.setText(this.getPreviousStimulusResponseLabelContents());
    this._nextStimulusResponseLabel.setText(this.getNextStimulusResponseLabelContents());
    _trialNumberLabel.setText("" + (_trialNumber + 1) );
    _experimentTimeLabel.setText ("" + _exptClock);
  }
}

