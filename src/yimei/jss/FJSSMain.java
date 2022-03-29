package yimei.jss;

import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import yimei.jss.gp.terminal.AttributeGPNode;
import yimei.jss.gp.terminal.JobShopAttribute;
import yimei.jss.jobshop.*;
import yimei.jss.rule.AbstractRule;
import yimei.jss.rule.RuleType;
import yimei.jss.rule.operation.basic.*;
import yimei.jss.rule.operation.composite.*;
import yimei.jss.rule.operation.evolved.GPRule;
import yimei.jss.rule.operation.weighted.*;
import yimei.jss.rule.workcenter.basic.*;
import yimei.jss.simulation.DynamicSimulation;
import yimei.jss.simulation.Simulation;
import yimei.jss.simulation.StaticSimulation;
import yimei.jss.simulation.event.AbstractEvent;
import yimei.jss.simulation.state.SystemState;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static yimei.jss.helper.GenerateTerminalSet.getDirectoryNames;
import static yimei.jss.helper.GridResultCleaner.roundMakespan;
import static yimei.jss.jobshop.Objective.*;
import static yimei.jss.ruleevaluation.RuleComparison.EvaluateOutput;

/**
 * The main program of job shop scheduling, for basic testing.
 *
 * Created by YiMei on 27/09/16.
 */
public class FJSSMain {

