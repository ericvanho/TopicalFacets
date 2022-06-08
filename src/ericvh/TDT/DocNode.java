package ericvh.TDT;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;


/** Class DocNode instance is a cell with a non-zero value in a symmetric 
 * document-by-document matrix. There are n(n-1)/2 nodes possible, where n is 
 * the number of documents. The node knows its coordinates in the matrix 
 * (the document in the row and the document in the colum) and the components
 * of the similarity calculation. This node also keeps two sets: one with 
 * shared vertices and one with shared topical facets.
 * @author Eric Van Horenbeeck
 * Created on 21 maart 2005, 9:39
 */
public class DocNode implements Serializable
{
    // Vertex similarity (simV) expresses how many vertices two document graphs 
    // have in common.
    private double mSimV;
    // Walk similarity indicates how similar the relations between the vertices
    // are. The relation is called 'weak' because no full mapping is sought 
    // between the walks in doc 1 and doc 2. It suffices that parts of a walk 
    // in doc 1 (one or more vertices) belong to a walk in the other document.
    private double mSimW;
    // The coefficient alpha expresses the ratio of information contained only 
    // in the vertices.
    // It is calculated as the proportion between the number of the shared 
    // vertices and the number of elements connected to walks of doc1 and doc2.
    private double mAlpha;
    // Beta: 1 - alpha.
    private double mBeta;
    // Set of vertices shared by both documents.
    private Set mVShared;
    // Set of topical facets shared by both documents.
    private Set mFShared;
    // Coordinates of this cell.
    private int thisRow;
    private int thisColumn;
    
    /** Default DocNode Constructor
     */
    public DocNode()
    {  }
    
    /** Constructor called by DocSimilarity
     * @param row : the row number of the matrix is the id of the first 
     * document (int).
     * @param column : the column number of the matrix is the id of the second 
     * document (int).
     * @param simV : vertex similarity metric (double).
     * @param simW : walk similarity metric (double).
     * @param alpha : the vertex/weakly connected ratio (double).
     * @param sharedVertices : SortedSet of vertex keys (Integer) shared by 
     * doc 1 and doc 2.
     * @param sharedFacets : Set of facet keys (Integer) shared by doc 1 
     * and doc 2.
     */
    public DocNode(int row, int column, double simV, double simW, double alpha,
            SortedSet sharedVertices, Set sharedFacets)
    {
        thisRow = row;
        thisColumn = column;
        mSimV = simV;
        mSimW = simW;
        mAlpha = alpha;
        mBeta = 1 - mAlpha;
        mVShared = new HashSet(sharedVertices);
        mFShared = new HashSet(sharedFacets);
    }
    
    /** getDocSimilarity: Getter returns the similarity value in a
     * document-by-document matrix at the  row-column intersection. 
     * The total similarity value is the product of the value of having vertices
     * in common (simV) and the value of having similar 'walks' (simW).
     * Two documents having some words in common will be considered 'similar' 
     * when furthermore those words are embedded in shared constructions such 
     * as phrases or sentences.
     * @return the calculated similarity value at this node in the matrix 
     * (double).
     */
    public double getDocSimilarity()
    {
        return mSimV * (mAlpha + mBeta * mSimW);
    }
    
    /** getSimVComponent: Getter returns the simV (vertex similarity) component 
     * of this node.
     * @return the simV value (double).
     */
    public double getSimVComponent()
    {
        return mSimV;
    }
    
    /** getSimWComponent: Getter returns the simW (walk similarity) component 
     * of this node.
     * @return the simW value (double).
     */
    public double getSimWComponent()
    {
        return mSimW;
    }
    
    /** getAlphaComponent: Getter returns the alpha (information carried by 
     * vertices only) compoment of this node.
     * @return the alpha value (double).
     */
    public double getAlphaComponent()
    {
        return mAlpha;
    }
    
    /** getMatrixRow: Getter returns the row position (a doc-id) of this 
     * node in the matrix.
     * @return the row position (int)
     */
    public int getMatrixRow()
    {
        return thisRow;
    }
    
    /** getMatrixColumn: Getter returns the column position (a doc-id) of 
     * this node in the matrix.
     * @return the column position (int).
     */
    public int getMatrixColumn()
    {
        return thisColumn;
    }
    
    /** getMatrixCoordinate: Getter returns a linkedList with the coordinates
     * of this node in the matrix.
     * @return the coordinates of this node (int).
     */
    public LinkedList getMatrixCoordinate()
    {
        LinkedList coordinate = new LinkedList();
        coordinate.add(thisRow);
        coordinate.add(thisColumn);
        return coordinate;
    }
    
    /** contains: Returns boolean 'true' if this matrixNode is on this row or 
     * column.
     * @param rowOrCol : the row or column of this node (int).
     * @return boolean 'true' or 'false'
     */
    public boolean contains(int rowOrCol)
    {
        return getMatrixCoordinate().contains(rowOrCol);
    }
    
    /** getOtherMatrixCoordinate: Returns the other coordinate of this node, 
     * given one of both.
     * @param rowOrCol : the row or column of this node (int).
     * @return the other coordinate (int).
     */
    public int getOtherMatrixCoordinate(int rowOrCol)
    {
        if(rowOrCol == thisRow) return thisColumn;
        else return thisRow;
    }
    
    /** getSharedVertices: Returns a set of vertices shared by doc1 and doc2.
     * @return HashSet with vertex keys (Integer).
     */
    public Set getSharedVertices()
    {
        return mVShared;
    }
    
    /** getSharedFacets: Returns a set of topical facets shared by doc1 and doc2.
     * @return Set with facet keys (Integer).
     */
    public Set getSharedFacets()
    {
        return mFShared;
    }
    
}
