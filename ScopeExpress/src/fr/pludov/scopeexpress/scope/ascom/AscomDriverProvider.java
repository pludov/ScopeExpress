package fr.pludov.scopeexpress.scope.ascom;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.jawin.COMException;
import org.jawin.DispatchPtr;

import fr.pludov.scopeexpress.platform.windows.Ole;
import fr.pludov.scopeexpress.scope.Scope;
import fr.pludov.scopeexpress.scope.DeviceChoosedCallback;
import fr.pludov.scopeexpress.scope.DeviceIdentifier;
import fr.pludov.scopeexpress.scope.DeviceListedCallback;
import fr.pludov.scopeexpress.ui.DriverProvider;
import fr.pludov.scopeexpress.ui.IDeviceBase;

public abstract class AscomDriverProvider<HARDWARE extends IDeviceBase> implements DriverProvider<HARDWARE>{

	final String ascomType;
	
	public AscomDriverProvider(String ascomType)
	{
		this.ascomType = ascomType;
	}
	
	
	private void asyncListScopes(final DeviceListedCallback onDone)
	{
		
		List<AscomDeviceIdentifier> result = null;
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
		final List<AscomDeviceIdentifier> fResult = result;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				onDone.onDeviceListed(fResult);
			}
		});
		
	}

	private List<AscomDeviceIdentifier> listScopeIdentifiers() throws COMException {
		List<AscomDeviceIdentifier> result;
		DispatchPtr app = new DispatchPtr("ASCOM.Utilities.Profile");
					  
		Object tmp = app.invoke("RegisteredDevices", ascomType);
		
		List list = Ole.readList((DispatchPtr)tmp);
		result = new ArrayList<AscomDeviceIdentifier>();
		for(DispatchPtr dp : (List<DispatchPtr>) list)
		{
			Object key = dp.get("Key");
			Object value = dp.get("Value");
		
			AscomDeviceIdentifier si = new AscomDeviceIdentifier((String)key, (String)value);
			result.add(si);
		}
		return result;
	}
	
	private void asyncChooseScope(final String prefered, final DeviceChoosedCallback onDone)
	{
		
		DeviceIdentifier result = null;
		try {
			Ole.initOle();
			try {
				DispatchPtr app = new DispatchPtr("ASCOM.Utilities.Chooser");
				
				app.put("DeviceType", ascomType);
			  
				String resultId = (String)app.invoke("choose", prefered);
				
				if (resultId != null && !"".equals(resultId)) {
					for(AscomDeviceIdentifier si : listScopeIdentifiers())
					{
						if (si.classId.equals(resultId)) {
							result = si;
							break;
						}
					}
					if (result == null) {
						result = new AscomDeviceIdentifier(resultId, resultId);
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
		final DeviceIdentifier fResult = result;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				onDone.onDeviceChoosed(fResult);
			}
		});
		
	}
	
	@Override
	public void listDevices(final DeviceListedCallback onListed) {
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
	public void chooseDevice(final String prefered, final DeviceChoosedCallback onChoose) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				asyncChooseScope(prefered, onChoose);
			}
		}).start();

	}
	
	public abstract HARDWARE buildDevice(DeviceIdentifier si);


	@Override
	public DeviceIdentifier buildIdFor(String storedId) {
		int dash = storedId.indexOf('#');
		String classId, title;
		if (dash != -1) {
			classId = storedId.substring(0, dash);
			title = storedId.substring(dash + 1);
		} else {
			classId = storedId;
			title = classId;
		}
		return new AscomDeviceIdentifier(classId, title);
	}

}
