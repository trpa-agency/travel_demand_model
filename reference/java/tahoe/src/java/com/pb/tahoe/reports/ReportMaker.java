package com.pb.tahoe.reports;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TextFile;

import java.util.*;

import org.apache.log4j.Logger;

/**
 * The {@code ReportMaker} offers convenient methods of summarizing data held in a {@code TableDataSet}. The basic
 * component of a class is a "filter," which is essentially a classification mapping for a particular data column.
 * As an example, imagine you have the following data set:
 * <p>
 *<table border="1px" CELLPADDING="1" CELLSPACING="0">
 *   <tr ALIGN="center">
 *     <th>Person #</th>
 *     <th>Type</th>
 *     <th>Weight</th>
 *   </tr>
 *   <tr ALIGN="center">
 *     <td>1</td>
 *     <td>Child</td>
 *     <td>2</td>
 *   </tr>
 *   <tr ALIGN="center">
 *     <td>2</td>
 *     <td>Adult</td>
 *     <td>5</td>
 *   </tr>
 *   <tr ALIGN="center">
 *     <td>3</td>
 *     <td>Child</td>
 *     <td>1</td>
 *   </tr>
 *   <tr ALIGN="center">
 *     <td>4</td>
 *     <td>Adult</td>
 *     <td>5</td>
 *   </tr>
 *   <tr ALIGN="center">
 *     <td>5</td>
 *     <td>Adult</td>
 *     <td>3</td>
 *   </tr>
 * </table>
 * <p>
 * A filter based on the "Type" column would map each person to one of two unique types: "Child" or "Adult."  If you
 * did a summary of the "Weight" column, using the "Type" column as a filter, the following result would be produced:
 * <p>
 * <table border="1px" CELLPADDING="1" CELLSPACING="0">
 *   <tr ALIGN="center">
 *     <th>Type</th>
 *     <th>Weight</th>
 *   </tr>
 *   <tr ALIGN="center">
 *     <td>Child</td>
 *     <td>3</td>
 *   </tr>
 *   <tr ALIGN="center">
 *     <td>Adult</td>
 *     <td>13</td>
 *   </tr>
 * <table>
 * <p>
 * Filters may be specified either from the table which will be summarized, or from an external table.  The latter
 * method allows a seperate tables' mapping of the data to be applied to the table of interest (for example, a TAZ
 * to county mapping file can be used to create a county filter, which may then be applied to a data set with TAZ
 * column (but no county column)).
 * <p>
 * This class allows multiple filters to be specified, allowing for multiple dimensions of analysis. Also, two types
 * of summaries may be produced: column sums (where values in a column are summed by filter values), or column
 * summaries (where statistics based on the column values are produced by filter value).
 * <p>
 * <p>
 * User: Frazier
 * Date: Aug 28, 2006
 * Time: 12:51:50 PM
 * Created by IntelliJ IDEA.
 */
public class ReportMaker {
    static Logger logger = Logger.getLogger(ReportMaker.class);

    /**
     * Table from which report will be built.
     */
    TableDataSet baseTable;

    /**
     * Hashmap linking a {@code String} name to a filter.
     */
    HashMap filterMap = new HashMap<String,Object[]>();

    /**
     * String used to split filter. This should not be found in the filter names\values.
     */
    static String splitter = "SPLIT";

    /**
     * Constructor.
     *
     * @param baseTable
     *        The base table from which the report will be built.
     */
    public ReportMaker(TableDataSet baseTable) {
        this.baseTable = baseTable;
    }

    /**
     * Add a column of ones to the {@link #baseTable} if a straight frequency count is desired.
     */
    public void addOnes () {
        float[] ones = new float[baseTable.getRowCount()];
        Arrays.fill(ones,1.0f);
        baseTable.appendColumn(ones,"ones");
    }

    /**
     * Reverse a {@code String} array. This is needed to correctly prioritize filters.
     *
     * @param array
     *        The {@code String} array to reverse.
     *
     * @return the reversed array.
     */
    private String[] flipArray(String[] array) {
        int test = array.length;
        int counter = 0;
        String[] newArray = new String[test];
        while (test > 0) {
            test--;
            newArray[counter] = array[test];
            counter++;
        }
        return newArray;
    }

