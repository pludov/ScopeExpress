package fr.pludov.scopeexpress.ui;

import fr.pludov.scopeexpress.focus.Mosaic;

public class ActivitySelectorItem {
	final String title;
	final Mosaic mosaic;
	final Activity activity;
	
	public ActivitySelectorItem(Mosaic mosaic, String title, Activity activity) {
		this.title = title;
		this.mosaic = mosaic;
		this.activity = activity;
	}
	
	@Override
	public String toString() {
		return title;
	}

}
