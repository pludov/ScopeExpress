package fr.pludov.scopeexpress.ui.vector;

public class Text {
	String text;
	int xAlign, yAlign;
	double x, y;
	
	public Text(String str) {
		this.text = str;
	}
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public int getxAlign() {
		return xAlign;
	}
	public void setxAlign(int xAlign) {
		this.xAlign = xAlign;
	}
	public int getyAlign() {
		return yAlign;
	}
	public void setyAlign(int yAlign) {
		this.yAlign = yAlign;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}
	
}