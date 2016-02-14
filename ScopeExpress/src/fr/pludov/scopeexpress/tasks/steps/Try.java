package fr.pludov.scopeexpress.tasks.steps;

import java.util.*;
import java.util.function.*;

import fr.pludov.scopeexpress.utils.*;

/** 
 * Execute un bloc en réagissant au message d'erreur éventuels 
 * Les bloques catch ne sont pas interruptibles 
 */
public class Try extends Step implements StepContainer
{
	final Step main;
	final List<Couple<Function<EndMessage, Boolean>, Step>> catches = new ArrayList<>();
	
	InterruptType pendingInterruption;
	Runnable onResume;
	Step current;
	
	public Try(Step main)
	{
		this.main = main;
		this.main.setParent(this);
	}

	public Step Catch(Function<EndMessage, Boolean> filter, Step immediate) {
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
	public void handleMessage(Step child, EndMessage err) {
		assert(current == child);
		if (err == null) {
			if (pendingInterruption != null) {
				onResume = () -> {handleMessage(child, null);};
				StepInterruptedMessage stepError = new StepInterruptedMessage(pendingInterruption);
				pendingInterruption = null;
				terminate(stepError);
				return;
			}
			current = null;
			terminate(EndMessage.success());
		} else {
			if (child != main) {
				terminate(err);
			} else {
				if (EndMessage.isPausedMessage(err) && pendingInterruption == InterruptType.Pause) {
					pendingInterruption = null;
					onResume = ()->{main.resume();};
					terminate(err);
					return;
				}
				// Le catch
				for(Couple<Function<EndMessage, Boolean>, Step> catchItem : catches)
				{
					if (catchItem.getA().apply(err)) {
						current = catchItem.getB();
						current.enter();
						return;
					}
				}
				terminate(err);
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

	
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("Try\n");
		result.append(Utils.indent(main.toString()));
		
		for(Couple<Function<EndMessage, Boolean>, Step> item : this.catches)
		{
			result.append("Catch ");
			result.append(Utils.lambdaToString(item.getA()));
			result.append("\n");
			result.append(Utils.indent(item.getB().toString()));
			result.append("\n");
		}
		result.append("End Try\n");
		
		return result.toString();
	}
}