package fr.pludov.scopeexpress.tasks.steps;

public interface StepContainer
{
	/** Un fils est termin�. err contient un message eventuel (null == succ�s) */
	public void handleMessage(Step child, StepMessage err);
}