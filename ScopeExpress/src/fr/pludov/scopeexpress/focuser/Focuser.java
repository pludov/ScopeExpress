package fr.pludov.scopeexpress.focuser;

import fr.pludov.scopeexpress.script.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;

public interface Focuser extends IDeviceBase {
	
	/** Les notifications ne sont pas reçues dans le thread swing */
	public static interface Listener {
		/** Emis suite à un ordre de déplacement qui se termine */
		void onMoveEnded();
		/** Emis periodiquement tant que le focuser est en cours de mouvement */
		void onMoving();
		void onConnectionStateChanged();
	}
	
	WeakListenerCollection<Listener> getListeners();
	
	@Override
	boolean isConnected();

	
	int maxStep();
	int position();
	
	void moveTo(int newPosition) throws FocuserException;
	
	@Override
	void start();
	@Override
	void close();
	
	
	public default Task coMove(final int newPosition) {
		return new NativeTask() {
			
			@Override
			public void init() throws Throwable {
				if (!isConnected()) {
					failed("Not connected");
					return;
				}
				
				getListeners().addListener(this.listenerOwner, new Listener() {
					@Override
					public void onMoving() {
					}
					
					@Override
					public void onMoveEnded() {
						if (getStatus() == Status.Blocked) {
							done(null);
						}
					}
					
					@Override
					public void onConnectionStateChanged() {
						if (getStatus() == Status.Blocked) {
							failed("Disconnected");
						}
					}

				});
				moveTo(newPosition);
				setStatus(Status.Blocked);
			}
		};
	}
}
