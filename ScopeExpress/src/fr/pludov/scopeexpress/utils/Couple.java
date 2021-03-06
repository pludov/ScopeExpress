package fr.pludov.scopeexpress.utils;

public class Couple<A, B> {
	final A a;
	final B b;
	
	public Couple(A a, B b) {
		this.a = a;
		this.b = b;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Couple other = (Couple) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "Couple [a=" + a + ", b=" + b + "]";
	}

	public A getA() {
		return a;
	}

	public B getB() {
		return b;
	}
	
	

}
