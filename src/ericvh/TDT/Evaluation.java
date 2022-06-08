package ericvh.TDT;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/** Class Evaluation reads a map with the outcome of a topic detection test and 
 * compares it to the official TDT results.
 * @author Eric Van Horenbeeck
 * Created on 20 september 2005, 10:37
 * Revision: October 9, 2007.
 */

public class Evaluation
{
    private static final String separator = System.getProperty("file.separator");
    private static final String dir = "C:\\Documents and Settings\\Eric\\My Documents";
    private static final String workFiles = "\\Doctoraat\\Computing\\TopicDetection";
    private static final String communityName = "news_us";
    private static final String resultDir  = dir + workFiles + separator 
            + communityName + separator +
        "Results" + separator;
    private static final String researchDir  = dir + workFiles + separator 
            + "Research" + separator;
    private static final String comment = "";
    // Reading and writing methods.
    private static final InputOutput io = new InputOutput();
    // DocTable with general information and the names of the files.
    private DocTable mDocTable;
    // Maps to hold the evaluation data.
    private SortedMap mResultMap;
    private SortedMap mStandardMap;
    // Common methods from the Algorithm class.
    private final Algorithms alg = new Algorithms();
    
    /** Constructor
     */
    public Evaluation()
    {  }
    
    /** parseTDTStandard: This method is used only once to extract the official 
     * solution of the TDT1999-task for English newsfiles. The txt-file is translated
     * into a SortedMap with the task-id as key (String) and as value a SortedSet of 
     * documents (String) belonging to that task. The document names are transformed
     * into the TDT-format of this application: 'yyyymmdd_SourceSequentialNumber'. 
     * The data are written to the Research-directory as 'TDTStandard.map'.
     */
    private static void parseTDTStandard()
    {
        String standardFile  = researchDir + "OnTopicResult.txt";
        String outputFile = researchDir + "TDTStandard.map";
        try
        {
            SortedMap standardMap = new TreeMap();
            FileReader inputFileReader = new FileReader(standardFile);
            try (BufferedReader inputStream = new BufferedReader(inputFileReader)) {
                String inLine = null;
                while ((inLine = inputStream.readLine()) != null)
                {
                    String task = inLine.substring(0, 5);
                    task = task.trim();
                    String source = inLine.substring(6,9);
                    String date = inLine.substring(9, 17);
                    String file = inLine.substring(18, inLine.indexOf("fi"));
                    String language = inLine.substring(inLine.length()-4);
                    language = language.trim();
                    // Skips the Chinese (Mandarin) newsfiles.
                    if(!language.equals("MAN"))
                    {
                        SortedSet fileSet = new TreeSet();
                        // Filename in TDT format.
                        String fileName = date + "_" + source + file;
                        fileSet.add(fileName);
                        if(standardMap.containsKey(task))
                            fileSet.addAll((TreeSet)standardMap.get(task));
                        standardMap.put(task, fileSet);
                    }
                }
            }
            io.writeThisObject(outputFile, standardMap);
        }
        catch (IOException e)
        {
            System.out.println("IOException: ");
            e.printStackTrace(System.err);
        }
    }
    
