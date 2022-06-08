package ericvh.TDT;

import java.io.Serializable;


/** Class Arc instance is a line between two points (the vertices) in a text. 
 * An arc is an edge with direction and weight. Direction is either incoming 
 * (the vertex to the left) or outgoing (vertex to the right). The weight is a 
 * measure of the informative importance of a link between two tokens. The arc 
 * weight is computed using the frequency of the link and the weight of the 
 * linked tokens, its vertices. An arc has an position index, allowing to sort
 * arcs inside a document.
 * @author  Eric Van Horenbeeck
 */
public class Arc implements Serializable
{
    private int mVertex1Idx;    // First vertex (point) left of the arc 
    //(directed link or line).
    private int mVertex2Idx;    // Second vertex (point) to the right of the arc.
    private int mDocNr;         // The unique document number.
    private double mArcWeight;  // Weight showing importance of this arc.
    private int mPosition;      // The position of this arc in a document.
    
    /** Default constructor
     */
    public Arc()
    { }
    
    /** Constructor
     * @param position : the position in a document (int).
     * @param indx1 : first point (vertex-id) of this arc (int).
     * @param indx2 : second point (vertex-id) of this arc (int).
     * @param docNr : the document of this arc (int).
     * @param weight : the infomative value of this arc (double).
     */
    public Arc(int position, int indx1, int indx2, int docNr, double weight)
    {
        mPosition = position;
        mVertex1Idx = indx1;
        mVertex2Idx = indx2;
        mDocNr = docNr;
        mArcWeight = weight;
    }
    
    /** getVertex1Index: Getter returns the first point (vertex) of this arc.
     * @return first point (int)
     */
    public int getVertex1Index()
    {
        return mVertex1Idx;
    }
    
    /** getVertex2Index: Getter returns second arc point (vertex).
     * @return second point (int)
     */
    public int getVertex2Index()
    {
        return mVertex2Idx;
    }
    
    /** getArcId: Getter returns the key of this arc.
     * @return the key identifying this arc (String).
     */
    public String getArcId()
    {
        return getVertex1Index() + "*" + getVertex2Index();
    }
    
    /** getArcDocNr: Getter returns the unique document number of this arc.
     * @return document number (int)
     */
    public int getArcDocNr()
    {
        return mDocNr;
    }
    
    /** getArcCollection: Getter returns the collection key of the document of this arc.
     * @return collection key (String)
     */
    public String getArcCollection()
    {
        return ApplicationManager.getCollTable().getDocsCollectionKey(getArcDocNr());
    }
    
    /** getArcPosition: Getter returns the position of this arc in a document.
     * @return the position of this arc (int)
     */
    public int getArcPosition()
    {
        return mPosition;
    }
    
    /** getArcWithPosition: Getter returns this arc when its position is given.
     * @param position : the position of this arc (int)
     * @return this arc if this is its position, null otherwise.
     */
    public Arc getArcWithPosition(int position)
    {
        if (position == mPosition) return this;
        else return null;
    }
    
    /** getOtherVertex: Looks up if this arc contains this vertex. 
     * Returns the other vertex in this arc when true or zero when not.
     * @param index : the token-type index of one of the two vertices (int)
     * @return vertex-id on the other side or zero (int)
     */
    public int getOtherVertex(int index)
    {
        int index1 = getVertex1Index();
        int index2 = getVertex2Index();
        if (index1 == index) return index2;
        else if (index2 == index) return index1;
        else return 0;
    }
    
    /** getArcWeight: Getter returns the weight of this arc.
     * @return arc weight (double)
     */
    public double getArcWeight()
    {
        return mArcWeight;
    }
    
    /** containsIndexFirst: Returns 'true' if the first index of this arc is 
     * the same as the argument.
     * @param idx : index identifying a vertex (int)
     * @return boolean 'true' if first index matchs argument
     */
    public boolean containsIndexFirst(int idx)
    {
        return mVertex1Idx == idx;
    }
    
    /** containsIndexSecond: Returns 'true' if the second index of this arc 
     * is the same as the argument.
     * @param idx : index identifying a vertex (int)
     * @return boolean 'true' if the second index matches argument
     */
    public boolean containsIndexSecond(int idx)
    {
        return mVertex2Idx == idx;
    }
    
    /** containsEither: Returns 'true' if this arc contains one or both of 
     * this vertices.
     * @param idx1 : index identifying first vertex (int)
     * @param idx2 : index identifying second vertex (int)
     * @return boolean 'true' if this arc has one or both of this vertices.
     */
    public boolean containsEither(int idx1, int idx2)
    {
        return containsVertex(idx1) || containsVertex(idx2);
    }
    
    /** containsVertex: Returns 'true' if this arc contains this vertex v on 
     * either the left or the right side.
     * @param idx : index identifying a vertex (int)
     * @return boolean 'true' if this arc has vertex v, 'false' if not.
     */
    public boolean containsVertex(int idx)
    {
        return (mVertex1Idx == idx || mVertex2Idx == idx);
    }
    
    /** containsDummy: Returns 'true' if this arc contains a dummy vertex (0)
     * on the left or on the right side.
     * @return boolean 'true' if this arc has a dummy vertex, 'false' if not.
     */
    public boolean containsDummy()
    {
        return containsVertex(0);
    }
    
    /** containsDocNr: Returns 'true' if this arc contains this document number.
     * @param docNr : unique document identifier (int)
     * @return boolean 'true' if this arc has this docNr, 'false' if not.
     */
    public boolean containsDocNr(int docNr)
    {
        return (mDocNr == docNr);
    }
    
    /** setArcWeight: Sets the weight of this arc to a new value.
     * @param newWeight : new arc weight (double)
     */
    public void setArcWeight(double newWeight)
    {
        this.mArcWeight = newWeight;
    }
    
    /** addWeight: Adds a weight to the existing value. Calculated by ArcsTable 
     * with the info value of the vertices and by incrementing the frequency 
     * count at the creation of the arc in the GraphBuilder class.
     * @param weight : info value (double)
     */
    public void addWeight(double weight)
    {    
        mArcWeight += weight;
    }
    
    /** toLabel: Returns the arc represented as a string with the two labels 
     * of the vertices it connects.
     * @return arc as a String with two labels
     */
    public String toLabel()
    {
        return ApplicationManager.getLabels().getVertexLabel(mVertex1Idx) + " " +
                ApplicationManager.getLabels().getVertexLabel(mVertex2Idx);
    }
    
    /** toString: Returns String representation of an arc using its vertex indices.
     * @return arc and index as a String
     */
    @Override
    public String toString()
    {
        return mVertex1Idx + " - " + mVertex2Idx;
    }
    
}
