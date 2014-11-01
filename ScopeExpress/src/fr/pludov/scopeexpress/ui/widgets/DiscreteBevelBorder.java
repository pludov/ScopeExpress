package fr.pludov.scopeexpress.ui.widgets;

import java.awt.Color;
import java.awt.Component;

import javax.swing.border.BevelBorder;

public class DiscreteBevelBorder extends BevelBorder {
	Color componentBackground;
	
	public DiscreteBevelBorder(int bevelType, Color componentBackground) {
		super(bevelType);
		this.componentBackground = componentBackground;
	}

	public Color getHighlightInnerColor(Component c)   {
		return componentBackground != null ? componentBackground : c.getBackground();
    }

    public Color getShadowInnerColor(Component c)      {
    	return componentBackground != null ? componentBackground :c.getBackground();
    }
}
