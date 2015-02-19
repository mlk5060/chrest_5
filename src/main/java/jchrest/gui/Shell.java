// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.gui;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import jchrest.architecture.Chrest;
import jchrest.lib.FileUtilities;
import jchrest.lib.ListPattern;
import jchrest.lib.PairedPattern;
import jchrest.lib.Pattern;
import jchrest.lib.Scenes;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultFormatter;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.*;
import org.jfree.data.statistics.*;

/**
 * The main frame for the Chrest shell.
 *
 * @author Peter C. R. Lane
 */
public class Shell extends JFrame implements Observer {
  private Chrest _model;
  private JMenu _dataMenu; //Required so that "Data" menu options can be disabled if the model is engaged in an experiment.

  public Shell () {
    super ("CHREST 4");

    _model = new Chrest ();

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    createMenuBar ();
    
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
    
    _model.addObserver(this);
    _model.setNotLoadedIntoExperiment();
    _model.setNotEngagedInExperiment();
  }

  private void createMenuBar () {
    JMenuBar mb = new JMenuBar ();
    mb.add (createShellMenu ());
    mb.add (createDataMenu ());
    mb.add (createModelMenu ());
    setJMenuBar (mb);
  }

  @Override
  public void update(Observable o, Object arg) {
    if(_model.engagedInExperiment()){
      this._dataMenu.getItem(0).setEnabled(false);
      this._dataMenu.getItem(1).setEnabled(false);
    }
    else {
      this._dataMenu.getItem(0).setEnabled(true);
      this._dataMenu.getItem(1).setEnabled(true);
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
  private class LoadDataThread extends SwingWorker<Void, Void> {
    private Shell _parent;
    private String _task;
    private List<ListPattern> _items;
    private List<PairedPattern> _pairs;
    private Scenes _scenes;
    private Status _status = Status.OK;
    private final String _openDialogTitle;
    private final boolean _experiment;
    private String _experimentName;

    LoadDataThread (Shell parent, String openDialogTitle, boolean experiment) {
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
                _items = readItems (input, false);
              } else if (_task.equals ("serial-anticipation")) {
                _items = readItems (input, true);
              } else if (_task.equals ("paired-associate")) {
                _pairs = readPairedItems (input, false);
              } else if (_task.equals ("categorisation")) {
                _pairs = readPairedItems (input, true);
              } else if (_task.equals ("visual-search")) {
                _scenes = Scenes.read (input); // throws IOException if any problem
              } else if (_task.equals ("visual-search-with-move")){
                _scenes = Scenes.readWithMove (input); // throws IOException if any problem
              }
            } catch (InterruptedIOException ioe) {
              _status = Status.CANCELLED_RUNNING; // flag cancelled error
            } catch (IOException ioe) {
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
            //Before loading a new experiment, save the current learning clock 
            //of the model in this experiment so that the most recent state of
            //the model for this experiment can be rendered graphically, if
            //requested.
            _model.setMaxmimumTimeInExperiment(_model.getLearningClock());
            
            if(this._experiment){
              _model.setLoadedIntoExperiment();
              _model.addExperimentsLocatedInName(_experimentName);
            }
            else{
              _model.setNotLoadedIntoExperiment();
              _model.addExperimentsLocatedInName(_model.getPreExperimentPrepend() + _experimentName);
            }
            
            if (_task.equals ("recognise-and-learn") && _items != null) {
              _parent.setContentPane (new RecogniseAndLearnDemo (_model, _items));
            } else if (_task.equals ("serial-anticipation") && _items != null) {
              _parent.setContentPane (new PairedAssociateExperiment (_model, PairedAssociateExperiment.makePairs(_items)));
            } else if (_task.equals ("paired-associate") && _pairs != null) {
              _parent.setContentPane (new PairedAssociateExperiment (_model, _pairs));
            } else if (_task.equals ("categorisation") && _pairs != null) {
              _parent.setContentPane (new CategorisationExperiment (_model, _pairs));
            } else if (_task.equals ("visual-search") && _scenes != null) {
              _parent.setContentPane (new VisualSearchPane (_model, _scenes));
            } else {
              JOptionPane.showMessageDialog (_parent,
                  "Invalid task on first line of file",
                  "File error",
                  JOptionPane.ERROR_MESSAGE);
            }
            _parent.validate ();
            
            break;
        }
      }

    private List<ListPattern> readItems (BufferedReader input, boolean verbal) throws IOException {
      List<ListPattern> items = new ArrayList<ListPattern> ();
      String line = input.readLine ();

      while (line != null) {
        ListPattern pattern;
        if (verbal) {
          pattern = Pattern.makeVerbalList (line.trim().split("[, ]"));
        } else {
          pattern = Pattern.makeVisualList (line.trim().split("[, ]"));
        }
        pattern.setFinished ();
        items.add (pattern);
        line = input.readLine ();
      } 

      return items;
    }

    // categorisation = false => make both verbal
    // categorisation = true  => make first visual, second verbal
    private List<PairedPattern> readPairedItems (BufferedReader input, boolean categorisation) throws IOException {
      List<PairedPattern> items = new ArrayList<PairedPattern> ();
      String line = input.readLine ();
      while (line != null) {
        String[] pair = line.split (":");
        if (pair.length != 2) throw new IOException (); // malformed pair
        ListPattern pat1;
        if (categorisation) {
          pat1 = Pattern.makeVisualList (pair[0].trim().split("[, ]"));
        } else {
          pat1 = Pattern.makeVerbalList (pair[0].trim().split("[, ]"));
        }
        pat1.setFinished ();
        ListPattern pat2 = Pattern.makeVerbalList (pair[1].trim().split("[, ]"));
        pat2.setFinished ();
        items.add (new PairedPattern (pat1, pat2));

        line = input.readLine ();
      }

      return items;
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
      if (JOptionPane.OK_OPTION == JOptionPane.showOptionDialog (_parent, 
            properties(), 
            "CHREST: Model properties", 
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null, null, 0)) {
        _model.setAddLinkTime (((SpinnerNumberModel)_addLinkTime.getModel()).getNumber().intValue ());
        _model.setDiscriminationTime (((SpinnerNumberModel)_discriminationTime.getModel()).getNumber().intValue ());
        _model.setFamiliarisationTime (((SpinnerNumberModel)_familiarisationTime.getModel()).getNumber().intValue ());
        _model.setRho (((SpinnerNumberModel)_rhoEntry.getModel()).getNumber().floatValue ());
        _model.setVisualStmSize (((SpinnerNumberModel)_visualStmSize.getModel()).getNumber().intValue ());
        _model.setVerbalStmSize (((SpinnerNumberModel)_verbalStmSize.getModel()).getNumber().intValue ());
        _model.getPerceiver().setFieldOfView (((SpinnerNumberModel)_fieldOfView.getModel()).getNumber().intValue ());
        _model.setCreateSemanticLinks (_createSemanticLinks.isSelected ());
        _model.setCreateTemplates(_createTemplates.isSelected ());
        _model.setRecordHistory(_recordHistory.isSelected());
        _model.setSimilarityThreshold(((SpinnerNumberModel)_similarityThreshold.getModel()).getNumber().intValue ());
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
      // -- create entry widgets
      _addLinkTime = new JSpinner (new SpinnerNumberModel (_model.getAddLinkTime (), 1, 100000, 1));
      _discriminationTime = new JSpinner (new SpinnerNumberModel (_model.getDiscriminationTime (), 1, 100000, 1));
      _familiarisationTime = new JSpinner (new SpinnerNumberModel (_model.getFamiliarisationTime (), 1, 100000, 1));
      _rhoEntry = new JSpinner (new SpinnerNumberModel (_model.getRho (), 0.0, 1.0, 0.1));
      _visualStmSize = new JSpinner (new SpinnerNumberModel (_model.getVisualStmSize (), 1, 10, 1));
      _verbalStmSize = new JSpinner (new SpinnerNumberModel (_model.getVerbalStmSize (), 1, 10, 1));
      _fieldOfView = new JSpinner (new SpinnerNumberModel (_model.getPerceiver().getFieldOfView (), 1, 100, 1));
      _similarityThreshold = new JSpinner (new SpinnerNumberModel (_model.getSimilarityThreshold (), 1, 100, 1));
      _createSemanticLinks = new JCheckBox ("Use semantic links", _model.getCreateSemanticLinks ());
      _createTemplates = new JCheckBox ("Use templates", _model.getCreateTemplates ());
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

    SaveModelAsVnaAction (Shell parent) {
      super ("Save visual network (.VNA)"); 

      _parent = parent;
    }

    public void actionPerformed (ActionEvent e) {
      File file = FileUtilities.getSaveFilename (_parent, "Save visual network");
      if (file == null) return;
      try {
        FileWriter writer = new FileWriter (file);
        _model.writeModelAsVna (writer);
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

    SaveModelSemanticLinksAsVnaAction (Shell parent) {
      super ("Save visual semantic links (.VNA)"); 

      _parent = parent;
    }

    public void actionPerformed (ActionEvent e) {
      File file = FileUtilities.getSaveFilename (_parent, "Save visual semantic links");
      if (file == null) return;
      try {
        FileWriter writer = new FileWriter (file);
        _model.writeModelSemanticLinksAsVna (writer);
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
    private Shell _parent;

    ModelInformationAction (Shell parent) {
      super ("Information", new ImageIcon (Shell.class.getResource("icons/Information16.gif")));

      _parent = parent;
    }

    public void actionPerformed (ActionEvent e) {
      JPanel base = new JPanel ();
      base.setLayout (new GridLayout(1,1));

      JTabbedPane jtb = new JTabbedPane ();
      jtb.addTab ("Info", getInfoPane ());
      jtb.addTab ("Contents", getHistogramPane (_model.getContentCounts(), "contents", "Histogram of Contents Sizes", "Contents size"));
      jtb.addTab ("Images", getHistogramPane (_model.getImageCounts(), "images", "Histogram of Image Sizes", "Image size"));
      jtb.addTab ("Semantic links", getHistogramPane (_model.getSemanticLinkCounts(), "semantic", "Histogram of Number of Semantic Links", "Number of semantic links"));
      jtb.addTab("History", getHistoryPane());
      base.add (jtb);

      JOptionPane pane = new JOptionPane (base, JOptionPane.INFORMATION_MESSAGE);
      JDialog dialog = pane.createDialog (_parent, "CHREST: Model information");
      dialog.setResizable (true);
      dialog.setVisible (true);
    }
  }

  private JLabel getInfoPane () {
    DecimalFormat twoPlaces = new DecimalFormat("0.00");
    return new JLabel (
        "<html><p>" + 
        "Total nodes in LTM: " + _model.getTotalLtmNodes () +
        "<hr>" + 
        "Visual nodes: " + _model.ltmVisualSize () + 
        " Average depth: " + twoPlaces.format (_model.getVisualLtmAverageDepth ()) +
        "<br>Verbal nodes: " + _model.ltmVerbalSize () + 
        " Average depth: " + twoPlaces.format (_model.getVerbalLtmAverageDepth ()) +
        "<br>Action nodes: " + _model.ltmActionSize () + 
        " Average depth: " + twoPlaces.format (_model.getActionLtmAverageDepth ()) +
        "<br>Number of templates: " + _model.countTemplates () +
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
  
  private JSpinner _timeFrom;
  private JSpinner _timeTo;
  private JSpinner _operations;
  private JTable _historyTable;
  private final List<List<String>> _history = new ArrayList<>();
  
  private void updateHistory(SQLiteStatement sqlResults){
    _history.clear();
    int row = 0;

    try {
      while(sqlResults.step()){
        _history.add(new ArrayList<>());
        for(int col = 0; col < sqlResults.columnCount(); col++){
          _history.get(row).add(sqlResults.columnString(col));
        }
        row += 1;
      }
    } catch (SQLiteException ex) {
      Logger.getLogger(Shell.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
  private void configureHistoryTable(){
    TableColumnModel historyTableColumnModel = _historyTable.getColumnModel();
      
    //Remove primary key column from table GUI.
    _historyTable.removeColumn(historyTableColumnModel.getColumn(0)); 

    if(!_history.isEmpty()){
      //Set each column's width in the table to the size of the longest string
      //contained in a row for that column.
      for (int col = 0; col < _historyTable.getColumnCount(); col++) {
        int maxColWidth = 0;

        for(int row = 0; row < _historyTable.getRowCount(); row++){
          TableCellRenderer renderer = _historyTable.getCellRenderer(row, col);
          Component comp = _historyTable.prepareRenderer(renderer, row, col);
          maxColWidth = Math.max (comp.getPreferredSize().width, maxColWidth);
        }

        historyTableColumnModel.getColumn(col).setPreferredWidth(maxColWidth);
      }
    }
  }
  
  private JPanel getHistoryPane() {
    
    JPanel historyPanel = new JPanel();
    updateHistory(this._model.getHistory());
    
    if(_history.isEmpty()){
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
      historyPanel.add(historyScrollPane);
    }
    else{
      
      //Filter options creation.
      int maxChrestTime = Math.max(this._model.getLearningClock(), this._model.getAttentionClock());
      
      _timeFrom = new JSpinner(new SpinnerNumberModel(0, 0, maxChrestTime, 1));
      _timeFrom.setToolTipText("Max value is largest value for CHREST's attention/learning clock");
      
      _timeTo = new JSpinner(new SpinnerNumberModel(maxChrestTime, 0, maxChrestTime, 1));
      _timeTo.setToolTipText("Max value is largest value for CHREST's attention/learning clock");
      
      JFormattedTextField timeFromEditorField = (JFormattedTextField)_timeFrom.getEditor().getComponent(0);
      DefaultFormatter timeFromFormatter = (DefaultFormatter) timeFromEditorField.getFormatter();
      timeFromFormatter.setCommitsOnValidEdit(true);
      _timeFrom.addChangeListener(new ChangeListener() {

        @Override
        public void stateChanged(ChangeEvent e) {
          
          //When changing the "Time From" value, this should change the "Time 
          //To" minimum value and, if the "Time From" value is greater than the
          //"Time To" value, the "Time To" value should be equal to the new
          //"Time From" value plus 1.
          SpinnerNumberModel timeToModel = (SpinnerNumberModel)_timeTo.getModel();
          Integer timeFromCurrentValue = (Integer)_timeFrom.getValue();
          timeToModel.setMinimum(timeFromCurrentValue);
        }

      });
      
      JFormattedTextField timeToEditorField = (JFormattedTextField) _timeTo.getEditor().getComponent(0);
      DefaultFormatter timeToFormatter = (DefaultFormatter) timeToEditorField.getFormatter();
      timeToFormatter.setCommitsOnValidEdit(true);
      _timeTo.addChangeListener(new ChangeListener() {

        @Override
        public void stateChanged(ChangeEvent e) {
          
          //When changing the "Time To" value, this should alter the "Time From"
          //maximum value and, if the "Time To" value is less than the
          //"Time From" value, the "Time From" value should be equal to the new
          //"Time To" value minus 1.
          SpinnerNumberModel timeFromModel = (SpinnerNumberModel)_timeFrom.getModel();
          Integer timeToCurrentValue = (Integer)_timeTo.getValue();
          timeFromModel.setMaximum(timeToCurrentValue);
          
        }

      });
      
      ArrayList<String> operations = new ArrayList<>();
      operations.addAll(Chrest.getPossibleOperations());
      operations.add(0, "");
      _operations = new JSpinner(new SpinnerListModel(operations));
      
      JPanel filterOptions = new JPanel();
      filterOptions.setBorder(new TitledBorder ("Filters"));
      filterOptions.setLayout(new GridLayout(4, 2));
      
      filterOptions.add(new JLabel("Time From: "));
      filterOptions.add(_timeFrom);
      
      filterOptions.add(new JLabel("Time To: "));
      filterOptions.add(_timeTo);
      
      filterOptions.add(new JLabel("Filter by Operation: "));
      filterOptions.add(_operations);
      
      filterOptions.add( new JLabel(""));
      filterOptions.add(new JButton( new FilterExecutionHistoryAction() ));
      
      //Execution history creation.
      TableModel historyTableModel = new AbstractTableModel() {
        
        @Override
        public int getRowCount() {
          return _history.size();
        }

        @Override
        public int getColumnCount(){
          return 4; 
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
          return _history.get(rowIndex).get(columnIndex);
        }

        @Override
        public String getColumnName (int columnIndex) {
          String columnName = "";
          
          switch(columnIndex){
            case 0:
              //Even though this isn't displayed in the JTable, it needs to be
              //specified so that the CSV file is formatted correctly if the
              //user chooses to export table data.
              columnName = "Id";
              break;
            case 1:
              columnName = "Time";
              break;
            case 2:
              columnName = "Operation";
              break;
            case 3:
              columnName = "Description";
              break;
          }
          
          return columnName;
        }
        
        @Override
        public void fireTableStructureChanged() {
          super.fireTableStructureChanged ();
          configureHistoryTable();
        }
      };
      
      //Construct new JTable using "historyTableModel" data and return the
      //table's column model.
      _historyTable = new JTable (historyTableModel);
      configureHistoryTable();
      _historyTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
      JScrollPane historyScrollPane = new JScrollPane(_historyTable);
      
      JPanel executionHistory = new JPanel();
      executionHistory.setBorder(new TitledBorder ("Execution History"));
      executionHistory.setLayout(new BoxLayout(executionHistory, BoxLayout.Y_AXIS));
      executionHistory.add(historyScrollPane);
      JButton exportButton = new JButton( new ExportHistoryAction() );
      exportButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
      executionHistory.add(exportButton);
      
      //History panel creation.
      historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
      historyPanel.add(filterOptions);
      historyPanel.add(executionHistory);
    }
    
    return historyPanel;
  }
  
  class ExportHistoryAction extends AbstractAction implements ActionListener {

    ExportHistoryAction(){
      super ("Export History as CSV");
    }
      
    @Override
    public void actionPerformed(ActionEvent e) {
      ArrayList<String> executionHistoryData = new ArrayList<>();
      executionHistoryData.add(ExportData.extractJTableDataAsCsv(_historyTable));
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
      
      if(_operations.getValue().toString().equals("")){
       updateHistory(_model.getHistory((Integer)_timeFrom.getValue(), (Integer)_timeTo.getValue()));
      }
      else{
        updateHistory(_model.getHistory((String) _operations.getValue(), (Integer)_timeFrom.getValue(), (Integer)_timeTo.getValue()));
      }

      ((AbstractTableModel)_historyTable.getModel()).fireTableStructureChanged();
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
      new ChrestView (_parent, _model);
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
    this._dataMenu.add (new LoadPreExperimentDataAction (this)).setAccelerator (KeyStroke.getKeyStroke('P', java.awt.Event.CTRL_MASK, false));
    this._dataMenu.add (new LoadExperimentDataAction (this)).setAccelerator(KeyStroke.getKeyStroke('O', java.awt.Event.CTRL_MASK, false));
    return this._dataMenu;
  }

  private JMenu createModelMenu () {
    JMenu menu = new JMenu ("Model");
    menu.setMnemonic (KeyEvent.VK_M);
    menu.add (new ClearModelAction (this));
    menu.getItem(0).setMnemonic (KeyEvent.VK_C);

    JMenu submenu = new JMenu ("Save");
    submenu.setMnemonic (KeyEvent.VK_S);
    submenu.add (new SaveModelAsVnaAction (this));
    submenu.getItem(0).setMnemonic (KeyEvent.VK_N);
    submenu.add (new SaveModelSemanticLinksAsVnaAction (this));
    submenu.getItem(1).setMnemonic (KeyEvent.VK_L);
    menu.add (submenu);

    menu.add (new ModelPropertiesAction (this));
    menu.getItem(2).setMnemonic (KeyEvent.VK_P);
    menu.add (new JSeparator ());
    menu.add (new ModelInformationAction (this));
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

