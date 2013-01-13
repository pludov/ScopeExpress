package fr.pludov.cadrage.focus;

public class MosaicImageParameter {
	
	// Positionnement de l'image
	double tx, ty, cs, sn;
	
	// Etat du positionnement de l'image
	
	
	public MosaicImageParameter() {
		this.tx = 0;
		this.ty = 0;
		this.cs = 1.0;
		this.sn = 0.0;
	}

	public double getTx() {
		return tx;
	}

	public void setTx(double tx) {
		this.tx = tx;
	}

	public double getTy() {
		return ty;
	}

	public void setTy(double ty) {
		this.ty = ty;
	}

	public double getCs() {
		return cs;
	}

	public void setCs(double cs) {
		this.cs = cs;
	}

	public double getSn() {
		return sn;
	}

	public void setSn(double sn) {
		this.sn = sn;
	}

}
