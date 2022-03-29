package yimei.jss.ruleoptimisation;

import java.util.ArrayList;
import java.util.List;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.util.Parameter;
import yimei.jss.jobshop.Objective;
import yimei.jss.rule.AbstractRule;
import yimei.jss.rule.RuleType;
import yimei.jss.rule.operation.evolved.GPRule;
import yimei.jss.ruleevaluation.AbstractEvaluationModel;

public class MultipleTreeRuleOptimizationProblem extends RuleOptimizationProblem {

	   public static final String P_EVAL_MODEL = "eval-model";

	    private AbstractEvaluationModel evaluationModel;

	    public List<Objective> getObjectives() {
	        return evaluationModel.getObjectives();
	    }

	    public AbstractEvaluationModel getEvaluationModel() {
	        return evaluationModel;
	    }

	    public void rotateEvaluationModel() {
	        evaluationModel.rotate();
	    }
	 @Override
	    public void setup(final EvolutionState state, final Parameter base) {
	        super.setup(state, base);  //about ADFStack and ADFContext

	        Parameter p = base.push(P_EVAL_MODEL);  //yimei.jss.ruleevaluation.MultipleRuleEvaluationModel  here is different with before.
	        evaluationModel = (AbstractEvaluationModel)(
	                state.parameters.getInstanceForParameter(
	                        p, null, AbstractEvaluationModel.class));

	        evaluationModel.setup(state, p);
	    }

	 public void normObjective(EvolutionState state, Individual indi, int subpopulation, int threadnum) {

		 GPRule sequencingRule = new GPRule(RuleType.SEQUENCING, ((GPIndividual) indi).trees[0]);
		 GPRule routingRule = new GPRule(RuleType.ROUTING, ((GPIndividual) indi).trees[1]);

		 List rules = new ArrayList();
		 List fitnesses = new ArrayList();

		 rules.add(sequencingRule);
		 rules.add(routingRule);

		 fitnesses.add(indi.fitness);

		 evaluationModel.normObjective(fitnesses, rules, state);
	 }


	public void evaluate(EvolutionState state, Individual indi, int subpopulation, int threadnum) {

		//GPRule rule = new GPRule(RuleType.SEQUENCING, ((GPIndividual) indi).trees[0]);

		//modified by fzhang 23.5.2018  read two rules from one individual
		GPRule sequencingRule = new GPRule(RuleType.SEQUENCING, ((GPIndividual) indi).trees[0]);
		GPRule routingRule = new GPRule(RuleType.ROUTING, ((GPIndividual) indi).trees[1]);

		List rules = new ArrayList();
		List fitnesses = new ArrayList();

		//rules.add(rule);
		//modified by fzhang  to save two rules for evaluating from one individual
		rules.add(sequencingRule);
		rules.add(routingRule);

		fitnesses.add(indi.fitness);

		evaluationModel.evaluate(fitnesses, rules, state);

		indi.evaluated = true;
	}
}
