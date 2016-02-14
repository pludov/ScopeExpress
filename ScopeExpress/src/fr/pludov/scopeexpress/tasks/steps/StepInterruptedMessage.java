package fr.pludov.scopeexpress.tasks.steps;

/** 
 * Ce message est émit par un step qui s'est interrompu,
 * ou alors qui s'est mis en pause.
 * 
 * Un step en pause peut être redémarré par un appel à resume
 */
public class StepInterruptedMessage implements StepMessage
{
	InterruptType type;
	
	public StepInterruptedMessage(InterruptType requested) {
		this.type = requested;
	}
}