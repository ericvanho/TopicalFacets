package ericvh.TDT;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.JOptionPane;


/** Class ResultExport saves the result of a retrieval task to a map on disk 
 * for evaluation purposes.
 * @author Eric Van Horenbeeck
 * Created on 16 september 2005, 16:07
 * Revision: August 22, 2006
 */
public class ResultExport
{
    // SortedMap with for every task a set of documents representing the result 
    // of a retrieval task.
    private SortedMap mResults;
    // SortedSet with all doc-ids retained in a retrieval task.
    private LinkedHashSet mDocSet;
    // Flag indicating if a map with results has to be saved to disk.
    private boolean mResultExists;
    // Number of tasks to expect in this session.
    private int mTaskSize;
    // The name of a task to be saved.
    private String mNewName;
    // DocTable with general information and the names of the files.
    private final DocTable mDocTable;
    
    /**Constructor
     * @param dt : DocTable instance retrieved by the ApplicationManager.
     */
    public ResultExport(DocTable dt)
    {
        mDocTable = dt;
        mResults = new TreeMap();
    }
    
    /** addResultMap: At the start of every session the previous resultMap is 
     * retrieved from disk. New tasks and results can be added and will be saved
     * together with the older information. New information on an old task is 
     * also added. If no map was found a new one is created.
     * @param resultMap : HashMap with as key a task identification (String) 
     * and a HashSet of documents (String) as value.
     */
    public void addResultMap(SortedMap resultMap)
    {
        mResults = resultMap;
        if(mResults != null) mResultExists = true;
    }
    
    /** setClusterMap: Cluster class sends a map with clusters and the 
     * corresponding task identification.
     * @param allDocs : LinkedHashSet with all doc-ids (Integer) retained in 
     * this retrieval task.
     * @param taskName : String with short task description.
     */
    public void setClusterMap(LinkedHashSet allDocs, String taskName)
    {
        mDocSet = new LinkedHashSet(allDocs);
        prepareExport(taskName);
    }
    
    /** prepareExport: Prepares an export map after receiving the data. 
     * A dialog captures the user instructions. No dialog when more than one 
     * task is processed, default values are applied.
     * @param task : the identification of this task (String).
     */
    private void prepareExport(String task)
    {
        String oldTaskName;
        // There is a result map.
        if(mResultExists)
        {
            // Checks if the result map knows the given task name.
            oldTaskName = containsTask(task);
            if(oldTaskName.length() > 0)
            {
                // No user dialog if more than one task is being processed, default
                // value applies.
                if(mTaskSize > 1) updateResults(oldTaskName, "", 1);
                else
                {
                    int answer = JOptionPane.showConfirmDialog(ApplicationManager.getTabbedPane(),
                            "Task '" + task + "' already exists in the Result Map.\nClick 'Yes' to " +
                            "override with the new data.\nClick 'No' save under a new name,\nor " +
                            "'Cancel' to abort.", "Export Results", + JOptionPane.YES_NO_CANCEL_OPTION);
                    // Adds the new data to the set with the existing task name.
                    switch (answer) {
                        case JOptionPane.YES_OPTION -> updateResults(oldTaskName, "", 0);
                        case JOptionPane.NO_OPTION -> {
                            makeNewName(oldTaskName);
                            updateResults(oldTaskName, getName(), 0);
                        }
                        case JOptionPane.CANCEL_OPTION -> mResultExists = false;
                        default -> {
                        }
                    }
                    // Aborts the 'save' operation.
                                    }
            }
            // A result map exists but the task name is new.
            else
            {
                mResultExists = false;
                oldTaskName = task;
                // No user dialog if more than one task is being processed, default value applies.
                
                if(mTaskSize > 1)
                {
                    makeNewName(oldTaskName);
                    updateResults(oldTaskName, getName(), 1);
                }
                else
                {
                    int answer = JOptionPane.showConfirmDialog(ApplicationManager.getTabbedPane(),
                            "This is a new task.\nClick 'Yes' to save the data or\nclick 'No' to abort.",
                            "Export Results", JOptionPane.YES_NO_OPTION);
                    // Saves the data under the new name, else abort.
                    if(answer == JOptionPane.YES_OPTION)
                    {
                        makeNewName(oldTaskName);
                        updateResults(oldTaskName, getName(), 0);
                    }
                    
                }
            }
        }
        // There is no results map. A new map can be created, the data are saved under the given name.
        else
        {
            // No user dialog if more than one task is being processed, default value applies.
            oldTaskName = task;
            if(mTaskSize > 1)
            {
                makeNewName(oldTaskName);
                updateResults(oldTaskName, getName(), 1);
            }
            else
            {
                int answer = JOptionPane.showConfirmDialog(ApplicationManager.getTabbedPane(),
                        "There is no Result Map retrieved.\nClick 'Yes' to create one or" +
                        "\nclick 'No' to abort.", "Export Results", JOptionPane.YES_NO_OPTION);
                if(answer == JOptionPane.YES_OPTION)
                {
                    makeNewName(oldTaskName);
                    updateResults(oldTaskName, getName(), 0);
                }
                else if(answer == JOptionPane.NO_OPTION) mResultExists = false;
            }
        }
    }
    
