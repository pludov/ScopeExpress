package fr.pludov.scopeexpress.ui;

import fr.pludov.scopeexpress.utils.WeakListenerCollection;

public abstract class ScriptTest {
	public WeakListenerCollection<ScriptTestListener> listeners = new WeakListenerCollection<ScriptTestListener>(ScriptTestListener.class);
	
	public abstract void start();
	public abstract void step();

	private boolean finished;
	
	public boolean isFinished()
	{
		return finished;
	}
	
	void done()
	{
		this.finished = true;
		listeners.getTarget().testDone();
	}
	
}
