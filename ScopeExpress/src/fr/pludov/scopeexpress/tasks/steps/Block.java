package fr.pludov.scopeexpress.tasks.steps;

import java.util.*;


/**
 * Un Block execute séquentiellement les Steps qui le constitue
 */
public class Block extends StepWithSimpleInterruptionHandler implements StepContainer
{
	Step [] steps;
	int currentPosition;
	
	
	public Block(Step ...steps) {
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
			terminate(EndMessage.success());
		} else {
			steps[currentPosition].enter();
		}
	}
	
	@Override
	public void handleMessage(Step child, EndMessage err) {
		if (interruptionHandler.handleChildMessage(child, err)) {
			return;
		}
		if (err == null) {
			if (interruptionHandler.doInterrupt(()->{handleMessage(child, err);})) {
				return;
			}
		
			currentPosition++;
			if (currentPosition >= steps.length) {
				terminate(EndMessage.success());
			} else {
				steps[currentPosition].enter();
			}
		} else {
			terminate(err);
		}
	}
	
	@Override
	public String toString() {
		return Utils.toBlockString("", Arrays.asList(steps));
	}
}