package fr.pludov.scopeexpress.openphd;

import java.util.*;

import org.apache.log4j.*;

import com.google.gson.*;

import fr.pludov.scopeexpress.script.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;
import fr.pludov.scopeexpress.utils.WeakListenerCollection.*;

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
					public void onConnectionError(Throwable message) {
						i.onConnectionError(message);
					}
					
					@Override
					public void onEvent(String e, JsonObject message) {
						if (e.equals("AppState")) {
							currentState = message.get("State").getAsString();
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
		Throwable reportError = null;
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
			reportError = t;
		} finally {
			close();
			if (reportError != null) {
				listeners.getTarget().onConnectionStateChanged();
				listeners.getTarget().onConnectionError(reportError);
			}
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

	public NativeTask coReadEvents()
	{
		return new NativeTask() {
			
			@Override
			protected void init() throws Throwable {
				if (establishedConnection == null) {
					throw new Exception("PHD not connected");
				}
				listeners.addListener(this.listenerOwner, new IGuiderListener() {
					
					@Override
					public void onConnectionStateChanged() {
						failed("disconnected");
					}
					
					@Override
					public void onConnectionError(Throwable message) {
						failed("disconnected: " + message.getMessage());
					}
					
					@Override
					public void onEvent(String event, JsonObject message) {
						produce(new GsonBuilder().create().toJson(message));
					}
				});
			}
		};
	}
	
	public NativeTask coSendRequest(final String jsonRqt)
	{
		return new NativeTask() {
			
			@Override
			protected void init() throws Throwable {
				OpenPhdRawQuery query = new OpenPhdRawQuery() {
					
					@Override
					protected String buildContent(int uid) {
						Map<String, Object> rqt = new GsonBuilder().create().fromJson(jsonRqt, Map.class);
						rqt.put("id", uid);
						String result = new GsonBuilder().create().toJson(rqt);
						
						return result;

					}
					
					@Override
					public void onReply(JsonObject message) {
						done(new GsonBuilder().create().toJson(message));
					}
					
					@Override
					public void onFailure() {
						failed("generic failure");
					}
				};
				query.send(OpenPhdDevice.this);
			}
		};
	}
	
	public WeakListenerCollection<IGuiderListener> getListeners() {
		return listeners;
	}
}
