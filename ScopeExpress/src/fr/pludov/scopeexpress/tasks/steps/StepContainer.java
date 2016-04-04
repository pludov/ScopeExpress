package fr.pludov.scopeexpress.tasks.steps;

import fr.pludov.scopeexpress.ui.log.*;

public interface StepContainer
{
	/** Un fils est terminé. err contient un message eventuel (null == succès) */
	public void handleMessage(Step child, EndMessage err);
	
	public UILogger getUILogger();
}