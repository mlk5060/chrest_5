// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.*;
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
  
  private final Component _window;
  private final Chrest _model;
  
  //Determinants of experiment behaviour.
  private int _exptClock;
  private int _patternNumber;
  private int _trialNumber;
  private final List<PairedPattern> _patterns;
  private List<List<ListPattern>> _responses;
  private List<HashMap<ListPattern, Integer>> _numberPatternErrors;
  
  //Experiment information and set-up variables.
  private JLabel _patternLabel;
  private JLabel _trialNumberLabel;
  private JLabel _experimentTimeLabel;
  private JComboBox _learningStrategy;
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
  private JPanel renderStimulusResponsePairsView () {
    JPanel panel = new JPanel ();
    panel.setBorder (new TitledBorder ("Stimulus-Response Pairs"));
    panel.setLayout (new GridLayout (1, 1));

    JPanel pairsPanel = new JPanel ();
    pairsPanel.setLayout (new GridLayout (_patterns.size(), 2));
    for (PairedPattern pair : _patterns) {
      pairsPanel.add (new JLabel (pair.getFirst().toString ()));
      pairsPanel.add (new JLabel (pair.getSecond().toString ()));
    }

    panel.add (new JScrollPane (pairsPanel));
    return panel;        
  }
  
  /**
   * Renders a panel containing useful experiment information that is updated
   * periodically.
   * 
   * @return 
   */
  private JPanel renderExperimentInformationView(){
    
    //Define experiment information elements
     PairedPattern pair = _patterns.get(_patternNumber);
    _patternLabel = new JLabel (pair.getFirst().toString() + " " + pair.getSecond().toString(), SwingConstants.RIGHT);
    
    _trialNumberLabel = new JLabel (Integer.toString(_trialNumber), SwingConstants.RIGHT);
    
    _experimentTimeLabel = new JLabel ("0", SwingConstants.RIGHT);
    
    //Create experiment interface element.
    JPanel experimentInformation = new JPanel();
    experimentInformation.setBorder(new TitledBorder("Experiment Information"));
    experimentInformation.setLayout(new GridLayout(3, 2, 2, 2));
    
    experimentInformation.add(new JLabel ("Pattern pair to learn next", SwingConstants.RIGHT));
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
    
    _learningStrategy = new JComboBox(new String[] {"Minimal stimulus - whole response"});
    ((JLabel)_learningStrategy.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
    _learningStrategy.setToolTipText("The learning strategy that should be used by CHREST");
    
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
    controls.setLayout (new GridLayout (7, 2, 2, 2));
    
    controls.add (new JLabel ("Presentation time (ms)", SwingConstants.RIGHT));
    controls.add (_presentationTime);
    
    controls.add (new JLabel ("Inter item time (ms)", SwingConstants.RIGHT));
    controls.add (_interItemTime);
    
    controls.add (new JLabel ("Inter trial time (ms)", SwingConstants.RIGHT));
    controls.add (_interTrialTime);
    
    controls.add (new JLabel ("Learning strategy", SwingConstants.RIGHT));
    controls.add (_learningStrategy);
    
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
          EventQueue.invokeLater (() -> {
            _errorsHorizontalBar.setValue (_errorsHorizontalBar.getMaximum ());
          });
        });;
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
      _model.clear ();
      _responses.clear ();
      _exptClock = 0;
      _patternNumber = 0;
      ((AbstractTableModel)_trialsTable.getModel()).fireTableStructureChanged();
      ((AbstractTableModel)_errorsTable.getModel()).fireTableStructureChanged();
      _trialNumber = 1;
      
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
      _model.freeze (); // save all gui updates to the end
      shufflePatterns();
      processPattern();
      checkEndTrial();
      updateExperimentInformation();
      _model.unfreeze();
    }  
  }
  
  class RunTrialAction extends AbstractAction implements ActionListener {
    
    RunTrialAction(){
      super ("Run Trial");
    }
      
    @Override
    public void actionPerformed(ActionEvent e){
      _model.freeze();
      shufflePatterns();
      while(_patternNumber < _patterns.size()){
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
      
      //Store trail and error data for the experiment in CSV formatted strings.
      String trialsData = extractTableDataInCsvFormat(_trialsTable);
      String errorsData = extractTableDataInCsvFormat(_errorsTable);
      
      //Create a "Save File" dialog that can only list directories since the 
      //user should select an existing directory to save data in not a file.
      final JFileChooser fc = new JFileChooser();
      fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      int resultOfFileSelect = fc.showSaveDialog(_window);
      
      //If the user selected a directory, store this directory as an object
      //and extract its absolute path as a string.  Otherwise, do nothing.
      if (resultOfFileSelect == JFileChooser.APPROVE_OPTION) {
        File directoryToCreateResultDirectoryIn = fc.getSelectedFile();
        String directoryToCreateResultDirectoryInAbsPath = directoryToCreateResultDirectoryIn.getAbsolutePath();
        
        //Check that the directory specified by the user can be written and
        //executed, if not, display an error informing the user that the 
        //directory specified has incorrect permissions set that prevent this
        //function from executing further.
        if(directoryToCreateResultDirectoryIn.canWrite() && directoryToCreateResultDirectoryIn.canExecute()){  
          
          //Create a new file object for the directory that is to be created 
          //inside the directory that the user has specified.  Creation of a 
          //file to store trial and error data prevents this data from becoming
          //disjoint in the file system (inconvenient for the user).
          File directoryToStoreData = new File(directoryToCreateResultDirectoryInAbsPath + File.separator + "paired-associate-experiment-data");
          
          //Check that the default directory name doesn't already exist.  If it
          //does, append a number to the end of the directory name and check
          //again.  If the directory name doesn't exist, create the directory
          //and set permissions to allow trial and error data to be written to 
          //files within it.
          int i = 0;
          while(directoryToStoreData.exists()){
            i++;
            directoryToStoreData = new File(directoryToCreateResultDirectoryInAbsPath + File.separator + "paired-associate-experiment-data-" + i);
          }
          directoryToStoreData.mkdir();
          directoryToStoreData.setExecutable(true, true);
          directoryToStoreData.setWritable(true, true);
        
          //Extract absolute path to the directory that will contain the data 
          //files and create the path names for the data files.
          String directoryToStoreDataAbsPath = directoryToStoreData.getAbsolutePath();
          String trialFileAbsPath = directoryToStoreDataAbsPath + File.separator + "trialData.csv";
          String errorFileAbsPath = directoryToStoreDataAbsPath + File.separator + "errorData.csv";
          
          //Create the data files and set read, write and execute permissions
          //so that data can be written to them.
          File trialFile = new File(trialFileAbsPath);
          File errorFile = new File(errorFileAbsPath);
          try {
            trialFile.createNewFile();
            errorFile.createNewFile();
          } catch (IOException ex) {
            Logger.getLogger(PairedAssociateExperiment.class.getName()).log(Level.SEVERE, null, ex);
          }
          
          trialFile.setWritable(true, true);
          errorFile.setWritable(true, true);
          
          trialFile.setReadable(true, true);
          errorFile.setReadable(true, true);
          
          trialFile.setExecutable(true, true);
          errorFile.setExecutable(true, true);
          
          //Write data to data files.
          try (PrintWriter trialFilePrintWriter = new PrintWriter(trialFile)) {
            trialFilePrintWriter.write(trialsData);
          } catch (FileNotFoundException ex) {
            Logger.getLogger(PairedAssociateExperiment.class.getName()).log(Level.SEVERE, null, ex);
          }
          
          try (PrintWriter errorFilePrintWriter = new PrintWriter(errorFile)) {
            errorFilePrintWriter.write(errorsData);
          } catch (FileNotFoundException ex) {
            Logger.getLogger(PairedAssociateExperiment.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
        else{
          JOptionPane.showMessageDialog(null, "Directory '" + directoryToCreateResultDirectoryInAbsPath + "' does not have write and/or execute privileges", "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
    
    /**
     * Converts table data into CSV format by first extracting column headers
     * then data on a per-row basis.
     * 
     * @param table
     * @return 
     */
    public String extractTableDataInCsvFormat(JTable table){
      AbstractTableModel atm = (AbstractTableModel) table.getModel();
      int nRow = atm.getRowCount();
      int nCol = atm.getColumnCount();
      String tableData = "";
      
      for(int col = 0; col < nCol; col++){
        tableData += "," + atm.getColumnName(col);
      }
      tableData += "\n";
      
      for(int row = 0; row< nRow; row++){
        for(int col = 0; col < nCol; col++){
          tableData += "," + atm.getValueAt(row, col);
        }
        tableData += "\n";
      }
      
      return tableData.replaceAll("^,", "").replaceAll("\n,","\n");
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
      _trialNumber += 1;
      _exptClock += ((SpinnerNumberModel)_interTrialTime.getModel()).getNumber().intValue ();
    }
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
    PairedPattern pair = _patterns.get(_patternNumber);
    int presentationFinishTime = ((SpinnerNumberModel)_presentationTime.getModel()).getNumber().intValue() + _exptClock;

    while(_exptClock < presentationFinishTime){
      
      switch(_learningStrategy.getSelectedIndex()){

        //"Minimal stimulus-whole response" strategy: model learns all stimuli
        //first using as little information as possible before learning 
        //responses to each stimulus.
        case 0:
          _model.associateAndLearn(pair.getFirst(), pair.getSecond(), _exptClock);
          break;
      }
  
      _exptClock += 1;
    }
    _patternNumber += 1;
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
      ListPattern response = _model.associatedPattern (pair.getFirst ());

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
    PairedPattern nextPair = _patterns.get(_patternNumber);
    _patternLabel.setText(nextPair.getFirst().toString() + " " + nextPair.getSecond().toString());
    _trialNumberLabel.setText("" + _trialNumber);
    _experimentTimeLabel.setText ("" + _exptClock);
  }
}

