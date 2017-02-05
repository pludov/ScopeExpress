package fr.pludov.scopeexpress.camera;

import java.util.*;

public class CameraProperties {

	boolean canStopExposure;
	boolean canAbortExposure;
	boolean canFastReadout;
	boolean canGetCoolerPower;
	boolean canSetCCDTemperature;
	/** Null == pas supporté */
	List<String> gains;
	
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


	public List<String> getGains()
	{
		return gains != null ? Collections.unmodifiableList(gains) : null;
	}
	
	public void setGains(List<String> gains) {
		this.gains = gains != null ? new ArrayList<>(gains): null;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (canAbortExposure ? 1231 : 1237);
		result = prime * result + (canFastReadout ? 1231 : 1237);
		result = prime * result + (canGetCoolerPower ? 1231 : 1237);
		result = prime * result + (canSetCCDTemperature ? 1231 : 1237);
		result = prime * result + (canStopExposure ? 1231 : 1237);
		result = prime * result + ((gains == null) ? 0 : gains.hashCode());
		result = prime * result + maxBin;
		result = prime * result + ((pixelSizeX == null) ? 0 : pixelSizeX.hashCode());
		result = prime * result + ((pixelSizeY == null) ? 0 : pixelSizeY.hashCode());
		result = prime * result + ((sensorName == null) ? 0 : sensorName.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CameraProperties other = (CameraProperties) obj;
		if (canAbortExposure != other.canAbortExposure)
			return false;
		if (canFastReadout != other.canFastReadout)
			return false;
		if (canGetCoolerPower != other.canGetCoolerPower)
			return false;
		if (canSetCCDTemperature != other.canSetCCDTemperature)
			return false;
		if (canStopExposure != other.canStopExposure)
			return false;
		if (gains == null) {
			if (other.gains != null)
				return false;
		} else if (!gains.equals(other.gains))
			return false;
		if (maxBin != other.maxBin)
			return false;
		if (pixelSizeX == null) {
			if (other.pixelSizeX != null)
				return false;
		} else if (!pixelSizeX.equals(other.pixelSizeX))
			return false;
		if (pixelSizeY == null) {
			if (other.pixelSizeY != null)
				return false;
		} else if (!pixelSizeY.equals(other.pixelSizeY))
			return false;
		if (sensorName == null) {
			if (other.sensorName != null)
				return false;
		} else if (!sensorName.equals(other.sensorName))
			return false;
		return true;
	}

}
