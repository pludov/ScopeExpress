package fr.pludov.scopeexpress.tasks.autofocus;

import java.awt.Color;

import fr.pludov.scopeexpress.tasks.IStatus;

public enum TaskAutoFocusStatus implements IStatus {
	Shoot("Photo", Color.orange), 
	Move("Déplacement", Color.orange),
	Analysing("Analyse", Color.orange), 
	ClearingBacklash("Backlash", Color.orange);
	
	final String title;
	final Color color;
	
	TaskAutoFocusStatus(String title, Color display)  {
		this.title = title;
		this.color = display;
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
		return false;
	}
}