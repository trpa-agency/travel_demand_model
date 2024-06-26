package com.pb.tahoe.util;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class DCAlternativeListCreator {

    private static TableDataSet walkSegment;

     public static void createDCAlternativeSet(ResourceBundle rb) {
    	
		//Read walk segment file into table data set
		CSVFileReader fileReader = new CSVFileReader();
         TableDataSet zoneMapTable;
         String walkPercentsFile = rb.getString("zonal.walk.percents.file");
        try {
//			walkSegment = fileReader.readFile(new File(rb.getString("zonal.walk.percents.file")));
            walkSegment = fileReader.readFile(new File(walkPercentsFile));
            //Build index for access to TAZs in order
            walkSegment.buildIndex(1);

            //get the zone mappings file as it is the only file guaranteed to have a list of all zones in study area.
            zoneMapTable = fileReader.readFile(new File(rb.getString("taz.correspondence.file")));
        } catch (IOException e){
        	//throw new RuntimeException("There was a problem reading " + rb.getString("zonal.walk.percents.file") , e);
            throw new RuntimeException("There was a problem reading " + walkPercentsFile , e);
        }
        
	    //Determine which segments are available to DC
        ArrayList<Integer> a = new ArrayList<Integer>();
        ArrayList<Integer> dtaz = new ArrayList<Integer>();
        ArrayList<Integer> shortWalk = new ArrayList<Integer>();

         int altNumber = 1;
        for (int taz : zoneMapTable.getColumnAsInt(1)){
        	ArrayList<Integer> walkSegments = getWalkSegments(taz);
        	for (int wlk : walkSegments) {
                a.add(altNumber);
        		dtaz.add(taz);
        		shortWalk.add(wlk);
        		altNumber++;
        	}
        }
        
        //Create TableDataSet from arrays, and export to file
        TableDataSet dcAlternativeSet = new TableDataSet();
        //Need to cast from object ArrayList to primitive array - how annoying!
        int[] ai = new int[a.size()];
        int[] dtazi = new int[dtaz.size()];
        int[] swi = new int[shortWalk.size()];
        for (int i = 0; i < a.size(); i++){
            ai[i] = a.get(i);
            dtazi[i] = dtaz.get(i);
            swi[i] = shortWalk.get(i);
        }

        dcAlternativeSet.appendColumn(ai,"a");
        dcAlternativeSet.appendColumn(dtazi,"dtaz");
        dcAlternativeSet.appendColumn(swi,"shortWalk");
        
		CSVFileWriter fileWriter = new CSVFileWriter();
		try {
			fileWriter.writeFile(dcAlternativeSet,new File(rb.getString("dc.alternative.set.file")));
		} catch (IOException e) {
            throw new RuntimeException("Error writing file " +  rb.getString("dc.alternative.set.file"), e);
        }
        
	}
	
	//This method returns an ArrayList of all of the walk segments available to a particular TAZ
	static ArrayList<Integer> getWalkSegments(int taz) {
		double eps = .0000001;
		ArrayList<Integer> walkSegments = new ArrayList<Integer>();
        try {
            double shortWalk = walkSegment.getValueAt(walkSegment.getIndexedRowNumber(taz),"ShortWalkPercent");
            double longWalk = walkSegment.getValueAt(walkSegment.getIndexedRowNumber(taz),"LongWalkPercent");
            if (Math.abs(1 - (shortWalk + longWalk)) > eps) {
                walkSegments.add(0);
            }
            if (shortWalk > 0) {
                walkSegments.add(1);
            }
            if (longWalk > 0) {
                walkSegments.add(2);
            }
        } catch (Exception e) {
            //must be an external zone and therefore not in the the zonal walk percent file
            //in this case we are making the walk percent 0.
            walkSegments.add(0);
        }
        return walkSegments;
	}
	
	public static void main(String[] args) {
		ResourceBundle rb = ResourceUtil.getResourceBundle("tahoe");
		createDCAlternativeSet(rb);

	}

}
