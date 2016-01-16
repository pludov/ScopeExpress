package fr.pludov.scopeexpress.tasks;

// Input, Output, PresentInConfig, PresentInConfigForEachUsage et DoNotPresentLastValue sont exclusif
public enum ParameterFlag {
	Input,				// Positionn� en entr�
	Mandatory,			// Doit �tre positionn�
	Internal,			// Pas sauvegard� en cas d'interruption
	Output,				// Exclusif avec input
	PresentInConfig,	// La valeur par d�faut est pr�sente en configuration
	PresentInConfigForEachUsage, // La valeur par d�faut est pr�sente en configuration, mais � chaque niveau de la tache
	
	/** Ne pas retenir la pr�c�dente valeur (utile uniquement si !PresentInConfig) */
	DoNotPresentLasValue
}
