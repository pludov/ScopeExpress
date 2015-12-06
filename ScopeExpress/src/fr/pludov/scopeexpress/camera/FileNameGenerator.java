//package fr.pludov.scopeexpress.camera;
//
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import fr.pludov.scopeexpress.ui.FocusUi;
//
//public class FileNameGenerator {
//	FocusUi focusUi;
//	
//	public FileNameGenerator() {
//	}
//
//	private String performDate(long date, String expression)
//	{
//		
//	}
//	
//	private String perform(String expression)
//	{
//		if (expression.startsWith("NOW")) {
//			return performDate(System.currentTimeMillis(), expression.substring(3));
//		}
//		if (expression.startsWith("SESSION")) {
//			return performDate(focusUi.getStartTime(), expression.substring(7));
//		}
//	}
//	
//	public String construct(String pattern)
//	{
//		Pattern re = Pattern.compile("\\$(.*?)\\$");
//		
//		StringBuffer result = new StringBuffer();
//		Matcher m = re.matcher(pattern);
//		while(m.find()) {
//			String exp = m.group(1);
//			String replacement = "";
//			m.appendReplacement(result, replacement);
//		}
//		m.appendTail(result);
//		
//		return result.toString();
//	}
//	
//}
