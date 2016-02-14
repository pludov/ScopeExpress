package fr.pludov.scopeexpress.tasks.steps;

import java.util.*;

public class If extends StepWithSimpleInterruptionHandler implements StepContainer {
	StepCondition condition;
	Step onTrue, onFalse;
	Boolean where;

	public If(StepCondition condition)
	{
		this.condition = condition;
	}

	@Override
	protected Collection<Step> getActiveSteps() {
		Step result = getActiveStep();
		if (result == null) return null;
		return Collections.singletonList(result);
	}

	public If Then(Step step)
	{
		if (onTrue != null) {
			throw new RuntimeException("Multiple then for one if");
		}
		onTrue = step;
		onTrue.setParent(this);
		return this;
	}

	public If Else(Step step)
	{
		if (onFalse != null) {
			throw new RuntimeException("Multiple false for one if");
		}
		onFalse = step;
		onFalse.setParent(this);

		return this;
	}

	private Step getActiveStep()
	{
		if (where == null) {
			return null;
		}
		return where ? onTrue : onFalse;
	}

	@Override
	public void enter() {
		super.enter();
		where = null;
		where = condition.evaluate();
		Step currentStep = getActiveStep();
		if (currentStep == null) {
			terminate(EndMessage.success());
		} else {
			currentStep.enter();
		}
	}
	
	@Override
	public String toString() {
		if (onFalse == null) {
			return "If " + Utils.lambdaToString(condition) + "\n" + Utils.indent(onTrue.toString()) + "\nEnd If\n";
		} else {
			return "If " + Utils.lambdaToString(condition) + "\n" + Utils.indent(onTrue.toString()) + "\nElse\n" + Utils.indent(onFalse.toString()) + "\nEnd If\n";
		}
	}
}