    /**
     * Add a filter.
     *
     * @param filterName
     *        The name of the filter.
     *
     * @param colName
     *        The column name (from the {@link #baseTable} this filter applies to.
     *
     * @param filterValues
     *        A 2-D array of {@code float}s.  The length of the first dimension is the number of filter classifications;
     *        the length of the second is the number of unique values that can be mapped to the filter classification.
     *        The {@code float} value in the array is the unique value which will be mapped to a filter classification.
     *
     * @param filterNames
     *        The names of each filter classification.  This should be the same length as the first dimension of
     *        {@code filterValues}.
     */
    public void addFilter(String filterName, String colName, float[][] filterValues, String[] filterNames) {
        HashMap filterSubMap = new HashMap<Float,String>();
        for (int i = 0; i < filterValues.length; i++) {
            for (float j : filterValues[i]) {
                filterSubMap.put((Float) j, filterNames[i]);
            }
        }
        Object[] filterMapArray = {colName, filterSubMap, filterNames};
        filterMap.put(filterName,filterMapArray);
    }

    /**
     * Copy a filter to a different column with a different name.
     *
     * @param filterName
     *        The new filter name.
     *
     * @param sourceFilterName
     *        The original filter name.
     *
     * @param colName
     *        The column from {@link #baseTable} this filter will apply to.
     */
    public void copyFilter(String filterName, String sourceFilterName, String colName) {
        Object[] filterMapArray = {colName, ((Object[]) filterMap.get(sourceFilterName))[1], ((Object[]) filterMap.get(sourceFilterName))[2]};
        filterMap.put(filterName,filterMapArray);
    }

    /**
     * Add a filter from an {@code TableDataSet}, using the values from {@code filterIntegerColumn} as the values for the
     * filter, and mapping the values in {@code filterValuesColumn} to them.
     *
     * @param filterName
     *        The new filter name.
     *
     * @param filterTable
     *        The {@code TableDataSet} to base the filter on.
     *
     * @param filterValuesColumn
     *        The name of the column from {@code filterTable} with the values this filter will apply to.
     *
     * @param filterColumn
     *        The name of the column from {@code filterTable} with the values the filter will use as classification
     *        levels.
     *
     * @param filterNameConnect
     *        A {@code LinkedHashMap} linking the unique values from {@code filterIntegerColumn} to their classification
     *        name.
     *
     * @param colName
     *        The column in {@link #baseTable} this filter will apply to.
     */
    public void addFilterFromTable(String filterName, TableDataSet filterTable, String filterValuesColumn,
                                   String filterColumn, LinkedHashMap<Float,String> filterNameConnect,
                                   String colName) {
        //fill up a series of sets with the correspondence values in the filterTable
        HashMap filterVals = new HashMap<String,HashSet<Float>>();
        for (String i : filterNameConnect.values()) {
            filterVals.put(i,new HashSet<Float>());
        }
        for (int i = 0; i < filterTable.getRowCount(); i++) {
            String filt = filterNameConnect.get(filterTable.getValueAt(i+1,filterColumn));
            ((HashSet<Float>) filterVals.get(filt)).add(filterTable.getValueAt(i+1,filterValuesColumn));
        }
        //Create filterValues float[][] array and filterNames String[] array
        String[] filterNames = new String[filterNameConnect.size()];
        float[][] filterValues = new float[filterNameConnect.size()][];
        int counter = 0;
        for (Float i : filterNameConnect.keySet()) {
            filterNames[counter] = filterNameConnect.get(i);
            HashSet<Float> tmp = ((HashSet<Float>) filterVals.get(filterNames[counter]));
            Float[] temp = tmp.toArray(new Float[tmp.size()]);
            float[] temp2 = new float[temp.length];
            for (int j = 0; j < temp.length; j++) temp2[j] = (float) temp[j];
            filterValues[counter] = temp2;
            counter++;
        }
        this.addFilter(filterName, colName, filterValues, filterNames);
    }

