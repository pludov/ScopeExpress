package fr.pludov.scopeexpress.tasks.steps;

import java.util.*;

class Utils {

	static String indent(String s)
	{
		if (s.indexOf('\n') == -1) {
			return "    " + s;
		}
		StringBuilder result = new StringBuilder();
		for(String item : s.split("\\n"))
		{
			result.append("    ");
			result.append(item);
			result.append("\n");
		}
		return result.toString();
	}
	
	static String toBlockString(String title, List<Step> childs)
	{
		StringBuilder result = new StringBuilder();
		result.append(title);
		result.append("{\n");
		for(Step child : childs)
		{
			result.append(Utils.indent(child.toString()));
			result.append("\n");
		}
		result.append("}\n");
		return result.toString();

	}

	static String lambdaToString(Object condition) {
		return condition.toString();
	}
}
