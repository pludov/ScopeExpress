package fr.pludov.scopeexpress.ui;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import fr.pludov.scopeexpress.ui.vector.*;;

public class DecoratedPanel extends JPanel {
	List<Item> items = new ArrayList<>();

	public void clearItems()
	{
		this.items.clear();
		// Trigger update.
		repaint(50);
	}
	
	public void addItem(Item i)
	{
		this.items.add(i);
		// Trigger update.
		repaint(50);
	}
	
	@Override
	public void paint(Graphics gPaint) {
		super.paint(gPaint);
		
		Graphics2D g2d = (Graphics2D)gPaint;
    	
    	Item.draw(g2d, items);

	}
}
