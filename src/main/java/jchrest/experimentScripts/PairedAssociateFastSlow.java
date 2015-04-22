package jchrest.experimentScripts;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JEditorPane;
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
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
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
 * Script to run the fast/slow paired associate presentation experiments used by 
 * Dmitri Bennett in his third year dissertation at the University of Liverpool
 * (2015).
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class PairedAssociateFastSlow {
  
  private final Shell _shell;
  private final Chrest _model;
  private final JPanel _experimentInterface;
  private PairedAssociateExperiment _experiment;
  
  private int _experimentNumber = 0;
  private int _numberExperimentConditions = 0;
  
  //Used to control whether experiments should continue or stop.  Switched to
  //false by the "stop" button.
  private boolean _runExperiments = true;
  
  private final JLabel _experimentsProcessedLabel = new JLabel("");
    
  private final JSpinner _fastPresentationTime;
  private final JSpinner _fastInterItemTime;
  private final JSpinner _fastInterTrialTime;
  
  private final JSpinner _slowPresentationTime;
  private final JSpinner _slowInterItemTime;
  private final JSpinner _slowInterTrialTime;
  
  private DefaultTableModel _stimulusResponsePriorityTableModel;
  
  private JButton _exportDataButton;
  private JButton _restartButton;
  private JButton _stopButton;
  private JButton _runButton;
  
  private List<ListPattern> _dictionary = new ArrayList<>();
  private List<ListPattern> _letters = new ArrayList<>();
  private Map<PairedPattern, Integer> _stimRespPairsAndPriorities = new HashMap<>(); 
  
  //Whilst this may look like duplication (since stimulus-response pairs are 
  //already stored in the Map data structure for stimulus-response pairs and 
  //priorities above), it allows for easy retrieval of stimulus-response pairs
  //when constructing data tables.
  private List<PairedPattern> _stimRespPairs = new ArrayList<>();
  
  private JTable _experimentConditionsTable;
  
  private Map<String, List<Double>> _humanPercentageCorrectData;
  private JTable _humanPercentageCorrectDataTable;
  private Map<String, Map<ListPattern, Double>> _humanSerialPositionData;
  private JTable _humanSerialPositionDataTable;
  private List<Double> _modelPercentageCorrectData;
  private JTable _modelPercentageCorrectDataTable;  
  private List<Map<ListPattern, Double>> _modelSerialPositionData;
  private JTable _modelSerialPositionDataTable;
  private List<Double> _percentageCorrectRSquares;
  private List<Double> _percentageCorrectRootMeanSquaredErrors;
  private JTable _percentageCorrectRSquareDataTable;
  private List<Double> _serialPositionRSquares;
  private List<Double> _cumulativeErrorRootMeanSquaredErrors;
  private JTable _serialPositionRSquareDataTable;
    
  public PairedAssociateFastSlow(Shell shell){
    this._shell = shell;
    this._model = this._shell.getModel();
      
    this._experimentInterface = new JPanel();
    this._experimentInterface.setLayout(new GridLayout(1, 1));
    
    this._fastPresentationTime = new JSpinner(new SpinnerNumberModel (2000, 0, Integer.MAX_VALUE, 1));
    this._fastInterItemTime = new JSpinner(new SpinnerNumberModel (3000, 0, Integer.MAX_VALUE, 1));
    this._fastInterTrialTime = new JSpinner(new SpinnerNumberModel (15000, 0, Integer.MAX_VALUE, 1));
    
    this._slowPresentationTime = new JSpinner(new SpinnerNumberModel (2500, 0, Integer.MAX_VALUE, 1));
    this._slowInterItemTime = new JSpinner(new SpinnerNumberModel (3500, 0, Integer.MAX_VALUE, 1));
    this._slowInterTrialTime = new JSpinner(new SpinnerNumberModel (15000, 0, Integer.MAX_VALUE, 1));
    
    ReadExperimentData readExperimentDataThread = new ReadExperimentData();
    readExperimentDataThread.execute();
  }
  
  private void createHumanPercentageCorrectDataTable(){
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        //Its not unreasonable to expect that in some cases, the number of 
        //trials performed in the fast and slow human experiments is not equal.
        //So, return the maximum number of trials after comparing the number of
        //both.
        return Math.max(
          PairedAssociateFastSlow.this._humanPercentageCorrectData.get("fast").size(),
          PairedAssociateFastSlow.this._humanPercentageCorrectData.get("slow").size()
        );
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
        else if(column == 1 && row < PairedAssociateFastSlow.this._humanPercentageCorrectData.get("fast").size()) {
          value = String.valueOf(PairedAssociateFastSlow.this._humanPercentageCorrectData.get("fast").get(row));
        }
        else{
          if(row < PairedAssociateFastSlow.this._humanPercentageCorrectData.get("slow").size()){
            value = String.valueOf(PairedAssociateFastSlow.this._humanPercentageCorrectData.get("slow").get(row));
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
  
  private void createHumanSerialPositionDataTable(){
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
          
          //No idea why but if you try to access the Double value directly, null
          //is returned.  The loop below seems to circumvent this problem.
          String value = "";
          ListPattern first = PairedAssociateFastSlow.this._stimRespPairs.get(row).getFirst();
          for(Entry<ListPattern, Double> stimulusError : PairedAssociateFastSlow.this._humanSerialPositionData.get("fast").entrySet()){
            if(stimulusError.getKey().equals(first)){
              value = stimulusError.getValue().toString();
            }
          }
          return value;
        }
        else{
          
          //No idea why but if you try to access the Double value directly, null
          //is returned.  The loop below seems to circumvent this problem.
          String value = "";
          ListPattern first = PairedAssociateFastSlow.this._stimRespPairs.get(row).getFirst();
          for(Entry<ListPattern, Double> stimulusError : PairedAssociateFastSlow.this._humanSerialPositionData.get("slow").entrySet()){
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
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._humanSerialPositionDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._humanSerialPositionDataTable = new JTable (tm);
    PairedAssociateFastSlow.this._humanSerialPositionDataTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._humanSerialPositionDataTable);
  }
  
  private void createModelPercentageCorrectDataTable(){
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        
        //Total number of experiment conditions for the model multipled by the
        //maximum number of trails it needs to perform.
        return PairedAssociateFastSlow.this._numberExperimentConditions * 
          Math.max(
            PairedAssociateFastSlow.this._humanPercentageCorrectData.get("fast").size(), 
            PairedAssociateFastSlow.this._humanPercentageCorrectData.get("slow").size()
          );
      }
      
      @Override
      public int getColumnCount () {
        return 3; 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        if(column == 0){
          int maxNumberTrials = Math.max(
            PairedAssociateFastSlow.this._humanPercentageCorrectData.get("fast").size(), 
            PairedAssociateFastSlow.this._humanPercentageCorrectData.get("slow").size()
          );
          
          row++;
          
          int integer = row / maxNumberTrials;
          int fraction = row % maxNumberTrials;
          
          if(fraction != 0){
            return integer + 1;
          }
          else{
            return integer;
          }
        }
        else if (column == 1) {
          
          //Want the last digit (or 2 if 10).
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
  
  private void createModelSerialPositionDataTable(){
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        
        //Total number of experiment conditions for the model multipled by the
        //number of stimulus-response pairs in the experiment.
        return PairedAssociateFastSlow.this._numberExperimentConditions * 
          PairedAssociateFastSlow.this._stimRespPairs.size();
      }
      
      @Override
      public int getColumnCount () {
        return 3; 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        if(column == 0){
          int numberStimRespPairs = PairedAssociateFastSlow.this._stimRespPairs.size();
          
          row++;
          
          int integer = row / numberStimRespPairs;
          int fraction = row % numberStimRespPairs;
          
          if(fraction != 0){
            return integer + 1;
          }
          else{
            return integer;
          }
        }
        else if (column == 1) {
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
          if(experimentCondition < PairedAssociateFastSlow.this._modelSerialPositionData.size()){
            ListPattern stimulus = PairedAssociateFastSlow.this._stimRespPairs.get(PairedAssociateFastSlow.this.getStimulusResponsePairIndexForExperimentNumber(row + 1)).getFirst();
            value = String.valueOf(PairedAssociateFastSlow.this._modelSerialPositionData.get(experimentCondition).get(stimulus));
          }
          
          return value;
        }
      }
      
      @Override
      public String getColumnName (int column) {
        if(column == 0){
          return "Experiment Condition";
        } else if (column == 1) {
          return "Stimulus";
        } else {
          return "Result";
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._modelSerialPositionDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._modelSerialPositionDataTable = new JTable (tm);
    PairedAssociateFastSlow.this._modelSerialPositionDataTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._modelSerialPositionDataTable);
  }
  
  private void createRSquarePercentageCorrectDataTable(){
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        
        //Total number of experiment conditions.
        return PairedAssociateFastSlow.this._numberExperimentConditions;
      }
      
      @Override
      public int getColumnCount () {
        return 3; 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        if(column == 0){
          return row + 1;
        }
        else if(column == 1){
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
        } else if(column == 1){
          return "<html>R<sup>2</sup></html>";
        } else {
          return "RMSE";
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._percentageCorrectRSquareDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._percentageCorrectRSquareDataTable = new JTable (tm);
    PairedAssociateFastSlow.this._percentageCorrectRSquareDataTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._percentageCorrectRSquareDataTable);
  }
  
  private void createRSquareSerialPositionDataTable(){
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        
        return PairedAssociateFastSlow.this._numberExperimentConditions * 
          PairedAssociateFastSlow.this._stimRespPairs.size();
      }
      
      @Override
      public int getColumnCount () {
        return 4; 
      }
      
      @Override
      public Object getValueAt (int row, int column) {
        if(column == 0){
          int numberStimRespPairs = PairedAssociateFastSlow.this._stimRespPairs.size();
          
          row++;
          
          int integer = row / numberStimRespPairs;
          int fraction = row % numberStimRespPairs;
          
          if(fraction != 0){
            return integer + 1;
          }
          else{
            return integer;
          }
        }
        else if(column == 1){
          return PairedAssociateFastSlow.this._stimRespPairs.get(PairedAssociateFastSlow.this.getStimulusResponsePairIndexForExperimentNumber(row + 1)).getFirst().toString();
        }
        else if(column == 2){
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
          if(experimentCondition < PairedAssociateFastSlow.this._serialPositionRSquares.size()){
            value = String.format("%.2f", PairedAssociateFastSlow.this._serialPositionRSquares.get(experimentCondition));
          }
          
          return value;
        }
        else{
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
          if(experimentCondition < PairedAssociateFastSlow.this._cumulativeErrorRootMeanSquaredErrors.size()){
            value = String.format("%.2f", PairedAssociateFastSlow.this._cumulativeErrorRootMeanSquaredErrors.get(experimentCondition));
          }
          
          return value;
        }
      }
      
      @Override
      public String getColumnName (int column) {
        if(column == 0){
          return "Experiment Condition";
        } else if(column == 1){
          return "Stimulus";
        } else if(column == 2){
          return "<html>R<sup>2</sup></html>";
        } else {
          return "RMSE";
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._serialPositionRSquareDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._serialPositionRSquareDataTable = new JTable (tm);
    PairedAssociateFastSlow.this._serialPositionRSquareDataTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._serialPositionRSquareDataTable);
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
  
  private JPanel renderDataView(){
    this.createHumanPercentageCorrectDataTable();
    this.createHumanSerialPositionDataTable();
    this.createModelPercentageCorrectDataTable();
    this.createModelSerialPositionDataTable();
    this.createRSquarePercentageCorrectDataTable();
    this.createRSquareSerialPositionDataTable();
    
    JScrollPane humanPercentageCorrectDataTableScrollPane = new JScrollPane (this._humanPercentageCorrectDataTable);
    humanPercentageCorrectDataTableScrollPane.setBorder(new TitledBorder("Human % Correct"));
    
    JScrollPane humanSerialPositionDataTableScrollPane = new JScrollPane (this._humanSerialPositionDataTable);
    humanSerialPositionDataTableScrollPane.setBorder(new TitledBorder("Human Cumulative Errors"));
    
    JScrollPane modelPercentageCorrectDataTableScrollPane = new JScrollPane (this._modelPercentageCorrectDataTable);
    modelPercentageCorrectDataTableScrollPane.setBorder(new TitledBorder("Model % Correct"));
    
    JScrollPane modelSerialPositionDataTableScrollPane = new JScrollPane (this._modelSerialPositionDataTable);
    modelSerialPositionDataTableScrollPane.setBorder(new TitledBorder("Model Cumulative Errors"));
    
    JScrollPane percentageCorrectRSquareDataTableScrollPane = new JScrollPane (this._percentageCorrectRSquareDataTable);
    percentageCorrectRSquareDataTableScrollPane.setBorder(new TitledBorder("<html>% Correct Human/Model Fit</html>"));
    
    JScrollPane serialPositionRSquareDataTableScrollPane = new JScrollPane (this._serialPositionRSquareDataTable);
    serialPositionRSquareDataTableScrollPane.setBorder(new TitledBorder("<html>Cumulative Error Human/Model Fit</html>"));
    
    JPanel dataPanel = new JPanel();
    dataPanel.setLayout(new GridLayout(3, 2));
    dataPanel.setBorder(new TitledBorder("Data View"));
    dataPanel.add(humanPercentageCorrectDataTableScrollPane);
    dataPanel.add(humanSerialPositionDataTableScrollPane);
    dataPanel.add(modelPercentageCorrectDataTableScrollPane);
    dataPanel.add(modelSerialPositionDataTableScrollPane);
    dataPanel.add(percentageCorrectRSquareDataTableScrollPane);
    dataPanel.add(serialPositionRSquareDataTableScrollPane);
    
    return dataPanel;
  }
  
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
    jsp.setMinimumSize(new Dimension((int)(this._shell.getWidth() * 0.5), this._shell.getHeight()));
    jsp.setBorder(new TitledBorder("Experiment Description"));
    
    //Scroll the vertical sidebar position to the top.  Its at the bottom by 
    //default (no idea why).
    experimentInfo.setCaretPosition(0);
    return jsp;
  }
  
  private JScrollPane renderExperimentConditionsView(){
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }

      @Override
      public int getRowCount() {
        return PairedAssociateFastSlow.this._numberExperimentConditions;
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
          if(experimentCondition < PairedAssociateFastSlow.this._numberExperimentConditions/2){
            value = "Fast";
          } else {
            value = "Slow";
          }
        } else if(col == 2){
          int auditoryLoopSize = experimentCondition / PairedAssociateFastSlow.this._stimRespPairs.size();
          int fraction = experimentCondition % PairedAssociateFastSlow.this._stimRespPairs.size();
          if(fraction > 0){
            auditoryLoopSize++;
          }
          
          if(experimentCondition > PairedAssociateFastSlow.this._numberExperimentConditions/2){
            auditoryLoopSize -= 3;
          }
          
          value = String.valueOf(auditoryLoopSize);
        } else{
          int fraction = experimentCondition % 3;
          if(fraction == 1){
            value = "Dictionary";
          } else if(fraction == 2){
            value = "Letters";
          } else{
            value = "None";
          }
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
          return "Auditory Loop Size";
        } else {
          return "Pre-Learning Type";
        }
      }
      
      @Override
      public void fireTableStructureChanged() {
        super.fireTableStructureChanged ();
        JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._humanSerialPositionDataTable);
      }
    };
    
    PairedAssociateFastSlow.this._experimentConditionsTable = new JTable (tm);
    PairedAssociateFastSlow.this._experimentConditionsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(PairedAssociateFastSlow.this._experimentConditionsTable);
    
    JScrollPane experimentConditionsView = new JScrollPane(PairedAssociateFastSlow.this._experimentConditionsTable);
    experimentConditionsView.setBorder (new TitledBorder ("Experiment Conditions"));
    return experimentConditionsView;
  }
  
  private JScrollPane renderStimulusResponsePrioritiesView () {
    
    this._stimulusResponsePriorityTableModel = new DefaultTableModel(){
      
      @Override
      public boolean isCellEditable(int row, int column) {
        //Only the "Priority" column is editable.
        return column == 2;
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
        for(int i = 0; i < PairedAssociateFastSlow.this._stimulusResponsePriorityTableModel.getRowCount(); i++){
          if(i != tcl.getRow()){
            Integer priorityToCheck = Integer.valueOf((String)PairedAssociateFastSlow.this._stimulusResponsePriorityTableModel.getValueAt(i, 2));
            if(Objects.equals(priorityToCheck, newPriorityInteger)){
              PairedAssociateFastSlow.this._stimRespPairsAndPriorities.put( 
                PairedAssociateFastSlow.this.getStimulusResponsePairsArrayFromKeysInMap(PairedAssociateFastSlow.this._stimRespPairsAndPriorities).get(i), 
                oldPriorityInteger 
              );
              break;
            }
          }
        }
        
        //Now, set the new priority for P in the data model that feeds the table
        //model.
        PairedAssociateFastSlow.this._stimRespPairsAndPriorities.put( 
          PairedAssociateFastSlow.this.getStimulusResponsePairsArrayFromKeysInMap(PairedAssociateFastSlow.this._stimRespPairsAndPriorities).get(tcl.getRow()), 
          newPriorityInteger 
        );
        
        //Finally, update the table model so that the table in the GUI displays
        //the updated priorities.
        PairedAssociateFastSlow.this.populateStimulusResponsePriorityTableModel();
      }
    };
    TableCellListener tcl = new TableCellListener(stimulusResponsePriorityJTable, priorityReassignment);
    
    JScrollPane stimulusResponsePriorityView = new JScrollPane(stimulusResponsePriorityJTable);
    stimulusResponsePriorityView.setBorder (new TitledBorder ("Stimulus-Response Pairs and Priorities"));
    return stimulusResponsePriorityView;
  }
  
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
  
  public ArrayList<PairedPattern> getStimulusResponsePairsArrayFromKeysInMap(Map<PairedPattern,?> mapToProcess){
    ArrayList<PairedPattern> stimulusResponsePairs = new ArrayList<>();
    Iterator<PairedPattern> iterator = mapToProcess.keySet().iterator();
    while(iterator.hasNext()){
      stimulusResponsePairs.add(iterator.next());
    }
    
    return stimulusResponsePairs;
  }
  
  /**
   * Instantiates the data structures used to record results from the experiment.
   */
  public void instantiateResultsStorage(){
    PairedAssociateFastSlow.this._modelPercentageCorrectData = new ArrayList();
    PairedAssociateFastSlow.this._modelSerialPositionData = new ArrayList();
    PairedAssociateFastSlow.this._percentageCorrectRSquares = new ArrayList();
    PairedAssociateFastSlow.this._serialPositionRSquares = new ArrayList();
    PairedAssociateFastSlow.this._percentageCorrectRootMeanSquaredErrors = new ArrayList();
    PairedAssociateFastSlow.this._cumulativeErrorRootMeanSquaredErrors = new ArrayList();
  }
  
  private void populateStimulusResponsePriorityTableModel(){
    
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
  
  private void updateDataTables(){
    ((AbstractTableModel)PairedAssociateFastSlow.this._modelPercentageCorrectDataTable.getModel()).fireTableStructureChanged();
    ((AbstractTableModel)PairedAssociateFastSlow.this._modelSerialPositionDataTable.getModel()).fireTableStructureChanged();
    ((AbstractTableModel)PairedAssociateFastSlow.this._percentageCorrectRSquareDataTable.getModel()).fireTableStructureChanged();
    ((AbstractTableModel)PairedAssociateFastSlow.this._serialPositionRSquareDataTable.getModel()).fireTableStructureChanged();
  }
  
  private void updateExperimentsProcessedLabel(){
    String text = "";
    if(this._numberExperimentConditions > 0){
      text = this._experimentNumber + "/" + this._numberExperimentConditions;
    }
    this._experimentsProcessedLabel.setText(text);
  }

  public Shell getShell() {
    return _shell;
  }
  
  public List<ListPattern> getDictionary(){
    return this._dictionary;
  }
  
  public JButton getExportDataButton(){
    return this._exportDataButton;
  }
  
  public List<ListPattern> getLetters(){
    return this._letters;
  }
  
  public List<PairedPattern> getStimRespPairs(){
    return this._stimRespPairs;
  }

  public JSpinner getFastPresentationTime() {
    return _fastPresentationTime;
  }

  public JSpinner getFastInterItemTime() {
    return _fastInterItemTime;
  }

  public JSpinner getFastInterTrialTime() {
    return _fastInterTrialTime;
  }

  public JSpinner getSlowPresentationTime() {
    return _slowPresentationTime;
  }

  public JSpinner getSlowInterItemTime() {
    return _slowInterItemTime;
  }

  public JSpinner getSlowInterTrialTime() {
    return _slowInterTrialTime;
  }

  public JButton getRestartButton(){
    return _restartButton;
  }
  
  public JButton getRunButton() {
    return _runButton;
  }
  
  public JButton getStopButton() {
    return _stopButton;
  }
  
  public void setDictionary(List<ListPattern> dictionary){
    this._dictionary = dictionary;
  }
  
  public void setLetters(List<ListPattern> letters){
   this._letters = letters; 
  }
  
  public void setStimRespPairs(List<PairedPattern> stimRespPairs){
    this._stimRespPairs = stimRespPairs;
  }
  
  public void setStimRespPairsAndPriorities(Map<PairedPattern, Integer> stimRespPairsAndPriorities){
    this._stimRespPairsAndPriorities = stimRespPairsAndPriorities;
  }
  
  public void setHumanPercentageCorrectData(Map<String, List<Double>> humanPercentageCorrectData) {
    this._humanPercentageCorrectData = humanPercentageCorrectData;
  }

  public void setHumanSerialPositionData(Map<String, Map<ListPattern, Double>> humanSerialPositionData) {
    this._humanSerialPositionData = humanSerialPositionData;
  }
  
  class ExportDataAction extends AbstractAction implements ActionListener {
    
    ExportDataAction () {
      super ("Export Model Fit Data as CSV");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      
      ArrayList<String> percentageCorrectRSquare = new ArrayList<>();
      ArrayList<String> cumulativeErrorRSquare = new ArrayList<>();
      
      percentageCorrectRSquare.add(ExportData.extractJTableDataAsCsv(PairedAssociateFastSlow.this._percentageCorrectRSquareDataTable));
      percentageCorrectRSquare.add("percentageCorrectRSquareData");
      percentageCorrectRSquare.add("csv");
      
      cumulativeErrorRSquare.add(ExportData.extractJTableDataAsCsv(PairedAssociateFastSlow.this._serialPositionRSquareDataTable));
      cumulativeErrorRSquare.add("cumulativeErrorRSquareData");
      cumulativeErrorRSquare.add("csv");
      
      ArrayList<ArrayList<String>> dataToSave = new ArrayList<>();
      dataToSave.add(percentageCorrectRSquare);
      dataToSave.add(cumulativeErrorRSquare);
      
      ExportData.saveFile(PairedAssociateFastSlow.this._shell, "CHREST-paired-associate-fast-slow-experiment-data", dataToSave);
    }
  }
  
  class RestartExperimentAction extends AbstractAction implements ActionListener {
    
    RestartExperimentAction(){
      super("Restart Experiment");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      PairedAssociateFastSlow.this._model.unfreeze();
      PairedAssociateFastSlow.this._model.clear();
      PairedAssociateFastSlow.this._experimentNumber = 0;
      PairedAssociateFastSlow.this._runExperiments = true;
      PairedAssociateFastSlow.this.instantiateResultsStorage();
      PairedAssociateFastSlow.this.updateDataTables();
      PairedAssociateFastSlow.this.getExportDataButton().setEnabled(false);
      PairedAssociateFastSlow.this.getRestartButton().setEnabled(false);
      PairedAssociateFastSlow.this.getRunButton().setEnabled(true);
      PairedAssociateFastSlow.this.updateExperimentsProcessedLabel();
    }
  }
  
  class RunExperimentAction extends AbstractAction implements ActionListener {
    
    RunExperimentAction(){
      super("Run Experiment");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      
      //Schedule these threads so one executes after another has completed.
      if(PairedAssociateFastSlow.this._experimentNumber == 0){
        LoadDataThread dictionaryRead = _shell.new LoadDataThread (_shell, "Location of Dictionary Data", false);
        dictionaryRead.doInBackground();

        LoadDataThread lettersRead = _shell.new LoadDataThread (_shell, "Location of Letters Data", false);
        lettersRead.doInBackground();

        PairedAssociateFastSlow.this.setDictionary(dictionaryRead.getItems());
        PairedAssociateFastSlow.this.setLetters(lettersRead.getItems());
      }
      
      if(
        PairedAssociateFastSlow.this.getDictionary() != null && 
        !PairedAssociateFastSlow.this.getDictionary().isEmpty() &&
        PairedAssociateFastSlow.this.getLetters() != null && 
        !PairedAssociateFastSlow.this.getLetters().isEmpty() 
      ){
        javax.swing.SwingUtilities.invokeLater(() -> {
          Task task = new Task();
          task.execute();
        });
      }
    }
  }
  
  class StopExperimentAction extends AbstractAction implements ActionListener {
    
    StopExperimentAction(){
      super("Stop Experiment");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      PairedAssociateFastSlow.this._runExperiments = false;
      PairedAssociateFastSlow.this.getRestartButton().setEnabled(true);
      PairedAssociateFastSlow.this.getRunButton().setEnabled(true);
      PairedAssociateFastSlow.this.getStopButton().setEnabled(false);
    }
  }
  
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

      //En/Disable experiment interaction devices.
      PairedAssociateFastSlow.this.getRestartButton().setEnabled(false);
      PairedAssociateFastSlow.this.getStopButton().setEnabled(true);
      PairedAssociateFastSlow.this.getFastInterItemTime().setEnabled(false);
      PairedAssociateFastSlow.this.getFastInterTrialTime().setEnabled(false);
      PairedAssociateFastSlow.this.getFastPresentationTime().setEnabled(false);
      PairedAssociateFastSlow.this.getSlowInterItemTime().setEnabled(false);
      PairedAssociateFastSlow.this.getSlowInterTrialTime().setEnabled(false);
      PairedAssociateFastSlow.this.getSlowPresentationTime().setEnabled(false);
      PairedAssociateFastSlow.this.getRunButton().setEnabled(false);

      //Set cursor to "busy" so the user knows that the script is busy.
      PairedAssociateFastSlow.this.getShell().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      
      //Don't update model visuals since this will slow the script to some 
      //degree and is superflous any way.
      _model.freeze();
      
      //Get the model's current history recording setting and turn off history
      //recording.
      this._originalRecordKeepingSetting = _model.canRecordHistory();
      _model.setRecordHistory(false);

      for(
        PairedAssociateFastSlow.this._experimentNumber = 1; 
        PairedAssociateFastSlow.this._experimentNumber <= PairedAssociateFastSlow.this._numberExperimentConditions && PairedAssociateFastSlow.this._runExperiments; 
        PairedAssociateFastSlow.this._experimentNumber++
      ){
        
        //Copy the experiment number so the copy can be used to determine 
        //independent variable setting without altering the actual experiment
        //number.
        int exptNum = PairedAssociateFastSlow.this._experimentNumber;

        //Pre-learning setting.  To determine what pre-learning should occur, 
        //divide the experiment number by 3 and retrieve the modulus.  Division
        //by 3 occurs because there are three types of pre-learning possible.
        int fraction = PairedAssociateFastSlow.this._experimentNumber % 3;
        switch(fraction){
          case 1:
            for(ListPattern word : PairedAssociateFastSlow.this.getDictionary()){
              _model.recogniseAndLearn(word);
            }
            break;
          case 2:
            for(ListPattern letter : PairedAssociateFastSlow.this.getLetters()){
              Node recognisedNode = _model.recogniseAndLearn(letter);
              while(!recognisedNode.getImage().equals(letter)){
                recognisedNode = _model.recogniseAndLearn(letter);
              }
            }
            break;
        }
        
        //Set-up the experiment after pre-learning since the experiment 
        //constructor resets the model's learning clock.  This clock will be 
        //higher than 0 due to pre-learning so the experiment will not be 
        //conducted properly (experiment time will need to be greater than the 
        //learning clock value before any learning of experiment stimuli-response
        //pairs occurs) and may cause out-of-memory errors.
        _experiment = new PairedAssociateExperiment(_model, PairedAssociateFastSlow.this.getStimRespPairs());
        
        //Set the auditory loop priorities in the experiment according to what 
        //has been specified in the interface.
        for(Entry<PairedPattern, Integer> stimulusResponseAndPriority : PairedAssociateFastSlow.this._stimRespPairsAndPriorities.entrySet()){
          _experiment.setStimulusResponsePriority(
            stimulusResponseAndPriority.getKey(),
            stimulusResponseAndPriority.getValue(), 
            true
          );
        }

        //Speed parameter setting.
        String presentationSpeed = "";
        if(PairedAssociateFastSlow.this._experimentNumber <= PairedAssociateFastSlow.this._numberExperimentConditions/2 ){
          _experiment.setPresentationTime((int)_fastPresentationTime.getModel().getValue());
          _experiment.setInterItemTime((int)_fastInterItemTime.getModel().getValue());
          _experiment.setInterTrialTime((int)_fastInterTrialTime.getModel().getValue());
          presentationSpeed = "fast";
        }
        else{
          exptNum = PairedAssociateFastSlow.this._experimentNumber - (PairedAssociateFastSlow.this._numberExperimentConditions/2);
          _experiment.setPresentationTime((int)_slowPresentationTime.getModel().getValue());
          _experiment.setInterItemTime((int)_slowInterItemTime.getModel().getValue());
          _experiment.setInterTrialTime((int)_slowInterTrialTime.getModel().getValue());
          presentationSpeed = "slow";
        }

        //Auditory loop size setting.  The auditory loop should only ever be as
        //large as the number of stimulus-response pairs in the experiment since
        //being able to hold more confers no benefits.
        int auditoryLoopSize = exptNum / PairedAssociateFastSlow.this._stimRespPairs.size();
        fraction = exptNum % PairedAssociateFastSlow.this._stimRespPairs.size();
        if(fraction > 0){
          auditoryLoopSize++;
        }
        _experiment.setAuditoryLoopMaxSize(auditoryLoopSize);
        
        /**********************************************************************/
        /**********************************************************************/
        /***************************** RUN TRIALS *****************************/
        /**********************************************************************/
        /**********************************************************************/
        
        //Create a new regression object so that human and model observations 
        //can be added after each trial and the R^2 value can be calculated for
        //the entire experiment.
        SimpleRegression percentageCorrectRegression = new SimpleRegression();
        SimpleRegression serialPositionRegression = new SimpleRegression();
        
        Map<ListPattern, Double> serialPositionsForModelOverExperiment = new HashMap<>();
        
        for(int trialNumber = 1; trialNumber <= PairedAssociateFastSlow.this._humanPercentageCorrectData.get(presentationSpeed).size(); trialNumber++){
          _experiment.runTrial(false);
          
          int totalErrors = 0;
          for(Entry<ListPattern, Integer> stimuliAndError : _experiment.getErrors().get(trialNumber-1).entrySet()){
            totalErrors += stimuliAndError.getValue();
            
            //If this isn't the first trial, the stimulus will already exist in
            //the serialPositionsForModelOverExperiment data structure so get
            //the current value for this stimulus (the cumulative total of 
            //errors for this stimulus) and add its value in this trial to it).
            if(serialPositionsForModelOverExperiment.containsKey(stimuliAndError.getKey())){
              serialPositionsForModelOverExperiment.put(
                stimuliAndError.getKey(),
                serialPositionsForModelOverExperiment.get(stimuliAndError.getKey()) + stimuliAndError.getValue().doubleValue()
              );
            }
            //Otherwise, this is the first trial so add a new key (the stimulus)
            //and set its initial value to the current error value.
            else{
              serialPositionsForModelOverExperiment.put(stimuliAndError.getKey(), stimuliAndError.getValue().doubleValue());
            }
          }
          
          //Casting "totalErrors" to double will promote denominator to 
          //double too.
          double modelPercentageCorrect = 100 - (
            ( ((double)totalErrors) / PairedAssociateFastSlow.this._stimRespPairs.size() ) 
            * 100 
          ); 
          
          PairedAssociateFastSlow.this._modelPercentageCorrectData.add(modelPercentageCorrect);
          
          //Get the percentage of correct responses given by the human in this
          //trial.
          Double humanPercentageCorrect = PairedAssociateFastSlow.this._humanPercentageCorrectData.get(presentationSpeed).get(trialNumber - 1);
          
          //Add the percentage correct observations to the percentage correct 
          //regression object.
          percentageCorrectRegression.addData(humanPercentageCorrect, modelPercentageCorrect);
        }
        
        //Add cumulative error data.
        PairedAssociateFastSlow.this._modelSerialPositionData.add(serialPositionsForModelOverExperiment);
        for(Entry<ListPattern, Double> modelStimulusAndCumulativeError : serialPositionsForModelOverExperiment.entrySet()){
          for(Entry<ListPattern, Double> humanStimulusAndCumulativeError : PairedAssociateFastSlow.this._humanSerialPositionData.get(presentationSpeed).entrySet()){
            
            if(humanStimulusAndCumulativeError.getKey().toString().equals(modelStimulusAndCumulativeError.getKey().toString())){

              serialPositionRegression.addData(
                humanStimulusAndCumulativeError.getValue(), 
                modelStimulusAndCumulativeError.getValue()
              );
            }
          }
        }
          
        //Now compute R^2 and RSME mean for percentage correct and serial 
        //position for this experiment.
        PairedAssociateFastSlow.this._percentageCorrectRSquares.add(percentageCorrectRegression.getRSquare());
        PairedAssociateFastSlow.this._percentageCorrectRootMeanSquaredErrors.add(Math.sqrt(percentageCorrectRegression.getMeanSquareError()));
        PairedAssociateFastSlow.this._serialPositionRSquares.add(serialPositionRegression.getRSquare());
        PairedAssociateFastSlow.this._cumulativeErrorRootMeanSquaredErrors.add(Math.sqrt(serialPositionRegression.getMeanSquareError()));
        
        _model.clear();
        PairedAssociateFastSlow.this.updateExperimentsProcessedLabel();
      }

      return null;
    }
  
    @Override
    public void done() {
      _model.unfreeze();
      _model.setRecordHistory(this._originalRecordKeepingSetting);
      PairedAssociateFastSlow.this.getExportDataButton().setEnabled(true);
      PairedAssociateFastSlow.this.getRestartButton().setEnabled(true);
      PairedAssociateFastSlow.this.getStopButton().setEnabled(false);
      
      PairedAssociateFastSlow.this.getFastInterItemTime().setEnabled(true);
      PairedAssociateFastSlow.this.getFastInterTrialTime().setEnabled(true);
      PairedAssociateFastSlow.this.getFastPresentationTime().setEnabled(true);
      PairedAssociateFastSlow.this.getSlowInterItemTime().setEnabled(true);
      PairedAssociateFastSlow.this.getSlowInterTrialTime().setEnabled(true);
      PairedAssociateFastSlow.this.getSlowPresentationTime().setEnabled(true);
      PairedAssociateFastSlow.this.getShell().setCursor(null);
      
      PairedAssociateFastSlow.this.updateDataTables();
    }
      
    //USED TO UPDATE GUI - SHOULD UPDATE RESULTS TABLE.  NEED TO DETERMINE WHAT
    //PARAMETER TYPE IS TO BE PASSED IN AND THEN CHANGE PARAMETER FOR THREAD
    //CLASS DECLARATION ACCORDINGLY.
//    @Override
//    protected void process (PairedPattern value) {
//      
//    }
  }
  
  class ReadExperimentData extends SwingWorker<Void, Void>{
    private final Map<String, List<Double>> _percentageCorrectData = new HashMap();
    private final Map<String, Map<ListPattern, Double>> _serialPositionData = new HashMap();
    private final Map<PairedPattern, Integer> _stimulusResponsePairsAndPriorities = new LinkedHashMap<>();
    
    @Override
    protected Void doInBackground() throws Exception {
      //TODO: Validate XML document using InputOutput.validiteXmlInputData with 
      //before continuing.
      
      XMLInputFactory inputFactory = XMLInputFactory.newInstance();
      InputStream in = new FileInputStream(".." + File.separator + "scripted-experiment-inputs" + File.separator + "PairedAssociateFastSlow" + File.separator + "input.xml");
      XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
      
      while(eventReader.hasNext()){
        XMLEvent event = eventReader.nextEvent();
          
        if (event.isStartElement()) {
          StartElement startElement = event.asStartElement();
          String elementName = startElement.getName().getLocalPart();
          
          if(elementName.equalsIgnoreCase("stimulus-repsonse-pair")){
            Attribute auditoryLoopPriority = startElement.getAttributeByName(new QName("auditory-loop-priority"));
            event = eventReader.nextEvent();
            this._stimulusResponsePairsAndPriorities.put(
              InputOutput.generatePairedPattern(event.asCharacters().getData(), false),
              Integer.valueOf(auditoryLoopPriority.getValue())
            );
          }
          
          if(elementName.equalsIgnoreCase("fast-presentation") || elementName.equalsIgnoreCase("slow-presentation")){
            
            String presentationSpeed = "";
            if(elementName.equalsIgnoreCase("fast-presentation")){
              presentationSpeed = "fast";
            }
            else if(elementName.equalsIgnoreCase("slow-presentation")){
              presentationSpeed = "slow";
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
                      if(event.asStartElement().getName().getLocalPart().equalsIgnoreCase("data")){
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
                
                if(startElementName.equals("serial-position-errors")){
                  this._serialPositionData.put(presentationSpeed, new HashMap<>());
                  
                  boolean serialPositionEndTagEncountered = false;
                  while(!serialPositionEndTagEncountered){
                    
                    event = eventReader.nextEvent();
                    if(event.isStartElement()){
                      if(event.asStartElement().getName().getLocalPart().equalsIgnoreCase("data")){
                        Attribute idAttribute = event.asStartElement().getAttributeByName(new QName("id"));
                        String stimulusResponseString = idAttribute.getValue();
                        ListPattern key = InputOutput.generatePairedPattern(stimulusResponseString, false).getFirst();
                        event = eventReader.nextEvent();
                        this._serialPositionData.get(presentationSpeed).put(key, Double.valueOf(event.asCharacters().getData()));
                      }
                    }
                    else if(event.isEndElement()){
                      if(event.asEndElement().getName().getLocalPart().equalsIgnoreCase("serial-position-errors")){
                        serialPositionEndTagEncountered = true;
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
      
      return null;
    }
    
    @Override
    protected void done(){
      PairedAssociateFastSlow.this.setStimRespPairsAndPriorities(this._stimulusResponsePairsAndPriorities);
      PairedAssociateFastSlow.this.setHumanPercentageCorrectData(this._percentageCorrectData);
      PairedAssociateFastSlow.this.setHumanSerialPositionData(this._serialPositionData);
      
      ArrayList<PairedPattern> stimulusResponsePairs = new ArrayList<>();
      for(PairedPattern stimulusResponsePair : this._stimulusResponsePairsAndPriorities.keySet()){
        stimulusResponsePairs.add(stimulusResponsePair);
      }
      PairedAssociateFastSlow.this.setStimRespPairs(stimulusResponsePairs);
      
      //3 represents the 3 types of pre-learning (dictionary, letters or none).
      //2 represents the 2 speeds of presentation (fast and slow).
      //The maximum size of the auditory loop is the number of stimulus-response 
      //pairs to be used in the experiment (there is no reason for the auditory
      //loop to hold more stimulus-response pairs than what has been defined).
      PairedAssociateFastSlow.this._numberExperimentConditions = 3 * 2 * PairedAssociateFastSlow.this._stimRespPairs.size();
      PairedAssociateFastSlow.this.updateExperimentsProcessedLabel();
      
      PairedAssociateFastSlow.this.instantiateResultsStorage();
      
      JSplitPane jsp = new JSplitPane (
        JSplitPane.HORIZONTAL_SPLIT, 
        PairedAssociateFastSlow.this.renderInputView (), 
        PairedAssociateFastSlow.this.renderDataView ()
      );
      PairedAssociateFastSlow.this._experimentInterface.add(jsp);

      PairedAssociateFastSlow.this._shell.setContentPane(PairedAssociateFastSlow.this._experimentInterface);
      PairedAssociateFastSlow.this._shell.revalidate();
    }
    
  }
}
