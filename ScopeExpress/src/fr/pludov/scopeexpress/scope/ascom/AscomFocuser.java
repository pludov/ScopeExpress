package fr.pludov.scopeexpress.scope.ascom;

import java.util.*;

import org.apache.log4j.*;
import org.jawin.*;

import fr.pludov.scopeexpress.async.*;
import fr.pludov.scopeexpress.focuser.*;
import fr.pludov.scopeexpress.platform.windows.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;

public class AscomFocuser extends WorkThread implements Focuser {
	private static final Logger logger = Logger.getLogger(AscomFocuser.class);
	
	final WeakListenerCollection<Focuser.Listener> listeners = new WeakListenerCollection<Focuser.Listener>(Focuser.Listener.class, true);
	final IWeakListenerCollection<IDriverStatusListener> statusListeners;
	final String driver;
	DispatchPtr focuser;
	int focuserInterfaceVersion;
	String connectedPropertyName;
	
	Boolean lastMoving;
	Integer lastPosition;
	Integer maxPosition;
	boolean lastConnected;

	boolean waitingMoveEnd;
	
	public AscomFocuser(String driver) {
		super();
		statusListeners = new SubClassListenerCollection<IDriverStatusListener, Focuser.Listener>(listeners, IDriverStatusListener.class, Focuser.Listener.class);
		this.driver = driver;
		this.lastConnected = false;
		this.lastPosition = null;
	}


	@Override
	public WeakListenerCollection<Focuser.Listener> getListeners() {
		return listeners;
	}
	
	@Override
	public IWeakListenerCollection<IDriverStatusListener> getStatusListener() {
		return statusListeners;
	}
	
	@Override
	public void close()
	{
		logger.info("Closing focuser");
		setTerminated();
		
		boolean retry;
		do {
			try {
				retry = false;
				join();
			} catch(InterruptedException e) {
				e.printStackTrace();
				retry = true;
			}
		} while(retry);
		
		releaseFocuser();
	}

	private void notifyFinalDisconnection()
	{
		synchronized(this) {
			if (!this.lastConnected) return;
			this.lastConnected = false;
		}
		this.listeners.getTarget().onConnectionStateChanged();
	}
	
	private void releaseFocuser()
	{
		logger.debug("Releasing focuser");
		synchronized(this) {
			if (focuser == null) return;
			try {
				focuser.close();
			} catch(Throwable t) {
				logger.warn("Failed to release focuser", t);
			} finally {
				focuser = null;
			}
		}
	}

	private void refreshParameters() throws Throwable
	{
		Integer position, maxPosition;
		boolean connectStatus;
		boolean moving;
		if (focuser != null) {
			try {
				connectStatus = (Boolean)focuser.get(connectedPropertyName);
				if (connectStatus) {
					moving = (Boolean)focuser.get("IsMoving");
					position = (Integer)focuser.get("Position");
					if (this.maxPosition == null) {
						maxPosition = (Integer)focuser.get("MaxStep");
					} else {
						maxPosition = this.maxPosition;
					}
				} else {
					position = null;
					maxPosition = null;
					moving = false;
				}
			} catch(Throwable t) {
				logger.error("Communication with focuser failed", t);

				StatusUpdater su;
				synchronized(this) {
					releaseFocuser();
					su = new StatusUpdater(false, false, null, null);
				}
				su.sendNotifications();
				return;
			}
		} else {
			connectStatus = false;
			position = null;
			maxPosition = null;
			moving = false;
		}
		
		new StatusUpdater(connectStatus, moving, position, maxPosition).sendNotifications();
	}

	private class StatusUpdater {
		boolean fireConnectionChanged = false;
		boolean fireCoordinateChanged = false;
		boolean fireMoveEnded = false;
		
		
		
