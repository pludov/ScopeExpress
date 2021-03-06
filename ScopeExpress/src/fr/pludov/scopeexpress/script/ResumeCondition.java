package fr.pludov.scopeexpress.script;

import java.io.*;

import fr.pludov.scopeexpress.script.Task.*;

public abstract class ResumeCondition implements Closeable {
	
	
	
	JSTask.StackEntry target;
	
	// Retourne la valeur a retourner. Pas le droit � error
	abstract ConditionMeet check();
	
	final void refresh()
	{
		if (target == null) return;
		
		assert(target.getTask().currentStackEntry == target);
		assert(target.getTask().getStatus() == Status.Blocked);
		assert(target.blockingCondition == this);
		
		ConditionMeet result = null;
		result = check();
		if (result == null) {
			return;
		}

		target.blockingCondition = null;
		target.resumeResult = result;
		target.getTask().setStatus(Status.Runnable);
		close();
		this.target = null;
		
	}
	
	@Override
	public void close() {
	}
}
