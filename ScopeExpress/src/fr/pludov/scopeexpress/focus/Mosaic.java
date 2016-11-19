package fr.pludov.scopeexpress.focus;

import java.util.*;

import org.apache.log4j.*;
import org.w3c.dom.*;

import fr.pludov.scopeexpress.ui.*;
import fr.pludov.scopeexpress.utils.*;
import fr.pludov.utils.*;
import fr.pludov.utils.XmlSerializationContext.*;

public class Mosaic {
	private static final Logger logger = Logger.getLogger(Mosaic.class);
	
	public final WeakListenerCollection<MosaicListener> listeners = new WeakListenerCollection<MosaicListener>(MosaicListener.class);
	
	final Application focus;
	final List<Image> images;
	final List<Star> stars;
	final List<ExclusionZone> exclusionZones;
	/// Chaque image a ses paramètre ici
	final IdentityHashMap<Image, MosaicImageParameter> imageMosaicParameter;
	final Map<Star, Map<Image, StarOccurence>> occurences;
	final Map<String, PointOfInterest> pointOfInterest;
	
	// Correction de distorsion (éventuellement vide)
	private ImageDistorsion distorsion;
		
	public Mosaic(Application focus) {
		this.occurences = new HashMap<Star, Map<Image,StarOccurence>>();
		this.images = new ArrayList<Image>();
		this.imageMosaicParameter = new IdentityHashMap<Image, MosaicImageParameter>();
		this.stars = new ArrayList<Star>();
		this.exclusionZones = new ArrayList<ExclusionZone>();
		this.focus = focus;
		this.pointOfInterest = new TreeMap<String, PointOfInterest>();
		this.setDistorsion(null);
	}
	
	
	public Element save(XmlSerializationContext context)
	{
		Element mosaic = context.newNode(Mosaic.class.getSimpleName());

		
		NodeDictionary<Image> imageDict = context.new NodeDictionary<Image>();
		// Sauver les images
		for(Image image : images)
		{
			// Enregistrer l'image
			Element imageNode = context.newNode(Image.class.getSimpleName());
			context.setNodeAttribute(imageNode, "path", image.getPath().getPath());
			
			imageDict.addNodeForObject(image, imageNode);
			
			mosaic.appendChild(imageNode);
		}
		
		// Sauver les étoiles
		NodeDictionary<Star> starDict = context.new NodeDictionary<Star>();
		for(Star star : stars)
		{
			Element starElement = star.save(context, imageDict);
			starDict.addNodeForObject(star, starElement);
			
			mosaic.appendChild(starElement);
		}
		
		for(MosaicImageParameter mip : this.imageMosaicParameter.values())
		{
			Element mipElement = mip.save(context, imageDict);
			mosaic.appendChild(mipElement);
		}
		
		for(Map<Image, StarOccurence> imgMap : this.occurences.values())
		{
			for(StarOccurence oc : imgMap.values())
			{
				Element occNode = oc.save(context, imageDict, starDict);
				mosaic.appendChild(occNode);
			}
		}
		
		for(ExclusionZone ze : this.exclusionZones)
		{
			Element zeNode = ze.save(context);
			mosaic.appendChild(zeNode);
		}
		
		return mosaic;
		
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
	
	public List<ExclusionZone> getExclusionZones()
	{
		return exclusionZones;
	}
	
	public void addExclusionZone(ExclusionZone ze)
	{
		this.exclusionZones.add(ze);
		listeners.getTarget().exclusionZoneAdded(ze);
	}
	
	public void removeExclusionZone(ExclusionZone ze)
	{
		if (this.exclusionZones.remove(ze))
		{
			listeners.getTarget().exclusionZoneRemoved(ze);
		}
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
	
	public MosaicImageParameter addImage(Image image, MosaicListener.ImageAddedCause cause)
	{
		if (this.imageMosaicParameter.get(image) != null) {
			throw new RuntimeException("image already present");
		}
		images.add(image);
		
		/// FIXME: déduire la taille en pixel des caractéristiques de l'image 
		double pixSize = Configuration.getCurrentConfiguration().getPixelSize();
		double focal = Configuration.getCurrentConfiguration().getFocal();
		
		MosaicImageParameter mip = new MosaicImageParameter(this, image, pixSize, focal);
		this.imageMosaicParameter.put(image,  mip);
		listeners.getTarget().imageAdded(image, cause);
		
		return mip;
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
		MosaicImageParameter mip = this.imageMosaicParameter.remove(image);
		
		for(Star star : stars)
		{
			if (star.clickImage == image) star.clickImage = null;
			StarOccurence sco = removeStarOccurence(image, star);
			if (sco != null) {
				listeners.getTarget().starOccurenceRemoved(sco);
			}
		}
		
		listeners.getTarget().imageRemoved(image, mip);
	}
	
	public void reset()
	{
		List<MosaicImageParameter> deletedImages = new ArrayList<MosaicImageParameter>(this.imageMosaicParameter.values());
		List<Star> deletedStars = new ArrayList<Star>(this.stars);
		List<ExclusionZone> deletedExclusionZones = new ArrayList<ExclusionZone>(this.exclusionZones);
		List<StarOccurence> deletedStarOccurence = new ArrayList<StarOccurence>();
		for(Map<Image, StarOccurence> occForStar : this.occurences.values())
		{
			deletedStarOccurence.addAll(occForStar.values());
		}
		List<PointOfInterest> deletedPoi = new ArrayList<PointOfInterest>(this.pointOfInterest.values());
		
		images.clear();
		stars.clear();
		exclusionZones.clear();
		imageMosaicParameter.clear();
		occurences.clear();
		pointOfInterest.clear();
		
		
		for(StarOccurence oc : deletedStarOccurence)
		{
			this.listeners.getTarget().starOccurenceRemoved(oc);
		}
		
		for(Star s : deletedStars)
		{
			this.listeners.getTarget().starRemoved(s);
		}
		
		for(MosaicImageParameter i : deletedImages)
		{
			this.listeners.getTarget().imageRemoved(i.getImage(), i);
		}
		
		for(ExclusionZone ex : deletedExclusionZones)
		{
			this.listeners.getTarget().exclusionZoneRemoved(ex);
		}
		
		for(PointOfInterest pi : deletedPoi)
		{
			this.listeners.getTarget().pointOfInterestRemoved(pi);
		}
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
	
	double [] getAveragePosition(Star star)
	{
		Map<Image, StarOccurence> starOccurences = occurences.get(star);
		double [] socPos = new double[2];
		double [] socSky3dPos = new double[3];
		double [] result = new double[3];
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
			
			socPos[0] = soc.getCorrectedX();
			socPos[1] = soc.getCorrectedY();
			parameters.getProjection().image2dToSky3d(socPos, socSky3dPos);
			result[0] += socSky3dPos[0];
			result[1] += socSky3dPos[1];
			result[2] += socSky3dPos[2];
			
			count ++;
		}
		if (count == 0) return null;

		VecUtils.normalize(result);
		if (VecUtils.hasNaN(result)) return null;
		return result;
	}
	
	void updateCorrelatedStars(Star star)
	{
		if (star.getPositionStatus() == StarCorrelationPosition.Reference) {
			throw new RuntimeException("Cannot update reference star");
		}
		double [] averagePos = getAveragePosition(star);
		if (averagePos != null) {
			star.setCorrelatedPos(averagePos);	
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
	

	public static class CorrelatedGridPoint
	{
		final Star star;
		final double adu;
		
		CorrelatedGridPoint(Star star)
		{
			this.star = star;
			this.adu = Math.pow(2.512, -star.getMagnitude());
		}


		public Star getStar() {
			return star;
		}
		
		public double getAduLevel() {
			return adu;
		}
	}
	
	public List<CorrelatedGridPoint> calcCorrelatedImages()
	{
		List<CorrelatedGridPoint> result = new ArrayList<CorrelatedGridPoint>();
		for(Star star : stars)
		{
			if (star.getPositionStatus().hasPosition()) {
				result.add(new CorrelatedGridPoint(star));
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

	public List<StarOccurence> getStarOccurences(Image image)
	{
		List<StarOccurence> result = new ArrayList<StarOccurence>();
		for(Map.Entry<Star, Map<Image, StarOccurence>> entry : this.occurences.entrySet())
		{
			StarOccurence so = entry.getValue().get(image);
			if (so != null) result.add(so);
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
	
	public ImageDistorsion getDistorsion() {
		return distorsion;
	}


	public void setDistorsion(ImageDistorsion distorsion) {
		this.distorsion = distorsion;
	}

	/** Les StarOccurence valide pour le calcul FWHM */
	public List<StarOccurence> getFwhmStarOccurences(Image image)
	{
		List<StarOccurence> result = new ArrayList<>();
		for(StarOccurence so : getStarOccurences(image))
		{
			if (so.isSaturationDetected()) continue;
			if (!so.isStarFound()) continue;
			if (!so.isAnalyseDone()) continue;
			result.add(so);
		}
		return result;
	}
	
	public Double getFwhm(Image image)
	{
		int starCount = 0;
		double result = 0;
		for(StarOccurence so : getFwhmStarOccurences(image))
		{
			result += so.getFwhm();
			starCount++;
		}
		if (starCount == 0) {
			return null;
		}
		return result / starCount;
	}
}
