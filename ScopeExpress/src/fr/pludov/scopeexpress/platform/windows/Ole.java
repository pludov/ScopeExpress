package fr.pludov.scopeexpress.platform.windows;

import org.apache.log4j.Logger;
import org.jawin.COMException;
import org.jawin.win32.Ole32;

/**
 * Classe statique pour partager l'initialisation de l'ole
 */
public final class Ole {
	private static final Logger logger = Logger.getLogger(Ole.class);
	private static boolean isOleInitialized;
	
	private Ole() {
	}

	

	public static synchronized void initOle() throws COMException
	{
		if (isOleInitialized) {
			return;
		}
		Ole32.CoInitialize();
		isOleInitialized = true;
	}
	
	public static synchronized void releaseOle()
	{
		if (!isOleInitialized) {
			return;
		}
		try {
			Ole32.CoUninitialize();
		} catch(COMException e) {
			logger.error("Unable to uninitialize Ole32", e);
		}
	}
	
	
}
