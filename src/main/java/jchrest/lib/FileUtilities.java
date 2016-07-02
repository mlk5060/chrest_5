// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import java.awt.Component;
import java.io.*;
import java.lang.Character;
import javax.swing.*;

/** A collection of static methods used when handling files */
public class FileUtilities {
  // -- fields and methods to handle a file dialog
	private final static JFileChooser _fileChooser = new JFileChooser (System.getProperty("user.dir"));

  /**
   * Request user for a filename to save to.  Argument 'parent' centres the dialog on the 
   * given component.
   * 
   * @param parent
   * @return
   */
  public static File getSaveFilename (Component parent) {
    return FileUtilities.getSaveFilename (parent, "Save");
  }

  /**
   * Request user for a filename to save to.  Argument 'parent' centres the dialog on the 
   * given component.  Argument 'title' gives a title for the dialog.
   * 
   * @param parent
   * @param title
   * @return 
   */
  public static File getSaveFilename (Component parent, String title) {
    _fileChooser.setDialogTitle (title);
    _fileChooser.setMultiSelectionEnabled (false);
    _fileChooser.setSelectedFile (new File ("x")); // clear the previous selection
    _fileChooser.setSelectedFile (new File (""));
    int option = _fileChooser.showSaveDialog (parent);
    if (option == JFileChooser.APPROVE_OPTION) {
      File filename = _fileChooser.getSelectedFile ();
      if (filename.exists()) {
        int overwrite = JOptionPane.showConfirmDialog(parent, 
            "File " + filename.getName() +
						" exists.  Are you sure you want to overwrite it?",
						"Warning: Overwriting file",
						JOptionPane.YES_NO_OPTION);
				if (overwrite == JOptionPane.YES_OPTION) {
					return filename;
				} else {
					return getSaveFilename (parent);
				}
			} else {
				return filename;
			}
		}
		return null;
	}

  /**
   * Get a filename to load from.
   * 
   * @param parent
   * @return 
   */
  public static File getLoadFilename (Component parent) {
    _fileChooser.setMultiSelectionEnabled (false);
    _fileChooser.setSelectedFile (new File ("x")); // clear the previous selection
    _fileChooser.setSelectedFile (new File (""));
    int option = _fileChooser.showOpenDialog (parent);
    if (option == JFileChooser.APPROVE_OPTION) {
      File filename = _fileChooser.getSelectedFile ();
      if (filename.exists()) {
        return filename;
      } else {
        JOptionPane.showMessageDialog(parent, 
            "File " + filename.getName() +
            " does not exist.  You need to select an existing file.",
            "Error: No file exists",
            JOptionPane.ERROR_MESSAGE);
        return getLoadFilename (parent);
      }
		}
		return null;
	}

}

