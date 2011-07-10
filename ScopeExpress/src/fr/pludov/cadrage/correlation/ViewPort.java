package fr.pludov.cadrage.correlation;

import fr.pludov.cadrage.utils.WeakListenerCollection;

/**
 * 	Les viewports sont des endroits que l'on souhaite conserver indépendemment des photos
 * @author Ludovic POLLET
 *
 */
public class ViewPort implements CorrelationArea {
	public final WeakListenerCollection<ViewPortListener> listeners = new WeakListenerCollection<ViewPortListener>(ViewPortListener.class);
	
	private String viewPortName;
	private double width;
	private double height;
	private double tx;
	private double ty;
	private double cs;
	private double sn;

	public ViewPort(ViewPort copy)
	{
		this.viewPortName = copy.viewPortName;
		this.width = copy.width;
		this.height = copy.height;
		this.tx = copy.tx;
		this.ty = copy.ty;
		this.cs = copy.cs;
		this.sn = copy.sn;

	}
	
	public ViewPort() {
	}

	public String getViewPortName() {
		return viewPortName;
	}

	public void setViewPortName(String viewPortName) {
		this.viewPortName = viewPortName;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getTx() {
		return tx;
	}

	public void setTx(double tx) {
		if (this.tx != tx) {
			this.tx = tx;
			listeners.getTarget().viewPortMoved(this);
		}
	}

	public double getTy() {
		return ty;
	}

	public void setTy(double ty) {
		if (this.ty != ty) {
			this.ty = ty;
			listeners.getTarget().viewPortMoved(this);
		}
	}

	public double getCs() {
		return cs;
	}

	public void setCs(double cs) {
		if (this.cs != cs) {
			this.cs = cs;
			listeners.getTarget().viewPortMoved(this);
		}
	}

	public double getSn() {
		return sn;
	}

	public void setSn(double sn) {
		if (this.sn != sn) {
			this.sn = sn;
			listeners.getTarget().viewPortMoved(this);
		}
	}
}