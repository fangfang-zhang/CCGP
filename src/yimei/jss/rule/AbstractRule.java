package yimei.jss.rule;

import ec.EvolutionState;
import ec.Fitness;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import yimei.jss.jobshop.Objective;
import yimei.jss.jobshop.OperationOption;
import yimei.jss.jobshop.SchedulingSet;
import yimei.jss.jobshop.WorkCenter;
import yimei.jss.simulation.RoutingDecisionSituation;
import yimei.jss.simulation.SequencingDecisionSituation;
import yimei.jss.simulation.Simulation;
import yimei.jss.simulation.state.SystemState;

import java.util.List;

/**
 * The abstract dispatching rule for job shop scheduling.
 * <p>
 * Created by yimei on 22/09/16.
 */
public abstract class AbstractRule extends EvolutionState{

	protected String name;
	protected RuleType type;

	//fzhang 2018.10.10  get seed
    protected long jobSeed;

	public String getName() {
		return name;
	}

	public RuleType getType() {
		return type;
	}

	@Override
	public String toString() {
		return name;
	}

	public RealMatrix objectiveValueMatrix(SchedulingSet schedulingSet, List<Objective> objectives) {
		int rows = schedulingSet.getObjectiveLowerBoundMtx().getRowDimension();
		int cols = schedulingSet.getObjectiveLowerBoundMtx().getColumnDimension();

		RealMatrix matrix = new Array2DRowRealMatrix(rows, cols);
		List<Simulation> simulations = schedulingSet.getSimulations();
		int col = 0;

		for (int j = 0; j < simulations.size(); j++) {
			Simulation simulation = simulations.get(j);
			simulation.setSequencingRule(this);

			simulation.run();
			// System.out.println(simulation.workCenterUtilLevelsToString());

			for (int i = 0; i < objectives.size(); i++) {
				matrix.setEntry(i, col, simulation.objectiveValue(objectives.get(i)));
			}

			col++;

			for (int k = 1; k < schedulingSet.getReplications().get(j); k++) {
				simulation.rerun();
				// System.out.println(simulation.workCenterUtilLevelsToString());

				for (int i = 0; i < objectives.size(); i++) {
					matrix.setEntry(i, col, simulation.objectiveValue(objectives.get(i)));
				}

				col++;
			}

			simulation.reset();
		}

		return matrix;
	}

	public void calcFitness(Fitness fitness, EvolutionState state, SchedulingSet schedulingSet, AbstractRule otherRule,
			List<Objective> objectives) {
		// whenever fitness is calculated, need a routing rule and a sequencing rule
		if (this.getType() == otherRule.getType()) {
			System.out.println(
					"We need one routing rule and one sequencing rule, not 2 " + otherRule.getType() + " rules.");
			return;
		}
		AbstractRule routingRule;
		AbstractRule sequencingRule;
		// check type, not here
		if (this.getType() == RuleType.ROUTING) {
			routingRule = this;
			sequencingRule = otherRule;
		} else {
			routingRule = otherRule;
			sequencingRule = this;
		}

		double[] fitnesses = new double[objectives.size()];

		List<Simulation> simulations = schedulingSet.getSimulations();
		int col = 0;

		//System.out.println("The simulation size is "+simulations.size()); //1
		for (int j = 0; j < simulations.size(); j++) {
			Simulation simulation = simulations.get(j);
			simulation.setSequencingRule(sequencingRule);
			simulation.setRoutingRule(routingRule);
			// }
			simulation.rerun();

			for (int i = 0; i < objectives.size(); i++) {
				// System.out.println("Makespan:
				// "+simulation.objectiveValue(objectives.get(i)));
				// System.out.println("Benchmark makespan:
				// "+schedulingSet.getObjectiveLowerBound(i, col));
				
				//fzhang 2018.10.23  cancel normalizing objective
//				double normObjValue = simulation.objectiveValue(objectives.get(i))
//						/ schedulingSet.getObjectiveLowerBound(i, col);
				
				double ObjValue = simulation.objectiveValue(objectives.get(i));

				//modified by fzhang, 26.4.2018  check in test process, whether there is ba
				//fzhang 2018.10.23  cancel normalizing objective
//				fitnesses[i] += normObjValue;		
				
				fitnesses[i] += ObjValue;
			}

			col++;

			//System.out.println("The value of replication is "+schedulingSet.getReplications()); //50
			for (int k = 1; k < schedulingSet.getReplications().get(j); k++) {
				simulation.rerun();

				for (int i = 0; i < objectives.size(); i++) {
//					double normObjValue = simulation.objectiveValue(objectives.get(i))
//							/ schedulingSet.getObjectiveLowerBound(i, col);
//					fitnesses[i] += normObjValue;
				
					//fzhang 2018.10.23  cancel normalizing objective
					double ObjValue = simulation.objectiveValue(objectives.get(i));
					fitnesses[i] += ObjValue;
				}

				col++;
			}

			simulation.reset();
		}

		for (int i = 0; i < fitnesses.length; i++) {
			fitnesses[i] /= col;
		}
		MultiObjectiveFitness f = (MultiObjectiveFitness) fitness;
		f.setObjectives(state, fitnesses);
	}
	
