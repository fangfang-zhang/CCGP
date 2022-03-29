package yimei.jss.helper;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static yimei.jss.FJSSMain.getFileNames;

/**
 * Created by dyska on 5/10/17.
 *
 * This class should read in a job.x.out.stat file, discard the 51-99th generations
 * and update the best rules.
 */
public class GridResultFixer {

    public static void main(String[] args) {
        //Will need to run this on:
        // dynamic/raw/coevolution-fixed
        // dynamic/raw/coevolution_feature_selection?
        // static/raw/coevolve

        String directoryPath = (new File("")).getAbsolutePath() + "/grid_results/";
        directoryPath += "dynamic/raw/coevolution-fixed";
        Path path = Paths.get(directoryPath);
        updateResults(path);
    }

    public static void updateResults(Path path) {
        List<String> jobFileNames = getFileNames(new ArrayList(), path, ".out.stat");

        for (String jobFileName: jobFileNames) {
            File jobFile = new File(jobFileName);
            List<String> lines = readInLines(jobFile);
            if (lines != null) {
                writeToFile(jobFile, lines);
                System.out.println("Updated "+jobFileName);
            }
            //will be null if file already updated
        }
    }

    public static List<String> readInLines(File file) {
        BufferedReader br = null;
        List<String> lines = new ArrayList<>();
        double bestFitnessSubpop1 = 100;
        double bestFitnessSubpop2 = 100;
        int bestSubPop1Gen = -1;
        int bestSubPop2Gen = -1;
        int currentGeneration = -1;

        try {
            br = new BufferedReader(new FileReader(file));
            String sCurrentLine;
            int numFound = 0;
            while ((sCurrentLine = br.readLine()) != null) {
                if (sCurrentLine.startsWith("Generation:")) {
                    String gen = sCurrentLine.split(" ")[1];
                    currentGeneration = Integer.parseInt(gen);
                }
                if (sCurrentLine.startsWith("Fitness")) {
                    //line should be in format "Fitness: [0.8386540120793787]"
                    String fitnessString = sCurrentLine.substring(sCurrentLine.indexOf("[")+1, sCurrentLine.length()-1);
                    double fitness = Double.parseDouble(fitnessString);
                    if (numFound == 0) {
                        if (fitness <= bestFitnessSubpop1) {
                            bestFitnessSubpop1 = fitness;
                            bestSubPop1Gen = currentGeneration;
                        }
                    } else {
                        if (fitness <= bestFitnessSubpop2) {
                            bestFitnessSubpop2 = fitness;
                            bestSubPop2Gen = currentGeneration;
                        }
                    }
                    numFound++;
                }
                if (numFound == 2) {
                    numFound = 0;
                }
                if (sCurrentLine.equals("Generation: 51")) {
                    //add best, then we're done
                    //let's sweep through all the lines so far and track down the best fitnesses
                    List<String> bestIndividualLines = new ArrayList<>();
                    bestIndividualLines.add("Best Individual of Run:");
                    for (int i = 0; i < lines.size(); ++i) {
                        String line = lines.get(i);
                        if (line.startsWith("Generation: "+bestSubPop1Gen)) {
                            //get subpop1 best
                            i += 2; //skip 2 lines
                            for (int j = 0; j < 7; ++j) {
                                String sline = lines.get(i+j);
                                bestIndividualLines.add(sline);
                            }
                            break;
                        }
                    }

                    for (int i = 0; i < lines.size(); ++i) {
                        String line = lines.get(i);
                        if (line.startsWith("Generation: "+bestSubPop2Gen)) {
                            //get subpop2 best
                            i+=9; //skip lines
                            for (int j = 0; j < 7; ++j) {
                                String sline = lines.get(i+j);
                                bestIndividualLines.add(sline);
                            }
                            break;
                        }
                    }

                    lines.addAll(bestIndividualLines);
                    return lines;
                }
                lines.add(sCurrentLine);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static void writeToFile(File file, List<String> lines) {
        try {
            Files.write(file.toPath(), lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method should read in every line from a file, then close it.
     * These lines should be analysed and edited. Then the file should be
     * written to with the new lines.
     */
//    public Double[] GetFitnesses(String fileName) {
//        BufferedReader br = null;
//        List<Double> bestFitnesses = new ArrayList<Double>();
//
//        try {
//            br = new BufferedReader(new FileReader(fileName));
//            String sCurrentLine;
//            //may be multiple fitnesses per generation if numpops > 1
//            Double[] fitnesses = new Double[numPops]; //should be reset every generation
//            int numFound = 0;
//            while ((sCurrentLine = br.readLine()) != null) {
//                if (sCurrentLine.startsWith("Fitness")) {
//                    //line should be in format "Fitness: [0.8386540120793787]"
//                    sCurrentLine = sCurrentLine.substring(sCurrentLine.indexOf("[")+1, sCurrentLine.length()-1);
//                    fitnesses[numFound] = Double.parseDouble(sCurrentLine);
//                    numFound++;
//                }
//                if (numFound == numPops) {
//                    //quickly sort the fitnesses - only want lower one (best)
//                    Double best = fitnesses[0];
//                    if (fitnesses.length == 2) {
//                        if (fitnesses[1] < best) {
//                            best = fitnesses[1];
//                        }
//                    }
//                    bestFitnesses.add(best);
//                    //reset
//                    fitnesses = new Double[numPops];
//                    numFound = 0;
//                }
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return bestFitnesses.toArray(new Double[0]);
//    }
}
