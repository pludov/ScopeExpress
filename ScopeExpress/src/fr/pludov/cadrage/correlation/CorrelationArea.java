package fr.pludov.cadrage.correlation;

public interface CorrelationArea {
	public double getWidth();
	public double getHeight();
	public double getTx();
	public double getTy();
	public double getCs();
	public double getSn();
	
	public void setTx(double a);
	public void setTy(double a);
	public void setCs(double a);
	public void setSn(double a);
}
