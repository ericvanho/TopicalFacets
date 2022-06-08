package ericvh.TDT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;


/** Class QueryInput invites the user to provide 'search' material for the 
 * system. If necessary the query can be tokenized. The system expects one list 
 * of related tokens per retrieval task. Each list of tokens gets a name to 
 * identify the results. Each retrieval task gets one of six possible job
 * descriptions:
 * The tokens are called 'seeds' because the application will 'grow' relevant topical facets on them.
 * 
 * @author Eric Van Horenbeeck
 * Created on June 21, 2005, 16:46
 * Revision July 22, 2006
 */

// Job descriptions:
// - selecting all documents relevant to the seed document (assignment '0');
// - Question&Answer searches for a saved query that matches the question and answers with the saved main core topical facets
// from the matching query (assignmen1);
// - constructing a timeline inside the scope of this task with sources related to the seed document (assignment '2');
// - performing a simple text search with one keyword (assignment '3');
// - the 'serendipity' application: generates a few random tokens and tries to find a coherent semantic relation between them (assignment '4');
// - reading and transferring the data from a saved similarity view (assignment '5');

public class QueryInput extends JFrame
{
    // Pane in the GUI to recieve the text.
    private JTextPane textPane;
    // Text object.
    private final JTextComponent textComp;
    // Various buttons.
    private JButton nameBtn;
    private JButton assignBtn;
    private JButton tokenizeBtn;
    private JButton newTaskBtn;
    private final JButton confirmBtn;
    private final JButton cancelBtn;
    // Label to hold various messages.
    private JLabel message;
    // ComboBox with retrieval task assignment list.
    private JComboBox assignBox;
    // Input field for the retriaval task name
    private JTextField inputText;
    // Label border.
    private final Border border =
            BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
    // The system clipboard to temporary hold copied objects.
    private Clipboard clipboard;
    // File menu actions.
    private final Action openAction = new OpenAction();
    private final Action saveAction = new SaveAction();
    // Map to contain the tokenized elements with task name (String) 
    // and assignment (ArrayList).
    private Map mSeedMap;
    private Map mArcMap;
    private Map mTokenMap;
    // Set with tokens unknown to this application.
    private Set mUnknownTokenSet;
    // Retrieval task name
    private String mTaskName;
    private final String mDefaultTaskName = "Task";
    // The query as a linkedList of tokenIds.
    private LinkedList mQuery;
    // Assignment. Default '0' for normal document retrieval. 
    // Other possible values: 
    // '1' retrieving topical facets to answer a question (Q&A); 
    // '2' retrieving a timeline and sources; 
    // '3' simple text search with one keyword; 
    // '4' serendipity generated with random tokens; 
    // '5' transfer data from a saved similarity view.
    private int mAssign = 0;
    private final int mRandom = 10;
    // The maximum number of tokens allowed to formulate a query.
    private static int mMaxToken;
    // Task counter.
    private int mTaskCount;
    // The limit of the number of input tokens is reached.
    private static boolean mRequestLimitMessage;
    // Miscellaneous
    private InputOutput io = new InputOutput();
    private LabelTable labels = new LabelTable();
    private final DefaultHighlightPainter painter = 
            new DefaultHighlightPainter(Color.yellow);
    private final String separator = System.getProperty("file.separator");
    private final Pattern stringP = Pattern.compile("\\s(.*?)\\s |(\\d)+");
    // String with community name now in view.
    private final String community = ApplicationManager.getCommunity();
    // String with the full path to the query result data. ApplicationManager 
    // will use it to reconstruct a view on this task.
    private String mResultPath;
    
    // Singleton instance
    private static final QueryInput inputInstance = new QueryInput();
    
