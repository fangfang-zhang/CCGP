package yimei.jss.helper;

import ec.gp.GPNode;
import ec.rule.Rule;
import yimei.jss.gp.terminal.AttributeGPNode;
import yimei.jss.gp.terminal.JobShopAttribute;
import yimei.jss.rule.RuleType;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static yimei.jss.FJSSMain.getFileNames;
import static yimei.jss.gp.terminal.JobShopAttribute.relativeAttributes;
import static yimei.jss.helper.GridResultCleaner.writeLine;

/**
 * Created by dyska on 30/09/17.
 *
 */
public class GenerateTerminalSet {


    public static void main(String args[]) {
        //Should specify a directory path which will contain
        //csv files containing the chosen terminals - one for each job
        //then we should decide which ones to keep, and output these
        //features into a csv file in the /terminal/ directory

        // grid_results/dynamic/raw/simple_feature_selection/
        // dynamic/raw/simple_feature_selection/
        String path = "";
        String outputDirectory = "";
        if (args.length > 0) {
            //allow more specific folder or file paths to be used
            path = args[0];
            outputDirectory = args[1];
            path = (new File("")).getAbsolutePath() + "/grid_results/" + path;
        } else {
            System.out.println("Please specify a directory path.");
            return;
        }

        List<Path> directoryNames = getDirectoryNames(Paths.get(path));
        for (Path d : directoryNames) {
            System.out.println("Terminal counts for "+d.toString());
            List<String> terminalCSVs = getFileNames(new ArrayList<>(), d, ".terminals.csv");
            //if no routing files, will not do anything, so can safely call this also
            chooseTerminals(outputDirectory, d, RuleType.ROUTING, terminalCSVs);
            chooseTerminals(outputDirectory, d, RuleType.SEQUENCING, terminalCSVs);
        }
    }

    public static List<Path> getDirectoryNames(Path dir) {
        List directoryNames = new ArrayList();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (path.toFile().isDirectory()) {
                    directoryNames.add(path);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return directoryNames;
    }

    public static List<String> readFromFile(String fileName) {
        File csvFile = new File(fileName);
        LinkedList<String> terminals = new LinkedList<String>();

        BufferedReader br = null;
        String line = "";

        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                JobShopAttribute a = JobShopAttribute.get(line);
                AttributeGPNode attribute = new AttributeGPNode(a);
                terminals.add(attribute.toString());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return terminals;
    }

    public static void chooseTerminals(String outputDirectory, Path d,
                                       RuleType ruleType, List<String> terminalCSVs) {
        //we essentially want to have a list of all terminals
        //then, for each terminal file, want to record whether that terminal file included it
        JobShopAttribute[] attributes = relativeAttributes();
        int numAttributes = attributes.length;
        int[][] attributeCounts = new int[numAttributes][30];
        boolean isRead = false;

        for (String fileName: terminalCSVs) {
            if (fileName.contains(ruleType.name())) {
                isRead = true;
                List<String> terminals = readFromFile(fileName);
                for (String terminal: terminals) {
                    //what is the index of this terminal?
                    int index = -1;
                    for (int i = 0; i < numAttributes && index == -1; ++i) {
                        if (attributes[i].getName().equals(terminal)) {
                            index = i;
                        }
                    }

                    //what number job was this?
                    String[] fileNameParts = fileName.split("job.");
                    int jobNum = Integer.parseInt(fileNameParts[1].split(" -")[0]);
                    //System.out.println("Terminal "+terminal+" found in job "+jobNum);
                    attributeCounts[index][jobNum] = 1;
                }
            }
        }
        if (isRead) {
            File outputFile = createFileName(outputDirectory, d, ruleType);
            //now output this list of chosen terminals to a file
            outputToFile(outputFile, attributeCounts);
        }
    }

    public static File createFileName(String outputDirectory, Path d, RuleType ruleType) {
        outputDirectory = (new File("")).getAbsolutePath()+"/feature_selection/"+outputDirectory+"/";
        String path = d.toString();
        String fileName = outputDirectory + path.split("/")[path.split("/").length-1];
        fileName += "-"+ruleType.name()+".csv";
        return new File(fileName);
    }

    public static void outputToFile(File csvFile, int[][] attributeCounts) {
        try (FileWriter writer = new FileWriter(csvFile)) {
            List<String> headers = new ArrayList<String>();
            headers.add("Terminal");
            for (int i = 0; i < 30; ++i) {
                headers.add("J"+i);
            }
            writeLine(writer, headers);
            JobShopAttribute[] attributes = relativeAttributes();
            for (int i = 0; i < attributeCounts.length; ++i) {
                List<String> terminalLine = new ArrayList<String>();
                terminalLine.add(attributes[i].getName());
                for (int j = 0; j < 30; ++j) {
                    terminalLine.add(String.valueOf(attributeCounts[i][j]));
                }
                writeLine(writer, terminalLine);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

