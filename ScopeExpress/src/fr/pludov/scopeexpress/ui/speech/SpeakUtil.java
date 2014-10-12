package fr.pludov.scopeexpress.ui.speech;

public class SpeakUtil {

	/** Lit un angle positif, avec une précision dépendant de la taille de l'angle */
	public static String sayPositiveAngle(double d)
	{
		assert(d >= 0);
		int deg = (int)Math.floor(d);
		d = (d - deg) * 60;
		int min = (int)Math.floor(d);
		d = (d - min) * 60;
		double sec = d;
		int isec = (int)Math.floor(sec);
	
		if (deg > 5) {
			return deg + " degrés";
		}
		if (deg > 0) {
			return deg + " degrés et " + min + " minutes";  
		}
		
		// deg est null.
		if (min > 5) {
			return min + " minutes";
		}
		if (min > 0) {
			return min + " minutes et " + isec + " secondes";
		}
		// min est null
		if (sec > 5) {
			return isec + " secondes";
		}
		
		int dixieme = (int)Math.floor(10 * (sec - isec));
		if (isec > 0) {
			return isec + " secondes et " + dixieme + " dixièmes";
		} else {
			return dixieme + " dixièmes de secondes";
		}
	}

	public static String sign(Double diffAngle) {
		if (diffAngle == 0.0) {
			return "";
		}
		if (diffAngle >= 0.0) {
			return "plus";
		} else {
			return "moins";
		}
	}

}
