package yimei.jss.helper;

import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPNodeParent;
import ec.gp.GPTree;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.Clusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.util.MathUtils;
import yimei.jss.gp.terminal.TerminalERCUniform;

import java.io.*;
import java.util.*;

public class PopulationUtils
{
	/**
	 * Sorts individuals based on their fitness. The method iterates over all subpopulations
	 * and sorts the array of individuals in them based on their fitness so that the first
	 * individual has the best (i.e. least) fitness.
	 *
	 * @param pop a population to sort. Can't be {@code null}.
	 * @return the given pop with individuals sorted.
	 */
	public static Population sort(Population pop)
	{
		Comparator<Individual> comp = (Individual o1, Individual o2) ->
		{
			if(o1.fitness.fitness() < o2.fitness.fitness())
				return -1;
			if(o1.fitness.fitness() == o2.fitness.fitness())
				return 0;

			return 1;
		};

		for(Subpopulation subpop : pop.subpops)
			Arrays.sort(subpop.individuals, comp);

		return pop;
	}

	//2020.2.10 sort the individuals in the arraylist
	public static Individual[] sortInds(ArrayList<Individual> individuals) {
		Comparator<Individual> comp = (Individual o1, Individual o2) ->
		{
			if (o1.fitness.fitness() < o2.fitness.fitness())
				return -1;
			if (o1.fitness.fitness() == o2.fitness.fitness())
				return 0;

			return 1;
		};
		//convert arraylist to array
		Individual[] inds = individuals.toArray(new Individual[individuals.size()]);
		Arrays.sort(inds, comp);
		return inds;
	}

	//fzhang 2019.9.4 change a GPNode to a GPtree
	public static GPTree GPNodetoGPTree(GPNode node)
	{
		GPNodeParent parent = node.parent;
		if(parent != null)
		{
			if(parent instanceof GPNode)
			{
				GPNode p = (GPNode)parent;
				p.children[node.argposition] = null;
			}
			else if(parent instanceof GPTree)
			{
				GPTree t = (GPTree) parent;
				t.child = null;
			}
		}

		GPTree tree = new GPTree();
		tree.child = node;
		node.parent = tree;
		node.argposition = 0;

		return tree;
	}

	static void Frequency(TerminalsStats stats, GPNode node) {
		if (node == null) {
			return;
		}

		if (node.children == null || node.children.length == 0) {  //1. a node does not have child is a terminal
			                                                       //2. the length of node's child is 0 (empty array)---it is a terminal
			stats.update(((TerminalERCUniform)node).getTerminal().name()); //read terminals
			return;
		}

		for(GPNode child : node.children) //repeat to check the terminals
			Frequency(stats, child);
	}

	public static ArrayList<HashMap<String,Integer>> Frequency(Population pop, int topInds) {
		sort(pop); //sort the subpop separately

		ArrayList<HashMap<String,Integer>> retval = new ArrayList<HashMap<String,Integer>>();
		for(Subpopulation subpop : pop.subpops)
			for (int i = 0; i < topInds; i++) {
				TerminalsStats stats = new TerminalsStats();
				Frequency(stats, ((GPIndividual)(subpop.individuals[i])).trees[0].child);
				retval.add(stats.getStats());
		}
		return retval;
	}

	public static void sort(Individual[] ind)
	{
		Comparator<Individual> comp = (Individual o1, Individual o2) ->
		{
			if(o1.fitness.fitness() < o2.fitness.fitness())
				return -1;
			if(o1.fitness.fitness() == o2.fitness.fitness())
				return 0;

			return 1;
		};

		Arrays.sort(ind, comp);
	}

//	public void prepareForWriting(Population population, Subpopulation sub) throws IOException
//	{
//		if(!isSaving)
//			throw new IOException("This object is not initialized for saving objects.");
//		if(output == null)
//		{
//			output = new ObjectOutputStream(new FileOutputStream(file));
//			output.writeInt(population.subpops.length);
//		}
//
//		output.writeInt(sub.individuals.length);
//	}

//	public void write(Individual ind) throws IOException
//	{
//		if(!isSaving)
//			throw new IOException("This object is not initialized for saving objects.");
//
//		output.writeObject(ind);
//	}

	public static void savePopulation(Population pop, String fileName)
			throws FileNotFoundException, IOException
	{
		File file = new File(fileName);
		if(file.exists())
			file.delete();
		try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file)))
		{
			int nSubPops = pop.subpops.length;
			oos.writeInt(nSubPops);
			for(Subpopulation subpop : pop.subpops)
			{
				int nInds = subpop.individuals.length;
				oos.writeInt(nInds);
				for(Individual ind : subpop.individuals)
				{
					oos.writeObject(ind);
				}
			}
		}
	}

	public static Population loadPopulation(File file)
			throws FileNotFoundException, IOException, ClassNotFoundException, InvalidObjectException
	{
		Population retval = new Population();
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file)))
		{
			int numSub = ois.readInt();
			retval.subpops = new Subpopulation[numSub];
			for(int subInd = 0; subInd < numSub; subInd++)
			{
				int numInd = ois.readInt();
				retval.subpops[subInd] = new Subpopulation();
				retval.subpops[subInd].individuals = new Individual[numInd];
				for(int indIndex = 0; indIndex < numInd; indIndex++)
				{
					Object ind = ois.readObject();
					if(!(ind instanceof Individual))
						throw new InvalidObjectException("The file contains an object that is not "
								+ "instance of Individual: " + ind.getClass().toString());
					retval.subpops[subInd].individuals[indIndex] = (Individual)ind;
				}
			}
		}

		return retval;
	}
