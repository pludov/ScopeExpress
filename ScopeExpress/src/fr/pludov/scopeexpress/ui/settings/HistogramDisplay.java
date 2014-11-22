package fr.pludov.scopeexpress.ui.settings;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.apache.log4j.Logger;

import fr.pludov.scopeexpress.ImageDisplayParameter.AduLevelMapper;
import fr.pludov.scopeexpress.focus.Histogram;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

/**
 * Affiche un histogramme
 * @author utilisateur
 *
 */
public class HistogramDisplay extends JPanel {
	private static final Logger logger = Logger.getLogger(HistogramDisplay.class);
	
	public enum Channel {
		Grey(0xb0, 0xb0, 0xb0),
		Red(0xff, 0, 0),
		Green(0, 0xff, 0),
		Blue(0, 0, 0xff);
		
		final int r,g,b;
		
		Channel(int r, int g, int b)
		{
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}
	
	class ChannelData
	{
		Histogram hg;
		Channel channel;
		AduLevelMapper curve;
		
		// Pour chaque x, nb de pixels
		int [] pixCount;
		int [] curveMin;
		int [] curveMax;
		int maxPixCount;
		
		ChannelData(Channel ch, Histogram hg, AduLevelMapper curve)
		{
			this.channel = ch;
			this.hg = hg;
			this.curve = curve;
		}
		
		void update()
		{
			int w = getWidth();
			
			pixCount = new int[w];
			curveMin = new int[w];
			curveMax = new int[w];
        	maxPixCount = 1;
        	if (hg != null) {
	        	for(int x = 0; x < w; ++x)
	        	{
	        		int start = getAduStart(x, w);
	        		int end = getAduStart(x + 1, w) - 1;
	        		
	        		int pc = 0;
	        		for(int adu = start; adu <= end; ++adu)
	        		{
	        			pc += hg.getValue(adu);
	        		}
	        		pixCount[x] = pc;
	        		if (pc > maxPixCount) {
	        			maxPixCount = pc;
	        		}
	        	}
        	}
        	
        	if (curve != null) {
        		for(int x = 0; x < w; ++x)
	        	{
	        		int start = getAduStart(x, w);
	        		int end = getAduStart(x + 1, w) - 1;
	        		
	        		boolean first = true;
	        		int mincurve = 0, maxcurve = 0;
	        		for(int adu = start; adu <= end; ++adu)
	        		{
	        			int c = curve.getLevelForAdu(adu);
	        			if (first) {
	        				mincurve = c;
	        				maxcurve = c;
	        				first = false;
	        			}
	        		}
	        		curveMin[x] = mincurve;
	        		curveMax[x] = maxcurve;
	        	}
        		// Rend la courbe connexe
        		for(int x = 0; x < w - 1; ++x)
        		{
        			// Si on monte, la suivante s'abaisse pour faire la connection
        			if (curveMax[x + 0] + 1 < curveMin[x + 1]) {
        				curveMin[x + 1] = curveMax[x + 0] + 1; 
        			}
        			
        			// Si on descend, la précédente s'abaisse pour faire la connection
        			if (curveMin[x + 0] > curveMax[x + 1] + 1) {
        				curveMin[x + 0] = curveMax[x + 1] + 1;
        			}
        			
        		}
        		
        	}
		}
	}
	
	java.awt.Image backBuffer;
	final EnumMap<Channel, ChannelData> datas;
	final Timer repainter;
	final LevelDisplayPosition ldp;
	private final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);
	

