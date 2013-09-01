package fr.pludov.cadrage.ui.speech;

import fr.pludov.cadrage.utils.EndUserException;

public final class SpeakerProvider {

	private SpeakerProvider() {
		// TODO Auto-generated constructor stub
	}
	
	public static Speaker current = null;
	
	public static synchronized final Speaker getSpeaker() throws EndUserException
	{
		if (current == null) {
			try {
				current = new WinSpeaker();
			} catch(EndUserException e) {
				throw e;
			} catch(Throwable t) {
				throw new EndUserException("Erreur interne lors de l'initialisation du TTS", t);
			}
		}
		return current;
	}
}
