package fr.pludov.io;

public class PPMImage {
	int width, height;
	char [] data;
	
	PPMImage(int width, int height)
	{
		this.width = width;
		this.height = height;
		this.data = new char[width * height];
	}
	
}
