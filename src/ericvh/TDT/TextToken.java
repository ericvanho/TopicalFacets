package ericvh.TDT;

import java.io.Serializable;

/** Class TextToken creates instances for every token in a text, with its position
 * in this text and the collection it belongs to. There are several similar 
 * TextTokens if a token appears more than once in the same text. 
 * Their position however, will be different.
 * @author  Eric Van Horenbeeck 
 */
public class TextToken implements Serializable
{
    // Label of the token.
    private final String mLabel; 
    // Position of the token in a text.
    private final int mPosition;       
    // Key to the collection.
    private final String mCollectionKey; 
    
    /** Constructor
     * @param token : the label of this token (String)
     * @param position : index of this token in the text (int)
     * @param collKey : key composed with the source and date of the filename,
     * but not its the serial number. Docs with the same collKey-key belong to 
     * the same collection (String).
     */
    public TextToken(String token, int position, String collKey)
    {
        this.mLabel = token;
        this.mPosition = position;
        this.mCollectionKey = collKey;
    }
    
    /** getToken: Getter of a label.
     * @return label (token as String) 
     */
    public String getToken()
    {
        return mLabel;
    }
    
    /** getPosition: Getter of the position-index of a token.
     * @return position (int)
     */
    public int getPosition()
    {
        return mPosition;
    }
    
    /** getCollectionKey: Getter of the source-date key of this token.
     * @return collectionKey (String)
     */
    public String getCollectionKey()
    {
        return mCollectionKey;
    }
    
}
