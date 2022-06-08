package ericvh.TDT;

import java.util.Map;


/** Class TimeRetrieval constructs a timeline inside the scope of this session 
 * counting documents and sources related to the input.
 * Not yet implemented.
 * @author Eric Van Horenbeeck
 * Created on 7 juli 2005, 15:39
 */
public class TimeRetrieval
{
    private Map mTokenMap;
    
    /** Constructor
     */
    public TimeRetrieval()
    {
 System.out.println("Time retrieval not yet implemented");
    }

    public Map getTokenMap()
    {
        return mTokenMap;
    }

    public void setTokenMap(Map tokenMap)
    {
        this.mTokenMap = tokenMap;
    }
    
}