    /**
     * Initialize an array of {@code LinkedHashMap}s with a series of filters slices.
     *
     * @param filterGroup
     *        An array of {@code String}s listing the filters, in the order they are to be applied.
     *
     * @param maps
     *        The size of the array to create.
     *
     * @return an initalized array of {@code LinkedHashMap}s.
     */
    private LinkedHashMap[] lhmInitializer(String[] filterGroup,int maps) {
        String[] filters = (String[]) flipArray(filterGroup);
        LinkedHashMap[] lhm = new LinkedHashMap[maps];
        int loopLength = 1;
        int[] counterLoop = new int[filters.length];
        for (int i = 0; i < filters.length; i++) {
            counterLoop[i] = 0;
            loopLength = loopLength*((String[]) ((Object[]) filterMap.get(filters[i]))[2]).length;
        }
        for (int i = 0; i < maps; i++) {
            lhm[i] = new LinkedHashMap<String,Float>();
        }
        int counter = 0;
        while (counter < loopLength) {
            String filterSlice = "";
            boolean increment = true;
            for (int i = 0; i < filters.length; i++) {
                filterSlice = ((String[]) ((Object[]) filterMap.get(filters[i]))[2])[counterLoop[i]] + splitter + filterSlice;
                if (increment) {
                    if (counterLoop[i] == ((String[]) ((Object[]) filterMap.get(filters[i]))[2]).length -1) {
                        counterLoop[i] = 0;
                    } else {
                        counterLoop[i]++;
                        increment = false;
                    }
                }
            }
            filterSlice = filterSlice.substring(0,filterSlice.length() - splitter.length());
            for (LinkedHashMap<String,Float> i : lhm) {
                i.put(filterSlice,0.0f);
            }
            counter++;
        }
        return lhm;
    }

    /**
     * Create a {@code String} which is the unique representation of the set of filter values for a particular row
     * in the {@link #baseTable}.
     *
     * @param filters
     *        An array of {@code String}s listing the filters, in the order they are to be applied.
     *
     * @param row
     *        The row in the {@code baseTable} to apply this method to.
     *
     * @return a {@code String} representation of the values of this row corresponding to the {@code filters}.
     */
    private String getFilterString(String[] filters, int row) {
        String filterSlice = "";
        for (int j = 0; j < filters.length; j++) {
            String col = (String) ((Object[]) filterMap.get(filters[j]))[0];
            HashMap<Float,String> filterSubMap = (HashMap<Float,String>) ((Object[]) filterMap.get(filters[j]))[1];
            filterSlice = filterSubMap.get(baseTable.getValueAt(row,col)) + splitter + filterSlice;
        }
        filterSlice = filterSlice.substring(0,filterSlice.length() - splitter.length());
        return filterSlice;
    }

    /**
     * Sum over a set of filters.
     *
     * @param column
     *        The {@link #baseTable} column containing the values to sum.
     *
     * @param filterGroup
     *        An array of {@code String}s listing the filters, in the order they are to be applied.
     *
     * @return a {@code LinkedHashMap} linking the unique filter values to their sum.
     */
    private LinkedHashMap<String,Float> filteredSum(String column, String[] filterGroup) {
        String[] filters = (String[]) flipArray(filterGroup);

        LinkedHashMap[] lhm = lhmInitializer(filterGroup,5);
        LinkedHashMap<String,Float> sum = lhm[0];


        //sum over column by filter slices
        for (int i=0; i < baseTable.getRowCount(); i++) {
        	if(i==72871) {
        		logger.info(i);
        	}        	
            String filterSlice = getFilterString(filters,i+1);
            sum.put(filterSlice,(Float) sum.get(filterSlice) + baseTable.getValueAt(i+1,column));
        }
        return sum;
    }

