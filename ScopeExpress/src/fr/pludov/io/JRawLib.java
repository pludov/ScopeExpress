package fr.pludov.io;

import java.io.File;

import org.apache.log4j.Logger;

public class JRawLib {
	private static final Logger logger = Logger.getLogger(JRawLib.class);
	
	private int width;
	private int height;
	private int black;
	private int maximum;
	private char [] data;
	
	private int [] redHistogram;
	private int [] greenHistogram;
	private int [] blueHistogram;
	
	native char [] doLoad(byte [] path);
	
	public void load(File  path)
	{
		this.data = doLoad(path.getPath().getBytes());
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public char[] getData() {
		return data;
	}

	public int getBlack() {
		return black;
	}

	public void setBlack(int black) {
		this.black = black;
	}

	public int getMaximum() {
		return maximum;
	}

	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}

	public int[] getRedHistogram() {
		return redHistogram;
	}

	public int[] getGreenHistogram() {
		return greenHistogram;
	}

	public int[] getBlueHistogram() {
		return blueHistogram;
	}
}
