package yimei.jss.helper;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static yimei.jss.FJSSMain.getFileNames;
import static yimei.jss.helper.GridResultCleaner.writeLine;

/**
 * Created by dyska on 11/10/17.
 *
 * Should take the output of a file like: missing-0.85-4.csv
 * and put it into a format that the R script can handle
 * All this requires is a row of headers, and then the best fitness
 * of each generation, along with a column of the best fitness found
 * in that generation.
 */
public class TestResultCleaner {
    private static final char DEFAULT_SEPARATOR = ',';
    private static final String GRID_PATH = "/Users/dyska/Desktop/Uni/COMP489/GPJSS/grid_results/";
    private String dataPath;
    private String outPath;
    private boolean doOutput;

    public TestResultCleaner(String dataPath, String outPath, boolean doOutput) {
        this.dataPath = dataPath;
        this.outPath = outPath;
        this.doOutput = doOutput;
    }

    public static void main(String[] args) {
        //Just for dynamic results
        //String dirName = "simple_modified_terminal_final";
        //String dirName = "coevolution_modified_terminal_final";
        String dirName1 = "coevolution-fixed";
        String dirName2 = "coevolution_modified_terminal_final";

        boolean doOutput = false;

        String dataPath1 = GRID_PATH + "dynamic/test/" + dirName1;
        String outPath1 =  GRID_PATH + "dynamic/cleaned/" + dirName1+"_test";

        String dataPath2 = GRID_PATH + "dynamic/test/" + dirName2;
        String outPath2 =  GRID_PATH + "dynamic/cleaned/" + dirName2+"_test";

        TestResultCleaner t1 = new TestResultCleaner(dataPath1, outPath1, doOutput);
        t1.readInFile();

        TestResultCleaner t2 = new TestResultCleaner(dataPath2, outPath2, doOutput);
        t2.readInFile();
    }

    public void readInFile() {
        System.out.println("Reading from "+dataPath);
        System.out.println();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dataPath))) {
            for (Path path: stream) {
                if (path.toFile().isDirectory()) {
                    List<String> fileNames = getFileNames(new ArrayList(), path, ".csv");
                    for (String fileName: fileNames) {
                        double[][] fitnesses = parseFitnesses(fileName.toString());
                        if (doOutput) {
                            writeToFile(fileName, fitnesses);
                        } else {
                            //print best fitnesses of each generation
                            String folderName =
                                    fileName.substring(dataPath.length()+1).split("/")[0]+".csv";
                            System.out.println("Fitnesses for "+folderName);
                            String outputString = "";
                            for (int i = 0; i < 30; ++i) {
                                outputString += fitnesses[i][51] + ",";
                            }
                            System.out.println(outputString.substring(0,outputString.length()-1));
                            System.out.println();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double[][] parseFitnesses(String fileName) {
        double[][] fitnesses = new double[30][52]; //51 generations plus best
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(fileName));
            String sCurrentLine;
            //may be multiple fitnesses per generation if numpops > 1
            br.readLine(); //skip headers
            int seed = 0;
            double[] testFitnesses = new double[52];
            double bestFitness = Double.MAX_VALUE;
            while ((sCurrentLine = br.readLine()) != null) {
                String[] components = sCurrentLine.split(String.valueOf(DEFAULT_SEPARATOR));

                int run = Integer.parseInt(components[0]);
                int gen = Integer.parseInt(components[1]);
                double testFitness = Double.parseDouble(components[8]);
                if (seed != run) {
                    testFitnesses[51] = bestFitness;
                    fitnesses[seed] = testFitnesses;
                    seed = run;
                    testFitnesses = new double[52];
                    bestFitness = Double.MAX_VALUE;
                }

                testFitnesses[gen] = testFitness;
                if (testFitness < bestFitness) {
                    bestFitness = testFitness;
                }
            }

            //write final seed's values
            testFitnesses[51] = bestFitness;
            fitnesses[seed] = testFitnesses;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fitnesses;
    }

    public void writeToFile(String fileName, double[][] fitnesses) {
        String outputFileName = fileName.substring(dataPath.length()+1).split("/")[0]+".csv";
        String CSVFile = outPath + "/"+ outputFileName;
        System.out.println("Writing output to "+CSVFile);
        System.out.println();

        try (FileWriter writer = new FileWriter(CSVFile)) {
            //add headers first
            List<String> headers = new ArrayList<String>();

            for (int i = 0; i < 51; ++i) {
                headers.add("Gen"+i);
            }
            headers.add("Best");
            writeLine(writer, headers);

            for (int i = 0; i < fitnesses.length; ++i) {
                List<String> fitnessesStrings = new ArrayList<String>();
                String fitnessString = "";
                double[] testFitnesses = fitnesses[i];
                for (double testFitness: testFitnesses) {
                    fitnessString += testFitness + ",";
                }
                fitnessesStrings.add(fitnessString.substring(0, fitnessString.length()-1));
                writeLine(writer, fitnessesStrings);
            }

            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
