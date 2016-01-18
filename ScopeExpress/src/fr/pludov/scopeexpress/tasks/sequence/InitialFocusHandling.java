package fr.pludov.scopeexpress.tasks.sequence;

import fr.pludov.scopeexpress.ui.utils.*;

public enum InitialFocusHandling implements EnumWithTitle{
	Forced("Refaire la M.A.P"),
	Verified("Vérifier la M.A.P"),
	NotVerified("Ne pas modifier la M.A.P"),;

	public final String title;
	
	private InitialFocusHandling(String title)
	{
		this.title = title;
	}

	@Override
	public String getTitle() {
		return title;
	}
}
