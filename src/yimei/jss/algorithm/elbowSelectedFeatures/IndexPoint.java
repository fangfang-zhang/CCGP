package yimei.jss.algorithm.elbowSelectedFeatures;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class IndexPoint implements Clusterable {
    double[] position;

    public IndexPoint(double[] position){
        this.position = position;
    }

    public double[] getPoint(){
        return this.position;
    }
}
