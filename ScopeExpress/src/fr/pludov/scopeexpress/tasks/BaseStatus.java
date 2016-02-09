package fr.pludov.scopeexpress.tasks;

import java.awt.*;

public enum BaseStatus implements IStatus {
	Pending("En attente", Color.orange, false),
	Processing("En cours", Color.orange.darker(), false),
	Paused("Pause", Color.blue, false),
	
	
	Resuming("Reprise", Color.blue, false),
	Success("Terminé", Color.green.darker(), true),
	/** Abandon à la demande explicite */
	Canceled("Annulé", Color.red.darker(), true),
	/** Abandon (mais pas une erreur) */
	Aborted("Abandonné", Color.gray, true),
	Error("Echec", Color.red.darker(), true);
	
	final String title;
	final Color color;
	final boolean terminal;
	
	BaseStatus(String title, Color color, boolean terminal)
	{
		this.title = title;
		this.color = color;
		this.terminal = terminal;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public boolean isTerminal() {
		return terminal;
	}
}
