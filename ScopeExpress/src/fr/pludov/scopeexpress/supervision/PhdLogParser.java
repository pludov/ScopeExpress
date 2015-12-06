package fr.pludov.scopeexpress.supervision;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;

public class PhdLogParser extends BaseLogParser {
	final EventSource phdMessages;
	final EventSource phdGuidingData;
	final EventSource phdStatus;

	static enum PhdStatus {
		Idle, Calibration, Guiding;
	};
	
	
	
	File myDocuments = new JFileChooser().getFileSystemView().getDefaultDirectory();

	public PhdLogParser(Supervisor sup) {
		super(sup);
		phdStatus = new EventSource(sup, "phd.status", new String[]{"status"});
		phdMessages = new EventSource(sup, "phd.messages", new String[]{"log"});
		phdGuidingData = new EventSource(sup, "phd.guiding", new String[]{
				"Frame",
				"Time",
				"mount",
				"dx",
				"dy",
				"RARawDistance",
				"DECRawDistance",
				"RAGuideDistance",
				"DECGuideDistance",
				"RADuration",
				"RADirection",
				"DECDuration",
				"DECDirection",
				"XStep",
				"YStep",
				"StarMass",
				"SNR",
				"ErrorCode",
				"ErrorDetails"
		});
	}
	
	PhdStatus currentStatus;
	long currentStatusStartTime;
	
	@Override
	protected void fileStarted(File file) {
		currentStatus = PhdStatus.Idle;
		phdStatus.emit(new Event(phdStatus, System.currentTimeMillis(), PhdStatus.Idle.name()));
	}
	
	private void updateStatus(PhdStatus status, Matcher m)
	{
		currentStatus = status;
		if (m != null) {
			Date eventDate = new Date(
					Integer.parseInt(m.group(1)) - 1900,
					Integer.parseInt(m.group(2)) - 1,
					Integer.parseInt(m.group(3)),
					Integer.parseInt(m.group(4)),
					Integer.parseInt(m.group(5)),
					Integer.parseInt(m.group(6))
					);
			currentStatusStartTime = eventDate.getTime();
		} else {
			currentStatusStartTime = System.currentTimeMillis();
		}
		phdStatus.emit(new Event(phdStatus, currentStatusStartTime, currentStatus.name()));
	}
	
	private static Integer parseInt(String str)
	{
		if (str == null || str.isEmpty()) {
			return null;
		}
		return Integer.parseInt(str);
	}
	
	private static Double parseDouble(String str)
	{
		if (str == null || str.isEmpty()) {
			return null;
		}
		return Double.parseDouble(str);
	}
	
	private static String [] decodeCsv(String s) throws IOException {
		List<StringBuffer> items = new ArrayList<>();
		StringBuffer current = new StringBuffer();
		items.add(current);
		int rpos = 0;
		while(rpos < s.length())
		{
			char c = s.charAt(rpos++);
			if (c == ',') {
				items.add(current = new StringBuffer());
			} else if (c == '"') {
				while(true) {
					if (rpos == s.length()) {
						throw new IOException("Malformed csv");
					}
					c = s.charAt(rpos++);
					if (c == '"' && rpos < s.length() && s.charAt(rpos) == '"') {
						// Double '"' => un seul
						rpos ++;
						current.append('"');
					} else if (c == '"') {
						break;
					} else {
						current.append(c);
					}
				}
			} else {
				current.append(c);
			}
		}
		
		String [] result = new String[items.size()];
		for(int i = 0; i < items.size(); ++i) {
			result[i] = items.get(i).toString();
		}
		return result;
	}
	
	@Override
	protected void lineReceived(byte[] line, boolean emitEvent) throws IOException {
		
		String string = StandardCharsets.ISO_8859_1.decode(ByteBuffer.wrap(line)).toString();
		
		Pattern calibrationBeginsPattern = Pattern.compile("^Calibration Begins at (\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})$");
		
		Matcher m = calibrationBeginsPattern.matcher(string);
		if (m.matches()) {
			updateStatus(PhdStatus.Calibration, m);
			return;
		}
		
		Pattern calibrationEndPattern = Pattern.compile("^(RA|DEC) Calibration Failed:");
		m = calibrationEndPattern.matcher(string);
		if (m.matches()) {
			updateStatus(PhdStatus.Idle, null);
			return;
		}
	
		Pattern guidingBeginPattern = Pattern.compile("^Guiding Begins at (\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})$");
		m = guidingBeginPattern.matcher(string);
		if (m.matches()) {
			updateStatus(PhdStatus.Guiding, m);
			return;
		}
		
		Pattern guidingEndsPattern = Pattern.compile("^Guiding Ends at");
		m = guidingEndsPattern.matcher(string);
		if (m.matches()) {
			updateStatus(PhdStatus.Idle, null);
			return;
		}
		
		if (string.startsWith("INFO: ")) {
			phdMessages.emit(new Event(phdMessages, System.currentTimeMillis(), string.substring(6)));
			return;
		}
		
		if (currentStatus == PhdStatus.Guiding) {
			String [] values = decodeCsv(string);
			if (values.length >= 18 && values[0].matches("[0-9]+")) {
				Object[] args = new Object[19];
				int i = 0;
				// Frame,
				args[i] = parseInt(values[i]);
				i++;
				// Time
				args[i] = parseDouble(values[i]);
				i++;
				// mount
				args[i] = values[i];
				i++;
				// dx
				args[i] = parseDouble(values[i]);
				i++;
				// dy
				args[i] = parseDouble(values[i]);
				i++;
				// RARawDistance
				args[i] = parseDouble(values[i]);
				i++;
				// DECRawDistance
				args[i] = parseDouble(values[i]);
				i++;
				// RAGuideDistance
				args[i] = parseDouble(values[i]);
				i++;
				// DECGuideDistance
				args[i] = parseDouble(values[i]);
				i++;
				// RADuration
				args[i] = parseInt(values[i]);
				i++;
				// RADirection
				args[i] = values[i];
				i++;
				// DECDuration
				args[i] = parseInt(values[i]);
				i++;
				// DECDirection
				args[i] = values[i];
				i++;
				// XStep
				args[i] = values[i];
				i++;
				// YStep
				args[i] = values[i];
				i++;
				// StarMass
				args[i] = parseInt(values[i]);
				i++;
				// SNR
				args[i] = parseDouble(values[i]);
				i++;
				// ErrorCode
				args[i] = parseInt(values[i]);
				i++;
				if (i < values.length) {
					args[i] = values[i];
					i++;
				}
				
				phdGuidingData.emit(new Event(phdGuidingData, (long) (currentStatusStartTime + 1000 * (Double)args[1]), args));
				
				return;
			}
		}
	}
	
	@Override
	File getLogDir() {
		return new File(myDocuments, "PHD2");
	}

	@Override
	String getPattern() {
		return "PHD2_GuideLog_\\d{4}-\\d{2}-\\d{2}_\\d{6}.txt";
	}

	public static void main(String[] args) {
		Supervisor sup = new Supervisor();
		new PhdLogParser(sup).start();
	}
}
