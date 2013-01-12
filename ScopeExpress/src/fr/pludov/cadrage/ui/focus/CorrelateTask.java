package fr.pludov.cadrage.ui.focus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import fr.pludov.cadrage.focus.Focus;
import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.focus.correlation.Correlation;
import fr.pludov.cadrage.ui.utils.BackgroundTask;
import fr.pludov.cadrage.ui.utils.SwingThreadMonitor;
import fr.pludov.cadrage.utils.DynamicGridPoint;

public class CorrelateTask extends BackgroundTask {
	final FocusUi focusUi;	
	final Image reference, image;
	
	public CorrelateTask(FocusUi focusUi, Image reference, Image image)
	{
		super("Correlation des étoiles");
		this.focusUi = focusUi;
		this.reference = reference;
		this.image = image;
	}

	private static class ImageStar implements DynamicGridPoint
	{
		StarOccurence so;
		
		ImageStar(StarOccurence so)
		{
			this.so = so;
		}
		
		@Override
		public double getX() {
			return so.getX();
		}
		
		@Override
		public double getY() {
			return so.getY();
		}
	}
	
	private List<DynamicGridPoint> getImageStars(Image image)
	{
		List<DynamicGridPoint> imageStars = new ArrayList<DynamicGridPoint>();
		Focus focus = focusUi.getFocus();
		
		for(Star star : focus.getStars())
		{
			StarOccurence so = focus.getStarOccurence(star, image);
			if (so == null || !so.isAnalyseDone() || !so.isStarFound())
			{
				continue;
			}
			
			ImageStar imageStar = new ImageStar(so);
			
			imageStars.add(imageStar);
		}
		
		return imageStars;
	}
	
	@Override
	protected void proceed() throws BackgroundTaskCanceledException, Throwable {
		SwingThreadMonitor monitor = new SwingThreadMonitor();
		
		List<DynamicGridPoint> referenceStars;
		List<DynamicGridPoint> destStars;
		
		Correlation correlation = new Correlation();

		monitor.acquire();
		try {
		
			referenceStars = getImageStars(this.reference);
			
			destStars = getImageStars(this.image);
			
		} finally {
			monitor.release();
		}
		
		try {
			correlation.correlate(destStars, referenceStars);
			
			
			monitor.acquire();
			try {
				Focus focus = focusUi.getFocus();
	
				List<StarOccurence> referenceStarList = new ArrayList<StarOccurence>();
				List<StarOccurence> otherStarList = new ArrayList<StarOccurence>();
				
				for(Star star : focus.getStars())
				{
					StarOccurence so = focus.getStarOccurence(star, this.reference);
					if (so == null || !so.isAnalyseDone() || !so.isStarFound())
					{
						continue;
					}
					referenceStarList.add(so);
					
					StarOccurence otherSo = focus.getStarOccurence(star, this.image);
					if (otherSo == null || !otherSo.isAnalyseDone() || !otherSo.isStarFound()) 
					{
						continue;
					}
					otherStarList.add(otherSo);
				}
				
				
				// Trouver les occurences après transformation, dans un rayon, avec une tolérance donnée
				for(StarOccurence referenceSo : referenceStarList)
				{
					// Si il y a déjà une référence pour cette étoile, on abandonne !
					StarOccurence otherSo = focus.getStarOccurence(referenceSo.getStar(), this.image);
					if (otherSo != null) continue;
					
					double x = referenceSo.getX();
					double y = referenceSo.getY();

					double nvx = correlation.getTx() + x * correlation.getCs() + y * correlation.getSn();
					double nvy = correlation.getTy() + y * correlation.getCs() - x * correlation.getSn();
					
					
					otherSo = new StarOccurence(focus, this.image, referenceSo.getStar());
					otherSo.setPicX(nvx);
					otherSo.setPicY(nvy);
					otherSo.init(true);
					focus.addStarOccurence(otherSo);
				}
				
				
				
			} finally {
				monitor.release();
			}
			
			
		} catch(Throwable t) 
		{
			
		}
	}
}
