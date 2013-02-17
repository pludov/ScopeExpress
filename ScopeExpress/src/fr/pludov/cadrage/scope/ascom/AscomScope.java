package fr.pludov.cadrage.scope.ascom;

import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.jawin.COMException;
import org.jawin.DispatchPtr;
import org.jawin.win32.Ole32;

import fr.pludov.cadrage.Cadrage;
import fr.pludov.cadrage.async.CancelationException;
import fr.pludov.cadrage.scope.Scope;
import fr.pludov.cadrage.scope.ScopeException;
import fr.pludov.cadrage.utils.WorkThread;

public class AscomScope extends WorkThread implements Scope {
	private static final Logger logger = Logger.getLogger(AscomScope.class);
	
	boolean isOleInitialized;
	
	double decBias, raBias;
	
	
	String driver;
	DispatchPtr scope;
	
	// Toutes les communications OLE ont lieu dans ce Thread...
	Thread oleThread;
	
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
		
	private void refreshParameters() throws Throwable
	{
		double ra, declination;
		boolean connectStatus;
		ra = (Double)scope.get("RightAscension");
		declination = (Double)scope.get("Declination");
		connectStatus = (Boolean)scope.get("Connected");
		
		synchronized(this)
		{
			this.lastRa = ra;
			this.lastDec = declination;
			this.lastConnected = connectStatus;
		}
	}
	
	private void initOle() throws COMException
	{
		if (isOleInitialized) {
			throw new RuntimeException("OLE d�j� initialis�");
		}
		Ole32.CoInitialize();
		isOleInitialized = true;
	}
	
	private void releaseOle()
	{
		if (!isOleInitialized) {
			return;
		}
		try {
			Ole32.CoUninitialize();
		} catch(COMException e) {
			logger.error("Unable to uninitialize Ole32", e);
		}
	}
	
	
	// Quand cette m�thode retourne, 
	protected void chooseScope() throws CancelationException
	{
		try {
			initOle();

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
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					Cadrage.setScopeInterface(AscomScope.this);
				}
			});
			
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
			releaseOle();
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
			if (t instanceof CancelationException) {
				return;
			}
			
			t.printStackTrace();
			return;
		}
		
		// On reste � l'�coute du travail � faire...
		try {
			setPeriodicTask(new Task() {
				@Override
				public Object run() throws Throwable {
					refreshParameters();
					return null;
				}
			}, 1000);
			super.run();
		} finally {
			releaseOle();
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
