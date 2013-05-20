package fr.pludov.cadrage.ui.focus;

import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;

import fr.pludov.cadrage.focus.Image;
import fr.pludov.cadrage.focus.Mosaic;
import fr.pludov.cadrage.focus.Star;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.cadrage.utils.WeakListenerCollection;

public class GraphPanelParameters extends GraphPanelParametersDesign {
	public final WeakListenerCollection<GraphPanelParametersListener> listeners = new WeakListenerCollection<GraphPanelParametersListener>(GraphPanelParametersListener.class);

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
				refreshUi();
				listeners.getTarget().filterUpdated();
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
	
	private void filterForEnergy(List<Image> images, Image currentImage, List<Star> starListToFilter, Double min, Double max)
	{
		for(Iterator<Star> it = starListToFilter.iterator(); it.hasNext();)
		{
			Star s = it.next();
			boolean hasBiggerThanMin = false;
			boolean hasLowerThanMax = false;
			for(Image img : images)
			{
				StarOccurence so = focus.getStarOccurence(s, img);
				if (so == null) continue;
				if (so.isAnalyseDone() && so.isStarFound())
				{
					int [] adus = so.getAduSumByChannel();
					long v = 0;
					for(int i = 0; i < adus.length; ++i)
					{
						v += adus[i];
					}
					if (min != null && !hasBiggerThanMin && v > min)
					{
						hasBiggerThanMin = true;
					}
					
					if (max != null && !hasLowerThanMax && v < max)
					{
						hasLowerThanMax = true;
					}
					
					if ((min == null || hasBiggerThanMin) && (max == null || hasLowerThanMax)) {
						break;
					}
				}
			}
				
			if (min != null && !hasBiggerThanMin) {
				it.remove();
				continue;
			}
			if (max != null && !hasLowerThanMax) {
				it.remove();
				continue;
			}
		}
	}
	
	private void filterForSaturation(List<Image> images, List<Star> starListToFilter)
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
					it.remove();
					break;
				}
			}
		}
	}
	
	private void filterForCurrentPos(List<Image> images, Image currentImage, List<Star> starListToFilter, Double minX, Double maxX, Double minY, Double maxY)
	{
		if (currentImage == null) {
			starListToFilter.clear();
			return;
		}
		double width = currentImage.getCameraFrame().getWidth() / 100.0;
		double height = currentImage.getCameraFrame().getHeight() / 100.0;
		int kept = 0;
		int size = starListToFilter.size();
		for(Iterator<Star> it = starListToFilter.iterator(); it.hasNext();)
		{
			Star s = it.next();

			StarOccurence so = focus.getStarOccurence(s, currentImage);
			if (so == null || !so.isAnalyseDone() || !so.isStarFound()) {
				it.remove();
				continue;
			}
			if (minX != null && 2 * so.getPicX() < minX * width)
			{
				it.remove();
				continue;
			}
			if (minY != null && 2 * so.getPicY() < minY * height)
			{
				it.remove();
				continue;
			}
			if (maxX != null && 2 * so.getPicX() > maxX * width)
			{
				it.remove();
				continue;
			}
			if (maxY != null && 2 * so.getPicY() > maxY * height)
			{
				it.remove();
				continue;
			}
			kept++;
		}
		System.out.println("filter left " + kept + " / " + size);
	}
	
	public void filter(List<Image> images, Image currentImage, List<Star> starListToFilter)
	{
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
				filterForEnergy(images, currentImage, starListToFilter, minVal, maxVal);
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
			filterForCurrentPos(images, currentImage, starListToFilter, minH, maxH, minV, maxV);
		}
		if (isFilterSaturation()) {
			filterForSaturation(images, starListToFilter);
		}
	}
	
}
