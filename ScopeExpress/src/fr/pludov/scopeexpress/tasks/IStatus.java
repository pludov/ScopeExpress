package fr.pludov.scopeexpress.tasks;

import java.awt.Color;

/** Classe de base pour les enums de status */
public interface IStatus {
	public String getTitle();
	public Color getColor();
	public boolean isTerminal();
}
