package fr.pludov.scopeexpress.scope.ascom;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.jawin.COMException;
import org.jawin.DispatchPtr;
import org.jawin.win32.Ole32;

import fr.pludov.scopeexpress.async.CancelationException;
import fr.pludov.scopeexpress.focus.SkyProjection;
import fr.pludov.scopeexpress.platform.windows.Ole;
import fr.pludov.scopeexpress.scope.Scope;
import fr.pludov.scopeexpress.scope.ScopeException;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WorkThread;

public class AscomScope extends WorkThread implements Scope {
	private static final Logger logger = Logger.getLogger(AscomScope.class);
	
	final WeakListenerCollection<Scope.Listener> listeners = new WeakListenerCollection<Scope.Listener>(Listener.class);
	
	public AscomScope()
	{
		super();
		this.lastNotifiedDec = Double.NaN;
		this.lastNotifiedRa = Double.NaN;
	}
	
	@Override
	public WeakListenerCollection<Listener> getListeners() {
		return listeners;
	}
	
	double decBias, raBias;
	
	
	String driver;
	DispatchPtr scope;
	
	// On n'emet pas de notification pour moins de 0,1 arcsec
	double lastNotifiedRa, lastNotifiedDec;
	
	double lastRa;
	double lastDec;
	boolean lastConnected;
	
	public boolean isConnected()
	{
		return lastConnected;
	}
	
	public double getRightAscension()
	{
		return lastRa + raBias;
	}
	
	public double getDeclination()
	{
		return lastDec + decBias;
	}
	
	/// Bloque l'appelant. ra en hms
	@Override
	public void sync(final double ra, final double dec) throws ScopeException {
		logger.info("Sync to ra=" + (ra - raBias) + ", dec=" + (dec - decBias));
		try {
			exec(new AsyncOrder() {
				
				@Override
				public Object run() throws Throwable {
					scope.invoke("SyncToCoordinates", ra - raBias, dec - decBias);
					return null;
				}
			});
		} catch(Throwable t) {
			throw new ScopeException("Erreur de sync du t�l�scope", t);
		}

	}
	
	// Bloque l'appelant
	public void slew(final double ra, final double dec) throws ScopeException
	{
		logger.info("Slew to ra=" + (ra - raBias) + ", dec=" + (dec - decBias));
		try {
			exec(new AsyncOrder() {
				
				@Override
				public Object run() throws Throwable {
					scope.invoke("SlewToCoordinatesAsync", ra - raBias, dec - decBias);
					return null;
				}
			});
		} catch(Throwable t) {
			throw new ScopeException("Erreur de d�placement du t�l�scope", t);
		}
	}
	
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
		double ra, declination;
		boolean connectStatus;
		if (scope != null) {
			ra = (Double)scope.get("RightAscension");
			declination = (Double)scope.get("Declination");
			connectStatus = (Boolean)scope.get("Connected");
		} else {
			connectStatus = false;
			ra = 0;
			declination = 0;
			this.lastNotifiedDec = Double.NaN;
			this.lastNotifiedRa = Double.NaN;
		}
		
		boolean fireConnectionChanged = false;
		boolean fireCoordinateChanged = false;
		
		synchronized(this)
		{
			if (this.lastRa != ra || this.lastDec != declination) {
				this.lastRa = ra;
				this.lastDec = declination;
				
				if (Double.isNaN(this.lastNotifiedDec)) {
					fireCoordinateChanged = true;
				} else {
					double degDist = SkyProjection.getDegreeDistance(
							new double[] { this.lastNotifiedRa * 360 / 24, this.lastNotifiedDec},
							new double[] { this.lastRa * 360 / 24, this.lastDec}
					);
					fireCoordinateChanged = degDist > 0.1 / 3600;
				}
			}
			if (this.lastConnected != connectStatus) {
				this.lastConnected = connectStatus;
				fireConnectionChanged = true;
				if (connectStatus) {
					fireCoordinateChanged = true;
				}
			}
		}
		
		if (fireConnectionChanged) {
			this.listeners.getTarget().onConnectionStateChanged();
		}
		
		if (fireCoordinateChanged) {
			this.listeners.getTarget().onCoordinateChanged();
			this.lastNotifiedRa = this.lastRa;
			this.lastNotifiedDec = this.lastDec;
		}
	}
	
	// Quand cette m�thode retourne, 
	protected void chooseScope() throws CancelationException
	{
		try {
			Ole.initOle();

			DispatchPtr app = new DispatchPtr("ASCOM.Utilities.Chooser");
		  
			app.put("DeviceType", "Telescope");
		  
			String result = (String)app.invoke("Choose", "");
		  
			if (result == null || "".equals(result)) {
				throw new CancelationException("Pas de driver s�l�ctionn�");
			}
			
			logger.debug("driver : " + result);
			driver = result;
			
			scope = new DispatchPtr(result);
			
			scope.put("Connected", true);
			
			refreshParameters();
			
//		  app.invoke("SetupDialog");
//		  app.put("Connected", true);
//		  System.err.println("RightAscension=" + app.get("RightAscension"));
//		  System.err.println("Declination=" + app.get("Declination"));

		  
		  
//		  DispatchPtr preses = app.getObject("Presentations");
//		  DispatchPtr pres = (DispatchPtr) preses.invoke("add", -1);
//		  DispatchPtr slides = pres.getObject("Slides");
//		  DispatchPtr slide = (DispatchPtr) slides.invoke("Add", 1, 2);
//		  DispatchPtr shapes = slide.getObject("Shapes");
//		  DispatchPtr shape = (DispatchPtr) shapes.invoke("Item", 1);
//		  DispatchPtr frame = shape.getObject("TextFrame");
//		  DispatchPtr range = frame.getObject("TextRange");
//		  range.put("Text", "Using Jawin to call COM objects");
		} catch (Throwable e) {
			scope = null;
//			releaseOle();
			if (e instanceof CancelationException) throw (CancelationException)e;
			e.printStackTrace();
			return;
		}
	}
	
	@Override
	public void run() {
		try {
			chooseScope();
			
		} catch(Throwable t) {
			notifyFinalDisconnection();
			if (t instanceof CancelationException) {
				return;
			}
			
			t.printStackTrace();
			return;
		}
		
		// On reste � l'�coute du travail � faire...
		try {
			try {
				setPeriodicTask(new Task() {
					@Override
					public Object run() throws Throwable {
						refreshParameters();
						return null;
					}
				}, 500);
				super.run();
			} finally {
				notifyFinalDisconnection();
			}
		} finally {
			// releaseOle();
		}
	}
	
	public static void connectScope()
	{
		new AscomScope().start();
	}

	public String getDriver() {
		return driver;
	}

	public double getDecBias() {
		return decBias;
	}

	public void setDecBias(double decBias) {
		this.decBias = decBias;
	}

	public double getRaBias() {
		return raBias;
	}

	public void setRaBias(double raBias) {
		this.raBias = raBias;
	}
}