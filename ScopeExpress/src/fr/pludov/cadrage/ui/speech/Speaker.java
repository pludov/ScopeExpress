package fr.pludov.cadrage.ui.speech;

import fr.pludov.cadrage.focus.MosaicListener;
import fr.pludov.cadrage.utils.WeakListenerCollection;

/**
 * Point d'entrée pour la sortie de texte.
 */
public interface Speaker {

	public void enqueue(String text);
	public void clearQueue();
}
