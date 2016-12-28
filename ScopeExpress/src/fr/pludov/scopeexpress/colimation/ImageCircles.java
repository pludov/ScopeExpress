package fr.pludov.scopeexpress.colimation;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.imageio.*;

import fr.pludov.io.*;
import fr.pludov.scopeexpress.focus.*;
import fr.pludov.utils.*;

public class ImageCircles {
	
	
	ImageCircles(CameraFrame rawCf, double blackPercent, double whitePercent, int minRay, int maxRay, int x0, int y0, int x1, int y1, int bin)
	{
        this.cf = bin > 1 ? rawCf.bin(x0, y0, x1, y1, bin) : rawCf;
        
		this.width = (x1 - x0 + 1) / bin;
		this.height = (y1 - y0 + 1) / bin;
		this.cfx0 = bin <= 1 ? x0 : 0;
		this.cfy0 = bin <= 1 ? y0 : 0;
		this.x0 = x0;
		this.y0 = y0;
        
		minRay = Math.floorDiv(minRay, bin);
		maxRay = Math.floorDiv(maxRay + bin - 1, bin);
        
		this.centerMinX = (int) Math.round(maxRay + width * 0.15);
        this.centerMinY = (int) Math.round(maxRay + height * 0.15);
        this.centerMaxX = (int) Math.round(width * 0.85 - maxRay);
        this.centerMaxY = (int) Math.round(height * 0.85 - maxRay);
		
        this.radiusMin = minRay;
        this.radiusMax = maxRay;

        this.agCount = 360;
        cs = new double[agCount];
        sn = new double[agCount];
        for(int i = 0; i < agCount; ++i)
        {
            cs[i] = Math.cos(i * 2 * Math.PI / (agCount));
            sn[i] = Math.sin(i * 2 * Math.PI / (agCount));
        }
        this.isCfa = cf.isCfa();
        this.cfbin = bin;
        aduMin = new int[isCfa ? 3 : 1];
        aduMax = new int[isCfa ? 3 : 1];
        aduMult = new double[isCfa ? 3 : 1];
        for(int c = 0; c < (isCfa ? 3 : 1); ++c)
        {
        	Histogram hg = Histogram.forArea(cf, cfx0, cfx0, cfx0 + width - 1, cfy0 + height - 1, isCfa ? ChannelMode.values()[c] : ChannelMode.Bayer);
        	aduMin[c] = hg.getBlackLevel(blackPercent);
        	aduMax[c] = hg.getBlackLevel(whitePercent);
        	if (aduMax[c] <= aduMin[c]) {
        		aduMax[c] = aduMin[c] + 1;
        	}
        	aduMult[c] = 1.0 / (aduMax[c] - aduMin[c]);
        }
	}
	
	final boolean isCfa;
	
	/** Origine dans l'image reçue */
	final int x0, y0;
	/** Origine dans cf */
	final int cfx0,cfy0;
	/** Bin appliqué */
	final int cfbin;
	double addPointWidth = 1.5;
	
//    static File workDir = new File("C:\\Documents and Settings\\utilisateur\\Bureau\\");
    //static File workDir = new File("C:\\Documents and Settings\\utilisateur\\Bureau\\");
    
//    final BufferedImage bf;

//    BufferedImage result;
	CameraFrame cf;
    int centerMinX, centerMaxX, centerMinY, centerMaxY, radiusMin, radiusMax;
    
    int agCount;
    
    final int width, height;

    int [] aduMin;
    int [] aduMax;
    double [] aduMult;
    
    
    double getAdu(int vx, int vy)
    {
    	int adu = cf.getAdu(vx, vy);
    	int ch;
    	if (isCfa) {
    		ch = ChannelMode.getRGBBayerId(vx, vy);
    	} else {
    		ch = 0;
    	}
    	adu -= aduMin[ch];
    	return adu * aduMult[ch];
    }
    
    double getPixel(double x, double y)
    {
    	if (x < 0 || x >= width || y < 0 || y >= height) return 0;
    	
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        
        double aduSum = getAdu(cfx0 + ix, cfy0 + iy);
        if (aduSum < 0) return 0;
        if (aduSum > 1) return 1;
        return aduSum;
    }
    
