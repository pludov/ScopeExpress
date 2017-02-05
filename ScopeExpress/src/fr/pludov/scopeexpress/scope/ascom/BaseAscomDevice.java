package fr.pludov.scopeexpress.scope.ascom;

import java.util.*;

import org.apache.log4j.*;
import org.jawin.*;

import fr.pludov.scopeexpress.utils.*;

public class BaseAscomDevice extends WorkThread {
	public static final Logger logger = Logger.getLogger(BaseAscomDevice.class);

	DispatchPtr devicePtr;

	/**
	 *  Liste des propriétés qui ont déclenché une erreur en lecture
	 *  (pour ne pas boucler sur les erreurs. Réinitialisé à la connection)
	 */
	private Map<String, Boolean> cameraSupportedProperties = new HashMap<>();
	

	void clearSupportedProperties()
	{
		cameraSupportedProperties.clear();
	}
	
	<T> T getCapacity(String name, T defaultValue)
	{
		Boolean supported = cameraSupportedProperties.get(name);
		if (supported != null && !supported.booleanValue()) {
			return defaultValue;
		}
		
		try {
			T result = (T)devicePtr.get(name);
			if (supported == null) {
				cameraSupportedProperties.put(name, Boolean.TRUE);
			}
			return result;
		} catch(COMException  t) {
			if (supported == null) {
				logger.info("Property not supported: " + name, t);
				cameraSupportedProperties.put(name, Boolean.FALSE);
			} else {
				logger.error("Error getting property: " + name, t);
			}
			return defaultValue;
		}
	}

}
