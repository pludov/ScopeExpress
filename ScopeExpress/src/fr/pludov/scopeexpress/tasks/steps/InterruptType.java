package fr.pludov.scopeexpress.tasks.steps;

/**
 * Type d'interruption d'�tape
 * On peut demander pause puis Abort.
 * Par contre une demande de Pause apr�s un Abort sera ignor�e
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