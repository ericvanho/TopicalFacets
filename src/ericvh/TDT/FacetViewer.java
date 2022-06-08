package ericvh.TDT;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.BoxLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.JLabel;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;
import javax.swing.JComboBox;
import javax.swing.Box;
import javax.swing.border.TitledBorder;

/** Class FacetViewer shows the results of the FacetMaker computations in the GUI.
 * @author Eric Van Horenbeeck
 * Created on 23 mei 2005, 22:35
 * Revision Oct. 10 2006
 */
public class FacetViewer
{
    // Table with information on documents to add the final informative value.
    private final DocTable mDocTable;
    // The table with the arcs of this dataGraph.
    private final ArcsTable mArcTable;
    // CollectionTable with information on the collections in this dataGraph.
    private final CollectionTable mCollTable;
    // ArcsTable with the informative arcs.
    private final ArcsTable mInfoArcTable;
    // Class to store semantic information on documents.
    private SemanticUnit mSemUnit;
    // Class merges documents into the same topical facet based on semantic
    // similarity.
    private SemanticAnalysis mSemAnalysis;
    // Class with all the labels.
    private LabelTable mAllLabels;
    // Map with 'named entities' and informative phrases from the Associations class.
    private Map mAssociations;
    // Map with the topical facets from the SemanticAnalysis.
    private LinkedHashMap mTopicMap;
    // Boolean flag to restrict use of labels in this view.
    private boolean mNoLabels;
    // Map with the topic and the unit keys.
    private static Map mKeyMap;
    // Set with the identification of documents that remained unconnected 
    // in this session.
    private static SortedSet mUnusedDocSet;
    // Reconstructor for the text of selected documents.
    private Reconstructor rec;
    // Numbers for the topical facet, the largest, smallest and total
    // topical facet, total and unused docs.
    private int mTopicKey, mLargest, mSmallest, mTotalSets, mTotalDocs,
            mUnusedDocs;
    // Average number of docs in a topical facet and the average number 
    // of sets having the same document.
    private double mAverageSize, mSameDocUsed;
    // Flags to indicate a full, selective or unused build of the topical facets.
    private final int FULL = 0;
    private final int SELECT = 1;
    private final int UNUSED = 2;
    // Panels to assemble the topic data, the document data, the unused docs 
    // and the comboBox.
    private JPanel topicPanel;
    private JPanel headerPanel;
    private JPanel unusedPanel;
    private JPanel boxPanel;
    // Final panes with scroll bars.
    private JScrollPane topicScroll;
    private JScrollPane scrollSelectedText;
    // Text panes for the topic data.
    private JTextPane topicArea;
    private static JTextArea selectedTextPane;
    // Labels for documents.
    private JLabel docLabel;
    private JLabel headerLabel;
    private JLabel selectedTextHeader;
    // ComboBox with the informative value of the topics.
    private JComboBox topicBox;
    // Formatting decimal numbers.
    private DecimalFormat df;
    // Label border.
    private Border border = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
    
    /** Constructor
     * @param ia : ArcsTable instance with only the informative arcs.
     * @param at : ArcsTable instance with all the arcs needed to reconstruct
     * the documents.
     * @param dt : DocTable instance with general document info.
     * @param ct : CollectionTable instance with general collection info.
     */
    public FacetViewer(ArcsTable ia, ArcsTable at, DocTable dt,
            CollectionTable ct)
    {
        mInfoArcTable = ia;
        mArcTable = at;
        mDocTable = dt;
        mCollTable = ct;
    }
    
