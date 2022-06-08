package ericvh.TDT;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/** Class WorkFiles dispatches the text files after parsing to the Tokenizer
 * and then sends the tokenized files to InputOutput to be saved on disk.
 * @author  Eric Van Horenbeeck
 * Created on 2 juni 2004, 21:14
 */
public class WorkFiles implements Serializable
{
    // The html-parsed text.
    private final String mTextPage;
    // The filename.
    private String mFileName;
    // Array with total tokens and total types.
    private int[] mTotToken;
    // Tokenized text in a map with its indices.
    private Map mWordMap;
    // Number of processed files.
    private static int mCountFiles;
    private final Tokenizer tok = new Tokenizer();
    private final InputOutput io = new InputOutput();
    
    /** Constructor called by the NewsParser.
     * @param text : String with the text to tokenize
     * @param fileName : filename used to save the text elements (String)
     */
    public WorkFiles(String text, String fileName)
    {
        this.mTextPage = text;
        this.mFileName = fileName;
        mWordMap = new HashMap();
        mCountFiles++;
    }
    
    /** extract: This workFile is to be tokenized; total tokens and total 
     * token-type are retrieved and writeResult is instructed to save the data.
     */
    public void extract()
    {
        // Maximum number of tokens to extract. 'Max = 0' extracts all tokens 
        // from the input.
        int max = 0;
        try
        {
            mWordMap = tok.extractTokens(mTextPage, max);
            mTotToken = tok.getTokenTypeCount();
        }
        catch(Exception e)
        {
            System.out.println("Tokenizing " + mFileName + " did not succeed");
            e.printStackTrace(System.err);
        }
        writeResult();
    }
    
    /** writeResult: The summary of this workFile is shown on the GUI; the map 
     * with the tokenized text and its  indices (positions in the text) is send 
     * to InputOutput to be saved on disk as a * '*.tok' (tokenized) or '*.emp'
     * (empty) file.
     */
    private void writeResult()
    {
        try
        {
            ApplicationManager.clearText();
            ApplicationManager.showText("* Files processed: " + mCountFiles + " *", 0);
            io.writeTokenizedText(mWordMap, mFileName, mTotToken[0], mTotToken[1]);
        }
        catch (IOException e)
        {
            System.out.println("Could not transfer the results for " + mFileName);
            e.printStackTrace(System.err);
        }
        mWordMap = null;
        mFileName = null;
        mTotToken = null;
    }
    
}
