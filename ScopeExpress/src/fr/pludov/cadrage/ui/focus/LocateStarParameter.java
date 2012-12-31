package fr.pludov.cadrage.ui.focus;

import java.util.List;

import fr.pludov.cadrage.focus.Focus;
import fr.pludov.cadrage.focus.Image;

public class LocateStarParameter {

	public static enum CorrelationMode { 
		None, 			// Pas de recherche 
		SamePosition, 	// Etoiles de même position
		Global			// Recherche avec toutes les étoiles connues 
	};
	
	final Focus focus;
	
	int blackPercent;
	int aduSumMini;
	CorrelationMode correlationMode;
	// Null signifie prend la derniere
	Image reference;
	
	public LocateStarParameter(Focus focus) {
		this.focus = focus;
		this.blackPercent = 25;
		this.aduSumMini = 10000;
		this.correlationMode = CorrelationMode.SamePosition;
		this.reference = null;
	}
	
	public Image getEffectiveReferenceImage(Image forImage)
	{
		if (reference != null) {
			if (reference != forImage) return reference;
			return null;
		}
		
		List<Image> images = focus.getImages();
		for(int i = images.size() - 1; i >= 0; --i)
		{
			Image result = images.get(i);
			if (result != forImage) return result;
		}
		
		return null;
	}

	public int getBlackPercent() {
		return blackPercent;
	}

	public void setBlackPercent(int blackPercent) {
		this.blackPercent = blackPercent;
	}

	public int getAduSumMini() {
		return aduSumMini;
	}

	public void setAduSumMini(int aduSumMini) {
		this.aduSumMini = aduSumMini;
	}

	public CorrelationMode getCorrelationMode() {
		return correlationMode;
	}

	public void setCorrelationMode(CorrelationMode correlationMode) {
		this.correlationMode = correlationMode;
	}

	public Image getReference() {
		return reference;
	}

	public void setReference(Image reference) {
		this.reference = reference;
	}

	public Focus getFocus() {
		return focus;
	}

	
}
