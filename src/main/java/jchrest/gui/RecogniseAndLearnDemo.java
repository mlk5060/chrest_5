// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import jchrest.architecture.Chrest;
import jchrest.lib.ListPattern;
import jchrest.lib.Modality;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * This panel provides an interface for a simple demonstration of recognising 
 * and learning the given set of panels.
 *
 * @author Peter C. R. Lane
 */
class RecogniseAndLearnDemo extends JPanel {
  private Chrest _model;
  private List<ListPattern> _patterns;
  private int _exptClock;

  public RecogniseAndLearnDemo (Chrest model, List<ListPattern> patterns) {
    _model = model;
    // for simplicity, turn off the more confusing aspects of learning
    _model.setRho (1.0f);
    _model.setCanCreateSemanticLinks (false);
    _model.setCanCreateTemplates (false);
    _model.setClocks(0);
    _patterns = patterns;
    _exptClock = 0;

    setLayout (new BorderLayout ());
    add (constructPatternList (), BorderLayout.CENTER);
    add (constructFeedbackPanel (), BorderLayout.SOUTH);
    add (constructButtons (), BorderLayout.EAST);
  }

  private JList _patternList;
  private JPanel constructPatternList () {
    JPanel panel = new JPanel ();
    panel.setBorder (new TitledBorder ("Patterns"));
    panel.setLayout (new GridLayout (1, 1));

    _patternList = new JList (_patterns.toArray ());
    _patternList.setSelectedIndex (0);

    panel.add (new JScrollPane (_patternList));

    return panel;
  }

  abstract class PatternAction extends AbstractAction implements ActionListener {
    PatternAction (String label) {
      super (label);
    }

    boolean isSelected () {
      return (_patternList.getSelectedIndex () != -1);
    }

    ListPattern selectedPattern () {
      ListPattern pattern = null;
      if (isSelected ()) {
        pattern = (ListPattern)_patternList.getSelectedValue ();
        pattern.setModality ((Modality)_modeButton.getSelectedItem ());
      }
      return pattern;
    }

    public abstract void actionPerformed (ActionEvent e);
  }

  class LearnPatternAction extends PatternAction {
    LearnPatternAction () {
      super ("Learn");
    }

    public void actionPerformed (ActionEvent e) {
      if (isSelected ()) {
        _model.setEngagedInExperiment();
        _model.recogniseAndLearn (selectedPattern(), _exptClock);
        _feedback.setText ("Learning " + selectedPattern().toString ());
      }
    }
  }

  class LearnAllPatternAction extends AbstractAction implements ActionListener {
    LearnAllPatternAction () {
      super ("Learn all"); 
    }

    public void actionPerformed (ActionEvent e) {
      _model.setEngagedInExperiment();
      for (ListPattern pattern : _patterns) {
        ListPattern patternInCorrectMode = pattern.clone ();
        patternInCorrectMode.setModality ((Modality)_modeButton.getSelectedItem ());
        _model.recogniseAndLearn (patternInCorrectMode, _exptClock);
      }
      _feedback.setText ("Learnt all patterns");
    }
  }

  class RecognisePatternAction extends PatternAction {
    RecognisePatternAction () {
      super ("Recognise");
    }

    public void actionPerformed (ActionEvent e) {
      if (isSelected ()) {
        _model.setEngagedInExperiment();
        _feedback.setText ("Recalled " + 
            _model.recognise (selectedPattern(), _exptClock, true).toString () +
            " for " +
            selectedPattern().toString ());
      }
    }
  }

  private JComboBox _modeButton;
  private int _currentMode;

  private Box constructButtons () {
    Box buttons = Box.createVerticalBox ();
    _modeButton = new JComboBox (Modality.values ()); 
    JButton learnButton = new JButton (new LearnPatternAction ());
    JButton learnAllButton = new JButton (new LearnAllPatternAction ());
    JButton recogniseButton = new JButton (new RecognisePatternAction ());
    
    learnButton.setToolTipText ("Train model on currently selected pattern");
    learnAllButton.setToolTipText ("Train model on all patterns");
    recogniseButton.setToolTipText ("Recall currently selected pattern from model");

    _modeButton.setMaximumSize (recogniseButton.getPreferredSize ());
    _modeButton.setAlignmentX(0.0f); // correct the alignment of buttons and combobox
    learnButton.setMaximumSize (recogniseButton.getPreferredSize ());
    learnAllButton.setMaximumSize (recogniseButton.getPreferredSize ());

    buttons.add (Box.createGlue ());
    buttons.add (new JLabel ("Pattern type:"));
    buttons.add (_modeButton);
    buttons.add (Box.createVerticalStrut (20)); // add a small gap before buttons
    buttons.add (learnButton);
    buttons.add (learnAllButton);
    buttons.add (recogniseButton);
    buttons.add (Box.createGlue ());

    return buttons;
  }

  private JLabel _feedback;

  private JLabel constructFeedbackPanel () {
    _feedback = new JLabel ("FEEDBACK");
    _feedback.setFont (new Font ("Arial", Font.PLAIN, 18));
    _feedback.setBorder (new EmptyBorder (10, 50, 10, 50));
    return _feedback;
  }
}

