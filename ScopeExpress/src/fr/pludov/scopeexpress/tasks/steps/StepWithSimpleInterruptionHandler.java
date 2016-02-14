package fr.pludov.scopeexpress.tasks.steps;

import java.util.*;

/**
 * Pour l'utiliser, il faut:
 *   surcharger handleMessage pour faire passer par l'interruptionHandler
 */
abstract class StepWithSimpleInterruptionHandler extends Step {
	protected final StepInterruptionHandler interruptionHandler;

	StepWithSimpleInterruptionHandler()
	{
		this.interruptionHandler = new StepInterruptionHandler(this) {
			@Override
			Collection<Step> getActiveSteps() {
				Collection<Step> rslt = StepWithSimpleInterruptionHandler.this.getActiveSteps();
				if (rslt == null) {
					return Collections.emptyList();
				}
				return rslt;
			}
		};
	}
	
	protected abstract Collection<Step> getActiveSteps();
	
	@Override
	public void enter() {
		interruptionHandler.reset();
	}
	
	@Override
	public void abortRequest(InterruptType type) {
		interruptionHandler.interruptRequest(type);
	}

	@Override
	public void resume() {
		interruptionHandler.resume();
	}
	
	@Override
	void terminate(EndMessage stepError) {
		interruptionHandler.leave();
		super.terminate(stepError);
	}
	

	/** Implementation de base */
	public void handleMessage(Step child, EndMessage err) {
		if (interruptionHandler.handleChildMessage(child, err)) {
			return;
		}
		if (err == null && interruptionHandler.doInterrupt(()->{handleMessage(child, null);})) {
			return;
		}
		terminate(err);
	}
	
}