package fr.pludov.scopeexpress.ui.joystick;

public class TriggerSourceProvider {

	static TriggerSource ti = null;
	public static TriggerSource getInstance()
	{
		if (ti == null) {
			ti = new JInputTriggerSource();
		}
		return ti;
	}
	
}
