package fr.pludov.scopeexpress.filterwheel;

import org.apache.log4j.*;

import fr.pludov.scopeexpress.script.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;

public interface FilterWheel extends IDeviceBase {
	static final class LoggerContainer {
		private static final Logger logger = Logger.getLogger(FilterWheel.class);
	}
	
	/** Les notifications ne sont pas reçues dans le thread swing */
	public static interface Listener {
		/** Emis suite à un ordre de déplacement qui se termine */
		void onMoveEnded();
		/** Emis periodiquement tant que la roue à filtre est en cours de mouvement */
		void onMoving();
		void onConnectionStateChanged();
	}
	
	WeakListenerCollection<Listener> getListeners();
	
	@Override
	boolean isConnected();

	void moveTo(int newPosition) throws FilterWheelException;
	
	int getCurrentPosition() throws FilterWheelException;
	
	String [] getFilters() throws FilterWheelException;
	
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
						try {
							if (newPosition != getCurrentPosition()) {
								failed("Position not achieved. conflict ?");
							} else {
								done(null);
							}
						} catch(FilterWheelException e) {
							failed(e);
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
						moveTo(getCurrentPosition());
					} catch(Throwable t) {
						LoggerContainer.logger.warn("Failed to stop focuser", t);
					}
				}
			}
		};
	}
}
