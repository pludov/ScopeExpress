package fr.pludov.scopeexpress.camera;

/**
 * Les paramètres d'un shoot. 
 * Pour chaque nullable, null, signifie ne pas toucher
 *
 */
public class ShootParameters {
	// La durée du shoot
	double exp;
	String gain;
	Integer readoutMode;
	Integer binx, biny;
	String path;
	String fileName;
	
	/** will go in fit header */	
	ImageType type;
	/** If type is not expressive enough */
	String phase;
	
	Object correlator;
	
	public ShootParameters() {
	}

	public ShootParameters(ShootParameters parameters) {
		this.exp = parameters.exp;
		this.gain = parameters.gain;
		this.readoutMode = parameters.readoutMode;
		this.binx = parameters.binx;
		this.biny = parameters.biny;
		this.path = parameters.path;
		this.fileName = parameters.fileName;
		this.phase = parameters.phase;
		this.type = parameters.type;
		this.correlator = parameters.correlator;
	}

	public double getExp() {
		return exp;
	}

	public void setExp(double exp) {
		this.exp = exp;
	}

	public String getGain() {
		return gain;
	}

	public void setGain(String val) {
		this.gain = val;
	}

	public Integer getReadoutMode() {
		return readoutMode;
	}

	public void setReadoutMode(Integer readoutMode) {
		this.readoutMode = readoutMode;
	}

	public Integer getBinx() {
		return binx;
	}

	public void setBinx(Integer binx) {
		this.binx = binx;
	}

	public Integer getBiny() {
		return biny;
	}

	public void setBiny(Integer biny) {
		this.biny = biny;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public ImageType getType() {
		return type;
	}

	public void setType(ImageType type) {
		this.type = type;
	}

	public String getPhase() {
		return phase;
	}

	public void setPhase(String phase) {
		this.phase = phase;
	}

	public Object getCorrelator() {
		return correlator;
	}

	public void setCorrelator(Object correlator) {
		this.correlator = correlator;
	}

}
