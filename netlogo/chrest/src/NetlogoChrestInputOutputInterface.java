import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import jchrest.architecture.Chrest;
import jchrest.lib.ItemSquarePattern;
import jchrest.lib.ListPattern;
import jchrest.lib.Modality;

import org.nlogo.agent.Turtle;
import org.nlogo.api.AgentException;
import org.nlogo.api.Context;

/**
 * Contains variable name constants and methods to be used by the CHREST Netlogo 
 * extension primitives. 
 * 
 * @author Martyn Lloyd-Kelly
 */
public class NetlogoChrestInputOutputInterface {
    
    public final static String CHREST_INSTANCE_CHREST_AGENT_BREED_VAR_NAME = "CHREST-INSTANCE";
    public final static String NETLOGO_MODEL_OBSERVER_AGENT_VAR_NAME = "NETLOGO-MODEL";
    
    /**
     * Checks to see if a turtle has been endowed with an instance of the CHREST
     * architecture.
     * 
     * @param context Instance of the current Netlogo execution environment 
     *                object.
     * @return 
     */
    private static boolean agentHasChrestInstance(Context context) throws AgentException{
        org.nlogo.agent.Agent agent = getAgent(context);
        return agent.getBreedVariable(NetlogoChrestInputOutputInterface.CHREST_INSTANCE_CHREST_AGENT_BREED_VAR_NAME) instanceof Chrest;
    }
    
    /**
     * Returns an instance of an org.nlogo.agent.Agent object i.e the turtle 
     * that is executing the function in which this function is called.
     * 
     * @param context Instance of the current Netlogo execution environment 
     *                object.
     * @return org.nlogo.agent.Agent
     */
    private static org.nlogo.agent.Agent getAgent(Context context){
        return (org.nlogo.agent.Agent)context.getAgent();
    }
    
    /**
     * Constructs a "ListPattern" object specific to a particular Netlogo model
     * that can be used by the Chrest.java class.
     * 
     * @param context Instance of the current Netlogo execution environment 
     *                object.
     * @param xCorOffset Number of patches east that the calling turtle is able 
     *                   to "see".
     * @param yCorOffset Number of patches north that the calling turtle is able
     *                   to "see".
     * @return 
     */
    public static ListPattern buildListPatternFromNetlogoPercept(Context context, int xCorOffset, int yCorOffset) throws AgentException{
        
        /////////////////////////////////////////////////////////////////
        ///// CREATE NEW LIST PATTERN AND CHECK FOR CHREST INSTANCE /////
        /////////////////////////////////////////////////////////////////
        ListPattern listPattern = new ListPattern();
        if(NetlogoChrestInputOutputInterface.agentHasChrestInstance(context)){
            
            /////////////////////////////////////////
            ///// RETURN CURRENT MODEL FILENAME /////
            /////////////////////////////////////////
            org.nlogo.agent.Agent agent = (org.nlogo.agent.Agent)context.getAgent();
            String modelFilename = (String)agent.world().getObserverVariableByName(NetlogoChrestInputOutputInterface.NETLOGO_MODEL_OBSERVER_AGENT_VAR_NAME);

            ////////////////////////////////
            ///// CHECK MODEL FILENAME /////
            ////////////////////////////////
            
            /*** chresttileworld MODEL ***/
            if(modelFilename.equalsIgnoreCase("chresttileworld")){
                listPattern =  NetlogoChrestInputOutputInterface.chrestTileworldBuildListPattern(context, listPattern, xCorOffset, yCorOffset);
            }
        }
        
        ///////////////////////////////
        ///// RETURN LIST PATTERN /////
        ///////////////////////////////
        return listPattern;
    }
    
