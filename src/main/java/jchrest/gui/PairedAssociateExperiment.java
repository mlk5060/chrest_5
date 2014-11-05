// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import jchrest.architecture.Chrest;
import jchrest.lib.ListPattern;
import jchrest.lib.PairedPattern;
import jchrest.lib.Pattern;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * This panel provides an interface for running paired associate
 * experiments.
 * 
 * @author Peter C. R. Lane
 */
public class PairedAssociateExperiment extends JPanel {
  private final Chrest _model;
  private final List<PairedPattern> _patterns;
  private List<List<ListPattern>> _responses;
  private List<Integer> _numberPatternErrors;
  private int _trialNumber;

  public PairedAssociateExperiment (Chrest model, List<PairedPattern> patterns) {
    super ();
    
    _model = model;
    _patterns = patterns;
    _numberPatternErrors = new ArrayList<Integer>();
    _trialNumber = 0;

    setLayout (new GridLayout (1, 1));
    JSplitPane jsp = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, createRunExperimentView (), createExperimentView ());
    jsp.setOneTouchExpandable (true);

    add (jsp);
  }

  /**
   * Convert a list of ListPatterns into a list of stimulus-response pairs.
   */
  public static List<PairedPattern> makePairs (List<ListPattern> patterns) {
    List<PairedPattern> pairs = new ArrayList<PairedPattern> ();
    for (int i = 1; i < patterns.size (); ++i) {
      pairs.add (new PairedPattern (patterns.get(i-1), patterns.get(i)));
    }

    return pairs;
  }

  private JPanel createListView () {
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

  class RestartAction extends AbstractAction implements ActionListener {
    
    RestartAction () {
      super ("Restart");
    }

    @Override
    public void actionPerformed (ActionEvent e) {
      _model.clear ();
      _responses.clear ();
      _exptClock = 0;
      _numberPatternErrors.clear();
      _trialNumber = 0;

      updateControls ();
    }
  }

  

  class RunTrialAction extends AbstractAction implements ActionListener {
    
    RunTrialAction () {
      super ("Run Trial");
      _exptClock = 0;
    }

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

    private void collectResponses () {
      List<ListPattern> responses = new ArrayList<ListPattern> ();
      for (PairedPattern pair : _patterns) {
        ListPattern response = _model.associatePattern (pair.getFirst ());
        if (response != null) {
          responses.add (response);
        } else {
          responses.add (Pattern.makeVisualList (new String[]{"NONE"}));
        }
      }
      _responses.add (responses);
    }

    @Override
    public void actionPerformed (ActionEvent e) {
      _model.freeze (); // save all gui updates to the end
      _trialNumber += 1;
      collectResponses ();
      for (PairedPattern pair : preparePatterns ()) {
        _model.associateAndLearn (pair.getFirst (), pair.getSecond (), _exptClock);
        _exptClock += ((SpinnerNumberModel)_interItemTime.getModel()).getNumber().intValue ();
        _exptClock += ((SpinnerNumberModel)_presentationTime.getModel()).getNumber().intValue ();
      }
      _exptClock += ((SpinnerNumberModel)_endTrialTime.getModel()).getNumber().intValue ();
      updateControls ();
    }
  }

  private int _exptClock;
  private JLabel _experimentTimeLabel;
  private JSpinner _endTrialTime;
  private JSpinner _interItemTime;
  private JSpinner _presentationTime;
  private JCheckBox _randomOrder;
  private JTable _trialsTable;
  private JTable _errorsTable;
  private JScrollBar _trialsHorizontalBar;

  private void updateControls () {
    ((AbstractTableModel)_trialsTable.getModel()).fireTableStructureChanged ();
    ((AbstractTableModel)_errorsTable.getModel()).fireTableStructureChanged ();
    _experimentTimeLabel.setText ("" + _exptClock);
    _model.unfreeze ();
  }

  private JPanel createControls () {
    _experimentTimeLabel = new JLabel ("0");
    _endTrialTime = new JSpinner (new SpinnerNumberModel (2000, 1, 50000, 1));
    _interItemTime = new JSpinner (new SpinnerNumberModel (2000, 1, 50000, 1));
    _presentationTime = new JSpinner (new SpinnerNumberModel (2000, 1, 50000, 1));
    _randomOrder = new JCheckBox ("Random order");
    _randomOrder.setToolTipText ("Set this to pass pairs to model in a random order");
    JButton restart = new JButton (new RestartAction ());
    restart.setToolTipText ("Reset the experiment and clear the model");
    JButton runTrial = new JButton (new RunTrialAction ());
    runTrial.setToolTipText ("Pass each stimulus-response pair once against the model");

    JPanel controls = new JPanel ();
    controls.setLayout (new GridLayout (6, 2, 10, 3));
    controls.add (new JLabel ("Experiment time (ms)", SwingConstants.RIGHT));
    controls.add (_experimentTimeLabel);
    controls.add (new JLabel ("End trial time (ms)", SwingConstants.RIGHT));
    controls.add (_endTrialTime);
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

  private JPanel createRunExperimentView () {
    JPanel panel = new JPanel ();
    panel.setLayout (new BorderLayout ());
    panel.add (createListView ());
    panel.add (createControls (), BorderLayout.SOUTH);

    return panel;
  }
  
  private void createTrialsTable () {
    
    TableModel tm = new AbstractTableModel () {
      
      @Override
      public int getRowCount () {
        
        // include a row for the number of errors
        return 1 + _patterns.size ();
      }
      
      @Override
      public int getColumnCount () {
        
        // include two columns for the stimulus and target response and one
        // for the total number of errors for each stimulus.
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
            return "" + getTotalTrialErrors (column-2);
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
      
      /**
       * Computes the total number of errors for a trial.
       */
      private int getTotalTrialErrors (int trial) {
        int errors = 0;
        for (int i = 0, n = _patterns.size (); i < n; ++i) {
          ListPattern target = _responses.get(trial).get(i).clone ();
          ListPattern response = _patterns.get(i).getSecond().clone ();
          target.setNotFinished ();
          response.setNotFinished ();
          if (response.equals (target)) {
          } else {
            errors += 1;
          }
        }
        return errors;
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
        
        //Only ever "stimulus" and the total number of errors for "stimulus"
        //in this table.
        return 2;
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
        if(columnIndex == 0){
          return _patterns.get(rowIndex).getFirst();
        }
        else {
          
          return getTotalPatternErrors(_trialNumber, rowIndex);
        }
      }
      
      @Override
      public String getColumnName (int columnIndex) {
        if (columnIndex == 0) {
          return "Stimulus";
        } else {
          return "Total # Errors";
        }
      }
      
      /**
       * Returns the total number of errors for each presented pattern row over 
       * all trials and updates these values accordingly.
       */
      private int getTotalPatternErrors (int trial, int patternIndex){
        if(trial == 0){
          _numberPatternErrors.add(0);
        }
        else{
          trial -= 1;
          ListPattern target = _responses.get(trial).get(patternIndex).clone ();
          ListPattern response = _patterns.get(patternIndex).getSecond().clone ();
          target.setNotFinished ();
          response.setNotFinished ();
          if (!response.equals (target)) {
            _numberPatternErrors.set(patternIndex, _numberPatternErrors.get(patternIndex) + 1);
          }
        }
        
        return _numberPatternErrors.get(patternIndex);
      }
      
    };
            
    _errorsTable = new JTable (tm);
    _errorsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);       
  }

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
    
    experimentView.add(trialsScrollPane);
    experimentView.add(errorsScrollPane);

    return experimentView;
  }
}

