package jchrest.experimentScripts;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import jchrest.architecture.Chrest;
import jchrest.architecture.Node;
import jchrest.gui.ExportData;
import jchrest.gui.JTableCustomOperations;
import jchrest.gui.Shell;
import jchrest.gui.Shell.LoadDataThread;
import jchrest.gui.TableCellListener;
import jchrest.lib.InputOutput;
import jchrest.lib.ListPattern;
import jchrest.lib.PairedAssociateExperiment;
import jchrest.lib.PairedPattern;
import org.apache.commons.math.stat.regression.SimpleRegression;

/**
 * Fast/slow paired associate scripted experiment class.
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class PairedAssociateFastSlow {
  
  //TODO: Modify input so that any input file can be selected and read-in so 
  //long as its an XML file and valid according to the schema specified.  Make
  //sure the experiment description is updated accordingly too after doing this.
  
  //TODO: Refactor the renderStimulusResponsePrioritiesView method after the 
  //phonological/auditory loop has been made a part of CHREST and there is a
  //contralised location for constructing such views of the 
  //phonological/auditory loop.
  
  /****************************************************************************/
  /****************************************************************************/
  /************************ CLASS AND INSTANCE MEMBERS ************************/
  /****************************************************************************/
  /****************************************************************************/
  
  private final Shell _shell;
  private final Chrest _model;
  private PairedAssociateExperiment _experiment;
  
  /********************************/
  /***** Experiment Constants *****/
  /********************************/
  
  //Condition constants - these are strings since they are displayed in the 
  //experiment conditions table.
  private final static String FAST_PRESENTATION_CONDITION = "Fast";
  private final static String SLOW_PRESENTATION_CONDITION = "Slow";
  private final static String DICTIONARY_CONDITION = "Dictionary";
  private final static String LETTERS_CONDITION = "Letters";
  private final static String NO_PRE_LEARNING_CONDITION = "None";
  
  /*******************************/
  /***** Experiment Controls *****/
  /*******************************/
  
  //The current experiment condition being processed.
  private int _experimentCondition = 0;
  
  //The total number of experiment conditions that should be run.
  private int _experimentConditionsTotal = 0;
  
  //Indicates whether independent variables have been setup correctly after 
  //reading input files.
  private boolean _independentVariablesSetup = false;
  
  //The maximum number of trials that need to be performed (number of trials for
  //"fast" and "slow" presentation cinditions may differ.
  private int _maximumNumberOfTrials;
  
  //Used to determine if input data has been correctly read in, if so, 
  //experiments will be processed.
  private boolean _readInSuccessful = false;
  
  //Used to control whether experiments should continue or stop irrespective of
  //the number of experiment conditions processed vs. the total number of 
  //experiment conditions to process.  Manipulated by the "Start Experiment" and 
  //"Stop" experiment buttons.
  private boolean _runExperiments = true;
  
  /************************/
  /***** GUI Elements *****/
  /************************/
  
  //Main experiment interface panel.
  private JPanel _experimentInterface;
  
  //Tables.
  private JTable _experimentConditionsTable;
  private JTable _humanPercentageCorrectDataTable;
  private JTable _humanCumulativeErrorsDataTable;
  private JTable _modelPercentageCorrectDataTable;
  private JTable _modelCumulativeErrorsDataTable;
  private JTable _percentageCorrectModelFitDataTable;
  private JTable _cumulativeErrorsModelFitDataTable;
    
  //"Fast" presentation speed condition elements.
  private final JSpinner _fastPresentationTime;
  private final JSpinner _fastInterItemTime;
  private final JSpinner _fastInterTrialTime;
  
  //"Slow" presentation speed condition elements.
  private final JSpinner _slowPresentationTime;
  private final JSpinner _slowInterItemTime;
  private final JSpinner _slowInterTrialTime;
  
  //Action buttons
  private JButton _exportDataButton;
  private JButton _restartButton;
  private JButton _stopButton;
  private JButton _runButton;
  
  //Textual information.
  private final JLabel _experimentsProcessedLabel = new JLabel("");
  
  /**************************************/
  /***** Experiment Data Structures *****/
  /**************************************/
  
  //Holds the dictionary used by CHREST when the experiment condition prescribes
  //that the model should undertaking pre-learning using a dictionary.
  private List<ListPattern> _dictionary = new ArrayList<>();
  
  //Holds the letters used by CHREST when the experiment condition prescribes
  //that the model should undertaking pre-learning using some letters.
  private List<ListPattern> _letters = new ArrayList<>();
  
  //Holds stimulus-response pairs as keys and their declared priorities as 
  //integers.  These declarations may be performed via an input file or in the
  //experiment GUI before any 
  private Map<PairedPattern, Integer> _stimRespPairsAndPriorities = new HashMap<>(); 
  
  //Holds ordered stimulus-response pairs, useful when constructing and updating 
  //GUI tables since the 3rd stimulus-response pair can be retrieved, for 
  //example.
  private List<PairedPattern> _stimRespPairs = new ArrayList<>();
  
  //Holds data model for the stimulus-response-priority GUI table.
  private DefaultTableModel _stimulusResponsePriorityTableModel;
  
  //Holds the data for the human percentage correct GUI table.  Keys indicate
  //whether the percentage correct data is for the "fast" or "slow" experiment
  //condition and the value is a list of percentage correct data for each 
  //stimulus-response pair used in the experiment.
  private Map<String, List<Double>> _humanPercentageCorrectData;
  
  //Holds the data for the human cumulative errors GUI table.  Keys indicate
  //whether the cumulative errors data is for the "fast" or "slow" experiment
  //condition and the value is a map whose keys are each of the stimuli used in
  //the experiment and whose values are the cumulative number of errors produced
  //for that stimulus.
  private Map<String, Map<ListPattern, Double>> _humanCumulativeErrorsData;
  
  //Holds the percentage of correct responses produced by the CHREST model 
  //taking part in the experiment for each trial in each experiment condition.
  private List<Double> _modelPercentageCorrectData;
  
  //Holds the cumulative number of errors made by the CHREST model taking part 
  //in the experiment for each stimulus-response pair in each experiment 
  //condition.
  private List<Map<ListPattern, Double>> _modelCumulativeErrorsData;
  
  //Holds the percentage correct R-square and RMSE values (used to determine
  //the model's data fit to the human data specified).
  private List<Double> _percentageCorrectRSquares;
  private List<Double> _percentageCorrectRootMeanSquaredErrors;
  
  //Holds the cumulative errors R-square and RMSE values (used to determine the 
  //model's data fit to the human data specified).
  private List<Double> _cumulativeErrorsRSquares;
  private List<Double> _cumulativeErrorRootMeanSquaredErrors;
  
  /****************************************************************************/
  /****************************************************************************/
  /******************************** CONSTRUCTOR *******************************/
  /****************************************************************************/
  /****************************************************************************/
  
  /**
   * Constructor.
   * 
   * @param shell The shell that the paired associate fast/slow experiment GUI
   * should be loaded into.
   */
  public PairedAssociateFastSlow(Shell shell){
    this._shell = shell;
    this._model = this._shell.getModel();
    
    this._fastPresentationTime = new JSpinner(new SpinnerNumberModel (2000, 0, Integer.MAX_VALUE, 1));
    this._fastInterItemTime = new JSpinner(new SpinnerNumberModel (3000, 0, Integer.MAX_VALUE, 1));
    this._fastInterTrialTime = new JSpinner(new SpinnerNumberModel (15000, 0, Integer.MAX_VALUE, 1));
    
    this._slowPresentationTime = new JSpinner(new SpinnerNumberModel (2500, 0, Integer.MAX_VALUE, 1));
    this._slowInterItemTime = new JSpinner(new SpinnerNumberModel (3500, 0, Integer.MAX_VALUE, 1));
    this._slowInterTrialTime = new JSpinner(new SpinnerNumberModel (15000, 0, Integer.MAX_VALUE, 1));
    
    this.renderInterface();
  }
  
  /****************************************************************************/
  /****************************************************************************/
  /***************************** GUI CONSTRUCTION *****************************/
  /****************************************************************************/
  /****************************************************************************/
  
  //Code that follows is organised from the GUI element's positions with respect 
  //to the screen i.e., left to right and then from high to low-level, i.e. from 
  //the most abstract GUI elements to the most concrete GUI elements. 
  
  private void renderInterface(){
    JSplitPane jsp = new JSplitPane (
      JSplitPane.HORIZONTAL_SPLIT, 
      this.renderInputView (), 
      this.renderDataView ()
    );
    jsp.setResizeWeight(0.5); //Sets split equally.
    
    this._experimentInterface = new JPanel();
    this._experimentInterface.setLayout(new GridLayout(1, 1));
    this._experimentInterface.add(jsp);
    
    this._shell.setContentPane(this._experimentInterface);
    this._shell.revalidate();
  }
  
  /****************************/
  /***** Experiment Input *****/
  /****************************/
  
  /**
   * Renders the input view.
   * 
   * @return 
   */
  private JPanel renderInputView(){
    JPanel input = new JPanel();
    input.setLayout(new GridLayout(5, 1));
    input.add(this.renderExperimentInformation());
    input.add(this.renderExperimentConditionsView());
    input.add(this.renderStimulusResponsePrioritiesView());
    input.add(this.renderExperimentInputView());
    input.add(this.renderExperimentControlsView());
    return input;
  }
  
  /**
   * Renders a textual description of the experiment.
   * 
   * @return 
   */
  private JScrollPane renderExperimentInformation(){
    JEditorPane experimentInfo = new JEditorPane ();
    experimentInfo.setEditorKit(new HTMLEditorKit());
    experimentInfo.setText(
      "<html>"
        + "<h2>Experiment Overview</h2>"
        + "This is the fast/slow paired-associate scripted experiment originally "
        + "created for Dmitry Bennet, a third year student of Fernand Gobet who "
        + "used it to prepare his third year dissertation in 2015 while studying "
        + "at the University of Liverpool."
        + "<h2>Experiment Input</h2>"
        + "This scripted experiment compares the performance of CHREST in a "
        + "number of paired-associate experiments using the stimulus-response "
        + "pairs and human data specified in its <code>input.xml</code> file "
        + "located in the following directory:"
        + "<p>"
        + "chrest-directory<br/>"
        + "&nbsp &#8627 scripted-experiment-inputs<br/>"
        + "&nbsp &nbsp &#8627 PairedAssociateFastSlow"
        + "<p>"
        + "The structure of the <code>input.xml</code> file is validated by its "
        + "corresponding <code>schema.xsd</code> file (found in the same "
        + "directory defined above)."
        + "<p>"
        + "After reading in the data specified in <code>input.xml</code>, the "
        + "<i>Human % Correct</i> and <i>Human Cumulative Errors</i> tables in"
        + "the <i>Data View</i> panel will be updated for verification."
        + "<h3>Pre-Learning</h3>"
        + "During experiment set-up, you will be asked to specify the location "
        + "of two files containing pre-learning data that CHREST will process "
        + "before a paired-associate experiment is undertaken.  There are three "
        + "types of pre-learning undertaken that produce three distinct "
        + "experiment conditions, these are:"
        + "<ol>"
        + "<li>Scan a dictionary</li> "
        + "<li>Learn some letters</li> "
        + "<li>No pre-learning</li>"
        + "</ol>"
        + "Both the files containing dictionary and letters data must be plain "
        + "<code>.txt</code> files and must be formatted as "
        + "<code>recognise-and-learn</code> data.  For an example of such data "
        + "see any of the <code>demo-*.txt</code> files found in the following "
        + "directory:"
        + "<p>"
        + "chrest-directory<br/>"
        + "&nbsp &#8627 examples<br/>"
        + "&nbsp &nbsp &#8627 sample-data"
        + "<h3>Presentation Speed</h3>"
        + "After pre-learning, the model will take part in one of two types of "
        + "paired-associate experiment, producing a further two experiment "
        + "conditions:"
        + "<ol>"
        + "<li>Fast presentation condition</li>"
        + "<li>Slow presentation condition</li>"
        + "</ol>"
        + "These conditions are implemented by the values set for the "
        + "paired-associate experiment's <code>presentation</code>, "
        + "<code>inter-item</code> and </code>inter-trial</code> times.  The "
        + "values for these times can be set in the <i>Experiment Controls</i> "
        + "panel below."
        + "<h3>Auditory Loop</h3>"
        + "CHREST's auditory loop in this experiment is limited by the total "
        + "number of stimulus-response pairs specified; there is no reason for "
        + "an auditory loop that can hold more stimulus-response pairs than the "
        + "amount specified."
        + "<p>"
        + "It is possible to specify the priority with which stimulus-response "
        + "pairs are placed into the model's auditory loop by either specifying "
        + "the priorities in the <code>input.xml</code> file or altering the "
        + "priority for each stimulus-response pair in the table contained "
        + "within the <i>Auditory Loop</i> panel below."
        + "<h2>Experiment Progression</h2>"
        + "There are at least six basic experiment conditions that CHREST is "
        + "tested on (three types of pre-learning for each presentation speed "
        + "condition: 3 &times 2).  Depending on the number of stimulus-response "
        + "pairs specified in the <code>input.xml</code> file, there will be at "
        + "most 6 &times <i>n</i> conditions set-up where <i>n</i> = the maximum "
        + "size of CHREST's auditory loop."
        + "<p>"
        + "After reading in the data specified in the <code>input.xml</code> "
        + "file, the experiment conditions will be displayed in the "
        + "<i>Experiment Conditions</i> table and the value of <i>n</i> will be "
        + "displayed as the denominator in the <i>Experiments Processed</i> "
        + "label in the <i>Experiment Controls</i> panel."
        + "<p>"
        + "Each experiment condition has CHREST undertake one paired-associate "
        + "experiment for <i>t</i> trials where <i>t</i> = the number of trials "
        + "for the presentation speed of the current experiment condition "
        + "specified in the <code>input.xml</code> file.  So, if there are 5 "
        + "percentage-correct data items specified for the fast presentation "
        + "speed and 6 for the slow presentation speed, <i>t</i> will equal 5 "
        + "in fast presentation speed experiment conditions and 6 in slow "
        + "presentation speed experiment conditions."  
        + "<h2>Experiment Output</h2>"
        + "For each trial in an experiment condition, the percentage of correct "
        + "responses and cumulative number of errors for each stimulus-response "
        + "pair made by CHREST is recorded.  When CHREST has undertaken <i>t</i> "
        + "trials for an experiment condition, the R<sup>2</sup> and "
        + "root-mean-squared-error (RMSE) is calculated by comparing the model's "
        + "performance with regard to these variables against the human data "
        + "specified in the <code>input.xml</code> file."
        + "<p>"
        + "After all experiment conditions have been undertaken by CHREST, the "
        + "<i>Model % Correct</i>, <i>Model Cumulative Errors</i>, "
        + "<i>% Correct Human/Model Fit</i> and <i>Cumulative Error Human/Model "
        + "Fit</i> data tables will be updated with relevant values and the "
        + "option to export the data from these tables will become available to "
        + "you."
        + "</html>"
    );

    JScrollPane jsp = new JScrollPane(
      experimentInfo
    );
    jsp.setBorder(new TitledBorder("Experiment Description"));
    
    //Scroll the vertical sidebar position to the top.  Its at the bottom by 
    //default (no idea why).
    experimentInfo.setCaretPosition(0);
    return jsp;
  }
  
  /**
   * Renders the experiment conditions used in the experiment, this data is used
   * by the experiment task and output data tables to set values for the 
   * independent variables used in the experiment.
   * 
   * @return 
   */
  private JScrollPane renderExperimentConditionsView(){
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }

      @Override
      public int getRowCount() {
        return PairedAssociateFastSlow.this._experimentConditionsTotal;
      }

      @Override
      public int getColumnCount() {
        return 4;
      }

      @Override
      public Object getValueAt(int row, int col) {
        String value;
        int experimentCondition = row + 1;
        
        if(col == 0){
          value = String.valueOf(experimentCondition);
        } else if(col == 1){
          
          value = PairedAssociateFastSlow.FAST_PRESENTATION_CONDITION;
          if(experimentCondition > PairedAssociateFastSlow.this._experimentConditionsTotal/2){
            value = PairedAssociateFastSlow.SLOW_PRESENTATION_CONDITION;
          }
          
        } else if(col == 2){
          int baseConditions = PairedAssociateFastSlow.this._experimentConditionsTotal/2;
          if(experimentCondition > baseConditions){
            experimentCondition -= baseConditions;
          }
          
          int numberConditionsPerPrelearningType = (baseConditions / 3);
          int integer = experimentCondition / numberConditionsPerPrelearningType;
          int fraction = experimentCondition % numberConditionsPerPrelearningType;
          
          if( integer == 0 || (integer == 1 && fraction == 0) ){
            value = PairedAssociateFastSlow.DICTIONARY_CONDITION;
          } else if( integer == 1 || (integer == 2 && fraction == 0)){
            value = PairedAssociateFastSlow.LETTERS_CONDITION;
          } else{
            value = PairedAssociateFastSlow.NO_PRE_LEARNING_CONDITION;
          }
        } else{
          int auditoryLoopSize = experimentCondition % PairedAssociateFastSlow.this._stimRespPairs.size();
          if(auditoryLoopSize == 0){
            auditoryLoopSize = PairedAssociateFastSlow.this._stimRespPairs.size();
          }
          
          value = String.valueOf(auditoryLoopSize);
        }
        
        return value;
      }
      
      @Override
      public String getColumnName (int column) {
        if (column == 0) {
          return "Experiment Condition";
        } else if (column == 1) {
          return "Presentation Speed";
        } else if (column == 2){
          return "Pre-Learning Type";
        } else {
          return "Auditory Loop Size";
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._humanCumulativeErrorsDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._experimentConditionsTable = new JTable (tm);
    PairedAssociateFastSlow.this._experimentConditionsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._experimentConditionsTable);
    
    JScrollPane experimentConditionsView = new JScrollPane(PairedAssociateFastSlow.this._experimentConditionsTable);
    experimentConditionsView.setBorder (new TitledBorder ("Experiment Conditions"));
    return experimentConditionsView;
  }
  
  /**
   * Renders the stimulus-response pairs priority view.
   * @return 
   */
  private JScrollPane renderStimulusResponsePrioritiesView () {
    
    this._stimulusResponsePriorityTableModel = new DefaultTableModel(){
      
      @Override
      public boolean isCellEditable(int row, int column) {
        
        //Only the "Priority" column is editable before any experiment 
        //conditions have been processed.
        return column == 2 && PairedAssociateFastSlow.this._experimentCondition == 0;
      }
    };
    
    this._stimulusResponsePriorityTableModel.addColumn("Stimulus");
    this._stimulusResponsePriorityTableModel.addColumn("Response");
    this._stimulusResponsePriorityTableModel.addColumn("Priority");
    
    this.updateStimulusResponsePriorityTableModel();
    
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
        for(int i = 0; i < PairedAssociateFastSlow.this._stimulusResponsePriorityTableModel.getRowCount(); i++){
          if(i != tcl.getRow()){
            Integer priorityToCheck = Integer.valueOf((String)PairedAssociateFastSlow.this._stimulusResponsePriorityTableModel.getValueAt(i, 2));
            if(Objects.equals(priorityToCheck, newPriorityInteger)){
              PairedAssociateFastSlow.this._stimRespPairsAndPriorities.put( 
                PairedAssociateFastSlow.this._stimRespPairs.get(i), 
                oldPriorityInteger 
              );
              break;
            }
          }
        }
        
        //Now, set the new priority for P in the data model that feeds the table
        //model.
        PairedAssociateFastSlow.this._stimRespPairsAndPriorities.put( 
          PairedAssociateFastSlow.this._stimRespPairs.get(tcl.getRow()), 
          newPriorityInteger 
        );
        
        //Finally, update the table model so that the table in the GUI displays
        //the updated priorities.
        PairedAssociateFastSlow.this.updateStimulusResponsePriorityTableModel();
      }
    };
    TableCellListener tcl = new TableCellListener(stimulusResponsePriorityJTable, priorityReassignment);
    
    JScrollPane stimulusResponsePriorityView = new JScrollPane(stimulusResponsePriorityJTable);
    stimulusResponsePriorityView.setBorder (new TitledBorder ("Stimulus-Response Pairs and Priorities"));
    return stimulusResponsePriorityView;
  }
  
  /**
   * Renders the experiment input view - contains GUI elements for setting speed
   * condition independent variables.
   * 
   * @return 
   */
  private JScrollPane renderExperimentInputView(){
    JPanel panel = new JPanel();
    panel.setLayout (new GridLayout (6, 2, 2, 2));
    
    panel.add(new JLabel ("Fast presentation time (ms)", SwingConstants.RIGHT));
    panel.add(this._fastPresentationTime);
    
    panel.add(new JLabel ("Fast inter item time (ms)", SwingConstants.RIGHT));
    panel.add(this._fastInterItemTime);
    
    panel.add(new JLabel ("Fast inter trial time (ms)", SwingConstants.RIGHT));
    panel.add(this._fastInterTrialTime);
    
    panel.add(new JLabel ("Slow presentation time (ms)", SwingConstants.RIGHT));
    panel.add(this._slowPresentationTime);
    
    panel.add(new JLabel ("Slow inter item time (ms)", SwingConstants.RIGHT));
    panel.add(this._slowInterItemTime);
    
    panel.add(new JLabel ("Slow inter trial time (ms)", SwingConstants.RIGHT));
    panel.add(this._slowInterTrialTime);
    
    JScrollPane dataInputView = new JScrollPane(panel);
    dataInputView.setBorder (new TitledBorder ("Experiment Input Data"));
    return dataInputView;
  }
  
  /**
   * Renders the experiment controls view - buttons for starting, stopping, 
   * restarting the experiment, export data actions and information about 
   * experiment progress.
   * 
   * @return 
   */
  private JScrollPane renderExperimentControlsView(){
    JPanel panel = new JPanel();
    panel.setLayout (new GridLayout (3, 2, 2, 2));
    
    this._restartButton = new JButton(new RestartExperimentAction());
    this._restartButton.setEnabled(false);
    panel.add(this._restartButton);
    
    this._stopButton = new JButton(new StopExperimentAction());
    this._stopButton.setEnabled(false);
    panel.add(this._stopButton);
    
    this._exportDataButton = new JButton(new ExportDataAction());
    this._exportDataButton.setEnabled(false);
    panel.add(this._exportDataButton);
      
    this._runButton = new JButton(new RunExperimentAction());
    panel.add(this._runButton);
    
    panel.add(new JLabel("<html><b>Experiments Processed</b></html>"));
    panel.add(this._experimentsProcessedLabel);
    
    JScrollPane controlsView = new JScrollPane(panel);
    controlsView.setBorder (new TitledBorder ("Experiment Controls"));
    return controlsView;
  }
  
  /***************************/
  /***** Experiment Data *****/
  /***************************/
  
  /**
   * Renders the data view containing human data that will be used as input to 
   * the experiment and model output data.
   * 
   * @return 
   */
  private JPanel renderDataView(){
    this.createHumanPercentageCorrectDataTable();
    this.createHumanCumulativeErrorsDataTable();
    this.createModelPercentageCorrectDataTable();
    this.createModelCumulativeErrorsDataTable();
    this.createPercentageCorrectModelFitDataTable();
    this.createCumulativeErrorsModelFitDataTable();
    
    JScrollPane humanPercentageCorrectDataTableScrollPane = new JScrollPane (this._humanPercentageCorrectDataTable);
    humanPercentageCorrectDataTableScrollPane.setBorder(new TitledBorder("Human % Correct"));
    
    JScrollPane humanCumulativeErrorsDataTableScrollPane = new JScrollPane (this._humanCumulativeErrorsDataTable);
    humanCumulativeErrorsDataTableScrollPane.setBorder(new TitledBorder("Human Cumulative Errors"));
    
    JScrollPane modelPercentageCorrectDataTableScrollPane = new JScrollPane (this._modelPercentageCorrectDataTable);
    modelPercentageCorrectDataTableScrollPane.setBorder(new TitledBorder("Model % Correct"));
    
    JScrollPane modelCumulativeErrorsDataTableScrollPane = new JScrollPane (this._modelCumulativeErrorsDataTable);
    modelCumulativeErrorsDataTableScrollPane.setBorder(new TitledBorder("Model Cumulative Errors"));
    
    JScrollPane percentageCorrectModelFitDataTableScrollPane = new JScrollPane (this._percentageCorrectModelFitDataTable);
    percentageCorrectModelFitDataTableScrollPane.setBorder(new TitledBorder("% Correct Model Fit"));
    
    JScrollPane cumulativeErrorsModelFitDataTableScrollPane = new JScrollPane (this._cumulativeErrorsModelFitDataTable);
    cumulativeErrorsModelFitDataTableScrollPane.setBorder(new TitledBorder("Cumulative Errors Model Fit"));
    
    JPanel dataPanel = new JPanel();
    dataPanel.setLayout(new GridLayout(3, 2));
    dataPanel.setBorder(new TitledBorder("Data View"));
    dataPanel.add(humanPercentageCorrectDataTableScrollPane);
    dataPanel.add(humanCumulativeErrorsDataTableScrollPane);
    dataPanel.add(modelPercentageCorrectDataTableScrollPane);
    dataPanel.add(modelCumulativeErrorsDataTableScrollPane);
    dataPanel.add(percentageCorrectModelFitDataTableScrollPane);
    dataPanel.add(cumulativeErrorsModelFitDataTableScrollPane);
    
    return dataPanel;
  }
  
  /**
   * Creates the table holding human percentage correct data that has been 
   * read-in from the input file.
   */
  private void createHumanPercentageCorrectDataTable(){
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        return PairedAssociateFastSlow.this._maximumNumberOfTrials;
      }
      
      @Override
      public int getColumnCount () {
        return 3; 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        String value = "";
        
        if (column == 0) {
          value = String.valueOf(row + 1);
        } 
        else if(column == 1 && row < PairedAssociateFastSlow.this._humanPercentageCorrectData.get( PairedAssociateFastSlow.FAST_PRESENTATION_CONDITION ).size()) {
          value = String.valueOf(PairedAssociateFastSlow.this._humanPercentageCorrectData.get( PairedAssociateFastSlow.FAST_PRESENTATION_CONDITION ).get(row));
        }
        else{
          if(row < PairedAssociateFastSlow.this._humanPercentageCorrectData.get( PairedAssociateFastSlow.SLOW_PRESENTATION_CONDITION ).size()){
            value = String.valueOf(PairedAssociateFastSlow.this._humanPercentageCorrectData.get( PairedAssociateFastSlow.SLOW_PRESENTATION_CONDITION ).get(row));
          }
        }
        
        return value;
      }
      
      @Override
      public String getColumnName (int column) {
        if (column == 0) {
          return "Trial #";
        } else if (column == 1) {
          return "Fast Presentation";
        } else {
          return "Slow Presentation";
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._humanPercentageCorrectDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._humanPercentageCorrectDataTable = new JTable (tm);
    PairedAssociateFastSlow.this._humanPercentageCorrectDataTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._humanPercentageCorrectDataTable);
  }
  
  /**
   * Creates the table holding human cumulative errors data that has been 
   * read-in from the input file. 
   */
  private void createHumanCumulativeErrorsDataTable(){
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        return PairedAssociateFastSlow.this._stimRespPairs.size();
      }
      
      @Override
      public int getColumnCount () {
        return 3; 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        if (column == 0) {
          return PairedAssociateFastSlow.this._stimRespPairs.get(row).getFirst().toString();
        } 
        else if(column == 1) {
          
          //No idea why but if an attempt is made to access the Double value 
          //directly, null is returned.  The loop below seems to circumvent this 
          //problem.
          String value = "";
          ListPattern first = PairedAssociateFastSlow.this._stimRespPairs.get(row).getFirst();
          for(Entry<ListPattern, Double> stimulusError : PairedAssociateFastSlow.this._humanCumulativeErrorsData.get( PairedAssociateFastSlow.FAST_PRESENTATION_CONDITION ).entrySet()){
            if(stimulusError.getKey().equals(first)){
              value = stimulusError.getValue().toString();
            }
          }
          return value;
        }
        else{
          
          //No idea why but if an attempt is made to access the Double value 
          //directly, null is returned.  The loop below seems to circumvent this 
          //problem.
          String value = "";
          ListPattern first = PairedAssociateFastSlow.this._stimRespPairs.get(row).getFirst();
          for(Entry<ListPattern, Double> stimulusError : PairedAssociateFastSlow.this._humanCumulativeErrorsData.get( PairedAssociateFastSlow.SLOW_PRESENTATION_CONDITION ).entrySet()){
            if(stimulusError.getKey().equals(first)){
              value = stimulusError.getValue().toString();
            }
          }
          return value;
        }
      }
      
      @Override
      public String getColumnName (int column) {
        if (column == 0) {
          return "Stimulus";
        } else if (column == 1) {
          return "Fast Presentation";
        } else {
          return "Slow Presentation";
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._humanCumulativeErrorsDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._humanCumulativeErrorsDataTable = new JTable (tm);
    PairedAssociateFastSlow.this._humanCumulativeErrorsDataTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._humanCumulativeErrorsDataTable);
  }
  
  /**
   * Displays the percentage of correct responses produced by CHREST for each 
   * trial in each experiment condition.
   */
  private void createModelPercentageCorrectDataTable(){
    TableModel tm = new AbstractTableModel () {
      
      /**
       * Converts a row number to the correct experiment condition.  The 
       * following procedure is implemented:
       * 
       * <ol>
       *  <li>
       *    Increment the row number by 1 since they are zero-indexed to return
       *    a number, n, indicative of an experiment condition and a trial 
       *    number within that condition.  For example, if there are 3 trials 
       *    per experiment condition and row 7 is passed, the row indicates that 
       *    its data pertains to the second trial of the third experiment 
       *    condition so n = 8.
       *  </li>
       *  <li>
       *    Divide n by the maximum number of trials possible given the human
       *    data that has been read-in and retrieve both the integer, i, and the
       *    fraction, f, yielded by this division: i denotes what experiment 
       *    condition n represents, f determines whether i is returned 
       *    unmodified or not.
       *  </li>
       *  <li>
       *   If f is greater than 0 then i should be incremented by 1 before it
       *   is returned.  For example, if there are 3 trials per experiment 
       *   condition and row 0 is passed, n = 1 and 1/3 gives i = 0 and f = 33..
       *   Therefore, i should be incremented by 1.  However, if row 2 is 
       *   passed, n = 3 and 3/3 gives i = 1 and f = 0 so i should not be 
       *   modified before being returned.
       *  </li>
       * <ol>
       * @param row
       * @return 
       */
      private int convertRowNumberToExperimentCondition(int row){        
        row++;

        int integer = row / PairedAssociateFastSlow.this._maximumNumberOfTrials;
        int fraction = row % PairedAssociateFastSlow.this._maximumNumberOfTrials;

        if(fraction > 0){
          integer ++;
        }

        return integer;
      }
      
      @Override
      public int getRowCount () {
        return PairedAssociateFastSlow.this._experimentConditionsTotal * PairedAssociateFastSlow.this._maximumNumberOfTrials;
      }
      
      @Override
      public int getColumnCount () {
        return 6; 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        if(column == 0){
          return this.convertRowNumberToExperimentCondition(row);
        } else if(column == 1){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(this.convertRowNumberToExperimentCondition(row) - 1, column);
        } else if(column == 2){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(this.convertRowNumberToExperimentCondition(row) - 1, column);
        } else if(column == 3){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(this.convertRowNumberToExperimentCondition(row) - 1, column);
        } else if (column == 4) {
          
          //The trial number should be the smallest unit of the integer yielded
          //after adding 1 to row (rows are zero-indexed) from 1-10.
          String rowAsString = String.valueOf(row + 1);
          if(rowAsString.endsWith("0")){
            return 10;
          }
          else{
            return rowAsString.substring(rowAsString.length()-1);
          }
        } 
        else {
          String value = "";
          if(row < PairedAssociateFastSlow.this._modelPercentageCorrectData.size()){
            return String.format("%.2f", PairedAssociateFastSlow.this._modelPercentageCorrectData.get(row));
          }
          
          return value;
        }
      }
      
      @Override
      public String getColumnName (int column) {
        if(column == 0){
          return "Experiment Condition";
        } else if (column == 1) {
          return "Presentation Speed";
        } else if (column == 2){
          return "Pre-Learning Type";
        } else if (column == 3){
          return "Auditory Loop Size";
        } else if (column == 4) {
          return "Trial #";
        } else {
          return "Result";
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._modelPercentageCorrectDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._modelPercentageCorrectDataTable = new JTable (tm);
    PairedAssociateFastSlow.this._modelPercentageCorrectDataTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._modelPercentageCorrectDataTable);
  }
  
  /**
   * Displays the cumulative number of errors for each stimulus-response pair 
   * produced by CHREST in each experiment condition.
   */
  private void createModelCumulativeErrorsDataTable(){
    TableModel tm = new AbstractTableModel () {
      
      /**
       * Converts a row number to the correct experiment condition.  The 
       * following procedure is implemented:
       * 
       * <ol>
       *  <li>
       *    Increment the row number by 1 since they are zero-indexed to return
       *    a number, n, indicative of an experiment condition and a 
       *    stimulus-response pair within that condition.  For example, if there 
       *    are 3 stimulus-response pairs per experiment condition and row 7 is 
       *    passed, the row indicates that its data pertains to the second 
       *    stimulus-response pair trial of the third experiment condition so n 
       *    = 8.
       *  </li>
       *  <li>
       *    Divide n by the total number of stimulus-response pairs given the 
       *    human data that has been read-in and retrieve both the integer, i, 
       *    and the fraction, f, yielded by this division: i denotes what 
       *    experiment condition n represents, f determines whether i is 
       *    returned unmodified or not.
       *  </li>
       *  <li>
       *   If f is greater than 0 then i should be incremented by 1 before it
       *   is returned.  For example, if there are 3 stimulus-response pairs per 
       *   experiment condition and row 0 is passed, n = 1 and 1/3 gives i = 0 
       *   and f = 33.. Therefore, i should be incremented by 1.  However, if 
       *   row 2 is passed, n = 3 and 3/3 gives i = 1 and f = 0 so i should not 
       *   be modified before being returned.
       *  </li>
       * <ol>
       * @param row
       * @return 
       */
      private int convertRowNumberToExperimentCondition(int row){
        row++;

        int integer = row / PairedAssociateFastSlow.this._stimRespPairs.size();
        int fraction = row % PairedAssociateFastSlow.this._stimRespPairs.size();

        if(fraction > 0){
          integer ++;
        }

        return integer;
      }
      
      @Override
      public int getRowCount () {
        return PairedAssociateFastSlow.this._experimentConditionsTotal * PairedAssociateFastSlow.this._stimRespPairs.size();
      }
      
      @Override
      public int getColumnCount () {
        return 6; 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        if(column == 0){
          return this.convertRowNumberToExperimentCondition(row);
        } else if(column == 1){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(this.convertRowNumberToExperimentCondition(row) - 1, column);
        } else if(column == 2){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(this.convertRowNumberToExperimentCondition(row) - 1, column);
        } else if(column == 3){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(this.convertRowNumberToExperimentCondition(row) - 1, column);
        } else if (column == 4) {
          return PairedAssociateFastSlow.this._stimRespPairs.get(PairedAssociateFastSlow.this.getStimulusResponsePairIndexForExperimentNumber(row + 1)).getFirst().toString();
        } 
        else {
          String value = "";
          
          //Convert the row into an experiment number by adding 1 to row 
          //experiment numbers are non-zero indexed).
          int experimentNumber = row + 1;
          
          //Get the number of stimulus response pairs and divide the experiment
          //number by this and retrieve the fraction too.  We divide by the 
          //number of stimulus-response pairs since each experiment conidition
          //is comprised of n stimulus-response pairs in the table.
          int numberStimRespPairs = PairedAssociateFastSlow.this._stimRespPairs.size();
          int experimentCondition = experimentNumber / numberStimRespPairs;
          int fraction = experimentNumber % numberStimRespPairs;

          //If the fraction is 0 then this is the last-stimulus-response pair
          //of the experiment condition and the experiment condition will be 1
          //greater than what it should be, so correct this.
          if(fraction == 0){
            experimentCondition--;
          }

          //Now, check to see if the experiment condition calculated has a value
          //in the data used to populate the table model.  If so, get the value
          //for the stimulus in the experiment condition.
          if(experimentCondition < PairedAssociateFastSlow.this._modelCumulativeErrorsData.size()){
            ListPattern stimulus = PairedAssociateFastSlow.this._stimRespPairs.get(PairedAssociateFastSlow.this.getStimulusResponsePairIndexForExperimentNumber(row + 1)).getFirst();
            value = String.valueOf(PairedAssociateFastSlow.this._modelCumulativeErrorsData.get(experimentCondition).get(stimulus));
          }
          
          return value;
        }
      }
      
      @Override
      public String getColumnName (int column) {
        if(column == 0){
          return "Experiment Condition";
        } else if (column == 1) {
          return "Presentation Speed";
        } else if (column == 2){
          return "Pre-Learning Type";
        } else if (column == 3){
          return "Auditory Loop Size";
        } else if (column == 4) {
          return "Stimulus";
        } else {
          return "Result";
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._modelCumulativeErrorsDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._modelCumulativeErrorsDataTable = new JTable (tm);
    PairedAssociateFastSlow.this._modelCumulativeErrorsDataTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._modelCumulativeErrorsDataTable);
  }
  
  /**
   * Displays data denoting how well the model fits the human data for 
   * percentage of correct responses given for each experiment condition.
   */
  private void createPercentageCorrectModelFitDataTable(){
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        
        //Total number of experiment conditions.
        return PairedAssociateFastSlow.this._experimentConditionsTotal;
      }
      
      @Override
      public int getColumnCount () {
        return 6; 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        if(column == 0){
          return row + 1;
        } else if(column == 1){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(row, column);
        } else if(column == 2){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(row, column);
        } else if(column == 3){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(row, column);
        } else if(column == 4){
          String value = "";
          if(row < PairedAssociateFastSlow.this._percentageCorrectRSquares.size()){
            value = String.format("%.2f", PairedAssociateFastSlow.this._percentageCorrectRSquares.get(row));
          }
          return value;
        }
        else{
          String value = "";
          if(row < PairedAssociateFastSlow.this._percentageCorrectRootMeanSquaredErrors.size()){
            value = String.format("%.2f", PairedAssociateFastSlow.this._percentageCorrectRootMeanSquaredErrors.get(row));
          }
          return value;
        }
      }
      
      @Override
      public String getColumnName (int column) {
        if(column == 0){
          return "Experiment Condition";
        } else if (column == 1) {
          return "Presentation Speed";
        } else if (column == 2){
          return "Pre-Learning Type";
        } else if (column == 3){
          return "Auditory Loop Size";
        } else if(column == 4){
          return "<html>R<sup>2</sup></html>";
        } else {
          return "RMSE";
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._percentageCorrectModelFitDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._percentageCorrectModelFitDataTable = new JTable (tm);
    PairedAssociateFastSlow.this._percentageCorrectModelFitDataTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._percentageCorrectModelFitDataTable);
  }
  
  /**
   * Displays data denoting how well the model fits the human data for 
   * cumulative errors made for each experiment condition.
   */
  private void createCumulativeErrorsModelFitDataTable(){
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        return PairedAssociateFastSlow.this._experimentConditionsTotal;
      }
      
      @Override
      public int getColumnCount () {
        return 6; 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        
        if(column == 0){
          return row + 1;
        } else if(column == 1){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(row, column);
        } else if(column == 2){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(row, column);
        } else if(column == 3){
          return PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(row, column);
        } else if(column == 4){
          String value = "";
          
          if(row < PairedAssociateFastSlow.this._cumulativeErrorsRSquares.size()){
            value = String.format("%.2f", PairedAssociateFastSlow.this._cumulativeErrorsRSquares.get(row));
          }
          
          return value;
        }
        else{
          String value = "";
          
          if(row < PairedAssociateFastSlow.this._cumulativeErrorRootMeanSquaredErrors.size()){
            value = String.format("%.2f", PairedAssociateFastSlow.this._cumulativeErrorRootMeanSquaredErrors.get(row));
          }
          
          return value;
        }
      }
      
      @Override
      public String getColumnName (int column) {
        if(column == 0){
          return "Experiment Condition";
        } else if (column == 1) {
          return "Presentation Speed";
        } else if (column == 2){
          return "Pre-Learning Type";
        } else if (column == 3){
          return "Auditory Loop Size";
        } else if(column == 4){
          return "<html>R<sup>2</sup></html>";
        } else {
          return "RMSE";
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._cumulativeErrorsModelFitDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._cumulativeErrorsModelFitDataTable = new JTable (tm);
    PairedAssociateFastSlow.this._cumulativeErrorsModelFitDataTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._cumulativeErrorsModelFitDataTable);
  }
  
  /**
   * Calculates the stimulus-response pair index to use for a given experiment
   * number.
   * 
   * @param experimentNumber
   * @return 
   */
  private int getStimulusResponsePairIndexForExperimentNumber(int experimentNumber){
          
    //Get the ineteger and fraction returned after dividing the experiment
    //number by the number of stimulus-response pairs in the experiment.
    //The integer denotes the experiment condition and this should be
    //zero-indexed for the next step of the calculation.
    int integer = experimentNumber / PairedAssociateFastSlow.this._stimRespPairs.size();
    int fraction = experimentNumber % PairedAssociateFastSlow.this._stimRespPairs.size();

    //If the fraction is equal to 0 then this is the last 
    //stimulus-response pair in the experiment condition.  In this case, 
    //decrement the integer by 1.  For example, if there are 3 
    //stimulus-response pairs, and row = 3, the integer will
    //be equal to 1 however, the integer should be equal to what it is
    //when experiment number is = 1/2 (since these experiment numbers are
    //part of the same experiment condition).
    if(fraction == 0){
      integer--;
    }

    //Now, calculate the stimulus-response pair number to retrieve (index)
    //by multiplying the integer calculated by the number of 
    //stimulus-response pairs in the experiment.  This will yield the 
    //value that needs to be subtracted from the experiment number to 
    //retrieve the correct stimulus-response pair.  For example, if this
    //row is equivalent to experiment number 4 and there are three 
    //stimulus-response pairs used in the human data then row 4 should 
    //display the serial position data for the first stimulus-response #
    //pair of the second experiment condition.
    int modifier = integer * PairedAssociateFastSlow.this._stimRespPairs.size();
    return (experimentNumber - modifier) - 1;
  }
  
  /**
   * Populates the stimulus-response-priority table model with the data in the
   * stimulus-response priority table model.
   */
  private void updateStimulusResponsePriorityTableModel(){
    
    //Check that there are stimulus-response-priorites.
    if(PairedAssociateFastSlow.this._stimRespPairsAndPriorities.size() > 0){
      
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
    for(Entry<PairedPattern, Integer> pairedPatternAndPriority : PairedAssociateFastSlow.this._stimRespPairsAndPriorities.entrySet()){
      PairedPattern pairedPattern = pairedPatternAndPriority.getKey();
      Object[] rowData = {
        pairedPattern.getFirst().toString (),
        pairedPattern.getSecond().toString (),
        pairedPatternAndPriority.getValue().toString()
      };
      this._stimulusResponsePriorityTableModel.addRow(rowData);
    }
  }
  
  /**
   * Instantiates the data structures used to record model results from the 
   * experiment.
   */
  public void instantiateResultsStorage(){
    PairedAssociateFastSlow.this._modelPercentageCorrectData = new ArrayList();
    PairedAssociateFastSlow.this._modelCumulativeErrorsData = new ArrayList();
    PairedAssociateFastSlow.this._percentageCorrectRSquares = new ArrayList();
    PairedAssociateFastSlow.this._cumulativeErrorsRSquares = new ArrayList();
    PairedAssociateFastSlow.this._percentageCorrectRootMeanSquaredErrors = new ArrayList();
    PairedAssociateFastSlow.this._cumulativeErrorRootMeanSquaredErrors = new ArrayList();
  }
  
  /**
   * Updates display of all non-model output data tables.
   */
  private void updateExperimentInformationTables(){
    ((AbstractTableModel)PairedAssociateFastSlow.this._experimentConditionsTable.getModel()).fireTableStructureChanged();
    this.updateStimulusResponsePriorityTableModel();
    ((AbstractTableModel)PairedAssociateFastSlow.this._humanPercentageCorrectDataTable.getModel()).fireTableStructureChanged();
    ((AbstractTableModel)PairedAssociateFastSlow.this._humanCumulativeErrorsDataTable.getModel()).fireTableStructureChanged();
  }
  
  /**
   * Updates display of all model output data tables.
   */
  private void updateModelOutputTables(){
    ((AbstractTableModel)PairedAssociateFastSlow.this._modelPercentageCorrectDataTable.getModel()).fireTableStructureChanged();
    ((AbstractTableModel)PairedAssociateFastSlow.this._modelCumulativeErrorsDataTable.getModel()).fireTableStructureChanged();
    ((AbstractTableModel)PairedAssociateFastSlow.this._percentageCorrectModelFitDataTable.getModel()).fireTableStructureChanged();
    ((AbstractTableModel)PairedAssociateFastSlow.this._cumulativeErrorsModelFitDataTable.getModel()).fireTableStructureChanged();
  }
  
  /**
   * Updates experiments processed label.
   */
  private void updateExperimentsProcessedLabel(){
    String text = "";
    if(this._experimentConditionsTotal > 0){
      text = this._experimentCondition + "/" + this._experimentConditionsTotal;
    }
    this._experimentsProcessedLabel.setText(text);
  }
  
  /****************************************************************************/
  /****************************************************************************/
  /********************************** ACTIONS *********************************/
  /****************************************************************************/
  /****************************************************************************/
  
  /**
   * Prepares and exports model output table data.
   */
  class ExportDataAction extends AbstractAction implements ActionListener {
    
    ExportDataAction () {
      super ("Export Data Tables as CSV Files");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      
      ArrayList<String> percentageCorrectModel = new ArrayList<>();
      ArrayList<String> cumulativeErrorModel = new ArrayList<>();
      ArrayList<String> percentageCorrectRSquare = new ArrayList<>();
      ArrayList<String> cumulativeErrorRSquare = new ArrayList<>();
      
      percentageCorrectModel.add(ExportData.extractJTableDataAsCsv(PairedAssociateFastSlow.this._modelPercentageCorrectDataTable));
      percentageCorrectModel.add("percentageCorrectModelData");
      percentageCorrectModel.add("csv");
      
      cumulativeErrorModel.add(ExportData.extractJTableDataAsCsv(PairedAssociateFastSlow.this._modelCumulativeErrorsDataTable));
      cumulativeErrorModel.add("cumulativeErrorModelData");
      cumulativeErrorModel.add("csv");
      
      percentageCorrectRSquare.add(ExportData.extractJTableDataAsCsv(PairedAssociateFastSlow.this._percentageCorrectModelFitDataTable));
      percentageCorrectRSquare.add("percentageCorrectModelFitData");
      percentageCorrectRSquare.add("csv");
      
      cumulativeErrorRSquare.add(ExportData.extractJTableDataAsCsv(PairedAssociateFastSlow.this._cumulativeErrorsModelFitDataTable));
      cumulativeErrorRSquare.add("cumulativeErrorModelFitData");
      cumulativeErrorRSquare.add("csv");
      
      ArrayList<ArrayList<String>> dataToSave = new ArrayList<>();
      dataToSave.add(percentageCorrectModel);
      dataToSave.add(cumulativeErrorModel);
      dataToSave.add(percentageCorrectRSquare);
      dataToSave.add(cumulativeErrorRSquare);
      
      ExportData.saveFile(PairedAssociateFastSlow.this._shell, "CHREST-paired-associate-fast-slow-experiment-data", dataToSave);
    }
  }
  
  /**
   * Restarts the experiment.  The following procedures are performed:
   * 
   * <ol>
   *  <li>
   *    CHREST model being used in the experiment is unfrozen and cleared.
   *  </li>
   *  <li>
   *    Model output data storage is re-instantiated and model data tables are
   *    updated (clearing them).
   *  </li>
   *  <li>
   *    Experiment progress variables are reset to their original values.
   *  </li>
   *  <li>
   *    GUI elements are reset.
   *  </li>
   * </ol>
   */
  class RestartExperimentAction extends AbstractAction implements ActionListener {
    
    RestartExperimentAction(){
      super("Restart Experiment");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      PairedAssociateFastSlow.this._model.unfreeze();
      PairedAssociateFastSlow.this._model.clear();
      
      PairedAssociateFastSlow.this._dictionary.clear();
      PairedAssociateFastSlow.this._experiment = null;
      PairedAssociateFastSlow.this._experimentCondition = 0;
      PairedAssociateFastSlow.this._experimentConditionsTotal = 0;
      PairedAssociateFastSlow.this.updateExperimentsProcessedLabel();
      PairedAssociateFastSlow.this._humanCumulativeErrorsData.clear();
      PairedAssociateFastSlow.this._humanPercentageCorrectData.clear();
      PairedAssociateFastSlow.this._independentVariablesSetup = false;
      PairedAssociateFastSlow.this._letters.clear();
      PairedAssociateFastSlow.this._maximumNumberOfTrials = 0;
      PairedAssociateFastSlow.this._modelCumulativeErrorsData.clear();
      PairedAssociateFastSlow.this._modelPercentageCorrectData.clear();
      PairedAssociateFastSlow.this._percentageCorrectRSquares.clear();
      PairedAssociateFastSlow.this._percentageCorrectRootMeanSquaredErrors.clear();
      PairedAssociateFastSlow.this._readInSuccessful = false;
      PairedAssociateFastSlow.this._runExperiments = false;
      PairedAssociateFastSlow.this._stimRespPairs.clear();
      PairedAssociateFastSlow.this._stimRespPairsAndPriorities.clear();
      PairedAssociateFastSlow.this.instantiateResultsStorage();
      
      PairedAssociateFastSlow.this._fastInterItemTime.setEnabled(true);
      PairedAssociateFastSlow.this._fastInterTrialTime.setEnabled(true);
      PairedAssociateFastSlow.this._fastPresentationTime.setEnabled(true);
      PairedAssociateFastSlow.this._slowInterItemTime.setEnabled(true);
      PairedAssociateFastSlow.this._slowInterTrialTime.setEnabled(true);
      PairedAssociateFastSlow.this._slowPresentationTime.setEnabled(true);
      
      PairedAssociateFastSlow.this._exportDataButton.setEnabled(false);
      PairedAssociateFastSlow.this._restartButton.setEnabled(false);
      PairedAssociateFastSlow.this._runButton.setEnabled(true);
      
      PairedAssociateFastSlow.this.renderInterface();
    }
  }
  
  /**
   * Triggers running of experiments from the current experiment condition that
   * is to be processed.
   */
  class RunExperimentAction extends AbstractAction implements ActionListener {
    
    RunExperimentAction(){
      super("Run Experiment");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      javax.swing.SwingUtilities.invokeLater(() -> {
        ReadExperimentData readExperimentDataThread = new ReadExperimentData();
        readExperimentDataThread.execute();
      });
    }
  }
  
  /**
   * Stops experiment processing after the current experiment condition has
   * completed.
   */
  class StopExperimentAction extends AbstractAction implements ActionListener {
    
    StopExperimentAction(){
      super("Stop Experiment");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      PairedAssociateFastSlow.this._shell.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      PairedAssociateFastSlow.this._runExperiments = false;
      PairedAssociateFastSlow.this._restartButton.setEnabled(true);
      PairedAssociateFastSlow.this._stopButton.setEnabled(false);
      PairedAssociateFastSlow.this._exportDataButton.setEnabled(true);
      PairedAssociateFastSlow.this._runButton.setEnabled(true);
    }
  }
  
  /**
   * Conducts the experiments using the experiment conditions specified and the
   * CHREST model loaded into the experiment.
   */
  class Task extends SwingWorker<Void, Void> {
    
    //Allows the model's record keeping setting to be set back to what it was
    //when this script was run since record keeping will be turned off for the
    //duration of the experiment.  This is done since there is a lot of 
    //information to process so can slow the process of the script plus, the 
    //model is cleared after each experiment any way so no history is maintained
    //any way.
    private boolean _originalRecordKeepingSetting; 

    @Override
    protected Void doInBackground() throws Exception {
      
      /*****************/
      /***** Setup *****/
      /*****************/

      //Set cursor to "busy" so the user knows that the script is busy.
      PairedAssociateFastSlow.this._shell.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      
      //Don't update model visuals since this will slow the script to some 
      //degree and is superflous any way.
      PairedAssociateFastSlow.this._model.freeze();
      
      //Get the model's current history recording setting and turn off history
      //recording.
      this._originalRecordKeepingSetting = _model.canRecordHistory();
      PairedAssociateFastSlow.this._model.setRecordHistory(false);
      
      //If the experiment is not "resuming" after being stopped, set the 
      //experiment number to 1 otherwise, do not change it.
      if(PairedAssociateFastSlow.this._experimentCondition == 0){
        PairedAssociateFastSlow.this._experimentCondition = 1;
      }
      
      /******************************************************/
      /***** Start Experiment Condition Processing Loop *****/
      /******************************************************/

      for(
        ; //Don't set any counters initially, this is done above and will cause incorrect experiment progression if the experiment is stopped after starting.
        PairedAssociateFastSlow.this._experimentCondition <= PairedAssociateFastSlow.this._experimentConditionsTotal && PairedAssociateFastSlow.this._runExperiments; 
        PairedAssociateFastSlow.this._experimentCondition++
      ){
        
        /************************/
        /***** Pre-learning *****/
        /************************/
        
        String preLearning = PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(_experimentCondition - 1, 2).toString();
        switch(preLearning){
          
          //If pre-learning prescribes use of the dictionary the model should 
          //"skim" the dictionary once.
          case PairedAssociateFastSlow.DICTIONARY_CONDITION:
            for(ListPattern word : PairedAssociateFastSlow.this._dictionary){
              PairedAssociateFastSlow.this._model.recogniseAndLearn(word);
            }
            break;
            
          //If pre-learning prescribes the use of letters the model should learn
          //the letters completely.
          case PairedAssociateFastSlow.LETTERS_CONDITION:
            for(ListPattern letter : PairedAssociateFastSlow.this._letters){
              Node recognisedNode = PairedAssociateFastSlow.this._model.recogniseAndLearn(letter);
              while(!recognisedNode.getImage().equals(letter)){
                recognisedNode = PairedAssociateFastSlow.this._model.recogniseAndLearn(letter);
              }
            }
            break;
        }
        
        /*****************************/
        /***** Create Experiment *****/
        /*****************************/
        
        //Set-up the experiment after pre-learning since the experiment 
        //constructor resets the model's learning clock.  This clock will be 
        //higher than 0 due to pre-learning so the experiment will not be 
        //conducted properly (experiment time will need to be greater than the 
        //learning clock value before any learning of experiment stimuli-response
        //pairs occurs) and may cause out-of-memory errors.
        PairedAssociateFastSlow.this._experiment = new PairedAssociateExperiment(_model, PairedAssociateFastSlow.this._stimRespPairs);
        
        /******************************************/
        /***** Setup Auditory Loop Priorities *****/
        /******************************************/
        
        //Set the auditory loop priorities in the experiment according to what 
        //has been specified in the interface.
        for(Entry<PairedPattern, Integer> stimulusResponseAndPriority : PairedAssociateFastSlow.this._stimRespPairsAndPriorities.entrySet()){
          PairedAssociateFastSlow.this._experiment.setStimulusResponsePriority(
            stimulusResponseAndPriority.getKey(),
            stimulusResponseAndPriority.getValue(), 
            true
          );
        }
        
        /**********************************************/
        /***** Setup Presentation Speed Variables *****/
        /**********************************************/
        
        String presentationSpeed = PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(_experimentCondition - 1, 1).toString();
        switch(presentationSpeed){
          case PairedAssociateFastSlow.FAST_PRESENTATION_CONDITION:
            PairedAssociateFastSlow.this._experiment.setPresentationTime((Integer)PairedAssociateFastSlow.this._fastPresentationTime.getValue());
            PairedAssociateFastSlow.this._experiment.setInterItemTime((Integer)PairedAssociateFastSlow.this._fastInterItemTime.getValue());
            PairedAssociateFastSlow.this._experiment.setInterTrialTime((Integer)PairedAssociateFastSlow.this._fastInterTrialTime.getValue());
            break;
          case PairedAssociateFastSlow.SLOW_PRESENTATION_CONDITION:
            PairedAssociateFastSlow.this._experiment.setPresentationTime((Integer)PairedAssociateFastSlow.this._slowPresentationTime.getValue());
            PairedAssociateFastSlow.this._experiment.setInterItemTime((Integer)PairedAssociateFastSlow.this._slowInterItemTime.getValue());
            PairedAssociateFastSlow.this._experiment.setInterTrialTime((Integer)PairedAssociateFastSlow.this._slowInterTrialTime.getValue());
            break;
        }
        
        /**********************************/
        /***** Set Auditory Loop Size *****/
        /**********************************/
        
        PairedAssociateFastSlow.this._experiment.setAuditoryLoopMaxSize( Integer.valueOf(PairedAssociateFastSlow.this._experimentConditionsTable.getValueAt(_experimentCondition - 1, 3).toString()) );
        
        /***********************************/
        /***** Setup Model Fit Objects *****/
        /***********************************/
        
        //Create regression objects for the percentage correct and cumulative 
        //errors model fit can be calculated for the experiment condition.
        SimpleRegression percentageCorrectRegression = new SimpleRegression();
        SimpleRegression cumulativeErrorsRegression = new SimpleRegression();
        
        //Create a data structure to store the cumulative errors for each 
        //stimulus-response pair over the trials in the experiment condition.
        //This is not required for percentage correct data since this data is 
        //added on a per-trial basis.
        Map<ListPattern, Double> cumulativeErrorsData = new HashMap<>();
        
        /**********************************/
        /***** Experiment Trials Loop *****/
        /**********************************/
        
        for(int trialNumber = 1; trialNumber <= PairedAssociateFastSlow.this._humanPercentageCorrectData.get(presentationSpeed).size(); trialNumber++){
          
          //Run the trial.
          PairedAssociateFastSlow.this._experiment.runTrial(false);
          
          /***************************************/
          /***** Calculate Cumulative Errors *****/
          /***************************************/
          
          int cumulativeErrors = 0;
          for(Entry<ListPattern, Integer> stimuliAndError : PairedAssociateFastSlow.this._experiment.getErrors().get(trialNumber-1).entrySet()){
            cumulativeErrors += stimuliAndError.getValue();
            
            //If this isn't the first trial, the stimulus will already exist in
            //the cumulativeErrorsData data structure so get
            //the current value for this stimulus (the cumulative total of 
            //errors for this stimulus) and add its value in this trial to it).
            if(cumulativeErrorsData.containsKey(stimuliAndError.getKey())){
              cumulativeErrorsData.put(stimuliAndError.getKey(),
                cumulativeErrorsData.get(stimuliAndError.getKey()) + stimuliAndError.getValue().doubleValue()
              );
            }
            //Otherwise, this is the first trial so add a new key (the stimulus)
            //and set its initial value to the current error value.
            else{
              cumulativeErrorsData.put(stimuliAndError.getKey(), stimuliAndError.getValue().doubleValue());
            }
          }
          
          /****************************************/
          /***** Calculate Percentage Correct *****/
          /****************************************/
          
          //Casting "cumulativeErrors" to double will promote denominator to 
          //double too.
          double modelPercentageCorrect = 100 - (
            ( ((double)cumulativeErrors) / PairedAssociateFastSlow.this._stimRespPairs.size() ) 
            * 100 
          ); 
          
          PairedAssociateFastSlow.this._modelPercentageCorrectData.add(modelPercentageCorrect);
          
          /*************************************************/
          /***** Add Percentage Correct Model Fit Data *****/
          /*************************************************/
          
          Double humanPercentageCorrect = PairedAssociateFastSlow.this._humanPercentageCorrectData.get(presentationSpeed).get(trialNumber - 1);
          percentageCorrectRegression.addData(humanPercentageCorrect, modelPercentageCorrect);
        }
        
        /***********************************************/
        /***** Add Cumulative Error Model Fit Data *****/
        /***********************************************/
        
        PairedAssociateFastSlow.this._modelCumulativeErrorsData.add(cumulativeErrorsData);
        for(Entry<ListPattern, Double> modelStimulusAndCumulativeError : cumulativeErrorsData.entrySet()){
          for(Entry<ListPattern, Double> humanStimulusAndCumulativeError : PairedAssociateFastSlow.this._humanCumulativeErrorsData.get(presentationSpeed).entrySet()){
            if(humanStimulusAndCumulativeError.getKey().toString().equals(modelStimulusAndCumulativeError.getKey().toString())){
              cumulativeErrorsRegression.addData(
                humanStimulusAndCumulativeError.getValue(), 
                modelStimulusAndCumulativeError.getValue()
              );
            }
          }
        }
          
        /***************************************************/
        /***** Calculate and Publish Model Fit Results *****/
        /***************************************************/
        
        PairedAssociateFastSlow.this._percentageCorrectRSquares.add(percentageCorrectRegression.getRSquare());
        PairedAssociateFastSlow.this._percentageCorrectRootMeanSquaredErrors.add(Math.sqrt(percentageCorrectRegression.getMeanSquareError()));
        PairedAssociateFastSlow.this._cumulativeErrorsRSquares.add(cumulativeErrorsRegression.getRSquare());
        PairedAssociateFastSlow.this._cumulativeErrorRootMeanSquaredErrors.add(Math.sqrt(cumulativeErrorsRegression.getMeanSquareError()));
        
        /********************/
        /***** Clean Up *****/
        /********************/
        
        _model.clear();
        PairedAssociateFastSlow.this.updateExperimentsProcessedLabel();
      }

      return null;
    }
  
    @Override
    public void done() {
      
      //Since this method will be called if the "StopExperimentAction" is 
      //invoked, the following code should only be run once, when all experiment 
      //conditions have been processed.  Otherwise, the code in the 
      //"StopExperimentAction" should be run.
      if(PairedAssociateFastSlow.this._experimentCondition > PairedAssociateFastSlow.this._experimentConditionsTotal ){
        _model.unfreeze();
        _model.setRecordHistory(this._originalRecordKeepingSetting);
        PairedAssociateFastSlow.this._exportDataButton.setEnabled(true);
        PairedAssociateFastSlow.this._restartButton.setEnabled(true);
        PairedAssociateFastSlow.this._stopButton.setEnabled(false);

        PairedAssociateFastSlow.this._fastInterItemTime.setEnabled(true);
        PairedAssociateFastSlow.this._fastInterTrialTime.setEnabled(true);
        PairedAssociateFastSlow.this._fastPresentationTime.setEnabled(true);
        PairedAssociateFastSlow.this._slowInterItemTime.setEnabled(true);
        PairedAssociateFastSlow.this._slowInterTrialTime.setEnabled(true);
        PairedAssociateFastSlow.this._slowPresentationTime.setEnabled(true);
        PairedAssociateFastSlow.this._shell.setCursor(null);

        PairedAssociateFastSlow.this.updateModelOutputTables();
      }
    }
  }
  
  /**
   * Reads in experiment data specified by the user.
   */
  class ReadExperimentData extends SwingWorker<Void, Void>{
    private final Map<String, List<Double>> _percentageCorrectData = new HashMap();
    private final Map<String, Map<ListPattern, Double>> _cumulativeErrorData = new HashMap();
    private final Map<PairedPattern, Integer> _stimulusResponsePairsAndPriorities = new LinkedHashMap<>();
    
    @Override
    protected Void doInBackground() throws Exception {
      
      if(!PairedAssociateFastSlow.this._readInSuccessful){
      
        //Validate the input data to be used using the corresponding schema.
        String experimentSchemaDirectory = ".." + File.separator + "scripted-experiment-inputs" + File.separator + "PairedAssociateFastSlow";
        JFileChooser fileChooser = new JFileChooser(experimentSchemaDirectory);
        fileChooser.setDialogTitle("Select Input Data");
        int resultOfFileSelect = fileChooser.showOpenDialog(PairedAssociateFastSlow.this._experimentInterface);
        if(resultOfFileSelect == JFileChooser.APPROVE_OPTION){
          File inputFile = fileChooser.getSelectedFile();

          String experimentInputDataSchema = experimentSchemaDirectory + File.separator + "schema.xsd";
          if(InputOutput.validateXmlInputData(PairedAssociateFastSlow.this._shell, inputFile.getAbsolutePath(), experimentInputDataSchema)){

            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            InputStream in = new FileInputStream(inputFile);
            XMLEventReader eventReader = inputFactory.createXMLEventReader(in);

            while(eventReader.hasNext()){
              XMLEvent event = eventReader.nextEvent();

              if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();

                if(elementName.equalsIgnoreCase("stimulus-response-pair")){

                  boolean stimulusResponsePairEndTagEncountered = false;
                  PairedPattern pair = null;
                  Integer auditoryLoopPriority = null;

                  while(!stimulusResponsePairEndTagEncountered){

                    event = eventReader.nextEvent();
                    if(event.isStartElement()){

                      String startElementName = event.asStartElement().getName().getLocalPart();
                      if(startElementName.equals("auditory-loop-priority")){
                        event = eventReader.nextEvent();
                        auditoryLoopPriority = Integer.valueOf(event.asCharacters().getData());
                      }
                      else if(startElementName.equals("pair")){
                        event = eventReader.nextEvent();
                        pair = InputOutput.generatePairedPattern(event.asCharacters().getData(), false);
                      }

                      if(pair != null && auditoryLoopPriority != null){
                        this._stimulusResponsePairsAndPriorities.put(pair, auditoryLoopPriority);
                      }
                    }
                    else if(event.isEndElement()){
                      if(event.asEndElement().getName().getLocalPart().equalsIgnoreCase("stimulus-response-pair")){
                        stimulusResponsePairEndTagEncountered = true;
                      }
                    }
                  }

                }

                if(elementName.equalsIgnoreCase("fast-presentation") || elementName.equalsIgnoreCase("slow-presentation")){

                  String presentationSpeed = PairedAssociateFastSlow.FAST_PRESENTATION_CONDITION;
                  if(elementName.equalsIgnoreCase("slow-presentation")){
                    presentationSpeed = PairedAssociateFastSlow.SLOW_PRESENTATION_CONDITION;
                  }

                  boolean presentationEndTagEncountered = false;
                  while(!presentationEndTagEncountered){

                    event = eventReader.nextEvent();
                    if(event.isStartElement()){

                      String startElementName = event.asStartElement().getName().getLocalPart();
                      if(startElementName.equalsIgnoreCase("percentage-correct")){

                        this._percentageCorrectData.put(presentationSpeed, new ArrayList<>());

                        boolean percentageCorrectEndTagEncountered = false;
                        while(!percentageCorrectEndTagEncountered){

                          event = eventReader.nextEvent();
                          if(event.isStartElement()){
                            if(event.asStartElement().getName().getLocalPart().equalsIgnoreCase("percentage-correct-data")){
                              event = eventReader.nextEvent();
                              this._percentageCorrectData.get(presentationSpeed).add(Double.valueOf(event.asCharacters().getData()));
                            }
                          }
                          else if(event.isEndElement()){
                            if(event.asEndElement().getName().getLocalPart().equalsIgnoreCase("percentage-correct")){
                              percentageCorrectEndTagEncountered = true;
                            }
                          }

                        }
                      }

                      if(startElementName.equals("cumulative-errors")){
                        this._cumulativeErrorData.put(presentationSpeed, new HashMap<>());

                        boolean cumulativeErrorsEndTagEncountered = false;
                        while(!cumulativeErrorsEndTagEncountered){

                          event = eventReader.nextEvent();
                          if(event.isStartElement()){
                            if(event.asStartElement().getName().getLocalPart().equalsIgnoreCase("cumulative-errors-data")){

                              boolean cumulativeErrorsDataEndTagEncountered = false;
                              ListPattern stimulus = null;
                              Double cumulativeError = null;

                              while(!cumulativeErrorsDataEndTagEncountered){

                                event = eventReader.nextEvent();
                                if(event.isStartElement()){
                                  if(event.asStartElement().getName().getLocalPart().equalsIgnoreCase("pair")){
                                    event = eventReader.nextEvent();
                                    stimulus = InputOutput.generatePairedPattern(event.asCharacters().getData(), false).getFirst();
                                  }
                                  else if(event.asStartElement().getName().getLocalPart().equalsIgnoreCase("cumulative-error")){
                                    event = eventReader.nextEvent();
                                    cumulativeError = Double.valueOf(event.asCharacters().getData());
                                  }

                                  if(stimulus != null && cumulativeError != null){
                                    this._cumulativeErrorData.get(presentationSpeed).put(stimulus, cumulativeError);
                                  }
                                }
                                else if(event.isEndElement()){
                                  if(event.asEndElement().getName().getLocalPart().equalsIgnoreCase("cumulative-errors-data")){
                                    cumulativeErrorsDataEndTagEncountered = true;
                                  }
                                }
                              }
                            }
                          }
                          else if(event.isEndElement()){
                            if(event.asEndElement().getName().getLocalPart().equalsIgnoreCase("cumulative-errors")){
                              cumulativeErrorsEndTagEncountered = true;
                            }
                          }

                        }
                      }
                    }
                    if(event.isEndElement()){
                      if(event.asEndElement().getName().getLocalPart().equalsIgnoreCase(presentationSpeed + "-presentation")){
                        presentationEndTagEncountered = true;
                      }
                    }
                  }
                } //End fast presentation check
              } //End start element check
            }//End read loop
            
            /***********************************************/
            /***** Read In Dictionary and Letters Data *****/
            /***********************************************/

            LoadDataThread dictionaryRead = _shell.new LoadDataThread (_shell, "Location of Dictionary Data", false);
            dictionaryRead.doInBackground();

            LoadDataThread lettersRead = _shell.new LoadDataThread (_shell, "Location of Letters Data", false);
            lettersRead.doInBackground();

            EventQueue.invokeLater(()->{
              PairedAssociateFastSlow.this._dictionary = dictionaryRead.getItems();
              PairedAssociateFastSlow.this._letters = lettersRead.getItems();

              if(
                PairedAssociateFastSlow.this._dictionary != null && 
                !PairedAssociateFastSlow.this._dictionary.isEmpty() &&
                PairedAssociateFastSlow.this._letters != null && 
                !PairedAssociateFastSlow.this._letters.isEmpty() 
              ){
                PairedAssociateFastSlow.this._readInSuccessful = true;
              }
            });
          }
        }
      }
      
      return null;
    }
    
    @Override
    protected void done(){
      if(PairedAssociateFastSlow.this._readInSuccessful){
        if(!PairedAssociateFastSlow.this._independentVariablesSetup){
          PairedAssociateFastSlow.this._stimRespPairsAndPriorities = this._stimulusResponsePairsAndPriorities;
          PairedAssociateFastSlow.this._humanPercentageCorrectData = this._percentageCorrectData;
          PairedAssociateFastSlow.this._humanCumulativeErrorsData = this._cumulativeErrorData;

          PairedAssociateFastSlow.this._maximumNumberOfTrials = Math.max(
            PairedAssociateFastSlow.this._humanPercentageCorrectData.get(PairedAssociateFastSlow.FAST_PRESENTATION_CONDITION).size(), 
            PairedAssociateFastSlow.this._humanPercentageCorrectData.get(PairedAssociateFastSlow.SLOW_PRESENTATION_CONDITION).size()
          );

          ArrayList<PairedPattern> stimulusResponsePairs = new ArrayList<>();
          for(PairedPattern stimulusResponsePair : this._stimulusResponsePairsAndPriorities.keySet()){
            stimulusResponsePairs.add(stimulusResponsePair);
          }
          PairedAssociateFastSlow.this._stimRespPairs = stimulusResponsePairs;

          //3 represents the 3 types of pre-learning (dictionary, letters or none).
          //2 represents the 2 speeds of presentation (fast and slow).
          //The maximum size of the auditory loop is the number of stimulus-response 
          //pairs to be used in the experiment (there is no reason for the auditory
          //loop to hold more stimulus-response pairs than what has been defined).
          PairedAssociateFastSlow.this._experimentConditionsTotal = 3 * 2 * PairedAssociateFastSlow.this._stimRespPairs.size();
          PairedAssociateFastSlow.this.updateExperimentsProcessedLabel();

          PairedAssociateFastSlow.this.instantiateResultsStorage();

          PairedAssociateFastSlow.this.updateExperimentInformationTables();
          PairedAssociateFastSlow.this.updateModelOutputTables();
          PairedAssociateFastSlow.this._shell.revalidate();

          PairedAssociateFastSlow.this._independentVariablesSetup = true;
        }

        /****************************/
        /***** Load Experiments *****/
        /****************************/

        //Disable fast/slow presentation independent variable GUI elements so
        //they remain consistent throughout experiments.
        PairedAssociateFastSlow.this._fastInterItemTime.setEnabled(false);
        PairedAssociateFastSlow.this._fastInterTrialTime.setEnabled(false);
        PairedAssociateFastSlow.this._fastPresentationTime.setEnabled(false);
        PairedAssociateFastSlow.this._slowInterItemTime.setEnabled(false);
        PairedAssociateFastSlow.this._slowInterTrialTime.setEnabled(false);
        PairedAssociateFastSlow.this._slowPresentationTime.setEnabled(false);

        //Disable all buttons except the "Stop" button.
        PairedAssociateFastSlow.this._restartButton.setEnabled(false);
        PairedAssociateFastSlow.this._stopButton.setEnabled(true);
        PairedAssociateFastSlow.this._exportDataButton.setEnabled(false);
        PairedAssociateFastSlow.this._runButton.setEnabled(false);

        //Set this variable to true since it will have been set to false if 
        //the "StopExperimentAction" has been invoked.  Consequently, without 
        //this reset, remaining experiments will not run even though all 
        //experiment conditions have not yet been processed.
        PairedAssociateFastSlow.this._runExperiments = true;
        
        Task experiment = new Task();
        experiment.execute();
      }
    }
  }
}
