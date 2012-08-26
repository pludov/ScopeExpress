package fr.pludov.cadrage.ui.utils;

public final class Utils {

	public static boolean equalsWithNullity(Object a, Object b)
	{
		return ((a == null) == (b == null)) && (a == null || a.equals(b)); 
	}
	
	private Utils() {
		// TODO Auto-generated constructor stub
	}

}
