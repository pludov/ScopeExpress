package fr.pludov.scopeexpress.openphd;

import java.util.*;

import org.apache.log4j.*;

import com.google.gson.*;

public abstract class OpenPhdQuery extends OpenPhdRawQuery {
	public static final Logger logger = Logger.getLogger(OpenPhdQuery.class);

	Map<String, Object> content;
	
	public OpenPhdQuery() {
		content = new HashMap<>();
	}
	
	@Override
	protected String buildContent(int uid) {
		content.put("id", uid);
		
		String result = new GsonBuilder().create().toJson(content);
		content = null;
		return result;
	}

	public void put(String string, Object object) {
		content.put(string, object);
	}
}