    /** evaluate: Main method called by the ApplicationManager or the FinalViewer. 
     * The standard on-topic map as a task-id as key (String) and a SortedSet with 
     * documents (String) relevant to the task as value.
     * The resultMap has a task-id as key (String) and a SortedSet with docIds 
     * (Integer) as value. The map with the official sixty topic retrieval tasks is
     * read and compared with the results of the TDT-application.
     * @param dt : a DocTable class instance with general document info on all
     * documents seen by the application.
     * @param scope : the scope of the task (String).
     * @param begin : begindate of this session's scope (Calendar).
     * @param end : enddate of this session's scope (Calendar).
     * @param task : limit the evaluation to one task, or to all (String).
     * todo Issue warning when a task is not TDT related and stop evaluation.
     */
    public void evaluate(DocTable dt, String scope, Calendar begin, Calendar end, 
            String task)
    {
        // Scope of the evaluation
        Calendar beginDate = begin;
        Calendar endDate = end;
        // The maximum number of documents to show.
        int max = 15;
        String thisTask = task;
        boolean multiTask = false;
        if(thisTask.equals("all")) multiTask = true;
        // Showing most of the false alarms and missed documents if there is
        // only one task to evaluate.
        else max = 50; // Integer.MAX_VALUE;
        // Optional beta parameter for the F-score. Default value beta = 1 gives
        // equal emphasis to recall and precision. A higher beta favors recall
        // over precision.
        double beta = 1.0;
        // Formatting the number output.
        DecimalFormat df = new DecimalFormat("###.#####");
        NumberFormat pf = NumberFormat.getPercentInstance();
        pf.setMinimumFractionDigits(3);
        final String tab = "\t";
        // Gets the number of documents involved in the scope of this session.
        mDocTable = dt;
        int allDocs = mDocTable.getDocCount();
        // Reads the official score and the computed results.
        getMaps();
        // String buffer to hold the report.
        StringBuilder report = new StringBuilder("\nEvaluating TDT Results");
        // String buffer to hold a summary.
        StringBuilder summary = new StringBuilder("\nSummary for scope: " 
                + scope + "\n" + "Task" + tab +  tab + tab + "Recall" + tab 
                + "Precision" + tab + "F-score" + tab + "Miss" + tab + "False"  
                + tab + "Cost"+ "\n");
        // Average values.
        double avgPrecision = 0.0;
        double avgRecall = 0.0;
        double avgFScore = 0.0;
        double avgMiss = 0.0;
        double avgFalse = 0.0;
        double avgCost = 0.0;
        int count = 0;
        // Makes a map with only the TDT test results.
        SortedMap evalMap = collectTDTKeys(mResultMap, multiTask, thisTask);
        Set standardKeys = mStandardMap.keySet();
        // Computing the results for every task defined by thisTask.
        Iterator result_itr = evalMap.keySet().iterator();
        while(result_itr.hasNext())
        {
            String testKey = result_itr.next().toString();
            String standardKey = testKey.substring(0,5);
            SortedSet resultSet = (TreeSet) evalMap.get(testKey);
            SortedSet tmpSet = (TreeSet) mStandardMap.get(standardKey);
            // Translates the list with the official on-topic filenames to their 
            // doc-ids (Integer).
            SortedSet onTopicSet = getDocIds(tmpSet, beginDate, endDate);
            // ArrayList with a SortedSet of common objects (the s2 elements removed
            // from s1) at  index '0' and a SortedSet with the different elements at
            // index '1' (elements left  over in set s1 and s2 after removing
            // the common elements);
            List splitList = Algorithms.split(onTopicSet, resultSet);
            // Both sets agree on the retrieval of these documents.
            SortedSet shared = (TreeSet) splitList.get(0);
            // Documents lacking in one of the two sets.
            SortedSet different = (TreeSet) splitList.get(1);
            SortedSet falseAlarmSet = new TreeSet();
            SortedSet missedDoc = new TreeSet();
            // Finding out where the document is missing.
            Iterator diff_itr = different.iterator();
            while(diff_itr.hasNext())
            {
                Integer diff = (Integer) diff_itr.next();
                // If this document is only in the on-topic set, it was missed by 
                // the application.
                if(onTopicSet.contains(diff)) missedDoc.add(diff);
                // If this document is in the result set and not in the on-topic set,
                // it is a false alarm.
                else if(resultSet.contains(diff)) falseAlarmSet.add(diff);
            }
            // Contingency table from 'Detection as Multi-Topic Tracking',James Allan.
            // A
            int retrievedSize = shared.size(); 
            // B
            int missedSize = missedDoc.size(); 
            // C
            int falseSize = falseAlarmSet.size(); 
            // A + B
            int onTopic = onTopicSet.size();  
            // A + C
            int allRetrieved = retrievedSize + falseSize;
            //(A + B + C + D) - (A + B) = C + D
            int offTopic = allDocs - onTopic;                
            // Metrics
            double precision = 0.0;
            double recall = 0.0;
            double miss = 0.0;
            double falseAlarm = 0.0;
            double fScore = 0.0;
            double cost;
            // Precision, Recall, F-score and other measures.
		if(allRetrieved > 0) recall = ((double) retrievedSize) / onTopic;
            if(onTopic > 0) precision = ((double) retrievedSize) / allRetrieved;   
            if(onTopic > 0) miss = ((double) missedSize) / onTopic;
            if(offTopic > 0) falseAlarm = ((double) falseSize) / offTopic;
            if(recall + precision > 0)
            {
                fScore = ((1 + beta * beta) * recall * precision) / (recall + beta 
                        * beta * precision);
            }
            // Topic 'richness', also called 'generality' (Salton & McGill 1983). 
            // It determines the a priori probability that a story in the corpus is 
            // on a given topic.
            double richness = ((double) onTopic) / allDocs;
            // The TDT2000 consortium defined the constant terms. 
            // See 'Detection as Multi-Topic Tracking'. 
            // Constant cost parameters:
            double cMiss = 1.0;
            double cAlarm = 0.1;
            // A priori probability that a story is about a given topic:
            double pTopic = 0.02;
            // Cost function:
            cost  = (cMiss * miss * pTopic) + (cAlarm * falseAlarm * (1 - pTopic));
            // Simplified cost function:
            // cost = 0.02 * miss + 0.098 * falseAlarm;  
            // cAlarm * (1-pTopic) = 0.1 * 0.98 = 0.098
            
            // Gathering all results in a string to show in the GUI.
            report.append("\n\n* Task ").append(testKey).append(" for the period ")
                    .append(scope).append(" *\n");
            report.append("Annotated on-topic files for this task: ").append(onTopic)
                    .append(" - Total file count: ").append(allDocs)
                    .append(" - On-topic fraction: ")
                    // + " - Cores: " + (Clusters.getNumberOfCores() - 1));
                    .append(pf.format(richness)); 
            report.append("\nIdentified correctly ").append(retrievedSize).
                    append(" files. False Alarms: ").append(falseSize).
                    append(" Missed Docs: ").append(missedSize);
            report.append("\nPrecision: ").append(df.format(precision)).
                    append(" - Recall: ").append(df.format(recall))
                    .append(" - F-score: ")
                    .append(df.format(fScore)).append(" - TDT Cost: ")
                    .append(df.format(cost)).append('\n');
            if(falseSize > 0) report.append(getFaultyDocuments(falseAlarmSet, 1, max));
            if(missedSize > 0) report.append(getFaultyDocuments(missedDoc, 0, max));
            // Early exit when only one evaluation is needed.
            if(multiTask)
            {
                if(testKey.length() > 22) summary.append(testKey).append(tab);
                else summary.append(testKey).append(tab + tab);
                summary.append(df.format(recall)).append(tab)
                        .append(df.format(precision))
                        .append(tab).append(df.format(fScore)).append(tab)
                        .append(pf.format(miss)).append(tab)
                        .append(pf.format(falseAlarm))
                        .append(tab).append(df.format(cost)).append('\n');
                avgPrecision += precision;
                avgRecall += recall;
                avgFScore += fScore;
                avgMiss += miss;
                avgFalse += falseAlarm;
                avgCost += cost;
                count++;
            }
            else
            {
                // No summary necessary for just one evaluation.
                summary.delete(0,summary.length());
            }
        }
        ApplicationManager.showText(report.toString(), 0);
        // Summary of the task results with average values for precision, recall,
        // f-score and cost.
        if(multiTask)
        {
            summary.append("Average (").append(count).append(" tasks)" + tab + tab)
                    .append(df.format(avgRecall/count)).append(tab)
                    .append(df.format(avgPrecision/count)).append(tab)
                    .append(df.format(avgFScore/count)).append(tab)
                    .append(pf.format(avgMiss/count))
                    .append(tab).append(pf.format(avgFalse/count)).append(tab)
                    .append(df.format(avgCost/count)).append('\n');
            ApplicationManager.showText(summary.toString(), 0);
        }
    }
    