    /**
     * Create average, standard deviation, minimum, maximum, and frequency counts (in that order) for a
     * {@link #baseTable} column split by filters.
     *
     * @param column
     *        The {@link #baseTable} column containing the values to summarize.
     *
     * @param filterGroup
     *        An array of {@code String}s listing the filters, in the order they are to be applied.
     *
     * @return an array of {@code LinkedHashMap}s linking unique filter values to their summary level.
     */
    private LinkedHashMap[] filteredSummary(String column, String[] filterGroup) {
        String[] filters = (String[]) flipArray(filterGroup);

        LinkedHashMap[] lhm = lhmInitializer(filterGroup,7);
        LinkedHashMap<String,Float> sum = lhm[0];
        LinkedHashMap<String,Float> squaredSum = lhm[1];
        LinkedHashMap<String,Float> count = lhm[2];
        LinkedHashMap<String,Float> average = lhm[3];
        LinkedHashMap<String,Float> sd = lhm[4];
        LinkedHashMap<String,Float> max = lhm[5];
        LinkedHashMap<String,Float> min = lhm[6];

        //sum column by filter slices
        Set filterSlices = new HashSet<String>();
        for (int i=0; i < baseTable.getRowCount(); i++) {
            float entry = baseTable.getValueAt(i+1,column);
            String filterSlice = getFilterString(filters,i+1);
            sum.put(filterSlice,sum.get(filterSlice) + entry);
            squaredSum.put(filterSlice,squaredSum.get(filterSlice) + (float) Math.pow(entry,2.0f));
            count.put(filterSlice,count.get(filterSlice) + 1.0f);
            if (!filterSlices.contains(filterSlice)) {
                min.put(filterSlice, entry);
                max.put(filterSlice, entry);
                filterSlices.add(filterSlice);
            }
            if (max.get(filterSlice) < entry) max.put(filterSlice, entry);
            if (min.get(filterSlice) > entry) min.put(filterSlice, entry);

        }

        //calculate sd and average
        for (String filterSlice : average.keySet()) {
            average.put(filterSlice,(sum.get(filterSlice))/(count.get(filterSlice)));
            sd.put(filterSlice,((Double) Math.sqrt(((squaredSum.get(filterSlice)) - (Math.pow(sum.get(filterSlice),2.0f)/(count.get(filterSlice))))/((count.get(filterSlice)) - 1.0f))).floatValue());
        }
        LinkedHashMap[] sda = new LinkedHashMap[5];
        sda[0] = average;
        sda[1] = sd;
        sda[2] = min;
        sda[3] = max;
        sda[4] = count;
        return sda;
    }

    /**
     * Create a "nice" {@code String} representaion of the summary calculated using {@link #filteredSummary}.
     *
     * @param sda
     *        The {@code LinkedHashMap} array returned from {@code filteredSummary}.
     *
     * @param filters
     *        An array of {@code String}s listing the filters, in the order they are to be applied.
     *
     * @param column
     *        The {@link #baseTable} column name containing the values to summarize.
     *
     * @return a string table representation of the summary.
     */
    private String printSummary (LinkedHashMap[] sda, String[] filters, String column) {
        String outString = "\n\nSummary for " + column + "\n";
        String midString = "";
        LinkedHashMap average = (LinkedHashMap<String,Float>) sda[0];
        LinkedHashMap sd = (LinkedHashMap<String,Float>) sda[1];
        LinkedHashMap min = (LinkedHashMap<String,Float>) sda[2];
        LinkedHashMap max = (LinkedHashMap<String,Float>) sda[3];
        LinkedHashMap count = (LinkedHashMap<String,Float>) sda[4];

        for (int i = 0; i < filters.length; i++) {
            if (i == 0) {
                for (int j=0; j < filters.length; j++) {
                    if (j == 0) {
                        midString += "+";
                    }
                    midString += "-------------------------------+";
                }
                midString += "-----------+-----------+-----------+-----------+-----------+\n";
                outString += midString;
                outString += String.format("| %1$-30s|",filters[i]);
            } else {
                outString += String.format(" %1$-30s|",filters[i]);
            }
        }
        outString += String.format(" %1$-10s| %2$-10s| %3$-10s| %4$-10s| %5$-10s|\n","Average","Std. Dev.","Minimum","Maximum","Count");
        outString += midString;
        for (String filterString : ((LinkedHashMap<String,Float>) average).keySet()) {
            String[] filter = filterString.split(splitter);
            for (int i = 0; i < filter.length; i++) {
              if (i == 0) {
                  outString += "|";
              }
              outString += String.format(" %1$-30s|",filter[i]);
            }
            outString += String.format(" %1$10.3f| %2$10.3f| %3$10.3f| %4$10.3f| %5$10.3f|\n",
                    average.get(filterString),sd.get(filterString),min.get(filterString),max.get(filterString),count.get(filterString));
        }
        outString += midString;
        return outString;
    }

