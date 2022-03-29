package yimei.jss.gp;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static yimei.jss.FJSSMain.getFileNames;

/**
 * Created by dyska on 21/05/17.
 */
public class GPMain {

    public static void main(String[] args) {
        List<String> gpRunArgs = new ArrayList<>();
        boolean isTest = true;
        int maxTests = 1;
        boolean isDynamic = true;

        //include path to params file
        gpRunArgs.add("-file");

        if (isDynamic) {
            double utilLevel = 0.85;
            String objective = "max-flowtime";

            gpRunArgs.add("/Users/dyska/Desktop/Uni/COMP489/GPJSS/src/yimei/jss/algorithm/featureconstruction/fcgp-simplegp-dynamic.params");
            //gpRunArgs.add("/Users/dyska/Desktop/Uni/COMP489/GPJSS/src/yimei/jss/algorithm/coevolutiongp/baseline-coevolutiongp-dynamic.params");
            //gpRunArgs.add("/Users/dyska/Desktop/Uni/COMP489/GPJSS/src/yimei/jss/algorithm/simplegp/simplegp-dynamic.params");
            gpRunArgs.add("-p");
            gpRunArgs.add("eval.problem.eval-model.sim-models.0.util-level="+utilLevel);
            gpRunArgs.add("-p");
            gpRunArgs.add("eval.problem.eval-model.objectives.0="+objective);
            gpRunArgs.add("-p");
            for (int i = 1; i <= 30 && i <= maxTests; ++i) {
                gpRunArgs.add("seed.0="+String.valueOf(i));
                //convert list to array
                GPRun.main(gpRunArgs.toArray(new String[0]));
                //now remove the seed, we will add new value in next loop
                gpRunArgs.remove(gpRunArgs.size()-1);
            }
        } else {
            //gpRunArgs.add("/Users/dyska/Desktop/Uni/COMP489/GPJSS/src/yimei/jss/algorithm/featureselection/fsgp-simplegp-static.params");
            //gpRunArgs.add("/Users/dyska/Desktop/Uni/COMP489/GPJSS/src/yimei/jss/algorithm/featureselection/fsgp-coevolutiongp-static.params");
            //gpRunArgs.add("/Users/dyska/Desktop/Uni/COMP489/GPJSS/src/yimei/jss/algorithm/simplegp/simplegp.params");
            gpRunArgs.add("/Users/dyska/Desktop/Uni/COMP489/GPJSS/src/yimei/jss/algorithm/coevolutiongp/coevolutiongp.params");
            //gpRunArgs.add("-p");
            //gpRunArgs.add("terminals-from.0=static-coevolution/data-FJSS-Hurink_Data-Text-vdata-orb9-SEQUENCING.csv");
            //gpRunArgs.add("-p");
            //gpRunArgs.add("terminals-from.1=static-coevolution/data-FJSS-Hurink_Data-Text-vdata-orb9-ROUTING.csv");

            //gpRunArgs.add("/Users/dyska/Desktop/Uni/COMP489/GPJSS/src/yimei/jss/algorithm/simplegp/simplegp.params");
            //gpRunArgs.add("/Users/dyska/Desktop/Uni/COMP489/GPJSS/src/yimei/jss/algorithm/coevolutiongp/coevolutiongp.params");
            gpRunArgs.add("-p");

            //static FJSS, so using file paths
            String path = "";
            if (args.length > 0) {
                //allow more specific folder or file paths to be used
                path = args[0];
            }
            path = (new File("")).getAbsolutePath() + "/data/FJSS/" + path;

            List<String> fileNames = getFileNames(new ArrayList(), Paths.get(path), ".fjs");

            for (String fileName: fileNames) {
                //worry about saving output later
                gpRunArgs.add("filePath="+fileName);
                gpRunArgs.add("-p");
                for (int i = 1; i <= 30 && i <= maxTests; ++i) {
                    gpRunArgs.add("seed.0="+String.valueOf(i));
                    //convert list to array
                    GPRun.main(gpRunArgs.toArray(new String[0]));
                    //now remove the seed, we will add new value in next loop
                    gpRunArgs.remove(gpRunArgs.size()-1);
                }
                //now remove filePath etc
                gpRunArgs = gpRunArgs.subList(0,3);
                if (isTest) {
                    break;
                }
            }
        }
    }
}
