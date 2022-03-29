package yimei.jss.ruleanalysis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ec.Fitness;
import ec.gp.koza.KozaFitness;
import ec.multiobjective.MultiObjectiveFitness;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import yimei.jss.jobshop.Objective;
import yimei.jss.jobshop.SchedulingSet;
import yimei.jss.rule.AbstractRule;

public class TestResult {

	private List<AbstractRule[]> generationalRules;
	private List<Fitness> generationalTrainFitnesses;  //generational things is a list to contain informaiton related
	private List<Fitness> generationalValidationFitnesses;
	private List<Fitness> generationalTestFitnesses;
	private AbstractRule[] bestRules;
	private Fitness bestTrainingFitness;
	private Fitness bestValidationFitness;
	private Fitness bestTestFitness;
	private DescriptiveStatistics generationalTimeStat;
	//fzhang 24.8.2018 read badrun into CSV
	private DescriptiveStatistics generationalBadRunStat;

	//fzhang 31.5.2019 read average rule size into CSV
	private DescriptiveStatistics generationalAveRoutingRuleSizeStat;
	private DescriptiveStatistics generationalAveSequencingRuleSizeStat;
	private DescriptiveStatistics generationalAveRuleSizeStat;

	
	//fzhang 2019.1.14 read training fitness into CSV
	private DescriptiveStatistics generationalTrainingFitnessStat0;
	private DescriptiveStatistics generationalTrainingFitnessStat1;

	public static final long validationSimSeed = 483561;

	public TestResult() {
		generationalRules = new ArrayList<>();
		generationalTrainFitnesses = new ArrayList<>();
		generationalValidationFitnesses = new ArrayList<>();
		generationalTestFitnesses = new ArrayList<>();
	}

	public List<AbstractRule[]> getGenerationalRules() {
		return generationalRules;
	}

	public AbstractRule[] getGenerationalRules(int idx) {
		return generationalRules.get(idx);
	}

	public List<Fitness> getGenerationalTrainFitnesses() {
		return generationalTrainFitnesses;
	}

	public Fitness getGenerationalTrainFitness(int idx) {
		return generationalTrainFitnesses.get(idx);
	}

	public List<Fitness> getGenerationalValidationFitnesses() {
		return generationalValidationFitnesses;
	}

	public Fitness getGenerationalValidationFitness(int idx) {
		return generationalValidationFitnesses.get(idx);
	}

	public List<Fitness> getGenerationalTestFitnesses() {
		return generationalTestFitnesses;
	}

	public Fitness getGenerationalTestFitness(int idx) {
		return generationalTestFitnesses.get(idx);
	}

	public AbstractRule[] getBestRules() {
		return bestRules;
	}

	public Fitness getBestTrainingFitness() {
		return bestTrainingFitness;
	}

	public Fitness getBestValidationFitness() {
		return bestValidationFitness;
	}

	public Fitness getBestTestFitness() {
		return bestTestFitness;
	}

	public DescriptiveStatistics getGenerationalTimeStat() {
		return generationalTimeStat;
	}

	public double getGenerationalTime(int gen) {
		return generationalTimeStat.getElement(gen);
	}

	//fzhang 24.8.2018 get the badrun into CSV
	public DescriptiveStatistics getGenerationalBadRunStat() {
		return generationalBadRunStat;
	}

	public double getGenerationalBadRun(int gen) {
		return generationalBadRunStat.getElement(gen);
	}

	//fzhang 2019.6.1 get the average routing rule size into CSV
	//===========================start========================
	public DescriptiveStatistics getGenerationalAveRoutingRuleSizeStatStat() {
		return generationalAveRoutingRuleSizeStat;
	}
	public double getGenerationalAveRoutingRuleSizeStatStat(int gen) {
		return generationalAveRoutingRuleSizeStat.getElement(gen);
	}
    //======================================end================================

	//fzhang 2019.6.1 get the average routing rule size into CSV
	//===================================start==================================
	public DescriptiveStatistics getGenerationalAveSequencingRuleSizeStatStat() { return generationalAveSequencingRuleSizeStat;	}

	public double getGenerationalAveSequencingRuleSizeStatStat(int gen) {
		return generationalAveSequencingRuleSizeStat.getElement(gen);
	}
	//====================================end===================================

	//fzhang 2019.6.1 get the average rule size into CSV
	//===================================start==================================
	public DescriptiveStatistics getGenerationalAveRuleSizeStatStat() { return generationalAveRuleSizeStat;}

	public double getGenerationalAveRuleSizeStatStat(int gen) {
		return generationalAveRuleSizeStat.getElement(gen);
	}
	//====================================end===================================

	// fzhang 2019.1.14 get the training fitness into CSV
	public DescriptiveStatistics getGenerationalTrainingFitnessStat0() {
		return generationalTrainingFitnessStat0;
	}

	public double getGenerationalTrainingFitness0(int gen) {
		return generationalTrainingFitnessStat0.getElement(gen);
	}
	
	public DescriptiveStatistics getGenerationalTrainingFitnessStat1() {
		return generationalTrainingFitnessStat1;
	}

