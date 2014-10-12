package fr.pludov.scopeexpress.ui.utils.tiles;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.omg.CORBA.WStringValueHelper;

/**
 * Encapsule un ensemble de bufferedImage dans un tableau.
 * 
 * @author Ludovic POLLET
 */
public class TiledImage {
	TiledImagePool pool;
	
	int w, h;
	int tile_sx,tile_sy;
	int num_tile_x, num_tile_y;
	
	int type;
	
	TiledImage source;					// Cette image est produite à partir de cette source...
	
	// Tout ceci est accédé sous la synchro de pool
	BufferedImage [] tiles;
	final long [] tileContent;			// 0 - Empty, autre = dirty
	final BitSet tileRequired;			// Ce tile doit être calculé
	final BitSet tileWorking;			// Un thread produit actuellement ce tile.
	final LinkedHashSet<Integer> tileTodoList;	// Liste des tiles à produire
	
	TileProducer producer;
	protected long producerSerial;		// Utiliser lors de la production de tile pour vérifier qu'il n'y a pa de changement de producer entre début et fin...
	
	
	public static interface TileIterator
	{
		void tile(int offsetX, int offsetY, int w, int h, BufferedImage potentialData);
	}
	
	public TiledImage(TiledImagePool pool, int w, int h, int type)
	{
		this.pool = pool;
		
		this.w = w;
		this.h = h;
		this.type = type;
		
		this.tile_sx = 64;
		this.tile_sy = 64;
		
		this.num_tile_x = (w + tile_sx - 1) / tile_sx;
		this.num_tile_y = (h + tile_sy - 1) / tile_sy;
		
		tiles = new BufferedImage[num_tile_x * num_tile_y];
		tileContent = new long [num_tile_x * num_tile_y];
		tileRequired = new BitSet(num_tile_x * num_tile_y);
		tileWorking = new BitSet(num_tile_x * num_tile_y);
		tileTodoList = new LinkedHashSet<Integer>();
	}
	
	/**
	 * Retourne l'ensemble des tiles qui sont à l'état ready (sous forme d'area)
	 * @return
	 */
	public Area getReadyArea()
	{
		Area result = new Area();
		synchronized(pool)
		{
			for(int tileY = 0; tileY < this.num_tile_y; ++tileY)
				for(int tileX = 0; tileX < this.num_tile_x; ++tileX)
				{
					int tileId = tileX + num_tile_x * tileY;
					if (tileContent[tileId] != producerSerial) continue;
					
					int x0 = tileX * tile_sx;
					int y0 = tileY = tile_sy;
					int w = tile_sx;
					int h = tile_sy;
					
					if (x0 + w > this.w) {
						w = this.w - x0;
					}
					if (y0 + h > this.h){
						h = this.h - y0;
					}
					
					result.add(new Area(new Rectangle2D.Double(x0, y0, w, h)));
				}
		}
		return result;
	}
	
	/**
	 * Retourne les tiles qui sont ready ou obsolètes
	 * @return
	 */
	public Shape getAvailableArea()
	{
		Area result = new Area();
		synchronized(pool)
		{
			for(int tileY = 0; tileY < this.num_tile_y; ++tileY)
				for(int tileX = 0; tileX < this.num_tile_x; ++tileX)
				{
					int tileId = tileX + num_tile_x * tileY;
					if (tileContent[tileId] == 0) continue;
					
					int x0 = tileX * tile_sx;
					int y0 = tileY = tile_sy;
					int w = tile_sx;
					int h = tile_sy;
					
					if (x0 + w > this.w) {
						w = this.w - x0;
					}
					if (y0 + h > this.h){
						h = this.h - y0;
					}
					
					result.add(new Area(new Rectangle2D.Double(x0, y0, w, h)));
				}
		}
		return result;
	}
	
	public void iterateTiles(TileIterator iterator)
	{
		for(int tileY = 0; tileY < this.num_tile_y; ++tileY)
			for(int tileX = 0; tileX < this.num_tile_x; ++tileX)
			{
				int tileId = tileX + num_tile_x * tileY;
				
				int x0 = tileX * tile_sx;
				int y0 = tileY = tile_sy;
				int w = tile_sx;
				int h = tile_sy;
				
				if (x0 + w > this.w) {
					w = this.w - x0;
				}
				if (y0 + h > this.h){
					h = this.h - y0;
				}
				
				BufferedImage im;
				synchronized(pool)
				{
					// FIXME : Il ne faudrait pas poubeliser cette entrée pendant ce temps !
					im = tiles[tileId];
				}
				iterator.tile(x0, y0, w, h, im);
			}
	}
	
