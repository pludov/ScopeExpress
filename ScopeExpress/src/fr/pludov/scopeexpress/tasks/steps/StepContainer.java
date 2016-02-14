package fr.pludov.scopeexpress.tasks.steps;

public interface StepContainer
{
	/** Un fils est terminé. err contient un message eventuel (null == succès) */
	public void handleMessage(Step child, StepMessage err);
}