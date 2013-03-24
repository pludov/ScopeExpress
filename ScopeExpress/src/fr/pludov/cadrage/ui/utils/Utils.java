package fr.pludov.cadrage.ui.utils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComboBox;
import javax.swing.JTextField;

public final class Utils {

	public static boolean equalsWithNullity(Object a, Object b)
	{
		return ((a == null) == (b == null)) && (a == null || a.equals(b)); 
	}
	
	private Utils() {
		// TODO Auto-generated constructor stub
	}

	public static void addComboChangeListener(JComboBox field, final Runnable listener)
	{
		field.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				listener.run();
			}	
		});
		field.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				listener.run();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				
			}
		});
	}
	
	public static void addTextFieldChangeListener(JTextField field, final Runnable listener)
	{
		field.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				listener.run();
			}	
		});
		field.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				listener.run();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				
			}
		});
	}

	public static String doubleToString(double d, int pres)
	{
		return String.format("%." + Integer.toString(pres) +"f", d);
	}

}
