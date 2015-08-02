package fr.pludov.scopeexpress.scope.dummy;

import javax.swing.SwingUtilities;

import fr.pludov.scopeexpress.scope.Scope;
import fr.pludov.scopeexpress.scope.ScopeException;
import fr.pludov.scopeexpress.scope.Scope.Listener;
import fr.pludov.scopeexpress.ui.IDriverStatusListener;
import fr.pludov.scopeexpress.utils.IWeakListenerCollection;
import fr.pludov.scopeexpress.utils.SubClassListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;

public class DummyScope implements Scope{
	boolean connectionStatus;
	
	double raBias, decBias, rightAscension, declination;
	final private WeakListenerCollection<Scope.Listener> listeners = new WeakListenerCollection<Scope.Listener>(Listener.class);
	final IWeakListenerCollection<IDriverStatusListener> statusListener;

	public DummyScope() {
		super();
		this.connectionStatus = false;
		this.statusListener = new SubClassListenerCollection<IDriverStatusListener, Scope.Listener>(this.listeners) {

			@Override
			protected Listener createListenerFor(final IDriverStatusListener i) {
				return new Listener() {
					@Override
					public void onConnectionStateChanged() {
						i.onConnectionStateChanged();
					}
					@Override
					public void onCoordinateChanged() {
					}
				};
			}
		};
	}
	
	@Override
	public IWeakListenerCollection<IDriverStatusListener> getStatusListener() {
		return statusListener;
	}
	
	@Override
	public boolean isConnected() {
		return connectionStatus;
	}

	private void fireCoordChanged()
	{
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				DummyScope.this.listeners.getTarget().onCoordinateChanged();	
			}
		});
	}
	
	@Override
	public void sync(final double ra, final double dec) throws ScopeException {
		final double wantRa = ra - raBias;
		final double wantDec = dec - decBias;
		this.rightAscension = wantRa;
		this.declination = wantDec;
		
		fireCoordChanged();
	}
	
	@Override
	public void slew(final double ra, final double dec) throws ScopeException {
		final double wantRa = ra - raBias;
		final double wantDec = dec - decBias;
		
		new Thread() {
			
			public void run() {
				boolean doRa = true;
				boolean doDec = true;
				
				// Vitesse en degrés
				double [] speedDecList = {0.1, 0.03, 0.01, 0.003, 0.001, 0.0003, 0.0001, 0.00003, 0.00001};
				// Vitesse en heure
				double [] speedRaList = new double[speedDecList.length];
				
				for(int i = 0 ; i < speedDecList.length; ++i)
				{
					speedRaList[i] = speedDecList[i] * 24.0 / 360.0;
				}
				
				int raSpeedMax = speedRaList.length - 1;
				int decSpeedMax = speedDecList.length - 1;
				int cpt = 0;
				do {
					if (doRa) {
						double delta = wantRa - rightAscension;
						
						// Trouver la premiere vitesse plus petite que delta / X
						double speedRa = delta;
						for(int i = raSpeedMax; i < speedRaList.length; ++i)
						{
							double speedRaItem = speedRaList[i];
							speedRa = speedRaItem;
							if (speedRa < Math.abs(delta) / 10.0) break;
						}

						
						while(delta >= 12) {
							delta -= 24;
						}
						while(delta < -12) {
							delta += 24;
						}
						
						// Le delta est modulo 24
						if (delta > speedRa) {
							delta = speedRa;
						} else if (delta < -speedRa) {
							delta = -speedRa;
						} else {
							doRa = false;
						}
						
						rightAscension += delta;
						if (rightAscension < 0) rightAscension += 24;
						if (rightAscension >= 24) rightAscension -= 24;
					}
					
					if (doDec) {
						double delta = wantDec - declination;
						
						// Trouver la premiere vitesse plus petite que delta / X
						double speedDec = delta;
						for(int i = decSpeedMax; i < speedDecList.length; ++i)
						{
							double speedDecItem = speedDecList[i];
							speedDec = speedDecItem;
							if (speedDec < Math.abs(delta) / 10.0) break;
						}

						
						if (delta > speedDec) {
							delta = speedDec;
						} else if (delta < -speedDec) {
							delta = -speedDec;
						} else {
							doDec = false;
						}
						
						declination += delta;
					}
					
					cpt++;
					if ((cpt & 15) == 0)
					{
						if (decSpeedMax > 0) decSpeedMax--;
						if (raSpeedMax > 0) raSpeedMax--;
					}
		
					fireCoordChanged();
					
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
					}
					
				} while(doRa || doDec);
				fireCoordChanged();
			}
			
		}.start();
		
	}

	@Override
	public void start() {
		this.connectionStatus = true;
		this.listeners.getTarget().onConnectionStateChanged();
	}
	
	@Override
	public void close() {
		
	}

	public double getRaBias() {
		return raBias;
	}

	public void setRaBias(double raBias) {
		this.raBias = raBias;
		fireCoordChanged();
	}

	public double getDecBias() {
		return decBias;
	}

	public void setDecBias(double decBias) {
		this.decBias = decBias;
		fireCoordChanged();
	}

	public double getRightAscension() {
		return rightAscension + raBias;
	}

	public double getDeclination() {
		return declination + decBias;
	}

	@Override
	public WeakListenerCollection<Listener> getListeners() {
		return this.listeners;
	}
}
