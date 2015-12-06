package fr.pludov.scopeexpress.camera;

/**
 * Les paramètres d'un shoot. 
 * Pour chaque nullable, null, signifie ne pas toucher
 *
 */
public class ShootParameters {
	// La durée du shoot
	double exp;
	Double gain;
	Integer readoutMode;
	Integer binx, biny;
	String path;
	String fileName;
	
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
	}

	public double getExp() {
		return exp;
	}

	public void setExp(double exp) {
		this.exp = exp;
	}

	public Double getGain() {
		return gain;
	}

	public void setGain(Double gain) {
		this.gain = gain;
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

}
