package fr.pludov.scopeexpress.openphd;

import java.util.Map;

import org.apache.log4j.Logger;

import fr.pludov.scopeexpress.filterwheel.FilterWheel;
import fr.pludov.scopeexpress.ui.IDeviceBase;
import fr.pludov.scopeexpress.ui.IDriverStatusListener;
import fr.pludov.scopeexpress.utils.IWeakListenerCollection;
import fr.pludov.scopeexpress.utils.SubClassListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerCollection.AsyncKind;

public class OpenPhdDevice extends Thread implements IDeviceBase {
	public static final Logger logger = Logger.getLogger(OpenPhdDevice.class);
	
	volatile OpenPhdConnection establishedConnection;
	volatile boolean normalClose;
	
	final WeakListenerCollection<IGuiderListener> listeners = new WeakListenerCollection<IGuiderListener>(IGuiderListener.class, AsyncKind.SwingQueueIfRequired);
	final IWeakListenerCollection<IDriverStatusListener> statusListeners;	
	
	String currentState;
	
	public OpenPhdDevice() {
		normalClose = false;
		currentState = null;
		statusListeners = new SubClassListenerCollection<IDriverStatusListener, IGuiderListener>(listeners) {
			@Override
			protected IGuiderListener createListenerFor(final IDriverStatusListener i) {
				return new IGuiderListener() {
					@Override
					public void onConnectionStateChanged() {
						i.onConnectionStateChanged();
					}
					
					@Override
					public void onEvent(String e, Map<?, ?> message) {
						if (e.equals("AppState")) {
							currentState = (String)message.get("State");
						}
					}
				};
			}
		};
		
	}

	@Override
	public IWeakListenerCollection<IDriverStatusListener> getStatusListener() {
		return statusListeners;
	}

	@Override
	public boolean isConnected() {
		return establishedConnection != null;
	}

	@Override
	public void run() {
		try {
			OpenPhdConnection connection = new OpenPhdConnection(this);
			connection.connect();
			establishedConnection = connection;
			listeners.getTarget().onConnectionStateChanged();

			connection.proceed();
		} catch(Throwable t) {
			if (!normalClose) {
				logger.warn("Exception in phd device", t);
			}
		} finally {
			close();
		}
	
	}

	@Override
	public void close() {
		normalClose = true;
		internalClose();
	}
	
	void internalClose() {
		// En cas d'appel asynchrone ???
		if (establishedConnection != null) {
			
			OpenPhdConnection previousConnection = establishedConnection;
			establishedConnection = null;
			listeners.getTarget().onConnectionStateChanged();
			previousConnection.close();
		}
	}

	public WeakListenerCollection<IGuiderListener> getListeners() {
		return listeners;
	}
}
