package jchrest.domainSpecifics.tileworld.fixations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import jchrest.architecture.Chrest;
import jchrest.domainSpecifics.Fixation;
import jchrest.domainSpecifics.Scene;
import jchrest.domainSpecifics.SceneObject;
import jchrest.domainSpecifics.tileworld.TileworldDomain;
import jchrest.lib.Square;

/**
 * Represents a {@link jchrest.domainSpecifics.Fixation} made in Tileworld where
 * an agent considers where a moveable Tileworld {@link 
 * jchrest.domainSpecifics.SceneObject} could be moved to
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class MovementFixation extends Fixation{
  private Chrest _associatedModel = null;

  /**
   * Constructor.
   * 
   * @param associatedModel The {@link jchrest.architecture.Chrest} model 
   * making {@link #this}.
   * @param timeThatDecidingUponThisStarts The time (in milliseconds) that it 
   * will be in the domain when {@link #this} starts to be decided upon.
   * @param timeTakenToDecideUponThis The time (in milliseconds) that it takes 
   * to decide upon the {@link jchrest.lib.Square} that {@link #this} will 
   * fixate on after starting deliberation on it.
   */
  public MovementFixation(Chrest associatedModel, int timeThatDecidingUponThisStarts, int timeTakenToDecideUponThis) {
    super(timeThatDecidingUponThisStarts, timeTakenToDecideUponThis);
    this._associatedModel = associatedModel;
  }
  
  /**
   * 
   * @param scene
   * @param time
   * 
   * @return If any of the following statements evaluate to {@link 
   * java.lang.Boolean#TRUE}, {@code null} is returned:
   * 
   * <ul>
   *  <li>
   *    The {@code time} specified is earlier than {@link 
   *    #this#_performanceTime}.
   *  </li>
   *  <li>
   *    The result of invoking {@link 
   *    jchrest.architecture.Perceiver#getMostRecentFixationPerformed(int)} is
   *    {@code null} for the {@link jchrest.architecture.Perceiver} associated
   *    with {@link #this#_associatedModel}.
   *  </li>
   *  <li>
   *    The coordinates fixated on in the result of {@link 
   *    jchrest.architecture.Perceiver#getMostRecentFixationPerformed(int)} are
   *    not present in the {@code scene} specified.
   *  </li>
   *  <li>
   *    Invoking {@link jchrest.domainSpecifics.SceneObject#getObjectType()} in
   *    context of the {@link jchrest.domainSpecifics.SceneObject} returned by
   *    invoking {@link jchrest.domainSpecifics.Fixation#getObjectSeen()} in 
   *    context of {@link 
   *    jchrest.architecture.Perceiver#getMostRecentFixationPerformed(int)} does
   *    not equal either {@link jchrest.domainSpecifics.tileworld.TileworldDomain#OPPONENT_SCENE_OBJECT_TYPE_TOKEN},
   *    {@link jchrest.domainSpecifics.tileworld.TileworldDomain#TILE_SCENE_OBJECT_TYPE_TOKEN} or
   *    {@link jchrest.domainSpecifics.Scene#CREATOR_TOKEN}, i.e. a moveable
   *    {@link jchrest.domainSpecifics.SceneObject} in context of Tileworld.
   *  </li>
   * <ul>
   * 
   * Otherwise, a {@link jchrest.lib.Square} that the {@link 
   * jchrest.domainSpecifics.SceneObject} fixated on previously can move to is
   * returned with equal probability.  This is any {@link jchrest.lib.Square} 
   * along any cardinal compass direction around the {@link 
   * jchrest.domainSpecifics.SceneObject SceneObject's} current location in 
   * {@code scene} that doesn't contain a {@link 
   * jchrest.domainSpecifics.SceneObject} that returns {@link 
   * jchrest.domainSpecifics.Scene#BLIND_SQUARE_TOKEN}.
   */
  @Override
  public Square make(Scene scene, int time) {
    this._associatedModel.printDebugStatement("===== MovementFixation.make() =====");
    Square squareToFixateOn = null;
    
    if(this._performanceTime <= time){
      this._associatedModel.printDebugStatement("- Performance time OK");
      
      Fixation mostRecentFixationPerformed = this._associatedModel.getPerceiver().getMostRecentFixationPerformed(time);
      if(mostRecentFixationPerformed != null){
        this._associatedModel.printDebugStatement("- Previous fixation OK");
        
        Integer sceneColFixatedOn = mostRecentFixationPerformed.getColFixatedOn();
        Integer sceneRowFixatedOn = mostRecentFixationPerformed.getRowFixatedOn();
        Scene sceneMostRecentFixationPerformedOn = mostRecentFixationPerformed.getScene();

        int mostRecentDomainColFixatedOn = sceneMostRecentFixationPerformedOn.getDomainSpecificColFromSceneSpecificCol(sceneColFixatedOn);
        int mostRecentDomainRowFixatedOn = sceneMostRecentFixationPerformedOn.getDomainSpecificRowFromSceneSpecificRow(sceneRowFixatedOn);

        if(scene.areDomainSpecificCoordinatesRepresented(mostRecentDomainColFixatedOn, mostRecentDomainRowFixatedOn)){
          this._associatedModel.printDebugStatement("- Previous Fixation coordinates represented in current Scene");

          Integer mostRecentColFixatedOnInContextOfNewScene = scene.getSceneSpecificColFromDomainSpecificCol(mostRecentDomainColFixatedOn);
          Integer mostRecentRowFixatedOnInContextOfNewScene = scene.getSceneSpecificColFromDomainSpecificCol(mostRecentDomainColFixatedOn);
          SceneObject objectOnSquareMostRecentlyFixatedOn = scene.getSquareContents(mostRecentColFixatedOnInContextOfNewScene, mostRecentRowFixatedOnInContextOfNewScene);

          //Check if the object is moveable (is it a tile, opponent or the 
          //creator?) and, if so, look 1 square aong each cardinal compass 
          //direction (objects in Tileworld can only be moved along cardinal 
          //compass directions) and add the resulting square to a set of 
          //potential fixations.
          String typeOfObjectOnSquareMostRecentlyFixatedOn = objectOnSquareMostRecentlyFixatedOn.getObjectType();
          List<Square> potentialFixations = new ArrayList();
          if(
            typeOfObjectOnSquareMostRecentlyFixatedOn.equals(TileworldDomain.TILE_SCENE_OBJECT_TYPE_TOKEN) ||
            typeOfObjectOnSquareMostRecentlyFixatedOn.equals(Scene.getCreatorToken()) ||
            typeOfObjectOnSquareMostRecentlyFixatedOn.equals(TileworldDomain.OPPONENT_SCENE_OBJECT_TYPE_TOKEN)
          ){
            
            //North
            if(
              (mostRecentRowFixatedOnInContextOfNewScene + 1 >= 0) && 
              (mostRecentRowFixatedOnInContextOfNewScene + 1 < scene.getHeight())
            ){
              potentialFixations.add(new Square(
                mostRecentColFixatedOnInContextOfNewScene, 
                mostRecentRowFixatedOnInContextOfNewScene + 1)
              );
            } 
           
            //East
            if(
              (mostRecentColFixatedOnInContextOfNewScene + 1 >= 0) && 
              (mostRecentColFixatedOnInContextOfNewScene + 1 < scene.getWidth())
            ){
              potentialFixations.add(new Square(
                mostRecentColFixatedOnInContextOfNewScene + 1, 
                mostRecentRowFixatedOnInContextOfNewScene)
              );
            }

            //South
            if(
              (mostRecentRowFixatedOnInContextOfNewScene - 1 >= 0) && 
              (mostRecentRowFixatedOnInContextOfNewScene - 1 < scene.getHeight())
            ){
              potentialFixations.add(new Square(
                mostRecentColFixatedOnInContextOfNewScene, 
                mostRecentRowFixatedOnInContextOfNewScene - 1)
              );
            }
            
            //West
            if(
              (mostRecentColFixatedOnInContextOfNewScene - 1 >= 0) && 
              (mostRecentColFixatedOnInContextOfNewScene - 1 < scene.getWidth())
            ){ 
              potentialFixations.add(new Square(
                mostRecentColFixatedOnInContextOfNewScene - 1, 
                mostRecentRowFixatedOnInContextOfNewScene)
              );
            }
          }
          this._associatedModel.printDebugStatement("- Potential Fixations represented in Scene:\n" + potentialFixations);

          //Remove any potential fixations on coordinates that are blind or the
          //creator.
          Iterator<Square> potentialFixationsIterator = potentialFixations.iterator();
          while(potentialFixationsIterator.hasNext()){
            Square potentialFixation = potentialFixationsIterator.next();
            String objectType = scene.getSquareContents(potentialFixation.getColumn(), potentialFixation.getRow()).getObjectType();
            if(objectType.equals(Scene.getBlindSquareToken()) || objectType.equals(Scene.CREATOR_TOKEN)){
              potentialFixationsIterator.remove();
            }
          }
          this._associatedModel.printDebugStatement(
            "- Potential Fixations after removing fixations on blind squares " +
            "and squares containing the creator:\n" + potentialFixations
          );

          //Check if there any potential fixations, if there are, select one at
          //random with equal probability.
          if(!potentialFixations.isEmpty()){
            squareToFixateOn = potentialFixations.get(new Random().nextInt(potentialFixations.size()));
          }
        }
      }
    }
    
    this._associatedModel.printDebugStatement("Returning " + (squareToFixateOn == null ? "null" : squareToFixateOn));
    this._associatedModel.printDebugStatement("===== RETURN =====");
    return squareToFixateOn;
  }
  
}
