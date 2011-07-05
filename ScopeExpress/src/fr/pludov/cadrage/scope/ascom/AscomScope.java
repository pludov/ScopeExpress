package fr.pludov.cadrage.scope.ascom;

import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.jawin.COMException;
import org.jawin.DispatchPtr;
import org.jawin.win32.Ole32;

import fr.pludov.cadrage.Cadrage;
import fr.pludov.cadrage.async.CancelationException;
import fr.pludov.cadrage.scope.Scope;
import fr.pludov.cadrage.scope.ScopeException;
import fr.pludov.cadrage.utils.WorkThread;

public class AscomScope extends WorkThread implements Scope {
	boolean isOleInitialized;
	
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
		return lastRa;
	}
	
	public double getDeclination()
	{
		return lastDec;
	}
	
	// Bloque l'appelant
	public void slew(final double ra, final double dec) throws ScopeException
	{
		try {
			exec(new AsyncOrder() {
				
				@Override
				public Object run() throws Throwable {
					scope.invoke("SlewToCoordinates", ra, dec);
					return null;
				}
			});
		} catch(Throwable t) {
			throw new ScopeException("Erreur de déplacement du téléscope", t);
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
			throw new RuntimeException("OLE déjà initialisé");
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
			System.err.println("Unable to uninitialize Ole32");
			e.printStackTrace();
		}
	}
	
	
	// Quand cette méthode retourne, 
	protected void chooseScope() throws CancelationException
	{
		try {
			initOle();

			DispatchPtr app = new DispatchPtr("ASCOM.Utilities.Chooser");
		  
			app.put("DeviceType", "Telescope");
		  
			String result = (String)app.invoke("Choose", "");
		  
			if (result == null || "".equals(result)) {
				throw new CancelationException("Pas de driver séléctionné");
			}
			
			System.err.println("driver : " + result);
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
		
		// On reste à l'écoute du travail à faire...
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
}
