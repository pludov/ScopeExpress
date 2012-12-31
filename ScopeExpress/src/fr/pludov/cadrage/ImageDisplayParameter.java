package fr.pludov.cadrage;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.cadrage.utils.WeakListenerCollection;
import fr.pludov.io.CameraFrame;

public class ImageDisplayParameter implements Serializable {
	
	public static enum TransfertFunction {
		Linear,
		SquareRoot,
		CubeRoot
	};
	
	/**
	 * Cet objet contient les métadata utilisée sur l'objet image.
	 * La méthode equals permet de savoir si les métadatas utilisées ont changé.
	 */
	public static class ImageDisplayMetaDataInfo
	{
		public Double expositionDuration;
		public Integer iso;
		
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
	
	private TransfertFunction transfertFunction;
	
	// Durée d'exposition ciblée : multiplie l'image par targetExposition / duration 
	private Double targetExposition;
	
	// Niveau d'iso ciblé : multiplie l'image par targetIso / iso
	private Integer targetIso;

	private int zero;
	
	// En adu, éventuellement après scaling par targetExpo et targetIso
	private double [] low;
	private double [] median;
	private double [] high;

	private boolean autoHistogram;
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ImageDisplayParameter)) return false;
		
		ImageDisplayParameter other = (ImageDisplayParameter)obj;
		
		if (other.autoHistogram == autoHistogram) return false;
		if (other.channelMode != channelMode) return false;
		if (other.zero != zero) return false;
		if (other.transfertFunction != transfertFunction) return false;
		
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
	
	public class AduLevelMapper
	{
		final int black;
		final int maximum;
		final int channel;
		final double mul;
		
		short [] mapping;
		
		AduLevelMapper(int black, int maximum, int channel, double mul)
		{
			this.black = black;
			this.maximum = maximum;
			this.channel = channel;
			this.mul = mul;
		}
		
		boolean shareParameters(AduLevelMapper other)
		{
			return other.black == black
					&& other.maximum == maximum
					&& other.channel == channel
					&& other.mul == mul;
		}
		
		void init()
		{
			this.mapping = new short[maximum - black + 1];
			for(int i = 0; i < mapping.length; ++i)
			{
				this.mapping[i] = -1;
			}
		}
		
		public int getLevelForAdu(int adu)
		{
			if (adu < black) {
				// System.err.println("adu is bellow min");
				adu = black;
			}
			
			if (adu > maximum) {
			//	System.err.println("adu is over max");
				adu = maximum;
			}
			
			int cacheId = adu - black;
			
			int cacheValue = this.mapping[cacheId];
			if (cacheValue != -1) {
				return cacheValue;
			}
			
			int normalizeAdu = 65535 * (adu - black) / (maximum - black);
			cacheValue = getLevelForNormalizedAdu(normalizeAdu);
			this.mapping[cacheId] = (short)cacheValue;
			
			return cacheValue;
		}
		
		private int getLevelForNormalizedAdu(int adu)
		{
			// adu est nécessairement entre 
			// low => 0
			// high => 255
			// median => 128
			
			double adudouble;
			
			adudouble = adu;
			adudouble *= mul;
			if (!autoHistogram) adudouble -= zero;
			
			// adu est normalisé entre 0 - 65535. Au delà, c'est non spécifié...
			if (adudouble < 0) adudouble = 0;
			if (adudouble > 65535) adudouble = 65535;
			switch(transfertFunction)
			{
			case Linear:
				break;
//			case Logarithm:
//				// adu /= 65535.0;
//				adudouble = 65535.0 * Math.log(1 + adudouble) / Math.log( 1 + 65535);
//				break;
			case SquareRoot:
				adudouble = 65535.0 * Math.sqrt(adudouble) / Math.sqrt(65535.0);
				break;
			case CubeRoot:
				adudouble = 65535.0 * Math.pow(adudouble, 1.0/3) / Math.pow(65535.0, 1.0/3);
				break;
			default:
				throw new RuntimeException("unsupported transfert function");
			}
			
			if (!autoHistogram) {
				if (adudouble < low[channel]) return 0;
				if (adudouble < median[channel])
				{
					int ret = (int)((adudouble - low[channel]) * 128.0 / (median[channel] - low[channel]));
					return ret;
				} else if (adudouble < high[channel]) {
					int ret = (int)(128.0 + (adudouble - median[channel]) * 128.0 / (high[channel] - median[channel]));
					if (ret > 255) ret = 255;
					return ret;
				} else {
					return 255;
				}
			} else {
				if (adudouble < 0) return 0;
				if (adudouble > 65535) return 0;
				int rslt = (int)Math.round(adudouble / 256);
				
				if (rslt > 255) return 255;
				return rslt;
			}
		}
	}
	
	final List<SoftReference<AduLevelMapper>> mappers = new ArrayList<SoftReference<AduLevelMapper>>();
	
	private void clearAduLevelCache()
	{
		mappers.clear();
	}
	
	public AduLevelMapper getAduLevelMapper(CameraFrame source, ImageDisplayMetaDataInfo frame, int channel)
	{
		double mul = 1.0;
					
		
		if (this.targetIso != null && frame.iso != null)
		{
			mul = this.targetIso * 1.0 / frame.iso;
		}
		
		if (this.targetExposition != null && frame.expositionDuration != null)
		{
			mul *= this.targetExposition * 1.0 / frame.expositionDuration;
		}
		
		int black, max;

		if (autoHistogram) {
			black = source.getAduForHistogramPos(channel, 0.5);
			max = source.getAduForHistogramPos(channel, 0.99999);
			if (max == black)  {
				max = black + 1;
			}
		} else {
			black = source.getBlack();
			max = source.getMaximum();
		}
		AduLevelMapper result = new AduLevelMapper(black, max, channel, mul);
		for(Iterator<SoftReference<AduLevelMapper>> it = mappers.iterator(); it.hasNext(); )
		{
			SoftReference<AduLevelMapper> softref = it.next();
			AduLevelMapper mapper = softref.get();
			
			if (mapper == null) {
				it.remove();
			} else {
				if (mapper.shareParameters(result)) {
					return mapper;
				}
			}
		}
		result.init();
		mappers.add(new SoftReference<ImageDisplayParameter.AduLevelMapper>(result));
		return result;
	}
	
	public ImageDisplayParameter()
	{
		this.listeners = new WeakListenerCollection<ImageDisplayParameterListener>(ImageDisplayParameterListener.class);
		this.channelMode = ChannelMode.Color;
		this.transfertFunction = TransfertFunction.Linear;
		this.targetExposition = null;
		this.targetIso = null;
		this.low = new double[] { 0, 0, 0};
		this.high = new double[] { 65535, 65535, 65535 };
		this.median = new double[] { 32767, 32767, 32767};
		this.autoHistogram = false;
	}
	
	public ImageDisplayParameter(ImageDisplayParameter copy)
	{
		this.listeners = new WeakListenerCollection<ImageDisplayParameterListener>(ImageDisplayParameterListener.class);
		this.channelMode = copy.getChannelMode();
		this.transfertFunction = copy.transfertFunction;
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
		
		this.autoHistogram = copy.autoHistogram;
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
	    this.listeners = new WeakListenerCollection<ImageDisplayParameterListener>(ImageDisplayParameterListener.class);
	    
	    clearAduLevelCache();
	}


	public ChannelMode getChannelMode() {
		return channelMode;
	}


	public void setChannelMode(ChannelMode channelMode) {
		if (this.channelMode == channelMode) return;
		this.channelMode = channelMode;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged();
	}


	public Double getTargetExposition() {
		return targetExposition;
	}


	public void setTargetExposition(Double targetExposition) {
		if (Utils.equalsWithNullity(this.targetExposition, targetExposition)) return;
		this.targetExposition = targetExposition;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged();
	}


	public Integer getTargetIso() {
		return targetIso;
	}


	public void setTargetIso(Integer targetIso) {
		if (Utils.equalsWithNullity(this.targetIso, targetIso)) return;
		this.targetIso = targetIso;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged();
	}


	public double[] getLow() {
		return low;
	}


	public void setLow(int channel, double low) {
		if (this.low[channel] == low) return;
		this.low[channel] = low;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged();
	}


	public double[] getMedian() {
		return median;
	}


	public void setMedian(int channel, double median) {
		if (this.median[channel] == median) return;
		this.median[channel] = median;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged();
	}


	public double[] getHigh() {
		return high;
	}


	public void setHigh(int channel, double high) {
		if (this.high[channel] == high) return;
		this.high[channel] = high;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged();
	}


	public int getZero() {
		return zero;
	}


	public void setZero(int zero) {
		if (this.zero == zero) return;
		this.zero = zero;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged();
	}

	public TransfertFunction getTransfertFunction() {
		return transfertFunction;
	}

	public void setTransfertFunction(TransfertFunction transfertFunction) {
		if (this.transfertFunction == transfertFunction) return;
		this.transfertFunction = transfertFunction;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged();
	}

	public boolean isAutoHistogram() {
		return autoHistogram;
	}

	public void setAutoHistogram(boolean autoHistogram) {
		if (this.autoHistogram == autoHistogram) return;
		this.autoHistogram = autoHistogram;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged();
	}
}
