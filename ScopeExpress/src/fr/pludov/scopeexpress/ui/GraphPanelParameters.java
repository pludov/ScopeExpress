package fr.pludov.scopeexpress.ui;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;

import fr.pludov.scopeexpress.focus.ExclusionZone;
import fr.pludov.scopeexpress.focus.Image;
import fr.pludov.scopeexpress.focus.Mosaic;
import fr.pludov.scopeexpress.focus.MosaicImageParameter;
import fr.pludov.scopeexpress.focus.MosaicListener;
import fr.pludov.scopeexpress.focus.PointOfInterest;
import fr.pludov.scopeexpress.focus.Star;
import fr.pludov.scopeexpress.focus.StarCorrelationPosition;
import fr.pludov.scopeexpress.focus.StarOccurence;
import fr.pludov.scopeexpress.focus.StarOccurenceListener;
import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.scopeexpress.utils.WeakListenerCollection;
import fr.pludov.scopeexpress.utils.WeakListenerOwner;

/**
 * @author utilisateur
 *
 */
public class GraphPanelParameters extends GraphPanelParametersDesign {
	public final WeakListenerCollection<GraphPanelParametersListener> listeners = new WeakListenerCollection<GraphPanelParametersListener>(GraphPanelParametersListener.class);
	protected final WeakListenerOwner listenerOwner = new WeakListenerOwner(this);

	enum EnergyLimitType { None, Raw, Tantieme };
	Mosaic focus;
	
	public GraphPanelParameters(Mosaic focus) {
		this.focus = focus;
		
		for(EnergyLimitType elt : EnergyLimitType.values())
		{
			energyLimitTypeSel.addItem(elt);
		}
		
		// FIXME: restaurer les valeurs par défaut

		Runnable refilter = new Runnable() {
			@Override
			public void run() {
				invalidateData();
				refreshUi();
			}
		};
		
		Utils.addCheckboxChangeListener(this.chckbxCacherLesSatures, refilter);
		
		Utils.addComboChangeListener(this.energyLimitTypeSel, refilter);
		Utils.addTextFieldChangeListener(this.energyMinTextField, refilter);
		Utils.addTextFieldChangeListener(this.energyMaxTextField, refilter);
		
		Utils.addTextFieldChangeListener(this.minRangeHTextField, refilter);
		Utils.addTextFieldChangeListener(this.maxRangeHTextField, refilter);
		Utils.addTextFieldChangeListener(this.minRangeVTextField, refilter);
		Utils.addTextFieldChangeListener(this.maxRangeVTextField, refilter);
		
		this.chckbxCacherLesSatures.setSelected(true);
		

		focus.listeners.addListener(listenerOwner, new MosaicListener() {

			@Override
			public void starAdded(Star star) {
				invalidateData();
			}
			
			@Override
			public void starRemoved(Star star) {
				invalidateData();
			}
			
			@Override
			public void starOccurenceRemoved(StarOccurence sco) {
				sco.listeners.removeListener(listenerOwner);
				invalidateData();
			}
			
			@Override
			public void starOccurenceAdded(final StarOccurence sco) {
				repaint();
				sco.listeners.addListener(listenerOwner, new StarOccurenceListener() {
					
					@Override
					public void analyseDone() {
						invalidateData();	
					}

					@Override
					public void imageUpdated() {
					}
				});
			}
			
			
			@Override
			public void imageRemoved(Image image, MosaicImageParameter mip) {
				invalidateData();
			}
			
			@Override
			public void imageAdded(Image image, MosaicListener.ImageAddedCause cause) {
				invalidateData();				
			}

			@Override
			public void pointOfInterestAdded(PointOfInterest poi) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void pointOfInterestRemoved(PointOfInterest poi) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void exclusionZoneAdded(ExclusionZone ze) {
				invalidateData();				
			}

			@Override
			public void exclusionZoneRemoved(ExclusionZone ze) {
				invalidateData();				
			}
		});
		
		
		// FIXME : mettre en conf et retenir !
		
		refreshUi();
	}
	
