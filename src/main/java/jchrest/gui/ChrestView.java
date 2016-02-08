// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import jchrest.architecture.Chrest;
import jchrest.lib.FileUtilities;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Observable;
import java.util.Observer;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ChrestView extends JFrame implements Observer {
  private Shell _shell;
  private Chrest _model;
  private ChrestLtmView _ltmView;
  private ChrestStmView _stmView;
  private ChrestTimeView _timeView;
  
  //Required so that views can be updated correctly.
  private String _experimentToVisualise = "";
  private Integer _timeToVisualise;
  
  //Required so that experiment being visualised can have its name displayed 
  //acccordingly in the view.
  private TitledBorder _titleBorder;
  private final String _selectExperimentText = this._experimentToVisualise;
  
  //Required so that 'Experiment' sub-menu can be updated as CHREST is placed 
  //into new experiments.
  private JMenu _viewMenu; 
  private JMenu _experimentNames;
  
  //Required so that toolbar can be added when an experiment is selected for 
  //viewing and so that values entered into text field can be retrieved.
  private JToolBar _stateAtTimeToolbar;
  private JTextField _stateAtTimeTextField; 

  public ChrestView (Chrest model, int timeToVisualise) {
    this (new Shell (model), model, timeToVisualise);
  }

  public ChrestView (Shell shell, Chrest model, int timeToVisualise) {
    super ("CHREST Model View");
    
    this._shell = shell;
    this._model = model;
    
    //Add this instance to the list of model observers so that updates to the
    //model can be drawn.
    this._model.addObserver (this);
    
    //Set the experiment and time to visualise to be the current experiment and 
    //the time passed to the constructor.
    this._experimentToVisualise = this._model.getCurrentExperimentName();
    this._timeToVisualise = timeToVisualise;
    
    //Create a clone of LTM at the current time so that all nodes required for 
    //S/LTM rendering are available to the relevant methods in ChrestLtmView and
    //ChrestStmView.
//    this._model.cloneLtm(this._timeToVisualise);
    this._timeView = new ChrestTimeView (this._timeToVisualise);
    this._ltmView = new ChrestLtmView (this._model, this._timeToVisualise, this._experimentToVisualise);
    this._stmView = new ChrestStmView (this._model, this._timeToVisualise);    

    //Add a window listener so that close-window events can be caught and
    //additional processing performed.
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent ev) { 
        closeView (); 
      }
    });

    //Layout shell components accordingly.
    createMenuBar ();
    JPanel leftSide = new JPanel ();
    leftSide.setLayout (new BorderLayout ());
    leftSide.add (_timeView, BorderLayout.NORTH);
    leftSide.add (_stmView, BorderLayout.CENTER);
    
    JSplitPane jsp = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT, leftSide, _ltmView);
    jsp.setOneTouchExpandable (true);
    this._titleBorder = new TitledBorder(setTitle());
    this.setTitleColour();
    jsp.setBorder(this._titleBorder);
    jsp.setDividerLocation(150 + jsp.getInsets().left); //Ensures the left side of the split pane is big enough to see.
    
    setLayout (new BorderLayout ());
    add (jsp, BorderLayout.CENTER);
    setSize(550, 550);
    setVisible (true);
    add (this.createStateAtTimeToolbar(this._timeToVisualise), BorderLayout.SOUTH);
    
    //Prompt LTM to draw itself
    _ltmView.setStandardDisplay ();
  }
  
  /**
   * Sets the title of the view.
   * 
   * @return A message requesting that the user load some data into CHREST if no
   * title (experiment) has been set (run) yet or the current experiment name.
   */
  private String setTitle(){
    return this._experimentToVisualise.isEmpty() ? this._selectExperimentText : this._experimentToVisualise;
  }
  
  /**
   * Sets the colour of the CHREST view title text to:
   * <ul>
   *  <li>
   *    Red if no experiment has been selected
   *  </li>
   *  <li>
   *    Orange if a pre-experiment has been selected
   *  </li>
   *  <li>
   *    Green if an experiment has been selected.
   *  </li>
   * </ul>
   */
  private void setTitleColour(){
    String titleText = this._titleBorder.getTitle();
    
    if(titleText.equals(this._selectExperimentText)){
      this._titleBorder.setTitleColor(Color.RED);
    }
    else if(titleText.startsWith(this._model.getPreExperimentPrepend())){
      this._titleBorder.setTitleColor(Color.ORANGE);
    }
    else{
      this._titleBorder.setTitleColor(new Color(34, 139, 34));
    }
  }

  private void createMenuBar () {
    JMenuBar mb = new JMenuBar ();
    mb.add (createViewMenu ());
    setJMenuBar (mb);
  }

  public void saveLongTermMemory (File file) {
    _ltmView.saveLongTermMemory (file);
  }

  class SaveLtmAction extends AbstractAction implements ActionListener {
    private ChrestView _parent;

    public SaveLtmAction (ChrestView parent) {
      super ("Save LTM", new ImageIcon (Shell.class.getResource ("icons/SaveAs16.gif")));

      _parent = parent;
    }

    public void actionPerformed (ActionEvent e) {
      File file = FileUtilities.getSaveFilename (_shell);
      if (file != null) {
        _parent.saveLongTermMemory (file);
      }
    }
  }

  class CloseAction extends AbstractAction implements ActionListener {
    private ChrestView _view;

    public CloseAction (ChrestView view) {
      super ("Close");
      _view = view;
    }

    public void actionPerformed (ActionEvent e) {
      _view.closeView ();
    }
  }

  private JMenu createViewMenu () {
    this._viewMenu = new JMenu ("View");
    this._viewMenu.setMnemonic (KeyEvent.VK_V);
    this._viewMenu.add (this.getExperimentNamesSubMenu());
    this._viewMenu.add (new SaveLtmAction (this)).setMnemonic (KeyEvent.VK_S);
    this._viewMenu.add (new CloseAction (this)).setMnemonic (KeyEvent.VK_C);
    return this._viewMenu;
  }
  
  private JMenu getExperimentNamesSubMenu(){
    this._experimentNames = new JMenu ("Experiment");
    for(String experimentName : _model.getExperimentsLocatedInNames()){
      this._experimentNames.add(new LoadExperimentViewAction(experimentName));
    }
    return this._experimentNames;
  }
  
  class LoadExperimentViewAction extends AbstractAction implements ActionListener{
    
    //Stores the experiment name for this action.
    private final String _experimentName;
    
    public LoadExperimentViewAction (String experimentName) {
      super (experimentName);
      this._experimentName = experimentName;
    }
    
    /**
     * Whenever an experiment is selected the following should occur:
     * - The name of the experiment should be set to the  enclosing class' 
     *   "_lastSelectedExperimentName" variable so that time filters can be 
     *   applied correctly.
     * - LTM should be cloned so that historical S/LTM can be drawn without 
     *   editing current LTM contents.
     * - Current state of LTM should be drawn.
     * - Current state of STM should be drawn.
     * - Time view should be re-drawn.
     * - State at time toolbar should be added to the view window so that user's
     *   can at the state of CHREST in the experiment selected at various 
     *   intervals.
     * 
     * @param e 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      ChrestView.this._experimentToVisualise = _experimentName;
      ChrestView.this._timeToVisualise = ChrestView.this._model.getMaximumTimeForExperiment(_experimentName);
      ChrestView.this.updateTitleAndView();
    }
  }
  
  /**
   * Creates the "State at Time" toolbar for the view window and instantiates 
   * enclosed text field with a value.
   * 
   * @param stateAtTimeValue
   * @return 
   */
  private JToolBar createStateAtTimeToolbar(int stateAtTimeValue){
    this._stateAtTimeToolbar = new JToolBar ();
    this._stateAtTimeToolbar.add(new JLabel ("<html><b>State at Time:</b></html>"));
    this._stateAtTimeToolbar.add(createStateAtTimeTextField(stateAtTimeValue));
    return this._stateAtTimeToolbar;
  }
  
  /**
   * Creates the "State at Time" text field.
   * 
   * TODO: Would be nice if the width of the field was dymanamic since extremely
   * large numbers may be displayed.  This could also be a JSpinner since the
   * user may not be aware of the maximum time that can be specified.
   * 
   * @param stateAtTimeValue
   * @return 
   */
  private JTextField createStateAtTimeTextField(int stateAtTimeValue){
    this._timeToVisualise = stateAtTimeValue;
    this._stateAtTimeTextField = new JTextField(String.valueOf(stateAtTimeValue));
    Dimension d = this._stateAtTimeTextField.getPreferredSize();
    d.width = 120;
    this._stateAtTimeTextField.setPreferredSize(d);
    this._stateAtTimeTextField.setToolTipText("Displays STM and LTM states as they were according to the time entered in the experiment selected (press 'ENTER' to apply filter).");
    this._stateAtTimeTextField.addKeyListener(new ApplyTimeFilter());
    return this._stateAtTimeTextField;
  }
  
  class ApplyTimeFilter implements KeyListener {

    @Override
    public void keyReleased(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_ENTER){
        String stateAtTimeTextFieldCurrentContents = ((JTextField)e.getComponent()).getText();
        
        if(stateAtTimeTextFieldCurrentContents.matches("[0-9]+")){
          _timeToVisualise = Integer.valueOf( stateAtTimeTextFieldCurrentContents );
          _ltmView.update (_timeToVisualise, true, ChrestView.this._experimentToVisualise);
          _stmView.update (_timeToVisualise);
          _timeView.update (ChrestView.this._model.getMaximumTimeForExperiment(ChrestView.this._experimentToVisualise));
        }
        else{
          JOptionPane.showMessageDialog(_shell,
          "Please enter positive numbers (0-9) only",
          "State at Time Error",
           JOptionPane.ERROR_MESSAGE
          );
        }
      }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}
    
  }

  /** 
   * Implement the observable interface, and update the view whenever 
   * the underlying model has changed.
   */
  @Override
  public void update(Observable o, Object arg) {
    int positionOfExperimentsSubMenuInViewMenu = 0;
    Component[] viewMenuComponents = this._viewMenu.getMenuComponents();
    while(positionOfExperimentsSubMenuInViewMenu < viewMenuComponents.length){
      if(viewMenuComponents[positionOfExperimentsSubMenuInViewMenu].equals(this._experimentNames)){
        break;
      }
    }
    this._viewMenu.remove(this._experimentNames);
    this._viewMenu.add(this.getExperimentNamesSubMenu(), positionOfExperimentsSubMenuInViewMenu);
    
    this._experimentToVisualise = this._model.getCurrentExperimentName();
    this._timeToVisualise = this._model.getMaximumTimeForExperiment(this._experimentToVisualise);
    this.updateTitleAndView();
  }
  
  private void updateTitleAndView(){
    ChrestView.this._titleBorder.setTitle(this._experimentToVisualise);
    ChrestView.this.setTitleColour();
    ChrestView.this.repaint();
//    _model.cloneLtm(this._timeToVisualise);
    _ltmView.update (this._timeToVisualise, false, this._experimentToVisualise);
    _stmView.update (this._timeToVisualise);
    _timeView.update (this._timeToVisualise);
    this._stateAtTimeTextField.setText(this._timeToVisualise.toString());
  }

  /**
   * When closing the view, make sure the observer is detached from the model
   * and that cloned LTM's are cleared.
   */
  private void closeView () {
//    _model.clearClonedLtm();
    _model.deleteObserver (this);
    setVisible (false);
    dispose ();
  }
}

