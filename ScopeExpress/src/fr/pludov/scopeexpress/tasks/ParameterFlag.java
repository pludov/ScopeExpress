package fr.pludov.scopeexpress.tasks;

public enum ParameterFlag {
	Input,				// Positionn� en entr�
	Mandatory,			// Doit �tre positionn�
	Internal,			// Pas sauvegard� en cas d'interruption
	Output,				// Exclusif avec input
	PresentInConfig,		// La valeur par d�faut est pr�sente en configuration
	
	/** Ne pas retenir la pr�c�dente valeur (utile uniquement si !PresentInConfig) */
	DoNotPresentLasValue
}
