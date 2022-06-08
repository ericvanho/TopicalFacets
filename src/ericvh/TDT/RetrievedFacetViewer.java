package ericvh.TDT;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;


/** Class RetrievedFacetViewer. Brings a view on the topical facets activated 
 * for a document retrieval task. It shows the tokens from the seed document 
 * (the 'query') that are not linked to any topical facet in the dataGraph at 
 * this moment. The user can select some of the activated topical facets as 
 * negative instances or he can abort the retrieval operation when none of the
 * activated topical facets seem promissing.
 * 
 * @author Eric Van Horenbeeck
 * Created on 21 september 2005, 13:30
 * Revision: September 7, 2006
 */
public class RetrievedFacetViewer extends JPanel
{
    // User decision on the next action to take.
    private int mDecision;
    // Name of the list in focus.
    private static String mListName;
    // Three lists with topical facets and documents.
    private static JList mTokenList;
    private static JList mCommonList;
    private static JList mDocList;
    // LabelTable instance to deliver labels.
    private final LabelTable labels;
    // JCheckBox selection to keep the facetViewer or not after viewing.
    private JCheckBox keepViewBx;
    // Border.
    private final Border border = BorderFactory.createEtchedBorder(EtchedBorder
            .LOWERED);
    // Class to handle drag & drop of data from one list to another and back.
    private ArrayListTransferHandler arrayListHandler;
    
    /** Constructor
     */
    public RetrievedFacetViewer()
    {
        labels = ApplicationManager.getLabels();
    }
    
