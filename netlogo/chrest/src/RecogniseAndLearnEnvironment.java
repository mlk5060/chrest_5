import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.nlogo.api.*;

import jchrest.architecture.Chrest;
import jchrest.lib.*;

/**
 * Netlogo extension primitive that generates a list pattern for the calling 
 * turtle given an integer indicating how many patches north/east can be "seen" 
 * by the turtle.  The turtle will then recognise and learn the list pattern 
 * using the "recogniseAndLearn()" function of its own CHREST instance resulting 
 * in familiarisation or discrimination of the turtle's LTM.
 * 
 * @author Martyn Lloyd-Kelly <mlk5060@liverpool.ac.uk>
 */
public class RecogniseAndLearnEnvironment extends DefaultCommand {

    /**
     * Required to allow one integer to be passed as parameters to this class 
     * when the extension primitive that calls this class is used in a Netlogo 
     * model.
     * 
     * @return 
     */
    public Syntax getSyntax() {
        return Syntax.commandSyntax( new int[] {Syntax.NumberType()} );
    }
    
    @Override
    public void perform(Argument args[], Context context) throws ExtensionException {  
        try {
            Chrest chrest = NetlogoChrestInputOutputInterface.getTurtlesChrestInstance(context);
            chrest.recogniseAndLearn(NetlogoChrestInputOutputInterface.buildListPatternFromNetlogoPercept( context, args[0].getIntValue(), args[0].getIntValue() ) );
            
            System.out.println( "Number of LTM nodes for agent with ID " + context.getAgent().id() + " = " + chrest.getTotalLtmNodes() );
        } catch (LogoException ex) {
            Logger.getLogger(RecogniseAndLearnEnvironment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AgentException ex) {
            Logger.getLogger(RecogniseAndLearnEnvironment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
