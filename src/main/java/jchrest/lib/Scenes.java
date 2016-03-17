// Copyright (c) 2012, Peter C. R. Lane
// Released under Open Works License, http://owl.apotheon.org/

package jchrest.lib;

import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.SceneObject;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Scenes class holds a list of Scene objects.  Each Scene contains 
 * an array of 'situations'.
 *
 * @author Peter C. R. Lane
 */
public class Scenes {

  /**
   * Read in a list of scenes from the given input stream.  
   *
   * @param input Format is:
   *
   * <ul>
   *  <li>
   *    line 1: height width minimum_domain_specific_x_coordinate 
   *    minimum_domain_specific_y_coordinate e.g. 8 8 13 10
   *  </li>
   *  <li>line 2: blank (can act as a comment)</li>
   *  <li>
   *    line 3 to 3+height: lines of length 'width'.  Each char in the line 
   *    should be the class of an object or '.' if the coordinate should be 
   *    empty
   *  </li>
   *  <li>line 3+height+3: blank (can be comment)</li>
   *  <li>Can then repeat previous 2 bullet points until EOF</li>
   * </ul>
   * 
   * @return 
   * @throws java.io.IOException If any line is short, or the number of lines 
   * cannot be read.
   */
  public static Scenes read(BufferedReader input) throws IOException {
    Scenes scenes;
    int height, width, minimumDomainSpecificXCoordinate, minimumDomainSpecificYCoordinate;

    //Line 1
    String line;
    line = input.readLine ();
    if (line == null) throw new IOException (); 
    String[] dimensions = line.split (" ");
    if (dimensions.length != 2) throw new IOException ();
    try {
      height = Integer.decode(dimensions[0]);
      width = Integer.decode(dimensions[1]);
      minimumDomainSpecificXCoordinate = Integer.decode(dimensions[2]);
      minimumDomainSpecificYCoordinate = Integer.decode(dimensions[3]);
    } catch (NumberFormatException nfe) { throw new IOException (); 
    }
    
    scenes = new Scenes (height, width);
    int sceneNumber = 0;
    line = input.readLine (); // read the blank/comment line
    while (line != null) { // finish if no more positions to read
      line = input.readLine (); // read first line of scene
      if (line == null) break;  // finish calmly if last position followed by blank line
      sceneNumber += 1;
    
      Scene scene = new Scene(
        "Scene " + sceneNumber, 
        width, 
        height,
        minimumDomainSpecificXCoordinate,
        minimumDomainSpecificYCoordinate,
        null
      );
      
      for (int y = 0; y < height; ++y) {
        if (line == null) throw new IOException ();         // finished in the middle of a position
        if (line.length() != width) throw new IOException (); // incorrect width of row
        char[] lineAsCharArray = line.toCharArray ();
        ArrayList<SceneObject> itemsToAdd = new ArrayList<>();
        
        int x = 0;
        for(char character : lineAsCharArray){
          scene.addItemToSquare(
            x,
            y,
            UUID.randomUUID().toString(),
            String.valueOf(character)
          );
          
          x++;
        }
        
        line = input.readLine (); // on last cycle, this tries to read blank/comment line
      }
      scenes.add (scene);
    }
    
    return scenes;
  }

  private int height;
  private int width;
  private List<Scene> scenes;
  private List<Move> moves;

  private Scenes (int height, int width) {
    this.height = height;
    this.width = width;
    this.scenes = new ArrayList<Scene> ();
    this.moves = new ArrayList<Move> ();
  }

  private void add (Scene scene) {
    scenes.add (scene);
  }

  private void add (Scene scene, Move move) {
    scenes.add (scene);
    moves.add (move);
  }

  public String [] getSceneNames () {
    String [] names = new String[scenes.size ()];
    for (int i = 0; i < scenes.size (); ++i) {
      names[i] = scenes.get(i).getName ();
    }
    return names;
  }

  public Scene get (int i) {
    return scenes.get (i);
  }

  public boolean haveMoves () {
    return (moves.size () > 0);
  }

  public Move getMove (int i) {
    if (haveMoves () && i < moves.size ()) {
      return moves.get (i);
    } else {
      return new Move ("None", 0, 0);
    }
  }

  public int size () {
    return scenes.size ();
  }
}

