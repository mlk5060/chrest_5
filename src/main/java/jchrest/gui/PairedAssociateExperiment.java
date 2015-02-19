// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import jchrest.architecture.Chrest;
import jchrest.lib.ListPattern;
import jchrest.lib.PairedPattern;
import jchrest.lib.Pattern;
import jchrest.lib.StringPattern;

/**
 * This panel provides an interface for running paired associate experiments.
 * 
 * @author Peter C. R. Lane
 * @author Martyn Lloyd-Kelly
 */
public class PairedAssociateExperiment extends JPanel {
  
  private final Component _window;
  private final Chrest _model;
  
  //Determinants of experiment behaviour.
  private int _exptClock;
  private int _patternNumber;
  private int _trialNumber;
  private final List<PairedPattern> _patterns;
  private List<List<ListPattern>> _responses;
  private List<HashMap<ListPattern, Integer>> _numberPatternErrors;
//  private List<Integer> _patternsPresented;
  
  //Experiment information and set-up variables.
  private JLabel _patternLabel;
  private JLabel _trialNumberLabel;
  private JLabel _experimentTimeLabel;
  //private JComboBox _learningStrategy;
  private DefaultTableModel _stimulusResponseTableModel;
  private JSpinner _presentationTime;
  private JSpinner _interItemTime;
  private JSpinner _interTrialTime;
  private JCheckBox _randomOrder;
  
  //Experiment output variables.
  private JTable _trialsTable;
  private final int _numberDefaultColsTrialsTable = 2; //Should always be "Stimulus" and "Target" columns.
  private JScrollPane _trialsScrollPane;
  private JScrollBar _trialsHorizontalBar;
  private JScrollPane _errorsScrollPane;
  private JTable _errorsTable;
  private final int _numberDefaultColsErrorsTable = 1; //Should always be "Stimulus" column.
  private JScrollBar _errorsHorizontalBar;

  public PairedAssociateExperiment (Chrest model, List<PairedPattern> patterns) {
    super ();
    
    _model = model;
    _patterns = patterns;
    _patternNumber = 0;
    _trialNumber = 1;
//    _patternsPresented = new ArrayList<>();
    
    _model.resetLearningClock();
    instantiateErrorStorage();

    setLayout (new GridLayout (1, 1));
    JSplitPane jsp = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, renderInputView (), renderOutputView ());
    jsp.setOneTouchExpandable (true);
    _window = add (jsp);
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
    
    experimentInput.add(renderStimulusResponsePairsView());
    experimentInput.add(renderExperimentInformationView());
    experimentInput.add(renderExperimentControlsView());

