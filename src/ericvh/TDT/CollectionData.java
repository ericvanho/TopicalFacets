package ericvh.TDT;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;


/** Class CollectionData creates an instance for every collection that holds 
 * two containers: first an array with the infovalue necessary to compute the 
 * average informative weight (mean) and the background noise factor that 
 * represents a constant minimum level to be subtracted from the info value of 
 * the vertices. The infovalue is used to discriminate between common words
 * and informative words. A second set keeps the unique document numbers and 
 * members. Instances of CollStats are stored in a CollectionTable.
 * @author Eric Van Horenbeeck
 * Created on 12 augustus 2004, 0:49
 * Revision: Sep 26, 2006
 */
public class CollectionData implements Serializable
{
    // Info value components for this collection.
    private final double[] mCollWeightArray;
    // Document nrs, members of this collection.
    private final SortedSet mDocNrSet;
    // Background noise: simulating a random loss of information. Arbitrarely 
    // set at 5% in the Constructor.
    private final double backgroundNoise;
    
    /** Constructor
     * Instances of this class have two containers: one for the info value 
     * components and one for the document numbers.
     */
    public CollectionData()
    {
        this.backgroundNoise = 0.1;
        mCollWeightArray = new double [6];
        // Sets counter.
        mCollWeightArray [2] = 0; 
        mDocNrSet = new TreeSet();
    }
    
    /** addDocNr: Adds a unique document number as member of this collection.
     * @param docNr : a unique document number (Integer)
     */
    public void addDocNr(int docNr)
    {
        mDocNrSet.add(docNr);
    }
    
    /** getDocNrSet: Getter returns a set with document numbers populating 
     * this collection.
     * @return set with docNrs (Integers) in this collection
     */
    public SortedSet getDocNrSet()
    {
        return mDocNrSet;
    }
    
    /** getDocCount: Getter returns of the size of this collection 
     * (number of documents).
     * @return number of files in this collection (int)
     */
    public int getDocCount()
    {
        return mDocNrSet.size();
    }
    
    /** addCollectionWeight: Adds the info value (weight) for this collection 
     * to an array. This array is either empty (not yet used) or has values for
     * these five elements: the sum of info values; the sum of the info values 
     * squared; a counter; the cutOffValue computed with the other elements and
     * the normalization factor to compare individual values over different 
     * collections. Values (weights) send by the InfoValueCalculator are added 
     * to this array.
     * @param infoWeight : the individual info value of a token (double)
     */
    public void addCollectionWeight(double infoWeight)
    {
        double sum;
        if(infoWeight > 0)
        {
            sum = infoWeight;  
            // Squares the individual infoWeight (TfIdf).
            double sqrSum = sum * sum ; 
                    
            // Initializes the value array.
            if(mCollWeightArray [2] == 0)
            {
                // Initializes running totals and counter.
                // Sum of the individual TfIdf scores.
                mCollWeightArray [0] = sum;      
                // Sum squared.
                mCollWeightArray [1] = sqrSum;   
                // Counter of the number of instances summed.
                mCollWeightArray [2] = 1;       
                 // CutOff value (mean).
                mCollWeightArray [3] = 0;   
                // Minimum value.
                mCollWeightArray [4] = 9999.9 ;  
                 // Standard deviation.     
                mCollWeightArray [5] = 0;               
            }
            else
            {
                 // Adds new value to the running total.
                mCollWeightArray [0] += sum; 
                // Adds new squared sum to the running total.
                mCollWeightArray [1] += sqrSum;  
                  // Increases number counter with 1.
                mCollWeightArray [2] += 1;     
                // Looking for the smallest value.
                if(infoWeight > 0.001 && infoWeight < mCollWeightArray [4]) 
                    mCollWeightArray [4] = infoWeight; 
            }              
        }
    }
    
    /** setFinalInfoValue: Calculates the normalized cutOff value to 
     * discriminate between common words and informative tokens in this 
     * collection. The cutOff value is the mean of the summed info value of the 
     * tokens (TfIdf) in this collection. The normalization factor is the square
     * root over the summed squared values (not used in the current version).
     */
    public void setFinalInfoValue()
    {
        double totSum = mCollWeightArray [0];
        double totSqrSum = mCollWeightArray [1];
        double totCnt = mCollWeightArray [2];
        // The mean
        double mean = totSum / totCnt;
        // The cutOff value is the mean.
        mCollWeightArray[3] =  mean;                 
        // Background noise.
        mCollWeightArray [4] = mCollWeightArray [4] * 
                (1 + backgroundNoise * StrictMath.random());
      //  mCollWeightArray [4] = mCollWeightArray [4] * 1.10;
        // The Standard Deviation.
        mCollWeightArray[5] = StrictMath.sqrt(totSqrSum / totCnt - mean * mean);
    }
    
    /** getInfoCutOffValue: Getter returns the normalized cutOff info value for
     * this collection.
     * @return cutOff (double) the divide between informative and common tokens.
     */
    public double getInfoCutOffValue()
    {
        return mCollWeightArray[3];
    }
    
    /** getBackgroundNoise: Getter returns the background noise for this 
     * collection.
     * @return background noise (double).
     */
    public double getBackgroundNoise()
    {
        return mCollWeightArray[4];
    }
    
    /** getStandardDeviation: Getter returns the standard deviation for this
     * collection.
     * @return standard deviation normalized (double).
     */
    public double getStandardDeviation()
    {
        return mCollWeightArray[5];
    }
    
    /** removeOldValues: When the InfoValueCalculator wants to replace existing
     * values, the old ones have to be removed first by reseting the counter.
     */
    public void removeOldValues()
    {
        mCollWeightArray [2] = 0;
    }
    
}
