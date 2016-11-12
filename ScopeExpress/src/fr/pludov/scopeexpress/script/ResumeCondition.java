package fr.pludov.scopeexpress.script;

import fr.pludov.scopeexpress.script.Task.*;

public abstract class ResumeCondition {
	static final Object Pending = new Object() {
		@Override
		public String toString() {return "(pending)";};
	};
	
	
	JSTask.StackEntry target;
	
	// Retourne la valeur a retourner. La valeur spéciale StillPending
	abstract Object check();
	
	final void refresh()
	{
		if (target == null) return;
		
		assert(target.getTask().currentStackEntry == target);
		assert(target.getTask().getStatus() == Status.Blocked);
		assert(target.blockingCondition == this);
		
		Throwable error = null;
		Object result = null;
		try {
			result = check();
			if (result == Pending) {
				return;
			}
		} catch(Throwable t) {
			error = t;
		}
		target.blockingCondition = null;
		target.resumeResult = result;
		// FIXME: on ne sait pas la passer ??? target.resumeError = error;
		target.getTask().setStatus(Status.Runnable);
		this.target = null;
		
	}
	

}
