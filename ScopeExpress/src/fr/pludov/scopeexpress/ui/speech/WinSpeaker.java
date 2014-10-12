package fr.pludov.scopeexpress.ui.speech;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jawin.COMException;
import org.jawin.DispatchPtr;

import fr.pludov.scopeexpress.platform.windows.Ole;
import fr.pludov.scopeexpress.utils.EndUserException;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;

/**
 * Pour la synthese vocale en français: http://www.zebulon.fr/astuces/200-synthese-vocale-windows-en-francais.html
 *
 */
class WinSpeaker implements Speaker {
	private static final Logger logger = Logger.getLogger(WinSpeaker.class);
	
	final DispatchPtr spVoice;
	
	static final Set<Integer> frLanguages = new HashSet<Integer>(Arrays.asList(0x040C, 0x080C, 0x0C0C, 0x100C, 0x140C));
	static final Set<Integer> enLanguages = new HashSet<Integer>(Arrays.asList(0x0009, 0x0409, 0x0809, 0x0C09, 0x1009, 0x1409, 0x1809, 0x1C09, 0x2009, 0x2809, 0x2C09)); 
	static final Set<Integer> languages = new HashSet<Integer>();
	
	WinSpeaker() throws EndUserException {
		try {
			Ole.initOle();
			
			// Vérifier que la langue par défaut est bonne.
			spVoice = new DispatchPtr("SAPI.SpVoice");
			DispatchPtr voice = (DispatchPtr)spVoice.get("Voice");
			List<Integer> codeLang = getWinLanguageCode(voice);
			if (!isLanguageCompatible(frLanguages, codeLang)) {
				logger.warn("Language of default voice is not the expected language. Searching for a good default voice");
				
				DispatchPtr voiceList = (DispatchPtr)spVoice.invoke("GetVoices");
				Number voiceCount = (Number)voiceList.get("Count");
				boolean languageFound = false;
				for(int i = 0; i < voiceCount.intValue(); ++i)
				{
					voice = (DispatchPtr)voiceList.invoke("Item", i);
					codeLang = getWinLanguageCode(voice);
					if (isLanguageCompatible(frLanguages, codeLang)) {
						languageFound = true;
						break;
					}
				}
				if (!languageFound) {
					throw new EndUserException("Pas de langue française installée. Pour l'installer, voir http://www.zebulon.fr/astuces/200-synthese-vocale-windows-en-francais.html");
				}
				spVoice.put("Voice", voice);
			}
			
			
			logger.info("language is: " + (String)voice.invoke("GetDescription"));
		} catch (COMException e) {
			logger.error("COMException occured during tts initialisation", e);
			throw new EndUserException("Erreur fatale lors de l'initialisation du moteur de synthèse vocale - vérifiez que le composant windows est bien installé", e);
		}
		
	}
	
	private static boolean isLanguageCompatible(Set<Integer> wanted, List<Integer> current)
	{
		for(Integer a : current)
		{
			if (wanted.contains(a)) return true;
		}
		return false;
	}
	
	private static List<Integer> getWinLanguageCode(DispatchPtr voice) throws COMException
	{
		String languageList = (String)voice.invoke("GetAttribute", "language");
		List<Integer> result = new ArrayList<Integer>();
		for(String language : languageList.split(";")) {
			try {
				int codeLang = Integer.parseInt(language, 16);
				result.add(codeLang);
			} catch(NumberFormatException e) {
				logger.warn("Invalid language code: " + language);
				logger.info("Language with invalid code is :" + (String)voice.invoke("GetDescription"));
			}
		}
		return result;
	}

	@Override
	public void enqueue(String text) {
		try {
			logger.info("Saying: " + text);
			// 1 = SVSFlagsAsync
			spVoice.invoke("speak", text, 1);
		} catch (COMException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void clearQueue() {
		// TODO Auto-generated method stub
		
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