	public HistogramDisplay(LevelDisplayPosition ldp) {
		datas = new EnumMap<>(Channel.class);
		this.backBuffer = null;
		setBackground(Color.DARK_GRAY);
		
		repainter = new Timer(250, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				repaint();
			}
		});
		repainter.setRepeats(false);
		repainter.stop();
		this.ldp = ldp;
		
		ldp.listeners.addListener(this.listenerOwner, new LevelDisplayPositionListener() {
			
			@Override
			public void positionChanged() {
				scheduleRepaint(true);
			}
		});
		
		addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent arg0) {
				HistogramDisplay.this.ldp.zoomAt(arg0.getWheelRotation(), arg0.getX() * 1.0 / getWidth());
			}
		});
	}
	
	protected void scheduleRepaint(boolean now)
	{
		if (now) {
			repainter.stop();
			repaint(50);
		} else {
			repainter.restart();
		}
	}

	private final int getAduStart(int x, int w)
	{
		return ldp.getOffset() + ldp.getZoom() * x / w;
	}
	
	private final int getAduPos(int adu, int w)
	{
		// adu = ldp.getOffset() + ldp.getZoom() * x / w;
		return (adu - ldp.getOffset()) * w / ldp.getZoom(); 
		
	}
	
	/** Pendant l'affichage, on fait varier la hauteur de ces "bar", pour colorier en mixant les couleurs */
	abstract class Bar
	{
		int colorBit;
		int colorMask;
		ChannelData ch;
		/** Hauteur */
		int height;

		Bar(int colorBit, ChannelData ch)
		{
			this.colorBit = colorBit;
			this.colorMask = 1 << colorBit;
			this.ch = ch;
		}
		
		abstract void update(int x);
	}
	
	/** Ordonne les bar par height croissante */
	class BarComparator implements Comparator<Bar>
	{
		@Override
		public int compare(Bar arg0, Bar arg1) {
			int dlt = arg0.height - arg1.height;
			
			if (dlt < 0) {
				return -1;
			}
			if (dlt > 0) {
				return 1;
			}
			return arg0.ch.channel.ordinal() - arg1.ch.channel.ordinal();
		}
	}
	
	/**
	 * Effectue un affichage des graph en dessinant des barre verticales mixant les couleurs des différents canaux
	 */
	class HistogramPainter
	{
		private final Graphics2D g2d;
		private final int w;
		private final int h;
		private Color [] colors;
		private List<Bar> bars;
		private final BarComparator comparator;
		/** 2^^ nb channels */
		private int bit;
		
		HistogramPainter(Graphics2D g2d)
		{
			this.g2d = g2d;
			w = getWidth();
			h = getHeight();
			comparator = new BarComparator();
		}

		void paint()
		{
	        bars = new ArrayList<>();

	        bit = 0;
	        for(Channel c : Channel.values())
	        {
	        	ChannelData ch = datas.get(c);
	        	if (ch == null) continue;
	        	ch.update();
	        	
	        	bars.add(new Bar(bit++, ch) 
				{
	        		@Override
	        		void update(int x) {
	        			height = h * ch.pixCount[x] / ch.maxPixCount;
	        		}
				});
	        	
	        }
	        
	        initColors(0.65);
	        
	        fillBars((1 << bit) - 1, true);
	        
	        // Maintenant, on fait les courbes
	        initColors(1.0);
	        bars.clear();
	        bit = 0;
	        for(Channel c : Channel.values())
	        {
	        	ChannelData ch = datas.get(c);
	        	if (ch == null) continue;
	      	
	        	Bar bLow = new Bar(bit, ch) {
	        		@Override
	        		void update(int x) {
	        			height = h * ch.curveMin[x] / 255;
	        		}
	        	};
	        	bars.add(bLow);
	        	Bar bHigh = new Bar(bit, ch){
	        		@Override
	        		void update(int x) {
	        			height = (h * ch.curveMax[x] / 255) + 1;
	        		}
	        	};
	        	bars.add(bHigh);
	        	bit++;
	        }
	        
	        fillBars(0, false);
		}

		/** Crée les couleur pour chaque mix possible */
		private void initColors(double fact) {
			colors = new Color[1 << bit];
	        for(int v = 0; v < colors.length; ++v)
	        {
	        	int r = 0, g = 0, b = 0;
	        	for(Bar bar : bars)
	        	{
	        		// Le bit est actif ?
	        		if ((v & bar.colorMask) != 0) {
	        			r += bar.ch.channel.r;
	        			g += bar.ch.channel.g;
	        			b += bar.ch.channel.b;
	        		}
	        	}
	        	double vfact = fact;
	        	int high = Math.max(r, Math.max(g, b));
	        	if (high > 255) {
	        		vfact = 255 / high;
	        	}
	        	colors[v] = new Color((int)(r * vfact), (int)(g * vfact), (int)(b * vfact));
	        }
		}

		/** Déplace les bars en x et rempli avec les couleurs calculées */
		private void fillBars(int startColor, boolean background) {
			Bar[] barsArray = bars.toArray(new Bar[0]);
	        
	        for(int x = 0; x < w; ++x)
	        {
	        	for(int i = 0; i < barsArray.length; ++i)
	        	{
	        		barsArray[i].update(x);
	        	}
	        	Arrays.sort(barsArray, comparator);
	        	int lastY = 0;
	        	// Au départ, on a toutes les couleurs
	        	int color = startColor;
	        	for(int i = 0; i < barsArray.length; ++i)
	        	{
	        		Bar nextEnd = barsArray[i];
	        		int nextY = nextEnd.height;
	        		if (nextY > lastY && (background || color != 0)) {
	        			g2d.setColor(colors[color]);
	        			g2d.fillRect(x, h - nextY, 1, nextY - lastY);
	        		}
	        		lastY = nextY;
	        		// Retirer la couleur qui est finie.
	        		color ^= nextEnd.colorMask;
	        	}
	        	if (lastY < h && (background || color != 0)) {
	        		g2d.setColor(colors[color]);
	        		g2d.fillRect(x, 0, 1, h - lastY);
	        	}
	        }
		}
	
	}
	
	public void paint(Graphics gPaint)
	{
		repainter.stop();
		Graphics2D g2d = (Graphics2D)gPaint;
		
		new HistogramPainter(g2d).paint();
		
        paintChildren(gPaint);

	}
	public void setHistogram(Channel channel, Histogram hg, AduLevelMapper curve)
	{
		if (curve != null) {
			datas.put(channel, new ChannelData(channel, hg, curve));
		} else {
			datas.remove(channel);
		}
		scheduleRepaint(true);
	}

	public void setHistogram(Channel channel, Histogram hg)
	{
		ChannelData channelData = datas.get(channel);
		if (channelData == null) {
			channelData = new ChannelData(channel, hg, null);
			datas.put(channel, channelData);
		} else {
			channelData.hg = hg;
		}
		scheduleRepaint(true);
	}

	public void setCurve(Channel channel, AduLevelMapper curve) {
		ChannelData channelData = datas.get(channel);
		if (channelData == null) {
			channelData = new ChannelData(channel, null, curve);
			datas.put(channel, channelData);
		} else {
			channelData.curve = curve;
		}
		scheduleRepaint(true);
	}
	
	public void clear()
	{
		datas.clear();
		scheduleRepaint(true);
	}

	
	
}
