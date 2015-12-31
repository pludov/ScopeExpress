package fr.pludov.scopeexpress.tasks.shoot;

import fr.pludov.scopeexpress.ui.utils.*;

public enum ShootKind implements EnumWithTitle {

	Exposure("Exposition", "light"),
	TestExposure("Image de test/réglage", "test"),
	Bias("Offset", "offset"),
	Dark("Dark", "dark"),
	Flat("Flat", "flat"),
	DarkFlat("DarkFlat", "darkflat");
	
	public final String title;
	public final String filePart;
	
	ShootKind(String title, String filePart) {
		this.title = title;
		this.filePart = filePart;
	}
	
	@Override
	public String getTitle() {
		return title;
	}
}
