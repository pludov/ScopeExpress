package fr.pludov.cadrage.focus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.async.WorkStepProcessor;
import fr.pludov.cadrage.focus.MosaicListener.ImageAddedCause;
import fr.pludov.cadrage.ui.utils.BackgroundTaskQueue;
import fr.pludov.cadrage.utils.DynamicGridPoint;
import fr.pludov.cadrage.utils.WeakListenerCollection;

public class Mosaic {
	private static final Logger logger = Logger.getLogger(Mosaic.class);
	
	public final WeakListenerCollection<MosaicListener> listeners = new WeakListenerCollection<MosaicListener>(MosaicListener.class);
	
	final Application focus;
	final List<Image> images;
	final List<Star> stars;
	final IdentityHashMap<Image, MosaicImageParameter> imageMosaicParameter;
	final Map<Star, Map<Image, StarOccurence>> occurences;
	final Map<String, PointOfInterest> pointOfInterest;
	
	
	List<StarOccurence> todoList;
	
	public Mosaic(Application focus) {
		this.occurences = new HashMap<Star, Map<Image,StarOccurence>>();
		this.images = new ArrayList<Image>();
		this.imageMosaicParameter = new IdentityHashMap<Image, MosaicImageParameter>();
		this.stars = new ArrayList<Star>();
		this.todoList = new ArrayList<StarOccurence>();
		this.focus = focus;
		this.pointOfInterest = new TreeMap<String, PointOfInterest>();
	}
	
	Image getPreviousImage(Image after)
	{
		Image previousImage = null;
		for(Image image : images)
		{
			if (image.equals(after)) return previousImage;
			previousImage = image;
		}
		return null;
	}
	
	Image getNextImage(Image before)
	{
		for(Iterator<Image> it = images.iterator(); it.hasNext(); )
		{
			Image image = it.next();
			if (image == before) {
				if (it.hasNext()) {
					return it.next();
				}
				return null;
			}
		}
		
		return null;
	}

	public boolean containsImage(Image image)
	{
		return images.contains(image);
	}
	
	public StarOccurence getStarOccurence(Star star, Image image)
	{
		Map<Image, StarOccurence> occurenceForStar = occurences.get(star);
		if (occurenceForStar == null) return null;
		return occurenceForStar.get(image);
	}
	

	public List<Image> getImages() {
		return images;
	}

	public boolean hasImage(Image image)
	{
		return imageMosaicParameter.containsKey(image);
	}

	public List<Star> getStars() {
		return stars;
	}
	
	public void addStarOccurence(StarOccurence sco)
	{
		if (sco.owner != null) throw new RuntimeException("adding already owned staroccurence");
		sco.owner = this;
		
		Map<Image, StarOccurence> imageMap = occurences.get(sco.getStar());
		if (imageMap == null) {
			imageMap = new HashMap<Image, StarOccurence>();
			occurences.put(sco.getStar(), imageMap);
		}
		
		imageMap.put(sco.getImage(), sco);
		
		listeners.getTarget().starOccurenceAdded(sco);
	}
	
	private StarOccurence removeStarOccurence(Image image, Star star)
	{
		Map<Image, StarOccurence> imageMap = occurences.get(star);
		if (imageMap == null) return null;
		StarOccurence result = imageMap.remove(image);
		if (result != null) result.owner = null;
		// FIXME: ce code doit être assuré par les appelants!
//		if (imageMap.isEmpty()) {
//			occurences.remove(star);
//		}
		return result;
	}
	
	/**
	 * Fait en sorte que other soit en fait une reference à target.
	 * On va supprimer l'image de other eventuellement
	 */
	public void mergeStarOccurence(Star target, StarOccurence other)
	{
		if (other.getStar() == target) return;
		if (other.getStar().getPositionStatus() == StarCorrelationPosition.Reference) {
			throw new RuntimeException("Cannot merge a reference star");
		}
		StarOccurence otherCopy = new StarOccurence(other, target);
		
		// On retire une éventuelle occurence déjà présence pour target sur l'image
		removeStarOccurence(other.getImage(), target);
		addStarOccurence(otherCopy);
		
		// On retire l'ancienne...
		removeStarOccurence(other.getImage(), other.getStar());
		Map m = occurences.get(other.getStar());
		if (m == null || m.isEmpty()) {
			removeStar(other.getStar());
		} else {
			logger.error("Correlation sans suppression de l'image source => c'est louche");
		}
			
	}
	
	public void addStar(Star star)
	{
		if (star.mosaic != null) {
			throw new RuntimeException("add star of already owned star");
		}
		star.mosaic = this;
		stars.add(star);
		listeners.getTarget().starAdded(star);
		
//		for(Image img : images)
//		{
//			StarOccurence sco = new StarOccurence(this, img, star);
//			sco.init();
//			addStarOccurence(sco);
//		}
	}
	
	public void removeStar(Star star)
	{
		if (star.mosaic != this) {
			throw new RuntimeException("Removing star from other mosaic");
		}
		star.mosaic = null;
		if (!stars.remove(star)) return;
		
		for(Image image : images)
		{
			StarOccurence sco = removeStarOccurence(image, star);
			if (sco != null) {
				listeners.getTarget().starOccurenceRemoved(sco);
			}
		}
		
		listeners.getTarget().starRemoved(star);
		
	}
	
