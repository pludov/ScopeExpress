package fr.pludov.cadrage;

import java.io.IOException;
import java.io.Serializable;

import fr.pludov.cadrage.utils.WeakListenerCollection;

public class ImageDisplayParameter implements Serializable {
	
	private static final long serialVersionUID = -8001279656594732796L;

	public transient WeakListenerCollection<ImageDisplayParameterListener> listeners;

	public enum ChannelMode { Color, GreyScale, NarrowRed, NarrowGreen, NarrowBlue }; 
	
	public ChannelMode channelMode;
	
	// Durée d'exposition ciblée : multiplie l'image par targetExposition / duration 
	public Double targetExposition;
	
	// Niveau d'iso ciblé : multiplie l'image par targetIso / iso
	public Integer targetIso;
	
	// En adu, éventuellement après scaling par targetExpo et targetIso
	public double [] low;
	public double [] median;
	public double [] high;

	public int getLevelForAdu(int channel, int adu)
	{
		// low => 0
		// high => 255
		// median => 128
		
		// FIXME: ça doit disparaitre...
		adu *= 16;
		if (adu < low[channel]) return 0;
		if (adu < median[channel])
		{
			int ret = (int)((adu - low[channel]) * 128.0 / (median[channel] - low[channel]));
			return ret;
		} else {
			int ret = (int)(128.0 + (adu - median[channel]) * 128.0 / (high[channel] - median[channel]));
			if (ret > 255) ret = 255;
			return ret;
		}
	}
	
	
	public ImageDisplayParameter()
	{
		this.listeners = new WeakListenerCollection<ImageDisplayParameterListener>(ImageDisplayParameterListener.class);
		this.channelMode = ChannelMode.Color;
		this.targetExposition = null;
		this.targetIso = null;
		this.low = new double[] { 0, 0, 0};
		this.high = new double[] { 65535, 65535, 65535 };
		this.median = new double[] { 16384, 16384, 16384 };
	}
	
	public ImageDisplayParameter(ImageDisplayParameter copy)
	{
		this.listeners = new WeakListenerCollection<ImageDisplayParameterListener>(ImageDisplayParameterListener.class);
		this.channelMode = copy.getChannelMode();
		this.targetExposition = copy.getTargetExposition();
		this.targetIso = copy.getTargetIso();
		this.low = new double[] { 0, 0, 0};
		this.high = new double[] { 65535, 65535, 65535 };
		this.median = new double[] { 16384, 16384, 16384 };
		
		for(int i = 0; i < 3; ++i)
		{
			this.low[i] = copy.low[i];
			this.high[i] = copy.high[i];
			this.median[i] = copy.median[i];
		}
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
	    this.listeners = new WeakListenerCollection<ImageDisplayParameterListener>(ImageDisplayParameterListener.class);
	}


	public ChannelMode getChannelMode() {
		return channelMode;
	}


	public void setChannelMode(ChannelMode channelMode) {
		this.channelMode = channelMode;
	}


	public Double getTargetExposition() {
		return targetExposition;
	}


	public void setTargetExposition(Double targetExposition) {
		this.targetExposition = targetExposition;
	}


	public Integer getTargetIso() {
		return targetIso;
	}


	public void setTargetIso(Integer targetIso) {
		this.targetIso = targetIso;
	}


	public double[] getLow() {
		return low;
	}


	public void setLow(double[] low) {
		this.low = low;
	}


	public double[] getMedian() {
		return median;
	}


	public void setMedian(double[] median) {
		this.median = median;
	}


	public double[] getHigh() {
		return high;
	}


	public void setHigh(double[] high) {
		this.high = high;
	}
}
