package fr.pludov.scopeexpress.ui.joystick;

public enum ButtonAction {
	Shoot("Declencher"),
	IncreaseDuration("Pauses plus longues"),
	DecreaseDuration("Pauses moins longues"),
	Recenter("Recentrer");
	
	public final String title;
	
	ButtonAction(String title)
	{
		this.title = title;
	}
}