    /** getDocIds: Returns a set with the document ids linked to these document names.
     * @param tmpSet : a SortedSet with document names (String).
     * @param beginDate : start of this session's scope (Calendar).
     * @param endDate : end of this session's scope (Calendar).
     * @return SortedSet with docNr (int) identifying the documents.
     */
    private SortedSet getDocIds(SortedSet tmpSet, Calendar beginDate, Calendar endDate)
    {
        SortedSet docSet = new TreeSet();
        Iterator tmp_itr = tmpSet.iterator();
        Calendar date = Calendar.getInstance();
        date.set(0000, 00, 00);
        while(tmp_itr.hasNext())
        {
            String docName = tmp_itr.next().toString().trim();
            // Checks if this file's date falls within the selected 
            // time interval(scope).
            Calendar fileDate = GraphTime.getDate(date, docName.substring(docName.
                    lastIndexOf(separator)+1));
            if(GraphTime.isBetweenDates(fileDate, beginDate, endDate))
            {
                Integer docInt = mDocTable.getDocNr(docName);
                if(docInt == -1) ApplicationManager.showText("File not found: " 
                        + docName, 0);
                else docSet.add(docInt);
            }
        }
        return docSet;
    }
    
    /** getMaps: The map with the official annotated data and the map with the 
     * computed results are retrieved.
     */
    private void getMaps()
    {
        // Maps to hold the standard solution and the results from the application.
        mStandardMap = new TreeMap();
        mResultMap = new TreeMap();
        try
        {
            String resultFile  = resultDir + "TaskResult.map";
            String standardFile = researchDir + "TDTStandard.map";
            mStandardMap = io.readSortedMap(standardFile);
            mResultMap = io.readSortedMap(resultFile);
        }
        catch (Exception e)
        {
            System.out.println("Problems reading from disk:");
            e.printStackTrace(System.err);
        }
    }
    