    return experimentInput;
  }
  
  /**
   * Renders a panel composed of two panels that display the responses/ 
   * errors given/made by the CHREST model for each pattern over each trial.
   * Composed of the trials view {@link #renderTrailsView()} and errors view
   * {@link #renderErrorsView()}.
   * 
   * @return 
   */
  private JPanel renderOutputView () {
    JPanel experimentOutput = new JPanel ();
    experimentOutput.setBorder (new TitledBorder ("Experiment Output"));
    experimentOutput.setLayout (new GridLayout (2, 1));
    
    experimentOutput.add(renderTrailsView());
    experimentOutput.add(renderErrorsView());
    
    return experimentOutput;
  }
  
  
  /**
   * Creates a panel containing all stimulus-response pattern pairs to be used 
   * in the experiment (pairs created by {@link #makePairs(java.util.List)} 
   * method.
   * 
   * @return 
   */
  private JScrollPane renderStimulusResponsePairsView () {
    
    this._stimulusResponseTableModel = new DefaultTableModel(){
      @Override
      public boolean isCellEditable(int row, int column) {
        //Only the third column is editable
        return column == 2;
      }
    };
    _stimulusResponseTableModel.addColumn("Stimulus");
    _stimulusResponseTableModel.addColumn("Response");
    _stimulusResponseTableModel.addColumn("Order");
    
    for (int i = 0; i < _patterns.size(); i++) {
      PairedPattern pair = _patterns.get(i);
      Object[] rowData = {
        pair.getFirst().toString (),
        pair.getSecond().toString (),
        String.valueOf(i + 1)
      };
      _stimulusResponseTableModel.addRow(rowData);
    }
    JTable table = new JTable(_stimulusResponseTableModel);
    table.setBackground(Color.LIGHT_GRAY);
    table.setShowHorizontalLines(false);
    table.setShowVerticalLines(false);
    table.setFillsViewportHeight(true);
    
    JScrollPane scrollPane = new JScrollPane(table);
    scrollPane.setBorder (new TitledBorder ("Stimulus-Response Pairs"));
    return scrollPane;        
  }
  
  /**
   * Renders a panel containing useful experiment information that is updated
   * periodically.
   * 
   * @return 
   */
  private JPanel renderExperimentInformationView(){
    
    //Define experiment information elements
    _patternLabel = new JLabel ("" + (_patternNumber + 1), SwingConstants.RIGHT);
    
    _trialNumberLabel = new JLabel (Integer.toString(_trialNumber), SwingConstants.RIGHT);
    
    _experimentTimeLabel = new JLabel ("0", SwingConstants.RIGHT);
    
    //Create experiment interface element.
    JPanel experimentInformation = new JPanel();
    experimentInformation.setBorder(new TitledBorder("Experiment Information"));
    experimentInformation.setLayout(new GridLayout(3, 2, 2, 2));
    
    experimentInformation.add(new JLabel ("Stimulus-response to learn next", SwingConstants.RIGHT));
    experimentInformation.add(_patternLabel);
    
    experimentInformation.add (new JLabel ("Trial #", SwingConstants.RIGHT));
    experimentInformation.add(_trialNumberLabel);
    
    experimentInformation.add (new JLabel ("Experiment time (ms)", SwingConstants.RIGHT));
    experimentInformation.add(_experimentTimeLabel);
  
    return experimentInformation;
  }
  
  /**
   * Renders a panel containing all independent variables that can be set in the
   * experiment.
   * 
   * @return 
   */
  private JPanel renderExperimentControlsView() {
    
    _presentationTime = new JSpinner (new SpinnerNumberModel (2000, 1, 50000, 1));
    _presentationTime.setToolTipText("The length of time each stimuli is presented for in a trial");
    
    _interItemTime = new JSpinner (new SpinnerNumberModel (2000, 1, 50000, 1));
    _interItemTime.setToolTipText("The length of time between presentation of each stimuli on each trial");
    
    _interTrialTime = new JSpinner(new SpinnerNumberModel(2000, 1, 50000, 1));
    _interTrialTime.setToolTipText("The length of time between trials");
    
//    _learningStrategy = new JComboBox(new String[] {
//      "First to last",
//      "Outer to inner"
//    });
//    ((JLabel)_learningStrategy.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
//    _learningStrategy.setToolTipText("The learning strategy that should be used by CHREST");
    
    _randomOrder = new JCheckBox ();
    _randomOrder.setToolTipText ("Check to present stimuli in a random order.  Shuffling of patterns only occurs at the start of a new trial.");
    
    JButton restart = new JButton (new RestartAction() );
    restart.setToolTipText ("Reset the experiment and clear the model");
    
    JButton learnPattern = new JButton (new LearnPatternAction() );
    learnPattern.setToolTipText ("Asks the model to learn a pair of stimulus-response patterns.  If this is the last pair, the model will generate output.");
    
    JButton runTrial = new JButton (new RunTrialAction() );
    runTrial.setToolTipText("Presents all remaining pattern pairs in a trial to the model and produces output.");
    
    JButton exportData = new JButton(new ExportDataAction());
    exportData.setToolTipText ("Export current experiment data as a CSV file to a specified location");
    
    //Set layout of the controls and add elements.
    JPanel controls = new JPanel ();
    controls.setBorder (new TitledBorder ("Experiment Controls"));
//    controls.setLayout (new GridLayout (7, 2, 2, 2));
    controls.setLayout (new GridLayout (6, 2, 2, 2));
    
    controls.add (new JLabel ("Presentation time (ms)", SwingConstants.RIGHT));
    controls.add (_presentationTime);
    
    controls.add (new JLabel ("Inter item time (ms)", SwingConstants.RIGHT));
    controls.add (_interItemTime);
    
    controls.add (new JLabel ("Inter trial time (ms)", SwingConstants.RIGHT));
    controls.add (_interTrialTime);
    
//    controls.add (new JLabel ("Learning strategy", SwingConstants.RIGHT));
//    controls.add (_learningStrategy);
    
    controls.add (new JLabel ("Randomise presentation", SwingConstants.RIGHT));
    controls.add (_randomOrder);
    
    controls.add (restart);    controls.add (learnPattern);
    controls.add (exportData); controls.add (runTrial);

    return controls;
  }
  
  /**
   * Renders a panel containing a table (created by {@link #createTrialsTable()}
   * ) that tracks the responses given by the CHREST model for each 
   * stimulus-response pair when a trial has been completed.
   * 
   * @return 
   */
  private JPanel renderTrailsView(){
    
    _responses = new ArrayList<List<ListPattern>> ();
    
    createTrialsTable ();
    _trialsScrollPane = new JScrollPane (_trialsTable);
    _trialsHorizontalBar = _trialsScrollPane.getHorizontalScrollBar ();
    
    JPanel trialsView = new JPanel();
    trialsView.setBorder (new TitledBorder ("Trial Results"));
    trialsView.setLayout (new GridLayout ());
    trialsView.add(_trialsScrollPane);
    
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
   * Creates the trials table that tracks the responses given by the CHREST 
   * model for each stimulus presented over the course of multiple trials.
   */
  private void createTrialsTable () {
    
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        return _patterns.size ();
      }
      
      @Override
      public int getColumnCount () {
        return _numberDefaultColsTrialsTable + _responses.size (); 
      }
      
      @Override
      public Object getValueAt (int row, int column) {      
        if (column == 0) {
          return _patterns.get(row).getFirst ();
        } else if (column == 1) {
          return _patterns.get(row).getSecond ();
        }
        else {
          return _responses.get(column-2).get(row).toString ();
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
        
        //Scrolls the horizontal bar for the trials scroll pane to its maximum
        //value (i.e. to the extreme right) to display the latest results.  The
        //wait is required since the UI will update before a new maximum 
        //horizontal bar value can be set.
        EventQueue.invokeLater (() -> {
          _trialsHorizontalBar.setValue (_trialsHorizontalBar.getMaximum ());
        });
      }
    };
    
    _trialsTable = new JTable (tm);
    _trialsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
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
        
        //Should only have rows for the patterns declared.
        return _patterns.size();
      }

      @Override
      public int getColumnCount() {
        return _numberDefaultColsErrorsTable + _responses.size();
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
        if(columnIndex == 0){
          return _patterns.get(rowIndex).getFirst();
        }
        else {
          return _numberPatternErrors.get(columnIndex).get(_patterns.get(rowIndex).getFirst());
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
        
        //Scrolls the horizontal bar for the errors scroll pane to its maximum
        //value (i.e. to the extreme right) to display the latest results.  The
        //wait is required since the UI will update before a new maximum 
        //horizontal bar value can be set.
        EventQueue.invokeLater (() -> {
          _errorsHorizontalBar.setValue (_errorsHorizontalBar.getMaximum ());
        });
      }
    };
    
    _errorsTable = new JTable (tm);
    _errorsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
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
      String lastExperimentLocatedInName = _model.getExperimentsLocatedInNames().get(_model.getExperimentsLocatedInNames().size() - 1);
      _model.clear ();
      _responses.clear ();
      _exptClock = 0;
      _patternNumber = 0;
