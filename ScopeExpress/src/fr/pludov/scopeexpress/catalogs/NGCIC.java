package fr.pludov.scopeexpress.catalogs;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import com.google.gson.*;
import com.google.gson.stream.*;

import fr.pludov.scopeexpress.focus.*;

// Représente le catalogue NGCIC...
public class NGCIC {

	public static class Element
	{
		String type;
		List<String> aka;
		double ra_deg, dec_deg; 
		double mag, mag_photo;
		
		float a, b, c;
		
		double getRa()
		{
			return ra_deg;
		}
		
		double getDec()
		{
			return dec_deg;
		}
		
		void loadSkyProjection(double[] tmp, double [] sky3d)
		{
			if (a == 0 && b == 0 && c == 0) {
				tmp[0] = ra_deg;
				tmp[1] = dec_deg;
				SkyProjection.convertRaDecTo3D(tmp, sky3d);
				a = (float)sky3d[0];
				b = (float)sky3d[1];
				c = (float)sky3d[2];
			} else {
				sky3d[0] = a;
				sky3d[1] = b;
				sky3d[2] = c;
			}
			
		}

		public String getType() {
			return type;
		}

		public List<String> getAka() {
			return aka;
		}

		public double getRa_deg() {
			return ra_deg;
		}

		public double getDec_deg() {
			return dec_deg;
		}

		public double getMag() {
			return mag;
		}

		public double getMag_photo() {
			return mag_photo;
		}
	}
	
	public static class ElementWithDistance implements Comparable<ElementWithDistance>
	{
		Element element;
		double dst;
		
		ElementWithDistance(Element e, double dst) {
			this.element = e;
			this.dst = dst;
		}

		public double getDst() {
			return dst;
		}

		public Element getElement() {
			return element;
		}

		@Override
		public int compareTo(ElementWithDistance o) {
			return Double.compare(this.dst, o.dst);
		}
	}
	
	public static class Catalog
	{
		List<Element> content;
	}
	
    public static class DoubleTypeAdapter extends TypeAdapter<Double>{
	    @Override
	    public Double read(JsonReader reader) throws IOException {
	        if(reader.peek() == JsonToken.NULL){
	            reader.nextNull();
	            return null;
	        }
	        
	        try{
	        	double d = reader.nextDouble();
	        	return d;
	        }catch(NumberFormatException e){
	        	reader.skipValue();
	            return Double.NaN;
	        }
	    }
	    @Override
	    public void write(JsonWriter writer, Double value) throws IOException {
	        if (value == null) {
	            writer.nullValue();
	            return;
	        }
	        writer.value(value);
	    }
	}
    
    List<Element> content;
	
	public NGCIC()
	{
		content = new ArrayList<>();
	}
	
	void read() throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Double.class, new DoubleTypeAdapter()).registerTypeAdapter(double.class, new DoubleTypeAdapter()).create();

		URL resource = this.getClass().getClassLoader().getResource("fr/pludov/scopeexpress/catalogs/ngcic.json.gz");
		try(InputStream is = resource.openStream();
				GZIPInputStream gzis = new GZIPInputStream(is);
				Reader reader = new InputStreamReader(gzis, "utf-8"))
		{
			Catalog newParameters = gson.fromJson(reader, Catalog.class);
			
			
			
			String [] rejectList = new String [] {
				//"Open cluster w/nebulosity",
				"Globular cluster in SMC",
				"Diffuse neb. in LMC",
				//"Open cluster(s)",
				"Diff. neb. in galaxy",
				"Triple Star",
				"Open cluster?",
				//"Asterism",
				//"Bright or Diffuse Nebula",
				//"Diffuse Nebula",
				"Part of galaxy",
				"Unknown / Unverified",
				"Clus. w/neb. in SMC",
				"Glob. cluster in SMC",
				"Single Star",
				"HII region in galaxy",
				//"Globular cluster",
				"Open Cluster in LMC",
				"Milky Way star cloud",
				// "Planetary nebula",
				"Double Star",
				// "Open Cluster",
				"Unknown/Unverified",
				"Star cloud in galaxy",
				// "Cluster w/nebulosity",
				// "Open cluster",
				"Open cluster in SMC",
				"Triple star",
				"Double star",
				"Bright nebula in LMC",
				"Diffuse neb. in SMC",
				"Single star",
				"Glob. cluster in LMC",
				//"Clus. w/neb",
				"Open cluster in LMC",
				//"Diffuse nebula",
				//"Galaxy",
				"Supernova remnant",
				"Star cloud in Milky Way",
				"Globular cluster in LMC",
				"Clus. w/neb. in LMC",
				// "Bright nebula",
				"Part of Galaxy",
				//"Bright nebula?"
			};
			Set<String> reject = new HashSet<>(Arrays.asList(rejectList));
			content = new ArrayList<>();
			for(Element e : newParameters.content)
			{
				if (reject.contains(e.getType())) {
					continue;
				}
				content.add(e);
			}
		}
	}
	
	
	public List<ElementWithDistance> findByTarget(double ra, double dec, double dstMax)
	{
		double [] radec = new double[] {ra, dec};
		double [] sky3d = new double[3];
		double [] element3d = new double[3];
		SkyProjection.convertRaDecTo3D(radec, sky3d);
		
		double skyDstMax = SkyProjection.radDst2Sky3dDst(dstMax * Math.PI/180);
		double skyDstMax2 = skyDstMax * skyDstMax;
		List<ElementWithDistance> result = new ArrayList<>();
		for(Element e : content) 
		{
			e.loadSkyProjection(radec, element3d);
			double dst2 = 
					(sky3d[0] - element3d[0]) * (sky3d[0] - element3d[0])
					+ (sky3d[1] - element3d[1]) * (sky3d[1] - element3d[1])
					+ (sky3d[2] - element3d[2]) * (sky3d[2] - element3d[2]);
		
			if (dst2 < skyDstMax2) {
				result.add(new ElementWithDistance(e, SkyProjection.sky3dDst2Rad(Math.sqrt(dst2))* 180.0/Math.PI));
			}
		}
		
		Collections.sort(result);
		
		return result;
		
	}
	

	static NGCIC singleton;
	public synchronized static NGCIC getInstance()
	{
		if (singleton == null) {
			singleton = new NGCIC();
			try {
				singleton.read();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
		return singleton;
	}
	
	public static void main(String[] args) {
		long t = System.currentTimeMillis(); 
			getInstance();
		t = System.currentTimeMillis() - t;
		System.out.println("took:" + t);
	}
}