    /**
     * Write a filtered sum to a csv format.  The last filter's unique values become columns in the resulting data
     * set; the other filters each have their own column with their unique values as row entries.
     *
     * @param sum
     *        The {@code LinkedHashMap<String,Float>} returned by {@link #filteredSum}.
     *
     * @param filters
     *        An array of {@code String}s listing the filters, in the order they are to be applied.
     *
     * @param outFile
     *        The path and filename of the file to save the results to.
     */
    private void writeSum2CSV (LinkedHashMap<String,Float> sum, String[] filters, String outFile) {
        String outString = "";
        for (int i = 0; i < filters.length - 1; i++) {
            if (i == 0) {
                outString += filters[i] ;
            } else {
                outString += "," + filters[i];
            }
        }
        String[] lastFilter = (String[]) ((Object[]) filterMap.get(filters[filters.length - 1]))[2];
        for (String i : lastFilter) {
            outString += "," + i;
        }
        outString += "\n";
        int counter = 1;
        for (String is : sum.keySet()) {
            String[] i = is.split(splitter);
            if(counter == 1) {
                for (int j = 0; j < i.length - 1; j++) {
                    if (j==0) {
                        outString += i[j];
                    } else {
                        outString += "," + i[j];
                    }
                }
            }
            outString += "," + sum.get(is);
            if (counter % lastFilter.length == 0) {
                counter = 0;
                outString += "\n";
            }
            counter++;
        }
        TextFile.writeTo(outFile,outString);
    }

    /**
     * Create a filtered sum and write it out to a file.
     *
     * @param column
     *        The {@link #baseTable} column name containing the values to summarize.
     *
     * @param filterGroup
     *        An array of {@code String}s listing the filters, in the order they are to be applied.
     *
     * @param outFile
     *        The path and filename of the file to save the results to.
     */
    public void filter2File(String column, String[] filterGroup, String outFile) {
        writeSum2CSV(filteredSum(column,filterGroup),filterGroup,outFile);
    }

    /**
     * Summarize a column (average, standard deviation, minimum, maximum, count) by filters and send the results to
     * the logger/console.
     *
     * @param column
     *        The {@link #baseTable} column name containing the values to summarize.
     *
     * @param filterGroup
     *        An array of {@code String}s listing the filters, in the order they are to be applied.
     */
    public void filterSummary(String column, String[] filterGroup) {
        logger.info(printSummary(filteredSummary(column,filterGroup),filterGroup,column));
    }

    /**
     * Summarize a column (average, standard deviation, minimum, maximum, count) by filters and send the results to
     * the a file as well as the logger/console.
     *
     * @param column
     *        The {@link #baseTable} column name containing the values to summarize.
     *
     * @param filterGroup
     *        An array of {@code String}s listing the filters, in the order they are to be applied.
     *
     * @param outFile
     *        The file to save the results in.
     */
    public void filterSummary(String column, String[] filterGroup, String outFile) {
        String summary = printSummary(filteredSummary(column,filterGroup),filterGroup,column);
        logger.info(summary);
        TextFile.writeTo(outFile,summary);
    }

    /**
     * Summarize a column (average, standard deviation, minimum, maximum, count) by filters and send the results to
     * the a file (possibly in append mode) as well as the logger/console.
     *
     * @param column
     *        The {@link #baseTable} column name containing the values to summarize.
     *
     * @param filterGroup
     *        An array of {@code String}s listing the filters, in the order they are to be applied.
     *
     * @param outFile
     *        The file to save the results in.
     *
     * @param append
     *        Indicates whether to append to the file or not.
     */
    public void filterSummary(String column, String[] filterGroup, String outFile, boolean append) {
        String summary = printSummary(filteredSummary(column,filterGroup),filterGroup,column);
        logger.info(summary);
        TextFile tf = new TextFile();
        tf.addLine(summary);
        tf.writeTo(outFile,append);
    }





    public static void main(String args[]) {
        
    }

}
