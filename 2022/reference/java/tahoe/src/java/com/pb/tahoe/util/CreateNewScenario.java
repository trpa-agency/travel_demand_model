package com.pb.tahoe.util;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.zip.Inflater;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.util.ArrayList;

/**
 * User: Chris
 * Date: Apr 16, 2007 - 8:45:01 AM
 */
public class CreateNewScenario {

    private static Logger logger = Logger.getLogger(CreateNewScenario.class);

    //path should be path + scenario directory (no created yet) - e.g. c:/blah/blah/scenarios/my_new_scenario
    private static boolean createDirectoryStructure(String path) {
        File scenarioLocation = new File(path);
        if (scenarioLocation.exists()) {
            logger.warn("Scenario directory already exists!");
            return false;
        }
        boolean success = scenarioLocation.mkdir();
        if (!success)
            logger.warn("Scneario directory creation failed!");
        return success;
    }

    //doesn't check for overwriting
    private static boolean unzipToDirectory(String path, String zipFile) {
        ZipFile zippedBase;
        Enumeration<? extends ZipEntry> entries;
        try {
            zippedBase = new ZipFile(new File(zipFile));
            entries = zippedBase.entries();
            //do directories
            ArrayList<String> directoryNames = new ArrayList<String>();
            int maxDepth = 1;
            while(entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    directoryNames.add(entry.getName());
                    if(entry.getName().split("/").length > maxDepth)
                        maxDepth = entry.getName().split("/").length;
                }
            }
            for (int i = 1; i <= maxDepth; i++)
                for (String dir : directoryNames)
                    if (dir.split("/").length == i)
                        (new File(path + dir)).mkdir();
            entries = zippedBase.entries();
            //do files
            while(entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    InputStream in = zippedBase.getInputStream(entry);
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(path + entry.getName()));
                    byte[] buffer = new byte[1024];
                    int len;
                    while((len = in.read(buffer)) >= 0)
                        out.write(buffer, 0, len);
                    in.close();
                    out.close();
                }
            }
            //do non directories
            zippedBase.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void createScenario(String scenarioPath, String scenarioName, String zippedBaseScenarioPath) {
        if (createDirectoryStructure(scenarioPath + scenarioName))
            if (unzipToDirectory(scenarioPath + scenarioName + "/",zippedBaseScenarioPath)) {
                logger.info("Scenario created.");
                return;
            }
        logger.warn("Scenario creation failed!");
    }


    public static void main(String[] args) {
        String usage = "java CreateNewScenario scenarioPath scenarioName zippedBaseScenario\n" +
                "\tscenarioPath - The path to the location to place the new scenario directory (ending in slash)\n" +
                "\tscenarioName - The name of the scenario to use for the directory\n" +
                "\tzippedBaseScenario - The path with filename of the zipped base scenario";
        if (args.length != 3) {
            logger.warn("Incorrect number of arguments.\n" + usage);
            return;
        }
        createScenario(args[0].replace('\\','/'),args[1].replace('\\','/'),args[2].replace('\\','/'));
    }
}
