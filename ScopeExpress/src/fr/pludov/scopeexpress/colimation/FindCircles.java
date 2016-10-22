package fr.pludov.scopeexpress.colimation;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.imageio.*;
import javax.imageio.stream.*;

public class FindCircles {
	final BufferedImage bf;

	BufferedImage result;
	int centerMinX, centerMaxX, centerMinY, centerMaxY, radiusMin, radiusMax;
	
	int agCount;
	
	public FindCircles(BufferedImage bf) {
		this.bf = bf;
		this.centerMinX = (int) Math.round(bf.getWidth() * 0.35);
		this.centerMinY = (int) Math.round(bf.getHeight() * 0.35);
		this.centerMaxX = (int) Math.round(bf.getWidth() * 0.65);
		this.centerMaxY = (int) Math.round(bf.getHeight() * 0.65);
		
		this.radiusMin = (int) (Math.min(bf.getWidth(), bf.getHeight()) * 0.5 * 0.75);
		this.radiusMax = (int) (Math.min(bf.getWidth(), bf.getHeight()) * 0.5 * 1.0);
		
		this.agCount = 360;
		cs = new double[agCount];
		sn = new double[agCount];
		for(int i = 0; i < agCount; ++i)
		{
			cs[i] = Math.cos(i * 2 * Math.PI / (agCount));
			sn[i] = Math.sin(i * 2 * Math.PI / (agCount));
		}
		
		result = deepCopy(bf);
	}
	
