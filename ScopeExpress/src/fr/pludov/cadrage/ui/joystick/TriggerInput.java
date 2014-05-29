package fr.pludov.cadrage.ui.joystick;

import fr.pludov.cadrage.utils.WeakListenerCollection;

public abstract class TriggerInput {
	final String uid;
	final WeakListenerCollection<TriggerInputListener> listeners = new WeakListenerCollection<TriggerInputListener>(TriggerInputListener.class);
	
	TriggerInput(String uid) {
		this.uid = uid;
	}
	
	public String getUid()
	{
		return uid;
	}
	
	@Override
	public String toString() {
		return uid;
	}

	/**
	 * Pour un bouton, retourne Boolean.TRUE ou Boolean.FALSE
	 * retourne NULL si pas connecté
	 * @return
	 */
	public abstract Object getStatus();
}
