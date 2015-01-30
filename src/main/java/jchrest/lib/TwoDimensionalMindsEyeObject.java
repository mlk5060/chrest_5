package jchrest.lib;

/**
 *
 * @author martyn
 */
public interface TwoDimensionalMindsEyeObject {
  
  /**
   * Essentially, a constructor for a concrete TwoDimensionalMindsEyeObject.
   * 
   * @param identifier A string used as a token to identify the object.
   * @param domainSpecificXCor The x-coordinate of the object in its domain when 
   * it is created.
   * @param domainSpecificYCor The y-coordinate of the object in its domain when
   * it is created.
   * @return 
   */
  public void instantiateObject(String identifier, int domainSpecificXCor, int domainSpecificYCor);
  
  public int getDomainSpecificXCor();
  public int getDomainSpecificYCor();
  public String getIdentifier();
  public int getTerminus();
  
  public void setTerminus(int currentDomainTime);
}
