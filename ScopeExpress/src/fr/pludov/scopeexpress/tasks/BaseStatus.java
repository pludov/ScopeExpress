package fr.pludov.scopeexpress.tasks;

import java.awt.*;

public enum BaseStatus implements IStatus {
	Pending("En attente", "status-pending", Color.orange, false),
	Processing("En cours", "status-running", Color.orange.darker(), false),
	Paused("Pause", "status-running", Color.blue, false),
	
	
	Resuming("Reprise", "status-running", Color.blue, false),
	Success("Terminé", "status-ok", Color.green.darker(), true),
	/** Abandon à la demande explicite */
	Canceled("Annulé", "status-canceled", Color.red.darker(), true),
	/** Abandon (mais pas une erreur) */
	Aborted("Abandonné", "status-aborted", Color.gray, true),
	Error("Echec", "status-error", Color.red.darker(), true);
	
	final String title;
	final String iconId;
	final Color color;
	final boolean terminal;
	
	BaseStatus(String title, String iconId, Color color, boolean terminal)
	{
		this.title = title;
		this.iconId = iconId;
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

	@Override
	public String getIconId() {
		return iconId;
	}
}
