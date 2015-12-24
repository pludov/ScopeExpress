package fr.pludov.scopeexpress.camera;

import java.awt.Color;
import java.io.File;

import javax.swing.SwingUtilities;

import fr.pludov.scopeexpress.ui.AutoFocusTask.Listener;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

public class TemperatureAdjusterTask {

	private final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	public final WeakListenerCollection<Listener> listeners = new WeakListenerCollection<>(Listener.class);
	Camera camera;

	Status status;	
	String statusErrorDetails;
	
	// -1 => cool, 1 => warm
	int sens;
	double targetTemperature;
	int timeout = 120;
	boolean shutdownOnTarget = false;
	double temperatureStep = 3.0;
	
	
	Double stepTemperatureStart;
	Double stepTemperatureSet;
	Long stepStart;
	
	public static enum Status {
		Processing("En cours", false, Color.orange),
		Done("Terminé", true, Color.green),
		Error("Erreur", true, Color.red),
		Canceled("Annulé", true, Color.red);
		
		public final String title;
		public final Color color;
		public final boolean closed;
		
		Status(String title, boolean closed, Color color)
		{
			this.title = title;
			this.color = color;
			this.closed = closed;
		}

		public boolean isClosed() {
			return this.closed;
		}
	};
	
	public static interface Listener {
		// Emis quand le status ou le flag interrupting change
		void onStatusChanged();
	}
	
	boolean acceptTemp(double currentTemp, double target)
	{
		boolean isFinal = target == this.targetTemperature;
		double delta = isFinal ? 0.1 : 0.3;
		if (this.shutdownOnTarget || !isFinal) {
			// On accepte que ça dépasse
			return sens * currentTemp > sens * (target - sens * delta);
		} else {
			// On veut précisément ce range
			return target - delta < currentTemp && target + delta > currentTemp;
		}
	}
	
	public TemperatureAdjusterTask() 
	{
		status = Status.Processing;
		targetTemperature = -5;
		sens = -1;
	}

	void die(Exception e) {
		
		e.printStackTrace();
		if (status.closed) {
			return;
		}
		setStatus(Status.Error, e.getMessage());
	}
	

	void setStatus(Status finalStatus, String errorMessage ) {
		if (camera != null) {
			camera.getListeners().removeListener(this.listenerOwner);
		}
		status = finalStatus;
		statusErrorDetails = errorMessage;
		this.listeners.getTarget().onStatusChanged();
	}
	
	
	void doProgress()
	{
		if (status != Status.Processing) {
			return;
		}
		if (camera == null || !camera.isConnected()) {
			die(new Exception("Déconnection"));
			return;
		}
		
		TemperatureParameters tp = camera.getTemperature();
		
		if (tp == null || tp.getCCDTemperature() == null) {
			// Pas de temperature ccd...
			die(new Exception("Température CCD non dispo"));
			return;
		}
		
		// On attend 2s avant de réagir, quoi qu'il arrive
		if ((stepStart == null) || ((System.currentTimeMillis() - stepStart > 2000) && acceptTemp(tp.getCCDTemperature(), stepTemperatureSet))) {
			if ((stepTemperatureSet != null && (stepTemperatureSet * sens >= this.targetTemperature * sens))
				|| (shutdownOnTarget && acceptTemp(tp.getCCDTemperature(), this.targetTemperature)))
			{
				// On est arrivé.
				if (shutdownOnTarget) {
					// En chauffage, on peut couper le refroidissement (FIXME: c'est un autre paramètre)
					try {
						camera.setCcdTemperature(false, null);
					} catch (CameraException e) {
						die(new Exception("Impossible de couper le refroidissement", e));
						return;
					}
				}
				
				setStatus(Status.Done, null);
				return;
			}
			
			double previousTemp = stepTemperatureStart == null ? tp.getCCDTemperature() : stepTemperatureSet;
			double setTemp = previousTemp + sens * temperatureStep;
			if (setTemp * sens > targetTemperature * sens) {
				setTemp = targetTemperature;
			}
			try {
				camera.setCcdTemperature(true, setTemp);
				stepTemperatureSet = setTemp;
				stepTemperatureStart = tp.getCCDTemperature();
				stepStart = System.currentTimeMillis();
			} catch (CameraException e) {
				die(new Exception("Impossible de controler la température", e));
				return;
			}
		} else {
			// Pas de progrès... On honore le timeout
			if (System.currentTimeMillis() > stepStart + ((long)timeout) * 1000) {
				die(new Exception("Délai dépassé"));
				return;
			}
			
		}
	}

	public void cancel() {
		if (status == Status.Processing) {
			setStatus(Status.Canceled, null);
		}
	}

	public Status getStatus() {
		return status;
	}

	public String getStatusErrorDetails() {
		return statusErrorDetails;
	}

	public void start(Camera camera) {

		
		this.camera = camera;

		stepTemperatureStart = null;
		stepTemperatureSet = null;
		stepStart = null;
		
		if (camera != null) {
			camera.getListeners().addListener(this.listenerOwner,
				new Camera.Listener() {
					
					@Override
					public void onTempeatureUpdated() {
						doProgress();
					}
					
					@Override
					public void onShootStarted(RunningShootInfo currentShoot) {
					}
					
					@Override
					public void onShootDone(RunningShootInfo shootInfo, File generatedFits) {						
					}
					@Override
					public void onShootInterrupted() {
					}
					@Override
					public void onConnectionStateChanged() {
						doProgress();
					}
			});
		}

		if (camera == null || !camera.isConnected()) {
			die(new Exception("Caméra non connectée"));
			return;
		}
		
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				doProgress();
			}
		});
	}

	public int getSens() {
		return sens;
	}

	public void setSens(int sens) {
		this.sens = sens;
	}

	public double getTargetTemperature() {
		return targetTemperature;
	}

	public void setTargetTemperature(double targetTemperature) {
		this.targetTemperature = targetTemperature;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isShutdownOnTarget() {
		return shutdownOnTarget;
	}

	public void setShutdownOnTarget(boolean shutdownOnTarget) {
		this.shutdownOnTarget = shutdownOnTarget;
	}

	public double getTemperatureStep() {
		return temperatureStep;
	}

	public void setTemperatureStep(double temperatureStep) {
		this.temperatureStep = temperatureStep;
	}
}