	public static BufferedImage deepCopy(BufferedImage bi) {
	    ColorModel cm = bi.getColorModel();
	    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
	    WritableRaster raster = bi.copyData(bi.getRaster().createCompatibleWritableRaster());
	    return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
	
	double getPixel(double x, double y)
	{
		int ix = (int) Math.floor(x);
		int iy = (int) Math.floor(y);
		if ((ix < 0) || (iy < 0)) return 0;
		if ((ix >= bf.getWidth()) || (iy >= bf.getHeight())) return 0;
		
		int rgb = bf.getRGB(ix, iy);
		
		int r = rgb & 255;
		int g = (rgb >>> 8) & 255;
		int b = (rgb >>> 16) & 255;

		double rslt = (r+g+b) / 765.0;
		rslt *= 4;
		if (rslt > 1) rslt = 1;
		return rslt;

	}
	
	int getPos(int sx, int sy, int sr, int steps)
	{
		return sx + steps * (sy + steps * sr);
	}
	
	class Tuple {
		final double x, y, r;
		
		public Tuple(double x2, double y2, double r2) {
			this.x = x2;
			this.y = y2;
			this.r = r2;
			
		}

		public Tuple(Tuple t) {
			this.x = t.x;
			this.y = t.y;
			this.r = t.r;
		}

		public Tuple mult(double d, double e, double f) {
			return new Tuple(x * d, y * e, f * r);
		}

		public Tuple add(Tuple other) {
			return new Tuple(x + other.x, y + other.y, r + other.r);
		}

		public Tuple sub(Tuple other) {
			return new Tuple(x - other.x, y - other.y, r - other.r);
		}

		@Override
		public String toString() {
			return x+", " + y + ", " + r;
		}
	}
	
	
	class Solution extends Tuple
	{
		final double err;
		
		public Solution(Tuple t, double err) {
			super(t);
			this.err = err;
		}
		
		public Solution(double x, double y, double r, double err)
		{
			super(x, y, r);
			this.err = err;
		}
		
		@Override
		public String toString() {
			return "[" + super.toString() + "]=" + err;
		}
		
	}
	
	List<Solution> listMinimums(Tuple min, Tuple max, int steps, boolean limiteExt, double maxErr)
	{
		double minx = min.x; double maxx = max.x; double miny = min.y; double maxy = max.y; double minr = min.r; double maxr = max.r;
		
		double [] errors = new double[steps * steps * steps];
		for(int sx = 0; sx < steps; ++sx) {
			System.out.println("Processing " + (sx + 1) + "/" + steps);
			for(int sy = 0; sy < steps; ++sy)
				for(int sr = 0; sr < steps; ++sr) {
					double x = minx + sx * (maxx - minx) / (steps - 1);
					double y = miny + sy * (maxy - miny) / (steps - 1);
					double r = minr + sr * (maxr - minr) / (steps - 1);
					double error = calcError(x, y, r, limiteExt);
					errors[getPos(sx, sy, sr, steps)] = error;
				}
		}
		List<Solution> result = new ArrayList<>();
		
		// A la recherche des minimum locaux
		for(int sx = 1; sx < steps - 1; ++sx)
			for(int sy = 1; sy < steps - 1; ++sy)
				SearchLoop: for(int sr = 1; sr < steps - 1; ++sr) {
					double e = errors[getPos(sx, sy, sr, steps)];
					if (e > maxErr) continue;
					
					for(int dx = -2; dx <= 2; ++dx) {
						if (sx + dx < 0) continue;
						if (sx + dx >= steps) continue;
					
						for(int dy = -2; dy <= 2; ++dy) {
							if (sy + dy < 0) continue;
							if (sy + dy >= steps) continue;
							
							for(int dr = -2; dr <= 2; ++dr) {
								if (sr + dr < 0) continue;
								if (sr + dr >= steps) continue;
								
								double e2 = errors[getPos(sx + dx, sy + dy, sr + dr, steps)];
								if (e2 < e) {
									continue SearchLoop;
								}
								
							}
						}
					}
					double x = minx + sx * (maxx - minx) / (steps - 1);
					double y = miny + sy * (maxy - miny) / (steps - 1);
					double r = minr + sr * (maxr - minr) / (steps - 1);

					// Trouvé un minimum local
					System.err.println("Error at " + x + " " + y + " " + r + " => " + e);
					result.add(new Solution(x, y, r, e));
				}
			
			
		return result;
		
	}

	/** On est sûr que t est un minimum local sur deltaMax. Cherche par optimisation du gradient */
	Solution optimize(Solution from, Tuple deltaMax, boolean limiteExt) {
		
		// Amortissement règle la décroissance de la série
		// de manière à ce que la distance max parcourue soit toujours deltaMax.
		int amortissement = 100;
		Tuple t = from;
		double current = from.err;
		
		Graphics2D g2d = ((Graphics2D)result.getGraphics());
		
		int count = 1000;
		// Globalement la somme fait 1. (limite convergente)
		for(int i = 0; i < count; ++i) {
			
			double dist = Math.pow(0.5, i * 1.0 / amortissement) / amortissement;
			
			Tuple bestNv = t;
			for(int dx = -1; dx <= 1; dx ++)
				for(int dy = -1; dy <= 1; dy ++)
					for(int dz = -1; dz <= 1; dz ++) {
						if (dx == 0 && dy == 0 && dz == 0) continue;
						Tuple nvPos = t.add(deltaMax.mult(dx * dist, dy * dist, dz * dist));
						double err = calcError(nvPos.x, nvPos.y, nvPos.r, limiteExt);
						if (err < current) {
							bestNv = nvPos;
							current = err;
						}
					}
			t = bestNv;
			g2d.setColor(new Color(255 - (i * 255) / (count - 1), (i * 255) / (count -1), 32));
			g2d.draw(new Ellipse2D.Double(t.x - t.r, t.y - t.r,2 * t.r, 2 * t.r));
		}
		return new Solution(t, current);
	
	}
	
	
	Solution findMinMaxRay(List<Solution> result, Tuple scale, double mult, boolean limiteExt)
	{
		Solution outter = null;
		for(Solution seed : result) {
			Solution better = optimize(seed, scale, limiteExt);
			if (outter == null || better.r * mult> outter.r * mult) {
				outter = better;
			}
		}
		return outter;
		
	}
	
	void perform2()
	{
		Tuple min = new Tuple(this.centerMinX, this.centerMinY, this.radiusMin);
		Tuple max = new Tuple(this.centerMaxX, this.centerMaxY, this.radiusMax);
		
		int firstRoundCpt = 12;
		List<Solution> result = listMinimums(min, max, firstRoundCpt, true, 400);
		Tuple scale = max.sub(min).mult(4.0 / firstRoundCpt, 4.0 / firstRoundCpt, 4.0 / firstRoundCpt);
		Solution outter = findMinMaxRay(result, scale, 1.0, true);
		
		
		System.out.println("Outer => " + outter);
		
		// MAintenant, on recherche le inner, avec plus de radius
		double decentrageMax = 0.6;
		double rayMin = 0.2;
		double rayMax = 0.9;
		Tuple innerMin = new Tuple(outter.x - decentrageMax * outter.r, outter.y - decentrageMax * outter.r, outter.r * rayMin);
		Tuple innerMax = new Tuple(outter.x + decentrageMax * outter.r, outter.y + decentrageMax * outter.r, outter.r * rayMax);

		List<Solution> innerResults = listMinimums(innerMin, innerMax, firstRoundCpt, false, 400);
		Tuple innerScale = innerMax.sub(innerMin).mult(4.0 / firstRoundCpt, 4.0 / firstRoundCpt, 4.0 / firstRoundCpt);
		Solution inner = findMinMaxRay(innerResults, innerScale, -1.0, false);
		
		System.out.println("Inner => " + inner);
		
	}
	
	void perform()
	{
//		calcError(216, 211, 54);
		
		// On recherche les minimum locaux : 
		
		// steps devrait être calculé pour faire en sorte que les test de radius soient inferieurs à la marge
		
		// Il faut être précis à 2 pixels car l'algo cherche avec un ++ du rayon de 3 pixels
		int steps = (int)Math.ceil(Math.max(Math.max((this.centerMaxX - this.centerMinX) / 2, (this.centerMaxY - this.centerMinY) / 2), (this.radiusMax - this.radiusMin) / 2));
		System.out.println("Performing " + steps + " steps");
//		listMinimums(this.centerMinX, this.centerMaxX, this.centerMinY, this.centerMaxY, this.radiusMin, this.radiusMax, steps, true, 400);
		
//		
//		double best = 10000;
//		for(int cx = this.centerMinX; cx < this.centerMaxX; cx += 3)
//			for(int cy = this.centerMinY; cy < this.centerMaxY; cy += 3)
//				for(int r = this.radiusMin; r < this.radiusMax; r += 3) {
//					
//					double error = calcError(cx, cy, r);
//					if (error < best) {
//						System.err.println("Error at " + cx + " " + cy + " " + r + " => " + error);
//						best = error;
//					}
//			
//					
//				}
	}
	
	double [] cs;
	double [] sn;
	
	/** retourne une erreur. 0 = match parfait 
	 * @param limiteEXt: on cherche une limite exterieure (le point en dehors doit être noir)
	 * 
	 * */
	double calcError(double cx, double cy, double radius, boolean limiteExt)
	{
		double err = 0;
		// En gros, fait 360° et regarder si chaque pixel radius, radius + 0.2 change de couleur
		for(int angleI = 0; angleI < 360; angleI++) {
			double cs = this.cs[angleI];
			double sn = this.sn[angleI];
			
			double px = cx + radius * cs;
			double py = cy + radius * sn;
			
			double v1 = (getPixel(px, py) + getPixel(px - 1, py)+ getPixel(px + 1, py)+ getPixel(px, py - 1)+ getPixel(px, py + 1)) /5;
			
			px = cx + (radius + 3) * cs;
			py = cy + (radius + 3) * sn;
			
			double v2 = (getPixel(px, py) + getPixel(px - 1, py)+ getPixel(px + 1, py)+ getPixel(px, py - 1)+ getPixel(px, py + 1)) /5;
			
			// On veut delta = 1
			
			
			
			double delta;
			if (limiteExt) {
				// On veut v1 plus grand : V1 = 1 + V2
				delta = (v1 - v2) - 1;
			} else {
				// On veut v1 plus petit : V2 = 1 + V1
				delta = (v2 - v1) - 1;
			}
//			System.out.println("delta = " + delta);
			delta = delta * delta;
			err += delta;
		}
		return err;
	}
	
	
	
	
	
	
	
	public static void main(String[] args) throws IOException {
		File f = new File("C:\\Documents and Settings\\utilisateur\\Bureau\\circles.png");
		
		BufferedImage bf = ImageIO.read(new FileImageInputStream(f));
		
		System.out.println("size : " + bf.getWidth() + "x" + bf.getHeight());
		
		FindCircles test = new FindCircles(bf);
		test.perform2();
		ImageIO.write(test.result, "png", new File("C:\\Documents and Settings\\utilisateur\\Bureau\\circles-analysed.png"));
	}
}
