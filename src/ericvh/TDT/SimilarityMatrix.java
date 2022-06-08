package ericvh.TDT;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;

/** Class SimilarityMatrix makes a list implementation of a symmetric sparse 
 * document-by-document matrix with non-zero DocNodes of the form: row, column,
 * values and shared arcs. This class also calculates the mean and standard 
 * deviation of the similarity values in the matrix.
 * @author Eric Van Horenbeeck
 * Created on 21 maart 2005, 0:58
 * Revision: September 8, 2006
 */
public class SimilarityMatrix implements Serializable
{
    // Sparse symmetric document-by-document matrix as a linked list.
    private LinkedList mSimNodeList;
    // Similarity value components for this matrix.
    private final double[] mSimValueArray;
    
    /** Constructor.
     */
    public SimilarityMatrix()
    {
        mSimNodeList = new LinkedList();
        mSimValueArray = new double [3];
    }
    
    /** addDocNode: SimilarityMatrix Class adds a new node to the list and adds 
     * the values send by the DocSimilarity class to an array.
     * @param node : a DocNode instance holding row (int), column (int) and 
     * some non-zero metrics used to calculate the similarity value: simV, 
     * simW, alpha (all Double).
     */
    public void addDocNode(DocNode node)
    {
        mSimNodeList.add(node);
        addSimilarityValue(node.getDocSimilarity());
    }
    
    /** addSimilarityValue: Adds the similarity value for this node to an array. 
     * This array is either not yet used or has values for these three elements:
     * the sum of the similarity values, the sum of the similarity values squared
     * and a counter. Allows the calculation of the mean and the standard deviation
     * of the values.
     * @param simValue : the individual similarity value of a node (double)
     */
    private void addSimilarityValue(double simValue)
    {
        double sum;
        if(simValue > 0)
        {
            sum = simValue;
            double sqrSum = sum * sum ;  // Squares the individual similarity value.
            
            if(Math.abs(mSimValueArray [0] - 0) < 0.0000001)
            {
                // Initializes running totals and counter
                // Sum of the individual similarity values.
                mSimValueArray [0] = sum;     
                // Sum squared.
                mSimValueArray [1] = sqrSum;  
                // Counter of the number of instances summed.
                mSimValueArray [2] = 1;       
            }
            else
            {
                // Adds new value to the running total.
                mSimValueArray [0] += sum;
                // Adds new squared sum to the running total.
                mSimValueArray [1] += sqrSum; 
                // Increases counter with 1.
                mSimValueArray [2] += 1;      
            }
        }
    }
    
    /** getMean: Getter returns the mean of the similarity values for this matrix.
     * @return mean (double).
     */
    public double getMean()
    {
        return mSimValueArray [0] / mSimValueArray [2];
    }
    
    /** getStDev: Getter returns the standard deviation of the similarity values.
     * @return standard deviation (double).
     */
    public double getStDev()
    {
        return StrictMath.sqrt(mSimValueArray [1] / mSimValueArray [2] - getMean()
                * getMean());
    }
    
    /** getNonZeroNodeCount: Returns the number of non-zero nodes in this matrix.
     * @return number of non-zero nodes (int).
     */
    private int getNonZeroNodeCount()
    {
        return mSimNodeList.size();
    }
    
    /** getDocNode: Returns the matrix cell identified by this row and column or 
     * 'null' if none was found.
     * @param row : the row of this cell (int).
     * @param col : the column of this cell (int).
     * @return the node in this matrix cell.
     */
    public DocNode getDocNode(int row, int col)
    {
        DocNode thisNode = new DocNode();
        Iterator matrix_itr = mSimNodeList.iterator();
        while(matrix_itr.hasNext())
        {
            thisNode = (DocNode) matrix_itr.next();
            // Looks for a node on this row-column intersection.
            if(thisNode.contains(row) && thisNode.contains(col)) return thisNode;
        }
        return thisNode;
    }
    
    /** extractDocNodes: Extracts nodes from the document-by-document matrix 
     * linked to this focus
     * document with a semantic value > e, an errorValue.
     * @param focusDoc : the document in focus (int).
     * @return LinkedList with nodes (DocNode).
     */
    public LinkedList extractDocNodes(int focusDoc)
    {
        double errorValue = 0.001;
        LinkedList linkedNodes = new LinkedList();
        Iterator matrix_itr = mSimNodeList.iterator();
        while(matrix_itr.hasNext())
        {
            DocNode node = (DocNode) matrix_itr.next();
            // Adds a node to the set if it contains the focus doc at the row-column
            // intersection and if it has a similarity value > e.
            if(node.contains(focusDoc) && node.getDocSimilarity() > errorValue)
            {
                linkedNodes.add(node);
            }
        }
        return linkedNodes;
    }
    
    /** findHighestValue: Returns the highest similarity value found in any node
     * of this document. If this high value occurs in the node with the focusDoc
     * then this doc will be assigned to the focusDoc cluster now being populated.
     * @param doc : document identification (Integer).
     * @return valueArray with at position '0' the doc-id where the highest 
     * similarity value was found. This value (double) at position '1'.
     */
    public Object[] findHighestValue(int doc)
    {
        double highScore = 0.0;
        int col = doc;
        Object[] valueArray = new Object[2];
        valueArray[0] = col;
        valueArray[1] = highScore;
        LinkedList nodeList = extractDocNodes(doc);
        Iterator node_itr = nodeList.iterator();
        while(node_itr.hasNext())
        {
            DocNode thisNode = (DocNode) node_itr.next();
            double simValue = thisNode.getDocSimilarity();
            // Replaces the old value and node coordinate when a higher similarity 
            // value was found.
            if(simValue > ((Double) valueArray[1]))
            {
                valueArray[0] = thisNode.getOtherMatrixCoordinate(col);
                valueArray[1] = simValue;
            }
        }
        return valueArray;
    }
    
    /** getNodeListSize: Returns the number of non-zero cells for this focus document.
     * @param focusDoc : the document in focus (int).
     * @return the number of nodes (int).
     */
    public int getNodeListSize(int focusDoc)
    {
        LinkedList linkedNodes = extractDocNodes(focusDoc);
        return linkedNodes.size();
    }
    
    /** getSimMatrixList: Getter returns the linkedList with the nodes from the 
     * document-by-document matrix.
     * @return simMatrixList, a linkedList with nodes.
     */
    public LinkedList getSimMatrixList()
    {
        return mSimNodeList;
    }
    
    /** discard: Discards elements no longer needed.
     */
    public void discard()
    {
        mSimNodeList = null;
    }
}
