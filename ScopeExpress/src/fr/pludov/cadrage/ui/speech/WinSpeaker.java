package fr.pludov.cadrage.ui.speech;

import org.apache.log4j.Logger;
import org.jawin.COMException;
import org.jawin.DispatchPtr;

import fr.pludov.cadrage.platform.windows.Ole;
import fr.pludov.cadrage.utils.WeakListenerCollection;

/**
 * Pour la synthese vocale en français: http://www.zebulon.fr/astuces/200-synthese-vocale-windows-en-francais.html
 *
 */
class WinSpeaker implements Speaker {
	private static final Logger logger = Logger.getLogger(WinSpeaker.class);
	
	final WeakListenerCollection<SpeakerListener> listeners;
	final DispatchPtr spVoice;
	
	WinSpeaker() throws COMException {
		Ole.initOle();
		listeners = new WeakListenerCollection<SpeakerListener>(SpeakerListener.class);
		spVoice = new DispatchPtr("SAPI.SpVoice");
		Object language = spVoice.get("Voice");
//		DispatchPtr languages = (DispatchPtr)spVoice.invoke("GetVoices");
	}

	
	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void enqueue(String text) {
		try {
			logger.info("Saying: " + text);
			//if (text.equals("")) {
				spVoice.invoke("speak", text);
			//}
		} catch (COMException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void clearQueue() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public WeakListenerCollection<SpeakerListener> getListeners() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String[] args) {
		Speaker sp;
		try {
			sp = new WinSpeaker();
			sp.enqueue("Bonjour");

			Thread.sleep(6000);

		} catch(Throwable t) {
			// TODO Auto-generated catch block
			t.printStackTrace();
		}
	}
}
