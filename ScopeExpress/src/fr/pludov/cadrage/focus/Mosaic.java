package fr.pludov.cadrage.focus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.pludov.cadrage.async.WorkStepProcessor;
import fr.pludov.cadrage.focus.MosaicListener.ImageAddedCause;
import fr.pludov.cadrage.ui.utils.BackgroundTaskQueue;
import fr.pludov.cadrage.utils.WeakListenerCollection;

public class Mosaic {
	public final WeakListenerCollection<MosaicListener> listeners = new WeakListenerCollection<MosaicListener>(MosaicListener.class);
	
	final Application focus;
	final List<Image> images;
	final List<Star> stars;
	final IdentityHashMap<Image, MosaicImageParameter> imageMosaicParameter;
	final Map<Star, Map<Image, StarOccurence>> occurences;
	
	List<StarOccurence> todoList;
	
	public Mosaic(Application focus) {
		this.occurences = new HashMap<Star, Map<Image,StarOccurence>>();
		this.images = new ArrayList<Image>();
		this.imageMosaicParameter = new IdentityHashMap<Image, MosaicImageParameter>();
		this.stars = new ArrayList<Star>();
		this.todoList = new ArrayList<StarOccurence>();
		this.focus = focus;
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


	public List<Star> getStars() {
		return stars;
	}
	
	public void addStarOccurence(StarOccurence sco)
	{
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
		if (imageMap.isEmpty()) {
			occurences.remove(star);
		}
		return result;
	}
	
	public void addStar(Star star)
	{
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
		this.imageMosaicParameter.put(image,  new MosaicImageParameter());
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
}