	public OperationOption priorOperation(SequencingDecisionSituation sequencingDecisionSituation) {
		
		List<OperationOption> queue = sequencingDecisionSituation.getQueue();
		WorkCenter workCenter = sequencingDecisionSituation.getWorkCenter();
		SystemState systemState = sequencingDecisionSituation.getSystemState();

		//fzhang 2018.10.23  original one
		//============================start==============================	  
		OperationOption priorOp = queue.get(0);
		priorOp.setPriority(priority(priorOp, workCenter, systemState));

		for (int i = 1; i < queue.size(); i++) {
			OperationOption op = queue.get(i);
			op.setPriority(priority(op, workCenter, systemState));

			if (op.priorTo(priorOp))
				priorOp = op;
		}

		return priorOp;
		}
		//============================end==============================

		//fzhang 2019.9.25 calculate the priority values of each operations, then get the rank of all operations
	//==============================================start=====================================================
		public int[] priorValueOperation(SequencingDecisionSituation sequencingDecisionSituation) {

			List<OperationOption> queue = sequencingDecisionSituation.getQueue();
			WorkCenter workCenter = sequencingDecisionSituation.getWorkCenter();
			SystemState systemState = sequencingDecisionSituation.getSystemState();

			//============================start=============================
			int[] ranks = new int[queue.size()];
			for (int i = 0; i < queue.size(); i++) {
				OperationOption op = queue.get(i);
				op.setPriority(priority(op, workCenter, systemState));
				//priorityValue[i] = op.getPriority();
			}

			int rank = 1;
			for (int i = 0; i < queue.size(); i++) {
				for (int j = 0; j < queue.size(); j++) {
					if (queue.get(j).priorTo(queue.get(i))) {
						rank++;
					}
				}
				ranks[i] = rank;
				rank = 1;
			}

			//do not have effect here, that means the phenotypic characterisation is fine for sequencing rule
			//but safe to put it here
			int count = 0;
			for(int i = ranks.length-1; i >= 0; i--){
				for(int j = 0; j < i; j++){
					if (ranks[j] == ranks[i]){
						count++;
					}
				}
				ranks[i] += count;
				count = 0;
			}

			return ranks;
		}
//==================================================end=======================================================
	
