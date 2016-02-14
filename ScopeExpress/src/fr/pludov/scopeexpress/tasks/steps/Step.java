package fr.pludov.scopeexpress.tasks.steps;

/**
 * Une étape démarre par un appel à .enter et se termine en emettant un message final (EndMessage)
 * Le parent peut controler le déroulement de l'étape en utilisant abortRequest.
 * Lorsqu'une étape emet un message de Pause, le parent peut utiliser "resume" pour la relancer
 * 
 * enter ne peut pas être appellé une deuxième fois sans que l'étape n'ait émis un message de fin.
 */
public abstract class Step {
	private StepContainer parent;
	
	/** 
	 * Démarrage de l'étape
	 * Doit réinitialiser tout état de l'instance
	 */
	public abstract void enter();

	/**
	 * Redémarre l'étape suite à un message Pause
	 * Ne sera appellé que si le dernier message emis est un message de pause 
	 */
	public abstract void resume();
	
	/** Met fin à l'étape en cours avec un message de fin passé au parent. null indique le succès */
	void terminate(EndMessage stepError)
	{
		parent.handleMessage(this, stepError);
	}

	public final void setParent(StepContainer stepSequence) {
		if (parent != null) {
			throw new RuntimeException("Multiple parent not allowed");
		}
		this.parent = stepSequence;
	}

	/** 
	 * Demande une terminaison.
	 * Doit être appellé par un parent (pas déclenché par un fils, sinon race condition) 
	 * Pas de garantie qu'elle soit honorée (un succès est toujours possible...)
	 * L'appelant devra alors gérer lui même la condition d'abort.
	 */
	public abstract void abortRequest(InterruptType type);		
}