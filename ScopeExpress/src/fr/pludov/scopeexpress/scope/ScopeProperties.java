package fr.pludov.scopeexpress.scope;

public class ScopeProperties {
	Double focale;
	
	public ScopeProperties() {
	}

	public Double getFocale() {
		return focale;
	}

	public void setFocale(Double focale) {
		this.focale = focale;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((focale == null) ? 0 : focale.hashCode());
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
		ScopeProperties other = (ScopeProperties) obj;
		if (focale == null) {
			if (other.focale != null)
				return false;
		} else if (!focale.equals(other.focale))
			return false;
		return true;
	}

}