	// Demande que cette shape soit produite à l'aide de producer.
	// Les autres emplacement sont discardés à l'issue.
	// Doit être appellé dans le thread swing
	public void setProducer(Area shape, TileProducer producer)
	{
		synchronized(pool)
		{
			if (producer == null) {
				if (this.producer == null) {
					// Pas de changement
					return;
				}
				producerSerial++;
				
				// Tout annuler
				// On n'a plus besoin de rien
				tileRequired.clear();
				// Tous les workers sont à annuler
				tileWorking.clear();
				
				// Disposer 
				for(int tileId = 0; tileId < num_tile_x * num_tile_y; ++tileId)
				{
					BufferedImage tile = this.tiles[tileId]; 
					if (tile != null) {
						pool.disposeTile(tile);
					}
					
					tileContent[tileId] = 0;
					tiles[tileId] = null;
				}
				
				tileTodoList.clear();
				
			} else {
				boolean producerChange = (producer != this.producer) && (producer == null || this.producer == null || !this.producer.equals(producer));

				if (producerChange) {
					producerSerial++;
					if (producerSerial == 0) producerSerial++;
				}
				
				for(int tileY = 0; tileY < this.num_tile_y; ++tileY)
					for(int tileX = 0; tileX < this.num_tile_x; ++tileX)
					{
						int x0 = tileX * tile_sx;
						int y0 = tileY * tile_sy;
						int w = tile_sx;
						int h = tile_sy;
						int tileId = tileX + num_tile_x * tileY;
						
						if (x0 + w > this.w) {
							w = this.w - x0;
						}
						if (y0 + h > this.h){
							h = this.h - y0;
						}
						
						
						if (shape.intersects(x0, y0, w, h))
						{
							// On veut cette tile
							this.tileRequired.set(tileId);
							// En cas de changement...
							if (producerChange) {
								if (this.tileWorking.get(tileId)) {
									this.tileWorking.clear(tileId);
								}
							}
						} else {
							// On n'en veut plus, on l'efface
							this.tileRequired.clear(tileId);
							this.tileWorking.clear(tileId);
							if (this.tileContent[tileId] != 0)
							{
								pool.disposeTile(this.tiles[tileId]);
								this.tiles[tileId] = null;
								this.tileContent[tileId] = 0;
							}
						}
					}
				
				// Mettre à jour la todoList...
				List<Integer> todoList = new ArrayList<Integer>();
				
				for(int tileId = 0; tileId < num_tile_x * num_tile_y; ++tileId)
				{
					if (!this.tileRequired.get(tileId)) continue;
					if (this.tileContent[tileId] == producerSerial) continue;
					
					todoList.add(tileId);
				}
				
				Collections.sort(todoList, new Comparator<Integer>() {
					@Override
					public int compare(Integer o1, Integer o2) {
						long delta = tileContent[o1] - tileContent[o2];
						if (delta < 0) return -1;
						if (delta > 0) return 1;
						return 0;
					}
				});
				
				this.tileTodoList.clear();
				for(Integer todo : todoList)
				{
					this.tileTodoList.add(todo);
				}
				
				// Du travail en plus, notifier les pools
				if (!this.tileTodoList.isEmpty()) {
					pool.notifyAll();
				}
			}
		}
	}
	
	public void put(Graphics2D graphics, int dstx, int dsty)
	{
		put(graphics, 0, 0, this.w, this.h, dstx, dsty);
	}
	
	public void put(Graphics2D graphics, int x, int y, int w, int h, int dstx, int dsty)
	{
		int minTileX = x / tile_sx;
		int minTileY = y / tile_sy;
		int maxTileX = (x + w - 1) / tile_sx;
		int maxTileY = (y + h - 1) / tile_sy;
		
		if (minTileX < 0) minTileX = 0;
		if (minTileY < 0) minTileY = 0;
		if (minTileX >= num_tile_x) minTileX = num_tile_x - 1;
		if (minTileY >= num_tile_y) minTileY = num_tile_y - 1;
		
		for(int tileY = minTileY; tileY <= maxTileY; ++tileY)
			for(int tileX = minTileX; tileX <= maxTileX; ++tileX)
			{
				int tileX0 = 0;
				int tileY0 = 0;
				int imageX0 = tile_sx * tileX;
				int imageY0 = tile_sy * tileY;
				int tileW = tile_sx;
				int tileH = tile_sy;
				
				if (imageX0 + tileW > this.w) {
					tileW = this.w - imageX0;
				}
				if (imageY0 + tileH > this.h){
					tileH = this.h - imageY0;
				}
				
				if (imageX0 < x) {
					tileX0 += x - imageX0;
					tileW -= x - imageX0;

					imageX0 = x;
				}

				if (imageY0 < y) {
					tileY0 += y - imageY0;
					tileH -= y - imageY0;

					imageY0 = y;
				}

				if (imageX0 + tileW > w) {
					tileW = w - imageX0;
				}
				
				if (imageY0 + tileH > h) {
					tileH = h - imageY0;
				}
				
				if (tileW <= 0) continue;
				if (tileH <= 0) continue;
				
				BufferedImage img;
				synchronized(pool)
				{
					img = tiles[tileX + num_tile_x * tileY];
				}
				
				int dx0 = dstx + imageX0 - x;
				int dy0 = dsty + imageY0 - y;
				int dx1 = dx0 + tileW - 1;
				int dy1 = dy0 + tileH - 1;
				
				int sx0 = tileX0;
				int sy0 = tileY0;
				int sx1 = sx0 + tileW - 1;
				int sy1 = sy0 + tileH - 1;
				
				if (img != null) {
					graphics.drawImage(img, dx0, dy0, dx1, dy1, sx0, sy0, sx1, sy1, null);
				} else {
					graphics.fillRect(dx0, dy0, tileW, tileH);
				}
			}
		
		
	}
	
}
