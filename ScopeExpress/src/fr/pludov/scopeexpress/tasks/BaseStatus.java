package fr.pludov.scopeexpress.tasks;

import java.awt.Color;

public enum BaseStatus implements IStatus {
	Pending("En attente", Color.orange, false),
	Processing("En cours", Color.orange.darker(), false),
	Paused("Pause", Color.blue, false),
	Resuming("Reprise", Color.blue, false),
	Success("Termin�", Color.green.darker(), true),
	Canceled("Annul�", Color.red.darker(), true),
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
