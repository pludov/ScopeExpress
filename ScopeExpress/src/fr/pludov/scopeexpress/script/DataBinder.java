package fr.pludov.scopeexpress.script;

import java.util.*;

import org.mozilla.javascript.*;

/** 
 * Copie des données depuis le JS vers un display
 * Le push n'est fait que au changement (de representation json)
 */
public class DataBinder {	
	boolean first;
	// JSON representation
	String data;
	
	JSTask jsTask;
	NativeFunction loadData;
	NativeFunction writeData;
	
	DataBinder(JSTask jsTask, NativeFunction load, NativeFunction write) {
		this.jsTask = jsTask;
		this.loadData = load;
		this.writeData = write;
		this.first = true;
	}

	void perform()
	{
		try {
			Object oldData = data;
			Object newData = this.loadData.call(Context.getCurrentContext(), jsTask.scope, jsTask.scope, new Object[0]);
			data = (String)NativeJSON.stringify(Context.getCurrentContext(), jsTask.scope, newData, null, 0);
			if (first) {
				first = false;
			} else {
				// Test for equals in json
				if (Objects.equals(oldData, data)) {
					return;
				}
			}
			
			
			this.writeData.call(Context.getCurrentContext(), jsTask.scope, jsTask.scope, new Object[]{newData});
			
		} catch(Throwable t) {
			t.printStackTrace();
		}
		return;
	}

}
