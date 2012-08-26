package fr.pludov.cadrage;

import java.io.IOException;
import java.io.Serializable;

import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.cadrage.utils.WeakListenerCollection;

public class ImageDisplayParameter implements Serializable {
	
	/**
	 * Cet objet contient les métadata utilisée sur l'objet image.
	 * La méthode equals permet de savoir si les métadatas utilisées ont changé.
	 */
	public static class ImageDisplayMetaDataInfo
	{
		Double expositionDuration;
		Integer iso;
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ImageDisplayMetaDataInfo)) return false;
			ImageDisplayMetaDataInfo other = (ImageDisplayMetaDataInfo)obj;
			return Utils.equalsWithNullity(this.expositionDuration, other.expositionDuration)
					&& Utils.equalsWithNullity(this.iso,  other.iso);
		}
	}
	
	private static final long serialVersionUID = -8001279656594732796L;

	public transient WeakListenerCollection<ImageDisplayParameterListener> listeners;

	public enum ChannelMode { Color, GreyScale, NarrowRed, NarrowGreen, NarrowBlue }; 
	
	private ChannelMode channelMode;
	
	// Durée d'exposition ciblée : multiplie l'image par targetExposition / duration 
	private Double targetExposition;
	
	// Niveau d'iso ciblé : multiplie l'image par targetIso / iso
	private Integer targetIso;

	private int zero;
	
	// En adu, éventuellement après scaling par targetExpo et targetIso
	private double [] low;
	private double [] median;
	private double [] high;

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ImageDisplayParameter)) return false;
		
		ImageDisplayParameter other = (ImageDisplayParameter)obj;
		
		if (other.channelMode != channelMode) return false;
		if (other.zero != zero) return false;
		
		if (((other.targetExposition == null) != (this.targetExposition == null))
				|| (this.targetExposition != null && !this.targetExposition.equals(other.targetExposition))) {
			return false;
		}
		
		if (((other.targetIso == null) != (this.targetIso == null))
				|| (this.targetIso != null && !this.targetIso.equals(other.targetIso))) {
			return false;
		}
		
		for(int i = 0; i < 3; ++i)
		{
			if (this.low[i] != other.low[i]) return false;
			if (this.median[i] != other.median[i]) return false;
			if (this.high[i] != other.high[i]) return false;
		}
		return true;
	}
	
	/**
	 * Retourne un objet contenant l'ensemble des méta data qui seront utilisée
	 * pour calculer l'image d'une frame. Permet de comparer par la suite les métadata...
	 */
	public ImageDisplayMetaDataInfo getMetadataInUse(Image frame)
	{
		ImageDisplayMetaDataInfo result = new ImageDisplayMetaDataInfo();
		if (this.targetIso != null) result.iso = frame.getIso();
		if (this.targetExposition != null) result.expositionDuration = frame.getPause();
		return result;
	}
	
	public int getLevelForAdu(ImageDisplayMetaDataInfo frame, int channel, int adu)
	{
		// low => 0
		// high => 255
		// median => 128
		adu -= zero;
		double mul = 1.0;
		if (this.targetIso != null && frame.iso != null)
		{
			mul = this.targetIso * 1.0 / frame.iso;
		}
		
		if (this.targetExposition != null && frame.expositionDuration != null)
		{
			mul *= this.targetExposition * 1.0 / frame.expositionDuration;
		}
		
		
		// FIXME: ça doit disparaitre...
		adu *= mul;
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
		this.zero = copy.zero;
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
		if (this.channelMode == channelMode) return;
		this.channelMode = channelMode;
		listeners.getTarget().parameterChanged();
	}


	public Double getTargetExposition() {
		return targetExposition;
	}


	public void setTargetExposition(Double targetExposition) {
		if (Utils.equalsWithNullity(this.targetExposition, targetExposition)) return;
		this.targetExposition = targetExposition;
		listeners.getTarget().parameterChanged();
	}


	public Integer getTargetIso() {
		return targetIso;
	}


	public void setTargetIso(Integer targetIso) {
		if (Utils.equalsWithNullity(this.targetIso, targetIso)) return;
		this.targetIso = targetIso;
		listeners.getTarget().parameterChanged();
	}


	public double[] getLow() {
		return low;
	}


	public void setLow(int channel, double low) {
		if (this.low[channel] == low) return;
		this.low[channel] = low;
		listeners.getTarget().parameterChanged();
	}


	public double[] getMedian() {
		return median;
	}


	public void setMedian(int channel, double median) {
		if (this.median[channel] == median) return;
		this.median[channel] = median;
		listeners.getTarget().parameterChanged();
	}


	public double[] getHigh() {
		return high;
	}


	public void setHigh(int channel, double high) {
		if (this.high[channel] == high) return;
		this.high[channel] = high;
		listeners.getTarget().parameterChanged();
	}


	public int getZero() {
		return zero;
	}


	public void setZero(int zero) {
		if (this.zero == zero) return;
		this.zero = zero;
		listeners.getTarget().parameterChanged();
	}
}