    /** collectTDTKeys: Extracts from the resultMap a map with only the TDT-keys and 
     * the related docs.
     * @param mResultMap : SortedMap with task key as value (String) and a SortedSet 
     * of related doc-ids (Integer).
     * @param multi : boolean 'true' if there are multitasks.
     * @param task : String with the task at hand ('all' when multi task).
     */
    private SortedMap collectTDTKeys(SortedMap mResultMap, boolean multi, String task)
    {
        String taskKey = task;
        if(!multi) taskKey = task.substring(0,5);
        SortedMap tmpMap = new TreeMap();
        Set standardKeys = mStandardMap.keySet();
        Iterator result_itr = mResultMap.keySet().iterator();
        while(result_itr.hasNext())
        {
            String taskName = ((String) result_itr.next());
            String trimKey = taskName.substring(0,5);
            // Only the TDT test results will be evaluated.
            if(multi && standardKeys.contains(trimKey))
            {
                SortedSet tmpSet = (TreeSet) mResultMap.get(taskName);
                tmpMap.put(taskName, tmpSet);
            }
            else if(trimKey.equals(taskKey))
            {
                SortedSet tmpSet = (TreeSet) mResultMap.get(taskName);
                tmpMap.put(taskName, tmpSet);
            }
        }
        return tmpMap;
    }
    
    /** getFaultyDocuments: Translates a set of missed documents and 'false alarms' 
     * into a string limited to the first 'max' document names.
     * @param docSet : SortedSet with docIds (Integer).
     * @param fault : flag (int) '0' for a set of 'missed' documents and flag '1' 
     * refers to a set of 'false alarms'.
     * @param max : the maximum number of documents to shown, or all.
     * @return the documents (or a selection) rendered as a String with their docId
     * and full name.
     */
    private String getFaultyDocuments(SortedSet docSet, int fault, int max)
    {
        int setSize = docSet.size();
        int count = 0;
        String faultyResult;
        if(setSize > max)
        {
            if(fault == 0) faultyResult = "Missed (first " + max + " shown)";
            else faultyResult = "False Alarms (first " + max + " shown)";
        }
        else
        {
            if(fault == 0) faultyResult = "Missed (all shown)";
            else faultyResult = "False Alarms (all shown)";
        }
        StringBuffer docString = new StringBuffer("\n " + faultyResult + " \n");
        Iterator doc_itr = docSet.iterator();
        while(doc_itr.hasNext() && count < max)
        {
            Integer docId = (Integer) doc_itr.next();
            String doc = mDocTable.getFilename(docId);
            docString.append("(").append(docId).append(") ").append(doc).append(", ");
            count++;
        }
        // Removes the last comma.
        int length = docString.length();
        docString = docString.replace(length -2, length, ".");
        // Adds ellipsis when not all the documents of the set are shown.
        if(setSize > max) docString.append(" (...)");
        return docString.toString();
    }
    
    /** main: Entry point needed only once to parse the official standard results. 
     * Otherwise this class is evoked in the main TDT-GUI and handled by the
     * ApplicationManager.
     * @param args : Default array arguments (String).
     */
    public static void main(String[] args)
    {
        parseTDTStandard();
    }
    
}
