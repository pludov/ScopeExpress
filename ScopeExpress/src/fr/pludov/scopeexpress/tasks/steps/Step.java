package fr.pludov.scopeexpress.tasks.steps;

/**
 * Une �tape d�marre par un appel � .enter et se termine en emettant un message final (EndMessage)
 * Le parent peut controler le d�roulement de l'�tape en utilisant abortRequest.
 * Lorsqu'une �tape emet un message de Pause, le parent peut utiliser "resume" pour la relancer
 * 
 * enter ne peut pas �tre appell� une deuxi�me fois sans que l'�tape n'ait �mis un message de fin.
 */
public abstract class Step {
	private StepContainer parent;
	
	/** 
	 * D�marrage de l'�tape
	 * Doit r�initialiser tout �tat de l'instance
	 */
	public abstract void enter();

	/**
	 * Red�marre l'�tape suite � un message Pause
	 * Ne sera appell� que si le dernier message emis est un message de pause 
	 */
	public abstract void resume();
	
	/** Met fin � l'�tape en cours avec un message de fin pass� au parent. null indique le succ�s */
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
	 * Doit �tre appell� par un parent (pas d�clench� par un fils, sinon race condition) 
	 * Pas de garantie qu'elle soit honor�e (un succ�s est toujours possible...)
	 * L'appelant devra alors g�rer lui m�me la condition d'abort.
	 */
	public abstract void abortRequest(InterruptType type);		
}