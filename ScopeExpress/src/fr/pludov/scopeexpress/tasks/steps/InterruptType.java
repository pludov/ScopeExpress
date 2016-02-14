package fr.pludov.scopeexpress.tasks.steps;

/**
 * Type d'interruption d'étape
 * On peut demander pause puis Abort.
 * Par contre une demande de Pause après un Abort sera ignorée
 */
public enum InterruptType {
	Pause,
	Abort;

	/** true si a < b */
	public static boolean lower(InterruptType a, InterruptType b) {
		if (a == null) {
			return (b != null);
		}
		if (b == null) {
			return false;
		}
		return a.ordinal() < b.ordinal();
	}
}