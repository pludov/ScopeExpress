package fr.pludov.scopeexpress.ui.joystick;

import java.util.ArrayList;
import java.util.List;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.EventQueue;

public class JoystickManager {
	JoystickManager()
	{
		
		
	}
	
	
	
	
	void toto() {
	    List<Controller> joysticks = new ArrayList<Controller>();
	    
		Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
		
	    for(int i = 0; i < controllers.length; i++){
	        Controller controller = controllers[i];
	        
	        System.out.println("name: " + controller.getName());
	        System.out.println("type: " + controller.getType());

	        if (
	                controller.getType() == Controller.Type.STICK || 
	                controller.getType() == Controller.Type.GAMEPAD || 
	                controller.getType() == Controller.Type.WHEEL ||
	                controller.getType() == Controller.Type.FINGERSTICK
	           )
	        {
	        	joysticks.add(controller);
//	            // Add new controller to the list of all controllers.
//	            foundControllers.add(controller);
//	            
//	            // Add new controller to the list on the window.
//	            window.addControllerName(controller.getName() + " - " + controller.getType().toString() + " type");
	        }
	    }
	    Component.Identifier.Axis axis;
	    
	    
	    while(true) {
		    for(Controller joystick : joysticks)
		    {
		    	if (!joystick.poll()) {
		    		break;
		    	}
		    	System.out.println("joystick:" + joystick.getName());
		    	for(Component component : joystick.getComponents())
		    	{
		    		component.isAnalog();
		    		System.out.println("component: " + component.getName() + "(" + component.getIdentifier().getClass() + ")" + " DZ=" + component.getDeadZone() + "=" + component.getPollData());
		    	}
		    	
//		    	for(Controller controler : joystick.get())
//		    	{
//		    		System.out.println("controler: " + controler.getName() + "=" + controler.get);
//		    	}
		    }
		    
		    try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    }
	}
	
	public static void main(String[] args) {
		JoystickManager m = new JoystickManager();
		m.toto();
	}
}
