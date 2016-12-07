package fr.pludov.scopeexpress.focuser;

import org.apache.log4j.*;

import fr.pludov.scopeexpress.script.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;

public interface Focuser extends IDeviceBase {
	static final class LoggerContainer {
		private static final Logger logger = Logger.getLogger(Focuser.class);
	}
	
	/** Les notifications ne sont pas reçues dans le thread swing */
	public static interface Listener extends IDriverStatusListener{
		/** Emis suite à un ordre de déplacement qui se termine */
		void onMoveEnded();
		/** Emis periodiquement tant que le focuser est en cours de mouvement */
		void onMoving();
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

			boolean moving;
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
						moving = false;
						if (newPosition != position()) {
							failed("Position not achieved. conflict ?");
						} else {
							done(null);
						}
					}
					
					@Override
					public void onConnectionStateChanged() {
						moving = false;
						failed("Disconnected");
					}
				});
				moveTo(newPosition);
				moving = true;
				setStatus(Status.Blocked);
			}
			
			@Override
			protected void cancel() {
				// On laisse le focuser en place ?
				super.cancel();
				if (moving) {
					moving = false;
					try {
						moveTo(position());
					} catch(Throwable t) {
						LoggerContainer.logger.warn("Failed to stop focuser", t);
					}
				}
			}
		};
	}
}
