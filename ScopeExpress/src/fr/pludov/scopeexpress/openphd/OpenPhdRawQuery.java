package fr.pludov.scopeexpress.openphd;

import java.util.*;

import org.apache.log4j.*;

import com.google.gson.*;

public abstract class OpenPhdRawQuery {
	public static final Logger logger = Logger.getLogger(OpenPhdRawQuery.class);

	public static final String GuideStep = "GuideStep";
	public static final String SettleDone = "SettleDone";

	public static final String CalibrationFailed = "CalibrationFailed";

	OpenPhdConnection phd;
	int uid;
	String json;
	
	public OpenPhdRawQuery() {
	}
	
	/** Appellé uniquement dnas le thread swing*/
	public abstract void onFailure();
	
	/** Appellé uniquement dnas le thread swing*/
	public abstract void onReply(JsonObject message); 
	
	public void send(OpenPhdDevice phd)
	{
		this.phd = phd.establishedConnection;
		if (this.phd == null) {
			onFailure();
		} else {
			this.uid = (++this.phd.nextOrderId);
			try {
				json = buildContent(uid);
			} catch(Throwable t) {
				logger.warn("Failed to build phd query", t);
				onFailure();
				return;
			}
			logger.info("Sending: " + json);
			this.phd.sendQuery(this);
		}
	}
	
	protected abstract String buildContent(int uid);
	
	/** 
	 * Abandonne l'ordre. Pas de garantie qu'il ne sera pas exécuté, par contre, on est sûr de ne pas avoir de notification
	 * Retourne true si on est sûr que l'ordre n'a pas été pris en compte
	 */
	public boolean cancel()
	{
		return phd.cancelQuery(this);
	}

	String getJson() {
		return json;
	}

	public static String getErrorMessage(JsonObject  object)
	{
		if (!object.has("error")) {
			if (!object.has("Error")) {
				return null;
			}
			return Objects.toString(object.get("Error"));
		}
		
		return Objects.toString(object.get("error"));
	}
}
