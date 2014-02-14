package fr.pludov.cadrage.ui.focus;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.pludov.cadrage.focus.AffineTransform3D;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.focus.SkyProjection;

/**
 * Represente un cercle en x, y de rayon 1 (l'équateur)
 * Le cercle est déplacé par une transformation affine
 * Finalement, le cercle peut être découpé en arc.
 * 
 */
public class Circle {

	
	double minAngle, maxAngle;
	
	final AffineTransform3D position;
	
	public Circle(AffineTransform3D position) {
		this.position = position;
		position = AffineTransform3D.identity;
		
		minAngle = 0;
		maxAngle = 2 * Math.PI;
	}
	
	private Circle(Circle copy, double minAngle, double maxAngle)
	{
		this.position = copy.position;
		this.minAngle = minAngle;
		this.maxAngle = maxAngle;
	}

	private void drawLine(Graphics2D g2d, double x0, double y0, double x1, double y1)
	{
		if (Double.isNaN(x0) || Double.isNaN(y0)) return;
		if (Double.isNaN(x1) || Double.isNaN(y1)) return;
		if (x0 < Integer.MIN_VALUE || x0 > Integer.MAX_VALUE) return;
		if (y0 < Integer.MIN_VALUE || y0 > Integer.MAX_VALUE) return;
		if (x1 < Integer.MIN_VALUE || x1 > Integer.MAX_VALUE) return;
		if (y1 < Integer.MIN_VALUE || y1 > Integer.MAX_VALUE) return;
		

		int ix0 = (int)Math.round(x0);
		int iy0 = (int)Math.round(y0);
		int ix1 = (int)Math.round(x1);
		int iy1 = (int)Math.round(y1);
		g2d.drawLine(ix0, iy0, ix1, iy1);
	}
	
	/**
	 * @param demiplan: ax+by+cz+d>=0
	 */
	public List<Circle> cut(double [] demiplan)
	{
		//		nvx = cos(k) * m11 + sin(k) * m12 + m14
		//				nvy = cos(k) * m21 + sin(k) * m22 + m24
		//				nvz = cos(k) * m31 + sin(k) * m32 + m34
		//
		//				a.nvx + b.nvy + c.nvz + d >= 0
		//
		//				a.(cos(k) * m11 + sin(k) * m12 + m14)
		//				 + b.(cos(k) * m21 + sin(k) * m22 + m24)
		//				 + c.(= cos(k) * m31 + sin(k) * m32 + m34)
		//				 + d >= 0

		//		cos(k)(a * m11 + b*m21 + c * m31)
		//		+ sin(k)(a.m12 + b*m22 + c* m 32)
		//		+ (a * m14 + b * m24 + c * m34 + d)
		//		 >= 0


		double a = demiplan[0] * position.fact(AffineTransform3D.m11)
							+ demiplan[1] * position.fact(AffineTransform3D.m21)
							+ demiplan[2] * position.fact(AffineTransform3D.m31);
		double b = demiplan[0] * position.fact(AffineTransform3D.m12)
							+ demiplan[1] * position.fact(AffineTransform3D.m22)
							+ demiplan[2] * position.fact(AffineTransform3D.m32);
		double c = demiplan[3] 
							+ demiplan[0] * position.fact(AffineTransform3D.m14)
							+ demiplan[1] * position.fact(AffineTransform3D.m24)
							+ demiplan[2] * position.fact(AffineTransform3D.m34);
				
		//		=> c'est une ellipse
		//				a * cos(k) + b * sin(k) + c > 0
		//
		//				a * cos(k) + b * sin(k) 
		//				R = sqrt(a² + b²)
		//				t = tan-1(b/a)
		//				R . cos(k - t) + c > 0
		//
		//				cos(k - t) > -c / R

		double r = Math.sqrt(a * a + b * b);
		double t = Math.atan2(b, a);
		
		double seuil = -c / r;
		if (seuil <= -1) {
			// on ne change rien.
			return Collections.singletonList(this);
		}
		if (seuil > 1) {
			return Collections.emptyList();
		}
		
		double acos = Math.acos(seuil);
		
		// Check :
		// a * cos( t - acos) + b * sin(t - acos) + c
		double v1 = a * Math.cos( t - acos) + b * Math.sin(t - acos) + c;
		double v2 = a * Math.cos( t + acos) + b * Math.sin(t + acos) + c;
		
		// k - t = +/- acos
		// k = t +/- acos
		double nvminangle = t - acos;
		double nvmaxangle = t + acos;
		
		// FIXME : il faut faire un matching des segments en prenant en compte le modulo 2PI
		
		return Collections.singletonList(new Circle(this, t - acos, t + acos));
		
//		List<Circle> result = new ArrayList<Circle>();
//		boolean lastAngleWasOk = false;
//		double firstOkAngle = -1;
//		double lastAngle = -1;
//
//		double [] tmp3d = new double[3];
//
//		for(int i = 0 ; i <= 360; ++i)
//		{
//			double angle = minAngle + i* (maxAngle - minAngle) / 360;
//			tmp3d[0] = Math.cos(angle);
//			tmp3d[1] = Math.sin(angle);
//			tmp3d[2] = 0;
//			
//			position.convert(tmp3d);
//			
//			double eval = tmp3d[0] * demiplan[0] + tmp3d[1] * demiplan[1] + tmp3d[2] * demiplan[2] + demiplan[3];
//			if (eval <= 0) {
//				if (lastAngleWasOk)
//				{
//					result.add(new Circle(this, firstOkAngle, lastAngle));
//				}
//				lastAngleWasOk = false;
//			} else {
//				if (!lastAngleWasOk)
//				{
//					firstOkAngle = angle;
//				}
//				lastAngle = angle;
//				lastAngleWasOk = true;
//			}
//		}
//		if (lastAngleWasOk)
//		{
//			result.add(new Circle(this, firstOkAngle, lastAngle));
//		}
//		return result;
	}
	
	public void draw(Graphics2D g2d, MosaicImageParameter mip, AffineTransform imageToScreen)
	{
		double lastPtx = 0, lastPty = 0;
		boolean hasLast = false;
		int stepCount = (int)Math.ceil(180 * (maxAngle - minAngle) / Math.PI);
		stepCount += 2;
		
		double [] tmp3d = new double[3];
		double [] tmp2d = new double[2];
		double [] tmp2d2 = new double[2];
		for(int step = 0; step < stepCount; ++step)
		{
			double angle = minAngle + ((maxAngle - minAngle) * step) / (stepCount - 1);
			
			tmp3d[0] = Math.cos(angle);
			tmp3d[1] = Math.sin(angle);
			tmp3d[2] = 0;
			
			position.convert(tmp3d);
			
			double x, y;
			if (!mip.getProjection().sky3dToImage2d(tmp3d, tmp2d))
			{
				hasLast = false;
				continue;
			} else {
				imageToScreen.transform(tmp2d, 0, tmp2d2, 0, 1);
				x = tmp2d2[0];
				y = tmp2d2[1];
			}
			
			if (hasLast) {
				drawLine(g2d, lastPtx, lastPty, x, y);
			}
			
			hasLast = true;
			lastPtx = x;
			lastPty = y;
		}
		
		
	}
	
}
