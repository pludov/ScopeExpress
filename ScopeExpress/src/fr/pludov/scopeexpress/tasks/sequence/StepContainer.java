package fr.pludov.scopeexpress.tasks.sequence;

import fr.pludov.scopeexpress.tasks.sequence.TaskAbstractSequence.*;

interface StepContainer
{
	/** Un fils est terminé. err contient un message eventuel (null == succès) */
	public void handleMessage(Step child, StepMessage err);
}