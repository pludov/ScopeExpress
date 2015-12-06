package fr.pludov.scopeexpress.supervision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 
 * Une event source peut �tre : phd, photos, ... 
 * 
 * Il y a plusieurs canaux qui produisent des �venements:
 *   - guiding_data => Frame, Time, mount
 *   - 
 *   
 * Une alarme est produite sur des conditions sur l'�venement:
 *   - valeur d�pass� sur un argument,
 *   - pas d'�venement depuis plus de X secondes
 *   - ...
 *   
 *   
 * L'evenement � la forme d'un id de source avec un objet:
 *   phd/guiding_data  {x, y, z, ... }
 *   .previous => les donn�es du message pr�c�dent
 *   .current => les donn�es courante
 *   .history => toutes les donn�es re�ues
 * Chaque evenement doit avoir une heure
 */
public class EventSource {
	final String path;
	final String [] fields;
	final List<Event> allTimeEvents;
	final Supervisor supervisor;
	
	public EventSource(Supervisor supervisor, String path, String [] fields) {
		this.supervisor = supervisor;
		this.path = path;
		this.fields = fields;
		allTimeEvents = new ArrayList<>();
		this.supervisor.sources.put(path, this);
	}

	public void emit(Event event) {
		System.out.println(path + ":" + event.when + " " + Arrays.toString(event.values));
		// FIXME: de la synchro...
		allTimeEvents.add(event);
	}

	public List<String> getFields() {
		return Arrays.asList(fields);
	}
	
	public List<Event> getEvents()
	{
		return allTimeEvents;
	}

}
