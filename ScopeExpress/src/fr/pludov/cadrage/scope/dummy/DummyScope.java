package fr.pludov.cadrage.scope.dummy;

import fr.pludov.cadrage.scope.Scope;
import fr.pludov.cadrage.scope.ScopeException;

public class DummyScope implements Scope{

	double raBias, decBias, rightAscension, declination;
	
	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public void slew(final double ra, final double dec) throws ScopeException {
		final double wantRa = ra - raBias;
		final double wantDec = dec - decBias;
		
		new Thread() {
			
			public void run() {
				boolean doRa = true;
				boolean doDec = true;
				
				// Vitesse en degr�s
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
					
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
					}
					
				} while(doRa || doDec);
			}
			
		}.start();
		
	}

	@Override
	public void close() {
		
	}

	public double getRaBias() {
		return raBias;
	}

	public void setRaBias(double raBias) {
		this.raBias = raBias;
	}

	public double getDecBias() {
		return decBias;
	}

	public void setDecBias(double decBias) {
		this.decBias = decBias;
	}

	public double getRightAscension() {
		return rightAscension + raBias;
	}

	public double getDeclination() {
		return declination + decBias;
	}

	
}
