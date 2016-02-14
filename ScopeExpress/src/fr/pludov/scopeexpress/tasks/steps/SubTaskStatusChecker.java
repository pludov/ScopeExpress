package fr.pludov.scopeexpress.tasks.steps;

import fr.pludov.scopeexpress.tasks.*;

/** Call back utilisé en fin d'execution de tache. Permet de sortir un résultat de la tache */
public interface SubTaskStatusChecker
{
	void evaluate(BaseTask bt);
}