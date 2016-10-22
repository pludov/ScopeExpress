package fr.pludov.scopeexpress.tasks.focuser;

import java.util.*;

/** 
 * Représente un programme de filtre + durée
 * 
 *  Filtre  bin   expo  repeat 
 * 
 * Le filtre peut être "ignorer"
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
