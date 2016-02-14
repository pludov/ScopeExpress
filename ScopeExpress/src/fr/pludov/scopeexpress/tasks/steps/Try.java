package fr.pludov.scopeexpress.tasks.steps;

import java.util.*;
import java.util.function.*;

import fr.pludov.scopeexpress.utils.*;

/** Les bloques catch ne sont pas interruptibles */
public class Try extends Step implements StepContainer
{
	final Step main;
	final List<Couple<Function<StepMessage, Boolean>, Step>> catches = new ArrayList<>();
	
	InterruptType pendingInterruption;
	Runnable onResume;
	Step current;
	
	public Try(Step main)
	{
		this.main = main;
		this.main.setParent(this);
	}

	public Step Catch(Function<StepMessage, Boolean> filter, Step immediate) {
		catches.add(new Couple<>(filter, immediate));
		immediate.setParent(this);
		return this;
	}
	
	@Override
	public void enter() {
		current = null;
		pendingInterruption = null;
		current = main;
		main.enter();
	}
	
	@Override
	public void handleMessage(Step child, StepMessage err) {
		assert(current == child);
		if (err == null) {
			if (pendingInterruption != null) {
				onResume = () -> {handleMessage(child, null);};
				StepInterruptedMessage stepError = new StepInterruptedMessage(pendingInterruption);
				pendingInterruption = null;
				throwError(stepError);
				return;
			}
			current = null;
			leave();
		} else {
			if (child != main) {
				throwError(err);
			} else {
				if (StepMessage.isPausedMessage(err) && pendingInterruption == InterruptType.Pause) {
					pendingInterruption = null;
					onResume = ()->{main.resume();};
					throwError(err);
					return;
				}
				// Le catch
				for(Couple<Function<StepMessage, Boolean>, Step> catchItem : catches)
				{
					if (catchItem.getA().apply(err)) {
						current = catchItem.getB();
						current.enter();
						return;
					}
				}
				throwError(err);
			}
		}
	}
	
	@Override
	public void resume() {
		onResume.run();
	}
	
	@Override
	public void abortRequest(InterruptType pendingInterruption) {
		if (this.pendingInterruption == null || this.pendingInterruption.ordinal() < pendingInterruption.ordinal())
		{
			this.pendingInterruption = pendingInterruption;
			if (current == main) {
				current.abortRequest(pendingInterruption);
			}
		}
	}
	
}