    int getPos(int sx, int sy, int sr, int steps)
    {
        return sx + steps * (sy + steps * sr);
    }
    
    public static class Tuple {
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

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		public double getR() {
			return r;
		}
    }
    
    
    public static class Solution extends Tuple implements Comparable<Solution>
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

        @Override
        public int compareTo(Solution o) {
            return (int)Math.signum(o.err - err);
        }

		public double getErr() {
			return err;
		}
        
    }
    
    List<Solution> listMinimums(Tuple min, Tuple max, int steps, boolean limiteExt, double maxErr)
    {
        double minx = min.x; double maxx = max.x; double miny = min.y; double maxy = max.y; double minr = min.r; double maxr = max.r;
        
        double [] errors = new double[steps * steps * steps];
        for(int sx = 0; sx < steps; ++sx) {
            System.out.println("Processing " + (sx + 1) + "/" + steps);
            for(int sy = 0; sy < steps; ++sy) {
                for(int sr = 0; sr < steps; ++sr) {
                    double x = minx + sx * (maxx - minx) / (steps - 1);
                    double y = miny + sy * (maxy - miny) / (steps - 1);
                    double r = minr + sr * (maxr - minr) / (steps - 1);
                    double error = calcError(x, y, r, limiteExt);
                    errors[getPos(sx, sy, sr, steps)] = error;
                }
            }
        }
        List<Solution> result = new ArrayList<>();
        
        // A la recherche des minimum locaux
        for(int sx = 1; sx < steps - 1; ++sx) {
            for(int sy = 1; sy < steps - 1; ++sy) {
                SearchLoop: for(int sr = 1; sr < steps - 1; ++sr) {
                    double e = errors[getPos(sx, sy, sr, steps)];
                    if (e > maxErr) {
                        continue;
                    }
                    
                    for(int dx = -2; dx <= 2; ++dx) {
                        if (sx + dx < 0) {
                            continue;
                        }
                        if (sx + dx >= steps) {
                            continue;
                        }
                    
                        for(int dy = -2; dy <= 2; ++dy) {
                            if (sy + dy < 0) {
                                continue;
                            }
                            if (sy + dy >= steps) {
                                continue;
                            }
                            
                            for(int dr = -2; dr <= 2; ++dr) {
                                if (sr + dr < 0) {
                                    continue;
                                }
                                if (sr + dr >= steps) {
                                    continue;
                                }
                                
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
            }
        }
            
            
        return result;
        
    }

    /** On est sûr que t est un minimum local sur deltaMax. Cherche par optimisation du gradient */
    Solution optimize(Solution from, Tuple deltaMax, boolean limiteExt) {
        
        // Amortissement règle la décroissance de la série
        // de manière à ce que la distance max parcourue soit toujours deltaMax.
        int amortissement = 10;
        Tuple t = from;
        double current = from.err;
        
//        Graphics2D g2d = ((Graphics2D)result.getGraphics());
        
        int count = 20;
        // Globalement la somme fait 1. (limite convergente)
        for(int i = 0; i < count; ++i) {
            
            double dist = Math.pow(0.5, i * 1.0 / amortissement) / amortissement;
            
            Tuple bestNv = t;
            for(int dx = -1; dx <= 1; dx ++) {
                for(int dy = -1; dy <= 1; dy ++) {
                    for(int dz = -1; dz <= 1; dz ++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Tuple nvPos = t.add(deltaMax.mult(dx * dist, dy * dist, dz * dist));
                        double err = calcError(nvPos.x, nvPos.y, nvPos.r, limiteExt);
                        if (err < current) {
                            bestNv = nvPos;
                            current = err;
                        }
                    }
                }
            }
            t = bestNv;
//            g2d.setColor(new Color(255 - (i * 255) / (count - 1), (i * 255) / (count -1), 32));
//            g2d.draw(new Ellipse2D.Double(t.x - t.r, t.y - t.r,2 * t.r, 2 * t.r));
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
    
    
    BitSet erode(BitSet bs)
    {
    	BitSet copy = new BitSet(bs.size());
    	int offset = 0;
    	
    	int w = width;
    	int h = height;
    	
    	for(int y = 0; y < height; ++y) {
    		for(int x = 0; x < width; ++x) {
    			if (bs.get(offset)) {
    				if ((x == 0 || bs.get(offset - 1))
    					&& (y == 0 || bs.get(offset - w))
    					&& (x == w -1 || bs.get(offset + 1))
    					&& (y == h - 1 || bs.get(offset + w)))
    				{
    					copy.set(offset, true);
    				}
    			}
    			offset++;
         	}
    	}
    	return copy;
    }
    
    void invert(BitSet bs)
    {
    	bs.flip(0, width * height);
    }
    
    BitSet grow(BitSet bs)
    {
    	invert(bs);
    	bs = erode(bs);
    	invert(bs);
    	return bs;
    }
    
    BitSet findDeltas() throws IOException
    {
    	BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = ((Graphics2D)temp.getGraphics());

        int cpt = 0;
        
        BitSet on = new BitSet();
        BitSet off = new BitSet();
        int offset = 0;
        int onCpt = 0, offCpt = 0;
        for(int y = 0; y < height; ++y) {
        	for(int x = 0; x < width; ++x) {
        		double d = getPixel(x, y);
        		
                if (d  <= 0.0) {
                	off.set(offset);
                	offCpt++;
                }
                if (d >= 1.0) {
                	temp.setRGB(x, y, 0xff0000ff);

                	on.set(offset);
                	onCpt++;
                }
                offset++;
        	}
        }
        System.out.println("on cpt = " + onCpt);
        System.out.println("off cpt = " + offCpt);
        
        on = erode(on);
        off = erode(off);
        offset = 0;
        for(int y = 0; y < height; ++y) {
        	for(int x = 0; x < width; ++x) {
                if (on.get(offset)) {
                	cpt++;
                }
                if (off.get(offset)) {
                	cpt++;
                }
        		
        		offset++;
        	}
        }
        
        
        on = grow(on);
        off = grow(off);
        on = grow(on);
        off = grow(off);
        
        on.and(off);
        
        
        offset = 0;
        cpt = 0;
        for(int y = 0; y < height; ++y) {
        	for(int x = 0; x < width; ++x) {
                if (on.get(offset)) {
                	temp.setRGB(x, y, 0xffff00ff);
                	cpt++;
                }
        		
        		offset++;
        	}
        }
        
        System.out.println("delta found : " + cpt);
        File workDir = new File("C:\\Documents and Settings\\utilisateur\\Bureau\\");
        ImageIO.write(temp, "png", new File(workDir, "circles-finddelta.png"));

        return on;
    }
    
    int imgOffset(int x, int y) {
        if (x < 0) {
            return -1;
        }
        if (y < 0) {
            return -1;
        }
        if (x >= width) {
            return -1;
        }
        if (y >= height) {
            return -1;
        }
        return x + y * width;
    }
    
    
    // Trouve dx tel que dx*dx + dy*dy = dlt
    static double findDx(double dy, double dlt)
    {
    	double v = dlt - dy * dy;
    	if (v < 0) {
			return Double.NaN;
		}
    	return Math.sqrt(v);
    }
    
    static Map<Integer, Map<Double, int []>> circles = new HashMap<>();
    
    
    static int [] getCircle(int cx, int cy, int ray, double width)
    {
    	Map<Double, int[]> forRay = circles.get(ray);
    	if (forRay == null) {
    		forRay = new HashMap<>();
    		circles.put(ray, forRay);
    	}
    	int [] content = forRay.get(width);
    	if (content == null) {
    		content = calcCenteredCircle(ray, width);
    		forRay.put(width, content);
    	}
    	
    	content = Arrays.copyOf(content, content.length);
    	for(int i = 0; i < content.length; i += 3)
    	{
    		content[i] += cx;
    		content[i + 1] += cy;
    	}
    	return content;
    	
    }	
	static int [] calcCenteredCircle(int ray, double width)
	{
    	double dltMin = Math.floor((ray - width) * (ray - width));
    	double dltMax = Math.ceil((ray + width) * (ray + width));
    	
    	int szeMax = (int)Math.floor(ray + width + 2);
    	int [] result = new int[3 * 2 * (2 * szeMax)];
    	int i = 0;
    	
    	for(int dy = -szeMax; dy <= szeMax; ++dy)
    	{
    		int y = dy;
    		double dyrel = y;
    		
    		double max = findDx(dyrel, dltMax);
    		if (Double.isNaN(max)) {
				continue;
			}
    		
    		int x0 = (int)Math.ceil( - max);
    		int x1 = (int)Math.floor( + max);
    		
    		if (x1 < x0) {
				continue;
			}
    		
    		double min = findDx(dyrel, dltMin);
    		if (Double.isNaN(min)) {
    			// On veut aller de min à max
    			result[i++] = x0;
    			result[i++] = y;
    			result[i++] = x1 - x0 + 1;
    		} else {
    			// Le dernnier plus grand que min
    			int x00 = (int) Math.floor(- min);
    			int x01 = (int) Math.ceil(+ min);
    			if (x01 < x00) {
    				// Tout compris
        			result[i++] = x0;
        			result[i++] = y;
        			result[i++] = x1 - x0 + 1;
    				
    			} else {
    				// Il faut deux segment
    				if (x0 <= x00) {
	        			result[i++] = x0;
	        			result[i++] = y;
	        			result[i++] = x00 - x0 + 1;
    				}
    				
    				if (x01 <= x1) {
	        			result[i++] = x01;
	        			result[i++] = y;
	        			result[i++] = x1 - x01 + 1;
    				}
    			}
    			
    		}
    	}
    	return Arrays.copyOf(result, i);
    }
    
    // Retourne des segments entiers : x, y, w
	@Deprecated
    static int [] getCircleZZZ(double cx, double cy, double ray, double width)
    {
    	int rx = (int)(Math.floor(cx));
    	int ry = (int)(Math.floor(cy));
    	
    	double dltMin = Math.floor((ray - width) * (ray - width));
    	double dltMax = Math.ceil((ray + width) * (ray + width));
    	
    	int szeMax = (int)Math.floor(ray + width + 2);
    	int [] result = new int[3 * 2 * (2 * szeMax)];
    	int i = 0;
    	
    	for(int dy = -szeMax; dy <= szeMax; ++dy)
    	{
    		int y = ry + dy;
    		double dyrel = y - cy;
    		
    		double max = findDx(dyrel, dltMax);
    		if (Double.isNaN(max)) {
				continue;
			}
    		
    		int x0 = (int)Math.ceil(cx - max);
    		int x1 = (int)Math.floor(cx + max);
    		
    		if (x1 < x0) {
				continue;
			}
    		
    		double min = findDx(dyrel, dltMin);
    		if (Double.isNaN(min)) {
    			// On veut aller de min à max
    			result[i++] = x0;
    			result[i++] = y;
    			result[i++] = x1 - x0 + 1;
    		} else {
    			// Le dernnier plus grand que min
    			int x00 = (int) Math.floor(cx - min);
    			int x01 = (int) Math.ceil(cx + min);
    			if (x01 < x00) {
    				// Tout compris
        			result[i++] = x0;
        			result[i++] = y;
        			result[i++] = x1 - x0 + 1;
    				
    			} else {
    				// Il faut deux segment
    				if (x0 <= x00) {
	        			result[i++] = x0;
	        			result[i++] = y;
	        			result[i++] = x00 - x0 + 1;
    				}
    				
    				if (x01 <= x1) {
	        			result[i++] = x01;
	        			result[i++] = y;
	        			result[i++] = x1 - x01 + 1;
    				}
    			}
    			
    		}
    	}
    	result[i++] = 0;	result[i++] = 0;	result[i++] = 0;
	
    	return result;
    }
    
    
    
    
    class CircleSpace {
        int [] data;
        
        int sx, sy, rcount, minr;
        
        CircleSpace(int isx, int isy, int radiusMin, int radiusMax)
        {
            this.minr = radiusMin;
            this.rcount = radiusMax - radiusMin + 1;
            	
            this.sx = isx;
            this.sy = isy;
            
            data = new int[this.sx * this.sy * this.rcount];
        }

        int offset(int x, int y, int r)
        {
            if (x < 0 || x >= sx) {
                return -1;
            }
            if (y < 0 || y >= sy) {
                return -1;
            }
            if (r < minr || r >= minr + rcount) {
                return -1;
            }
            return (r - minr) * sx * sy + y * sx + x;
        }
        
        int getCpt(int x, int y, int r)
        {
            int o = offset(x, y, r);
            if (o == -1) {
                return 0;
            }
            return data[o];
        }
        
        void inc(int x, int y, int r, int dlt)
        {
            int o = offset(x, y, r);
            if (o == -1) {
                return;
            }
            data[o]+= dlt;
            if (data[o] < 0) {
                data[o] = 0;
            }
        }
        
        void drawCircle(int x, int y, int r, int dlt, double width)
        {
        	int [] toRemove = getCircle(x, y, r, width);
        	for(int i = 0; i < toRemove.length; i += 3)
            {
            	int cx = toRemove[i];
            	int cy = toRemove[i + 1];
            	for(int cx2 = cx + toRemove[i + 2]; cx2 > cx; cx++) {
                    inc(cx, cy, r, dlt);
                }
            }
            
        }
        
        void addPoint(int x, int y, int dlt, double width)
        {
            // Pour tous les rayons possibles, ajoute tous les points
            for(int r = minr; r < minr + rcount; ++r)
            {
                // Dessine un cercle centré sur x, y, de rayon r
                drawCircle(x, y, r, dlt, width);
            }
        }
        
        // Trouver les maximas locaux...
        List<Solution> findMaximums(BitSet bitSet) throws IOException
        {
        	BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            
            Graphics2D g2d = ((Graphics2D)temp.getGraphics());
            List<Solution> result = new ArrayList<>();
            while(result.size() < 40)
            {    
                // FIXME: param: nombre de point mini sur un cercle
                int max = minr * 2;
                int besti = -1;
                for(int i = 0; i < data.length; ++i) {
                    if (data[i] > max) {
                        besti = i;
                        max = data[i];
                    }
                }
                if (besti == -1) {
                    break;
                }
                
                int ti = besti;
                int x, y, r;
                r = minr + ti / (sx * sy);
                ti %= (sx * sy);
                x = ti % sx;
                y = ti / sx;
                
                if (offset(x, y, r) != besti) {
                    throw new RuntimeException("Incorrect");
                }

                System.err.println("With " + max);
                
                // On va retirer tous les points qui sont couverts par le cercle. Un peut plus grand
                int [] toRemove = getCircle(x, y, r, 4);
                for(int i = 0; i < toRemove.length; i += 3)
                {
                	int cx = toRemove[i];
                	int cy = toRemove[i + 1];
                	for(int cx2 = cx + toRemove[i + 2]; cx2 > cx; cx++) {
                		int doffset = imgOffset(cx, cy);
                		if (doffset == -1) {
                            continue;
                        }
                        if (bitSet.get(doffset)) {
                            bitSet.set(doffset, false);
                            addPoint(cx, cy, -1, addPointWidth);
                        }
                	}
                }
                
                // On a un max. On le met dans les solution et on le retire
                Solution ext = new Solution(x, y, r, calcError(x, y, r, false));
                Solution in = new Solution(x, y, r, calcError(x, y, r, true));
                System.err.println("Ext: " + ext);
                System.err.println("In: " + ext);
                Solution s = (ext.err > in.err) ? in : ext;
                
                s = optimize(s, new Tuple(4, 4, 2),  true);
                System.err.println("Opt: " + s);
                
                result.add(s);

                int v = 255 - result.size() * 4;
                g2d.setColor(new Color(128, v, v));
                g2d.draw(new Ellipse2D.Double(s.x - s.r, s.y - s.r,2 * s.r, 2 * s.r));
                System.err.println("Draw: " + s);

            }
//            ImageIO.write(temp, "png", new File(workDir, "circles-found.png"));
            // Prendre chaque point sur le cercle, et retirer son influence.
            
            
//    		Collections.sort(result);
//    		for(int i = 0 ; i < Math.min(50, result.size()); ++i) {
//    			Solution s  = result.get(i);
//    			
////    	        Tuple min = new Tuple(s.x - 2, s.y - 2, s.r - 2);
////    	        Tuple max = new Tuple(s.x + 2, s.y + 2, s.r + 2);
////    	        
////    	        int firstRoundCpt = 8;
////    	        List<Solution> better = listMinimums(min, max, firstRoundCpt, true, 400);
////
////    	        if (better.isEmpty()) {
////    	        	continue;
////    	        }
////    	        
////    	        s = better.get(0);
////	            s = optimize(new Solution(s, calcError(s.x, s.y, s.r, true)), new Tuple(8, 8, 8), true);
//    			int v = 255 - i * 4;
//				g2d.setColor(new Color(128, v, v));
//	            g2d.draw(new Ellipse2D.Double(s.x - s.r, s.y - s.r,2 * s.r, 2 * s.r));
//	            System.err.println("Draw: " + s);
//    		}
            
            File workDir = new File("C:\\Documents and Settings\\utilisateur\\Bureau\\");

            ImageIO.write(temp, "png", new File(workDir, "circles-found.png"));
            return result;
        }
        
        
    }
    
    List<Solution> findCircles(BitSet from) throws IOException {
        CircleSpace cs = new CircleSpace(width, height, radiusMin, radiusMax);
        for (int i = from.nextSetBit(0); i >= 0; i = from.nextSetBit(i+1)) {
            int x = i % width;
            int y = i / width;
            cs.addPoint(x, y, 1, addPointWidth);
        }
        List<Solution> rslt = new ArrayList<>();
        for(Solution s : cs.findMaximums(from))
        {
        	s = new Solution(x0 + s.x * this.cfbin, y0 + s.y * this.cfbin, s.r * this.cfbin, s.err);
        	rslt.add(s);
        }
        return rslt;
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
            
            double px = cx + (radius - 1.5)* cs;
            double py = cy + (radius - 1.5)* sn;
            
            
            double v1 = (getPixel(px, py) + getPixel(px - 1, py)+ getPixel(px + 1, py)+ getPixel(px, py - 1)+ getPixel(px, py + 1)) /5;
            
            px = cx + (radius + 1.5) * cs;
            py = cy + (radius + 1.5) * sn;
            
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
    
    
    
    
//    void drawCircle() throws IOException
//    {
//        BufferedImage result = deepCopy(bf);
//
//    	int [] toRemove = getCircle(220, 120, 40, 10);
//    	for(int i = 0; toRemove[i + 2] != 0; i += 3)
//        {
//        	int cx = toRemove[i];
//        	int cy = toRemove[i + 1];
//        	for(int cx2 = cx + toRemove[i + 2]; cx2 > cx; cx++) {
//        		if (imgOffset(cx, cy) == -1) {
//					continue;
//				}
//                result.setRGB(cx, cy, 0xff0000ff);
//            }
//        }
//
//    	ImageIO.write(result, "png", new File(workDir, "circle-fill.png"));
//    }
    
    
//    public static void main(String[] args) throws IOException {
//        
//        File f = new File(workDir, "circles.png");
//        
//        BufferedImage bf = ImageIO.read(new FileImageInputStream(f));
//        
//        
//        	
//        System.out.println("size : " + bf.getWidth() + "x" + bf.getHeight());
//        
//        ImageCircles test = new ImageCircles(bf);
//        // test.drawCircle();
//        // test.perform2();
//        // ImageIO.write(test.result, "png", new File(workDir, "circles-analysed.png"));
//        test.findCircles(test.findDeltas());
//    }
}
