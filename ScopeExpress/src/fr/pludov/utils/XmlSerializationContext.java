package fr.pludov.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IdentityHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlSerializationContext {
	final DocumentBuilderFactory factory;
	final DocumentBuilder builder;
	final Document document;
	
	public XmlSerializationContext() {
		try {
			factory = DocumentBuilderFactory.newInstance();	
			builder = factory.newDocumentBuilder();
			document = builder.newDocument();
		} catch(Exception e) {
			throw new RuntimeException("xml context error", e);
		}
	}

	public final Document getDocument()
	{
		return document;
	}

	public Element newNode(String title)
	{
		return document.createElement(title);
	}
	
	public void setNodeAttribute(Element node, String attribute, String value)
	{
		if (value == null) return;
		node.setAttribute(attribute, value);
	}

	public void setNodeAttribute(Element node, String attribute, int value)
	{
		node.setAttribute(attribute, Integer.toString(value));
	}
	
	public void setNodeAttribute(Element node, String attribute, boolean value)
	{
		node.setAttribute(attribute, value ? "true" : "false");
	}
	
	public void setNodeAttribute(Element node, String attribute, double value)
	{
		String strValue;
		if (Double.isNaN(value))
		{
			strValue = "NaN";
		} else if (value == Double.POSITIVE_INFINITY) {
			strValue = "+inf";
		} else if (value == Double.NEGATIVE_INFINITY) {
			strValue = "-inf";
		} else {
			strValue = new BigDecimal(value).toString();
		}
		node.setAttribute(attribute, strValue);
	}
	
	public void addNodeArray(Element element, String childName, int [] values)
	{
		if (values == null) return;
		Element arrayRoot = newNode(childName);
		
		for(int i = 0; i < values.length; ++i)
		{
			Element arrayItem = newNode("item");
			setNodeAttribute(arrayItem, "value", values[i]);
			arrayRoot.appendChild(arrayItem);
		}
		
		element.appendChild(arrayRoot);
	}
	
	public void save(OutputStream outputStream) throws IOException
	{
		Transformer transformer;
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setAttribute("indent-number", new Integer(2));
			
			transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		} catch(Exception e) {
			throw new RuntimeException("xml saving initialisation error", e);
		}
		
		Result output = new StreamResult(new OutputStreamWriter(outputStream, "utf-8"));
		Source input = new DOMSource(document);

		try {
			transformer.transform(input, output);
		} catch (TransformerException e) {
			throw new IOException("Xml saving error", e);
		}
	}

	
	public class NodeDictionary<Something>
	{
		int nextId = 1;
		final IdentityHashMap<Something, String> dictionary;
		
		public String addNodeForObject(Something sth, Element e)
		{
			String id = Integer.toString(nextId ++);
			dictionary.put(sth, id);
			e.setAttribute("id", id);
			
			return id;
		}
		
		public String getIdForObject(Something sth)
		{
			return dictionary.get(sth);
		}
		
		public NodeDictionary()
		{
			this.dictionary = new IdentityHashMap<Something, String>();
		}
	
	}
	
//	public static void main(String[] args) {
//		for(int i = 0; i < 16; ++i)
//		{
//			double d = Math.random();
//			d *= Math.exp(100 * (Math.random() - 0.5));
//			
//			if (i == 0) {
//				d = Double.NaN;
//			}
//			BigDecimal bd = new BigDecimal(d);
//			
//			double dFromBd = bd.doubleValue();
//			
//			System.out.println(bd.toString() + " => " + (dFromBd == d));
//		}
//	}
}