	//fzhang 2019.9.25 calculate the priority values of each operations, then get the rank of all machines
	public int[] priorValueOperationMahcine(RoutingDecisionSituation routingDecisionSituation) {

		List<OperationOption> queue = routingDecisionSituation.getQueue();
		SystemState systemState = routingDecisionSituation.getSystemState();
		int[] ranks = new int[queue.size()];

		for (int i = 0; i < queue.size(); i++) {
			OperationOption operationOption = queue.get(i);
			operationOption.setPriority(priority(operationOption, operationOption.getWorkCenter(), systemState));
		}

		int rank = 1;
		for (int i = 0; i < queue.size(); i++) {
			for (int j = 0; j < queue.size(); j++) {
				if (queue.get(j).priorTo(queue.get(i))) {
					rank++;
				}
			}
			ranks[i] = rank;
			rank = 1;
		}

		int count = 0;
		for(int i = ranks.length-1; i >= 0; i--){
			for(int j = 0; j < i; j++){
                if (ranks[j] == ranks[i]){
                	count++;
				}
			}
			ranks[i] += count;
			count = 0;
		}

		return ranks;
	}
	//============end============================================


	public OperationOption nextOperationOption(RoutingDecisionSituation routingDecisionSituation) {

		List<OperationOption> queue = routingDecisionSituation.getQueue();
		SystemState systemState = routingDecisionSituation.getSystemState();
		//================original=================
		//==================start==================
		OperationOption bestOperationOption = queue.get(0);
		bestOperationOption
				.setPriority(priority(bestOperationOption, bestOperationOption.getWorkCenter(), systemState));
		// loop all the options, save the best one as "selected" one
		for (int i = 1; i < queue.size(); i++) {
			OperationOption operationOption = queue.get(i);
			operationOption.setPriority(priority(operationOption, operationOption.getWorkCenter(), systemState));

			if (operationOption.priorTo(bestOperationOption)) {
				bestOperationOption = operationOption;
			}
		}
		return bestOperationOption;// this links which machine will be chosen.
	}


	
		//===========================================AAAI2019========================================
		//fzhang 6.8.2018  incorporating knowledge (workload) into dispatching rule---start from here
		//=========================================start============================================
/*		double totalProcessTimeOfWorkCenterInSystem = 0;

		for (WorkCenter w : systemState.getWorkCenters()) {
			totalProcessTimeOfWorkCenterInSystem += w.getWorkInQueue();
		}

		OperationOption bestOperationOption = queue.get(0);

		//fzhang 4.6.2018 set the priority value related to workload/workloadInSystem
		//setPriority(): this method is set a double value as priority value to OperationOption
		double bestWorkLoadRatio = 0;
		if(bestOperationOption.getWorkCenter().getWorkInQueue() != 0) {
		     bestWorkLoadRatio = bestOperationOption.getWorkCenter().getWorkInQueue()
					/ totalProcessTimeOfWorkCenterInSystem;
		     bestOperationOption
				.setPriority(1/(1-bestWorkLoadRatio)*priority(bestOperationOption, bestOperationOption.getWorkCenter(), systemState));
		}
		else
			bestOperationOption
			.setPriority(priority(bestOperationOption, bestOperationOption.getWorkCenter(), systemState));
		
		
		// loop all the options, save the best one as "selected" one

		for (int i = 1; i < queue.size(); i++) {
			OperationOption operationOption = queue.get(i);
			double workLoadRatio = 0;
			
			if(operationOption.getWorkCenter().getWorkInQueue() != 0) {
				workLoadRatio = operationOption.getWorkCenter().getWorkInQueue()
						/ totalProcessTimeOfWorkCenterInSystem;
					operationOption.setPriority(1/(1-workLoadRatio) * priority(operationOption, operationOption.getWorkCenter(), systemState));
			}
			else
			  operationOption.setPriority(priority(operationOption, operationOption.getWorkCenter(), systemState));
			
			if (operationOption.priorTo(bestOperationOption)) {
				bestOperationOption = operationOption;
			}
		}

		return bestOperationOption;// this links which machine will be chosen.
}*/
//=========================================================end================================================
	public abstract double priority(OperationOption op, WorkCenter workCenter, SystemState systemState);
}
