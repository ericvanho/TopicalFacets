package ericvh.TDT;

import java.io.File;
import java.io.Serializable;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/** Class GraphTime has date and time related methods.
 * @author  Eric Van Horenbeeck
 * Created on 2 juni 2004, 21:14
 */
public class GraphTime implements Serializable
{
    // Local settings.
    public static final Locale thisLocale = new Locale("nl_BE");
    // Map with Dutch month names and numbers.
    private static Map mMonths;
    // The scope (begin- and enddate) of a file.
    private static String fileScope = "";
    // Generic file SEPARATOR.
    private static final String SEPARATOR = System.getProperty("file.separator");
    
    /** Constructor
     */
    public GraphTime()
    {
        mMonths = new HashMap();
        setMonths();
    }
    
    /** setMonths: List with Dutch months converted into month numbers.
     */
    private void setMonths()
    {
        final String[] maanden = {"januari", "februari", "maart", "april", "mei",
                "juni", "juli", "augustus", "september", "oktober", "november",
                "december"};
                for(int i = 0; i < maanden.length; i++) mMonths.put(maanden[i],
                        String.valueOf(i+1));
    }
    
    /** getMaand: Getter returns the month number when given a Dutch month name.
     * @param maand : Dutch month as String.
     */
    private static String getMaand(String maand)
    {
        String maandNr = "99";
        
        try
        {
            maandNr = mMonths.get(maand).toString();
        }
        catch(NullPointerException ne)
        {
            System.out.println("Month not converted");
        }
        return maandNr;
    }
    
    /** *  getDate: Getter returns the date parsed from a filenname or makes a 
     * string with the scope of the search.Returns a dummy value when a file does not
     * contain a regular date or scope string. 'No date' indication when begin- or 
     * end date is empty.
     * @param date  Calendar date instance
     * @param filename : a filename (String) expected to contain a datestring 
     * or scope.
     * @return date parsed from the filename in Calendar format.
     */
    public static Calendar getDate(Calendar date, String filename)
    {
    //    Calendar date = Calendar.getInstance();
        int year, month, day;
        try
        {
            // Checks if the scope of this file is 'AllDates'.
            if(filename.substring(0,8).equals("AllDates")) 
                fileScope = "AllDates";
            // Checks if this file has a full scope date...
            else if(filename.charAt(8) == ('-') 
                    && Character.isDigit(filename.charAt(9)))                 
            {
                fileScope = filename.substring(0,17);
            }
            // ...else the file could have a single date at its front.
            else if(Character.isDigit(filename.charAt(0)))
            {
                year = Integer.parseInt(filename.substring(0,4));
                // Subtracks 1 from the month number (January = 0)
                month = Integer.parseInt(filename.substring(4,6))-1;
                day = Integer.parseInt(filename.substring(6,8));
                date.clear();
                date.set(year, month, day);
            }
            // All other files get a dummy that will be discarded by the 
            // ApplicationManager.
            else date.set(0000,00,00);
        }
        catch(StringIndexOutOfBoundsException sobe)
        {
            // Returns a dummy if this file has no date or scope in front.
            date.set(0000,00,00);
        }
        catch(NumberFormatException e)
        {
             System.out.println("Not a number");
        }
        return date;
    }
    
    /** getFileScope: Getter returns the scope (begin- and enddate) of a file
     * or 'AllDates'.
     * @return the scope 'yyyymmdd-yyyymmdd' or 'AllDates' (String).
     */
    public static String getFileScope()
    {
        return fileScope;
    }
    
    /** getFullScope: Returns the scope with a begin- and enddate as string.
     * @param filePath : a file path (String) with a full scope.
     * @return the scope (String) of this file.
     */
    public static String getFullScope(String filePath)
    {
        return filePath.substring(filePath.lastIndexOf(SEPARATOR) + 1)
                .substring(0,17);
    }
    
    /** isBetweenDates: Returns a boolean 'true' if the date of a document lies 
     * between a user-choosen begin and end date. docDate is either between begin
     * and end; equal or greater than end or equal or smaller than begin. 
     * When begin is null, there is no begin date (every file before end is 
     * processed). When end is null, there is no end date and all available files
     * from begin are used. When both end and begin are null, all files are 
     * processed (always boolean true).
     * @param docDate : date of a document extracted from the filename (Calendar)
     * @param begin : start date of the scope as set in the GUI (Calendar).
     * @param end : final date of the scope as set in the GUI (Calendar).
     * @return boolean 'true' or 'false'
     */
    public static boolean isBetweenDates(Calendar docDate, Calendar begin,
            Calendar end)
    {
        if(begin == null && end == null) return true;
        else if(begin == null && end != null) return docDate.before(end) 
                || docDate.equals(end);
        else if(begin != null && end == null) return docDate.after(begin)
                || docDate.equals(begin);
        else return (docDate.before(end) || docDate.equals(end))
                && (docDate.after(begin) || docDate.equals(begin));
    }
    
