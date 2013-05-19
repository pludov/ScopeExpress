package fr.pludov.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class MedianCalculator {

	private static class Entry {
		double weight, value;
	}
	
	List<Entry> entries = new ArrayList<Entry>();
	
	public MedianCalculator() {
	}

	public void addEntry(double weight, double value)
	{
		Entry e = new Entry();
		e.weight = weight;
		e.value = value;
		entries.add(e);
	}
	
	public double getMedian()
	{
		// Trier par valeur, prendre le weight du milieu
		Collections.sort(entries, new Comparator<Entry>() {
			@Override
			public int compare(Entry o1, Entry o2) {
				return Double.compare(o1.value, o2.value);
			}
		});
		
		double totalWeight = 0;
		for(Entry e : entries)
		{
			totalWeight += e.weight;
		}
		
		double leftWeight = totalWeight / 2;
		for(Iterator<Entry> it = entries.iterator(); it.hasNext() && leftWeight > 0; )
		{
			Entry e = it.next();
			leftWeight -= e.weight;
			if (leftWeight < 0) {
				return e.value;
			}
		}
		
		return Double.NaN;
	}
	
}
