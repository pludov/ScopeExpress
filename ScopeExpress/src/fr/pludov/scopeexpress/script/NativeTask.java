package fr.pludov.scopeexpress.script;

import fr.pludov.scopeexpress.utils.*;

public abstract class NativeTask extends fr.pludov.scopeexpress.script.Task {

	protected WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	private boolean initialised;
	
	public NativeTask() {
		super(TaskGroup.getCurrent());
		
		// Relacher le listenerOwner à la fin pour liberer les resources
		onDone(()->{ listenerOwner = null; });
		setStatus(Status.Runnable);
	}

	@Override
	void advance() {
		if (!initialised) {
			try {
				initialised = true;
				init();
			} catch(Throwable t) {
				error = t;
				result = t;
				setStatus(Status.Done);
			}
			assert(getStatus() != Status.Runnable);
		}
	}
	
	protected void failed(Throwable t) {
		error = t;
		result = null;
		setStatus(Status.Done);
	}

	protected void failed(String string) {
		failed(new Exception(string));
	}

	
	protected void done(Object r) {
		error = null;
		result = r;
		setStatus(Status.Done);
	}
	
	protected abstract void init() throws Throwable;
}
