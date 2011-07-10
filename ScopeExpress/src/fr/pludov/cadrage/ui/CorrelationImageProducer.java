package fr.pludov.cadrage.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;

import fr.pludov.cadrage.Image;
import fr.pludov.cadrage.ui.utils.tiles.TileProducer;
import fr.pludov.cadrage.ui.utils.tiles.TiledImage;

public class CorrelationImageProducer {
	private final Image image;
	private final byte [] [] datas;
	private final BufferedImageOp op;
	
	protected CorrelationImageProducer(Image image)
	{
		this.image = image;
		
		datas = new byte [3][];
		
		for(int channel = 0; channel < datas.length; ++channel)
		{
			datas[channel] = new byte[256];
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
					vAsFloat *= Math.pow(10, image.getExpoComposensation() / 100.0);
					
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
		
		return op.filter(source, result);
	}
}
