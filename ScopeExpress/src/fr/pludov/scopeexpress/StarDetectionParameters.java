package fr.pludov.scopeexpress;

import java.io.Serializable;

public class StarDetectionParameters implements Serializable {
	private static final long serialVersionUID = 2988357720469010056L;

	// Nombre maxi d'étoile détectée par photo.
	protected int nbStarMax = 125;

	// Nombre de pixels utilisés dans le calculs de la valeur B&W
	// 1 - 2 - 3
	protected int binFactor = 1;

	// Portion des pixels les plus faibles utilisés pour l'évaluation du fond.
	// Tous ce qui est en dessous de ça est considéré comme noir.
	protected double backgroundEvaluationPct = 0.25;
	
	// Taille des pavés d'évaluation du fond.
	protected int backgroundSquare = 24;

	// Paramètre global de détéction des ADU.
	protected double absoluteAduSeuil = 0.25;
	
	// Les étoiles incluent automatiquement les pixels adjacent ayant une intensité > starGrowIntensityRatio
	protected double starGrowIntensityRatio = 0.25;

	// Taille maxi des étoiles en pixels > starGrowIntensityRatio
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
