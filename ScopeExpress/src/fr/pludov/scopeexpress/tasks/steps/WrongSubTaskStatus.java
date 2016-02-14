package fr.pludov.scopeexpress.tasks.steps;

import fr.pludov.scopeexpress.tasks.*;

public class WrongSubTaskStatus implements EndMessage
{
	final String message;
	final IStatus status;
	
	public WrongSubTaskStatus(BaseTask bt)
	{
		this.status = bt.getStatus();
		message = "Tache " + bt.getTitle() + " : " + bt.getTitle();
	}
	
	@Override
	public String toString() {
		return message;
	}

	public IStatus getStatus() {
		return status;
	}
}