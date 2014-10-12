package fr.pludov.scopeexpress.ui.speech;

import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;

/**
 * Point d'entrée pour la sortie de texte.
 */
public interface Speaker {

	public void enqueue(String text);
	public void clearQueue();
}
