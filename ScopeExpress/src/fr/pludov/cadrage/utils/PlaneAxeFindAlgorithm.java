package fr.pludov.cadrage.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.pludov.cadrage.focus.AffineTransform3D;
import fr.pludov.cadrage.focus.MosaicImageParameter;
import fr.pludov.cadrage.focus.SkyProjection;
import fr.pludov.utils.EquationSolver;
import fr.pludov.utils.VecUtils;

/**
 * Recoit des image (projetté sur le ciel)
 * 
 * Calcul la projection de leur centre sur une sphere locale (on ignore la longitude/latitude
 * 
 * 
 * Ceci n'est efficace que pour des images assez éloignées du pole
 */
public class PlaneAxeFindAlgorithm {
	final static double jourSideral = 0.997270 * 86400.0;
	
	private class Entry
	{
		public Entry(MosaicImageParameter mip2, Date time2) {
			this.mip = mip2;
			this.time = time2;
		}
		
		final MosaicImageParameter mip;
		final Date time;
		
		double [] getLocalProjection(Date forDate)
		{
			double [] imgcenter = new double[] {projCenterX, projCenterY};
			double [] skycenter = new double[3];
			mip.getProjection().image2dToSky3d(imgcenter, skycenter);
			
			// corriger par rapport au temps qui s'est écoulé - permet de compenser l'écart temporel entre les photos
			double timeOffset = (time.getTime() - forDate.getTime()) / 1000.0;

			double jourSideral = 0.997270 * 86400.0;
			// Caclule l'angle correspondant
			timeOffset %= jourSideral;
			
			double timeAngle = timeOffset * 2 * Math.PI / jourSideral;
			
			double [] poleDuJour = new double[3];
			double [] poleJ2000 = SkyAlgorithms.J2000RaDecFromEpoch(0, 90, forDate.getTime());
			poleJ2000[0] *= 360/24;
			SkyProjection.convertRaDecTo3D(poleJ2000, poleDuJour);
			
			AffineTransform3D correction = AffineTransform3D.getRotationAroundAxis(poleDuJour, Math.cos(-timeAngle), Math.sin(-timeAngle));
			
			correction.convert(skycenter);
			
			return skycenter;
		}
	}

	List<Entry> entries = new ArrayList<PlaneAxeFindAlgorithm.Entry>(); 
	double projCenterX, projCenterY;
	double x, y;
	boolean found;

	public void addImage(MosaicImageParameter mip, Date time)
	{
		entries.add(new Entry(mip, time));
	}
	
	
	public void perform() throws EndUserException
	{
		// On calcule le centre des projections - celui là peut jouer un role important
		this.projCenterX = 0;
		this.projCenterY = 0;
		for(Entry e : entries)
		{
			projCenterX += e.mip.getProjection().getCenterx();
			projCenterY += e.mip.getProjection().getCentery();
		}
		projCenterX /= entries.size();
		projCenterY /= entries.size();
		
		double [] xi = new double[entries.size()];
		double [] yi = new double[entries.size()];
		double [] zi = new double[entries.size()];
		
		Entry last = entries.get(xi.length - 1);
		Date refDate = last.time;
		
		for(int i = 0; i < xi.length; ++i)
		{
			double [] xyz = entries.get(i).getLocalProjection(refDate);
			xi[i] = xyz[0];
			yi[i] = xyz[1];
			zi[i] = xyz[2];
		}
		
		double [] planeEq = EquationSolver.findPlane3d(xi, yi, zi);

		// On a une equation de plan. On veut normaliser le vecteur 
		double norm = Math.sqrt(planeEq[0] * planeEq[0] + planeEq[1] * planeEq[1] + planeEq[2] * planeEq[2]);
		planeEq[0] /= norm;
		planeEq[1] /= norm;
		planeEq[2] /= norm;
		planeEq[3] /= norm;

		double dlt = 0;
		for(int i = 0; i < xi.length; ++i)
		{
			double e = xi[i] * planeEq[0] + yi[i] * planeEq[1] + zi[i] * planeEq[2] + planeEq[3];
			e = e * e;
			dlt += e;
		}
		// La variance en radian
		dlt = Math.sqrt(dlt / xi.length);
		System.out.println("Plane aatching at " + (3600 * dlt * 360.0 / (2 * Math.PI)) + " arcsec");
		
		// Maintenant, on a un vecteur qui pointe vers le centre de rotation
		double [] axis2d = new double[2]; 
		if (!last.mip.getProjection().sky3dToImage2d(planeEq, axis2d)) {
			planeEq[0] = -planeEq[0];
			planeEq[1] = -planeEq[1];
			planeEq[2] = -planeEq[2];
			planeEq[3] = -planeEq[3];
			if (!last.mip.getProjection().sky3dToImage2d(planeEq, axis2d)) {
				// Vecteur parallèle à la dernière image; ne se projette pas !
				found = false;
				return;
			}
		}
		
		double [] axis = new double[]{ planeEq[0], planeEq[1], planeEq[2]};
		
		this.x = axis2d[0];
		this.y = axis2d[1];
		// La variance de l'axe dans chaque repère (normalement, l'axe doit être toujours au même endroit)
		double imageAxisVariance = 0;
		for(Entry e : entries)
		{
			double [] axisForE = new double[3]; 
			e.mip.getProjection().image2dToSky3d(new double[]{this.x, this.y}, axisForE);
			
			double n = VecUtils.norm(VecUtils.sub(axis, axisForE));
			n = n*n;
			imageAxisVariance += n;
		}
		imageAxisVariance = Math.sqrt(imageAxisVariance / entries.size());
		System.out.println("Axis variance by image " + (3600 * imageAxisVariance * 360.0 / (2 * Math.PI)) + " arcsec");
		
		found = true;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}
	
	public boolean isFound() {
		return found;
	}
	
}
