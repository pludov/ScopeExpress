package fr.pludov.scopeexpress.camera;

public class TemperatureParameters {
	boolean coolerOn;
	Double coolerPower;
	Double CCDTemperature;
	Double SetCCDTemperature;
	Double heatSinkTemperature;
	
	public TemperatureParameters() {
	}

	public boolean isCoolerOn() {
		return coolerOn;
	}

	public void setCoolerOn(boolean coolerOn) {
		this.coolerOn = coolerOn;
	}

	public Double getCoolerPower() {
		return coolerPower;
	}

	public void setCoolerPower(Double coolerPower) {
		this.coolerPower = coolerPower;
	}

	public Double getCCDTemperature() {
		return CCDTemperature;
	}

	public void setCCDTemperature(Double cCDTemperature) {
		CCDTemperature = cCDTemperature;
	}

	public Double getSetCCDTemperature() {
		return SetCCDTemperature;
	}

	public void setSetCCDTemperature(Double setCCDTemperature) {
		SetCCDTemperature = setCCDTemperature;
	}

	public Double getHeatSinkTemperature() {
		return heatSinkTemperature;
	}

	public void setHeatSinkTemperature(Double heatSinkTemperature) {
		this.heatSinkTemperature = heatSinkTemperature;
	}

}
