package fr.pludov.scopeexpress.camera;

public class CameraProperties {

	boolean canStopExposure;
	boolean canAbortExposure;
	boolean canFastReadout;
	boolean canGetCoolerPower;
	boolean canSetCCDTemperature;
	
	String sensorName;
	Double pixelSizeX, pixelSizeY;
	int maxBin;
	
	public CameraProperties() {
	}


	public boolean isCanStopExposure() {
		return canStopExposure;
	}


	public void setCanStopExposure(boolean canStopExposure) {
		this.canStopExposure = canStopExposure;
	}


	public boolean isCanAbortExposure() {
		return canAbortExposure;
	}


	public void setCanAbortExposure(boolean canAbortExposure) {
		this.canAbortExposure = canAbortExposure;
	}


	public boolean isCanFastReadout() {
		return canFastReadout;
	}


	public void setCanFastReadout(boolean canFastReadout) {
		this.canFastReadout = canFastReadout;
	}


	public boolean isCanGetCoolerPower() {
		return canGetCoolerPower;
	}


	public void setCanGetCoolerPower(boolean canGetCoolerPower) {
		this.canGetCoolerPower = canGetCoolerPower;
	}


	public boolean isCanSetCCDTemperature() {
		return canSetCCDTemperature;
	}


	public void setCanSetCCDTemperature(boolean canSetCCDTemperature) {
		this.canSetCCDTemperature = canSetCCDTemperature;
	}


	public String getSensorName() {
		return sensorName;
	}


	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
	}


	public Double getPixelSizeX() {
		return pixelSizeX;
	}


	public void setPixelSizeX(Double pixelSizeX) {
		this.pixelSizeX = pixelSizeX;
	}


	public Double getPixelSizeY() {
		return pixelSizeY;
	}


	public void setPixelSizeY(Double pixelSizeY) {
		this.pixelSizeY = pixelSizeY;
	}

	public int getMaxBin() {
		return maxBin;
	}

	public void setMaxBin(int maxBin) {
		this.maxBin = maxBin;
	}

}
