// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import jchrest.architecture.Chrest;
import jchrest.lib.ListPattern;
import jchrest.lib.PairedPattern;
import jchrest.lib.Pattern;

/**
 * This panel provides an interface for running paired associate
 * experiments.
 * 
 * @author Peter C. R. Lane
 * @author Martyn Lloyd-Kelly
 */
public class PairedAssociateExperiment extends JPanel {
  
  private final Chrest _model;
  private final List<PairedPattern> _patterns;
  private List<List<ListPattern>> _responses;
  private List<Integer> _trialErrors;
  private List<HashMap<ListPattern, Integer>> _numberPatternErrors;
  private int _trialNumber;
  
  private int _exptClock;
  private JLabel _experimentTimeLabel;
  private JSpinner _presentationTime;
  private JSpinner _interItemTime;
  private JCheckBox _randomOrder;
  private JTable _trialsTable;
  private JScrollBar _trialsHorizontalBar;
  private JTable _errorsTable;
  private JScrollBar _errorsHorizontalBar;

  public PairedAssociateExperiment (Chrest model, List<PairedPattern> patterns) {
    super ();
    
    _model = model;
    _patterns = patterns;
    _trialNumber = 0;
    instantiateErrorStorage();

    setLayout (new GridLayout (1, 1));
    JSplitPane jsp = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, createExperimentControlView (), createExperimentView ());
    jsp.setOneTouchExpandable (true);