	public double getGenerationalTrainingFitness1(int gen) {
		return generationalTrainingFitnessStat1.getElement(gen);
	}
//=========================================================================
	public void setGenerationalRules(List<AbstractRule[]> generationalRules) {
		this.generationalRules = generationalRules;
	}

	public void addGenerationalRules(AbstractRule[] rules) {
		this.generationalRules.add(rules);
	}

	public void setGenerationalTrainFitnesses(List<Fitness> generationalTrainFitnesses) {
		this.generationalTrainFitnesses = generationalTrainFitnesses;
	}

	public void addGenerationalTrainFitness(Fitness f) {
		this.generationalTrainFitnesses.add(f);
	}

	public void setGenerationalValidationFitness(List<Fitness> generationalValidationFitnesses) {
		this.generationalValidationFitnesses = generationalValidationFitnesses;
	}

	public void addGenerationalValidationFitnesses(Fitness f) {
		this.generationalValidationFitnesses.add(f);
	}

	public void setGenerationalTestFitnesses(List<Fitness> generationalTestFitnesses) {
		this.generationalTestFitnesses = generationalTestFitnesses;
	}

	public void addGenerationalTestFitnesses(Fitness f) {
		this.generationalTestFitnesses.add(f);
	}

	public void setBestRules(AbstractRule[] bestRules) {
		this.bestRules = bestRules;
	}

	public void setBestTrainingFitness(Fitness bestTrainingFitness) {
		this.bestTrainingFitness = bestTrainingFitness;
	}

	public void setBestValidationFitness(Fitness bestValidationFitnesses) {
		this.bestValidationFitness = bestValidationFitnesses;
	}

	public void setBestTestFitness(Fitness bestTestFitnesses) {
		this.bestTestFitness = bestTestFitnesses;
	}

	public void setGenerationalTimeStat(DescriptiveStatistics generationalTimeStat) {
		this.generationalTimeStat = generationalTimeStat;
	}

	//31.5.2019 fzhang read average routing rule size into CSV
	public void setGenerationalAveRoutingRuleSizeStat(DescriptiveStatistics generationalAveRoutingRuleSizeStat) {
		this.generationalAveRoutingRuleSizeStat = generationalAveRoutingRuleSizeStat;
	}

	//31.5.2019 fzhang read average sequencing rule size into CSV
	public void setGenerationalAveSequencingRuleSizeStat(DescriptiveStatistics generationalAveSequencingRuleSizeStat) {
		this.generationalAveSequencingRuleSizeStat = generationalAveSequencingRuleSizeStat;
	}

	//5.6.2019 fzhang read average rule size into CSV
	public void setGenerationalAveRuleSizeStat(DescriptiveStatistics generationalAveRuleSizeStat) {
		this.generationalAveRuleSizeStat = generationalAveRuleSizeStat;
	}

	//=======================================================================================
	//24.8.2018 fzhang read badrun into CSV
	public void setGenerationalBadRunStat(DescriptiveStatistics generationalBadRunStat) {
		this.generationalBadRunStat = generationalBadRunStat;
	}
	//======================================================================================
	
	// =======================================================================================
	// 24.8.2018 fzhang read trainingfitness into CSV
	public void setGenerationalTrainingFitnessStat0(DescriptiveStatistics generationalTrainingFitnessStat0) {
		this.generationalTrainingFitnessStat0 = generationalTrainingFitnessStat0;
	}
	
	public void setGenerationalTrainingFitnessStat1(DescriptiveStatistics generationalTrainingFitnessStat1) {
		this.generationalTrainingFitnessStat1 = generationalTrainingFitnessStat1;
	}
	// ======================================================================================
	
	public static TestResult readFromFile(File file, RuleType ruleType, int numPopulations) {
		return ResultFileReader.readTestResultFromFile(file, ruleType, ruleType.isMultiobjective(), numPopulations);
	}

	public void validate(List<Objective> objectives) {
		SchedulingSet validationSet =
				SchedulingSet.dynamicMissingSet(validationSimSeed, 0.95,
						4.0, objectives, 50);

		Fitness validationFitness;
		if (objectives.size() == 1) {
			validationFitness = new KozaFitness();
			bestValidationFitness = new KozaFitness();
		}
		else {
			validationFitness = new MultiObjectiveFitness();
            bestValidationFitness = new MultiObjectiveFitness();
		}

		bestRules = generationalRules.get(0);

		//bestRule.calcFitness(bestValidationFitness, null, validationSet, objectives);
		generationalValidationFitnesses.add(bestValidationFitness);

//		System.out.println("Generation 0: validation fitness = " + bestValidationFitness.fitness());

		for (int i = 1; i < generationalRules.size(); i++) {
			//generationalRules.get(i).calcFitness(validationFitness, null, validationSet, objectives);
			generationalValidationFitnesses.add(validationFitness);


//			System.out.println("Generation " + i + ": validation fitness = " + validationFitness.fitness());

			if (validationFitness.betterThan(bestValidationFitness)) {
				bestRules = generationalRules.get(i);
				bestTrainingFitness = generationalTrainFitnesses.get(i);
				bestValidationFitness = validationFitness;
			}
		}
	}
}
