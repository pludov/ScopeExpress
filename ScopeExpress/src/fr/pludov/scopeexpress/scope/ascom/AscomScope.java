package fr.pludov.scopeexpress.scope.ascom;

import org.apache.log4j.*;
import org.jawin.*;

import fr.pludov.scopeexpress.async.*;
import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.platform.windows.*;
import fr.pludov.scopeexpress.scope.*;
import fr.pludov.scopeexpress.script.*;
import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;

public class AscomScope extends WorkThread implements Scope {
	private static final Logger logger = Logger.getLogger(AscomScope.class);
	
//	static StringConfigItem lastUsedScope = new StringConfigItem(AscomScope.class, "previousScope", "");

	
	final WeakListenerCollection<Scope.Listener> listeners = new WeakListenerCollection<Scope.Listener>(Listener.class, true);
	final IWeakListenerCollection<IDriverStatusListener> statusListener;
	
	public AscomScope(String driver)
	{
		super();
		this.driver = driver;
		this.lastNotifiedDec = Double.NaN;
		this.lastNotifiedRa = Double.NaN;
		this.statusListener = new SubClassListenerCollection<IDriverStatusListener, Scope.Listener>(this.listeners, IDriverStatusListener.class, Scope.Listener.class);
	}
	
	@Override
	public IWeakListenerCollection<IDriverStatusListener> getStatusListener() {
		return statusListener;
	}
	
	@Override
	public WeakListenerCollection<Listener> getListeners() {
		return listeners;
	}
	
	double decBias, raBias;
	
	
	final String driver;
	DispatchPtr scope;
	
	// On n'emet pas de notification pour moins de 0,1 arcsec
	double lastNotifiedRa, lastNotifiedDec;
	
	double lastRa;
	double lastDec;
	boolean lastConnected;
	
	@Override
	public boolean isConnected()
	{
		return lastConnected;
	}
	
	@Override
	public double getRightAscension()
	{
		return lastRa + raBias;
	}
	
	@Override
	public double getDeclination()
	{
		return lastDec + decBias;
	}
	
	/// Bloque l'appelant. ra en hms
	@Override
	public void sync(final double ra, final double dec) throws ScopeException {
		logger.info("Sync to ra=" + fr.pludov.scopeexpress.ui.utils.Utils.formatHourMinSec((ra - raBias) * 360.0/24) + ", dec=" + fr.pludov.scopeexpress.ui.utils.Utils.formatDegMinSec(dec - decBias) + " (jnow)");
		try {
			exec(new AsyncOrder() {
				
				@Override
				public Object run() throws Throwable {
					scope.invoke("SyncToCoordinates", ra - raBias, dec - decBias);
					return null;
				}
			});
		} catch(Throwable t) {
			throw new ScopeException("Erreur de sync du téléscope", t);
		}

	}
	
	// Bloque l'appelant
	@Override
	public void slew(final double ra, final double dec) throws ScopeException
	{
		logger.info("Slew to ra=" + fr.pludov.scopeexpress.ui.utils.Utils.formatHourMinSec((ra - raBias) * 360.0/24) + ", dec=" + fr.pludov.scopeexpress.ui.utils.Utils.formatDegMinSec(dec - decBias) + " (jnow)");
		try {
			exec(new AsyncOrder() {
				
				@Override
				public Object run() throws Throwable {
					scope.invoke("SlewToCoordinatesAsync", ra - raBias, dec - decBias);
					return null;
				}
			});
		} catch(Throwable t) {
			throw new ScopeException("Erreur de déplacement du téléscope", t);
		}
	}

	@Override
	public NativeTask coPulse(double ra, double dec) throws ScopeException {
		logger.info("pulsing : ra=" + ra + ", dec=" + dec);
		
		AsyncOrder asyncO = new AsyncOrder() {

			@Override
			public Object run() throws Throwable {
				long raMs = 0, decMs = 0;

				if (ra != 0) {
					int dir = ra > 0 ? 2 : 3;
					scope.put("GuideRateRightAscension",360 / 3600.0);
					double raSpeed = ((double)scope.get("GuideRateRightAscension"));
					logger.info("raSpeed is " + raSpeed * 3600);
					raMs = (long) Math.floor(1000 * Math.abs(ra) / raSpeed);
					logger.info("ra pulse duration: " + raMs);
					if (raMs > 15000) raMs = 15000;
					scope.invoke("PulseGuide", dir, raMs);
				}
				if (dec != 0) {
					int dir = dec > 0 ? 0 : 1;
					scope.put("GuideRateDeclination",360 / 3600.0);
					double decSpeed = ((double)scope.get("GuideRateDeclination"));
					logger.info("decSpeed is " + decSpeed * 3600);
					
					decMs = (long) Math.floor(1000 * Math.abs(dec) / decSpeed);
					if (decMs > 15000) decMs = 15000;
					logger.info("dec pulse duration: " + decMs);
					scope.invoke("PulseGuide", dir, decMs);
				}

				if ((Boolean)scope.get("IsPulseGuiding")) {
					Thread.sleep(Math.max(raMs, decMs));
				}

				return null;
			}
		};

		return execAsNativeTask(asyncO);
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
	
	// Quand cette méthode retourne, 
	protected void chooseScope() throws CancelationException, Throwable
	{
		try {
			Ole.initOle();

			logger.debug("driver : " + driver);
			
			scope = new DispatchPtr(driver);
			
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
			throw e;
		}
	}
	
	@Override
	public void run() {
		try {
			chooseScope();
			
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

	public String getDriver() {
		return driver;
	}

	@Override
	public double getDecBias() {
		return decBias;
	}

	@Override
	public void setDecBias(double decBias) {
		this.decBias = decBias;
	}

	@Override
	public double getRaBias() {
		return raBias;
	}

	@Override
	public void setRaBias(double raBias) {
		this.raBias = raBias;
	}
}