//      _patternsPresented.clear();
      ((AbstractTableModel)_trialsTable.getModel()).fireTableStructureChanged();
      ((AbstractTableModel)_errorsTable.getModel()).fireTableStructureChanged();
      _trialNumber = 1;
      _model.setNotEngagedInExperiment();
      _model.addExperimentsLocatedInName(lastExperimentLocatedInName);
      
      instantiateErrorStorage();
      updateExperimentInformation ();
    }
  }

  class LearnPatternAction extends AbstractAction implements ActionListener {
    
    LearnPatternAction () {
      super ("Learn Pattern");
    }
    
    @Override
    public void actionPerformed (ActionEvent e) {
      _model.setEngagedInExperiment();
      _model.freeze (); // save all gui updates to the end
      shufflePatterns();
      if(correctOrder()){
        processPattern();
        checkEndTrial();
        updateExperimentInformation();
      }
      _model.unfreeze();
    }  
  }
  
  class RunTrialAction extends AbstractAction implements ActionListener {
    
    RunTrialAction(){
      super ("Run Trial");
    }
      
    @Override
    public void actionPerformed(ActionEvent e){
      _model.setEngagedInExperiment();
      _model.freeze();
      shufflePatterns();
      if(correctOrder()){
        while(_patternNumber < _patterns.size()){
          processPattern();
        }
        checkEndTrial();
        updateExperimentInformation();
      }
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
      
      trialDataToSave.add(ExportData.extractJTableDataAsCsv(_trialsTable));
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
    if(_patternNumber == _patterns.size()){
      test();
      ((AbstractTableModel)_trialsTable.getModel()).fireTableStructureChanged();
      ((AbstractTableModel)_errorsTable.getModel()).fireTableStructureChanged();
      _patternNumber = 0;
//      _patternsPresented.clear();
      _trialNumber += 1;
      _exptClock += ((SpinnerNumberModel)_interTrialTime.getModel()).getNumber().intValue ();
    }
  }
  
  private boolean correctOrder(){
    System.out.println("Checking order specification...");
    List<String> orderNumbers = new ArrayList<>();
    for(int i = 0; i < _stimulusResponseTableModel.getRowCount(); i++){
      
      //Check that order number is:
      String orderContents = (String)this._stimulusResponseTableModel.getValueAt(i, 2);
      if(
        orderContents.matches("[1-9][0-9]{0,8}") && //a whole number greater than 0
        Integer.valueOf(orderContents) <= _patterns.size() && //less than or equal to the total number of patterns
        !orderNumbers.contains(orderContents)
      ){
        orderNumbers.add(orderContents);
      }
      else{
        JOptionPane.showMessageDialog(_window,
        "<html>The ordering specified for stimulus-response presentation is incorrect for one"
          + "of the following reasons:"
          + "<ul>"
          + "<li>A non-integer has been specified.</li>"
          + "<li>Zero (0) has been specified.</li>"
          + "<li>An integer greater than the number of patterns (" + _patterns.size() + ") has been specified.</li>"
          + "<li>The same integer has been entered twice.</li>"
          + "</ul>"
          + "Please rectify before continuing.</html>",
        "Stimulus-Pair Order Specification Error",
        JOptionPane.ERROR_MESSAGE);
        return false;
      }
    }
    
    System.out.println("Everything is fine, returning true!");
    return true;
  }
  
  public PairedPattern findPair(){
    PairedPattern pairToProcess = null;
    //Find the pattern
    for(int i = 0; i < this._stimulusResponseTableModel.getRowCount(); i++){
      String orderNumber = (String)this._stimulusResponseTableModel.getValueAt(i, 2);
      
      if( Integer.valueOf(orderNumber) == (_patternNumber + 1) ){
        return pairToProcess = _patterns.get(i);
      }
    }
    
    return pairToProcess;
  }
  
  /**
   * Instantiates both the "_numberPatternErrors" and "_trialErrors" instance
   * variables with values of 0.
   */
  public final void instantiateErrorStorage(){
    
    this._numberPatternErrors = new ArrayList<HashMap<ListPattern, Integer>>();
    this._numberPatternErrors.add( new HashMap<ListPattern, Integer>() );
    for(int i = 0; i < _patterns.size(); i++){
      this._numberPatternErrors.get(0).put(_patterns.get(i).getFirst(), 0);
    }
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
   * Attempts to associate the second pattern with the first pattern of a 
   * specified pattern if they are both learned or attempts to learn and 
   * associate them every millisecond until the presentation time specified is 
   * reached.
   * 
   */
  private void processPattern(){
    
    int presentationFinishTime = ((SpinnerNumberModel)_presentationTime.getModel()).getNumber().intValue() + _exptClock;
    
    PairedPattern pair = findPair();
    System.out.println(pair.getFirst().toString() + pair.getSecond().toString());
    
    while(_exptClock < presentationFinishTime){
      this._model.associateAndLearn(pair.getFirst(), pair.getSecond(), _exptClock);
      _exptClock += 1;
    }
    _patternNumber++;

    _exptClock += ((SpinnerNumberModel)_interItemTime.getModel()).getNumber().intValue ();
   }
  
  /**
   * Shuffle the patterns if this is the first pattern to be run in a new
   * trial and patterns are to be presented randomly.
   */
  private void shufflePatterns(){
    if(_patternNumber == 0 && _randomOrder.isSelected ()){
      Collections.shuffle(_patterns, new Random(System.nanoTime()));
    }
  }
  
  /**
   * Asks the CHREST model to retrieve the pattern that is currently 
   * associated with each stimuli pattern in LTM and records any errors.
   */
  private void test () {

    List<ListPattern> responses = new ArrayList<ListPattern> ();
    _numberPatternErrors.add( new HashMap<ListPattern, Integer>() );

    for (PairedPattern pair : _patterns) {
      ListPattern response = _model.associatedPattern (pair.getFirst (), _exptClock);

      if (response != null) {
        responses.add (response);
      } else {
        responses.add (Pattern.makeVisualList (new String[]{"NONE"}));
      }

      ListPattern latestResponse = responses.get(responses.size() - 1);
      latestResponse.setFinished();

      //If the response matches the second primitive of the pair, set the
      //error in this trial for the pattern to 0, otherwise, set to 1.
      if( latestResponse.matches(pair.getSecond()) ){
         _numberPatternErrors.get(_trialNumber).put( pair.getFirst(), 0 );
      }
      else{
        _numberPatternErrors.get(_trialNumber).put( pair.getFirst(), 1 );
      }
    }
    _responses.add (responses);
  }
  
  /**
   * Updates various information in the experiment control panel created by
   * {@link #createControlPanel()}.
   */
  private void updateExperimentInformation () {
    _patternLabel.setText("" + (_patternNumber + 1));
    _trialNumberLabel.setText("" + _trialNumber);
    _experimentTimeLabel.setText ("" + _exptClock);
  }
}