    /** isBeforeBegin: Returns boolean 'true' if the date of a document is
     * before the begindate. When both end and begin are null, all files are 
     * processed (always boolean true). When begin is null, there is no begin 
     * date and every file before end is processed. All other cases are excluded.
     * @param docDate : date of a document extracted from the filename (Calendar)
     * @param begin : start date of the scope as set in the GUI (Calendar).
     * @param end : final date of the scope as set in the GUI (Calendar).
     * @return boolean 'true' or 'false'.
     */
    public static boolean isBeforeBegin(Calendar docDate, Calendar begin, 
            Calendar end)
    {
        if(begin == null && end == null) return true;
        else if(begin == null  && end != null) return docDate.before(end);
        else if(begin != null) return docDate.before(begin);
        else return false;
    }
    
    /** setCalendar: Prepares a clean Calendar instance with given year, 
     * month and day.
     * @param year : year (int).
     * @param month : month (int).
     * @param date : the day (int).
     * @return yyyymmdd Calendar item.
     */
    public static Calendar setCalendar(int year, int month, int date)
    {
        Calendar c = Calendar.getInstance();
        // Discards remaining calendar.
        c.clear();
        c.set(year, month, date);
        return c;
    }
    
    /** normalizeDate: Incoming date is either (D)DMMYYYY or D(D) MONTH YYYY. 
     * The date is returned in a  YYYYMMDD format. If no valid date is found, 
     * today's system date is returned.
     * @param date : the raw date (String) to be converted.
     * @return clean normalized date (String).
     */
    public String normalizeDate(String date)
    {
        int end;
        String year, month, day, normDate;
        StringBuilder temp = new StringBuilder(date);
        StringBuilder datum = new StringBuilder();
        StringBuffer dag = new StringBuffer(2);
        
        // First removes useless blanks and slashes to clean up the String.
        for(int i = 0; i < temp.length(); i++)
        {
            if(Character.isLetterOrDigit(temp.charAt(i))) datum.
                    append(temp.charAt(i));
        }
        // Empties the temporary buffer.
        temp.delete(0, temp.length());
        
        int len = datum.length();
        // Gets the system date when empty.
        if(len == 0) normDate = getDateNow();
        else
        {
            end = len-4;
            year = datum.substring(end);
            String rest = datum.substring(0, end);
            switch (rest.length())
            {
                case 3 -> 
                {
                    day = "0" + rest.substring(0,1);
                    month = rest.substring(1);
                }
                case 4 -> 
                {
                    day = rest.substring(0,2);
                    month = rest.substring(2);
                }
                default -> 
                {
                    for(int i = 0; i < rest.length(); i++)
                    {
                        if(Character.isLetter(rest.charAt(i))) temp.
                                append(rest.charAt(i));
                        else dag.append(rest.charAt(i));
                    }
                    month = getMaand(temp.toString());
                    if(dag.length()== 1) day = "0" + dag;
                    else day = dag.toString();
                }
            }
            normDate = year + month + day;
        }
        return normDate;
    }
    
    /** getDateNow: Returns the system date in String format.
     * @return todays' date as a 'yyyymmdd' string.
     */
    public String getDateNow()
    {
        Calendar today = Calendar.getInstance();
        String month = timeString(today.get(Calendar.MONTH)+1);
        String day = timeString(today.get(Calendar.DATE));
        String dateNow = "" +today.get(Calendar.YEAR)+ month + day;
        return dateNow;
    }
    
    /** getModifiedDate: Returns the time a file was last modfied as a date.
     * @param file : the filename (String) to get the modified date from
     * @return the modified date (yyyymmdd) as a String
     */
    public String getModifiedDate(String file)
    {
        String dateModified = "";
        Format formatter = new SimpleDateFormat("yyyyMMdd");
        File thisFile = new File(file);
        long modifiedTime = thisFile.lastModified();
        if(modifiedTime > 0) dateModified = formatter.format(modifiedTime);
        return dateModified;
    }
    
