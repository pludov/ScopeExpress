package fr.pludov.scopeexpress.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;

import fr.pludov.scopeexpress.Image;
import fr.pludov.scopeexpress.ui.utils.tiles.TileProducer;
import fr.pludov.scopeexpress.ui.utils.tiles.TiledImage;

public class CorrelationImageProducer {
	private final Image image;
	private final byte [] [] datas;
	private final BufferedImageOp op;
	private final BufferedImageOp opGray;
	
	protected CorrelationImageProducer(Image image)
	{
		this.image = image;
		
		datas = new byte [3][];
		
		for(int channel = 0; channel < datas.length; ++channel)
		{
			datas[channel] = new byte[256];
			

			double multLevel = Math.pow(10, image.getExpoComposensation() / 100.0);
			double gamma = Math.pow(10, image.getGamma() / 100.0);
			
			double levelMin = image.getBlack();
			double levelMax = (255 - levelMin) * multLevel;
			
			for(int level = 0; level < 256; ++level)
			{
				int value;
				
				if (channel < 3)
				{
					double vAsFloat = level;
					
					switch(channel) {
					case 0:					// r
						break;
					case 1:					// g
						break;
					case 2:					// b
						break;
					}
					
					
					if (vAsFloat > levelMin) {
						vAsFloat -= levelMin;
						vAsFloat *= multLevel;
						
						// Remet entre 0 et 1...
						vAsFloat = (vAsFloat) / levelMax;
						// Applique le gamma
						vAsFloat = Math.pow(vAsFloat, gamma);
						// Remet entre levelMin et levelMax
						vAsFloat = levelMax * vAsFloat;
					} else {
						vAsFloat = 0;
					}
					
					if (vAsFloat > 255) {
						value = 255;
					} else if (vAsFloat < 0) {
						value = 0;
					} else {
						value = (int)Math.round(vAsFloat);
					}
					
				} else {
					value = level;
				}

				datas[channel][level] = (byte)value;
			}
		}
		
		op = new LookupOp(new ByteLookupTable(0, datas), null);
		opGray = new LookupOp(new ByteLookupTable(0, datas[0]), null);
	}


	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CorrelationImageProducer)) return false;
		CorrelationImageProducer other = (CorrelationImageProducer)obj;
		
		if (other.image != this.image) return false;
		
		if (other.datas.length != this.datas.length) return false;
		
		for(int i = 0; i < this.datas.length; ++i)
		{
			if (other.datas[i].length != this.datas[i].length) return false;
			for(int j = 0; j < this.datas[i].length; ++j)
				if (other.datas[i][j] != this.datas[i][j]) return false;
		}
		return true;
	}
	
	public BufferedImage produce(BufferedImage source) {
		
		BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
		if (source.getColorModel().getComponentSize().length == 1) {
			return opGray.filter(source,  result);
		} else {
			return op.filter(source, result);
		}
	}
}
