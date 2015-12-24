package fr.pludov.scopeexpress.openphd;

import java.util.Map;

public interface OpenPhdRequestListener {
	void onReply(Map<?, ?> message);
	void onFailure();
}
