package fr.pludov.scopeexpress.scope.ascom;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.jawin.COMException;
import org.jawin.DispatchPtr;

import fr.pludov.scopeexpress.platform.windows.Ole;
import fr.pludov.scopeexpress.scope.Scope;
import fr.pludov.scopeexpress.scope.ScopeChoosedCallback;
import fr.pludov.scopeexpress.scope.ScopeIdentifier;
import fr.pludov.scopeexpress.scope.ScopeListedCallback;
import fr.pludov.scopeexpress.scope.ScopeProvider;

public class AscomScopeProvider implements ScopeProvider {

	
	
	
	private void asyncListScopes(final ScopeListedCallback onDone)
	{
		
		List<AscomScopeIdentifier> result = null;
		try {
			Ole.initOle();
			try {
				result = listScopeIdentifiers();
				
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
//				Ole32.CoUninitialize();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		final List<AscomScopeIdentifier> fResult = result;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				onDone.onScopeListed(fResult);
			}
		});
		
	}

	private List<AscomScopeIdentifier> listScopeIdentifiers() throws COMException {
		List<AscomScopeIdentifier> result;
		DispatchPtr app = new DispatchPtr("ASCOM.Utilities.Profile");
					  
		Object tmp = app.invoke("RegisteredDevices", "Telescope");
		
		List list = Ole.readList((DispatchPtr)tmp);
		result = new ArrayList<AscomScopeIdentifier>();
		for(DispatchPtr dp : (List<DispatchPtr>) list)
		{
			Object key = dp.get("Key");
			Object value = dp.get("Value");
		
			AscomScopeIdentifier si = new AscomScopeIdentifier((String)key, (String)value);
			result.add(si);
		}
		return result;
	}
	
	private void asyncChooseScope(final String prefered, final ScopeChoosedCallback onDone)
	{
		
		ScopeIdentifier result = null;
		try {
			Ole.initOle();
			try {
				DispatchPtr app = new DispatchPtr("ASCOM.Utilities.Chooser");
				
				app.put("DeviceType", "Telescope");
			  
				String resultId = (String)app.invoke("choose", prefered);
				
				if (resultId != null && !"".equals(resultId)) {
					for(AscomScopeIdentifier si : listScopeIdentifiers())
					{
						if (si.classId.equals(resultId)) {
							result = si;
							break;
						}
					}
					if (result == null) {
						result = new AscomScopeIdentifier(resultId, resultId);
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
//				Ole32.CoUninitialize();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		final ScopeIdentifier fResult = result;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				onDone.onScopeChoosed(fResult);
			}
		});
		
	}
	
	@Override
	public void listScopes(final ScopeListedCallback onListed) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				asyncListScopes(onListed);
			}
		}).start();
	}

	@Override
	public boolean canChooseScope() {
		return true;
	}

	@Override
	public void chooseScope(final String prefered, final ScopeChoosedCallback onChoose) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				asyncChooseScope(prefered, onChoose);
			}
		}).start();

	}

	@Override
	public Scope buildScope(ScopeIdentifier si) {
		return new AscomScope(((AscomScopeIdentifier)si).classId);
	}

	@Override
	public ScopeIdentifier buildIdFor(String storedId) {
		int dash = storedId.indexOf('#');
		String classId, title;
		if (dash != -1) {
			classId = storedId.substring(0, dash);
			title = storedId.substring(dash + 1);
		} else {
			classId = storedId;
			title = classId;
		}
		return new AscomScopeIdentifier(classId, title);
	}
	
}
