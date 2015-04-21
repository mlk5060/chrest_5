// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;
import jchrest.architecture.Chrest;
import jchrest.lib.PairedAssociateExperiment;
import jchrest.lib.PairedPattern;

/**
 * This panel provides an interface for running paired associate experiments.
 * 
 * @author Peter C. R. Lane
 * @author Martyn Lloyd-Kelly
 */
public class PairedAssociateInterface extends JPanel implements Observer {
  
  private final PairedAssociateExperiment _experiment;
  
  //The window containing the experiment interface.  Used as an anchor point for 
  //error and save dialogs.
  private final Component _window;
  
  /****************************************************************************/
  /****************************************************************************/
  /******************************* GUI VARIABLES ******************************/
  /****************************************************************************/
  /****************************************************************************/
  
  //Variables in this section are organised according to their view "sections" 
  //in the GUI from top-to-bottom then left-to-right.
  
  //////////////////////////////////////////////////////////////////////////////
  /////////////////////// Stimulus-response-priority view //////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  //Contains the stimulus-response-priority table.
  private JScrollPane _stimulusResponsePriorityView;
  
  //The data model that underpins the stimulus-response-priority table.  Changes
  //made here should be reflected in the stimulus-response-priority table.  This
  //model is itself underpinned by the current stimulus-response-priority data
  //structure in the paired associate experiment being interfaced with so 
  //changes made to that structure should be reflected in this model.
  private DefaultTableModel _stimulusResponsePriorityTableModel;
  
  //////////////////////////////////////////////////////////////////////////////
  ///////////////////////////// Auditory loop view /////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  //Contains the auditory loop table.
  private JScrollPane _auditoryLoopView;
  
  //The data model that underpins the auditory loop table.  Changes made here 
  //should be reflected in the auditory loop table.  This model is itself 
  //underpinned by the auditory loop data structure in the paired associate 
  //experiment being interfaced with so changes made to that structure should be 
  //reflected in this model.
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
  
  /****************************************************************************/
  /****************************************************************************/
  /****************************************************************************/
  /****************************************************************************/
  /****************************************************************************/