    private static int calculateFitness(String fileName,
                                        Objective objective,
                                        AbstractRule sequencingRule,
                                        AbstractRule routingRule) {
//        if (sequencingRules.isEmpty() ||
//                routingRules.isEmpty() ||
//                objectives.isEmpty()) {
//            System.out.println("Please check parameters," +
//                    " at least one of the objectives, sequencing rules or routing rules was empty.");
//            return;
//        }
        if (sequencingRule.getType() != RuleType.SEQUENCING) {
            System.out.println("Invalid sequencing rule");
            return -1;
        }
        if (routingRule.getType() != RuleType.ROUTING) {
            System.out.println("Invalid routing rule");
            return -1;
        }
//        BufferedWriter writer = null;
//
//        if (doStore) {
//            writer = createFileWriter(fileName);
//        }

        MultiObjectiveFitness fitness = new MultiObjectiveFitness();
        fitness.objectives = new double[1];
        fitness.maxObjective = new double[1];
        fitness.maxObjective[0] = 1.0;
        fitness.minObjective = new double[1];
        fitness.maximize = new boolean[1];

        FlexibleStaticInstance instance = FlexibleStaticInstance.readFromAbsPath(fileName);
//            System.out.println("Num jobs "+instance.getNumJobs()+" num work centers: "+instance.getNumWorkCenters());
//            double numOperations = 0;
//            double numOptions = 0;
//            for (int i = 0; i < instance.getNumJobs(); ++i) {
//                FlexibleStaticInstance.JobInformation job = instance.getJobInformations().get(i);
//                numOperations += job.getNumOps();
//                for (int j = 0; j < job.getOperations().size(); ++j) {
//                    numOptions += job.getOperations().get(j).getOperationOptions().size();
//                }
//            }
//            System.out.println("Num operations per job "+numOperations/instance.getNumJobs());
//            System.out.println("Num options per operation "+numOptions/numOperations);

        List<Integer> replications = new ArrayList<>();
        replications.add(1);
        List<Objective> objectives = new ArrayList<>();
        objectives.add(objective);

        List<Simulation> simulations = new ArrayList<>();
        simulations.add(new StaticSimulation(sequencingRule, routingRule, instance));
        SchedulingSet set = new SchedulingSet(simulations, replications, objectives);
        int objectiveValue = calcObjective(sequencingRule, routingRule, fitness, set, objectives);
        return objectiveValue;
//                String fitnessResult = calcFitness(doStore, sequencingRule, routingRule, fitness, set, objectives);
//
//                //store fitness result with sequencing rule and routing rule
//                if (doStore) {
//                    try {
//                        writer.write(String.format("RR:%s SR:%s - %s", getRuleName(routingRule),
//                                getRuleName(sequencingRule), fitnessResult));
//                        writer.newLine();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//        if (doStore) {
//            try {
//                writer.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private static BufferedWriter createFileWriter(String filePath) {
        //replace /data/ with /out/test/
        String[] pathComponents = filePath.split("/data/");
        //to avoid heavy nesting in output, replace nested directory with filenames
       
        String output = pathComponents[0] + "/out/rule_comparisons/" + pathComponents[1].replace("/","-");

        File file = new File(output);
        try {
            //create file in this location if one does not exist
            if (!file.exists()) {
                file.createNewFile();
            }
            return new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getRuleName(AbstractRule rule) {
        if (rule instanceof GPRule) {
            //should return full rule, not just "GPRule"
            return ((GPRule) rule).getLispString();
        }
        return rule.getName();
    }

    private static String calcFitness(boolean doStore, AbstractRule sequencingRule,
                                      AbstractRule routingRule,
                                      MultiObjectiveFitness fitness,
                                      SchedulingSet set,
                                      List<Objective> objectives) {
        String output = "";

        sequencingRule.calcFitness(fitness, null, set, routingRule, objectives);

        double benchmarkMakeSpan = set.getObjectiveLowerBound(0,0);
        output += "Benchmark makespan: "+benchmarkMakeSpan+"\n";
        double ruleMakespan = fitness.fitness()*benchmarkMakeSpan;
        output += "Rule makespan: "+ruleMakespan+"\n";

        output += "Fitness = " + fitness.fitnessToStringForHumans();
        System.out.println(output);
        return output;
    }

    private static int calcObjective(AbstractRule sequencingRule,
                                      AbstractRule routingRule,
                                      MultiObjectiveFitness fitness,
                                      SchedulingSet set,
                                      List<Objective> objectives) {
        sequencingRule.calcFitness(fitness, null, set, routingRule, objectives);

        double benchmarkObjectiveValue = set.getObjectiveLowerBound(0,0);
        int ruleObjectiveValue = roundMakespan(fitness.fitness()*benchmarkObjectiveValue);
        return ruleObjectiveValue;
    }

    public static List<String> getFileNames(List<String> fileNames, Path dir, String ext) {
        if (dir.toAbsolutePath().toString().endsWith(ext)) {
            //have been passed a file
            fileNames.add(dir.toString());
        } else {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path path : stream) {
                    if (path.toFile().isDirectory()) {
                        getFileNames(fileNames, path, ext);
                    } else {
                        if (path.toString().endsWith(ext)) {
                            fileNames.add(path.toString());
                        }
                    }
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
        return fileNames;
    }

    public static List<Path> getDirectoryNames(List directoryNames, Path dir, String ext) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (path.toFile().isDirectory()) {
                    if (containsFiles(path, ext)) {
                        directoryNames.add(path);
                    } else {
                        getDirectoryNames(directoryNames, path, ext);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return directoryNames;
    }

    public static boolean containsFiles(Path dir, String ext) {
        ///want to find out whether this directory contains files of a given extensions
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (!path.toFile().isDirectory()) {
                    if (path.toString().endsWith(ext)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String formatFileName(String fileName) {
        String path = (new File("")).getAbsolutePath() + "/data/FJSS/";
        String outputName = fileName.substring(path.length());
        outputName = outputName.substring(0,outputName.length()-4); //remove ".fjs"
        return outputName.replace('/','-');
    }

    public static HashMap<String, Integer> readInFJSSBounds() {
        File csvFile = new File((new File("")).getAbsolutePath()
                +"/fjss_bounds/fjss_bounds.csv");
        HashMap<String, Integer> bounds = new HashMap<>();

        BufferedReader br = null;
        String line = "";

        try {
            br = new BufferedReader(new FileReader(csvFile));
            line = br.readLine(); //skip header
            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",");
                String fileName = vals[0];
                Integer lowerBound = Integer.valueOf(vals[1]);
                bounds.put(fileName,lowerBound);
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
        return bounds;
    }

    public static void main(String[] args) {
        //first arg should be static or dynamic (as a string)
        //second arg should be filepath if static
        if (args.length == 0) {
            System.out.println("Must include at least one argument");
            System.out.println("Args[0] = static or dynamic");
            return;
        }
        String simulationType = args[0].toLowerCase();
        if (simulationType.equals("static")) {
            String path = "";
            if (args.length > 1) {
                //allow more specific folder or file paths to be used
                path = args[1];
            }
            path = (new File("")).getAbsolutePath() + "/data/FJSS/" + path;
            runStaticSimulation(path);
        } else if (simulationType.equals("dynamic")) {
            AbstractRule sequencingRule = new WSPT(RuleType.SEQUENCING);
            AbstractRule routingRule = new WIQ(RuleType.ROUTING);
            Objective[] objectives = new Objective[]{MEAN_FLOWTIME, MAX_FLOWTIME, MEAN_WEIGHTED_FLOWTIME};
            Double[] utilLevels = new Double[]{0.85, 0.95};

            for (int i = 0; i < 2; ++i) {
                double utilLevel = utilLevels[i];
                for (int j = 0; j < 3; ++j) {
                    Objective o = objectives[j];
                    System.out.println("Objective: "+o.getName()+", Utilisation level: "+utilLevel);
                    System.out.println("Sequencing rule: "+sequencingRule.getName());
                    //System.out.println("Routing rule: "+routingRule.getName());
                    runDynamicSimulation(o, utilLevel, sequencingRule, routingRule);
                }
            }
        } else {
            System.out.println("Invalid argument of "+simulationType+" specified");
        }
    }

    public static void runStaticSimulation(String path) {
        //path may be a directory path or a file path
        //example file path: Brandimarte_Data/Text/Mk08.fjs

        //boolean doStore = false;
        //List<Objective> objectives = new ArrayList<>();
        HashMap<String, Integer> lowerBounds = readInFJSSBounds();

        Objective objective = MAKESPAN;
//        List<AbstractRule> sequencingRules = new ArrayList();
//        List<AbstractRule> routingRules = new ArrayList();

        //objectives.add(Objective.MAKESPAN);
        //AbstractRule sequencingRule = new FCFS(RuleType.SEQUENCING);
        AbstractRule sequencingRule = new FCFS(RuleType.SEQUENCING);
        AbstractRule routingRule = new WIQ(RuleType.ROUTING);
        System.out.println("Running static simulation for objective: "+objective.getName());
        System.out.println("Sequencing rule: "+sequencingRule.getName());
        System.out.println("Routing rule: "+routingRule.getName());

        //routingRules.add(GPRule.readFromLispExpression(RuleType.ROUTING," (+ (max WIQ DD) (- (/ AT PT) (min SL NOR)))"));

        //routingRules.add(new SBT(RuleType.ROUTING));
        //sequencingRules.add(GPRule.readFromLispExpression(RuleType.SEQUENCING," (+ (min (min (max (min WIQ t) (max NOR SL)) MRT) (max DD WKR)) (* (+ SL SL) (- WIQ PT)))"));
        //sequencingRules.add(GPRule.readFromLispExpression(RuleType.SEQUENCING," (/ WKR rDD)"));
//        routingRules.add(GPRule.readFromLispExpression(RuleType.ROUTING," (max (max NINQ PT) (max (- (/ (min t NINQ)" +
//                " (max AT W)) (min (max NOR FDD) (* MRT (- SL W)))) AT))"));

//        sequencingRules.add(new AVPRO(RuleType.SEQUENCING));
//        sequencingRules.add(new CR(RuleType.SEQUENCING));
//        sequencingRules.add(new EDD(RuleType.SEQUENCING));
//        sequencingRules.add(new FCFS(RuleType.SEQUENCING));
//        sequencingRules.add(new FDD(RuleType.SEQUENCING));
//        sequencingRules.add(new LCFS(RuleType.SEQUENCING));
//        sequencingRules.add(new LPT(RuleType.SEQUENCING));
//        sequencingRules.add(new LWKR(RuleType.SEQUENCING));
//        sequencingRules.add(new MOPNR(RuleType.SEQUENCING));
//        sequencingRules.add(new MWKR(RuleType.SEQUENCING));
//        sequencingRules.add(new NPT(RuleType.SEQUENCING));
//        sequencingRules.add(new PW(RuleType.SEQUENCING));
//        sequencingRules.add(new SL(RuleType.SEQUENCING));
//        sequencingRules.add(new Slack(RuleType.SEQUENCING));
//        sequencingRules.add(new SPT(RuleType.SEQUENCING));
//
//        sequencingRules.add(new ATC(RuleType.SEQUENCING));
//        sequencingRules.add(new COVERT(RuleType.SEQUENCING));
//        sequencingRules.add(new CRplusPT(RuleType.SEQUENCING));
//        sequencingRules.add(new LWKRplusPT(RuleType.SEQUENCING));
//        sequencingRules.add(new OPFSLKperPT(RuleType.SEQUENCING));
//        sequencingRules.add(new PTplusPW(RuleType.SEQUENCING));
//        sequencingRules.add(new PTplusPWplusFDD(RuleType.SEQUENCING));
//        sequencingRules.add(new SlackperOPN(RuleType.SEQUENCING));
//        sequencingRules.add(new SlackperRPTplusPT(RuleType.SEQUENCING));
//
//        sequencingRules.add(new WATC(RuleType.SEQUENCING));
//        sequencingRules.add(new WCOVERT(RuleType.SEQUENCING));
//        sequencingRules.add(new WSPT(RuleType.SEQUENCING));
//
//        //add work center specific rules, as other rules will always give the same values
//        routingRules.add(new LBT(RuleType.ROUTING));
//        routingRules.add(new LRT(RuleType.ROUTING));
//        routingRules.add(new NIQ(RuleType.ROUTING));
//        routingRules.add(new SBT(RuleType.ROUTING));
//        routingRules.add(new SRT(RuleType.ROUTING));
//        routingRules.add(new WIQ(RuleType.ROUTING));

        //get the Filenames of all static FJSS instances in the relevant directory
        List<Path> directoryNames = getDirectoryNames(new ArrayList(), Paths.get(path),".fjs");

        //List<String> instanceFileNames = getFileNames(new ArrayList(), Paths.get(path), ".fjs");
        for (int i = 0; i < directoryNames.size(); ++i) {
            Path directoryName = directoryNames.get(i);
            List<String> instanceFileNames = getFileNames(new ArrayList(), directoryName, ".fjs");
            int numInstances = instanceFileNames.size();
            System.out.println(numInstances +" FJSS instances in "+directoryName.toString());
            double[] makeSpanRatios = new double[numInstances];
            for (int j = 0; j < numInstances; ++j) {
                String instanceFileName = instanceFileNames.get(j);
                //System.out.println("\nInstance "+(i+1)+" - Path: "+instanceFileName);
                double objectiveVal =
                        calculateFitness(instanceFileName, objective, sequencingRule, routingRule);
                String formattedFileName = formatFileName(instanceFileName);
                double lowerBound = lowerBounds.get(formattedFileName);
                double ratio = objectiveVal/lowerBound;
                makeSpanRatios[j] = ratio;
            }
            double ratioSum = 0.0;
            for (int j = 0; j < numInstances; ++j) {
                ratioSum += makeSpanRatios[j];
            }
            System.out.println("Mean lb/objective value is: "+ratioSum/numInstances);
            System.out.println();
        }

        //want to be able to feed in a sequencing rule and a routing rule
        //and be able to find out the makespan of that pair on a given instance
        //should then store that, as well a ratio of that to the lower bound,
        //in a file, and aggregate by directory

        //EvaluateOutput("/out/rule_comparisons/", "RR");
    }

    public static void runDynamicSimulation(Objective o, double utilLevel,
                                            AbstractRule sequencingRule, AbstractRule routingRule) {
        MultiObjectiveFitness fitness = new MultiObjectiveFitness();
        fitness.objectives = new double[1];
        fitness.maxObjective = new double[1];
        fitness.maxObjective[0] = 1.0;
        fitness.minObjective = new double[1];
        fitness.maximize = new boolean[1];

        List<Objective> objectives = new ArrayList<>();
        objectives.add(o);
        int seed = 0;
        SchedulingSet set = createSchedulingSet(seed,o,utilLevel);

        int numRuns = 30;
        double[] results = new double[numRuns];
        for (int i = 0; i < numRuns; ++i) {
            sequencingRule.calcFitness(fitness, null, set, routingRule, objectives);
            results[i] = fitness.fitness();
            set.rotateSeed(objectives);
        }

        String resultString = "";
        for (int i = 0; i < numRuns; ++i) {
            resultString += results[i] +",";
        }
        System.out.println(resultString.substring(0,resultString.length()-1));

        //System.out.println("Fitness averaged across "+numRuns+ " runs: "+(fitnessSum/numRuns));
        System.out.println();
    }

    public static SchedulingSet createSchedulingSet(int seed, Objective objective, double utilLevel) {
        List<Simulation> trainSimulations = new ArrayList<>();
        List<Integer> replications = new ArrayList<>();
        List<Objective> objectives = new ArrayList<>();
        objectives.add(objective);

        int numMachines = 10;
        // Number of jobs
        int numJobs = 5000;
        // Number of warmup jobs
        int warmupJobs = 1000;
        // Min number of operations
        int minNumOperations = 1;
        // Max number of operations
        int maxNumOperations = numMachines;
        // Due date factor
        double dueDateFactor = 4.0;
        // Number of replications
        int rep = 1;
        //Seed
        Simulation simulation = new DynamicSimulation(seed,
                    null, null, numMachines, numJobs, warmupJobs,
                    minNumOperations, maxNumOperations,
                    utilLevel, dueDateFactor, false);

        trainSimulations.add(simulation);
        replications.add(new Integer(rep));

        return new SchedulingSet(trainSimulations, replications, objectives);
    }

}