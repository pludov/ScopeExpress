package fr.pludov.scopeexpress.openphd;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.google.gson.GsonBuilder;

public abstract class OpenPhdQuery {
	public static final Logger logger = Logger.getLogger(OpenPhdQuery.class);

	public static final String GuideStep = "GuideStep";
	public static final String SettleDone = "SettleDone";

	public static final String CalibrationFailed = "CalibrationFailed";

	OpenPhdConnection phd;
	int uid;
	String json;
	Map<String, Object> content;
	
	public OpenPhdQuery() {
		content = new HashMap<>();
	}
	
	/** Appellé uniquement dnas le thread swing*/
	public abstract void onFailure();
	
	/** Appellé uniquement dnas le thread swing*/
	public abstract void onReply(Map<?, ?> message); 
	
	public void send(OpenPhdDevice phd)
	{
		this.phd = phd.establishedConnection;
		if (this.phd == null) {
			onFailure();
		} else {
			this.uid = (this.phd.nextOrderId++);
			content.put("id", uid);
	
			json = new GsonBuilder().create().toJson(content);
			content = null;
			logger.info("Sending: " + json);
			this.phd.sendQuery(this);
		}
	}
	
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

	public void put(String string, Object object) {
		content.put(string, object);
	}

	public static String getErrorMessage(Map<?, ?> object)
	{
		Object o = object.get("Error");
		if (o == null) {
			return null;
		}
		return Objects.toString(o);
	}
}
