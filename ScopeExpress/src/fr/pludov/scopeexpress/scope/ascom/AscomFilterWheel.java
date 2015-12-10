package fr.pludov.scopeexpress.scope.ascom;

import java.util.Objects;

import org.apache.log4j.Logger;
import org.jawin.COMException;
import org.jawin.DispatchPtr;

import fr.pludov.scopeexpress.async.CancelationException;
import fr.pludov.scopeexpress.filterwheel.FilterWheel;
import fr.pludov.scopeexpress.filterwheel.FilterWheelException;
import fr.pludov.scopeexpress.focuser.Focuser;
import fr.pludov.scopeexpress.focuser.FocuserException;
import fr.pludov.scopeexpress.platform.windows.Ole;
import fr.pludov.scopeexpress.ui.IDriverStatusListener;
import fr.pludov.scopeexpress.utils.IWeakListenerCollection;
import fr.pludov.scopeexpress.utils.SubClassListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WorkThread;

public class AscomFilterWheel extends WorkThread implements FilterWheel {
	private static final Logger logger = Logger.getLogger(AscomFilterWheel.class);


	final WeakListenerCollection<FilterWheel.Listener> listeners = new WeakListenerCollection<FilterWheel.Listener>(FilterWheel.Listener.class, true);
	final IWeakListenerCollection<IDriverStatusListener> statusListeners;
	final String driver;

	DispatchPtr focuser;
	
	String [] filters;
	// -1 on move
	Integer lastPosition;
	boolean lastConnected;

	boolean waitingMoveEnd;

	public AscomFilterWheel(String driver) {
		super();
		statusListeners = new SubClassListenerCollection<IDriverStatusListener, FilterWheel.Listener>(listeners) {
			@Override
			protected FilterWheel.Listener createListenerFor(final IDriverStatusListener i) {
				return new FilterWheel.Listener() {
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
	public WeakListenerCollection<FilterWheel.Listener> getListeners() {
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
		Integer position;
		String [] maxPosition;
		boolean connectStatus;
		boolean moving;
		if (focuser != null) {
			connectStatus = (Boolean)focuser.get("Connected");
			if (connectStatus) {
				Short pos = (Short)focuser.get("Position");
				if (pos == null) {
					position = null;
				} else {
					position = (int)pos;
				}
				if (this.filters == null) {
					maxPosition = (String[])focuser.get("Names");
				} else {
					maxPosition = this.filters;
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
					|| (!Objects.equals(maxPosition, this.filters)))
			{
				this.lastPosition = position;
				this.filters = maxPosition;

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
							|| (this.lastPosition >= 0)))
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
			
			focuser.put("Connected", true);
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
	public void moveTo(final int newPosition) throws FilterWheelException {
		logger.info("Moving filterWheel to " + newPosition);
		try {
			exec(new AsyncOrder() {
				@Override
				public Object run() throws Throwable {
					focuser.put("Position", (Short)(short)newPosition);
					synchronized(this) {
						waitingMoveEnd = true;
						lastPosition = -1;
					}
					
					return null;
				}
			});
		} catch(Throwable t) {
			throw new FilterWheelException("Erreur de positionnement du focuser", t);
		}		
	}

	@Override
	public int getCurrentPosition() throws FilterWheelException {
		return lastPosition;
	}

	@Override
	public String[] getFilters() throws FilterWheelException {
		return filters;
	}

}
