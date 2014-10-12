package fr.pludov.scopeexpress;

import java.io.Serializable;

public class StarDetectionParameters implements Serializable {
	private static final long serialVersionUID = 2988357720469010056L;

	// Nombre maxi d'�toile d�tect�e par photo.
	protected int nbStarMax = 125;

	// Nombre de pixels utilis�s dans le calculs de la valeur B&W
	// 1 - 2 - 3
	protected int binFactor = 1;

	// Portion des pixels les plus faibles utilis�s pour l'�valuation du fond.
	// Tous ce qui est en dessous de �a est consid�r� comme noir.
	protected double backgroundEvaluationPct = 0.25;
	
	// Taille des pav�s d'�valuation du fond.
	protected int backgroundSquare = 24;

	// Param�tre global de d�t�ction des ADU.
	protected double absoluteAduSeuil = 0.25;
	
	// Les �toiles incluent automatiquement les pixels adjacent ayant une intensit� > starGrowIntensityRatio
	protected double starGrowIntensityRatio = 0.25;

	// Taille maxi des �toiles en pixels > starGrowIntensityRatio
	protected int starMaxSize = 16;

	public StarDetectionParameters() {
	}
	
	public StarDetectionParameters(StarDetectionParameters copy) {
		this.nbStarMax = copy.nbStarMax;
		this.backgroundEvaluationPct = copy.backgroundEvaluationPct;
		this.absoluteAduSeuil = copy.absoluteAduSeuil;
		this.binFactor = copy.binFactor;
		this.backgroundSquare = copy.backgroundSquare;
		this.starGrowIntensityRatio = copy.starGrowIntensityRatio;
		this.starMaxSize = copy.starMaxSize;
	}
	
	public double getBackgroundEvaluationPct() {
		return backgroundEvaluationPct;
	}

	public void setBackgroundEvaluationPct(double backgroundEvaluationPct) {
		this.backgroundEvaluationPct = backgroundEvaluationPct;
	}

	public double getAbsoluteAduSeuil() {
		return absoluteAduSeuil;
	}

	public void setAbsoluteAduSeuil(double absoluteAduSeuil) {
		this.absoluteAduSeuil = absoluteAduSeuil;
	}

	public int getBinFactor() {
		return binFactor;
	}

	public void setBinFactor(int binFactor) {
		this.binFactor = binFactor;
	}

	public int getBackgroundSquare() {
		return backgroundSquare;
	}

	public void setBackgroundSquare(int backgroundSquare) {
		this.backgroundSquare = backgroundSquare;
	}

	public double getStarGrowIntensityRatio() {
		return starGrowIntensityRatio;
	}

	public void setStarGrowIntensityRatio(double starGrowIntensityRatio) {
		this.starGrowIntensityRatio = starGrowIntensityRatio;
	}

	public int getStarMaxSize() {
		return starMaxSize;
	}

	public void setStarMaxSize(int starMaxSize) {
		this.starMaxSize = starMaxSize;
	}

	public int getNbStarMax() {
		return nbStarMax;
	}

	public void setNbStarMax(int nbStarMax) {
		this.nbStarMax = nbStarMax;
	}

}
