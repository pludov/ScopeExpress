package fr.pludov.scopeexpress.tasks.steps;

import fr.pludov.scopeexpress.tasks.*;

/** Call back utilis� en fin d'execution de tache. Permet de sortir un r�sultat de la tache */
public interface SubTaskStatusChecker
{
	void evaluate(BaseTask bt);
}