	private EnergyLimitType getEnergyLimitType()
	{
		Object current = energyLimitTypeSel.getSelectedItem();
		if (current instanceof EnergyLimitType) return (EnergyLimitType)current;
		return EnergyLimitType.None;
	}
	
	private void setErrorLabel(JLabel errorLabel, String message)
	{
		if (message != null) {
			errorLabel.setToolTipText(message);
			errorLabel.setVisible(true);
		} else {
			errorLabel.setVisible(false);
		}
	}
	
	private void refreshUi()
	{
		String energyMinLblText = null;
		String energyMaxLblText = null;
		switch(getEnergyLimitType())
		{
		case None:
			this.energyMinTextField.setEnabled(false);
			this.energyMaxTextField.setEnabled(false);
			break;
		case Raw:
		case Tantieme:
			this.energyMinTextField.setEnabled(true);
			this.energyMaxTextField.setEnabled(true);
			Double minVal;
			try {
				minVal = getEnergyLimitMin();
				if (minVal != null && minVal < 0) {
					throw new Exception("Doit être positif ou nul");
				}
			} catch(Exception e) {
				energyMinLblText = "Minimum invalide: " + e.getMessage();
				minVal = null;
			}
			try {
				Double max = getEnergyLimitMax();
				if (max != null && max < 0) {
					throw new Exception("Doit être positif ou nul");
				}
				if (max != null && getEnergyLimitType() == EnergyLimitType.Tantieme && max > 100) {
					throw new Exception("Doit être inferieur ou égal à 100");
				}
				if (max != null && minVal != null && max <= minVal) {
					throw new Exception("Doit être superieur au minimum");
				}
			} catch(Exception e) {
				energyMaxLblText += "Maximum invalide : " + e.getMessage();
			}
			break;
		}
		setErrorLabel(this.energyMinErrorLbl, energyMinLblText);
		setErrorLabel(this.energyMaxErrorLbl, energyMaxLblText);
		
		String rangeHLblText = null;
		Double minH;
		try {
			minH = getHMin();
			if (minH != null && minH < 0) throw new Exception("Doit être positif ou nul");
			if (minH != null && minH > 100) throw new Exception("Doit être inferieur à 100");
		} catch(Exception e) {
			rangeHLblText = "Minimum invalide : " + e.getMessage();
			minH = null;
		}
		try {
			Double maxH = getHMax();
			if (maxH != null && maxH < 0) throw new Exception("Doit être positif ou nul");
			if (maxH != null && maxH > 100) throw new Exception("Doit être inferieur à 100");
			if (minH != null && maxH != null && minH >= maxH) throw new Exception("le minimum doit être inferieur au maximum");
		} catch(Exception e) {
			if (rangeHLblText != null) {
				rangeHLblText += "\n";
			} else {
				rangeHLblText = "";
			}
				
			rangeHLblText += "Maximum invalide : " + e.getMessage();
		}
		

		setErrorLabel(this.rangeHErrorLbl, rangeHLblText);

		String rangeVLblText = null;

		Double minV;
		try {
			minV = getVMin();
			if (minV != null && minV < 0) throw new Exception("Doit être positif ou nul");
			if (minV != null && minV > 100) throw new Exception("Doit être inferieur à 100");
		} catch(Exception e) {
			rangeVLblText = "Minimum invalide : " + e.getMessage();
			minV = null;
		}
		try {
			Double maxV = getVMax();
			if (maxV != null && maxV < 0) throw new Exception("Doit être positif ou nul");
			if (maxV != null && maxV > 100) throw new Exception("Doit être inferieur à 100");
			if (minV != null && maxV != null && minV >= maxV) throw new Exception("le minimum doit être inferieur au maximum");
		} catch(Exception e) {
			if (rangeVLblText != null) {
				rangeVLblText += "\n";
			} else {
				rangeVLblText = "";
			}
				
			rangeVLblText += "Maximum invalide : " + e.getMessage();
		}
		setErrorLabel(this.rangeVErrorLbl, rangeVLblText);

		String maxCountLblText = null;
		setErrorLabel(this.missingMaxCountErrorLbl, maxCountLblText);
	}
	