    /** topicView: Entry point to set up a view on the topical facets collected 
     * by the TopicMaker class. The final panel is sent to the ApplicationManager
     * to show on a tabbedPane in the GUI.
     * @param semUnit : Information collected by the TopicMaker. Instance of the
     * SemanticUnit class
     * @param semAnalysis : Semantic document relations. Instance of the 
     * SemanticAnalysis class.
     * @param noLabels : if boolean 'true' no labels will be retrieved, 
     * only arc keys.
     * @param summary : if boolean 'true' only a summary will be shown on the 
     * 'General Info'-tab.
     */
    public void topicView(SemanticUnit semUnit, SemanticAnalysis semAnalysis, 
            boolean noLabels,
            boolean summary )
    {
        mSemUnit = semUnit;
        mSemAnalysis = semAnalysis;
        mNoLabels = noLabels;
        mLargest = 0;
        mSmallest = 99999;
        mAverageSize = 0;
        mTopicKey = 0;
        mKeyMap = new HashMap();
        df = new DecimalFormat("##.#");
        // Header with general information on the data used in this session.
        prepareHeader();
        // Collects the documents per topical facet from the SemanticAnalysis.
        mTopicMap = mSemAnalysis.getTopicalFacetMap();
        // Calculates a few statistics to be shown in the general header:
        // - The number of topical facet sets found in these documents
        mTotalSets = mTopicMap.size();
        if(!summary)
        {
            rec = new Reconstructor(mArcTable);
            mAllLabels = ApplicationManager.getLabels();
            // Retrieves associations and phrases to illustrate the content of 
            // the documents.
            mAssociations = ApplicationManager.getAssociationMap();
            prepareTopicPanel();
        }
        Iterator topic_itr = mTopicMap.keySet().iterator();
        while (topic_itr.hasNext())
        {
            // A sequential number identifying a topical facet as prepared by 
            // the SemanticAnalysis.
            Integer facetKey = (Integer) topic_itr.next();
            // Collects a panel with information and doc-ids per topical facet. 
            // FULL is an instruction flag to indicate that the computed data 
            // go all to the full topical facets overview. 
            // The SELECT instruction is used to compute and show a selection 
            // of topics. If summary = true, only a general statistic will be
            // shown on the 'General Info'-tab.
            selectTopicsAndDocs(facetKey, FULL, summary);
        }
        // - The average number of documents in a topical facet.
        mAverageSize = mAverageSize / mTotalSets;
        // - The total number of different documents in this exercise.
        mTotalDocs = mSemAnalysis.getActiveDocCount();
        // - The number of times the same document appears in an average topical
        // facet.
        mSameDocUsed = (mAverageSize * mTotalDocs) / mTotalSets;
        // Constructs the header and puts it in the top row of the topic 
        // overview or shows it on the 'General Info'-tab if only a summary is 
        // requested.
        setHeader(summary);
        // Makes a comboBox with the values of the different topical facets.
        if(summary && !noLabels) makeValueList(true);
        if(!summary)
        {
            makeValueList(false);
            topicScroll.setColumnHeaderView(headerPanel);
            headerPanel.add(boxPanel);
            // Collects the unused documents and puts them in a panel at the 
            // bottom of the topic pane.
            unusedPanel = new JPanel();
            unusedPanel.setBackground(Color.white);
            TitledBorder title = BorderFactory.
                    createTitledBorder(border, "Documents not used in any "
                            + "topical facet in this session");
            title.setTitleJustification(TitledBorder.CENTER);
            title.setTitleColor(Color.black);
            unusedPanel.setBorder(title);
            prepareDocLabels(unusedDocArray(), UNUSED);
            topicPanel.add(Box.createRigidArea(new Dimension(0,7)));
            topicPanel.add(unusedPanel);
            // Sends the final panel to the ApplicationManager.
            ApplicationManager.addTopicPane(topicScroll, 6);
        }
    }
    
    /** viewSummary: Shows a short summary with the number of facets found on 
     * the General Info-tab.
     * @param semUnit : Information collected by the TopicMaker. Instance of 
     * the SemanticUnit class
     * @param semAnalysis : Semantic document relations. Instance of the 
     * SemanticAnalysis class.
     * @param noLabels : if boolean 'false' a list with topical facets will
     * be shown on the 'General Info'-tab.
     * @param summary : if boolean 'true' only a summary will be shown on the 
     * 'General Info'-tab.
     */
    public void viewSummary(SemanticUnit semUnit, SemanticAnalysis semAnalysis,
            boolean noLabels, boolean summary)
    {
        // Computes the data. Only a short summary will be shown.
        topicView(semUnit, semAnalysis, noLabels, true);
    }
    
