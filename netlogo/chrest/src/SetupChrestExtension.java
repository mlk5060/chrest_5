import java.util.logging.Level;
import java.util.logging.Logger;
import org.nlogo.api.*;
import org.nlogo.nvm.ExtensionContext;
import org.nlogo.nvm.Workspace;

/**
 * Netlogo CHREST extension primitive which is used to set-up the current 
 * Netlogo model to use the Chrest extension.  Specifically, the class does the
 * following:
 * 
 * 1) Sets the filename of the Netlogo model implementing the CHREST extension 
 *    to the appropriate global variable.
 * 
 * @author Martyn Lloyd-Kelly <mlk5060@liverpool.ac.uk>
 */
public class SetupChrestExtension extends DefaultCommand {
    @Override
    public void perform(Argument args[], Context context){
        try {
            
            /////////////////////////////////////////////////
            ///// SET MODEL FILENAME AS GLOBAL VARIABLE /////
            /////////////////////////////////////////////////
            ExtensionContext extensionContext = (ExtensionContext)context;
            Workspace workspace = extensionContext.workspace();
            String modelFilename = workspace.getModelFileName().replaceAll("\\s+|\\.nlogo", "");
            org.nlogo.agent.Agent agent = (org.nlogo.agent.Agent)context.getAgent();
            agent.world().setObserverVariableByName(NetlogoChrestInputOutputInterface.NETLOGO_MODEL_OBSERVER_AGENT_VAR_NAME, modelFilename);
            
        } catch (LogoException ex) {
            Logger.getLogger(SetupChrestExtension.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AgentException ex) {
            Logger.getLogger(SetupChrestExtension.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