	private boolean isFilterSaturation()
	{
		return this.chckbxCacherLesSatures.isSelected();
	}
	
	private Double getEnergyLimitMin() throws NumberFormatException
	{
		String text = this.energyMinTextField.getText();
		if (text.equals("")) return null;
		return Double.parseDouble(text);
	}
	
	private Double getEnergyLimitMax() throws NumberFormatException
	{
		String text = this.energyMaxTextField.getText();
		if (text.equals("")) return null;
		return Double.parseDouble(text);
	}

	private Double getHMin() throws NumberFormatException
	{
		String text = this.minRangeHTextField.getText();
		if (text.equals("")) return null;
		return Double.parseDouble(text);
	}

	private Double getHMax() throws NumberFormatException
	{
		String text = this.maxRangeHTextField.getText();
		if (text.equals("")) return null;
		return Double.parseDouble(text);
	}

	private Double getVMin() throws NumberFormatException
	{
		String text = this.minRangeVTextField.getText();
		if (text.equals("")) return null;
		return Double.parseDouble(text);
	}

	private Double getVMax() throws NumberFormatException
	{
		String text = this.maxRangeVTextField.getText();
		if (text.equals("")) return null;
		return Double.parseDouble(text);
	}
	
	private void filterForEnergy(List<Image> images, List<Star> starListToFilter, IdentityHashMap<StarOccurence, Boolean> starOccurencesToFilter, Double min, Double max)
	{
		for(Iterator<Star> it = starListToFilter.iterator(); it.hasNext();)
		{
			Star s = it.next();
			for(Image img : images)
			{
				StarOccurence so = focus.getStarOccurence(s, img);
				if (so == null) continue;
				if (!so.isAnalyseDone()) {
					continue;
				}
				if (!so.isStarFound()) {
					continue;
				}
				
				if (!starOccurencesToFilter.containsKey(so)) continue;
				
				
				int [] adus = so.getAduSumByChannel();
				long v = 0;
				for(int i = 0; i < adus.length; ++i)
				{
					v += adus[i];
				}
				if (min != null && v < min)
				{
					starOccurencesToFilter.remove(so);
					continue;
				}
				
				if (max != null && v > max)
				{
					starOccurencesToFilter.remove(so);
					continue;
				}
				
			}
		}
	}
	
	private void filterForSaturation(List<Image> images, List<Star> starListToFilter, IdentityHashMap<StarOccurence, Boolean> starOccurencesToFilter)
	{
		for(Iterator<Star> it = starListToFilter.iterator(); it.hasNext();)
		{
			Star s = it.next();
			
			for(Image image : images)
			{
				StarOccurence so = focus.getStarOccurence(s, image);
				if (so == null || !so.isAnalyseDone() || !so.isStarFound()) {
					continue;
				}
				if (so.isSaturationDetected()) {
					starOccurencesToFilter.remove(so);
					continue;
				}
			}
		}
	}
	
	private void filterForCurrentPos(List<Image> images, List<Star> starListToFilter, IdentityHashMap<StarOccurence, Boolean> starOccurencesToFilter, Double minX, Double maxX, Double minY, Double maxY)
	{
		for(Iterator<Star> it = starListToFilter.iterator(); it.hasNext();)
		{
			Star s = it.next();

			for(Image currentImage : images)
			{
				StarOccurence so = focus.getStarOccurence(s, currentImage);
				if (so == null || !so.isAnalyseDone() || !so.isStarFound()) {
					continue;
				}
			
				double width = currentImage.getWidth() / 100.0;
				double height = currentImage.getHeight() / 100.0;
				
				if (minX != null && 2 * so.getPicX() < minX * width)
				{
					starOccurencesToFilter.remove(so);
					continue;
				}
				if (minY != null && 2 * so.getPicY() < minY * height)
				{
					starOccurencesToFilter.remove(so);
					continue;
				}
				if (maxX != null && 2 * so.getPicX() > maxX * width)
				{
					starOccurencesToFilter.remove(so);
					continue;
				}
				if (maxY != null && 2 * so.getPicY() > maxY * height)
				{
					starOccurencesToFilter.remove(so);
					continue;
				}
			}
		}
	}
	
	
	List<Image> images;
	List<Star> stars;
	IdentityHashMap<StarOccurence, Boolean> starOccurences;
	
