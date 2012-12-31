package fr.pludov.cadrage.async;

/**
 * Une resource peut �tre asynchrone.
 * Il n'y a pas de garantie d'appel depuis un thread particulier
 */
public interface WorkStepResource {

	// Lock la resource si elle est encore en m�moire. Sinon, retourne false
	// Le lock est un compteur qui est incr�ment�/d�cr�ment�
	boolean lock();
	
	// Produit la resource et la met dans l'�tat lock (FIXME: �a devrait donner un workstep.)
	void produce();
	
	// D�verouille la resource
	void unlock();
}