    /** prepareHeader: Prepares a general header for the topic collection of 
     * this dataGraph and adds it to the topicPanel. Header text is inserted 
     * after assembling all the topic panels to allow some general statistics 
     * on this session to be added.
     */
    private void prepareHeader()
    {
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.LINE_AXIS));
        headerLabel = new JLabel();
        headerPanel.setBorder(border);
        headerPanel.add(Box.createRigidArea(new Dimension(15,0)));
        headerPanel.add(headerLabel);
        headerPanel.add(Box.createHorizontalGlue());
    }
    
    /** setHeader: Sets the text to go into the general header of this topic 
     * overview with some statistics on the number and the size of the topical
     * facet sets.
     * @param summary : if boolean 'true' only a summary will be shown in the
     * 'General Info'-tab.
     */
    private void setHeader(boolean summary)
    {
        StringBuilder header = new StringBuilder();
        StringBuffer sources = new StringBuffer();
        NumberFormat pf = NumberFormat.getPercentInstance();
        // Gets the different sources used in this search.
        SortedSet sourceSet = new TreeSet();
        // Gets the collection keys and the scope (time frame) of this dataGraph.
        String scope = ApplicationManager.getScope();
        Set collKeys = mCollTable.getScopeCollectionKeys();
        Iterator collKey_itr = collKeys.iterator();
        while (collKey_itr.hasNext())
        {
            String key = collKey_itr.next().toString();
            sourceSet.add(mCollTable.getSourceName(key));
        }
        Iterator source_itr = sourceSet.iterator();
        while(source_itr.hasNext()) sources.append(source_itr.next().toString())
                .append(" - ");
        // Removes hyphen and blank from the end of the sources string.
        sources = sources.delete(sources.length()-2, sources.length());
        // The number of unused documents without a meaningful relation with 
        // other documents.
        mUnusedDocs = (mDocTable.getDocCount() - mTotalDocs);
        // Header of the table with results on the topic search of this dataGraph.
        if(summary)
        {
            String specialString = "";
             /*
            List  facets = new ArrayList();
              
              
          SortedSet docsSet =  mSemAnalysis.getActiveDocs();
              
      //      Iterator facet_itr = mTopicMap.keySet().iterator();
    //        while(facet_itr.hasNext())
  //          {
   //            Integer facetKey = (Integer) facet_itr.next();
    //      if(facetKey.equals(2959) )
   //       if(docsSet.contains(315))
        //   {
              
         //    LinkedHashSet arcs = mSemUnit.getArcSet(facetKey);
         //    Set arcSet = mInfoArcTable.getAllArcKeysInDoc(421);
       //      specialString += "\nDoc 421 " + arcSet;
              
        //       specialString += " FacetKey: " + facetKey.toString() + " "
                    +  mSemUnit.getArcSet(facetKey) + '\n';
              
            if( docsSet.contains(311))
           {
             specialString += "\nDoc 311\n" ;
             Set arcSet = mInfoArcTable.getAllArcKeysInDoc(311);
             Iterator arc_itr = arcSet.iterator();
             while(arc_itr.hasNext())
             {
                    String arc = arc_itr.next().toString();
                    Set arcs = new HashSet();
                    arcs.add(arc);
                    specialString += arc + " " + mInfoArcTable
                          .makeLabelList(arcs) + " ";
             }
              
           }
           if( docsSet.contains(609))
           {
            specialString += "\nDoc 609\n" ;
             Set arcSet = mInfoArcTable.getAllArcKeysInDoc(609);
             Iterator arc_itr = arcSet.iterator();
             while(arc_itr.hasNext())
             {
                    String arc = arc_itr.next().toString();
                    Set arcs = new HashSet();
                    arcs.add(arc);
                    specialString += arc + " " + mInfoArcTable
                                    .makeLabelList(arcs) + " ";
             }
           }
            if( docsSet.contains(632))
           {
              specialString += "\nDoc 632\n" ;
             Set arcSet = mInfoArcTable.getAllArcKeysInDoc(632);
             Iterator arc_itr = arcSet.iterator();
             while(arc_itr.hasNext())
             {
                    String arc = arc_itr.next().toString();
                    Set arcs = new HashSet();
                    arcs.add(arc);
                    specialString += arc + " " + mInfoArcTable
                                    .makeLabelList(arcs) + " ";
             }
           }
  
            int docId = 0;
            int count = 0;
            String noDoc = "\nNo doc ";
            List  facets = new ArrayList();
            SortedSet docsSet =  mSemAnalysis.getActiveDocs();
            if( docsSet.contains(6))
            {
                docId = 6;
                facets = Arrays.asList(mSemAnalysis.relatedFacetSet(docId)
                                   .toArray());
                specialString += "Facets for doc " + docId + " : " + facets
                                   + '\n';
                count++;
            }
            else noDoc +=  "6, ";
            if( docsSet.contains(7))
            {
                docId = 7 ;
                facets = Arrays.asList(mSemAnalysis.relatedFacetSet(docId)
                                      .toArray());
                specialString += "Facets for doc " + docId + " : " + facets 
                                       + '\n';
                 count++;
            }
             else noDoc +=   "7, ";
    
         String specialString  = "\nFacett\n";
            Iterator facet_itr = mTopicMap.keySet().iterator();
            
            while(facet_itr.hasNext())
            {
                Integer facetKey = (Integer) facet_itr.next();
         //       specialString +=  facetKey.toString() + ": " + mSemUnit
                               .getArcSet(facetKey).size()+ " arcs - " +
          //             mSemUnit.getDocSet(facetKey).size() + " docs \n";
              if (facetKey.equals(1552)  ||  facetKey.equals(3077)  
                     ||  facetKey.equals(2435) ||  facetKey.equals(3540)
                     ||  facetKey.equals(3363) ||  facetKey.equals(1290))
                  specialString += "\nTF" + facetKey.toString() + " " 
                          +  mInfoArcTable.makeLabelList(mSemUnit
                             .getArcSet(facetKey) ); // "  Docs " +
                //      mSemUnit.getDocSet(facetKey)+ '\n';
            }
            
            /*
            specialString  += "\nUnused Docst\n";
            Iterator unused_itr = mUnusedDocSet.iterator();
            while(unused_itr.hasNext())
            {
                specialString += unused_itr.next().toString() + ", ";
            }
            */
            if (mSmallest == 99999)
            {
                mSmallest = 0;
            }
            int rescanned = 0;
            if (FacetMaker.getRescannedDocs() != null)
            {
                rescanned = FacetMaker.getRescannedDocs().size();
            }            
                header.append("Topical Facets\nCommunity & Scope: ").
                        append(ApplicationManager.getCommunity()).
                        append(", for the period: ").append(GraphTime
                                .formatScope(scope))
                        .append("\nCombined ").append(mTotalDocs)
                        .append(" documents belonging to ").append(sourceSet
                                .size())
                        .append(" sources ( ").append(sources).append(" ). ")
                        .append(mUnusedDocs).append(" documents remained"
                                + " unrelated (")
                        .append(pf.format(((double) mUnusedDocs) / mDocTable
                                .getDocCount()))
                        .append(").\nFound ").append(mTotalSets).append(" topical"
                                + " facets. Largest set: ").
                        append(mLargest).append(" documents, smallest: ")
                        .append(mSmallest).append(", average: ")
                        .append(df.format(mAverageSize))
                        .append(". A document is seen in ")
                        .append(df.format(mSameDocUsed))
                        .append(" topical facets on average. ").append(rescanned)
                        .append(" documents rescanned.");
                header.append(specialString);

                ApplicationManager.showText(header.toString(), 0);
            }
            else
            {
                header.append("<html><left>Community & Scope: ")
                        .append(ApplicationManager.getCommunity())
                        .append(", for the period: ").append(GraphTime
                                .formatScope(scope))
                        .append("<br> Combined ").append(mTotalDocs)
                        .append(" documents belonging to ").append(sourceSet
                                .size())
                        .append(" sources ( ").append(sources).append(" ). ")
                        .append(mUnusedDocs).append(" documents remained "
                                + "unrelated (")
                        .append(pf.format(((double) mUnusedDocs) / mDocTable
                                .getDocCount()))
                        .append("). <br>Found ").append(mTotalSets)
                        .append(" topical facets. Largest set: ").append(mLargest)
                        .append(" documents, smallest: ").append(mSmallest)
                        .append(", average: ").append(df.format(mAverageSize))
                        .append(".&nbsp A document is seen in ").append(df
                                .format(mSameDocUsed))
                        .append(" topical facets on average.<br></left></html>");
                headerLabel.setText(header.toString());
            }
        }
        
        /** prepareTopicArea: Makes a text pane for every topical facet and 
         * adds it to the topicPanel.
         * @param topString : String with all the information on this topic.
         * @param lines : the number of text lines in this panel (int).
         * Defines the height of the panel.
         */
        private void prepareTopicArea(String topString, int lines)
        {
            JTextPane topPane = new JTextPane();
            topPane.setEditable(false);
            topPane.setMargin(new Insets(5,15,5,5));
            // Applies some style characteristics to the text in this pane.
            Style topicStyle = topPane.addStyle("italic", null);
            StyleConstants.setItalic(topicStyle, true);
            topicStyle = topPane.addStyle("bold", null);
            StyleConstants.setBold(topicStyle, true);
            topPane.setText(topString);
            StyledDocument topicDoc = topPane.getStyledDocument();
            int start = 0;
            int end = topString.indexOf('-') -1;
            int pattern = end - start;
            topicDoc.setCharacterAttributes(start, end, topPane
                    .getStyle("bold"),false);
            start = end;
            end = topString.indexOf('[') - pattern;
            topicDoc.setCharacterAttributes(start, end, topPane
                    .getStyle("italic"),false);
            start = topString.indexOf(']') + 1;
            end = 32;
            topicDoc.setCharacterAttributes(start, end, topPane
                    .getStyle("italic"),false);
            start = topString.indexOf("**");
            end = 46;
            topicDoc.setCharacterAttributes(start, end, topPane
                    .getStyle("italic"),false);
            // Sets the dimensions for this text pane.
            int preferredWidth = 800;
            int preferredHeight = (lines + 4) * 19;
            View v = topPane.getUI().getRootView(topPane);
            v.setSize(preferredWidth, preferredHeight);
            topPane.setPreferredSize(new Dimension(preferredWidth
                    , preferredHeight));
            // Adds the finished text pane to the topicPanel.
            topicPanel.add(topPane);
        }
        
        /** prepareDocLabels: Transforms an array of doc-ids into a grid of 
         * clickable labels and adds it to the topicPanel.
         * @param docString : array of doc-ids (String).
         * @param instruction : full(0) selective(1) or unused(2) build of the
         * topical facet sets (int).
         */
        private void prepareDocLabels(String[] docString, int instruction)
        {
            JPanel docPanel = new JPanel();
            JPanel labelPanel = new JPanel();
            Color unusedColor = new Color(13, 174, 242);
            if(instruction == UNUSED)
            {
                docPanel.setBackground(Color.white);
                labelPanel.setBackground(Color.white);
            }
            // Labels are placed in 6 columns with as many rows as necessary. 
            // A mouse listener is added to each label to allow the 
            // reconstruction of the underlying text and the retrieval of all
            // topical facets related to the selected document.
            int labels = docString.length;
            int col = 6;
            if(labels < 6) col = labels;
            int row = labels / 6;
            if((labels % 6) > 0) row++;
            if (row < 1) row = 1;
            labelPanel.setLayout(new GridLayout(row, col));
            JLabel[] thisDocLabel = new JLabel[labels];
            for(int i = 0; i < labels; i++)
            {
                thisDocLabel[i] = new JLabel(docString[i]);
                thisDocLabel[i].addMouseListener(new DocMouseListener());
                if(instruction < UNUSED) thisDocLabel[i].setForeground(Color.blue);
                else thisDocLabel[i].setForeground(unusedColor);
                labelPanel.add(thisDocLabel[i]);
            }
            docPanel.add(labelPanel);
            docPanel.setVisible(true);
            // To the topicPanel
            if(instruction < UNUSED) topicPanel.add(docPanel);
            else unusedPanel.add(docPanel);
        }
        
        /** DocMouseListener: MouseListener extracts the doc-id part of the 
         * clicked label and directs it to the 'collectTopics' method where all
         * topical facets having the same doc-id will be collected in a
         * new panel. The text of the selected document is also reconstructed.
         */
        public class DocMouseListener extends MouseAdapter
        {
            /** mouseClicked: user selected a label.
             * @param me : mouse event
             */
            @Override
            public void mouseClicked(MouseEvent me)
            {
                JLabel label;
                label = (JLabel) me.getSource();
                // Changes the color of a clicked doc-id label.
                label.setForeground(Color.GRAY);
                String docId = label.getText();
                docId = docId.substring(docId.indexOf('(') + 1, 
                        docId.indexOf(')'));
                Integer[] tmpArray = new Integer[1];
                tmpArray[0] = Integer.valueOf(docId);
                collectTopics(tmpArray, "Document: " + docId);
            }
        }
        
        /** prepareTopicPanel: Prepares a panel to accomodate the different
         * text areas and doc-id labels and puts it in a scrollable panel.
         */
        private void prepareTopicPanel()
        {
            topicPanel = new JPanel();
            topicPanel.setBorder(border);
            topicPanel.setLayout(new BoxLayout(topicPanel, BoxLayout.PAGE_AXIS));
            // Makes scroll panel to accomodate the topic data and the document data.
            topicScroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            topicScroll.add(topicPanel);
            topicScroll.setViewportView(topicPanel);
        }
        
        /** selectTopicsAndDocs: Converts a sorted map with doc-ids into 
         * topical facets on displayable panels.
         * @param facetKey : key (Integer) to a set of doc-ids that form a 
         * topical facet from TopicMap.
         * @param instruction : this method is used either to compute the 
         * full topicMap (instruction 0) or a selection of it (instruction 1).
         * @param summary : if boolean 'true' only a summary will be shown 
         * on the 'General Info'-tab.
         */
        private void selectTopicsAndDocs(Integer facetKey, int instruction, 
                boolean summary)
        {
            // A set with the doc-ids making up this topical facet.
            SortedSet docSet = (TreeSet) mTopicMap.get(facetKey);
            Object[] docs = (Object[])docSet.toArray(new Object[docSet.size()]);
            // Collects a few statistics on the size of the topical facet sets.
            int docLength =  docs.length;
            if(docLength > mLargest) mLargest = docLength;
            else if(docLength < mSmallest) mSmallest = docLength;
            mAverageSize += docLength;
            // Creates new topic key and puts in into a map linked to the unit key.
            if(instruction == FULL)
            {
                mTopicKey++;
                mKeyMap.put(facetKey, mTopicKey);
            }
            // Retrieves an existing topic key with the unit key.
            else mTopicKey = (Integer) mKeyMap.get(facetKey);
            if(!summary)
            {
                // The maximum number of associations to show.
                int maxWords = 7;
                String arcString = "";
                StringBuilder topicString = new StringBuilder();
                int docInt = ((Integer) docs[0]);
                // Makes a string for the topic header with a few (maxWords) 
                // of the main keywords.
                String arcWords = prepareKeywords(facetKey, maxWords);
                topicString.append("\nTopical facet # ").append(mTopicKey).
                        append(" - ").append(docSet.size())
                        .append(" documents. Informative weight: ")
                        .append(df.format(mSemUnit.getValue(facetKey)))
                        .append(". General content indication: ")
                        .append(arcWords)
                        .append("\nSpecific keywords and phrases:\n");
                // The number of text lines this panel needs to provide.
                int lines = 0;
                // Visits all docs to prepare a string with content info and 
                // to collect their filenames.
                String[] docLink = new String[docLength];
                for(int i = 0; i < docLength; i++)
                {
                    int docNr = ((Integer) docs[i]);
                    // Stores the filenames belonging to the doc-ids in an array.
                    docLink[i] = "(" + docNr + ") " + mDocTable.
                            getFilename(docNr) + "  ";
                    // Prepares a string from associations and arcs to 
                    // illustrate the topic content.
                    if(mAssociations.containsKey(docNr))
                    {
                        List assocList;
                        assocList = (ArrayList) mAssociations.get(docNr);
                        arcString += "Doc " + docNr + ": " 
                                + prepareArcString(assocList, maxWords) + '\n';
                        lines++;
                    }
                }
                // Putting topic information and arc strings together. Prints 
                // a line with a warning when no information is available.
                if(arcString.equals(""))
                {
                    arcString = "** No specific keywords found **\n";
                    lines++;
                }
                // Warning if no keywords (associations) are available for some
                // documents.
                else if(lines < docLink.length)
                {
                    arcString += "** No specific keywords found for some "
                            + "documents **\n";
                    lines++;
                }
                topicString.append(arcString);
                // Displays the topical facets in the GUI and places the topic
                // string in a textArea.
                prepareTopicArea(topicString.toString(), lines);
                // Transforms the doc-ids into a panel of clickable labels.
                prepareDocLabels(docLink, instruction);
            }
        }
        
        /** prepareArcString: Prepares a string with a maximum number of labels 
         * from the vertex-ids that defined this topic and from associations 
         * related to this document.
         * @param arcList : one or more vertex indices (Integer) collected by 
         * the Associations class.
         * @param maxWords : the maximum number (int) of keywords (associations)
         * per document to return (currently set at 7 in the 'selectTopicsAndDocs'
         * method).
         * @return String with the vertex-ids replaced by their labels.
         */
        private String prepareArcString(List arcList, int maxWords)
        {
            StringBuffer arcString = new StringBuffer();
            int count = 0;
            boolean hyphen = false;
            List labelList;
            Iterator assoc_itr = arcList.iterator();
            while(assoc_itr.hasNext() && count < maxWords )
            {
                LinkedList assocList = (LinkedList) assoc_itr.next();
                hyphen = false;
                // No labels are retrieved if noLabels is 'true'. 
                // Vertex-ids are given instead.
                if(mNoLabels) labelList = assocList;
                else labelList = mAllLabels.returnLabelList(assocList);
                Iterator label_itr = labelList.iterator();
                while (label_itr.hasNext()) arcString.append(label_itr.next()
                        .toString()).append(" ");
                if(assoc_itr.hasNext())
                {
                    arcString.append(" -  ");
                    hyphen = true;
                }
                count++;
            }
            // Removes last hyphen.
            if(hyphen) arcString = arcString.delete(arcString.length()-3, 999999);
            return arcString.toString();
        }
        
        /** prepareKeywords: Returns a maximum of keywords from a list of arc
         * labels.
         * @param unitKey : looks up the labels that belong to the arc set under
         * this key (Integer).
         * @param maxWords : the maximum number (int) of keywords per string to
         * return.
         * @return String with a maximum number of words from a list.
         */
        private String prepareKeywords(Integer unitKey, int maxWords)
        {
            // Gets the set with the arc-keys that semantically defined this
            // topic.
            LinkedHashSet arcSet = mSemUnit.getArcSet(unitKey);
            List labels = mInfoArcTable.makeLabelList(arcSet);
            StringBuffer keywordString = new StringBuffer();
            keywordString.append("[ ");
            int count = 0;
            Iterator label_itr = labels.iterator();
            while(label_itr.hasNext() && count < maxWords)
            {
                keywordString.append(label_itr.next().toString()).append(", ");
                count++;
            }
            // Replaces last comma with bracket.
            int start = keywordString.length() - 2;
            int end = keywordString.length() - 1;
            keywordString = keywordString.replace(start, end, " ]");
            return keywordString.toString();
        }
        
        /** collectTopics: When the user clicks on a doc-id, this method collects
         * all topical facets related to the selected document. The resulting
         * data are published on a new tabbed pane in the GUI.
         * @param docArray : an array with the doc-ids (Integer) selected by 
         * the user in the 'Topics' tabbed pane or from the comboBox in that pane.
         * @param selected : String identifying the user's selection.
         */
        private void collectTopics(Integer[] docArray, String selected)
        {
            prepareTopicPanel();
            setTextPane();
            // Writes the header for a selected document.
            Integer docInt = docArray[0];
            String docName = "(" + docInt + ") " + mDocTable.getFilename(docInt);
            String textHeader = "Selected document " + docName;
            selectedTextHeader.setText(textHeader);
            // Reconstructs all the text lines of the selected document.
            selectedTextPane.setText("");
            selectedTextPane.setText(rec.reconstructText(docInt, 99999));
            topicPanel.add(scrollSelectedText);
            // Collects all the topical facet sets related to these documents.
            Set unitKeySet = new HashSet();
        for (Integer docArray1 : docArray) {
            docInt = docArray1;
            unitKeySet.addAll(mSemAnalysis.relatedFacetSet(docInt));
        }
            Iterator key_itr = unitKeySet.iterator();
            // Restricted selection of topical facets.
            while(key_itr.hasNext()) selectTopicsAndDocs((Integer) key_itr.next(),
                    SELECT, false);
            // Sends the final panel to the ApplicationManager.
            ApplicationManager.addSelectedPane(topicScroll, 5, selected);
        }
        
        /** setTextPane: Prepares a text pane to reproduce the text of a document
         * selected by the user.
         */
        private void setTextPane()
        {
            // Prepares the header for the selected document.
            selectedTextHeader = new JLabel();
            selectedTextHeader.setHorizontalAlignment(JLabel.CENTER);
            selectedTextHeader.setBorder(border);
            // Sets the layout parameters for this text area.
            selectedTextPane = new JTextArea();
            selectedTextPane.setBorder(BorderFactory.createEmptyBorder(5,15,5,5));
            selectedTextPane.setEditable(false);
            selectedTextPane.setLineWrap(true);
            //selectedTextPane.setContentType("text/plain");
            int preferredWidth = 800;
            int preferredHeight = 220;
            View v = selectedTextPane.getUI().getRootView(selectedTextPane);
            v.setSize(preferredWidth, preferredHeight);
            selectedTextPane.setPreferredSize(new Dimension(preferredWidth,
                    preferredHeight));
            // Puts the text areas in their scrollPane.
            scrollSelectedText = new JScrollPane(JScrollPane.
                    VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollSelectedText.setViewportView(selectedTextPane);
            scrollSelectedText.setColumnHeaderView(selectedTextHeader);
        }
        
        /** makeValueList: Makes a list with three elements: the value of a 
         * topical facet, the number of the topical facets and a maximum of two 
         * keywords to describe this topic. The list is then sorted on info-value
         * and put in a combobox.
         * @param showList : if boolean 'true' a list with topical facets will 
         * be shown on the 'General Info'-tab.
         */
        private void makeValueList(boolean showList)
        {
            LinkedList valueList = new LinkedList();
            Iterator key_itr = mKeyMap.keySet().iterator();
            while(key_itr.hasNext())
            {
                Integer facetKey = (Integer) key_itr.next();
                Integer topicKey = (Integer) mKeyMap.get(facetKey);
                Object[] value = new Object[3];
                value[0] = mSemUnit.getValue(facetKey);
                value[1] = topicKey;
                value[2] = prepareKeywords(facetKey, 2);
                valueList.add(value);
            }
            StringBuilder facetList =
                    new StringBuilder("\n**  TopicaL Facet List "
                            + " **\nKey\tValue\tDescription\n\n");
            ArrayList<String> topicList = new ArrayList(valueList.size());
            Collections.sort(valueList, infoValue);
            Iterator value_itr = valueList.iterator();
            while(value_itr.hasNext())
            {
                Object[] value = (Object[]) value_itr.next();
                //     if(showList)
                facetList.append(value[1]).append("\t").append(df.format(value[0]))
                        .append("\t").append(value[2]).append("\n");
                //    else
                {
                    String topicStr = "<html><left>&nbsp# "+ value[1] + " - " 
                            + df.format(value[0]) +
                            " - " + value[2] + "</left></html>";
                    topicList.add(topicStr);
                }
            }
            //  if(showList)
            ApplicationManager.showText(facetList.toString(), 0);
            // else
            prepareComboBox(topicList);
        }
        
        /** prepareComboBox: Makes a comboBox with a list of topical facets
         * sorted on info-value for the user to select.
         * @param topicList : ArrayList with topical facets (String).
         */
        private void prepareComboBox(ArrayList<String> topicList)
        {
            String[] topics = topicList.toArray(new String[topicList.size()]);
            topicBox = new JComboBox(topics);
            topicBox.setMinimumSize(new Dimension(300, 30));
            topicBox.setForeground(Color.blue);
            topicBox.addActionListener((ActionEvent event) -> {
                comboSelectionPerformed(event);
            });
            boxPanel = new JPanel();
            boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.PAGE_AXIS));
            boxPanel.setBackground(Color.blue);
            boxPanel.add(Box.createRigidArea(new Dimension(0,3)));
            boxPanel.add(topicBox);
            boxPanel.add(Box.createRigidArea(new Dimension(0,3)));
            Border whiteline = BorderFactory.createLineBorder(Color.white);
            TitledBorder title = BorderFactory.
                    createTitledBorder(whiteline, "Topical facet  -  "
                            + "Informative Value  -  Keyword");
            title.setTitleJustification(TitledBorder.CENTER);
            title.setTitleColor(Color.white);
            boxPanel.setBorder(title);
        }
        
        /** comboSelectionPerformed: Action triggered by the user when selecting 
         * a topical facet in the comboBox. Opens a new tabbedPane in the GUI
         * with the topic data of the selection and all its related topic facet sets.
         * @param event : topic selected
         */
        public void comboSelectionPerformed(ActionEvent event)
        {
            String topSelect = (String) topicBox.getSelectedItem();
            String topKey = topSelect.substring(19, topSelect.indexOf("-"))
                    .trim();
            Set docSet = mSemUnit.getDocSet(getFacetKey(Integer.
                    valueOf(topKey)));
            Integer[] docArray = (Integer[])docSet.
                    toArray(new Integer[docSet.size()]);
            String selection = "Topic " + topSelect.substring(19, 
                    topSelect.indexOf("-")).trim();
            collectTopics(docArray, selection);
        }
        
        /** getFacetKey: Finds the facetKey that goes with this topicKey.
         * @param topKey : the key linked to a topical facet in the GUI.
         * @return the internal facetKey.
         */
        private Integer getFacetKey(Integer topKey)
        {
            Integer facetKey = -1;
            Iterator key_itr = mKeyMap.keySet().iterator();
            while(key_itr.hasNext())
            {
                Integer setKey = (Integer) key_itr.next();
                Integer topicKey = (Integer) mKeyMap.get(setKey);
                if(topicKey.equals(topKey))
                {
                    facetKey = setKey;
                    break;   // Early exit.
                }
            }
            return facetKey;
        }
        
        /** Comparator sorts list on the info value from high to low.
         */
        private final Comparator infoValue =(Comparator)(Object o1, Object o2)->
        {
            Object[] b1 = (Object[])o1;
            Object[] b2 = (Object[])o2;
            Double d1 = (Double) b1[0];
            Double d2 = (Double) b2[0];
            return d2.compareTo(d1);
    };
        
        /** unusedDocArray: Returns an array with all the loose documents that 
         * do not collaborate with any topical facet of this dataGraph. 
         * The list is reproduced at the bottom of the 'Topics' tabbed pane.
         * Unused docs are rescanned in a next session with a different scope.
         * @return String array with unused document names (String).
         */
        private String[] unusedDocArray()
        {
            int i = 0;
            String[] unusedArray = new String[mUnusedDocSet.size()];
            Iterator doc_itr = mUnusedDocSet.iterator();
            while(doc_itr.hasNext())
            {
                int docNr = ((Integer) doc_itr.next());
                // Stores the filenames belonging to the unused doc-ids in
                // an array.
                unusedArray[i] = "(" + docNr + ") " + mDocTable
                        .getFilename(docNr) + '\n';
                i++;
            }
            return unusedArray;
        }
        
        /** getTextPane: Returns the textPane with the full text from a
         * selected document.
         * @return selectedTextPane, a text area with a reconstructed text.
         */
        public static JTextArea getTextPane()
        {
            return selectedTextPane;
        }
        
        /** setUnusedDocs: TopicMaker makes available a set with all documents
         * from this session that were not involved in the topic making.
         * They get another chance in a next session.
         * @param unusedDocs : SortedSet with docIds (Integer)
         */
        public static void setUnusedDocs(SortedSet unusedDocs)
        {
            mUnusedDocSet = unusedDocs;
        }
        
    }
