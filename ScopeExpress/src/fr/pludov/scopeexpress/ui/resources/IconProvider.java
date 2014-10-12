package fr.pludov.scopeexpress.ui.resources;

import java.awt.Image;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class IconProvider {

	public static enum IconSize {
		IconSizeButton(16, 16);
		
		private final int expectedWidth;
		private final int expectedHeight;
		
		IconSize(int expectedWidth, int expectedHeight)
		{
			this.expectedWidth = expectedWidth;
			this.expectedHeight = expectedHeight;
		}
	}
	
	public static Icon getIcon(String fileName, IconSize iconSize)
	{
		ImageIcon imageicon = new ImageIcon(IconProvider.class.getResource("/fr/pludov/scopeexpress/ui/resources/icons/" + fileName + ".png"));
		
		int width, height;
		width = imageicon.getIconWidth();
		height = imageicon.getIconHeight();
		
		double scale = 1.0;
		
		if (width > iconSize.expectedWidth)
		{
			scale = iconSize.expectedWidth * 1.0 / width;
		}
		
		if (height > iconSize.expectedHeight)
		{
			double s2 = iconSize.expectedHeight * 1.0 / height;
			if (s2 < scale) scale = s2;
		}
		
		if (scale != 1.0) {
			Image img = imageicon.getImage();
			img = img.getScaledInstance( (int)(Math.round(width) * scale), (int)(Math.round(height) * scale),  java.awt.Image.SCALE_SMOOTH );  
			imageicon = new ImageIcon( img );
		}
	
		return imageicon; 	
	}
}
