package fr.pludov.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EquationSolverBuilder {

	//	On veut optimiser les coefficiant du polynome suivant :
	//
	//		F(x,y) = a.x3+b.x2+c.x + d.y3 + e.y2 + f.y + g.x2y + h.xy2 + i.xy + j
	//
	//		pour minimiser
	//
	//		SUM (F(xi, yi) - vi)²
	//
	//  Avec Maxima, on calcule la somme pour un i, et on dérive pour chaque coefficient du polynome
	//  On va trouver autant de coef qu'il y a d'equation.
	//  Les polynomes suivants sont obtenus par copier collé de ces equation dans eclipse
	String [] derivates = 
		{
			"2*d*x_i^3*y_i^3+2*h*x_i^4*y_i^2+2*e*x_i^3*y_i^2+2*g*x_i^5*y_i+2*i*x_i^4*y_i+2*f*x_i^3*y_i+2*a*x_i^6+2*b*x_i^5+2*c*x_i^4-2*v_i*x_i^3+2*j*x_i^3",
			"2*d*x_i^2*y_i^3+2*h*x_i^3*y_i^2+2*e*x_i^2*y_i^2+2*g*x_i^4*y_i+2*i*x_i^3*y_i+2*f*x_i^2*y_i+2*a*x_i^5+2*b*x_i^4+2*c*x_i^3-2*v_i*x_i^2+2*j*x_i^2",
			"2*d*x_i*y_i^3+2*h*x_i^2*y_i^2+2*e*x_i*y_i^2+2*g*x_i^3*y_i+2*i*x_i^2*y_i+2*f*x_i*y_i+2*a*x_i^4+2*b*x_i^3+2*c*x_i^2-2*v_i*x_i+2*j*x_i",
			"2*d*y_i^6+2*h*x_i*y_i^5+2*e*y_i^5+2*g*x_i^2*y_i^4+2*i*x_i*y_i^4+2*f*y_i^4+2*a*x_i^3*y_i^3+2*b*x_i^2*y_i^3+2*c*x_i*y_i^3-2*v_i*y_i^3+2*j*y_i^3",
			"2*d*y_i^5+2*h*x_i*y_i^4+2*e*y_i^4+2*g*x_i^2*y_i^3+2*i*x_i*y_i^3+2*f*y_i^3+2*a*x_i^3*y_i^2+2*b*x_i^2*y_i^2+2*c*x_i*y_i^2-2*v_i*y_i^2+2*j*y_i^2",
			"2*d*y_i^4+2*h*x_i*y_i^3+2*e*y_i^3+2*g*x_i^2*y_i^2+2*i*x_i*y_i^2+2*f*y_i^2+2*a*x_i^3*y_i+2*b*x_i^2*y_i+2*c*x_i*y_i-2*v_i*y_i+2*j*y_i",
			"2*d*x_i^2*y_i^4+2*h*x_i^3*y_i^3+2*e*x_i^2*y_i^3+2*g*x_i^4*y_i^2+2*i*x_i^3*y_i^2+2*f*x_i^2*y_i^2+2*a*x_i^5*y_i+2*b*x_i^4*y_i+2*c*x_i^3*y_i-2*v_i*x_i^2*y_i+2*j*x_i^2*y_i",
			"2*d*x_i*y_i^5+2*h*x_i^2*y_i^4+2*e*x_i*y_i^4+2*g*x_i^3*y_i^3+2*i*x_i^2*y_i^3+2*f*x_i*y_i^3+2*a*x_i^4*y_i^2+2*b*x_i^3*y_i^2+2*c*x_i^2*y_i^2-2*v_i*x_i*y_i^2+2*j*x_i*y_i^2",
			"2*d*x_i*y_i^4+2*h*x_i^2*y_i^3+2*e*x_i*y_i^3+2*g*x_i^3*y_i^2+2*i*x_i^2*y_i^2+2*f*x_i*y_i^2+2*a*x_i^4*y_i+2*b*x_i^3*y_i+2*c*x_i^2*y_i-2*v_i*x_i*y_i+2*j*x_i*y_i",
			"2*d*y_i^3+2*h*x_i*y_i^2+2*e*y_i^2+2*g*x_i^2*y_i+2*i*x_i*y_i+2*f*y_i+2*a*x_i^3+2*b*x_i^2+2*c*x_i-2*v_i+2*j"
		};
	
	// Retourne le nom de variable pour un facteur sous la forme "x_i^2*y_i^...*v_i" 
	private String getFactorName(String factor)
	{
		return "s" + factor.replace("^", "").replace("*", "").replace("_i", "");
	}
	
	private List<String> declareFactor(String factor, Set<String> doneFactors)
	{
		List<String> result = new ArrayList<String>();
		
		String factorName = getFactorName(factor);
		
		if (!doneFactors.add(factorName)) return result;
		
		result.add("   double " + factorName + " = 0;");
		result.add("   for(int i = 0; i < dataSize; ++i) {");
		
		String javaExpr = null;
		for(String f : factor.replace("_i", "i[i]").split("\\*"))
		{
			int pow = f.indexOf('^');
			if (pow != -1) {
				int count = Integer.parseInt(f.substring(pow + 1));
				String v = f.substring(0, pow);
				f = v;
				for(int i = 1; i < count; ++i)
				{
					f += " * " + v;
				}
			}
			if (javaExpr == null) {
				javaExpr = f;
			} else {
				javaExpr += " * " + f;
			}
		}
		result.add("       " + factorName + " += " + javaExpr + ";");
		result.add("   }");
		return result;
	}
	
	private List<String> splitSum(String sum)
	{
		List<String> result= new ArrayList<String>();
		int index = 0;
		while(index < sum.length())
		{
			int tokenStart = index;
			if (sum.charAt(index) == '+') {
				index++;
				tokenStart++;
			} else if (sum.charAt(index) == '-') {
				index++;
			}
			int nextPlus = sum.indexOf('+', index);
			int nextMinus = sum.indexOf('-', index);
			
			int nextToken;
			if (nextPlus != -1 && nextMinus != -1)
			{
				nextToken = Math.min(nextPlus, nextMinus);
			} else if (nextPlus != -1) {
				nextToken = nextPlus;
			} else if (nextMinus != -1) {
				nextToken = nextMinus;
			} else {
				nextToken = sum.length();
			}
			
			String token = sum.substring(tokenStart, nextToken);
			result.add(token);
			index = nextToken;
		}
		return result;
	}
	
	public EquationSolverBuilder() {
		// On va transformer chaque derivates en système d'équation
		// On va avoir en tout 
		
		String [][] equationExpression = new String[derivates.length][];
		
		// Liste des variables "x_i..."
		Set<String> doneFactors = new HashSet<String>();
		List<String> varDefinitions = new ArrayList<String>();
		
		List<String> valueAffectation = new ArrayList<String>();
		for(int eqid = 0; eqid < derivates.length; ++eqid)
		{
			String derivate = derivates[eqid];
			valueAffectation.add("    // " + derivate);
			for(String monome : splitSum(derivate))
			{
				List<String> factors = Arrays.asList(monome.split("\\*"));
				Collections.sort(factors);
				// Contient uniquement les x_i, y_i, v_i
				String sumFactor = null;	
				Integer coef = 1;
				int coefId = -1;
				for(String factor : factors)
				{
					if (factor.startsWith("x_i") || factor.startsWith("y_i") || factor.startsWith("v_i"))
					{
						if (sumFactor == null) {
							sumFactor = factor;
						} else {
							sumFactor = sumFactor + "*" + factor;
						}
					} else if (factor.length() == 1 && factor.charAt(0) >= 'a' && factor.charAt(0) <= 'z') {
						if (coefId != -1) {
							throw new RuntimeException("more than one factor in " + monome + " found in " + derivate);
						}
						coefId = factor.charAt(0) - 'a';
					} else {
						// On doit avoir un coefficiant entier.
						coef = Integer.parseInt(factor);
					}
				}
				
				String factorVar;
				if (sumFactor != null) {
					varDefinitions.addAll(declareFactor(sumFactor, doneFactors));
					factorVar = getFactorName(sumFactor);
				} else {
					factorVar = "dataSize";
				}
				
				if (coefId != -1) {
					valueAffectation.add("    parameters[" + (eqid * derivates.length + coefId) + "] = " + coef + " * " + factorVar + ";");
				} else {
					valueAffectation.add("    values[" + eqid + "] = " + (-coef) + " * " + factorVar + ";");
				}
			}
		}
		for(String varDef : varDefinitions)
		{
			System.out.println(varDef);
		}
		
		for(String valueAffect : valueAffectation)
		{
			System.out.println(valueAffect);
		}
	}

	public static void main(String[] args) {
		new EquationSolverBuilder();
	}
}
