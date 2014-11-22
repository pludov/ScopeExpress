package fr.pludov.scopeexpress.focus;

import fr.pludov.io.CameraFrameMetadata;

public class DarkRequest {
	
	public DarkRequest(CameraFrameMetadata metadata) {
		this.instrument = metadata.getInstrument();
		this.binX = metadata.getBinX();
		this.binY = metadata.getBinY();
		this.duration = metadata.getDuration();
		this.temp = metadata.getCcdTemp();
	}
	
	// Doit matcher exactement
	String instrument;
	// Doit matcher exactement
	Integer binX, binY;
	
	
	Double duration;
	// Comme le bruit double tous les 6°, calcule 2^(X + T1 - T2), soit 2^X * 2^(T2 - T1)
	Double temp;
	

}