    /** dateToString: Input is a Calendar date, output the String equivalent.
     * @param date : a Calendar date.
     * @return the String equivalent of the input.
     */
    public static String dateToString(Calendar date)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        return (formatter.format(date.getTime()));
    }
    
    /** hourMinSec: Returns the millisecond-input (long) as a String converted 
     * to hours, minutes, seconds and the remaining hunderds of the milliseconds.
     * @param milliSec : a number of milliseconds (long).
     * @return a String formated as hh:mm:ss,SS.
     */
    public static String hourMinSec(long milliSec)
    {
        String timeString = "";
        long msec = milliSec;
        int hour, min, sec, hunderd;
        hour = (int) msec/3600000;
        if(hour > 0)
        {
            timeString = hour + " hr ";
            msec = msec - hour * 3600000;
        }
        min = (int) msec /60000;
        if(min > 0)
        {
            timeString += min + " min ";
            msec = msec - min * 60000;
        }
        sec = (int) msec/1000;
        if(sec > 0) msec = msec - sec * 1000;
        hunderd = (int) msec/100;
        timeString += sec + "." + hunderd + " sec ";
        return timeString;
    }
    
    /** timeString: Expands a 1 digit time element into a 2 digit string by 
     * adding a '0' in front.
     * @param time : an int representing a time element.
     * @return 2 digit timestring (String).
     */
    public static String timeString(int time)
    {
        String timeStr;
        if(time < 10) timeStr = "0" + time;
        else timeStr = String.valueOf(time);
        return timeStr;
    }
    
    /** *  constructFileScope: Makes a double date scope from the single date in 
     * the filename by adding one day.When the date of the input file is equal to the
     * enddate of the scope of this session a day is subtracted so that the composed
     * scope never runs past the session's scope limit.
     * @param date
     * @param filePath : the name of this file (String).
     * @return the scope (String) of this file.
     */
    public static String constructFileScope(Calendar date, String filePath)
    {        
        if(ApplicationManager.getScope().equals("AllDates")) return "AllDates";
        Calendar fileDate = getDate(date, filePath);
        String thisFileScope = dateToString(fileDate);   
        String endDate = ApplicationManager.getLastDate();
        
        if(Integer.parseInt(thisFileScope) == Integer.parseInt(endDate))
        {
            fileDate.add(Calendar.DAY_OF_MONTH, -1);
            return dateToString(fileDate) + "-" + thisFileScope;
        }
        else fileDate.add(Calendar.DAY_OF_MONTH, 1);
        return thisFileScope + "-" + dateToString(fileDate);
    }
    
    /** formatScope: Returns the scope of the topic search in European format 
     * ('dd/mm/yyyy').
     * @param scope : scope of the search is 'yyyymmdd-yyyymmdd' format(String).
     * @return scope in 'dd/mm/yyyy - dd/mm/yyyy' format (String).
     */
    public static String formatScope(String scope)
    {
        if (scope.equals("AllDates")) {
            return scope;
        } else {
            return scope.substring(6, 8) + "/" + scope.substring(4, 6) + "/"
                    + scope.substring(0, 4) + " - " + scope.substring(15) + "/"
                    + scope.substring(13, 15) + "/" + scope.substring(9, 13);
        }
    }
    
    /** getFirstScope: Getter returns the first date part of a scope String.
     * @param date
     * @return first date in 'yyyymmdd' format (Calendar).
     */
    public static Calendar getFirstScope(Calendar date)
    {      
        // Trailing zeros added to pass the getDate filters.
        return getDate(date,fileScope.substring(0,8) + "00");
    }
    
    /** getLastScope: Getter returns the last date part of a scope String.
     * @param date
     * @return last date in 'yyyymmdd' format (Calendar).
     */
    public static Calendar getLastScope(Calendar date)
    {
        // Trailing zeros added to pass the getDate filters.
        return getDate(date, fileScope.substring(9) + "00");
    }
    
    /** scopeDays: Returns the number of days between the begin and end date 
     * of the filesScope.
     * @return the number of days of the scope of this file (int).
     */
    public static int scopeDays()
    {
        int elapsedDays = 0;
        Calendar date = Calendar.getInstance();
        date.set(0000, 00, 00);
        
        Calendar first = getFirstScope(date);
        Calendar last = getLastScope(date);
        
        first.clear(Calendar.MILLISECOND);
        first.clear(Calendar.SECOND);
        first.clear(Calendar.MINUTE);
        first.clear(Calendar.HOUR_OF_DAY);
        
        last.clear(Calendar.MILLISECOND);
        last.clear(Calendar.SECOND);
        last.clear(Calendar.MINUTE);
        last.clear(Calendar.HOUR_OF_DAY);
        
        while ( first.before(last) )
        {
            first.add(Calendar.DATE, 1);
            elapsedDays++;
        }
        return elapsedDays;
    }

}
