import java.util.logging.Level;
import java.util.logging.Logger;

import jchrest.architecture.*;

import org.nlogo.api.*;

/**
 * Creates a new Chrest.java object instance and assigns it to the calling 
 * turtle's CHREST instance breed variable.
 * 
 * @author Martyn Lloyd-Kelly <mlk5060@liverpool.ac.uk>
 */
public class InstantiateChrestInTurtle extends DefaultCommand {
    
    @Override
    public void perform(Argument args[], Context context) throws ExtensionException, LogoException {
        try {
            org.nlogo.agent.Agent agent = (org.nlogo.agent.Agent)context.getAgent();
            agent.setBreedVariable(NetlogoChrestInputOutputInterface.CHREST_INSTANCE_CHREST_AGENT_BREED_VAR_NAME, new Chrest());
        } catch (AgentException ex) {
            Logger.getLogger(InstantiateChrestInTurtle.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}