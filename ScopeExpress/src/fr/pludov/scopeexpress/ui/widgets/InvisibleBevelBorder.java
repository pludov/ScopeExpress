package fr.pludov.scopeexpress.ui.widgets;

import java.awt.Color;
import java.awt.Component;

import javax.swing.border.BevelBorder;

public class InvisibleBevelBorder extends BevelBorder {
	Color componentBackground;

	public InvisibleBevelBorder(Color componentBackground) {
		super(RAISED);
		this.componentBackground = componentBackground;
	}

	@Override
	public Color getHighlightOuterColor(Component c) {
		return componentBackground != null ? componentBackground : c.getBackground();
	}

	@Override
	public Color getShadowOuterColor(Component c) {
		return componentBackground != null ? componentBackground : c.getBackground();
	}
	
	
	public Color getHighlightInnerColor(Component c)   {
		return componentBackground != null ? componentBackground : c.getBackground();
    }

    public Color getShadowInnerColor(Component c)      {
    	return componentBackground != null ? componentBackground : c.getBackground();
    }
}
