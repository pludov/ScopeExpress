package fr.pludov.scopeexpress.ui.vector;

import java.awt.*;

public class Graphics2DState
{
	Color color;
	Stroke stroke;
	Composite composite;
	Color background;
	Font font;
	
	Graphics2DState apply(Graphics2D g2d)
	{
		Graphics2DState revert = new Graphics2DState();
		if (color != null) {
			revert.color = g2d.getColor();
			g2d.setColor(color);
		}
		
		if (stroke != null) {
			revert.stroke = g2d.getStroke();
			g2d.setStroke(stroke);
		}
		
		if (composite != null) {
			revert.composite = g2d.getComposite();
			g2d.setComposite(composite);
		}
		
		if (background != null) {
			revert.background = g2d.getBackground();
			g2d.setBackground(background);
		}
		
		if (font != null) {
			revert.font = g2d.getFont();
			g2d.setFont(font);
		}
		
		return revert;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Stroke getStroke() {
		return stroke;
	}

	public void setStroke(Stroke stroke) {
		this.stroke = stroke;
	}

	public Composite getComposite() {
		return composite;
	}

	public void setComposite(Composite composite) {
		this.composite = composite;
	}

	public Color getBackground() {
		return background;
	}

	public void setBackground(Color background) {
		this.background = background;
	}

	public Font getFont() {
		return font;
	}

	public void setFont(Font font) {
		this.font = font;
	}
}