package fr.pludov.cadrage.async;

/**
 * Une resource peut être asynchrone.
 * Il n'y a pas de garantie d'appel depuis un thread particulier
 */
public interface WorkStepResource {

	// Lock la resource si elle est encore en mémoire. Sinon, retourne false
	// Le lock est un compteur qui est incrémenté/décrémenté
	boolean lock();
	
	// Produit la resource et la met dans l'état lock (FIXME: ça devrait donner un workstep.)
	void produce();
	
	// Déverouille la resource
	void unlock();
}
