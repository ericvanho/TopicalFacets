package ericvh.TDT;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;


/** Class Reconstructor (partially) reconstructs a text with the arcs associated
 * with this document and sorted on their indexes.
 * @author Eric Van Horenbeeck
 * Created on 27 januari 2005, 14:48
 */
public class Reconstructor
{
    private final Map mArcsMap;
    private final ArcsTable mArcsTable;
    // Position to start coloring the text.
    private int mOffset;
    
    /**Constructor
     * @param at : the origninal arcsTable linked to this dataGraph needed to
     * reconstruct the text.
     */
    public Reconstructor(ArcsTable at)
    {
        mArcsTable = at;
        mArcsMap = mArcsTable.getFullArcsMap();
    }
    
    /** reconstructText: Reflects the original document. Reconstruction is limited
     * to a maximum of 'maxTokens' tokens (not lines!) as set by the user in 
     * the GUI. Default is none.
     * @param docNr : the unique document number (int)
     * @param max : the number of tokens in a reconstructed text.
     * @return String with the (partially) reconstructed text
     */
    public String reconstructText(int docNr, int max)
    {
        StringBuilder reconstructedText = new StringBuilder();
        LabelTable allLabels = ApplicationManager.getLabels();
        // Maximum number of tokens in the reconstructed text (none, 100, 200 or all).
        int maxTokens = max;
        if (maxTokens == 0) return "";
        if(maxTokens > 200) maxTokens = 99999;
        // Counts the number of tokens on a line.
        int cnt = 0;
        // String with headline.
        String headLine;
        String blank = " ";
        String previousLabel = "";
        headLine = switch (maxTokens) {
            case 100 -> "\nPartially reconstructed text (first 100 tokens shown) " +
                "for document: ";
            case 200 -> "\nPartially reconstructed text (first 200 tokens shown) " +
                "for document: ";
            case 99999 -> "\nReconstructed text (all tokens shown) " +
                "for document: ";
            default -> "\nReconstructed text (first " + maxTokens + " tokens shown) "
                + "for document: ";
        };
        mOffset += headLine.length();
        try
        {
            // Opens the appropriate tables.
            String docLabel = ApplicationManager.getDocTable().getFilename(docNr) 
                    + " (" + docNr + ")";
            reconstructedText.append(headLine).append(docLabel).append('\n');
            List arcs = mArcsTable.getArcsInDoc(mArcsMap, docNr);
            // Sorts the arcs on their position in the text.
            List sortedArcs = ArcsTable.sortArcsOnPosition(arcs);
            // Composes the text String.
            Iterator arc_itr = sortedArcs.listIterator();
            reconstruct:
                while (arc_itr.hasNext())
                {
                    cnt++;
                    Arc currentArc = (Arc) arc_itr.next();
                    Integer v1Idx = currentArc.getVertex1Index();
                    Integer v2Idx = currentArc.getVertex2Index();
                    String label1 = allLabels.getVertexLabel(v1Idx);
                    String label2 = allLabels.getVertexLabel(v2Idx);
                    if(label1.equals(previousLabel)) label1 = "";
                    else reconstructedText.append(blank);
                    previousLabel = label2;
                    reconstructedText.append(label1).append(blank).append(label2);
                    // Limits the reconstruction to maximum 'maxTokens' (int) tokens.
                    if(cnt == maxTokens)
                    {
                        reconstructedText.append("(...) ");
                        break;
                    }
                }
        }
        catch(Exception e)
        {
            ApplicationManager.showText("Exception while reconstructing a text", 0);
            e.printStackTrace(System.err);
        }
        return reconstructedText.toString();
    }
    
    /** applyColor: Changes the color of informative tokens into blue italic or
     * into red italic in the reconstructed documents, depending on the color flag.
     * @param textPane : a pane with a styled text document.
     * @param tokenList : List with tokens (String) to be colored in the text.
     * @param colorFlag : the flag indicates what color to apply to the token 
     * in the text area (int).
     */
    public void applyColor(JTextPane textPane, List tokenList, int colorFlag)
    {
        try
        {
            StyledDocument doc =  textPane.getStyledDocument();
            int endPos = doc.getLength();
            String text = doc.getText(0, endPos);
            // Iterates over the list to look for the special tokens in the text.
            Iterator tokens_itr = tokenList.listIterator();
            while(tokens_itr.hasNext())
            {
                // Blanks added to force pattern matching on whole tokens.
                String pattern = " " + tokens_itr.next().toString() + " ";
                int start = mOffset;
                int length = pattern.length();
                // Applies the style characteristics if a token was found.
                while ((start = text.indexOf(pattern, start)) >= 0)
                {
                    if(colorFlag == 1) doc.setCharacterAttributes(start, length,
                            textPane.getStyle("Red Italic"), false);
                    if(colorFlag == 0) doc.setCharacterAttributes(start, length,
                            textPane.getStyle("Blue Italic"), false);
                    start += length;
                }
            }
        }
        catch(BadLocationException ble)
        {
            ApplicationManager.showText("Bad location exception", 0);
            ble.printStackTrace(System.err);
        }
        catch(Exception e)
        {
            ApplicationManager.showText("Exception while coloring a text", 0);
            e.printStackTrace(System.err);
        }
    }
    
    /** setOffset: Indicates where to start the text to highlight in order 
     * to avoid coloring the table header.
     * @param offset : starting point of the coloring area (int).
     */
    public void setOffset(int offset)
    {
        mOffset = offset + 2;
    }
    
}
