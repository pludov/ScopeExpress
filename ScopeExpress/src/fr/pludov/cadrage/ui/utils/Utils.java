package fr.pludov.cadrage.ui.utils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public final class Utils {

	public static boolean equalsWithNullity(Object a, Object b)
	{
		return ((a == null) == (b == null)) && (a == null || a.equals(b)); 
	}
	
	private Utils() {
		// TODO Auto-generated constructor stub
	}

	public static void addCheckboxChangeListener(JCheckBox box, final Runnable listener)
	{
		box.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				listener.run();
			}	
		});
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

	public static String repeat(char ch, int length) {
		char[] chars = new char[length];
	    Arrays.fill(chars, ch);
	    return new String(chars);
	}

}
