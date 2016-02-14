package fr.pludov.scopeexpress.tasks.steps;

import java.util.*;

/**
 * Logique de traitement des interruptions qui prend en compte l'arret et la relance des fils
 * 
 * Elle repose sur getActiveChilds() qui doit retourner les fils en cours
 * 
 * 
 */
abstract class StepInterruptionHandler
{
	class InterruptionStatus
	{
		boolean done;
		InterruptType requested;
	}
	
	final Step step;
	
	InterruptType requested;
	Object requestIdentifier;
	Object resumeIdentifier;
	Runnable restart;
	IdentityHashMap<Step, InterruptionStatus> childStatus;
	
	StepInterruptionHandler(Step parent)
	{
		this.step = parent;
	}
	
	abstract Collection<Step> getActiveSteps();
	
	
	void reset()
	{
		requested = null;
		requestIdentifier = null;
		resumeIdentifier = null;
		childStatus = null;
		restart = null;
	}
	
	/**
	 * Traite une demande d'interruption.
	 * Elle est forwardée au fils
	 * @param it
	 */
	void interruptRequest(InterruptType it)
	{
		if (requested == null || requested.ordinal() < it.ordinal()) {
			requested = it;
			requestIdentifier = new Object();
			finish();
		}
	}
	
	InterruptionStatus getChildStatus(Step child)
	{
		if (childStatus == null) {
			childStatus = new IdentityHashMap<>();
		}
		InterruptionStatus result = childStatus.get(child);
		if (result == null) {
			result = new InterruptionStatus();
			childStatus.put(child, result);
		}
		return result;
	}

	void finish()
	{
		Object requestToStop = requestIdentifier;
		boolean restart = true;
	CheckActiveLoop:
		while(requestIdentifier == requestToStop) {
			restart = false;
			// Faire passer la demande à tous les fils actifs (non terminés vu du parents, mais peut être en pause...)
			Collection<Step> activeSteps = getActiveSteps();
			boolean somethingNotDone = false;
			for(Step s : activeSteps)
			{
				InterruptionStatus status = getChildStatus(s);
				if (status.done) {
					continue;
				}
				somethingNotDone = true;
				if (InterruptType.lower(status.requested, requested))
				{
					status.requested = requested;
					s.abortRequest(requested);
					continue CheckActiveLoop;
				}
			}
			
			if (somethingNotDone) {
				return;
			}
			
			// On arrive là quand tout le monde est terminé.
			// Dans ce cas, on peut emetrre une message comme quoi on peut redémarrer.
			
			if (requested == InterruptType.Pause) {
				this.restart = ()->{
					Object resumeIdentifierValue = new Object();
					resumeIdentifier = resumeIdentifierValue;
					// Comment on fait pour savoir que les restart doivent continuer en cas d'interruption ?
					for(Step step : activeSteps) {
						if (resumeIdentifier != resumeIdentifierValue) {
							return;
						}
						step.resume();
					}
					if (resumeIdentifier != resumeIdentifierValue) {
						return;
					}
					// Tous le monde a redémarré ?
					resumeIdentifier = null;
				};
			}
			StepInterruptedMessage stepError = new StepInterruptedMessage(requested);
			requested = null;
			requestIdentifier = null;
			step.throwError(stepError);
			return;
		}
	}
	
	/** Interrompt l'action de resume éventuellement en cours */
	void leave()
	{
		resumeIdentifier = null;
		childStatus = null;
	}
	
	void resume()
	{
		Runnable todo = this.restart;
		this.restart = null;
		todo.run();
	}
	
	/** 
	 * Filtres les messages relatifs aux fils
	 * On suppose qu'on ne reçoit que des messages pour des fils actifs 
	 */
	boolean handleChildMessage(Step step, StepMessage msg)
	{
		if ((requested != null) && (msg instanceof StepInterruptedMessage))
		{
			StepInterruptedMessage smi = (StepInterruptedMessage) msg;
			if (smi.type == requested) {
				// Il est dans le bon type... On prend le message et on l'ignore
				getChildStatus(step).done = true;
				finish();
				return true;
			}
		}
		return false;
	}

	/** 
	 * Traite une interruption en attente. Celà suppose qu'il n'y a plus de fils actifs 
	 * Si retourne true, la tache est arrété (il ne doit pas y avoir de code après) 
	 */
	public boolean doInterrupt(Runnable onRestart) {
		if (requested != null) {
			StepInterruptedMessage sim = new StepInterruptedMessage(requested);
			if (requested == InterruptType.Pause) {
				restart = onRestart;
			} else {
				restart = null;
			}
			requested = null;
			
			step.throwError(sim);
			return true;
		}
		return false;
	}
}