		private StatusUpdater(boolean connectStatus, boolean moving, Integer position, Integer maxPosition) {
			
			synchronized(AscomFocuser.this)
			{
				if ((!Objects.equals(position, AscomFocuser.this.lastPosition))
						|| (!Objects.equals(maxPosition, AscomFocuser.this.maxPosition))
						|| (!Objects.equals(moving, AscomFocuser.this.lastMoving)))
				{
					AscomFocuser.this.lastPosition = position;
					AscomFocuser.this.maxPosition = maxPosition;
					AscomFocuser.this.lastMoving = moving;
					
					fireCoordinateChanged = true;
				}
				if (AscomFocuser.this.lastConnected != connectStatus) {
					AscomFocuser.this.lastConnected = connectStatus;
					fireConnectionChanged = true;
					if (connectStatus) {
						fireCoordinateChanged = true;
					}
				}
				
				if (waitingMoveEnd && 
						((!AscomFocuser.this.lastConnected)
								|| (!AscomFocuser.this.lastMoving)))
				{
					waitingMoveEnd = false;
					fireMoveEnded = true;
				}
			}
		}
		
		void sendNotifications() {
			if (fireConnectionChanged) {
				AscomFocuser.this.listeners.getTarget().onConnectionStateChanged();
			}
			
			if (fireCoordinateChanged) {
				AscomFocuser.this.listeners.getTarget().onMoving();
			}
			
			if (fireMoveEnded) {
				AscomFocuser.this.listeners.getTarget().onMoveEnded();
			}
			
		}
	}
	
	// Quand cette méthode retourne, 
	protected void chooseFocuser() throws CancelationException, Throwable
	{
		try {
			Ole.initOle();

			logger.debug("driver : " + driver);
			
			focuser = new DispatchPtr(driver);
			
			try {
				focuserInterfaceVersion = (Short)focuser.get("InterfaceVersion");
			} catch(COMException  t) {
				logger.info("Could not get InterfaceVersion", t);
				focuserInterfaceVersion = 1;
			}
			connectedPropertyName = focuserInterfaceVersion >= 2 ? "Connected" : "Link";
			focuser.put(connectedPropertyName, true);

			Boolean isAbsolute = (Boolean)focuser.get("Absolute");
			if (!isAbsolute) {
				throw new FocuserException("Only absolute focuser supported");
			}
			refreshParameters();
			
		} catch (Throwable e) {
			if (focuser != null) {
				try {
					focuser.close();
				} catch(Throwable t) {
					logger.warn("Failed to close focuser", t);
				}
				
				focuser = null;
			}
			
			if (e instanceof CancelationException) throw (CancelationException)e;
			e.printStackTrace();
			throw e;
		}
	}


	@Override
	public void run() {
		try {
			chooseFocuser();
			
		} catch(Throwable t) {
			this.listeners.getTarget().onConnectionStateChanged();
			if (t instanceof CancelationException) {
				return;
			}
			
			t.printStackTrace();
			this.listeners.getTarget().onConnectionError(t);
			return;
		}
		
		// On reste à l'écoute du travail à faire...
		try {
			try {
				setPeriodicTask(new Task() {
					int idleCount = 0;
					@Override
					public Object run() throws Throwable {
						if (waitingMoveEnd || (idleCount >= 10)) {
							idleCount = 0;
							refreshParameters();
						} else {
							idleCount++;
						}
						return null;
					}
				}, 50);
				super.run();
			} finally {
				notifyFinalDisconnection();
			}
		} finally {
			// releaseOle();
		}
	}


	public String getDriver() {
		return driver;
	}
	
	@Override
	public boolean isConnected() {
		return lastConnected;
	}

	@Override
	public int maxStep() {
		return 100000;
		//return maxPosition != null ? maxPosition : 0;
	}
	
	@Override
	public void moveTo(final int newPosition) throws FocuserException {
		logger.info("Moving focuser to " + newPosition);
		try {
			exec(new AsyncOrder() {
				@Override
				public Object run() throws Throwable {
					focuser.invoke("Move", newPosition);
					synchronized(this) {
						waitingMoveEnd = true;
						lastMoving = true;
					}
					
					return null;
				}
			});
		} catch(Throwable t) {
			throw new FocuserException("Erreur de positionnement du focuser", t);
		}
	}

	@Override
	public int position() {
		return lastPosition;
	}
}
