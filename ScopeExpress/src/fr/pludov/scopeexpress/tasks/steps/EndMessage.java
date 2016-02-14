package fr.pludov.scopeexpress.tasks.steps;

/** 
 * Un Step se termine par un Message (null en cas de succ�s)
 */
public interface EndMessage
{

	static boolean isPausedMessage(EndMessage sm)
	{
		return ((sm instanceof StepInterruptedMessage) && ((StepInterruptedMessage)sm).type == InterruptType.Pause);
	}
	
	/** Valeur particuli�re indiquant le succ�s */
	static EndMessage success()
	{
		return null;
	}
}