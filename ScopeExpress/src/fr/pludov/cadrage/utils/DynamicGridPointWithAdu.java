package fr.pludov.cadrage.utils;

public interface DynamicGridPointWithAdu extends DynamicGridPoint {
	// Pas d'unité particulière, on s'attend juste à une certaine uniformité
	public double getAduLevel();
}
