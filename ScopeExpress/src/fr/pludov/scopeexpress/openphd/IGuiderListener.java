package fr.pludov.scopeexpress.openphd;

import com.google.gson.*;

import fr.pludov.scopeexpress.ui.*;

public interface IGuiderListener extends IDriverStatusListener{
	void onEvent(String event, JsonObject message);
}
