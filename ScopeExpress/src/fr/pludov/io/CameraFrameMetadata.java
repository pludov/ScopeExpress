package fr.pludov.io;

public class CameraFrameMetadata {
	Double gain;
	Double duration;
	Integer binX, binY;
	/** Heure de début en epoch ms */
	Long startMsEpoch;
	Double ccdTemp;
	String instrument;
	// Size, after binning
	Double pixSizeX, pixSizeY;
	
	public Double getGain() {
		return gain;
	}
	public void setGain(Double gain) {
		this.gain = gain;
	}
	public Double getDuration() {
		return duration;
	}
	public void setDuration(Double duration) {
		this.duration = duration;
	}
	public Integer getBinX() {
		return binX;
	}
	public void setBinX(Integer binX) {
		this.binX = binX;
	}
	public Integer getBinY() {
		return binY;
	}
	public void setBinY(Integer binY) {
		this.binY = binY;
	}
	public Long getStartMsEpoch() {
		return startMsEpoch;
	}
	public void setStartMsEpoch(Long startMsEpoch) {
		this.startMsEpoch = startMsEpoch;
	}
	public String getInstrument() {
		return instrument;
	}
	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}
	public void setCcdTemp(Double v) {
		this.ccdTemp = v;
	}
	public Double getCcdTemp() {
		return ccdTemp;
	}
	public Double getPixSizeX() {
		return pixSizeX;
	}
	public void setPixSizeX(Double pixSizeX) {
		this.pixSizeX = pixSizeX;
	}
	public Double getPixSizeY() {
		return pixSizeY;
	}
	public void setPixSizeY(Double pixSizeY) {
		this.pixSizeY = pixSizeY;
	}
	
	
}