	public List<Image> getImages()
	{
		ensureDataIsReady();
		return this.images;
	}
	
	public List<Star> getStars()
	{
		ensureDataIsReady();
		return this.stars;
	}
	
	/**
	 * C'est evidemment un IdentityHashSet 
	 * @return
	 */
	public Set<StarOccurence> getStarOccurences()
	{
		ensureDataIsReady();
		return this.starOccurences.keySet();
	}
	
	protected void invalidateData()
	{
		if (this.images == null) return;
		this.images = null;
		this.stars = null;
		this.starOccurences = null;
		listeners.getTarget().filterUpdated();
	}
	
	private void ensureDataIsReady()
	{
		if (this.images != null) return;
	
		images = new ArrayList<Image>(focus.getImages());
		stars = new ArrayList<Star>(focus.getStars());
		// Filtrer les étoiles selon leur état de correlation
		for(Iterator<Star> it = stars.iterator(); it.hasNext(); )
		{
			Star star = it.next();
			
			if (star.getPositionStatus() == StarCorrelationPosition.None) continue;
			
			for(ExclusionZone ez : focus.getExclusionZones())
			{
				if (ez.intersect(star)) {
					it.remove();
					break;
				}
			}
		}
		
		starOccurences = new IdentityHashMap<StarOccurence, Boolean>();
		for(Star star : stars)
		{
			for(StarOccurence so : focus.getStarOccurences(star))
			{
				starOccurences.put(so, Boolean.TRUE);
			}
		}
		applyFilter(this.images, this.stars, this.starOccurences);
	}
	
	private void applyFilter(List<Image> images, List<Star> starListToFilter, IdentityHashMap<StarOccurence, Boolean> starOccurencesToFilter)
	{
		for(Iterator<Star> it = starListToFilter.iterator(); it.hasNext(); )
		{
			Star star = it.next();
			for(ExclusionZone ze : focus.getExclusionZones())
			{
				if (ze.intersect(star)) 
				{
					it.remove();
					break;
				}
			}
		}
		
		// 1 - Applique un filtre sur l'énergie ?
		Double minVal, maxVal;
		try {
			minVal = getEnergyLimitMin();
		} catch(Exception e) {
			minVal = null;
		}
		try {
			maxVal = getEnergyLimitMax();
		} catch(Exception e) {
			maxVal = null;
		}
		
		switch(getEnergyLimitType()) {
		case None:
			break;
		case Raw:
			if (minVal != null || maxVal != null) {
				filterForEnergy(images, starListToFilter, starOccurencesToFilter, minVal, maxVal);
			}
			break;
		}
		
		Double minH, maxH, minV, maxV;
		try {
			minH = getHMin();
		} catch(Exception e) {
			minH = null;
		}
		
		try {
			minV = getVMin();
		} catch(Exception e) {
			minV = null;
		}
		
		try {
			maxH = getHMax();
		} catch(Exception e) {
			maxH = null;
		}
		
		try {
			maxV = getVMax();
		} catch(Exception e) {
			maxV = null;
		}
		
		if (minH != null || maxH != null || minV != null || maxV != null)
		{
			filterForCurrentPos(images, starListToFilter, starOccurencesToFilter, minH, maxH, minV, maxV);
		}
		if (isFilterSaturation()) {
			filterForSaturation(images, starListToFilter, starOccurencesToFilter);
		}
	}
	
}
