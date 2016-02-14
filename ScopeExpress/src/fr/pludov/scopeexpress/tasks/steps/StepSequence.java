package fr.pludov.scopeexpress.tasks.steps;

import java.util.*;

public class StepSequence extends StepWithSimpleInterruptionHandler implements StepContainer
{
	Step [] steps;
	int currentPosition;
	
	
	public StepSequence(Step ...steps) {
		this.steps = steps;
		for(int i = 0; i < steps.length; ++i)
		{
			steps[i].setParent(this);
		}
	}
	
	@Override
	protected Collection<Step> getActiveSteps() {
		if (currentPosition >= steps.length) {
			return null;
		}
		return Collections.singleton(steps[currentPosition]);
	}
	
	
	@Override
	public void enter() {
		super.enter();
		currentPosition = 0;
		if (currentPosition >= steps.length) {
			leave();
		} else {
			steps[currentPosition].enter();
		}
	}
	
	@Override
	public void handleMessage(Step child, StepMessage err) {
		if (interruptionHandler.handleChildMessage(child, err)) {
			return;
		}
		if (err == null) {
			if (interruptionHandler.doInterrupt(()->{handleMessage(child, err);})) {
				return;
			}
		
			currentPosition++;
			if (currentPosition >= steps.length) {
				leave();
			} else {
				steps[currentPosition].enter();
			}
		} else {
			throwError(err);
		}
	}
}