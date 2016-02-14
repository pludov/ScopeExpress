package fr.pludov.scopeexpress.tasks.steps;

/** 
 * Un Step se termine par un Message (null en cas de succès)
 */
public interface EndMessage
{

	static boolean isPausedMessage(EndMessage sm)
	{
		return ((sm instanceof StepInterruptedMessage) && ((StepInterruptedMessage)sm).type == InterruptType.Pause);
	}
	
	/** Valeur particulière indiquant le succès */
	static EndMessage success()
	{
		return null;
	}
}