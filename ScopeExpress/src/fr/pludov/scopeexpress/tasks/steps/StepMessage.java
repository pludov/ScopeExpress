package fr.pludov.scopeexpress.tasks.steps;

public interface StepMessage
{

	static boolean isPausedMessage(StepMessage sm)
	{
		return ((sm instanceof StepInterruptedMessage) && ((StepInterruptedMessage)sm).type == InterruptType.Pause);
	}
	
}