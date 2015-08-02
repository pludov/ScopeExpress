package fr.pludov.scopeexpress.scope.ascom;

import java.util.Objects;

import org.apache.log4j.Logger;
import org.jawin.COMException;
import org.jawin.DispatchPtr;

import fr.pludov.scopeexpress.async.CancelationException;
import fr.pludov.scopeexpress.focus.SkyProjection;
import fr.pludov.scopeexpress.focuser.Focuser;
import fr.pludov.scopeexpress.focuser.FocuserException;
import fr.pludov.scopeexpress.platform.windows.Ole;
import fr.pludov.scopeexpress.scope.Scope;
import fr.pludov.scopeexpress.scope.Scope.Listener;
import fr.pludov.scopeexpress.ui.IDriverStatusListener;
import fr.pludov.scopeexpress.utils.IWeakListenerCollection;
import fr.pludov.scopeexpress.utils.SubClassListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WorkThread;

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
		statusListeners = new SubClassListenerCollection<IDriverStatusListener, Focuser.Listener>(listeners) {
			@Override
			protected Focuser.Listener createListenerFor(final IDriverStatusListener i) {
				return new Focuser.Listener() {
					@Override
					public void onConnectionStateChanged() {
						i.onConnectionStateChanged();
					}
					@Override
					public void onMoveEnded() {
					}
					@Override
					public void onMoving() {
					}
				};
			}
		};
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
	}

	private void notifyFinalDisconnection()
	{
		synchronized(this) {
			if (!this.lastConnected) return;
			this.lastConnected = false;
		}
		this.listeners.getTarget().onConnectionStateChanged();
	}
	

	private void refreshParameters() throws Throwable
	{
		Integer position, maxPosition;
		boolean connectStatus;
		boolean moving;
		if (focuser != null) {
			connectStatus = (Boolean)focuser.get(connectedPropertyName);
			if (connectStatus) {
				position = (Integer)focuser.get("Position");
				moving = (Boolean)focuser.get("IsMoving");
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
		} else {
			connectStatus = false;
			position = null;
			maxPosition = null;
			moving = false;
		}
		
		boolean fireConnectionChanged = false;
		boolean fireCoordinateChanged = false;
		boolean fireMoveEnded = false;
		
		synchronized(this)
		{
			if ((!Objects.equals(position, this.lastPosition))
					|| (!Objects.equals(maxPosition, this.maxPosition))
					|| (!Objects.equals(moving, this.lastMoving)))
			{
				this.lastPosition = position;
				this.maxPosition = maxPosition;
				this.lastMoving = moving;

				fireCoordinateChanged = true;
			}
			if (this.lastConnected != connectStatus) {
				this.lastConnected = connectStatus;
				fireConnectionChanged = true;
				if (connectStatus) {
					fireCoordinateChanged = true;
				}
			}
			
			if (waitingMoveEnd && 
					((!this.lastConnected)
							|| (!this.lastMoving)))
			{
				waitingMoveEnd = false;
				fireMoveEnded = true;
			}
		}
		
		if (fireConnectionChanged) {
			this.listeners.getTarget().onConnectionStateChanged();
		}
		
		if (fireCoordinateChanged) {
			this.listeners.getTarget().onMoving();
		}
		
		if (fireMoveEnded) {
			this.listeners.getTarget().onMoveEnded();
		}
	}
	
	// Quand cette méthode retourne, 
	protected void chooseFocuser() throws CancelationException
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
			focuser = null;
			if (e instanceof CancelationException) throw (CancelationException)e;
			e.printStackTrace();
			return;
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
