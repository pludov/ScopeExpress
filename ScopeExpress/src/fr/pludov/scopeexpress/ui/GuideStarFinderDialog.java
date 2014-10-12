package fr.pludov.scopeexpress.ui;

import java.awt.Window;

public class GuideStarFinderDialog extends GuideStarFinderDialogDesign {
	final GuideStarFinder guideStarFinder;
	
	public GuideStarFinderDialog(Window window) {
		super(window);
		guideStarFinder = new GuideStarFinder();
		getContentPane().add(guideStarFinder);
	}

	public GuideStarFinder getGuideStarFinder() {
		return guideStarFinder;
	}
	
}