//
//	public static ArrayList<Population> loadPopulation(String inputFileNamePath, int numGenerations)
//	{
//		ArrayList<Population> retval = new ArrayList<>();
//
//		for(int i = 0; i < numGenerations; i++)
//		{
//			Population p = PopulationUtils.loadPopulation(
//					Paths.get(inputFileNamePath, "population.gen." + i + ".bin").toFile());
//			p = PopulationUtils.sort(p);
//			double fit = p.subpops[0].individuals[0].fitness.fitness();
//			if(fit > maxFit)
//				maxFit = fit;
//			if(fit < minFit)
//				minFit = fit;
//			popList.add(p);
//		}
//
//		return retval;
//	}

	public static Population loadPopulation(String fileName)
			throws FileNotFoundException, IOException, ClassNotFoundException, InvalidObjectException
	{
		File file = new File(fileName);
		return loadPopulation(file);
	}


	//fzhang 2019.6.6 get the index of best individuals
	public static int  getIndexOfbestInds(Population pop, int numSubPop){
		{
			int best_index = 0;
			double best_fitness = pop.subpops[numSubPop].individuals[0].fitness.fitness();
			for(int ind = 0; ind < pop.subpops[numSubPop].individuals.length; ind++){
				if(pop.subpops[numSubPop].individuals[ind].fitness.fitness() < best_fitness){
					best_fitness = pop.subpops[numSubPop].individuals[ind].fitness.fitness();
					best_index = ind;
				}
			}
            return best_index;
	}
}

    //2021.4.16 calculate the diversity of individuals--based on phenotypic characteristic
	public static double entropy(double[][][] PC)
	{
//		List<GPIndsClusterable> pool = Arrays.stream(inds).map(
//				i -> new GPIndsClusterable(i, tree, filter, metric)).collect(Collectors.toList());

		List<DoublePoint> listPC = new ArrayList<>();
		for(int indpc = 0; indpc < PC[0].length; indpc++){
			listPC.add(new DoublePoint(PC[0][indpc]));
		}

		DBSCANClusterer<DoublePoint> clusterer = new DBSCANClusterer<>(0, 1);
		List<Cluster<DoublePoint>> clusters = clusterer.cluster(listPC);

		double entropy = 0;
		ArrayList<DoublePoint> noises = clusterer.getNoises();

		for(Cluster<DoublePoint> cluster : clusters)
		{
			int clusterSize = cluster.getPoints().size();
			if(clusterSize == 0) // for any reason
				continue;
			double p = clusterSize / ((double)listPC.size()); //there is the clusters have more than one points
			entropy += p * Math.log(p);
		}

		for(int i = 0; i < noises.size(); i++)
		{
			double p = 1 / ((double)listPC.size()); //noises indicate the clusters have only one point
			entropy += p * Math.log(p);
		}
//		nClusters = clusters.size();
//		nNoise = noises.size();

		return -entropy; //a larger entropy indicates a better diversity
	}

	public static double entropy(double[][] PC)
	{
//		List<GPIndsClusterable> pool = Arrays.stream(inds).map(
//				i -> new GPIndsClusterable(i, tree, filter, metric)).collect(Collectors.toList());

		List<DoublePoint> listPC = new ArrayList<>();
		for(int indpc = 0; indpc < PC.length; indpc++){
			listPC.add(new DoublePoint(PC[indpc]));
		}

		DBSCANClusterer<DoublePoint> clusterer = new DBSCANClusterer<>(0, 1);
		List<Cluster<DoublePoint>> clusters = clusterer.cluster(listPC);

		double entropy = 0;
		ArrayList<DoublePoint> noises = clusterer.getNoises();

		for(Cluster<DoublePoint> cluster : clusters)
		{
			int clusterSize = cluster.getPoints().size();
			if(clusterSize == 0) // for any reason
				continue;
			double p = clusterSize / ((double)listPC.size()); //there is the clusters have more than one points
			entropy += p * Math.log(p);
		}

		for(int i = 0; i < noises.size(); i++)
		{
			double p = 1 / ((double)listPC.size()); //noises indicate the clusters have only one point
			entropy += p * Math.log(p);
		}
//		nClusters = clusters.size();
//		nNoise = noises.size();

		return -entropy; //a larger entropy indicates a better diversity
	}



	public static double entropy(int[][] PC)
	{
//		List<GPIndsClusterable> pool = Arrays.stream(inds).map(
//				i -> new GPIndsClusterable(i, tree, filter, metric)).collect(Collectors.toList());

		List<DoublePoint> listPC = new ArrayList<>();
		for(int indpc = 0; indpc < PC.length; indpc++){
			listPC.add(new DoublePoint(PC[indpc]));
		}

		DBSCANClusterer<DoublePoint> clusterer = new DBSCANClusterer<>(0, 1);
		List<Cluster<DoublePoint>> clusters = clusterer.cluster(listPC);

		double entropy = 0;
		ArrayList<DoublePoint> noises = clusterer.getNoises();

		for(Cluster<DoublePoint> cluster : clusters)
		{
			int clusterSize = cluster.getPoints().size();
			if(clusterSize == 0) // for any reason
				continue;
			double p = clusterSize / ((double)listPC.size()); //there is the clusters have more than one points
			entropy += p * Math.log(p);
		}

		for(int i = 0; i < noises.size(); i++)
		{
			double p = 1 / ((double)listPC.size()); //noises indicate the clusters have only one point
			entropy += p * Math.log(p);
		}
//		nClusters = clusters.size();
//		nNoise = noises.size();

		return -entropy; //a larger entropy indicates a better diversity
	}
}


