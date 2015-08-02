package fr.pludov.scopeexpress.ui;

import fr.pludov.scopeexpress.utils.IWeakListenerCollection;

public interface IDeviceBase {

	IWeakListenerCollection<IDriverStatusListener> getStatusListener();
	
	public boolean isConnected();
	public void start();
	public void close();
}
