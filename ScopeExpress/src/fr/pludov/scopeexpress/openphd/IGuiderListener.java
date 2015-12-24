package fr.pludov.scopeexpress.openphd;

import java.util.Map;

import fr.pludov.scopeexpress.ui.IDriverStatusListener;

public interface IGuiderListener extends IDriverStatusListener{
	void onEvent(String event, Map<?, ?> message);
}