    /** makeView: Constructs a panel with three lists:
     * 1. with the known and unknown tokens from the seed document;
     * 2. with the common facets found in the seed document and in the dataGraph;
     * 3. with the topical facets selected by the user that should be eliminated
     * from the task.
     * todo : Allow searching in all three panels.
     */
    public void makeView()
    {
        arrayListHandler = new ArrayListTransferHandler();
        mDecision = -1;
        JPanel facetView = new JPanel();
        facetView.setLayout(new BoxLayout(facetView, BoxLayout.PAGE_AXIS));
        // 'Confirm' button ends the input of tasks and transfers the seedMap 
        // to the ApplicationManager.
        JButton confirmBtn = new JButton("Confirm");
        confirmBtn.setToolTipText("Negative instances will be removed");
        confirmBtn.addActionListener((ActionEvent event) -> 
        {
            confirmPerformed(event);
        });
        JButton continueBtn = new JButton("Continue");
        continueBtn.setToolTipText("Proceed without removing");
        continueBtn.addActionListener((ActionEvent event) -> {
            continueActionPerformed(event);
        });
        JButton abortBtn = new JButton("Abort");
        abortBtn.setToolTipText("Stops all activity");
        abortBtn.addActionListener((ActionEvent event) -> 
        {
            abortActionPerformed(event);
        });
        // The RetrievedFacetViewer remains visible (default).
        keepViewBx = new JCheckBox("Keep the Facet Viewer", true);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setMaximumSize(new Dimension(1300, 70));
        buttonPanel.setBorder(border);
        buttonPanel.add(keepViewBx);
        buttonPanel.add(Box.createRigidArea(new Dimension(380, 30)));
        buttonPanel.add(continueBtn);
        buttonPanel.add(confirmBtn);
        buttonPanel.add(abortBtn);
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new GridLayout(1,3));
        listPanel.add(setTokenList());
        listPanel.add(setCommonFacetList());
        listPanel.add(setDocScroll());
        facetView.add(listPanel);
        facetView.add(buttonPanel);
        facetView.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        // Tracks focus changes of the cursor.
        KeyboardFocusManager focusManager = KeyboardFocusManager.
                getCurrentKeyboardFocusManager();
        focusManager.addPropertyChangeListener((PropertyChangeEvent e) -> 
        {
            String prop = e.getPropertyName();
            if (("focusOwner".equals(prop) && (e.getNewValue() != null) &&
                    (e.getNewValue()) instanceof JList))
            {
                JList focus = (JList)e.getNewValue();
                mListName = focus.getName();
            }
        });
        ApplicationManager.addTabbedPane(facetView, 3);
        waitForDecision();
    }
    
    /** setTokenList: Query Expansion. Prepares a list with a maximum of 15 
     * facets (loosely) related to the tokens from the seed document. 
     * A mention is made when the query contains a token unknown to the 
     * application. Uses unknownTokens, a HashSet with tokens (String) unknown
     * to the dataGraph, collected by SeedInput and the tokenFacetMap, 
     * a HashMap with tokenId (Integer) as key and a SortedSet of shared 
     * facets (Integer).
     * @return JScrollPane with token list (JList).
     */
    private JScrollPane setTokenList()
    {
        Set unknownTokenSet = ApplicationManager.getUnknownTokens();
        SortedMap tokenFacetMap = TopicRetriever.getTokenFacetMap();
        ArrayList<String> tokenArray = new ArrayList<>();
        Iterator token_itr = tokenFacetMap.keySet().iterator();
        while(token_itr.hasNext())
        {
            Integer tokenKey = (Integer) token_itr.next();
            Set facetKeys = (TreeSet) tokenFacetMap.get(tokenKey);
            String token = labels.getVertexLabel(tokenKey);
            int keySetSize = facetKeys.size();
            int keyCount = 0;
            if(keySetSize == 0) token += " (no topical facets found in the "
                    + "scope of this session)";
            else
            {
                String keyString = " -> " + keySetSize + " facet bag: [ ";
                Iterator facet_itr = facetKeys.iterator();
                // Shows a maximum of 15 facets.
                while(facet_itr.hasNext() && keyCount < 15)
                {
                    Integer facet = (Integer) facet_itr.next();
                    keyString += facet.toString() + ", ";
                    keyCount++;
                }
                if(keySetSize >= 15) keyString += "... , ";
                // Removes last comma.
                keyString = keyString.substring(0, keyString.lastIndexOf(","))
                        + "]";
                token += keyString;
            }
            tokenArray.add(token);
        }
        int setSize = unknownTokenSet.size();
        Object[] unknownIds = unknownTokenSet.toArray();
        for (Object unknownId : unknownIds) {
            String unknownToken = unknownId.toString() + " (token unknown)";
            tokenArray.add(unknownToken);
        }
        Collections.sort(tokenArray);
        String unknownFound;
        unknownFound = switch (setSize) {
            case 0 -> "no unknown tokens";
            case 1 -> "1 token unknown";
            default -> setSize + " tokens unknown";
        };
        JLabel tokenHeader = new JLabel("<html><center>The query contained " +
                (setSize + tokenFacetMap.size()) + " recognized tokens ( "  
                + unknownFound + " )<br><I>First 15 linked topical facets "
                        + "shown</I></center></html>", JLabel.CENTER);
        tokenHeader.setBorder(border);
        String listOne = "listOne";
        JList<String> ThisTokenList = new JList(tokenArray.toArray());
        ThisTokenList.setName(listOne);
        ThisTokenList.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ThisTokenList.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        // Puts list in a scrollPane.
        JScrollPane scrollTokens = new JScrollPane(ThisTokenList);
        scrollTokens.setColumnHeaderView(tokenHeader);
        return scrollTokens;
    }
    
    /** setCommonFacetList: Prepares a list with the facets common to the 
     * seed document ('query') and the dataGraph.
     * Uses the facetArcMap, a LinkedHashMap with first a tokenCount as
     * key (Integer) then a second HashMap as value with a global facet 
     * key (Integer) and a LinkedHashSet of arc-keys (String) as value.
     * @return JPanel with shared topical facet list (JScrollPane with JList)
     * and negative facet list (JScrollPane with JList).
     */
    private JPanel setCommonFacetList()
    {
        LinkedHashMap facetArcMap = new LinkedHashMap(TopicRetriever
                .getFacetArcMap());
        // Maximum number of words to display as an illustration of a 
        // topical facet.
        int maxWords = 5;
        int i = 0;
        String listTwo = "listTwo";
        DefaultListModel commonModel = new DefaultListModel();
        mCommonList = new JList(commonModel);
        mCommonList.setName(listTwo);
        // Allows single row selection and sets the font for the list.
        mCommonList.setSelectionMode(ListSelectionModel
                .MULTIPLE_INTERVAL_SELECTION);
        mCommonList.setTransferHandler(arrayListHandler);
        mCommonList.setFont(new Font("SansSerif", Font.PLAIN, 11));
        mCommonList.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        if(facetArcMap.isEmpty()) commonModel.add(i, "<html>No usable"
                + " topical facets found.<br>Consider to abort this "
                + "task.</html>");
        else
        {
            mCommonList.setDragEnabled(true);
            Object[] facets = prepareArray(facetArcMap);
            ApplicationManager.showText("Preparing a Facet Viewer for " 
                    + facets.length +  " topical facets.", 0);
            for(i = 0; i < facets.length; i++)
            {
                Object[] arcArray;
                arcArray = (Object[]) facets[i];
                try
                {
                    Integer facetKey = (Integer)arcArray[0];
                    SortedSet arcSet = (TreeSet) arcArray[1];
                    String arcString = getKeywords(arcSet, maxWords);
                    String facet = facetKey + " " + arcString;
                    commonModel.add(i, facet);                
                }
                // Catches the strings.
                catch(ClassCastException cce)
                {
                    String countString = "** Topical facets shared by a"
                            + " group of " + arcArray[0].toString();
                    commonModel.add(i, countString);                             
                }
            }
        }
        String scope = ApplicationManager.getScope();
        String number;
        if(i == 0) number = "Zero";
        else number = String.valueOf(i);
        JLabel commonHeader = new JLabel("<html><center>" + number 
                + " shared topical facets. Scope: " + GraphTime.formatScope(scope) 
                + "<br><I>Drag & Drop below to make it a negative" +
                " instance</I></center></html>", JLabel.CENTER);
        commonHeader.setBorder(border);
        // Puts list in a scrollPane.
        JScrollPane scrollCommon = new JScrollPane(mCommonList);
        scrollCommon.setColumnHeaderView(commonHeader);
        JPanel facetPanel = new JPanel();
        facetPanel.setLayout(new BoxLayout(facetPanel, BoxLayout.PAGE_AXIS));
        facetPanel.add(scrollCommon);
        facetPanel.add(setNegativeScroll());
        return facetPanel;
    }
    
    /** setNegativeScroll: Prepares a list with topical facet-keys that are 
     * to be eliminated from the retrieval task.
     * @return JScrollPane with an empty JList to accomodate 
     * the Drag & Drop instances (String).
     */
    private JScrollPane setNegativeScroll()
    {
        SortedSet negativeSet = new TreeSet();
        negativeSet.add("");
        DefaultListModel negativeModel = new DefaultListModel();
        Object[] facets = negativeSet.toArray();
        for(int i = 0; i < facets.length; i++)
        {
            String facet = facets[i].toString();
            negativeModel.add(i, facet);
        }
        JList negativeList = new JList(negativeModel);
        negativeList.setSelectionMode(ListSelectionModel
                .SINGLE_INTERVAL_SELECTION);
        negativeList.setFont(new Font("SansSerif", Font.PLAIN, 11));
        negativeList.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        negativeList.setTransferHandler(arrayListHandler);
        negativeList.setDragEnabled(true);
        JLabel negativeHeader = new JLabel("<html><center> Negative topical"
                + " facet instances<br>to be removed from this retrieval "
                + "task</center></html>", JLabel.CENTER);
        negativeHeader.setBorder(border);
        JScrollPane scrollNegative = new JScrollPane(negativeList);
        scrollNegative.setColumnHeaderView(negativeHeader);
        return scrollNegative;
    }
    
    /** setDocScroll: Prepares a list with the document-ids linked to the 
     * topical facets. Uses facetDocs, a LinkedHashMap with as key the count 
     * class (sorted from high to low) and as value a HashMap with the global 
     * facet key (Integer) and a SortedSet with all document-ids (Integer)
     * related to this task and docTable, a DocTable instance to retrieve 
     * the filename of a document.
     * @return JScrollPane with document list (JList).
     */
    private JScrollPane setDocScroll()
    {
        LinkedHashMap facetDocMap = TopicRetriever.getFacetDocMap();
        DocTable docTable = TopicRetriever.getDocTable();
        // Maximum number of documents linked to a topical facet shown.
        int maxDoc = 15;
        // Vector to hold the data for the document scroll list.
        ArrayList<String> docList = new ArrayList<>();
        Object[] facets = prepareArray(facetDocMap);
        for (Object facet : facets) {
            Object[] docArray;
            docArray = (Object[]) facet;
            int docCount = 0;
            try
            {
                // When the object is not an array but a comment-string,
                // it is catched as an exception and rendered appropriately.
                Integer facetKey = (Integer)docArray[0];
                SortedSet docSet = (TreeSet) docArray[1];
                int docSetSize = docSet.size();
                String docString = "[ ";
                Iterator doc_itr = docSet.iterator();
                // Shows a maximum of 'maxDoc' documents.
                while(doc_itr.hasNext() && docCount < maxDoc)
                {
                    Integer docId = (Integer) doc_itr.next();
                    String docName = docTable.getFilename(docId);
                    docString += "(" + docId.toString() + ") " + docName + ", ";
                    docCount++;
                }
                if(docSetSize >= maxDoc) docString += "... , ";
                // Removes last comma.
                docString = docString.substring(0, docString.lastIndexOf(","));
                String docs = "Facet " + facetKey + " -> " + docSetSize 
                        + " related docs: " +  docString + " ]";
                docList.add(docs);
            }
            // Catches the strings in the array.
            catch(ClassCastException cce)
            {
                String countString = "** Documents found in a group of " 
                        + docArray[0].toString();
                docList.add(countString);
            }
        }
        DefaultListModel docModel = new DefaultListModel();
        Object[] docs = docList.toArray();
        for(int i = 0; i < docs.length; i++)
        {
          //  String docList = docs[i].toString();
            docModel.add(i, docs[i].toString());
        }
        String listThree = "listThree";
        JList thisDocList = new JList(docModel);
        thisDocList.setName(listThree);
        thisDocList.setFont(new Font("SansSerif", Font.PLAIN, 11));
        thisDocList.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        thisDocList.setSelectionMode(ListSelectionModel.
                MULTIPLE_INTERVAL_SELECTION);
        JLabel docHeader = new JLabel("<html><center> Documents linked to "
                + "the shared topic facets<br><I>First " + maxDoc
                +" documents shown</I></center></html>",  JLabel.CENTER);
        docHeader.setBorder(border);
        JScrollPane scrollDoc = new JScrollPane(thisDocList);
        scrollDoc.setColumnHeaderView(docHeader);
        return scrollDoc;
    }
    
    /** prepareArray: One array is made with all the topical facets or docs 
     * (String objects) from every count class.
     * @param linkedMap : LinkedHashMap with either the arcs or the docs in a 
     * HashMap as value to the count (int) key. The facet key (Integer) is the
     * key in the second HashMap.
     * @return Object array.
     */
    private Object[] prepareArray(LinkedHashMap linkedMap)
    {
        Object[] facetArray = new Object[20];
        Object[] facets;
        int capacity = 0;
        // Minimal number of topics sharing a set of facets.
        int countLimit = 1;
        LinkedHashMap thisMap = linkedMap;
        Iterator count_itr = thisMap.keySet().iterator();
        while(count_itr.hasNext())
        {
            Integer count = (Integer) count_itr.next();
            // Doesn't show shared facets not used by the count limit of
            // the tokens.
            if(count >= countLimit)
            {  
                // Increases array capacity with 20 elements when full.
                if (capacity % 20 == 0) facetArray = resizeArray(facetArray, 
                        capacity, 20);
                // Creates comment string indicating the number of tokens shared.
                facets = new Object[2];
                String countString = count + " tokens from the query **";
                facets[0] = countString;
                facets[1] = "";
                facetArray[capacity] = facets;
                capacity++;
                // Detailed list of elements shared.
                Map facetMap = (HashMap) thisMap.get(count);
                Iterator facet_itr = facetMap.keySet().iterator();
                while(facet_itr.hasNext())
                {
                    // Increases array capacity with 20 elements when full.
                    if (capacity % 20 == 0) facetArray = resizeArray(facetArray,
                            capacity, 20);
                    facets = new Object[2];
                    Integer facetKey = (Integer) facet_itr.next();
                    facets[0] = facetKey;
                    facets[1] = facetMap.get(facetKey);
                    facetArray[capacity] = facets;
                    capacity++;
                }
            }
        }
        // Removes unused array capacity.
        facetArray = resizeArray(facetArray, capacity, 0);
        return facetArray;
    }
    
    /** resizeArray: When addRows > 0 the array is expanded with that number 
     * of elements else the array is copied with only the real data to remove
     * any unused empty placeholders.
     * @param array : an array with Objects.
     * @param arrayLength : the actual number of elements in the array (int).
     * @param addCapacity : the number of elements (int) to add to the arry 
     * capacity. When '0' any remaining and unused placeholders are removed 
     * from the array.
     * @ToDo : resize belongs to the Algorithms Class but then I need to rerun 
     * the whole corpus!
     */
    private Object[] resizeArray(Object[] array, int arrayLength, int addCapacity)
    {
        Object[] tmpArray;
        if(addCapacity > 0) tmpArray = new Object[arrayLength + addCapacity];
        // Copies the actual content of the array to get rid of empty rows left
        // over when all the data are added.
        else tmpArray = new Object[arrayLength];
        System.arraycopy(array, 0, tmpArray, 0, arrayLength);
        return tmpArray;
    }
    
    /** getKeywords: Returns a selected maximum of keywords that semantically
     * defined this topical facet.
     * @param arcSet : SortedSet of arc-keys (String) from a topical facet.
     * @param maxWords : the maximum number (int) of arcs per string to return.
     * @return String with keywords.
     */
    private String getKeywords(SortedSet arcSet, int maxWords)
    {
        int arcCount = 0;
        String endString = " (...) ]";
        // Gets the labels from the arc-keys.
        List labelList = new ArrayList();
        Iterator arc_itr = arcSet.iterator();
        while(arc_itr.hasNext() && arcCount <= maxWords)
        {
            String arcKey = arc_itr.next().toString();
            Integer label1 = Integer.valueOf(arcKey.substring(0, arcKey
                    .indexOf("*")));
            labelList.add(label1);
            Integer label2 = Integer.valueOf(arcKey.substring(arcKey
                    .indexOf("*") + 1));
            labelList.add(label2);
            arcCount++;
        }
        List keyWords = labels.returnLabelList(labelList);
        StringBuffer keywordString = new StringBuffer();
        keywordString.append("[ ");
        Iterator label_itr = keyWords.iterator();
        while(label_itr.hasNext())
        {
            keywordString.append(label_itr.next().toString()).append(", ");
        }
        // Replaces last comma with bracket and ellipsis if words were omitted.
        int start = keywordString.length() - 2;
        int end = keywordString.length() - 1;
        if(maxWords >= arcCount) endString = " ]";
        keywordString = keywordString.replace(start, end, endString);
        return keywordString.toString();
    }
    
    /** confirmPerformed: Appropriate action when the user pushes the 'Confirm' 
     * button: in the next step all negative topical facets will be eliminated.
     * @param event : the selected action.
     */
    private void confirmPerformed(ActionEvent event)
    {
        mDecision = 0;
        releaseWait();
        closeFacetTab();
    }
    
    /** continueActionPerformed: Appropriate action when the user selects the
     * 'Continue' button: to the next step without removing any negative topical
     * facets that might have been selected.
     * @param event : the selected action.
     */
    private void continueActionPerformed(ActionEvent event)
    {
        mDecision = 1;
        releaseWait();
        closeFacetTab();
    }
    
    /** abortActionPerformed: Appropriate action when the user presses the 
     * 'Abort' button: all activity will be stopped.
     * @param event : the selected action.
     */
    private void abortActionPerformed(ActionEvent event)
    {
        mDecision = 2;
        releaseWait();
        closeFacetTab();
    }
    
    /**
     * closeFacetTab: Closes the RetrievedFacetViewer and discards all data when 
     * the checkBox 'Keep Facet Viewer' is unselected or when the user decides
     * to abort the retrieval task.
     */
    private void closeFacetTab()
    {
        if(!keepViewBx.isSelected() || mDecision == 2)
        {
            int tabIndex = ApplicationManager.getTabbedPane()
                    .indexOfTab("Facet Viewer");
            ApplicationManager.removeTab(tabIndex);
        }
        ApplicationManager.restoreDown();
    }
    
    /** waitForDecision: Application waits for the user to press one of 
     * three buttons in the GUI.
     */
    private synchronized void waitForDecision()
    {
        while(mDecision == -1)
        {
            ApplicationManager.setProgessLabel("Waiting to continue...");
            try
            {
                this.wait();
            }
            catch(InterruptedException ie)
            { }
        }
    }
    
    /** releaseWait: A button was pressed, the program continues. 
     * The decision is transferred to the TopicRetriever together with a 
     * SortedSet of negative token facets (Integer) as collected by the
     *  ArrayListHandler.
     */
    private synchronized void releaseWait()
    {
        this.notify();
        ApplicationManager.resetProgressText();
        TopicRetriever.setUserDecision(mDecision);
        TopicRetriever.setNegativeKeySet(arrayListHandler.getNegativeKeys());
    }
    
    /** getFocusList: Returns the list in focus with the topical facets or 
     * documents to the 'Find' function of the ApplicationManager.
     * @return a JList with a shared topics or documents (String).
     */
    public static JList getFocusList()
    {
        return switch (mListName) {
            case "listOne" -> mTokenList;
            case "listTwo" -> mCommonList;
            case "listThree" -> mDocList;
            default -> null;
        };
    }
    
}