    /** containsTask: The name to be saved is compared to tasks included in the result map, ignoring
     * the different versions of a task that may exist. The first match to be found is returned, it will
     * be changed later.
     * @param task : the task name to be saved (String).
     * @return boolean 'true' if this task exists in the result map, 'false' if not.
     */
    private String containsTask(String task)
    {
        Iterator result_itr = mResults.keySet().iterator();
        while(result_itr.hasNext())
        {
            String taskName = result_itr.next().toString();
            if(taskName.regionMatches(true, 0, task, 0, task.length())) return taskName;
        }
        return "";
    }
    
    /** updateResults: Depending on the answer coming from the 'Export Results' 
     * dialog the results will either override existing data, add to previous 
     * results, or be written to a new set.
     * @param oldName : the existing description of this task (String).
     * @param newName : a new name for this task (String).
     * @param defaultValue : when more tasks are processed, there is no user dialog 
     * and default values
     * apply.
     */
    private void updateResults(String oldName, String newName, int defaultValue)
    {
        String thisTask = oldName;
        // Retrieves the resultSet if there is one for this task.
        SortedSet resultSet = new TreeSet();
        // New results override the previous one under the same name.
        if(defaultValue > 0 && mResultExists) resultSet = (TreeSet) mResults
                .get(oldName);
        // Adds all doc-ids to the resultSet.
        resultSet.addAll(mDocSet);
        // Uses the new name if there is one.
        if(newName.length() > 0) thisTask = newName;
        mResults.put(thisTask.trim(), resultSet);
        mResultExists = true;
    }
    
    /** makeNewName: Makes a new task name.
     * @param oldTaskName : the original task name (String).
     */
    private void makeNewName(String oldTaskName)
    {
        int nameCount = 0;
        String newName = oldTaskName;
        int last = oldTaskName.lastIndexOf("_");
        if(last > 0)
        {
            newName = oldTaskName.substring(0, last);
            try
            {
                nameCount = Integer.parseInt(oldTaskName.substring(last + 2))+1;
            }
            // Detects if the taskname has a number attached.
            catch(NumberFormatException nfe)
            {
                nameCount = 0;
            }
        }
        if(nameCount < 10) newName = newName + "_00" + nameCount;
        else if (nameCount < 100) newName = newName + "_0" + nameCount;
        else newName = newName + "_" + nameCount;
        newName = newName.trim();
        // Makes a new name recursively, if it already exists in the resultMap.
        boolean nameExists = false;
        Iterator result_itr = mResults.keySet().iterator();
        while(result_itr.hasNext())
        {
            String taskName = result_itr.next().toString();
            if(taskName.regionMatches(true, 0, newName, 0, newName.length()))
            {
                nameExists = true;
                break;
            }
        }
        if(nameExists) makeNewName(newName);
        else setName(newName);
    }
    
    /** getName: Returns the name of this task.
     * @return name (String).
     */
    private String getName()
    {
        return mNewName;
    }
    
    /** setName: Sets the name of this task.
     * @param name : task name (String)
     */
    private void setName(String name)
    {
        this.mNewName = name;
    }
    
    /** viewResults: Presents the results of the different tasks on screen.
     */
    public void viewResults()
    {
        // If more than one task is being processed, no results on screen 
        // and no dialog.
        if(mTaskSize == 1)
        {
            int answer = JOptionPane.showConfirmDialog(ApplicationManager
                    .getTabbedPane(),
                    "Show results?\nPress 'Yes' or 'No' to cancel"
                    , "View Results", JOptionPane.YES_NO_OPTION);
            if(answer == JOptionPane.YES_OPTION)
            {
                StringBuffer resultString = new StringBuffer("\n* Retrieval"
                        + " Task Results *\n");
                Iterator result_itr = mResults.keySet().iterator();
                while(result_itr.hasNext())
                {
                    String task = result_itr.next().toString();
                    SortedSet docs = (TreeSet) mResults.get(task);
                    resultString.append("Task: ").append(task).append("\n").
                            append(docs.size()).append(" documents retrieved:\n");
                    Iterator doc_itr = docs.iterator();
                    while(doc_itr.hasNext())
                    {
                        Integer docId = (Integer) doc_itr.next();
                        String doc = mDocTable.getFilename(docId);
                        resultString.append(doc).append(", ");
                    }
                    // Removes the last comma.
                    int length = resultString.length();
                    resultString = resultString.replace(length -2, length, "\n");
                }
                ApplicationManager.showText(resultString.toString(), 0);
            }
        }
    }
    
    /** getResultMap: At the end of the session ApplicationManager writes the
     * updated resultMap to the hard disk as a 'TaskMap'.
     * @return SortedMap with task description (String) as key and a HashSet 
     * of document-ids (Integer) as value.
     */
    public SortedMap getResultMap()
    {
        if(mResultExists) return mResults;
        else return null;
    }
    
    /** resultExists: returns 'true' if a resultMap exists.
     * @return boolean 'true' if a resultMap exists.
     */
    public boolean resultExists()
    {
        return mResultExists;
    }
    
    /** setTaskSize: ApplicationManager informs on the number of tasks to 
     * include in the resultMap.
     * @param taskSize : number of tasks to handle in this session (int).
     */
    public void setTaskSize(int taskSize)
    {
        mTaskSize = taskSize;
    }
    
}
