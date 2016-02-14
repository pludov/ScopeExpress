package fr.pludov.scopeexpress.tasks.steps;

/** 
 * Execute du code immédiatement (donc du code non bloquant!).
 * Ne peut être interrompu ou mis en pause.
 */
public class Immediate extends Step
{
	final Runnable runnable;
	
	public Immediate(Runnable runnable)
	{
		this.runnable = runnable;
	}

	@Override
	public void enter() {
		runnable.run();
		terminate(EndMessage.success());
	}

	@Override
	public void resume() {
		throw new RuntimeException("Cannot be suspended");
	}
	
	@Override
	public void abortRequest(InterruptType type) {
		// Rien...
	}
}