package fr.pludov.scopeexpress.ui.utils.tiles;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TiledImagePool {

	final List<WeakReference<BufferedImage>> freeTilePool = new LinkedList<WeakReference<BufferedImage>>();
	
	final List<WeakReference<TiledImage>> images = new LinkedList<WeakReference<TiledImage>>();
	
	public TiledImagePool()
	{
		for(int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i)
		{
			new Thread() {
				public void run() {
					try {
						threadRun();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	private void threadRun() throws InterruptedException
	{
		TiledImage lastImage = null;
		while(true)
		{
			// Trouver un tile à produire, si possible sur la dernière image
			TiledImage nextImage = null;
			
			
			TileProducer producer;
			long producerSerial;
			
			BufferedImage producerTarget;
			
			int tileId;
			int tileX, tileY, offsetX, offsetY, tileW, tileH;
			
			synchronized(this)
			{
				while(nextImage == null) {
					for(Iterator<WeakReference<TiledImage>> it = images.iterator(); it.hasNext();)
					
					{
						WeakReference<TiledImage> ref = it.next();
						
						TiledImage image = ref.get();
						if (image == null) {
							ref.clear();
							it.remove();
							continue;
						}
						
						
						if (image.tileTodoList.isEmpty() && nextImage == null || image == lastImage) {
							nextImage = image;
						}
					}
					lastImage = null;
					if (nextImage == null) {
						wait();
					}
				}
				producer = nextImage.producer;
				producerSerial = nextImage.producerSerial;
				tileId = nextImage.tileTodoList.iterator().next();
				nextImage.tileTodoList.iterator().remove();
				
				tileX = tileId % nextImage.num_tile_x;
				tileY = tileId / nextImage.num_tile_x;
				offsetX = nextImage.tile_sx * tileX;
				offsetY = nextImage.tile_sy * tileY;
				tileW = nextImage.tile_sx;
				tileH = nextImage.tile_sy;
				if (offsetX + tileW > nextImage.w) {
					tileW = nextImage.w - offsetX;
				}
				if (offsetY + tileH > nextImage.h) {
					tileH = nextImage.h - offsetY;
				}
				
				producerTarget = newTile(tileW, tileH, nextImage.type);
			}
			
			BufferedImage result = producer.produce(offsetX, offsetY, producerTarget);
			
			synchronized(this)
			{
				if (result != producerTarget) {
					disposeTile(producerTarget);
				}
				
				if (nextImage.tileRequired.get(tileId) &&
						((nextImage.tileContent[tileId] == 0) ||
						(nextImage.producerSerial - producerSerial < nextImage.producerSerial - nextImage.tileContent[tileId])))
				{
					// La production est bonne, on la garder...
					nextImage.tiles[tileId] = result;
					nextImage.tileContent[tileId] = producerSerial;
					
					// FIXME : signal de progression ?
					
				} else {
					// on a travaillé pour rien
					disposeTile(result);
				}
			}
		}
	}
	
	protected void addTileImage(TiledImage image)
	{
		synchronized(this) {
			images.add(new WeakReference<TiledImage>(image));
			notifyAll();
		}
	}
	
	/**
	 * Retourne un tile de la taille donnée. Le tile est peut être sale
	 * @param sx
	 * @param sy
	 * @param type
	 * @return
	 */
	protected BufferedImage newTile(int sx, int sy, int type)
	{
		synchronized(this) {
			for(Iterator<WeakReference<BufferedImage>> it = freeTilePool.iterator(); it.hasNext();)
			{
				WeakReference<BufferedImage> ref = it.next();
				BufferedImage tile = ref.get();
				
				if (tile == null) {
					it.remove();
					continue;
				}
				
				if (tile.getWidth() != sx || tile.getHeight() != sy || tile.getType() != type) {
					continue;
				}
				
				ref.clear();
				it.remove();
				
				return tile;
			}
		}
		
		return new BufferedImage(sx, sy, type);
	}
	
	protected void disposeTile(BufferedImage bi)
	{
		synchronized(this)
		{
			freeTilePool.add(new WeakReference<BufferedImage>(bi));
		}
	}
}
