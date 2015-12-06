package fr.pludov.scopeexpress.tasks;

public enum ParameterFlag {
	Input,				// Positionné en entré
	Mandatory,			// Doit être positionné
	Internal,			// Pas sauvegardé en cas d'interruption
	Output,				// Exclusif avec input
	PresentInConfig,		// La valeur par défaut est présente en configuration
	
	/** Ne pas retenir la précédente valeur (utile uniquement si !PresentInConfig) */
	DoNotPresentLasValue
}
