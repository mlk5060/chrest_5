// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import jchrest.gui.experiments.*;
import jchrest.architecture.Chrest;
import jchrest.lib.FileUtilities;
import jchrest.lib.ListPattern;
import jchrest.lib.PairedPattern;
import jchrest.lib.Scenes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultFormatter;
import jchrest.lib.InputOutput;
import jchrest.lib.Modality;
import jchrest.lib.PairedAssociateExperiment;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.statistics.*;

/**
 * The main panel for the Chrest shell.
 *
 * @author Peter C. R. Lane
 */
public class Shell extends JFrame implements Observer {
  private Chrest _model;
  private JMenu _dataMenu; //Required so that "Data" menu options can be disabled if the model is engaged in an experiment.
  private final List<List<String>> _executionHistory = new ArrayList<>();
  private ArrayList<String> _executionHistoryOperations = new ArrayList<>();
  private JComboBox _executionHistoryOperationsComboBox;
  private JSpinner _executionHistoryTimeFrom;
  private JSpinner _executionHistoryTimeTo;
  private JTable _executionHistoryTable;
  
  public Shell(){
    this(new Chrest(0, false));
  }
  
  public Shell (Chrest model) {
    super ("jCHREST");
    this._model = model;

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    createMenuBar (0);
    
    JLabel startupInfo = new JLabel (
      "<html>Load data by clicking on the 'Data' toolbar option.  Two types of data<br>"
        + "can be loaded:"
        + "<ul>"
        + " <li>Pre-experiment data: trains CHREST before undertaking an experiment.</li>"
        + " <li>Experiment data: loads an experiment for CHREST to undertake.</li>"
        + "</ul>"
        + "Note that only one set of experiment data can be used with CHREST at any<br>"
        + "time whereas multiple pre-experiment data files can be used.<br>"
        + "<br>"
        + "To reset CHREST and undertake a different experiment, select 'Model' then<br>"
        + "'Clear' from the toolbar.  Clearing CHREST will remove any pre-experiment<br>"
        + "data learned too."
        + "</html>"
    );
    
    startupInfo.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    setContentPane(startupInfo);

    setSize(800, 600);
    setLocationRelativeTo (null);
    setTheme ("Nimbus");
        
    //_model.setNotLoadedIntoExperiment();
    //_model.setNotEngagedInExperiment();
    
    this._executionHistoryOperations.add("");
    _model.addObserver(this);
  }

  private void createMenuBar (int time) {
    JMenuBar mb = new JMenuBar ();
    mb.add (createShellMenu ());
    mb.add (createDataMenu ());
    mb.add (createModelMenu (time));
    setJMenuBar (mb);
  }
  
  public Chrest getModel(){
    return this._model;
  }

  @Override
  public void update(Observable o, Object arg) {
    if(_model.engagedInExperiment()){
      for(int menuItem = 0; menuItem < this._dataMenu.getItemCount(); menuItem++){
        this._dataMenu.getItem(menuItem).setEnabled(false);
      }
    }
    else {
      for(int menuItem = 0; menuItem < this._dataMenu.getItemCount(); menuItem++){
        this._dataMenu.getItem(menuItem).setEnabled(true);
      }
    }
  }
  
  /**
   * Updates the data structure that stores the current execution history of the 
   * model associated with this Shell instance. Whilst performing this update, 
   * the function can also update the data structure that stores the operations
   * currently present in the execution history of the model.
   * 
   * @param executionHistory 
   * @param updateOperations Set to true to update the data structure containing 
   * execution history operations for this Shell instance.  If this isn't
   * required, set to false.
   */
  private void updateExecutionHistoryAndOperations(ArrayList<ArrayList<Object[]>> executionHistory, boolean updateOperations){
    
    //Start with nothing.
    this._executionHistory.clear();
    if(executionHistory != null){
      
      //Get the metadata for the execution history table.
      ArrayList<String[]> executionHistoryColumnMetadata = this._model.getExecutionHistoryTableColumnMetadata();
      
      //Get a row of data from the execution history passed.
      for(ArrayList<Object[]> rowOfExecutionHistoryDataPassed : executionHistory){
        
        //Create a new data structure to hold a row of data from the execution
        //history passed.
        ArrayList rowData = new ArrayList();
        
        //Process each column from the CHREST model.
        for(String[] columnMetadata : executionHistoryColumnMetadata){
          
          //Get the column's name, c. 
          String columnName = columnMetadata[0];
          
          //Get the matching column value from the execution history data passed
          //and add it to the row.  Thus, the column order specified in the
          //execution history table will be maintained in this instance's data
          //structure that stores the model's current execution history.
          for(Object[] columnNameAndValuePassed : rowOfExecutionHistoryDataPassed){
            String columnNamePassed = (String)columnNameAndValuePassed[0];
            Object columnValuePassed = columnNameAndValuePassed[1];
            if(columnNamePassed.equals(columnName)){
              rowData.add(columnValuePassed);
            }
            
            //Update execution history operations.
            if(updateOperations){
              if(columnNamePassed.equals(Chrest._executionHistoryTableOperationColumnName)){
                String operation = (String)columnValuePassed;
                if(!this._executionHistoryOperations.contains(operation)){
                  this._executionHistoryOperations.add(operation);
                }
              }
            }
          }
        }
        
        //Add the row data to the data structure that will be used to populate 
        //the visible execution history table in the model information panel.
        _executionHistory.add(rowData);
      }
    }
  }

