package com.pb.tahoe.util.test;


import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetLoader;
import com.pb.common.util.ResourceUtil;
import com.pb.tahoe.util.UrbanTypeCalculator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ResourceBundle;

/**
 * UrbanTypeCalculatorTest is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Aug 8, 2006
 */

public class UrbanTypeCalculatorTest extends TestCase {
    Logger logger = Logger.getLogger(UrbanTypeCalculatorTest.class);

    ResourceBundle rb;

    public UrbanTypeCalculatorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        this.rb = ResourceUtil.getResourceBundle("tahoe");
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }


    public static Test suite() {
        return new TestSuite(UrbanTypeCalculatorTest.class);
    }

    public static void main(String[] args) {
        new TestRunner().doRun(suite());
    }

    public void testCreateUrbTypeTable() throws Exception {

        File urbFile = new File(rb.getString("urban.type.file"));
        if(urbFile.exists()) urbFile.delete();

        UrbanTypeCalculator.createUrbTypeTable();

        TableDataSet urbTable = TableDataSetLoader.loadTableDataSet(rb, "urban.type.file");
        urbTable.buildIndex(urbTable.getColumnPosition("taz"));
        boolean popDensity = String.valueOf(urbTable.getIndexedValueAt(9,urbTable.getColumnPosition("population_density"))).equals("1633.33");
        logger.info("popDensity test: " + popDensity);
        boolean empDensity = String.valueOf(urbTable.getIndexedValueAt(9,urbTable.getColumnPosition("employment_density"))).equals("3725.0");
        logger.info("empDensity test: " + empDensity + ", " + String.valueOf(urbTable.getIndexedValueAt(9,urbTable.getColumnPosition("employment_density"))));
        boolean empPlusPop = String.valueOf(urbTable.getIndexedValueAt(9,urbTable.getColumnPosition("emp_plus_pop"))).equals("5358.33");
        logger.info("empPlusPop test: " + empPlusPop);
        boolean urbType = String.valueOf(urbTable.getIndexedValueAt(9,urbTable.getColumnPosition("urbtype"))).equals("2.0");
        logger.info("urbType test: " + urbType + ", " + String.valueOf(urbTable.getIndexedValueAt(9,urbTable.getColumnPosition("urbtype"))));
        boolean check = popDensity && empDensity && empPlusPop && urbType;

        assertTrue(check);


    }
}