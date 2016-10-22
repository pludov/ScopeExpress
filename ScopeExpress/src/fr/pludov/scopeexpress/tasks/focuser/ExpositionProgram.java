package fr.pludov.scopeexpress.tasks.focuser;

import java.util.*;

/** 
 * Repr�sente un programme de filtre + dur�e
 * 
 *  Filtre  bin   expo  repeat 
 * 
 * Le filtre peut �tre "ignorer"
 * 
 */

public class ExpositionProgram {

	
	class Item {
		// Null si setFilter == false
		String filterName;
		
		int bin;
		
		double exposition;
		
		int shootCount;
	}
	
	List<Item> filterProgram; // min = 1
	
	
	public ExpositionProgram() {
		// TODO Auto-generated constructor stub
	}


	public boolean hasFilter() {
		return !(filterProgram.size() == 1 && filterProgram.get(0).filterName == null);
	}


	/** >= 1 */
	public int getRepeat() {
		return 1;
	}
	
}
