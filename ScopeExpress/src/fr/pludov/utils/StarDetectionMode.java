package fr.pludov.utils;

import fr.pludov.scopeexpress.ImageDisplayParameter;

public abstract class StarDetectionMode {
	public final int channelCount;
	public 	final ChannelMode [] channels;
	public final ImageDisplayParameter.ChannelMode displayMode;
	
	StarDetectionMode(int channelCount, ImageDisplayParameter.ChannelMode displayMode, ChannelMode ... channels) {
		this.channelCount = channelCount;
		this.displayMode = displayMode;
		this.channels = channels;
	}
	
	abstract int getChannelId(int x, int y);
}