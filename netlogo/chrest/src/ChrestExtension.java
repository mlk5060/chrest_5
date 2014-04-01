import org.nlogo.api.*;

/**
 * Primitive manager for CHREST Netlogo extension 
 * {@link https://github.com/NetLogo/NetLogo/wiki/Extensions-API#2-write-a-classmanager}.
 * 
 * @author Martyn Lloyd-Kelly <mlk5060@liverpool.ac.uk>
 */
public class ChrestExtension extends DefaultClassManager {
    
    @Override
    public void load(PrimitiveManager primitiveManager) {
        primitiveManager.addPrimitive("setup-chrest-extension", new SetupChrestExtension());
        primitiveManager.addPrimitive("instantiate-chrest-in-agent", new InstantiateChrestInTurtle());
        primitiveManager.addPrimitive("recognise-and-learn-environment", new RecogniseAndLearnEnvironment());
    }
}