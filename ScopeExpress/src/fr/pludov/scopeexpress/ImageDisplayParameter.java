package fr.pludov.scopeexpress;

import java.io.*;
import java.lang.ref.*;
import java.util.*;

import fr.pludov.scopeexpress.focus.*;
import fr.pludov.scopeexpress.utils.*;
import fr.pludov.scopeexpress.utils.cache.*;

public class ImageDisplayParameter implements Serializable, Cloneable {
	
	private static final long serialVersionUID = -8001279656594732796L;

	public transient WeakListenerCollection<ImageDisplayParameterListener> listeners;

	public enum ChannelMode { 
		Color(null), 
		GreyScale(java.awt.Color.white), 
		NarrowRed(java.awt.Color.red), 
		NarrowGreen(java.awt.Color.green),
		NarrowBlue(java.awt.Color.blue);
		
		public final java.awt.Color displayColor;
		ChannelMode(java.awt.Color color)
		{
			this.displayColor = color;
		}
		
	}; 
	
	private ChannelMode channelMode;
		
	// En adu, éventuellement après scaling par targetExpo et targetIso
	private double [] low;
	private double [] median;
	private double [] high;

	private boolean autoHistogram;
	
	private boolean darkEnabled;
	
	private Image darkFrame;
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ImageDisplayParameter)) return false;
		
		ImageDisplayParameter other = (ImageDisplayParameter)obj;
		if ((other.darkFrame == null) != (this.darkFrame == null)) return false;
		if (other.darkFrame != null && !other.darkFrame.equals(this.darkFrame)) return false;
		if (other.autoHistogram != autoHistogram) return false;
		if (other.channelMode != channelMode) return false;
		if (other.darkEnabled != darkEnabled) return false;
		
		for(int i = 0; i < 3; ++i)
		{
			if (this.low[i] != other.low[i]) return false;
			if (this.median[i] != other.median[i]) return false;
			if (this.high[i] != other.high[i]) return false;
		}
		return true;
	}
	
	public boolean equalsExceptAutoHistValue(Object obj) {
		if (!(obj instanceof ImageDisplayParameter)) return false;
		
		ImageDisplayParameter other = (ImageDisplayParameter)obj;
		if ((other.darkFrame == null) != (this.darkFrame == null)) return false;
		if (other.darkFrame != null && !other.darkFrame.equals(this.darkFrame)) return false;
		if (other.autoHistogram != autoHistogram) return false;
		if (other.channelMode != channelMode) return false;
		if (other.darkEnabled != darkEnabled) return false;
		if (!autoHistogram) {
			for(int i = 0; i < 3; ++i)
			{
				if (this.low[i] != other.low[i]) return false;
				if (this.median[i] != other.median[i]) return false;
				if (this.high[i] != other.high[i]) return false;
			}
		}
		return true;
	}
	
	
	
	private static final int hashDouble(double d)
	{
		long l = Double.doubleToLongBits(d);
		return (int)(l ^ (l >> 32));
	}
	
	@Override
	public int hashCode() {
		int result = 1;
		if (autoHistogram) result ^= 238;
		if (darkEnabled) result ^= 3423948;
		result ^= channelMode.hashCode();
		if (this.darkFrame != null) {
			result ^= darkFrame.hashCode();
		}
		if (!autoHistogram) {
			for(int i = 0; i < 3; ++i)
			{
				result ^= hashDouble(this.low[i]);
				result ^= hashDouble(this.median[i]);
				result ^= hashDouble(this.high[i]);
			}
		}
		return result;
	}
	
	@Override
	public ImageDisplayParameter clone() {
		ImageDisplayParameter result = new ImageDisplayParameter();
		
		result.channelMode = this.channelMode;
		result.low = Arrays.copyOf(this.low, this.low.length);
		result.median = Arrays.copyOf(this.median, this.median.length);
		result.high = Arrays.copyOf(this.high, this.high.length);
		result.autoHistogram = this.autoHistogram;
		result.darkFrame = this.darkFrame;
		result.darkEnabled = this.darkEnabled;
		return result;
	}
	
	public static class AduLevelMapperId
	{
		final double black;
		final double medium;
		final double maximum;
		final int channel;
		int hashCode;

		AduLevelMapperId(double black, double medium, double maximum, int channel)
		{
			this.black = black;
			this.medium = medium;
			this.maximum = maximum;
			this.channel = channel;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof AduLevelMapperId)) return false;
			AduLevelMapperId other = (AduLevelMapperId)obj;
			return other.black == this.black
					&& other.medium == this.medium
					&& other.maximum == this.maximum
					&& other.channel == this.channel;
		}
		
		@Override
		public int hashCode() {
			if (hashCode == -1) {
				hashCode = channel
						^new Double(black).hashCode()
						^new Double(medium).hashCode()
						^new Double (maximum).hashCode();
			}
			return hashCode;
		}

		public double getBlack() {
			return black;
		}

		public double getMaximum() {
			return maximum;
		}

		public double getMedium() {
			return medium;
		}

		public int getChannel() {
			return channel;
		}

		public AduLevelMapperId normalize() {
			if (black <= medium && medium <= maximum) {
				return this;
			}
			double nvmed = medium;
			if (nvmed < black) {
				nvmed = black;
			}
			double nvmax = maximum;
			if (nvmax < medium) {
				nvmax = medium;
			}
			return new AduLevelMapperId(black, nvmed, nvmax, channel);
		}
	}
	
	public class AduLevelMapper
	{
		final AduLevelMapperId id;
		double gamma;
		short [] mapping;
		
		AduLevelMapper(AduLevelMapperId id)
		{
			this.id = id.normalize();
			
		}
		
		void init()
		{
			this.mapping = new short[(int)Math.floor(id.getMaximum() - id.getBlack()) + 1];
			for(int i = 0; i < mapping.length; ++i)
			{
				this.mapping[i] = -1;
			}
			// (medium - black) / (max - black) ^ gamma = 0.5
			// log((med - black)/(max - black) ^ gamma) = log(0.5)
			// gamma . log((med - black)/(max - black)) = log(0.5)
			// gamma = log(0.5) / log((med - black)/(max - black))
			this.gamma = Math.log(0.5) / Math.log((id.getMedium() - id.getBlack()) / (id.getMaximum() - id.getBlack()));
			//gamma = 1.0;
		}
		
		public int getLevelForAdu(int iAdu)
		{
			double adu = iAdu;
			if (adu < id.getBlack()) {
				adu = id.getBlack();
			}
			
			if (adu > id.getMaximum()) {
				adu = id.getMaximum();
			}
			
			int cacheId = (int)Math.floor(adu - id.getBlack());
			
			int cacheValue = this.mapping[cacheId];
			if (cacheValue != -1) {
				return cacheValue;
			}
			
			// Entre 0 et 1
			double normalised = (adu - id.getBlack()) / (id.getMaximum() - id.getBlack());
			double m = (id.getMedium() - id.getBlack()) / (id.getMaximum() - id.getBlack());
			
			
			double v = (m-1) * normalised / (( 2 * m - 1) * normalised - m);
			
			double gammaAdjusted = 256.0 * v;
			// Applique le gamma pour que min mène à 50%
			int result = (int)Math.floor(gammaAdjusted);
			
			if (result > 255) {
				result = 255;
			}
			this.mapping[cacheId] = (short)result;
			
			return result;
		}
//		
//		private int getLevelForNormalizedAdu(int adu)
//		{
//			// adu est nécessairement entre 
//			// low => 0
//			// high => 255
//			// median => 128
//			
//			double adudouble;
//			
//			adudouble = adu;
//			adudouble *= id.getMul();
//			if (!autoHistogram) adudouble -= zero;
//			
//			// adu est normalisé entre 0 - 65535. Au delà, c'est non spécifié...
//			if (adudouble < 0) adudouble = 0;
//			if (adudouble > 65535) adudouble = 65535;
//			switch(transfertFunction)
//			{
//			case Linear:
//				break;
////			case Logarithm:
////				// adu /= 65535.0;
////				adudouble = 65535.0 * Math.log(1 + adudouble) / Math.log( 1 + 65535);
////				break;
//			case SquareRoot:
//				adudouble = 65535.0 * Math.sqrt(adudouble) / Math.sqrt(65535.0);
//				break;
//			case CubeRoot:
//				adudouble = 65535.0 * Math.pow(adudouble, 1.0/3) / Math.pow(65535.0, 1.0/3);
//				break;
//			default:
//				throw new RuntimeException("unsupported transfert function");
//			}
//			
//			if (!autoHistogram) {
//				if (adudouble < low[id.getChannel()]) return 0;
//				if (adudouble < median[id.getChannel()])
//				{
//					int ret = (int)((adudouble - low[id.getChannel()]) * 128.0 / (median[id.getChannel()] - low[id.getChannel()]));
//					return ret;
//				} else if (adudouble < high[id.getChannel()]) {
//					int ret = (int)(128.0 + (adudouble - median[id.getChannel()]) * 128.0 / (high[id.getChannel()] - median[id.getChannel()]));
//					if (ret > 255) ret = 255;
//					return ret;
//				} else {
//					return 255;
//				}
//			} else {
//				if (adudouble < 0) return 0;
//				if (adudouble > 65535) return 0;
//				int rslt = (int)Math.round(adudouble / 256);
//				
//				if (rslt > 255) return 255;
//				return rslt;
//			}
//		}
	}
	
	final List<SoftReference<AduLevelMapper>> mappers = new ArrayList<SoftReference<AduLevelMapper>>();
	
	final Cache<AduLevelMapperId, AduLevelMapper> levelMapperCache = new Cache<AduLevelMapperId, AduLevelMapper>() {
			@Override
			public AduLevelMapper produce(AduLevelMapperId identifier) {
				AduLevelMapper result = new AduLevelMapper(identifier);
				result.init();
				return result;
			}	
	};
	
	private void clearAduLevelCache()
	{
		mappers.clear();
	}
	
	public AduLevelMapper getAduLevelMapper(int channel)
	{
//		double mul = 1.0;
//		
//		if (this.targetIso != null && frame.iso != null)
//		{
//			mul = this.targetIso * 1.0 / frame.iso;
//		}
//		
//		if (this.targetExposition != null && frame.expositionDuration != null)
//		{
//			mul *= this.targetExposition * 1.0 / frame.expositionDuration;
//		}
//		
//		int black, max;
//
////		if (autoHistogram) {
//
//			black = (int)Math.round(this.low[channel]);
//			max = (int)Math.round(this.high[channel]);
//			if (max == black)  {
//				max = black + 1;
//			}
////		} else {
////			black = source.getBlack();
////			max = source.getMaximum();
////		}
		
		double black = this.low[channel];
		double median = this.median[channel];
		double high = this.high[channel];
		AduLevelMapperId id = new AduLevelMapperId(black, median, high, channel);
		
		return levelMapperCache.get(id);
	}
	
	public ImageDisplayParameter()
	{
		this.listeners = new WeakListenerCollection<ImageDisplayParameterListener>(ImageDisplayParameterListener.class);
		this.channelMode = ChannelMode.Color;
		this.low = new double[] { 0, 0, 0};
		this.high = new double[] { 65535, 65535, 65535 };
		this.median = new double[] { 32767, 32767, 32767};
		this.autoHistogram = true;
		this.darkEnabled = true;
	}
	
	public ImageDisplayParameter(ImageDisplayParameter copy)
	{
		this.listeners = new WeakListenerCollection<ImageDisplayParameterListener>(ImageDisplayParameterListener.class);
		this.channelMode = copy.getChannelMode();
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
		this.darkFrame = copy.darkFrame;
		this.darkEnabled = copy.darkEnabled;
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
	    this.listeners = new WeakListenerCollection<ImageDisplayParameterListener>(ImageDisplayParameterListener.class);
	    
	    clearAduLevelCache();
	}

	public void copyFrom(ImageDisplayParameter idp)
	{
		if (this.equals(idp)) {
			return;
		}
		ImageDisplayParameter previous = new ImageDisplayParameter(this);
		this.darkFrame = idp.darkFrame;
		this.channelMode = idp.getChannelMode();
		
		for(int i = 0; i < 3; ++i)
		{
			this.low[i] = idp.low[i];
			this.high[i] = idp.high[i];
			this.median[i] = idp.median[i];
		}
		
		this.autoHistogram = idp.autoHistogram;
		this.darkEnabled = idp.darkEnabled;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged(previous, this);
	}
	

	public ChannelMode getChannelMode() {
		return channelMode;
	}


	public void setChannelMode(ChannelMode channelMode) {
		if (this.channelMode == channelMode) return;
		ImageDisplayParameter previous = new ImageDisplayParameter(this);
		this.channelMode = channelMode;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged(previous, this);
	}


	public double[] getLow() {
		return low;
	}


	public void setLow(int channel, double low) {
		if (this.low[channel] == low) return;
		ImageDisplayParameter previous = new ImageDisplayParameter(this);
		this.low[channel] = low;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged(previous, this);
	}


	public double[] getMedian() {
		return median;
	}


	public void setMedian(int channel, double median) {
		if (this.median[channel] == median) return;
		ImageDisplayParameter previous = new ImageDisplayParameter(this);
		this.median[channel] = median;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged(previous, this);
	}


	public double[] getHigh() {
		return high;
	}


	public void setHigh(int channel, double high) {
		if (this.high[channel] == high) return;
		ImageDisplayParameter previous = new ImageDisplayParameter(this);
		this.high[channel] = high;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged(previous, this);
	}

	public boolean isAutoHistogram() {
		return autoHistogram;
	}

	public void setAutoHistogram(boolean autoHistogram) {
		if (this.autoHistogram == autoHistogram) return;
		ImageDisplayParameter previous = new ImageDisplayParameter(this);
		this.autoHistogram = autoHistogram;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged(previous, this);
	}

	public Image getDarkFrame() {
		return darkFrame;
	}

	public void setDarkFrame(Image darkFrame) {
		if (Objects.equals(this.darkFrame, darkFrame)) return;
		
		ImageDisplayParameter previous = new ImageDisplayParameter(this);
		this.darkFrame = darkFrame;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged(previous, this);
	}

	public double[] getLowMedHigh(int index) {
		if (index == 0) {
			return getLow();
		} else if (index == 1) {
			return getMedian();
		} else {
			return getHigh();
		}
	}

	public boolean isDarkEnabled() {
		return darkEnabled;
	}

	public void setDarkEnabled(boolean darkEnabled) {
		if (this.darkEnabled == darkEnabled) return;
		ImageDisplayParameter previous = new ImageDisplayParameter(this);
		this.darkEnabled = darkEnabled;
		clearAduLevelCache();
		listeners.getTarget().parameterChanged(previous, this);
	}

	public void resetChannelToneMapping() {
		for(int i = 0; i < low.length; ++i)
		{
			low[i] = 0;
		}
		for(int i = 0; i < median.length; ++i)
		{
			median[i] = 32768;
		}
		for(int i = 0; i < high.length; ++i)
		{
			high[i] = 65535;
		}
		
	}
}
