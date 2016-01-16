package fr.pludov.scopeexpress.tasks;

// Input, Output, PresentInConfig, PresentInConfigForEachUsage et DoNotPresentLastValue sont exclusif
public enum ParameterFlag {
	Input,				// Positionné en entré
	Mandatory,			// Doit être positionné
	Internal,			// Pas sauvegardé en cas d'interruption
	Output,				// Exclusif avec input
	PresentInConfig,	// La valeur par défaut est présente en configuration
	PresentInConfigForEachUsage, // La valeur par défaut est présente en configuration, mais à chaque niveau de la tache
	
	/** Ne pas retenir la précédente valeur (utile uniquement si !PresentInConfig) */
	DoNotPresentLasValue
}
