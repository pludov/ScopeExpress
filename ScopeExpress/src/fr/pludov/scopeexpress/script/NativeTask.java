package fr.pludov.scopeexpress.script;

import java.util.*;

import fr.pludov.scopeexpress.utils.*;

public abstract class NativeTask extends Task {

	protected WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	private boolean initialised;
	private List<Object> produced;
	
	public NativeTask() {
		super(TaskGroup.getCurrent());
		
		// Relacher le listenerOwner à la fin pour liberer les resources
		onDone(()->{
			listenerOwner.kill();
			listenerOwner = null;
		});
		setStatus(Status.Runnable);
	}
	
	@Override
	void cancel() {
		setStatus(Status.Done);
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
	
	protected void produce(Object r) {
		assert(r != null);
		assert(getStatus() != Status.Done);
		if (produced == null) {
			produced = new LinkedList<>();
		}
		produced.add(r);
		for(Runnable r2 : new ArrayList<>(onProduced)) {
			r2.run();
		}
	}
	
	@Override
	public Object readProduced() {
		if (produced == null || produced.isEmpty()) return null;
		return produced.remove(0);
	}
	
	protected abstract void init() throws Throwable;
}