	public void addImage(Image image, MosaicListener.ImageAddedCause cause)
	{
		if (this.imageMosaicParameter.get(image) != null) {
			throw new RuntimeException("image already present");
		}
		images.add(image);
		this.imageMosaicParameter.put(image,  new MosaicImageParameter(this, image));
		listeners.getTarget().imageAdded(image, cause);
		
//		for(Star star : stars)
//		{
//			StarOccurence sco = new StarOccurence(this, image, star);
//			sco.init();
//			addStarOccurence(sco);
//		}
	}
	
	public void removeImage(Image image)
	{
		if (!images.remove(image)) return;
		this.imageMosaicParameter.remove(image);
		
		for(Star star : stars)
		{
			if (star.clickImage == image) star.clickImage = null;
			StarOccurence sco = removeStarOccurence(image, star);
			if (sco != null) {
				listeners.getTarget().starOccurenceRemoved(sco);
			}
		}
		
		listeners.getTarget().imageRemoved(image);
	}
	
	public final Application getApplication()
	{
		return this.focus;
	}
	
	public List<PointOfInterest> getAllPointsOfInterest()
	{
		return new ArrayList<PointOfInterest>(pointOfInterest.values());
	}
	
	public void addPointOfInterest(PointOfInterest poi)
	{
		PointOfInterest old = pointOfInterest.put(poi.getName(), poi);
		
		if (old != null) {
			this.listeners.getTarget().pointOfInterestRemoved(old);
		}
		this.listeners.getTarget().pointOfInterestAdded(poi);
	}

	public void removePointOfInterest(PointOfInterest poi)
	{
		PointOfInterest current = pointOfInterest.get(poi.getName());
		if (current == poi) {
			pointOfInterest.remove(poi.getName());
			this.listeners.getTarget().pointOfInterestRemoved(poi);
		}
	}
	
	public MosaicImageParameter getMosaicImageParameter(Image image)
	{
		return this.imageMosaicParameter.get(image);
	}
	
	void updateCorrelatedStars(Star star)
	{
		if (star.getPositionStatus() == StarCorrelationPosition.Reference) {
			throw new RuntimeException("Cannot update reference star");
		}
		Map<Image, StarOccurence> starOccurences = occurences.get(star);
		double x = 0, y = 0;
		double [] result = new double[2];
		int count = 0;
		for(Map.Entry<Image, StarOccurence> entry : starOccurences.entrySet())
		{
			Image image = entry.getKey();
			StarOccurence soc = entry.getValue();
			
			MosaicImageParameter parameters = this.imageMosaicParameter.get(image);
			if (parameters == null || !parameters.isCorrelated()) {
				System.out.println("Suspect : une etoiles correllé a une occurence dans une image non correllée");
				continue;
			}
			if (!soc.isStarFound() || !soc.isAnalyseDone()) continue;
			
			result = parameters.imageToMosaic(soc.getX(), soc.getY(), result);
			
			x += result[0];
			y += result[1];
			count ++;
		}
		
		if (count > 0) {
			star.setCorrelatedPos(x / count, y / count);
		} else {
			star.unsetCorrelatedPos();
		}
	}
	
	void updateCorrelatedStars(Image modified)
	{
		List<Star> stars = new ArrayList<Star>();
		for(Map.Entry<Star, Map<Image, StarOccurence>> entry : occurences.entrySet())
		{
			if (entry.getKey().getPositionStatus() == StarCorrelationPosition.Reference) {
				continue;
			}
			
			Map<Image, StarOccurence> occByStar = entry.getValue();
			if (occByStar.containsKey(modified)) {
				stars.add(entry.getKey());
			}
		}
		
		for(Star star : stars) {
			updateCorrelatedStars(star);
		}
	}
	

	public static class CorrelatedGridPoint implements DynamicGridPoint
	{
		final Star star;
		final double x, y;
		
		CorrelatedGridPoint(Star star, double x, double y)
		{
			this.star = star;
			this.x = x;
			this.y = y;
		}

		@Override
		public double getX() {
			return this.x;
		}

		@Override
		public double getY() {
			return this.y;
		}

		public Star getStar() {
			return star;
		}
	}
	
	public List<CorrelatedGridPoint> calcCorrelatedImages()
	{
		List<CorrelatedGridPoint> result = new ArrayList<CorrelatedGridPoint>();
		for(Star star : stars)
		{
			if (star.getPositionStatus().hasPosition()) {
				result.add(new CorrelatedGridPoint(star, star.getCorrelatedX(), star.getCorrelatedY()));
			}
		}
		return result;
	}
	
	public boolean exists(Star star)
	{
		return star.mosaic == this;
	}
	
	public boolean exists(StarOccurence soc)
	{
		return soc.owner == this;
	}
	
	public List<StarOccurence> getStarOccurences(Star star)
	{
		List<StarOccurence> result = new ArrayList<StarOccurence>();
		Map<?, StarOccurence> occurencesForStar = this.occurences.get(star);
		if (occurencesForStar != null) {
			result.addAll(occurencesForStar.values());
		}
		return result;
	}
	
	public List<StarOccurence> getAllStarOccurences()
	{
		List<StarOccurence> result = new ArrayList<StarOccurence>();
		for(Map<Image, StarOccurence> occList : occurences.values())
		{
			result.addAll(occList.values());
		}
		
		return result;
	}
}
