package fr.pludov.scopeexpress.ui.utils.tiles;

import java.awt.image.BufferedImage;

public interface TileProducer {

	BufferedImage produce(int offsetx, int offsety, BufferedImage area);
	
	@Override
	public boolean equals(Object obj);
}
