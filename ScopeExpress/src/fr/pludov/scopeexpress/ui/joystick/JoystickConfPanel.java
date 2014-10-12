package fr.pludov.scopeexpress.ui.joystick;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import fr.pludov.scopeexpress.ui.FocusUi;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.ControllerEvent;
import net.java.games.input.ControllerListener;
import net.java.games.input.DirectInputEnvironmentPlugin;
import net.miginfocom.swing.MigLayout;

public class JoystickConfPanel extends JoystickConfPanelDesign {

	private boolean isDisplayingControllers;
	private final TriggerSource triggerSource;

	private final JPanel btonsPanel;
	private final MigLayout btonsLayout;
	private final FocusUi focusUi;
	public JoystickConfPanel(FocusUi focusUi) {
		this.focusUi = focusUi;
		this.isDisplayingControllers = false;
		triggerSource = TriggerSourceProvider.getInstance();
		
		this.btnRescan.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				updateCurrentController();
			}
		});
		
		this.btonsPanel = new JPanel() {
			@Override
			public Dimension getPreferredSize() {
				Dimension d = new Dimension(super.getPreferredSize());
				d.width = controllerDetails.getWidth();
				return d;
			}
			
			@Override
			public Dimension getMaximumSize() {
				Dimension d = new Dimension(super.getMaximumSize());
				d.width = controllerDetails.getWidth();
				return d;
			}
		};
		this.btonsPanel.setLayout(btonsLayout = new MigLayout());
		this.btonsPanel.addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
//				int px = btonsPanel.getWidth();
//				btonsLayout.setColumnConstraints("[" + px + "px:" + px + "px:" + px + "px]");				
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		this.controllerDetails.add(this.btonsPanel);
//		refreshTimer = new Timer(0, null);
//		refreshTimer.setCoalesce(true);
//		refreshTimer.setRepeats(true);
//		refreshTimer.setDelay(1000);
//		refreshTimer.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				// new ControllerEnvironment();
////				new DefaultControllerEn
//				ControllerEnvironment.getDefaultEnvironment().getControllers();
//				System.out.println("refreshing");
//				Object o = controllerSelector.getSelectedItem();
//				if (o instanceof ControllerItem) {
//					((ControllerItem)o).controller.poll();
//					for(PollingItem p : pollingItems) {
//						p.update();
//					}
//					
//				}
//			}
//		});
		this.addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
				if (isDisplayingControllers && !isDisplayable()) {
					dropControllers();
					JoystickConfPanel.this.focusUi.getJoystickHandler().reload();
					
				}
			}
		});
		this.addAncestorListener(new AncestorListener() {
			
			@Override
			public void ancestorRemoved(AncestorEvent event) {
				if (isDisplayingControllers && !isDisplayable()) {
					dropControllers();
				}
				
			}
			
			@Override
			public void ancestorMoved(AncestorEvent event) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void ancestorAdded(AncestorEvent event) {
				if (isDisplayable() && !isDisplayingControllers) {
					initControllers();
				}
				
			}
		});

		updateCurrentController();
		this.dropControllers();
	}
	
	private void updateCurrentController()
	{
		this.btonsPanel.removeAll();
		int i = 0;
		
		this.btonsLayout.setColumnConstraints("[400px:400px:400px]");
		this.btonsLayout.setRowConstraints("[]");
//		this.btonsLayout.setRows(1);
		for(TriggerInput ti : this.triggerSource.scan())
		{
			JoystickBtonConfPanel buttonPanel = new JoystickBtonConfPanel(ti);
//			GridBagConstraints gbc = new GridBagConstraints();
//			gbc.fill = GridBagConstraints.HORIZONTAL;
//			gbc.gridy = i++;
//			gbc.anchor = GridBagConstraints.NORTH;
//			
//			this.btonsLayout.setRows(i);
			buttonPanel.setSize(400, 20);
			this.btonsPanel.add(buttonPanel, "cell 0 " + (i++)+",growx");
			
			buttonPanel.doLayout();
		}
		
		// FIXME: pkoi ça ne marche pas ???
		this.btonsPanel.doLayout();
		this.controllerDetails.getParent().doLayout();
		this.controllerDetails.doLayout();
//		
		this.controllerDetails.getParent().repaint();
	}
	
	void initControllers()
	{
		isDisplayingControllers = true;
	}
	
	void dropControllers()
	{
		isDisplayingControllers = false;
	}
	
}