    /** Singleton Constructor
     * Constructs the GUI for the query and task name input.
     */
    private QueryInput()
    {
        super("Retrieval Task");
        setMaxToken(75);
        Image icon = ApplicationManager.getIcon();
        setIconImage(new ImageIcon(icon).getImage());
        setPreferredSize(new Dimension(510, 600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        textComp = makeTextComponent();
       // defineActions();
        setJMenuBar(makeMenuBar());
        Container content = getContentPane();
        setContentPane(content);
        // 'Confirm' button ends the input of retrieval tasks and transfers the
        // data to the ApplicationManager.
        confirmBtn = new JButton("Confirm");
        confirmBtn.addActionListener((ActionEvent event) -> {
            confirmPerformed(event);
        });
        confirmBtn.setEnabled(false);
        cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener((ActionEvent event) -> {
            exitActionPerformed(event);
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(cancelBtn, BorderLayout.CENTER);
        buttonPanel.add(confirmBtn, BorderLayout.CENTER);
        content.add(buttonPanel,BorderLayout.SOUTH);
        panel.add(Box.createRigidArea(new Dimension(5, 0)));
        panel.add(makeInputPanel());
        panel.add(Box.createRigidArea(new Dimension(5, 0)));
        content.add(panel, BorderLayout.CENTER);
        setLocation(120,50);
        pack();
        setVisible(true);
        mSeedMap = new HashMap();
        mTaskName = "";
        mTaskCount = 1;
    }
    
    /**
     * getQueryInputInstance: dialog to formulate a question. See class comment
     * for more details.
     * @return a singleton instance of this class.
     */
    public static QueryInput getQueryInputInstance()
    {
        return inputInstance;
    }
    
    /** makeTextComponent: Creates a JTextComponent to display the input data.
     * @return the text component.
     */
    private JTextComponent makeTextComponent()
    {
        textPane = new JTextPane();
        textPane.setContentType("text/plain");
        textPane.setMargin(new Insets(5,10,5,10));
        textPane.setEnabled(false);
        return textPane;
    }
    
    /** makeInputPanel: Creates a panel with text fields and buttons to get the 
     * name(s) of the tasks to perform; a description of the retrieval operation
     * to perform on the data and the search data.
     * @return the input panel for insertion in the GUI.
     */
    private JPanel makeInputPanel()
    {
        // User instructions.
        JLabel inputLbl = new JLabel("<html><left>1. Retrieve an existing task "
                + "or create a new one.<br>2. Give a name for a new task. Enter"
                + " an empty string to generate a default name.<br>3. Select a "
                + "job description, validate with the 'Assign' button <br>4. "
                + "Enter the data for this task (Step 2). Tokenize if necessary."
                + "<br>&nbsp &nbsp Adding or removing tokens manually is"
                + " possible.<br>5. The 'New Task' button writes the data to a"
                + " map for further treatment. Save to disk for reuse.<br>6. "
                + "Repeat from step 1 to add a task or push 'Confirm' to return"
                + " to the main window.</left></html>", SwingConstants.LEFT);
        inputLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        inputLbl.setAlignmentX(LEFT_ALIGNMENT);
        JPanel instructPanel = new JPanel();
        instructPanel.setLayout(new BoxLayout(instructPanel, 
                BoxLayout.LINE_AXIS));
        instructPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        instructPanel.add(inputLbl);
        instructPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        
        // Step One: User is to name this task.
        inputText = new JTextField();
        inputText.setMinimumSize(new Dimension(200, 20));
        inputText.setBorder(border);
        nameBtn = new JButton("Name");
        nameBtn.addActionListener((ActionEvent event) -> {
            namePerformed(event);
        });
        // Assignment button.
        assignBtn = new JButton("Assign");
        assignBtn.setEnabled(false);
        assignBtn.addActionListener((ActionEvent event) -> {
            assignPerformed(event);
        });
        JPanel button1Panel = new JPanel();
        button1Panel.setLayout(new BoxLayout(button1Panel, BoxLayout.LINE_AXIS));
        button1Panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        button1Panel.add(nameBtn);
        button1Panel.add(Box.createRigidArea(new Dimension(5, 0)));
        button1Panel.add(assignBtn);
        button1Panel.add(Box.createRigidArea(new Dimension(5, 0)));
        // Step Two : User assigns a retrieval operation to preform on these data.
        String[] jobList =
        {"Collect related documents","Q&A","Collect timeline and sources",
         "Simple text search", "Random serendipity", "Transfer similarity data"};
        assignBox = new JComboBox(jobList);
        assignBox.setBorder(border);
        assignBox.setSize(100, 20);
        assignBox.setSelectedIndex(0);
        JPanel inputField = new JPanel();
        inputField.setLayout(new BoxLayout(inputField, BoxLayout.LINE_AXIS));
        inputField.add(Box.createRigidArea(new Dimension(5, 0)));
        inputField.add(inputText);
        inputField.add(Box.createRigidArea(new Dimension(5, 0)));
        inputField.add(assignBox);
        inputField.add(Box.createRigidArea(new Dimension(5, 0)));
        
        // Puts steps 1 and 2 in a panel
        JPanel stepOnePanel = new JPanel();
        stepOnePanel.setLayout(new BoxLayout(stepOnePanel, BoxLayout.PAGE_AXIS));
        stepOnePanel.setBorder(BorderFactory.createTitledBorder(border,
                "Step 1: Task Name, Job Assignment and Relation Type"));
        stepOnePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        stepOnePanel.add(inputField);
        stepOnePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        stepOnePanel.add(button1Panel);
        
        // Step Three : User performs data input in the text component.
        JScrollPane textScroll = new JScrollPane(textComp, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textScroll.setPreferredSize(new Dimension(200,200));
        textScroll.setBorder(BorderFactory.createEtchedBorder());
        JPanel button3Panel = new JPanel();
        button3Panel.setLayout(new BoxLayout(button3Panel, BoxLayout.LINE_AXIS));
        button3Panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        // 'Tokenize' button tokenizes the text in the area.
        tokenizeBtn = new JButton("Tokenize");
        tokenizeBtn.setEnabled(false);
        tokenizeBtn.addActionListener((ActionEvent event) -> {
            tokenizerRequest(event);
        });
        // 'New task' button saves the data with the retrieval task name and
        // restarts the procedure for the next task.
        newTaskBtn = new JButton("New Task");
        newTaskBtn.setEnabled(false);
        newTaskBtn.addActionListener((ActionEvent event) -> {
            newTaskPerformed(event);
        });
        button3Panel.add(tokenizeBtn);
        button3Panel.add(Box.createRigidArea(new Dimension(5, 0)));
        button3Panel.add(newTaskBtn);
        JPanel stepThreePanel = new JPanel();
        stepThreePanel.setLayout(new BoxLayout(stepThreePanel,
                BoxLayout.PAGE_AXIS));
        stepThreePanel.setBorder(BorderFactory.createTitledBorder(border, 
                "Step 2: Task Data"));
        stepThreePanel.add(Box.createRigidArea(new Dimension(0,5)));
        stepThreePanel.add(textScroll);
        stepThreePanel.add(Box.createRigidArea(new Dimension(0,5)));
        stepThreePanel.add(button3Panel);
        // Putting it all together.
        JPanel finalPanel = new JPanel();
        finalPanel.setLayout(new BoxLayout(finalPanel, BoxLayout.PAGE_AXIS));
        finalPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        finalPanel.add(instructPanel);
        finalPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        finalPanel.add(stepOnePanel);
        finalPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        // Room for messages.
        message = new JLabel("",SwingConstants.CENTER);
        message.setAlignmentX(CENTER_ALIGNMENT);
        message.setText("");
        finalPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        finalPanel.add(message);
        finalPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        finalPanel.add(stepThreePanel);
        return finalPanel;
    }
    
    /** defineActions: Defines the Edit actions: Cut, Copy, Paste, Select All.
     */
    private void defineActions()
    {
        Action act;
        act = textComp.getActionMap().get(DefaultEditorKit.cutAction);
        act.putValue(Action.NAME, "Cut");
        act = textComp.getActionMap().get(DefaultEditorKit.copyAction);
        act.putValue(Action.NAME, "Copy");
        act = textComp.getActionMap().get(DefaultEditorKit.pasteAction);
        act.putValue(Action.NAME, "Paste");
        act = textComp.getActionMap().get(DefaultEditorKit.selectAllAction);
        act.putValue(Action.NAME, "Select All");
    }
    
    /** makeMenuBar: Creates a JMenuBar with File & Edit menus and their 
     * action listeners.
     * @return the menu bar.
     */
    private JMenuBar makeMenuBar()
    {
        JMenuBar menubar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenu edit = new JMenu("Edit");
        menubar.add(file);
        menubar.add(edit);
        JMenuItem openTaskMenuItem = new JMenuItem("Select TaskMap");
        openTaskMenuItem.addActionListener((ActionEvent event) -> {
            try {
                readMap(event, "task");
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        });
        JMenuItem openResultMenuItem = new JMenuItem("Select Query Results");
        openResultMenuItem.addActionListener((ActionEvent event) -> {
            try {
                readMap(event, "result");
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        });
        file.add(getOpenAction());
        file.add(openTaskMenuItem);
        file.add(openResultMenuItem);
        file.add(getSaveAction());
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener((ActionEvent event) -> {
            exitActionPerformed(event);
        });
        JMenuItem clearAllItem = new JMenuItem("Clear All");
        clearAllItem.addActionListener((ActionEvent event) -> {
            clearActionPerformed(event);
        });
        file.add(exitMenuItem);
        JMenuItem findItem = new JMenuItem("Find");
        findItem.addActionListener((ActionEvent event) -> {
            findItemActionPerformed(event);
        });
        edit.add(findItem);
        edit.add(textComp.getActionMap().get(DefaultEditorKit.cutAction));
        edit.add(textComp.getActionMap().get(DefaultEditorKit.copyAction));
        edit.add(textComp.getActionMap().get(DefaultEditorKit.pasteAction));
        edit.add(clearAllItem);
        edit.add(textComp.getActionMap().get(DefaultEditorKit.selectAllAction));
        return menubar;
    }
    
    /** actionPerformed: Appropriate action when the user selects 'Cut',
     * 'Copy' or 'Paste' in the menu.
     * @param event : the selected action.
     */
    public void actionPerformed(ActionEvent event)
    {
        clipboard = getToolkit().getSystemClipboard();
        String cmd = event.getActionCommand();
        if (cmd.equalsIgnoreCase("copy"))
        {
            StringSelection data = new StringSelection(textPane.getText());
            clipboard.setContents(data, data);
        }
        else if (cmd.equalsIgnoreCase("paste"))
        {
            Transferable clipData = clipboard.getContents(this);
            String clip;
            try
            {
                clip = (String)(clipData.getTransferData(DataFlavor.stringFlavor));
            }
            catch (UnsupportedFlavorException | IOException e)
            {
                clip = e.toString();
            }
            textPane.setText(clip);
        }
        else if (cmd.equalsIgnoreCase("cut"))
        {
            StringSelection data = new StringSelection(textPane.getText());
            clipboard.setContents(data, data);
            textPane.setText("");
        }
    }
    
    /** namePerformed: Appropriate action when the user pushes the 'Name' button
     * in the Step 1 panel. The user is invited to give a name for this 
     * retrieval task. A default name is given when an empty string was entered.
     * @param event : the selected action.
     */
    private void namePerformed(ActionEvent event)
    {
        textPane.setText("");
        mTaskName = inputText.getText().trim();
        // Creates default name when an empty string was entered.
        if(mTaskName.length() == 0)
        {
            mTaskName = getNameCount(mDefaultTaskName, mTaskCount);
            mTaskCount++;
        }
        inputText.setText(mTaskName);
        inputText.setEnabled(false);
        nameBtn.setEnabled(false);
        // Gets the job assignment for this retrieval task before entering 
        // a new name.
        message.setText("Select a job description for this task.");
        assignBtn.setEnabled(true);
        confirmBtn.setEnabled(false);
    }
    
    /** getNameCount: Returns a default retrieval task name made with a string 
     * and a number.
     * @param name : String with taskname.
     * @param nameCount : sequential counter (int);
     * @return String with name and a sequential number.
     */
    private String getNameCount(String name, int nameCount)
    {
        if(nameCount < 10) mTaskName = name + "00" + nameCount;
        else if (nameCount < 100) mTaskName = name + "0" + nameCount;
        else mTaskName = name + "" + nameCount;
        return mTaskName;
    }
    
    /** assignPerformed: Listens to the 'Assign' button.
     * @param event : the button pressed.
     */
    public void assignPerformed(ActionEvent event)
    {
        mAssign = assignBox.getSelectedIndex();
        mTaskName += "_" + mAssign;
        if(mAssign == 3) getMessage(10);
        else
        {
            if(mAssign == 4) getMessage(11);
            tokenizeBtn.setEnabled(true);
        }
        assignBtn.setEnabled(false);
        inputText.setText(mTaskName);
        textPane.setEnabled(true);
        textComp.requestFocus();
        message.setText("Enter data.");
        if(mAssign > 0) newTaskBtn.setEnabled(true);
    }
    
    /** tokenizerRequest: Starts tokenizing the text string when the user 
     * clicks the 'Tokenize' button.
     * @param event : appropriate action when 'Tokenize' was pushed.
     */
    private void tokenizerRequest(ActionEvent event)
    {
        
        tokenizeBtn.setEnabled(false);
        // Gets the raw text as it appears in the text area.
        String text = textComp.getText();
        if(text.length() == 0)
        {
            getMessage(1);
            tokenizeBtn.setEnabled(true);
            message.setText("Enter data.");
            textComp.requestFocus();
        }
        else
        {
            try
            {
                String t = "";
                int tokenCount = 0;
                // Position of a token in this text.
                int currentPosition;
                // Counts the number of labels unknown to the application 
                // appearing in this text.
                int zeroLabel = 0;
                // Counts the token with the highest frequency in this text.
                int maxFrequency = 0;
                mRequestLimitMessage = false;
                // The identification of a token delivered by the application.
                Integer tokenId;
                // List with all the positions of a token in this text.
                List positionList;
                // Map to hold a token-id as key and a list of positions as value.
                mTokenMap = new HashMap();
                // Set with tokens unknown to the application.
                mUnknownTokenSet = new HashSet();
                // Final map to transfer to the ApplicationManager for further 
                // treatment.
                mArcMap = new HashMap();
                
                labels = io.readLTable(ApplicationManager.getWorkPath()+ 
                        separator + "AllLabels.lbls");
                // Generates a five word random string to start a 'serendipity' 
                // search.
                if(mAssign == 4) // && text.equals("*random*"))
                {
                    List tokenList = new ArrayList(mRandom);
                    tokenList.addAll(getRandomWords());
                    List labelList = labels.returnLabelList(tokenList);
                    textComp.setText("");
                    String randomString = "";
                    Iterator label_itr = labelList.iterator();
                    while(label_itr.hasNext())
                    {
                        randomString += " " + label_itr.next().toString();
                    }
                    textComp.setText(randomString);
                }
                else if (mAssign == 0)
                {
                    // Tokenizer returns a map with the token as key and 
                    // positions in the text and an array with the number of 
                    // tokens and types.
                    Tokenizer tok = new Tokenizer();
                    // The intake of tokens is limited to a given maximum. 
                    // The remaining input is ignored.
                    Map tmpMap = tok.extractTokens(text, getMaxToken());
                    if(mRequestLimitMessage == true) getMessage(9);
                    int[] tokTypeCount;
                    tokTypeCount = tok.getTokenTypeCount();
                    // Parses the StringBuffer with token positions and looks up
                    // the token-ids for the labels found in this text.
                    Iterator tok_itr = tmpMap.keySet().iterator();
                    while(tok_itr.hasNext())
                    {
                        String token = tok_itr.next().toString();
                        // Attributes a dummy value to a token unknown to the 
                        // application and counts these zero occurrences.
                        // The unknown token is saved in the unknownTokenSet.
                        try
                        {
                            tokenId = labels.getVertexIndex(token);
                            if(tokenId == null)
                            {
                                tokenId = 0;
                                mUnknownTokenSet.add(token);
                                zeroLabel++;
                            }
                            else tokenCount++;
                        }
                        catch(Exception e)
                        {
                            tokenId = 0;
                            mUnknownTokenSet.add(token);
                            zeroLabel++;
                        }
                        String positions = tmpMap.get(token).toString();
                        positionList = new ArrayList();
                        t += token + " [";
                        // Regex pattern.
                        Matcher matcher = stringP.matcher(positions); 
                        while (matcher.find())
                        {
                            currentPosition = Integer.parseInt(matcher.group());
                            positionList.add(currentPosition);
                            t += "" + currentPosition + ", ";
                            currentPosition = 0;
                        }
                        t = t.substring(0, t.lastIndexOf(","));
                        t += "]\n";
                        mTokenMap.put(tokenId, positionList);
                        // Looks for the highest token frequency in this text.
                        if(positionList.size() > maxFrequency) 
                            maxFrequency = positionList.size();
                    }
                    // Issues a warning when more than half the labels are 
                    // unknown to the application.
                    if(zeroLabel > mTokenMap.size() / 2)
                    {
                        getMessage(8);
                        tokenizeBtn.setEnabled(true);
                    }
                    // Shows the tokenized text in the text area.
                    textPane.setText(t);
                    // Transforms this map with tokens into a map with arcs.
                    mArcMap = makeArcMap();
                    // Puts the doc statistics in the arcMap. 
                    // The translateSeedMap-method of the TopicRetriever class
                    // will unpack the data.
                    List statList = new ArrayList();
                    statList.add(maxFrequency);
                    statList.add(tokTypeCount[0]);
                    statList.add(tokTypeCount[1]);
                    statList.add(0);
                    statList.add("seedDoc");
                    mArcMap.put("stat", statList);
                }
                newTaskBtn.setEnabled(true);
            }
            catch(NullPointerException npe)
            {
                getMessage(1);
                tokenizeBtn.setEnabled(true);
            }
            catch(Exception e)
            {
                getMessage(1);
                e.printStackTrace(System.err);
            }
        }
    }
    
    /** newTaskPerformed: The user can edit the tokens in the text area. 
     * After the 'Next Task' button is pushed, tokens are saved in a map with 
     * their task name and job assignment.
     * @param event : appropriate action when 'Next Task' was pushed in the
     * Task Data Input panel.
     */
    private void newTaskPerformed(ActionEvent event)
    {
        List seedList = new ArrayList();
        String seeds = textComp.getText().trim();
        if(seeds.length() == 0)  getMessage(1);
        if(mAssign == 0)
        {
            mSeedMap.put(mTaskName, mArcMap);
            message.setText("Task '" + mTaskName + "' is stored with " +
                    (mArcMap.size() - 1) + " arcs in the 'seedMap'.");
        }
        else
        {
            StringTokenizer st = new StringTokenizer(seeds);
            if(mAssign != 3)
            {
                while (st.hasMoreTokens()) seedList.add(st.nextToken().trim());
                Collections.sort(seedList);
                message.setText("Task '" + mTaskName + "' is stored with " + 
                        seedList.size() + " tokens in the 'seedMap'.");
            }
            else
            {
                seedList.add(st.nextToken().trim());
                message.setText("Task '" + mTaskName + "' is ready for"
                        + " a simple text search");
            }
            mSeedMap.put(mTaskName, seedList);
        }
        confirmBtn.setEnabled(true);
        nameBtn.setEnabled(true);
        inputText.setEnabled(true);
        textPane.setEnabled(false);
        tokenizeBtn.setEnabled(false);
        newTaskBtn.setEnabled(false);
        inputText.setText("");
        inputText.requestFocus();
    }
    
    /** makeArcMap: Transforms a map with token-types and their position in the 
     * text into a map with arcs.
     * @return HashMap with arc key (String) as key and the position (int) of 
     * the arc in this text.
     */
    private Map makeArcMap()
    {
        Map arcMap = new HashMap();
        SortedMap tmpMap = new TreeMap();
        mQuery = new LinkedList();
        int arcPosition = 0;
        // Makes a map with the position of a token as key (position is unique)
        // and token as value.
        Iterator tok_itr = mTokenMap.keySet().iterator();
        while(tok_itr.hasNext())
        {
            Integer tokenId = (Integer) tok_itr.next();
            List positions = (ArrayList) mTokenMap.get(tokenId);
            Iterator pos_itr = positions.iterator();
            while(pos_itr.hasNext())
            {
                int position = ((Integer) pos_itr.next());
                tmpMap.put(position,tokenId);
            }
        }
        // Combines two consecutive tokens into an arc key.
        Integer prevToken = 0;
        Iterator arc_itr = tmpMap.keySet().iterator();
        while(arc_itr.hasNext())
        {
            int position = ((Integer) arc_itr.next());
            Integer tokenId = (Integer) tmpMap.get(position);
            // Gathers tokenIds to reconstruct a clean query string later.
            mQuery.add(tokenId);        
            if(arcPosition > 0)
            {
                String arcKey = prevToken + "*" + tokenId;
                arcMap.put(arcKey, arcPosition);
            }
            arcPosition++;
            prevToken = tokenId;
        }
        return arcMap;
    }
    
    /** getLimitMessage: A message request from the Tokenizer when the maximum
     * number of input tokens is reached.
     */
    public static void getLimitMessage()
    {
        mRequestLimitMessage = true;
    }
    
    /** clearActionPerformed: Appropriate action when the user selects 'Clear'
     * in the Edit menu.
     * @param event : the selected action.
     */
    private void clearActionPerformed(ActionEvent event)
    {
        textComp.setText("");
    }
    
    /** getOpenAction: Appropriate action when the user selects 'Open'
     * in the File menu.
     * @return the open file action.
     */
    private Action getOpenAction()
    {
        return openAction;
    }
    
    /** getSaveAction: Appropriate action when the user selects 'Save' 
     * in the File menu.
     * @return the save file action.
     */
    private Action getSaveAction()
    {
        return saveAction;
    }
    
    /** exitActionPerformed: Appropriate action when the user selects 'Exit' 
     * in the File menu or pressed the 'Cancel'
     * button.
     * @param event : the selected action.
     */
    private void exitActionPerformed(ActionEvent event)
    {
        this.dispose();
    }
    
    /** confirmPerformed: Appropriate action when the user pushes the 'Confirm'
     * button. The seedMap containing the retrieval tasks with the companion 
     * tokens is now transferred to the ApplicationManager.
     * The next step includes the file reading and processing of the data according
     * to the job description flag in the task name.
     * @param event : the selected action.
     */
    private void confirmPerformed(ActionEvent event)
    {
        if(mAssign == 5) ApplicationManager.setResultFile(mResultPath);
        else { ApplicationManager.setSeeds(mSeedMap, mUnknownTokenSet, mQuery);
        }
        getMessage(2);
        labels = null;
        io = null;
        mSeedMap = null;
        mArcMap = null;
        mTokenMap = null;
        this.dispose();
    }
    
    /** OpenAction class requests to open an existing file.
     */
    class OpenAction extends AbstractAction
    {
        public OpenAction()
        {
            super("Open");
        }
        
        /** actionPerformed: Queries the user for a filename and attempts to 
         * open and read this file into the text component.
         */
        @Override
        public void actionPerformed(ActionEvent ev)
        {
            JFileChooser fc = new JFileChooser();
            File dir = fc.getCurrentDirectory();
            String corpusFiles = ApplicationManager.getCorpusfiles();
            fc.setCurrentDirectory(new File(dir + corpusFiles));
            if (fc.showOpenDialog(QueryInput.this) != JFileChooser
                    .APPROVE_OPTION) return;
            File file = fc.getSelectedFile();
            if (file == null) return;
            FileReader reader = null;
            try
            {
                reader = new FileReader(file);
                textComp.read(reader, null);
            }
            catch (IOException ex)
            {
                getMessage(6);
                ex.printStackTrace(System.err);
            }
            finally
            {
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (IOException x)
                    { 
                        x.printStackTrace(System.err);
                    }
                }
            }
        }
    }
    
    /** SaveAction class writes the retrieval task map to a file.
     */
    class SaveAction extends AbstractAction
    {
        /**
         */
        public SaveAction()
        {
            super("Save TaskMap");
        }
        
        /** actionPerformed: Writes the taskMap to a file on disk.
         * @param event : the selected action.
         */
        @Override
        public void actionPerformed(ActionEvent event)
        {
            if(mSeedMap.isEmpty()) getMessage(4);
            else
            {
                String filePath = "";
                try {
                    filePath = ApplicationManager.getRetrievalPath() + 
                            ApplicationManager.getScope();
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
                String taskPath = makePathName(filePath, 0);
                saveFile(filePath, taskPath, 0);
            }
        }
        
        /** saveFile: Makes a pathname and saves the file after checking for 
         * duplicate filenames.
         * @param path : String with pathname of the file to save.
         * @param nameCount : a sequential counter to avoid duplicate filenames.
         */
        private void saveFile(String filePath, String path, int nameCount)
        {
            File file = new File(path);
            if(file.exists())
            {
                int answer = JOptionPane.showConfirmDialog(QueryInput.this, 
                        "This file exists already. Continue?\nPress 'Yes' to"
                                + " override or 'No' to attribute a new name",
                        "Task Map", + JOptionPane.YES_NO_OPTION);
                if(answer == JOptionPane.YES_OPTION) writeSeedMap(path);
                else
                {
                    path = makePathName(filePath, nameCount++);
                    saveFile(filePath, path, nameCount);
                }
            }
            else writeSeedMap(path);
        }
        
        /** makePathName
         */
        private String makePathName(String pathName, int nameCount)
        {
            return getNameCount(pathName, nameCount) + "_Task.tsk";
        }
        
        /** writeSeedMap
         */
        private void writeSeedMap(String path)
        {
            try
            {
                io.writeThisObject(path, mSeedMap);
                message.setText("TaskMap saved");
            }
            catch (IOException ex)
            {
                getMessage(5);
                ex.printStackTrace(System.err);
            }
        }
    }
    
    /** readMap: Reads an existing taskMap from disk into this application. 
     * The user can either append new tasks to this map or continue directly 
     * with the data processing step in the main frame.
     * @param event : the selected action.
     * @param map : the kind of directory to open: 'task' is the retrieval 
     * directory; 'result' is the result directory.
     */
    private void readMap(ActionEvent event, String map) throws IOException
    {
        String directory = "";
        JFileChooser fc = new JFileChooser();
        File dir = fc.getCurrentDirectory();
        if(map.equals("task")) directory = ApplicationManager.getRetrievalPath();
        else if(map.equals("result")) directory = ApplicationManager
                .getResultPath();
        fc.setCurrentDirectory(new File(directory));
        fc.addChoosableFileFilter(new TaskFilter());
        fc.setAcceptAllFileFilterUsed(false);
        if (fc.showOpenDialog(QueryInput.this) != JFileChooser.APPROVE_OPTION) 
            return;
        File file = fc.getSelectedFile();
        String fileString = file.toString();
        String subPath = fileString.substring(fileString.lastIndexOf(separator)+1);
        String fileScope =  subPath.substring(0,17);
        String scope = ApplicationManager.getScope();
        if(fileScope.equals(scope))
        {
            // Processes only files with the appropriate suffix.
            String suffix = fileString.substring(fileString.lastIndexOf(".") + 1);
            // Reads the taskMap.
            if(suffix.endsWith("tsk"))
            {
                // Issues a warning when there is an active taskMap in use: 
                //override or abort import.
                if(mSeedMap.isEmpty()) importTaskMap(fileString, scope);
                else
                {
                    int answer = JOptionPane.showConfirmDialog(QueryInput.this,
                            "Importing this task now would override\nthe taskMap"
                                    + " in use. Continue to import?", "Task Map",
                                    + JOptionPane.YES_NO_OPTION);
                    if(answer == JOptionPane.YES_OPTION) importTaskMap(fileString,
                            scope);
                    else fc.setSelectedFile(null);
                }
            }
            // The user has selected a file with similarity data to reconstruct 
            // the view on the results of a query. The filepath is transferred 
            // to the ApplicationManager who will handle this.
            else if(suffix.endsWith("sim"))
            {
                mAssign = 5;
                String task = subPath.substring(17,subPath.lastIndexOf('_'));
                inputText.setText(task + "_5");
                nameBtn.setEnabled(false);
                message.setText("<html><center>Data from task: " + task + 
                        " will be transferred.<br> Press 'Confirm' to rebuild "
                                + "the query results or 'Cancel' to abort."
                                + "</center></html>");
                confirmBtn.setEnabled(true);
                mResultPath = fileString;
            }
            else getMessage(7);
        }
        else getMessage(7);
    }
    
    /** TaskFilter shows only directories and relevant task files in the FileChooser.
     */
    public class TaskFilter extends FileFilter
    {
        private String mSuffix;
        
        /** accept: Accepts files with suffix 'tsk' or 'sim'.
         * @param file taskFile
         * @return boolean
         */
        @Override
        public boolean accept(File file)
        {
            if (file.isDirectory()) return true;
            String fileString = file.toString();
            mSuffix = fileString.substring(fileString.lastIndexOf(".") + 1);
            if (mSuffix != null)
            {
                return mSuffix.equals("tsk") || mSuffix.equals("sim");
            }
            return false;
        }
        
        /** getDescription: Returns the suffix of the chosen file.
         * @return suffix (String).
         */
        @Override
        public String getDescription()
        {
            return mSuffix;
        }
    }
    
    /** importTaskMap: Reading the taskMap on file into the seedMap initiated 
     * by the application. Only the last task in the task map is shown.
     * @param file : full path to the task map (String).
     * @param scope : begin- and enddate (String) of this session.
     */
    private void importTaskMap(String file, String scope)
    {
        try
        {
            mSeedMap = io.readMap(file);
            textPane.setText("");
            String task = "";
            String taskList = "";
            List list;
            Map arcMap;
            int size = 0;
            Iterator seed_itr = mSeedMap.keySet().iterator();
            while(seed_itr.hasNext())
            {
                task = seed_itr.next().toString();
                int firstUScore = task.indexOf("_") + 1;
                mAssign = Integer.parseInt(task.substring(firstUScore));
                if(mAssign > 0)
                {
                    list = (ArrayList) mSeedMap.get(task);
                    taskList = list.toString();
                    size = list.size();
                }
                else
                {
                    arcMap = (HashMap) mSeedMap.get(task);
                    Iterator arc_itr = arcMap.keySet().iterator();
                    while(arc_itr.hasNext())
                    {
                        taskList += arc_itr.next().toString() + ", ";
                    }
                    size = arcMap.keySet().size();
                }
            }
            inputText.setText(task);
            textComp.setText(taskList);
            message.setText("<html><left>Retrieved an existing taskMap with scope: "
                    + scope + " and " + size + " elements.<br>Last task is shown. "
                            + "Append a new task (Step 1) or press 'Confirm' to "
                            + "proceed.</left></html>");
            confirmBtn.setEnabled(true);
        }
        catch(Exception e)
        {
            getMessage(6);
            e.printStackTrace(System.err);
        }
    }
  
    /** findItemActionPerformed: Searches for a string in the visible textArea 
     * and highlights it if found.
     * @param event : 'find' menu item selected (ActionEvent).
     */
    private void findItemActionPerformed(ActionEvent event)
    {
        String searchString;
        try
        {
            searchString = JOptionPane.showInputDialog(QueryInput.this, 
                    "Enter your search term.\nAttention! Case sensitive",
                    "Search",JOptionPane.PLAIN_MESSAGE);
            if(searchString.equals(""))
            {
                searchString = JOptionPane.showInputDialog(QueryInput.this,
                        "Not a valid search term.","Search",JOptionPane.ERROR_MESSAGE);
            }
            // Highlights the occurrences of the search string.
            if(!searchString.equals("")) search(textComp, searchString);
            
        }
        catch (NullPointerException npe)
        {
            // If 'cancel' is pressed, there is no String. No action.
        }
    }
    
    /** search: Searches for the provided string from the 'Find' dialog. 
     * Creates highlights around the occurrences found in the active text area.
     * @param textComp : text to search in text area (JTextComponent).
     * @param searchString : the string to look for.
     */
    private void search(final JTextComponent textComp, String searchString)
    {
        // Removes first all highlights from a previous search.
        removeHighlights(textComp);
        int counter = 0;
        int start = 0;
        int firstPosition = 0;
        // List highLights = new ArrayList();
        try
        {
            Highlighter highLight = textComp.getHighlighter();
            Document doc = textComp.getDocument();
            String text = doc.getText(0, doc.getLength());
            // Searches for a pattern. Sets firstPosition to the index of the
            // first instance found.
            while ((start = text.indexOf(searchString, start)) >= 0)
            {
                // Applies a highlight on the search string.
                highLight.addHighlight(start, start + searchString.length()
                        , painter);
                start += searchString.length();
                // highLights.add(start);
                if(firstPosition == 0) firstPosition = start;
                counter++;
            }
            if (counter == 1)
            {
                textComp.setCaretPosition(firstPosition);
                JOptionPane.showMessageDialog(QueryInput.this, "One instance of '"
                        + searchString + "' found.","Search Result", 
                        JOptionPane.INFORMATION_MESSAGE);
            }
            else if (counter > 0)
            {
                JOptionPane.showMessageDialog(QueryInput.this, counter +  
                        " instances of '" + searchString + "' found.",
                        "Search Result", JOptionPane.INFORMATION_MESSAGE);
                textComp.setCaretPosition(firstPosition);
            }
            else
                JOptionPane.showMessageDialog(QueryInput.this, "No instances of '"
                        + searchString + "' found.","Search Result", 
                        JOptionPane.INFORMATION_MESSAGE);
        }
        catch (BadLocationException be)
        {
            System.out.println("BadLocationException while highlighting search-results");
            be.printStackTrace(System.err);
        }
    }
    
    /** removeHighlights: Removes all highlights of a previous search.
     * @param textComp : text in text area (JTextComponent).
     */
    private void removeHighlights(JTextComponent textComp)
    {
        Highlighter highLight = textComp.getHighlighter();
        Highlighter.Highlight[] oldHighLight = highLight.getHighlights();
        
        for (Highlighter.Highlight oldHighLight1 : oldHighLight) {
            highLight.removeHighlight(oldHighLight1);
        }
    }
    
    /** getMessage: Issues various warning messages.
     * @param msge : a message identification (int).
     */
    private void getMessage(int msge)
    {
        String thisMessage;
        switch(msge)
        {
            case 1 -> JOptionPane.showMessageDialog(QueryInput.this,
                    "Nothing to tokenize!", "Tokenizer", JOptionPane.ERROR_MESSAGE);
            case 2 -> {
                if( mSeedMap.size() > 1) thisMessage =  mSeedMap.size() +
                        " tasks are ready for processing\n";
                else thisMessage = "A task is ready for processing\n";
                JOptionPane.showMessageDialog(ApplicationManager.getTabbedPane(),
                        thisMessage + "Press 'Start' button in the main frame "
                                + "to proceed\nor 'Cancel' to abort all action.", 
                        "Categorization Task", JOptionPane.PLAIN_MESSAGE);
            }
            case 3 -> JOptionPane.showMessageDialog(QueryInput.this,
                    "This text needs tokenizing first!", "Tokenizer", 
                    JOptionPane.ERROR_MESSAGE);
            case 4 -> JOptionPane.showMessageDialog(QueryInput.this, 
                    "Nothing to save", "Task Map", JOptionPane.ERROR_MESSAGE);
            case 5 -> JOptionPane.showMessageDialog(QueryInput.this, 
                    "File not saved", "Task Map", JOptionPane.ERROR_MESSAGE);
            case 6 -> JOptionPane.showMessageDialog(QueryInput.this,
                    "Could not open the file", "Task Map", 
                    JOptionPane.ERROR_MESSAGE);
            case 7 -> JOptionPane.showMessageDialog(QueryInput.this, 
                    "Wrong file selected", "Task & Retrieval Map", 
                    JOptionPane.ERROR_MESSAGE);
            case 8 -> JOptionPane.showMessageDialog(QueryInput.this,
                    "Most of the words are unknown to this application!\n" +
                    "Consider to retrain the program or enter a new text.",
                    "Task Preparation",JOptionPane.INFORMATION_MESSAGE);
            case 9 -> JOptionPane.showMessageDialog(QueryInput.this,
                    "The query is limited to the first " + getMaxToken() 
                            + " tokens, \nthe rest of the input will be ignored.",
                    "Query Input", JOptionPane.INFORMATION_MESSAGE);
            case 10 -> JOptionPane.showMessageDialog(QueryInput.this,
                    "Enter one keyword for a simple search", "Simple Text Search",
                    JOptionPane.INFORMATION_MESSAGE);
            case 11 -> JOptionPane.showMessageDialog(QueryInput.this,
                    "Enter *random* to generate a few random tokens."
                            + "\nThe application will then try to generate\na "
                            + "coherent story with them.", "Random Serendipity",
                            JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /** getRandomWords: Returns a list with five random numbers that will be
     * used to extract an arbitrary token. The tokens will in turn retrieve 
     * a set of documents to generate a serendipity search.
     * @return an ArrayList with five random numbers (Integer).
     */
    private List getRandomWords()
    {
        Random rand = new Random();
        int limit = 25000;
        List integerList = new ArrayList(mRandom - 1);
        for(int i = 0; i < mRandom; i++)
        {
            integerList.add(rand.nextInt(limit + i));
        }
        return integerList;
    }

    /** getMaxToken: Returns the maximum number of tokens allowed to formulate
     * a query.
     * @return the number of token in the query (int).
     */
    private int getMaxToken()
    {
        return mMaxToken;
    }

    /** setMaxToken: Sets the maximum number of tokens allowed to formulate
     * a query.
     * @param maxToken : the number of token in the query (int).
     */
    private void setMaxToken(int maxToken)
    {
        mMaxToken = maxToken;
    }
    
}
