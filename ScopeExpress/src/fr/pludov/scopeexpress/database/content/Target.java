package fr.pludov.scopeexpress.database.content;

import java.util.*;

import fr.pludov.scopeexpress.catalogs.*;
import fr.pludov.scopeexpress.database.*;

public class Target extends BaseDatabaseItem<Root> {
	private static final long serialVersionUID = -2903715210221035092L;

	String name;
	Double ra, dec;
	
	long creationDate;
	long lastUseDate;
	long totalExposureMs;
	
	public Target(Database<Root> container) {
		super(container);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getRa() {
		return ra;
	}

	public void setRa(Double ra) {
		this.ra = ra;
	}

	public Double getDec() {
		return dec;
	}

	public void setDec(Double dec) {
		this.dec = dec;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getLastUseDate() {
		return lastUseDate;
	}

	public void setLastUseDate(long lastUseDate) {
		this.lastUseDate = lastUseDate;
	}

	public long getTotalExposureMs() {
		return totalExposureMs;
	}

	public void setTotalExposureMs(long totalExposureMs) {
		this.totalExposureMs = totalExposureMs;
	}
	
	public List<String> findPossibleNames(double degDistance)
	{
		if (ra == null || dec == null) {
			return Collections.emptyList();
		}
		
		
		List<String> result = new ArrayList<>();
		for(NGCIC.ElementWithDistance ewd : NGCIC.getInstance().findByTarget(ra, dec, degDistance))
		{
			if (ewd.getElement().getAka() == null) {
				continue;
			}
			if (ewd.getElement().getAka().isEmpty()) {
				continue;
			}
			String item = ewd.getElement().getAka().get(0) + " " + ewd.getElement().getType(); 
			result.add(item);
		}
		
		return result;
	}
	
}
