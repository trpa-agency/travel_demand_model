package com.pb.tahoe.util.test;


import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetLoader;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.util.DestinationChoiceSummarizer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import java.io.File;
import java.util.ResourceBundle;

/**
 * DestinationChoiceSummarizerTest is a class that tests
 * all methods of DestinationChoiceSummarizer
 *
 * @author Christi Willison
 * @version 1.0,  Aug 3, 2006
 */

public class DestinationChoiceSummarizerTest extends TestCase {

    DestinationChoiceSummarizer dcSummarizer;
    TableDataSet resultsTable;


    public DestinationChoiceSummarizerTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
        dcSummarizer = new DestinationChoiceSummarizer(rb);
        resultsTable = TableDataSetLoader.loadTableDataSet(rb, "mandatory_destination.choice.output.file");

    }

    public void tearDown() throws Exception {
        super.tearDown();
    }


    public static Test suite() {
        return new TestSuite(DestinationChoiceSummarizerTest.class);
    }

    public static void main(String[] args) {
        new TestRunner().doRun(suite());
    }



    public void testInitODMatrix() throws Exception {
        Matrix m = dcSummarizer.initODMatrix();

        assert(m.getValueAt(9,297) == 0.0f);       //the matrix has values in the first row, last column
    }

    //The assertion statement will vary depending on the number of the
    //various tour types
    public void testFillWorkODMatrix(){
        Matrix m = dcSummarizer.initODMatrix();

        dcSummarizer.fillODMatrix(resultsTable, m, 1);

        assert(m.getSum() == 23902);        //total number of work tours are in the matrix
    }


    public void testGenerateDCResults() throws Exception {
        dcSummarizer.generateDCResults("mandatory_destination.choice.output.file", "work_od.matrix.file", 1);
        File f = new File(ResourceUtil.getProperty(ResourceUtil.getResourceBundle("tahoe"),"work_od.matrix.file") + ".csv");

        assertTrue(f.exists());

    }


}