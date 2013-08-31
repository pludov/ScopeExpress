package fr.pludov.cadrage.ui.speech;

public final class SpeakerProvider {

	private SpeakerProvider() {
		// TODO Auto-generated constructor stub
	}
	
	public static Speaker current = null;
	
	public static synchronized final Speaker getSpeaker()
	{
		if (current == null) {
			try {
				current = new WinSpeaker();
			} catch(Throwable t) {
				t.printStackTrace();
				
			}
		}
		return current;
	}
}