    add (jsp);
  }
  
  public final void instantiateErrorStorage(){
    
    this._numberPatternErrors = new ArrayList<HashMap<ListPattern, Integer>>();
    this._numberPatternErrors.add( new HashMap<ListPattern, Integer>() );
    for(int i = 0; i < _patterns.size(); i++){
      this._numberPatternErrors.get(0).put(_patterns.get(i).getFirst(), 0);
    }
    
    this._trialErrors = new ArrayList<Integer>();
    this._trialErrors.add(0);
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
   * Creates a panel containing all stimulus-response pattern pairs to be used 
   * in the experiment (pairs created by {@link #makePairs(java.util.List)} 
   * method.
   * 
   * @return 
   */
  private JPanel createListPanel () {
    JPanel panel = new JPanel ();
    panel.setBorder (new TitledBorder ("Stimulus-response pairs"));
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
   * Creates a panel containing all independent variables that can be set in the
   * experiment and useful experiment information.
   * 
   * @return 
   */
  private JPanel createControlPanel () {
    _experimentTimeLabel = new JLabel ("0");
    
    _presentationTime = new JSpinner (new SpinnerNumberModel (2000, 1, 50000, 1));
    _presentationTime.setToolTipText("The length of time each stimuli is presented for on each trial");
    
    _interItemTime = new JSpinner (new SpinnerNumberModel (2000, 1, 50000, 1));
    _interItemTime.setToolTipText("The length of time between presentation of each stimuli on each trial");
    
    _randomOrder = new JCheckBox ("Random order");
    _randomOrder.setToolTipText ("Check to present stimuli in a random order");
    
    JButton restart = new JButton (new RestartAction() );
    restart.setToolTipText ("Reset the experiment and clear the model");
    
    JButton runTrial = new JButton (new RunTrialAction() );
    runTrial.setToolTipText ("Pass each stimulus-response pair once against the model");

    JPanel controls = new JPanel ();
    controls.setLayout (new GridLayout (5, 2, 10, 3));
    
    controls.add (new JLabel ("Experiment time (ms)", SwingConstants.RIGHT));
    controls.add (_experimentTimeLabel);
    
    controls.add (new JLabel ("Presentation time (ms)", SwingConstants.RIGHT));
    controls.add (_presentationTime);
    
    controls.add (new JLabel ("Inter item time (ms)", SwingConstants.RIGHT));
    controls.add (_interItemTime);
    
    controls.add (_randomOrder);
    
    controls.add (restart);
    
    controls.add (new JLabel (""));
    
    controls.add (runTrial);

    return controls;
  }
  
  /**
   * Creates a panel composed of the stimulus-target pattern list panel (created 
   * by {@link #createListPanel()}) and the experiment control panel 
   * {@link #createControlPanel()}.  
   * 
   * The stimulus-target pattern list panel is aligned above the experimental 
   * control panel.
   * 
   * @return 
   */
  private JPanel createExperimentControlView () {
    JPanel panel = new JPanel ();
    panel.setLayout (new BorderLayout ());
    panel.add (createListPanel ());
    panel.add (createControlPanel (), BorderLayout.SOUTH);

    return panel;
  }
  
  /**
   * Updates various information in the experiment control panel created by
   * {@link #createControlPanel()}.
   */
  private void updateExperimentControls () {
    _experimentTimeLabel.setText ("" + _exptClock);
  }
  
  /**
   * Creates the experiment panel where the progress of the experiment is
   * displayed.  This panel consists of a "trials" table  where the progress
   * of CHREST's learning is displayed over trials run and an "errors" table
   * where the errors made by CHREST for each trial are displayed.
   * 
   * 
   * @return 
   */
  private JPanel createExperimentView () {
    JPanel experimentView = new JPanel ();
    experimentView.setBorder (new TitledBorder ("Experiment"));
    experimentView.setLayout (new GridLayout (2, 1));
    _responses = new ArrayList<List<ListPattern>> ();
    
    createTrialsTable ();
    createErrorsTable();
    
    JScrollPane trialsScrollPane = new JScrollPane (_trialsTable);
    _trialsHorizontalBar = trialsScrollPane.getHorizontalScrollBar ();
    
    JScrollPane errorsScrollPane = new JScrollPane (_errorsTable);
    _errorsHorizontalBar = errorsScrollPane.getHorizontalScrollBar ();
    
    experimentView.add(trialsScrollPane);
    experimentView.add(errorsScrollPane);

    return experimentView;
  }
  
  private void createTrialsTable () {
    
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        
        //Include a row for the total number of errors for each trial as well
        //as each trial's stimuli pattern.
        return 1 + _patterns.size ();
      }
      
      @Override
      public int getColumnCount () {
        
        //Include two columns for the "Stimulus" and "Target" columns.
        return 2 + _responses.size (); 
      }
      
      @Override
      public Object getValueAt (int row, int column) {      
        if (column == 0) {
          if (row == _patterns.size ()) {
            return "";
          } else {
            return _patterns.get(row).getFirst ();
          }
        } else if (column == 1) {
          if (row == _patterns.size ()) {
            return "Total Trial Errors:";
          } else {
            return _patterns.get(row).getSecond ();
          }
        }
        else {
          if (row == _patterns.size ()) {
            return _trialErrors.get(column - 1);
          } else {
            return _responses.get(column-2).get(row).toString ();
          }
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
        _trialsHorizontalBar.setValue (_trialsHorizontalBar.getMaximum ());
      }
    };
    
    _trialsTable = new JTable (tm);
    _trialsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
  }
  
  private void createErrorsTable(){
    TableModel tm = new AbstractTableModel () {

      @Override
      public int getRowCount() {
        
        //Should only have rows for the patterns declared.
        return _patterns.size();
      }

      @Override
      public int getColumnCount() {
        return 1 + _responses.size();
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
        _errorsHorizontalBar.setValue (_errorsHorizontalBar.getMaximum ());
      }
    };
    
    _errorsTable = new JTable (tm);
    _errorsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
  }

  class RestartAction extends AbstractAction implements ActionListener {
    
    RestartAction () {
      super ("Restart");
    }

    @Override
    public void actionPerformed (ActionEvent e) {
      _model.clear ();
      _responses.clear ();
      _exptClock = 0;
      _trialNumber = 0;
      _trialErrors = new ArrayList<Integer>();
      instantiateErrorStorage();
      updateExperimentControls ();
    }
  }

  class RunTrialAction extends AbstractAction implements ActionListener {
    
    RunTrialAction () {
      super ("Run Trial");
    }
    
    @Override
    public void actionPerformed (ActionEvent e) {
      _model.freeze (); // save all gui updates to the end
      _trialNumber += 1;
      associateAndLearnPatterns();
      test();
      ((AbstractTableModel)_trialsTable.getModel()).fireTableStructureChanged();
      ((AbstractTableModel)_errorsTable.getModel()).fireTableStructureChanged();
      updateExperimentControls();
      _model.unfreeze();
    }

    /**
     * Prepares a list of the stimuli patterns to be presented according to
     * whether the stimuli patterns should be randomly presented or not.
     * @return 
     */
    private List<PairedPattern> preparePatterns () {
      List<PairedPattern> patterns = new ArrayList<PairedPattern> ();
      java.util.Random gen = new java.util.Random ();
      for (PairedPattern pattern : _patterns) {
        if (_randomOrder.isSelected ()) {
          patterns.add (gen.nextInt (patterns.size () + 1), pattern);
        } else {
          patterns.add (pattern);
        }
      }

      return patterns;
    }
    
    /**
     * Attempts to associate the second pattern with the first pattern in a pair 
     * of patterns if they are both learned or learns unlearned patterns every 
     * millisecond until the presentation time specified is reached.
     */
    private void associateAndLearnPatterns(){
      for (PairedPattern pair : preparePatterns()) {
        int presentationFinishTime = ((SpinnerNumberModel)_presentationTime.getModel()).getNumber().intValue() + _exptClock;
        
        while(_exptClock <= presentationFinishTime){
          _model.associateAndLearn (pair.getFirst (), pair.getSecond (), _exptClock);
          _exptClock += 1;
        }
        
        _exptClock += ((SpinnerNumberModel)_interItemTime.getModel()).getNumber().intValue ();
      }
    }

    /**
     * Asks the CHREST model to retrieve the pattern that is currently 
     * associated with each stimuli pattern in LTM and records any errors.
     */
    private void test () {
      
      List<ListPattern> responses = new ArrayList<ListPattern> ();
      int totalErrorsInTrial = 0;
      _numberPatternErrors.add( new HashMap<ListPattern, Integer>() );
      
      for (PairedPattern pair : _patterns) {
        ListPattern response = _model.associatedPattern (pair.getFirst ());
        
        if (response != null) {
          responses.add (response);
        } else {
          responses.add (Pattern.makeVisualList (new String[]{"NONE"}));
        }
        
        int previousNumberPatternErrors = _numberPatternErrors.get(_trialNumber - 1).get(pair.getFirst());
        ListPattern latestResponse = responses.get(responses.size() - 1);
        latestResponse.setFinished();
        
        if( latestResponse.matches(pair.getSecond()) ){
           _numberPatternErrors.get(_trialNumber).put( pair.getFirst(), previousNumberPatternErrors );
        }
        else{
          _numberPatternErrors.get(_trialNumber).put( pair.getFirst(), (previousNumberPatternErrors + 1) );
          totalErrorsInTrial += 1;
        }
      }
      
      _trialErrors.add(totalErrorsInTrial);
      _responses.add (responses);
    }    
  }
}

