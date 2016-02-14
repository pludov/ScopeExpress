package fr.pludov.scopeexpress.tasks.steps;

import java.util.*;

public class While extends StepWithSimpleInterruptionHandler implements StepContainer {
	
	StepCondition condition;
	Step block;
	
	public While(StepCondition condition)
	{
		this.condition = condition;
	}
	
	@Override
	protected Collection<Step> getActiveSteps() {
		return block != null ? Collections.singletonList(block) : null;
	}

	public Step Do(Step stepSequence) {
		if (block != null) {
			throw new RuntimeException("Multiple do");
		}
		this.block = stepSequence;
		this.block.setParent(this);
		return this;
	}
	 

	@Override
	public void enter() {
		super.enter();
		if (condition.evaluate() && this.block != null) {
			this.block.enter();
		} else {
			leave();
		}
	}
	
	@Override
	public void handleMessage(Step child, StepMessage err) {
		if (err == null) {
			// Dans ce cas, on boucle !
			if (interruptionHandler.doInterrupt(() -> {handleMessage(child, null);})) {
				return;
			}
			enter();	
		} else {
			super.handleMessage(child, err);
		}
	}
	
}