    /**
     * Builds a list pattern with visual modality for use by the calling 
     * turtle's CHREST instance in context of the "ChrestTileworld" Netlogo 
     * model.  The calling turtle will look for other turtles from the patch 
     * that is most south-west of its visual range to the patch that is most 
     * north-east of its visual range.
     * 
     * 1) Function begins by setting the modality of the list pattern object 
     *    passed as a parameter to "visual".
     * 2) The calling turtle's absolute maximum x/y-coordinate values are 
     *    converted to their negative values i.e. 2 is converted to -2.
     * 3) A for-loop is then entered where the following process occurs:
     *      a) Calling turtle retrieves an instance of the patch from its 
     *         current x/y-coordinate offset.
     *      b) Patch instance is checked for the existence of turtles.  If there
     *         are any turtles on the patch the calling turtle checks each one 
     *         to see if it is the calling turtle.
     *      c) If a turtle on the patch is not the calling turtle then the 
     *         breed of the turtle on the patch and the patch's x/y coordinate
     *         offset from the calling turtle is retrieved and added to the list 
     *         pattern object.
     *      d) The current x-coordinate offset value is increased by 1 resulting 
     *         in the calling turtle looking at the next patch east of the last 
     *         patch looked at in the next loop iteration (if the maximum 
     *         x-coordinate value has not been surpassed).
     *      e) If the maximum x-coordinate offset value has been surpassed then 
     *         the current y-coordinate offset value is increased by 1 and the 
     *         current x-coordinate offset value is reset to its negated 
     *         absolute value (if the maximum y-coordinate value has not been 
     *         surpassed).  This results in the calling turtle looking at the 
     *         next row of patches north of the last row looked at starting from 
     *         the western-most patch of that row.
     *      f) If the maximum y-coordinate offset value has been surpassed then
     *         the for-loop ends.
     * 4) List pattern is closed and returned.
     * 
     * @param context Instance of the current Netlogo execution environment 
     *                object.
     * @param listPattern A ListPattern object to be populated.
     * @param maxXCorOffset The maximum number of patches east of the calling
     *                      turtle that can be "seen".
     * @param maxYCorOffset The maximum number of patches north of the calling
     *                      turtle that can be "seen".
     * @return ListPattern A populated ListPattern object.
     */
    private static ListPattern chrestTileworldBuildListPattern(Context context, ListPattern listPattern, int maxXCorOffset, int maxYCorOffset){
        
        /////////////////////////////////////
        ///// SET LIST PATTERN MODALITY /////
        ////////////////////////////////////
        listPattern.setModality(Modality.VISUAL);
        
        /////////////////////////////////
        ///// POPULATE LIST PATTERN /////
        /////////////////////////////////
        for( int xCorOffset = maxXCorOffset * -1, yCorOffset = maxYCorOffset * -1; yCorOffset <= maxYCorOffset;){
            
            try {
                org.nlogo.agent.Agent agent = (org.nlogo.agent.Agent)context.getAgent();
                org.nlogo.agent.Patch patch = agent.getPatchAtOffsets(xCorOffset, yCorOffset);
                if( patch.turtleCount() > 0 ){
                    Iterator<org.nlogo.agent.Turtle> turtlesOnPatch = patch.turtlesHere().iterator();

                    while(turtlesOnPatch.hasNext()) {
                        Turtle turtle = turtlesOnPatch.next();
                        
                        if( turtle.id() != agent.id ){
                            String turtleBreed = turtle.getBreed().printName();

                            if( "tiles".equalsIgnoreCase( turtleBreed ) ){
                                listPattern.add( new ItemSquarePattern("T", xCorOffset, yCorOffset) );
                            }
                            if( "holes".equalsIgnoreCase( turtleBreed ) ){
                                listPattern.add( new ItemSquarePattern("H", xCorOffset, yCorOffset) );
                            }
                            if( "chrest-agents".equalsIgnoreCase( turtleBreed ) ){
                                listPattern.add( new ItemSquarePattern("A", xCorOffset, yCorOffset) );
                            }
                        }
                    }
                }
            } catch (AgentException ex) {
                Logger.getLogger(NetlogoChrestInputOutputInterface.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            xCorOffset++;
            if( xCorOffset > maxXCorOffset ){
                yCorOffset++;
                xCorOffset = maxXCorOffset * -1;
            }
        }

        //////////////////////////////////////////
        ///// FINISH AND RETURN LIST PATTERN /////
        //////////////////////////////////////////
        listPattern.setFinished();
        return listPattern;
    }
    
    /**
     * Returns a turtle's CHREST instance.
     * 
     * @param context Instance of the current Netlogo execution environment 
     *                object.
     * @return Chrest An instance of the calling turtle's CHREST object.
     * @throws AgentException 
     */
    public static Chrest getTurtlesChrestInstance(Context context) throws AgentException{
        org.nlogo.agent.Agent agent = getAgent(context);
        return (Chrest)agent.getBreedVariable(NetlogoChrestInputOutputInterface.CHREST_INSTANCE_CHREST_AGENT_BREED_VAR_NAME);
    }
}