  /**
   * Simple action to display a message about this application.
   */
  class AboutAction extends AbstractAction implements ActionListener {
    private Shell _parent;

    AboutAction (Shell parent) {
      super ("About", new ImageIcon (Shell.class.getResource("icons/About16.gif")));
      _parent = parent;
    }

    public void actionPerformed (ActionEvent e) {
      JOptionPane.showMessageDialog (
          _parent, 
          "<HTML><P>This program is a graphical interface to the <BR>" +
          "CHREST cognitive architecture.  You can load <BR>" +
          "data, train models, and visualise results in a <BR>" +
          "range of typical modelling problems for CHREST.</P>" +
          "<P><P>Copyright (c) 2010-12, Peter C. R. Lane.</P></P>" +
          "<P>Released under Open Works License</a>, version 0.9.2.</P>" + 
          
          "<p>See <a href=\"http://chrest.info\">http://chrest.info</a> for more information.</P></HTML>",
          "About CHREST Shell v. 4.0.0-ALPHA-1", 
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * Simple action to allow user to change look and feel.
   */
  class LookFeelAction extends AbstractAction implements ActionListener {
    private Shell _parent;

    LookFeelAction (Shell parent) {
      super ("Theme");
      _parent = parent;
    }

    public void actionPerformed (ActionEvent e) {
      Object [] possibleValues = new Object[UIManager.getInstalledLookAndFeels().length];
      int i = 0;
      for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels ()) {
        possibleValues[i] = info.getName ();
        i += 1;
      }
      String theme = (String)JOptionPane.showInputDialog (_parent,
          "Select look and feel:",
          "Change theme",
          JOptionPane.INFORMATION_MESSAGE, 
          null,
          possibleValues,
          UIManager.getLookAndFeel().getName ());
      if (theme != null) {
        setTheme (theme);
      }
    }
  }
  
  /**
   * Action to load in a scripted experiment from file.
   */
  class LoadScriptedExperimentAction extends AbstractAction implements ActionListener {
    private final Shell _parent;
    private final String _scriptedExperimentClassName;

    LoadScriptedExperimentAction (Shell parent, String scriptedExperimentName, String scriptedExperimentClassName) {
      super (scriptedExperimentName); 
      this._parent = parent;
      this._scriptedExperimentClassName = scriptedExperimentClassName;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      
      //Retrieve the selected file as a File object and get the file's path 
      //as a string so that file.getPath() isn't repeated.
      File file = new File("classes" + File.separator + "jchrest" + File.separator + "experimentScripts" + File.separator + this._scriptedExperimentClassName + ".class");
      String filePath = file.getPath();

      //Now, replace all file path seperators with periods to get a standard 
      //Java path specification.  Note that the file seperator needs to be 
      //escaped in the regex otherwise in Windows it will be a "\" and this is
      //used to indicate that the next character should be escaped in regex.  
      //Thus, a regex error will be thrown.  After this, get the path from 
      //"jchrest" to where the file extension begins i.e. the first part of the 
      //path to the scripted experiment class to the last.
      String fullyQualifiedExperimentClassName = filePath.replaceAll(Pattern.quote(File.separator), ".").substring(filePath.indexOf("jchrest"), filePath.lastIndexOf("."));

      //Finally, invoke the constructor for the scripted experiment class.
      try {
        Class<?> scriptedExperimentClass = Class.forName(fullyQualifiedExperimentClassName);
        Constructor scriptedExperimentConstructor = scriptedExperimentClass.getDeclaredConstructor(jchrest.gui.Shell.class);
        scriptedExperimentConstructor.setAccessible(true);
        scriptedExperimentConstructor.newInstance(this._parent);
      } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
        Logger.getLogger(Shell.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
  
  /**
   * Action to load in a new pre-experiment data set from file.
   */
  class LoadPreExperimentDataAction extends AbstractAction implements ActionListener {
    private Shell _parent;

    LoadPreExperimentDataAction (Shell parent) {
      super ("Load Pre-Experiment Data", new ImageIcon (Shell.class.getResource("icons/Open16.gif"))); 
      _parent = parent;
    }

    public void actionPerformed (ActionEvent e) {
      if(!_model.engagedInExperiment()){
        (new LoadDataThread (_parent, "Load Pre-Experiment Data", false)).execute ();
      }
    }
  }

  /**
   * Action to load in a new data set from file.
   */
  class LoadExperimentDataAction extends AbstractAction implements ActionListener {
    private Shell _parent;

    LoadExperimentDataAction (Shell parent) {
      super ("Load Experiment Data", new ImageIcon (Shell.class.getResource("icons/Open16.gif"))); 
      _parent = parent;
    }

    public void actionPerformed (ActionEvent e) {
      if(!_model.engagedInExperiment()){
        (new LoadDataThread (_parent, "Load Experiment Data", true)).execute ();
      }
    }
    
    @Override
    public String toString(){
      return "Load Experiment Data Action";
    }
  }

  enum Status {CANCELLED_SELECTION, CANCELLED_RUNNING, ERROR, OK};

  /**
   * Worker thread to handle loading the data.
   */
  public class LoadDataThread extends SwingWorker<Void, Void> {
    private final Shell _parent;
    private String _task;
    private List<ListPattern> _items;
    private List<PairedPattern> _pairs;
    private Scenes _scenes;
    private Status _status = Status.OK;
    private final String _openDialogTitle;
    private final boolean _experiment;
    private String _experimentName;

    public LoadDataThread (Shell parent, String openDialogTitle, boolean experiment) {
      _parent = parent;
      _task = "";
      _items = null;
      _pairs = null;
      _scenes = null;
      _openDialogTitle = openDialogTitle;
      _experiment = experiment;
    }

    @Override
    public Void doInBackground () {
      JFileChooser fileChooser = new JFileChooser(".");
      fileChooser.setDialogTitle(_openDialogTitle);
      int resultOfFileSelect = fileChooser.showOpenDialog(_parent);
      if(resultOfFileSelect == JFileChooser.APPROVE_OPTION){
        File file = fileChooser.getSelectedFile();

        if (file == null) {
          _status = Status.CANCELLED_SELECTION;
        } else {
          try {
            _status = Status.OK; // assume all will be fine
            _experimentName = file.getName().replaceFirst("\\..*$", "");
            _task = "";
            // add a monitor to the input stream, to show a message if input is taking a while
            InputStream inputStream = new ProgressMonitorInputStream(
                _parent, 
                "Reading the input file", 
                new FileInputStream (file));
            BufferedReader input = new BufferedReader (new InputStreamReader (inputStream));

            String line = input.readLine ();
            if (line != null) {
              _task = line.trim ();
            }

            if (_task.equals ("recognise-and-learn")) {
              _items = InputOutput.readItems (input, false);
            } else if (_task.equals ("serial-anticipation")) {
              _items = InputOutput.readItems (input, true);
            } else if (_task.equals ("paired-associate")) {
              _pairs = InputOutput.readPairedItems (input, false);
            } else if (_task.equals ("categorisation")) {
              _pairs = InputOutput.readPairedItems (input, true);
            } else if (_task.equals ("visual-search")) {
              _scenes = Scenes.read (input); // throws IOException if any problem
            }
          } catch (InterruptedIOException ioe) {
            _status = Status.CANCELLED_RUNNING; // flag cancelled error
          } catch (IOException ioe) {
            ioe.printStackTrace(System.err); //Give some meaningful info for debugging.
            _status = Status.ERROR; // flag an IO error
          }
        }
      }
      else if(resultOfFileSelect == JFileChooser.CANCEL_OPTION){
        _status = Status.CANCELLED_SELECTION;
      }
      return null;
    }

    @Override
    protected void done () {
      switch (_status) {
        case CANCELLED_SELECTION:
          break;
        case ERROR:
          JOptionPane.showMessageDialog (_parent, 
              "There was an error in processing your file", 
              "File error",
              JOptionPane.ERROR_MESSAGE);
          break;
        case CANCELLED_RUNNING:
          JOptionPane.showMessageDialog (_parent, 
              "You cancelled the operation : no change", 
              "File Load Cancelled",
              JOptionPane.WARNING_MESSAGE);
          break;
        case OK:
          //Before loading a new experiment, save the maximum value of the 
          //model's clocks in this experiment so that the most recent state of
          //the model for this experiment can be rendered graphically, if
          //requested.
          _model.setMaxmimumTimeInExperiment(_model.getMaximumClockValue());

          if(this._experiment){
            _model.setLoadedIntoExperiment();
            _model.addExperimentsLocatedInName(_experimentName);
          }
          else{
            _model.setNotLoadedIntoExperiment();
            _model.addExperimentsLocatedInName(Chrest.getPreExperimentPrepend() + _experimentName);
          }

          JPanel experimentInterface = null;
          
          if (_task.equals ("recognise-and-learn") && _items != null) {
            experimentInterface = new RecogniseAndLearnDemo (_model, _items);
          } else if (_task.equals ("serial-anticipation") && _items != null) {
            experimentInterface = new PairedAssociateInterface (_model, PairedAssociateExperiment.makePairs(_items));
          } else if (_task.equals ("paired-associate") && _pairs != null) {
            experimentInterface = new PairedAssociateInterface (_model, _pairs);
          } else if (_task.equals ("categorisation") && _pairs != null) {
            experimentInterface = new CategorisationExperiment (_model, _pairs);
          } else if (_task.equals ("visual-search") && _scenes != null) {
            experimentInterface = new VisualSearchPane (_model, _scenes);
          } else {
            JOptionPane.showMessageDialog (_parent,
                "Invalid task on first line of file",
                "File error",
                JOptionPane.ERROR_MESSAGE);
          }
          _parent.setContentPane(experimentInterface);
          _parent.validate ();
          _model.setCurrentExperiment((Experiment)experimentInterface);

          break;
      }
    }

    public List<ListPattern> getItems(){
      return this._items;
    }
    
    public List<PairedPattern> getPairs(){
      return this._pairs;
    }
  }

  /**
   * Action to clear data held in the model.
   */
  class ClearModelAction extends AbstractAction implements ActionListener {
    private Shell _parent;

    ClearModelAction (Shell parent) {
      super ("Clear", new ImageIcon (Shell.class.getResource ("icons/Delete16.gif")));

      _parent = parent;
    }

    public void actionPerformed (ActionEvent e) {
      if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog (
        _parent,
        "Are you sure you want to clear the model?",
        "Clear model?",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
      )){
        String lastExperimentLocatedInName = _model.getExperimentsLocatedInNames().get(_model.getExperimentsLocatedInNames().size() - 1);
        _model.clear ();
        _model.setNotEngagedInExperiment();
        _model.addExperimentsLocatedInName(lastExperimentLocatedInName);
      }
    }
  }

  /**
   * Action to show a dialog to change properties of model.
   */
  class ModelPropertiesAction extends AbstractAction implements ActionListener {
    private Shell _parent;

    ModelPropertiesAction (Shell parent) {
      super ("Properties", new ImageIcon (Shell.class.getResource("icons/Properties16.gif"))); 

      _parent = parent;
    }

    public void actionPerformed (ActionEvent e) {
      if(
        JOptionPane.OK_OPTION == JOptionPane.showOptionDialog (_parent, 
          properties(), 
          "CHREST: Model properties", 
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE,
          null, 
          null, 
          0
        )
      ) {
        Experiment currentExperiment = _model.getCurrentExperiment();
        int currentExperimentTime = currentExperiment == null ? 0 : currentExperiment.getCurrentTime();
        
        _model.setAddProductionTime (((SpinnerNumberModel)_addLinkTime.getModel()).getNumber().intValue ());
        _model.setDiscriminationTime (((SpinnerNumberModel)_discriminationTime.getModel()).getNumber().intValue ());
        _model.setFamiliarisationTime (((SpinnerNumberModel)_familiarisationTime.getModel()).getNumber().intValue ());
        _model.setRho (((SpinnerNumberModel)_rhoEntry.getModel()).getNumber().floatValue ());
        _model.getStm(Modality.VISUAL).setCapacity(((SpinnerNumberModel)_visualStmSize.getModel()).getNumber().intValue(), currentExperimentTime);
        _model.getStm(Modality.VERBAL).setCapacity(((SpinnerNumberModel)_verbalStmSize.getModel()).getNumber().intValue(), currentExperimentTime);
        _model.getPerceiver().setFixationFieldOfView (((SpinnerNumberModel)_fieldOfView.getModel()).getNumber().intValue ());
        _model.setCreateSemanticLinks (_createSemanticLinks.isSelected ());
        _model.setCreateTemplates(_createTemplates.isSelected ());
        _model.setRecordHistory(_recordHistory.isSelected());
        _model.setNodeImageSimilarityThreshold(((SpinnerNumberModel)_similarityThreshold.getModel()).getNumber().intValue ());
      }
    }

    private JSpinner _addLinkTime;
    private JSpinner _discriminationTime;
    private JSpinner _familiarisationTime;
    private JSpinner _rhoEntry;
    private JSpinner _visualStmSize;
    private JSpinner _verbalStmSize;
    private JSpinner _fieldOfView;
    private JSpinner _similarityThreshold;
    private JCheckBox _createSemanticLinks;
    private JCheckBox _createTemplates;
    private JCheckBox _recordHistory;

    private JPanel properties () {
      
      Experiment currentExperiment = _model.getCurrentExperiment();
      int currentExperimentTime = currentExperiment == null ? 0 : currentExperiment.getCurrentTime();
      
      // -- create entry widgets
      _addLinkTime = new JSpinner (new SpinnerNumberModel (_model.getAddProductionTime (), 1, 100000, 1));
      _discriminationTime = new JSpinner (new SpinnerNumberModel (_model.getDiscriminationTime (), 1, 100000, 1));
      _familiarisationTime = new JSpinner (new SpinnerNumberModel (_model.getFamiliarisationTime (), 1, 100000, 1));
      _rhoEntry = new JSpinner (new SpinnerNumberModel (_model.getRho (), 0.0, 1.0, 0.1));
      _visualStmSize = new JSpinner (new SpinnerNumberModel ((int)_model.getStm(Modality.VISUAL).getCapacity(currentExperimentTime), (int)1, (int)10, (int)1));
      _verbalStmSize = new JSpinner (new SpinnerNumberModel ((int)_model.getStm(Modality.VERBAL).getCapacity(currentExperimentTime), (int)1, (int)10, (int)1));
      _fieldOfView = new JSpinner (new SpinnerNumberModel (_model.getPerceiver().getFixationFieldOfView (), 1, 100, 1));
      _similarityThreshold = new JSpinner (new SpinnerNumberModel (_model.getNodeImageSimilarityThreshold(), 1, 100, 1));
      _createSemanticLinks = new JCheckBox ("Use semantic links", _model.canCreateSemanticLinks ());
      _createTemplates = new JCheckBox ("Use templates", _model.canCreateTemplates ());
      _recordHistory = new JCheckBox ("Record history", _model.canRecordHistory());

      JPanel panel = new JPanel ();
      panel.setLayout (new SpringLayout ());
      Utilities.addLabel (panel, "Add link time (ms)", _addLinkTime);
      Utilities.addLabel (panel, "Discrimination time (ms)", _discriminationTime);
      Utilities.addLabel (panel, "Familiarisation time (ms)", _familiarisationTime);
      Utilities.addLabel (panel, "Rho", _rhoEntry);
      Utilities.addLabel (panel, "Visual STM size", _visualStmSize);
      Utilities.addLabel (panel, "Verbal STM size", _verbalStmSize);
      Utilities.addLabel (panel, "Field of view", _fieldOfView);
      Utilities.addLabel (panel, "Similarity threshold", _similarityThreshold);
      Utilities.addLabel (panel, "", _createSemanticLinks);
      Utilities.addLabel (panel, "", _createTemplates);
      Utilities.addLabel (panel, "", _recordHistory);

      Utilities.makeCompactGrid (panel, 11, 2, 3, 3, 10, 5);
      panel.setMaximumSize (panel.getPreferredSize ());

      return panel;
    }
  }

  /**
   * Action to save test-link data from current model in VNA format.
   */
  class SaveModelAsVnaAction extends AbstractAction implements ActionListener {
    private Shell _parent;
    private int _time;

    SaveModelAsVnaAction (Shell parent, int time) {
      super ("Save visual network (.VNA)"); 

      _parent = parent;
      _time = time;
    }

    public void actionPerformed (ActionEvent e) {
      File file = FileUtilities.getSaveFilename (_parent, "Save visual network");
      if (file == null) return;
      try {
        FileWriter writer = new FileWriter (file);
        _model.writeModelAsVna (writer, _time);
        writer.close ();
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog (_parent,
            "File " + file.getName () + 
            " could not be saved due to an error.",
            "Error: File save error",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * Action to save semantic links in current model as VNA.
   */
  class SaveModelSemanticLinksAsVnaAction extends AbstractAction implements ActionListener {
    private Shell _parent;
    private int _time;

    SaveModelSemanticLinksAsVnaAction (Shell parent, int time) {
      super ("Save visual semantic links (.VNA)"); 

      _parent = parent;
      _time = time;
    }

    public void actionPerformed (ActionEvent e) {
      File file = FileUtilities.getSaveFilename (_parent, "Save visual semantic links");
      if (file == null) return;
      try {
        FileWriter writer = new FileWriter (file);
        _model.writeModelSemanticLinksAsVna (writer, _time);
        writer.close ();
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog (_parent,
            "File " + file.getName () + 
            " could not be saved due to an error.",
            "Error: File save error",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * Action to display information about the current model.
   */
  class ModelInformationAction extends AbstractAction implements ActionListener {
    private int _time;

    ModelInformationAction (Shell parent, int time) {
      super ("Information", new ImageIcon (Shell.class.getResource("icons/Information16.gif")));
      _time = time;
    }

    public void actionPerformed (ActionEvent e) {
      
      //Update the execution history data structures for this Shell instance 
      //first since the mode information panel requires execution history 
      //information from the CHREST model associated with this Shell instance 
      //so it can be displayed.
      Shell.this.updateExecutionHistoryAndOperations(Shell.this._model.getExecutionHistory(), true);
      JPanel base = new JPanel ();
      base.setLayout (new GridLayout(1,1));

      JTabbedPane jtb = new JTabbedPane ();
      jtb.addTab ("Info", getInfoPane (_time));
      jtb.addTab ("Contents", getHistogramPane (_model.getContentSizeCounts(_time), "contents", "Histogram of Contents Sizes", "Contents size"));
      jtb.addTab ("Images", getHistogramPane (_model.getImageSizeCounts(_time), "images", "Histogram of Image Sizes", "Image size"));
      jtb.addTab ("Semantic links", getHistogramPane (_model.getSemanticLinkCounts(_time), "semantic", "Histogram of Number of Semantic Links", "Number of semantic links"));
      jtb.addTab("Exec. History", getExecutionHistoryPanel());
      base.add (jtb);

      JOptionPane pane = new JOptionPane (base, JOptionPane.INFORMATION_MESSAGE);
      JDialog dialog = pane.createDialog (Shell.this, "CHREST: Model information");
      dialog.setResizable (true);
      dialog.setVisible (true);
    }
  }

  private JLabel getInfoPane (int time) {
    DecimalFormat twoPlaces = new DecimalFormat("0.00");
    return new JLabel (
        "<html><p>" + 
        "Total nodes in LTM: " + _model.getLtmSize(time) +
        "<hr>" + 
        "Visual nodes: " + _model.getLtmModalitySize(Modality.VISUAL, time) + 
        " Average depth: " + twoPlaces.format (_model.getLtmAverageDepth (Modality.VISUAL, time)) +
        "<br>Verbal nodes: " + _model.getLtmModalitySize (Modality.VERBAL, time) + 
        " Average depth: " + twoPlaces.format (_model.getLtmAverageDepth (Modality.VERBAL, time)) +
        "<br>Action nodes: " + _model.getLtmModalitySize (Modality.ACTION, time) + 
        " Average depth: " + twoPlaces.format (_model.getLtmAverageDepth (Modality.ACTION, time)) +
        "<br>Number of templates: " + _model.countTemplatesInVisualLtm(time) +
        "</p></html>"
        );
  }

  private JPanel getHistogramPane (Map<Integer, Integer> contentSizes, String label, String title, String xAxis) {
    int largest = 0;
    for (Integer key : contentSizes.keySet ()) {
      if (key > largest) largest = key;
    }
    SimpleHistogramDataset dataset = new SimpleHistogramDataset (label);
    for (int i = 0; i <= largest; ++i) {
      SimpleHistogramBin bin = new SimpleHistogramBin ((double)i, (double)(i+1), true, false);
      int count = 0;
      if (contentSizes.containsKey (i)) {
        count = contentSizes.get (i);
      }
      bin.setItemCount (count);
      dataset.addBin (bin);
    }
    PlotOrientation orientation = PlotOrientation.VERTICAL; 
    boolean show = false; 
    boolean toolTips = true;
    boolean urls = false; 
    JFreeChart chart = ChartFactory.createHistogram( title, xAxis, "frequency", 
        dataset, orientation, show, toolTips, urls);

    JPanel panel = new JPanel ();
    panel.setLayout (new BorderLayout ());
    panel.add (new ChartPanel (chart, 400, 300, 200, 100, 600, 600, true, true, true, true, true, true));
    JButton saveButton = new JButton ("Save Data");
    saveButton.setToolTipText ("Save the histogram data to a CSV file");
    saveButton.addActionListener ( new SaveHistogramActionListener (this, contentSizes));
    panel.add (saveButton, BorderLayout.SOUTH);
    return panel;
  }
  
  /**
   * Builds and returns the execution history panel.
   * @return 
   */
  public JPanel getExecutionHistoryPanel(){
    JPanel executionHistoryPanel = new JPanel();
    
    if(_executionHistory.isEmpty()){
      String emptyMessage = "No execution history recorded yet.<br><hr><br>";

      if(this._model.canRecordHistory()){
        emptyMessage += "The CHREST model associated with this GUI can record history so "
        + "<br>try running an experiment.";
      }
      else{
        emptyMessage += "The CHREST model associated with this GUI is not set to record history." +
          "<br>To enable this functionality go to 'Menu -> Properties' and check" +
          "<br>the 'Record History' box.";
      }
      
      JLabel emptyMessageJLabel = new JLabel("<html>" + emptyMessage + "</html>");
      JScrollPane historyScrollPane = new JScrollPane(emptyMessageJLabel);
      executionHistoryPanel.add(historyScrollPane);
    }
    else{
      
      /********************************************************/
      /***** Execution history filter components creation *****/
      /********************************************************/
      
      //Time filters construction
      int maxChrestTime = this._model.getMaximumClockValue();
      
      _executionHistoryTimeFrom = new JSpinner(new SpinnerNumberModel(0, 0, maxChrestTime, 1));
      _executionHistoryTimeFrom.setToolTipText("Max value is largest value for CHREST's attention/learning clock");
      
      _executionHistoryTimeTo = new JSpinner(new SpinnerNumberModel(maxChrestTime, 0, maxChrestTime, 1));
      _executionHistoryTimeTo.setToolTipText("Max value is largest value for CHREST's attention/learning clock");
      
      JFormattedTextField timeFromEditorField = (JFormattedTextField)_executionHistoryTimeFrom.getEditor().getComponent(0);
      DefaultFormatter timeFromFormatter = (DefaultFormatter) timeFromEditorField.getFormatter();
      timeFromFormatter.setCommitsOnValidEdit(true);
      _executionHistoryTimeFrom.addChangeListener(new ChangeListener() {

        @Override
        public void stateChanged(ChangeEvent e) {
          
          //When changing the "Time From" value, this should change the "Time 
          //To" minimum value and, if the "Time From" value is greater than the
          //"Time To" value, the "Time To" value should be equal to the new
          //"Time From" value plus 1.
          SpinnerNumberModel timeToModel = (SpinnerNumberModel)_executionHistoryTimeTo.getModel();
          Integer timeFromCurrentValue = (Integer)_executionHistoryTimeFrom.getValue();
          timeToModel.setMinimum(timeFromCurrentValue);
        }

      });
      
      JFormattedTextField timeToEditorField = (JFormattedTextField) _executionHistoryTimeTo.getEditor().getComponent(0);
      DefaultFormatter timeToFormatter = (DefaultFormatter) timeToEditorField.getFormatter();
      timeToFormatter.setCommitsOnValidEdit(true);
      _executionHistoryTimeTo.addChangeListener(new ChangeListener() {

        @Override
        public void stateChanged(ChangeEvent e) {
          
          //When changing the "Time To" value, this should alter the "Time From"
          //maximum value and, if the "Time To" value is less than the
          //"Time From" value, the "Time From" value should be equal to the new
          //"Time To" value minus 1.
          SpinnerNumberModel timeFromModel = (SpinnerNumberModel)_executionHistoryTimeFrom.getModel();
          Integer timeToCurrentValue = (Integer)_executionHistoryTimeTo.getValue();
          timeFromModel.setMaximum(timeToCurrentValue);
          
        }

      });
      
      //Operation filter construction
      String[] executionHistoryOperationsArray = this._executionHistoryOperations.toArray(new String[this._executionHistoryOperations.size()]);
      Arrays.sort(executionHistoryOperationsArray);
      this._executionHistoryOperationsComboBox = new JComboBox(executionHistoryOperationsArray);
      ((JLabel)this._executionHistoryOperationsComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
      
      //Add filter components to panel
      JPanel filterOptions = new JPanel();
      filterOptions.setBorder(new TitledBorder ("Filters"));
      filterOptions.setLayout(new GridLayout(4, 2));
      
      filterOptions.add(new JLabel("Time From: "));
      filterOptions.add(_executionHistoryTimeFrom);
      
      filterOptions.add(new JLabel("Time To: "));
      filterOptions.add(_executionHistoryTimeTo);
      
      filterOptions.add(new JLabel("Filter by Operation: "));
      filterOptions.add(this._executionHistoryOperationsComboBox);
      
      filterOptions.add( new JLabel(""));
      filterOptions.add(new JButton( new FilterExecutionHistoryAction() ));
      
      /******************************************/
      /***** Create execution history table *****/
      /******************************************/
      TableModel executionHistoryTableModel = new AbstractTableModel() {
        
        @Override
        public int getRowCount() {
          if(_executionHistory.size() > 0){
            return _executionHistory.size();
          } else {
            return 0;
          }
        }

        @Override
        public int getColumnCount(){
          if(_executionHistory.size() > 0){
            return _executionHistory.get(0).size();
          } else {
            return Shell.this._model.getExecutionHistoryTableColumnMetadata().size();
          }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
          if(_executionHistory.size() > rowIndex && _executionHistory.get(rowIndex).size() > columnIndex){
            return _executionHistory.get(rowIndex).get(columnIndex);
          } else {
            return null;
          }
        }

        @Override
        public String getColumnName (int columnIndex) {
          String uncapitalisedColumnName = (String)Shell.this._model.getExecutionHistoryTableColumnMetadata().get(columnIndex)[0];
          return uncapitalisedColumnName.substring(0, 1).toUpperCase() + uncapitalisedColumnName.substring(1);
        }
        
        @Override
        public void fireTableStructureChanged() {
          super.fireTableStructureChanged ();
          JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(Shell.this._executionHistoryTable);
          Shell.this._executionHistoryTable.removeColumn(Shell.this._executionHistoryTable.getColumnModel().getColumn(0));
        }
      };
      
      _executionHistoryTable = new JTable (executionHistoryTableModel);
      JTableCustomOperations.resizeColumnsToFitWidestCellContentInColumn(this._executionHistoryTable);
      Shell.this._executionHistoryTable.removeColumn(Shell.this._executionHistoryTable.getColumnModel().getColumn(0));
      
      /*****************************************/
      /***** Build execution history panel *****/
      /*****************************************/
      
      JScrollPane executionHistoryScrollPane = new JScrollPane(_executionHistoryTable);
      JButton exportExecutionHistoryButton = new JButton( new ExportExecutionHistoryAction() );
      exportExecutionHistoryButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
      
      JPanel executionHistory = new JPanel();
      executionHistory.setBorder(new TitledBorder ("Execution History"));
      executionHistory.setLayout(new BoxLayout(executionHistory, BoxLayout.Y_AXIS));
      executionHistory.add(executionHistoryScrollPane);
      executionHistory.add(exportExecutionHistoryButton);
      
      executionHistoryPanel.setLayout(new BoxLayout(executionHistoryPanel, BoxLayout.Y_AXIS));
      executionHistoryPanel.add(filterOptions);
      executionHistoryPanel.add(executionHistory);
    }
    
    return executionHistoryPanel;
  }
  
  class ExportExecutionHistoryAction extends AbstractAction implements ActionListener {

    ExportExecutionHistoryAction(){
      super ("Export Execution History as CSV");
    }
      
    @Override
    public void actionPerformed(ActionEvent e) {
      ArrayList<String> executionHistoryData = new ArrayList<>();
      executionHistoryData.add(ExportData.extractJTableDataAsCsv(_executionHistoryTable));
      executionHistoryData.add("CHRESTexecutionHistory");
      executionHistoryData.add("csv");
      
      ArrayList<ArrayList<String>> data = new ArrayList<>();
      data.add(executionHistoryData);
      ExportData.saveFile(null, "CHREST-execution-history-data", data);
    }
  }
  
  class FilterExecutionHistoryAction extends AbstractAction implements ActionListener{

    FilterExecutionHistoryAction(){
      super("Filter");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      
      SwingWorker<Void, Void> filterExecutionHistoryThread = new FilterExecutionHistoryThread();
      filterExecutionHistoryThread.execute();
    }
  }
  
  private final class FilterExecutionHistoryThread extends SwingWorker<Void, Void> {

    @Override
    protected Void doInBackground() throws Exception {
      ArrayList<ArrayList<Object[]>> executionHistory = null;
      
      String selectedOperation = Shell.this._executionHistoryOperationsComboBox.getSelectedItem().toString();
      if(selectedOperation.equals("")){
        executionHistory = Shell.this._model.getHistory((Integer)_executionHistoryTimeFrom.getValue(), (Integer)_executionHistoryTimeTo.getValue() );
      }
      else{
        executionHistory = _model.getHistory(selectedOperation, (Integer)_executionHistoryTimeFrom.getValue(), (Integer)_executionHistoryTimeTo.getValue());
      }
      
      Shell.this.updateExecutionHistoryAndOperations(executionHistory, false);
      return null;
    }
    
    @Override
    protected void done(){
      ((AbstractTableModel)_executionHistoryTable.getModel()).fireTableStructureChanged();
    }
  }

  class SaveHistogramActionListener implements ActionListener {
    Shell _parent;
    Map<Integer, Integer> _data;

    public SaveHistogramActionListener (Shell parent, Map<Integer, Integer> data) {
      _parent = parent;
      _data = data;
    }
    
    public void actionPerformed (ActionEvent e) {
      File file = FileUtilities.getSaveFilename (_parent, "Save histogram data");
      if (file == null) return;
      try {
        FileWriter writer = new FileWriter (file);
        for (Integer key : _data.keySet ()) {
          writer.write ("" + key + ", " + _data.get (key) + "\n");
        }
        writer.close ();
      } catch (IOException ioe) {
        JOptionPane.showMessageDialog (_parent,
            "File " + file.getName () + 
            " could not be saved due to an error.",
            "Error: File save error",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /** 
   * Action to display a separate frame with information about the current model.
   * Frame is composed of separate views onto the model, all using the observer 
   * design pattern to keep updated as the model changes.
   */
  class ViewModelAction extends AbstractAction implements ActionListener {
    private Shell _parent;

    ViewModelAction (Shell parent) {
      super ("View", new ImageIcon (Shell.class.getResource("icons/Find16.gif")));

      _parent = parent;
    }

    public void actionPerformed (ActionEvent e) {
      new ChrestView (_parent, _model, _model.getMaximumTimeForExperiment(_model.getCurrentExperimentName()));
    }
  }

  private JMenu createShellMenu () {
    JMenuItem exit = new JMenuItem ("Exit", new ImageIcon (Shell.class.getResource ("icons/Stop16.gif")));
    exit.addActionListener (new ActionListener () {
      public void actionPerformed (ActionEvent e) {
        System.exit (0);
      }
    });

    JMenu menu = new JMenu ("Shell");
    menu.setMnemonic (KeyEvent.VK_S);
    menu.add (new AboutAction (this));
    menu.getItem(0).setMnemonic (KeyEvent.VK_A);
    menu.add (new LookFeelAction (this));
    menu.getItem(1).setMnemonic (KeyEvent.VK_T);
    menu.add (new JSeparator ());
    menu.add (exit);
    menu.getItem(3).setMnemonic (KeyEvent.VK_X);

    menu.getItem(0).setAccelerator (KeyStroke.getKeyStroke('A', java.awt.Event.CTRL_MASK, false));
    menu.getItem(3).setAccelerator (KeyStroke.getKeyStroke('X', java.awt.Event.CTRL_MASK, false));
    return menu;
  }

  private JMenu createDataMenu () {
    this._dataMenu = new JMenu ("Data");
    this._dataMenu.setMnemonic (KeyEvent.VK_D);
    
    JMenu scriptedExperimentSubMenu = new JMenu ("Load Scripted Experiment");
    scriptedExperimentSubMenu.add(new LoadScriptedExperimentAction(this, "Paired Associate: Fast/Slow", "PairedAssociateFastSlow"));
    this._dataMenu.add(scriptedExperimentSubMenu);
    
    this._dataMenu.add (new LoadPreExperimentDataAction (this)).setAccelerator (KeyStroke.getKeyStroke('P', java.awt.Event.CTRL_MASK, false));
    this._dataMenu.add (new LoadExperimentDataAction (this)).setAccelerator(KeyStroke.getKeyStroke('O', java.awt.Event.CTRL_MASK, false));
    return this._dataMenu;
  }

  private JMenu createModelMenu (int time) {
    JMenu menu = new JMenu ("Model");
    menu.setMnemonic (KeyEvent.VK_M);
    menu.add (new ClearModelAction (this));
    menu.getItem(0).setMnemonic (KeyEvent.VK_C);

    JMenu submenu = new JMenu ("Save");
    submenu.setMnemonic (KeyEvent.VK_S);
    submenu.add (new SaveModelAsVnaAction (this, time));
    submenu.getItem(0).setMnemonic (KeyEvent.VK_N);
    submenu.add (new SaveModelSemanticLinksAsVnaAction (this, time));
    submenu.getItem(1).setMnemonic (KeyEvent.VK_L);
    menu.add (submenu);

    menu.add (new ModelPropertiesAction (this));
    menu.getItem(2).setMnemonic (KeyEvent.VK_P);
    menu.add (new JSeparator ());
    menu.add (new ModelInformationAction (this, time));
    menu.getItem(4).setMnemonic (KeyEvent.VK_I);
    menu.add (new ViewModelAction (this));
    menu.getItem(5).setMnemonic (KeyEvent.VK_V);

    return menu;
  }

  /**
   * Set theme of user interface to the one named.
   */
  private void setTheme (String theme) {
    try { 
      for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if (theme.equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (UnsupportedLookAndFeelException e) {
    } catch (ClassNotFoundException e) {
    } catch (InstantiationException e) {
    } catch (IllegalAccessException e) {
    }
    // make sure all components are updated
    SwingUtilities.updateComponentTreeUI(this);
    // SwingUtilities.updateComponentTreeUI(_fileChooser); TODO: update FileUtilities filechooser
  }

  /**
   * main method to get everything started.
   */
  public static void main (String[] args) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() { 
        Shell shell = new Shell ();
        shell.setVisible (true);
      }
    });
  }
}