class DBSCANClusterer<T extends Clusterable> extends Clusterer<T> {
	private final double eps;
	private final int minPts;

	public DBSCANClusterer(double eps, int minPts) throws NotPositiveException {
		this(eps, minPts, new EuclideanDistance());
	}

	public DBSCANClusterer(double eps, int minPts, DistanceMeasure measure) throws NotPositiveException {
		super(measure);
		if (eps < 0.0D) {
			throw new NotPositiveException(eps);
		} else if (minPts < 0) {
			throw new NotPositiveException(minPts);
		} else {
			this.eps = eps;
			this.minPts = minPts;
		}
	}

	public double getEps() {
		return this.eps;
	}

	public int getMinPts() {
		return this.minPts;
	}

	Map<T, PointStatus> visited;

	public List<Cluster<T>> cluster(Collection<T> points) throws NullArgumentException {
		MathUtils.checkNotNull(points);
		List<Cluster<T>> clusters = new ArrayList();
		Iterator<T> i = points.iterator();
		visited = new HashMap<>();

		while(i.hasNext()) {
			T point = i.next();
			if (visited.get(point) == null) {
				List<T> neighbors = this.getNeighbors(point, points);
				if (neighbors.size() >= this.minPts) {
					Cluster<T> cluster = new Cluster<>();
					clusters.add(this.expandCluster(cluster, point, neighbors, points, visited));
				} else {
					visited.put(point, PointStatus.NOISE);
				}
			}
		}

		return clusters;
	}

	private Cluster<T> expandCluster(Cluster<T> cluster, T point, List<T> neighbors, Collection<T> points, Map<T, PointStatus> visited) {
		cluster.addPoint(point);
		visited.put(point, PointStatus.PART_OF_CLUSTER);
		List<T> seeds = new ArrayList(neighbors);

		for(int index = 0; index < (seeds).size(); ++index) {
			T current = (seeds).get(index);
			PointStatus pStatus = (PointStatus)visited.get(current);
			if (pStatus == null) {
				List<T> currentNeighbors = this.getNeighbors(current, points);
				if (currentNeighbors.size() >= this.minPts) {
					seeds = this.merge(seeds, currentNeighbors);
				}
			}

			if (pStatus != PointStatus.PART_OF_CLUSTER) {
				visited.put(current, PointStatus.PART_OF_CLUSTER);
				cluster.addPoint(current);
			}
		}

		return cluster;
	}

	private List<T> getNeighbors(T point, Collection<T> points) {
		List<T> neighbors = new ArrayList();
		Iterator<T> i$ = points.iterator();

		while(i$.hasNext()) {
			T neighbor = i$.next();
			if (point != neighbor && this.distance(neighbor, point) <= this.eps) {
				neighbors.add(neighbor);
			}
		}

		return neighbors;
	}

	private List<T> merge(List<T> one, List<T> two) {
		Set<T> oneSet = new HashSet<T>(one);
		Iterator<T> i$ = two.iterator();

		while(i$.hasNext()) {
			T item = i$.next();
			if (!oneSet.contains(item)) {
				one.add(item);
			}
		}

		return one;
	}

	public ArrayList<T> getNoises()
	{
		ArrayList<T> retval = new ArrayList<>();
		if(visited == null)
			return retval;

		visited.forEach((clusterable, pointStatus) -> {if(pointStatus == PointStatus.NOISE) retval.add(clusterable);});

		return retval;
	}

	private static enum PointStatus {
		NOISE,
		PART_OF_CLUSTER;

		private PointStatus() {
		}
	}
}



class TerminalsStats {
	private HashMap<String, Integer> stats = new HashMap<>();

	public void update(String nodeName) {
		if (stats.containsKey(nodeName) == false) {
			stats.put(nodeName, 0); // put: set the value
		}

		stats.put(nodeName, stats.get(nodeName) + 1);
	}

	public HashMap<String, Integer> getStats() {
		return stats;
	}
}


