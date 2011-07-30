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
				do {
					if (doRa) {
						double delta = wantRa - rightAscension;
						if (delta > 0.01) {
							delta = 0.01;
						} else {
							doRa = false;
						}
						
						rightAscension += delta;
					}
					
					if (doDec) {
						double delta = wantDec - declination;
						if (delta > 0.01) {
							delta = 0.01;
						} else {
							doRa = false;
						}
						
						declination += delta;
					}
					
					try {
						Thread.sleep(100);
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