  /**
   * Constructor for the interface.
   * 
   * @param model The CHREST model to be used in the experiment.
   * @param patterns The stimulus-response pairs that are to be used in the 
   * experiment.
   */
  public PairedAssociateInterface (Chrest model, List<PairedPattern> patterns) {
    super ();
    this._experiment = new PairedAssociateExperiment(model, patterns);
    
    //Render the GUI.
    setLayout (new GridLayout (1, 1));
    JSplitPane jsp = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, this.renderInputView (), this.renderOutputView ());
    jsp.setOneTouchExpandable (true);
    this._window = add (jsp);
    this._experiment.addObserver(this);
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
        return column == 2 && _experiment.getStimulusResponseNumber() == 0;
      }
    };
    
    this._stimulusResponsePriorityTableModel.addColumn("Stimulus");
    this._stimulusResponsePriorityTableModel.addColumn("Response");
    this._stimulusResponsePriorityTableModel.addColumn("Priority");
    
    this.populateStimulusResponsePriorityTableModel();
    
    JTable stimulusResponsePriorityJTable = new JTable(this._stimulusResponsePriorityTableModel);
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
        //have the same priority.  The String casts here are required since data 
        //from a JTable cell are String objects and can't be cast directly to 
        //Integer objects.
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
        for(int i = 0; i < PairedAssociateInterface.this._stimulusResponsePriorityTableModel.getRowCount(); i++){
          if(i != tcl.getRow()){
            Integer priorityToCheck = Integer.valueOf((String)PairedAssociateInterface.this._stimulusResponsePriorityTableModel.getValueAt(i, 2));
            if(Objects.equals(priorityToCheck, newPriorityInteger)){
              _experiment.getCurrentStimulusResponsePairsAndPriorities().put( _experiment.getStimulusResponsePairsArrayFromKeysInMap(_experiment.getCurrentStimulusResponsePairsAndPriorities()).get(i), oldPriorityInteger );
              break;
            }
          }
        }
        
        //Now, set the new priority for P in the data model that feeds the table
        //model.
        _experiment.getCurrentStimulusResponsePairsAndPriorities().put( _experiment.getStimulusResponsePairsArrayFromKeysInMap(_experiment.getCurrentStimulusResponsePairsAndPriorities()).get(tcl.getRow()), newPriorityInteger );
        
        //Finally, update the table model so that the table in the GUI displays
        //the updated priorities.
        PairedAssociateInterface.this.populateStimulusResponsePriorityTableModel();
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
    this._trialNumberLabel = new JLabel (Integer.toString(_experiment.getTrialNumber() + 1), SwingConstants.RIGHT);
    this._experimentTimeLabel = new JLabel (String.valueOf(_experiment.getExptClock()), SwingConstants.RIGHT);
    
    //Create experiment interface element.
    this._experimentInformationView = new JPanel();
    this._experimentInformationView.setBorder(new TitledBorder("Experiment Information"));
    this._experimentInformationView.setLayout(new GridLayout(4, 2, 2, 2));
    
    this._experimentInformationView.add(new JLabel ("Prev. stimulus-response", SwingConstants.RIGHT));
    this._experimentInformationView.add(this._previousStimulusResponseLabel);
    
    this._experimentInformationView.add(new JLabel ("Next stimulus-response", SwingConstants.RIGHT));
    this._experimentInformationView.add(this._nextStimulusResponseLabel);
    
    this._experimentInformationView.add (new JLabel ("Trial #", SwingConstants.RIGHT));
    this._experimentInformationView.add(this._trialNumberLabel);
    
    this._experimentInformationView.add (new JLabel ("Experiment time (ms)", SwingConstants.RIGHT));
    this._experimentInformationView.add(this._experimentTimeLabel);
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
    
    JTable auditoryLoopTable = new JTable(this._auditoryLoopTableModel);
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
    _experiment.setAuditoryLoopMaxSize( (int)this._auditoryLoopMaxSize.getModel().getValue() );
    this._auditoryLoopMaxSize.addChangeListener((ChangeEvent e) -> {
      _experiment.setAuditoryLoopMaxSize( (int)this._auditoryLoopMaxSize.getModel().getValue() );
    });
    
    this._presentationTime = new JSpinner (new SpinnerNumberModel (2000, 0, Integer.MAX_VALUE, 1));
    this._presentationTime.setToolTipText("The length of time each stimuli is presented for in a trial");
    _experiment.setPresentationTime( (int)this._presentationTime.getModel().getValue() );
    this._presentationTime.addChangeListener((ChangeEvent e) -> {
      _experiment.setPresentationTime( (int)this._presentationTime.getModel().getValue() );
    });
    
    this._interItemTime = new JSpinner (new SpinnerNumberModel (2000, 0, Integer.MAX_VALUE, 1));
    this._interItemTime.setToolTipText("The length of time between presentation of each stimuli on each trial");
    _experiment.setInterItemTime( (int)this._interItemTime.getModel().getValue() );
    this._interItemTime.addChangeListener((ChangeEvent e) -> {
      _experiment.setInterItemTime( (int)this._interItemTime.getModel().getValue() );
    });
    
    this._interTrialTime = new JSpinner(new SpinnerNumberModel(2000, 0, Integer.MAX_VALUE, 1));
    this. _interTrialTime.setToolTipText("The length of time between trials");
    _experiment.setInterTrialTime( (int)this._interTrialTime.getModel().getValue() );
    this._interTrialTime.addChangeListener((ChangeEvent e) -> {
      _experiment.setInterTrialTime( (int)this._interTrialTime.getModel().getValue() );
    });
    
    this._randomOrder = new JCheckBox ();
    this._randomOrder.setToolTipText ("Check to present stimuli in a random order.  Shuffling of patterns only occurs at the start of a new trial.");
    this._randomOrder.addItemListener((ItemEvent e) -> {
      
      if (e.getStateChange() == ItemEvent.DESELECTED){
        _experiment.unshuffleStimulusResponsePairs();
      }
      else if(e.getStateChange() == ItemEvent.SELECTED){
        _experiment.shuffleStimulusResponsePairs();
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
    this._experimentControlsView.add(this._auditoryLoopMaxSize);
    
    //Row 2
    this._experimentControlsView.add (new JLabel ("Presentation time (ms)", SwingConstants.RIGHT));
    this._experimentControlsView.add (this._presentationTime);
    
    //Row 3
    this._experimentControlsView.add (new JLabel ("Inter item time (ms)", SwingConstants.RIGHT));
    this._experimentControlsView.add (this._interItemTime);
    
    //Row 4
    this._experimentControlsView.add (new JLabel ("Inter trial time (ms)", SwingConstants.RIGHT));
    this._experimentControlsView.add (this._interTrialTime);
    
    //Row 5
    this._experimentControlsView.add (new JLabel ("Randomise presentation", SwingConstants.RIGHT));
    this._experimentControlsView.add (this._randomOrder);
    
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
    this._responsesScrollPane = new JScrollPane (this._responsesTable);
    this._responsesHorizontalBar = this._responsesScrollPane.getHorizontalScrollBar();
    
    JPanel trialsView = new JPanel();
    trialsView.setBorder (new TitledBorder ("Responses"));
    trialsView.setLayout (new GridLayout ());
    trialsView.add(this._responsesScrollPane);
    
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
    this._errorsScrollPane = new JScrollPane (this._errorsTable);
    this._errorsHorizontalBar = this._errorsScrollPane.getHorizontalScrollBar ();
    
    JPanel errorsView = new JPanel();
    errorsView.setBorder (new TitledBorder ("Errors"));
    errorsView.setLayout (new GridLayout ());
    errorsView.add(this._errorsScrollPane);

    return errorsView;
  }
  
  /**
   * Returns the string to be displayed for the "Next Stimulus-Response" 
   * experiment information label.
   * 
   * @return 
   */
  private String getNextStimulusResponseLabelContents(){
    PairedPattern pair = _experiment.getStimulusResponsePairsArrayFromKeysInMap(_experiment.getCurrentStimulusResponsePairsAndPriorities()).get(_experiment.getStimulusResponseNumber());
    return pair.getFirst().toString() + pair.getSecond().toString();
  }
  
  /**
   * Returns the string to be displayed for the "Prev. Stimulus-Response" 
   * experiment information label.
   * 
   * @return 
   */
  private String getPreviousStimulusResponseLabelContents(){
    if(_experiment.getStimulusResponseNumber() == 0){
      return "";
    }
    else{
      PairedPattern pair = _experiment.getStimulusResponsePairsArrayFromKeysInMap(_experiment.getCurrentStimulusResponsePairsAndPriorities()).get(_experiment.getStimulusResponseNumber() - 1);
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
        return _experiment.getCurrentStimulusResponsePairsAndPriorities().size ();
      }
      
      @Override
      public int getColumnCount () {
        return _numberDefaultColsResponsesTable + _experiment.getResponses().size(); 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        if (column == 0) {
          return _experiment.getStimulusResponsePairsArrayFromKeysInMap(_experiment.getOriginalStimulusResponsePairsAndPriorities()).get(row).getFirst();
        } 
        else if (column == 1) {
          return _experiment.getStimulusResponsePairsArrayFromKeysInMap(_experiment.getOriginalStimulusResponsePairsAndPriorities()).get(row).getSecond();
        }
        else {
          String response = _experiment.getResponses().get(column-2).get(_experiment.getStimulusResponsePairsArrayFromKeysInMap(_experiment.getOriginalStimulusResponsePairsAndPriorities()).get(row).getFirst()).toString ();
          if(_experiment.getCheats().get(column-2).get(_experiment.getStimulusResponsePairsArrayFromKeysInMap(_experiment.getOriginalStimulusResponsePairsAndPriorities()).get(row).getFirst())){
            
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
        return _experiment.getCurrentStimulusResponsePairsAndPriorities().size() + 1;
      }

      @Override
      public int getColumnCount() {
        return _numberDefaultColsErrorsTable + _experiment.getResponses().size();
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
            return _experiment.getStimulusResponsePairsArrayFromKeysInMap(_experiment.getOriginalStimulusResponsePairsAndPriorities()).get(rowIndex).getFirst();
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
            String value = String.valueOf(_experiment.getErrors().get(columnIndex - 1).get(_experiment.getStimulusResponsePairsArrayFromKeysInMap(_experiment.getOriginalStimulusResponsePairsAndPriorities()).get(rowIndex).getFirst()));
            if(_experiment.getCheats().get(columnIndex - 1).get(_experiment.getStimulusResponsePairsArrayFromKeysInMap(_experiment.getOriginalStimulusResponsePairsAndPriorities()).get(rowIndex).getFirst())){
              value = "<html><b><font color=\"red\"> " + value + " </font></b></html>";
            }
            return value;
          }
          //For the bottom row, calculate the percentage of correct responses
          //given over the whole trial to 2 decimal places.
          else{
            Collection<Integer> values = _experiment.getErrors().get(columnIndex - 1).values();
            int totalErrors = 0;
            for(Integer value: values){
              totalErrors += value;
            }
            
            double result = 100 - (
              ( ((double)totalErrors) / _experiment.getErrors().get(columnIndex - 1).size() ) 
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

  @Override
  public void update(Observable o, Object arg) {
    this.populateStimulusResponsePriorityTableModel();
    this.populateAuditoryLoopTableModel();
    this._previousStimulusResponseLabel.setText(this.getPreviousStimulusResponseLabelContents());
    this._nextStimulusResponseLabel.setText(this.getNextStimulusResponseLabelContents());
    _trialNumberLabel.setText("" + (_experiment.getTrialNumber() + 1) );
    _experimentTimeLabel.setText ("" + _experiment.getExptClock());
    
    if(arg instanceof Boolean){
      Boolean argBoolean = (Boolean)arg;
      if(argBoolean){
        ((AbstractTableModel)_responsesTable.getModel()).fireTableStructureChanged();
        ((AbstractTableModel)_errorsTable.getModel()).fireTableStructureChanged();
      }
    }
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
    public void actionPerformed (ActionEvent e) {
      _experiment.restart();
      _randomOrder.setSelected(false);
    }
  }

  class LearnPatternAction extends AbstractAction implements ActionListener {
    
    LearnPatternAction () {
      super ("Present Next");
    }
    
    @Override
    public void actionPerformed (ActionEvent e) {
      _experiment.processNextPattern(_randomOrder.isSelected());
      if(_experiment.getStimulusResponseNumber() == 0){
        _randomOrder.setEnabled(true);
      }
    }  
  }
  
  class RunTrialAction extends AbstractAction implements ActionListener {
    
    RunTrialAction(){
      super ("Present Remaining");
    }
      
    @Override
    public void actionPerformed(ActionEvent e){
      _experiment.runTrial(_randomOrder.isSelected());
      _randomOrder.setEnabled(true);
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
   * Populates the auditory loop table model according to the contents of the
   * experiment's auditory loop causing the auditory loop contents table to 
   * update.
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
    for(int i = 0; i < _experiment.getAuditoryLoopMaxSize(); i++){
      String content = "";
      
      if(i < _experiment.getAuditoryLoop().size()){
        PairedPattern stimulusResponse = _experiment.getAuditoryLoop().get(i);
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
    
    //Check that there are stimulus-response-priorites set-up in the experiment.
    if(_experiment.getCurrentStimulusResponsePairsAndPriorities().size() > 0){
      
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
    for(Entry<PairedPattern, Integer> pairedPatternAndPriority : _experiment.getCurrentStimulusResponsePairsAndPriorities().entrySet()){
      PairedPattern pairedPattern = pairedPatternAndPriority.getKey();
      Object[] rowData = {
        pairedPattern.getFirst().toString (),
        pairedPattern.getSecond().toString (),
        pairedPatternAndPriority.getValue().toString()
      };
      this._stimulusResponsePriorityTableModel.addRow(rowData);
    }
  }
}