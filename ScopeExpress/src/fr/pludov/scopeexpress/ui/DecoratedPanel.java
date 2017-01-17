package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;

import fr.pludov.scopeexpress.ui.vector.*;;

public class DecoratedPanel extends JPanel {
	final ItemStack itemStack;
	
	public DecoratedPanel()
	{
		super();
		itemStack = new ItemStack(this, ()->(new AffineTransform()));
	}
	
	public void clearItems()
	{
		itemStack.clearItems();
	}
	
	public void addItem(Item i)
	{
		itemStack.addItem(i);
	}
	
	@Override
	public void paint(Graphics gPaint) {
		super.paint(gPaint);
		itemStack.paint((Graphics2D)gPaint);
	}
	
	
}
