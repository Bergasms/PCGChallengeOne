package com.bergasms.pcg;


import java.util.Random;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.math.MathUtils;

public class MidpointDisplacement {
	
	 

	public int n;
	public int wmult, hmult;

	public float smoothness;

	public int width, height;
	public float[][] map;
	
	public MidpointDisplacement(int n_, float smth, int mul) {

		
		// n partly controls the size of the map, but mostly controls the level of detail available
		n = n_;

		// wmult and hmult are the width and height multipliers.  They set how separate regions there are
		wmult=mul;
		hmult=mul;

		// Smoothness controls how smooth the resultant terain is.  Higher = more smooth
		smoothness = smth;
	}

	public int[][] getMap(Random r) {

		// get the dimensions of the map
		int power = (int) Math.pow(2,n);
		width = wmult*power + 1;
		height = hmult*power + 1;

		// initialize arrays to hold values 
		map = new float[width][height];
		int[][] returnMap = new int[width][height];


		int step = power/2;
		float sum;
		int count;

		// h determines the fineness of the scale it is working on.  After every step, h
		// is decreased by a factor of "smoothness"
		float h = 1;

		// Initialize the grid points
		for (int i=0; i<width; i+=2*step) {
			for (int j=0; j<height; j+=2*step) {
				map[i][j] = r.nextFloat() * (2*h);
			}
		}

		// Do the rest of the magic
		while (step > 0) {   
			// Diamond step
			for (int x = step; x < width; x+=2*step) {
				for (int y = step; y < height; y+=2*step) {
					sum = map[x-step][y-step] + //down-left
							map[x-step][y+step] + //up-left
							map[x+step][y-step] + //down-right
							map[x+step][y+step];  //up-right
					map[x][y] = sum/4 + ((r.nextFloat() * 2*h) - h);
				}
			}

			// Square step
			for (int x = 0; x < width; x+=step) {
				for (int y = step*(1-(x/step)%2); y<height; y+=2*step) {
					sum = 0;
					count = 0;
					if (x-step >= 0) {
						sum+=map[x-step][y];
						count++;
					}
					if (x+step < width) {
						sum+=map[x+step][y];
						count++;
					}
					if (y-step >= 0) {
						sum+=map[x][y-step];
						count++;
					}
					if (y+step < height) {
						sum+=map[x][y+step];
						count++;
					}
					if (count > 0) map[x][y] = sum/count + ((r.nextFloat() * 2*h) - h);
					else map[x][y] = 0;
				}

			}
			h /= smoothness;
			step /= 2;
		}

		// Normalize the map
		float max = Float.MIN_VALUE;
		float min = Float.MAX_VALUE;
		for (float[] row : map) {
			for (float d : row) {
				if (d > max) max = d;
				if (d < min) min = d;
			}
		}

		// Use the thresholds to fill in the return map
		for(int row = 0; row < map.length; row++){
			for(int col = 0; col < map[row].length; col++){
				map[row][col] = (map[row][col]-min)/(max-min);
				returnMap[row][col] = (int)(map[row][col] * 256);
			}
		}

		return returnMap;
	}
	
	
	public Pixmap getPixMap(Random r) {

		// get the dimensions of the map
		int power = (int) Math.pow(2,n);
		width = wmult*power + 1;
		height = hmult*power + 1;

		// initialize arrays to hold values 
		map = new float[width][height];
		Pixmap pmap = new Pixmap(width, height, Format.RGB888);


		int step = power/2;
		float sum;
		int count;

		// h determines the fineness of the scale it is working on.  After every step, h
		// is decreased by a factor of "smoothness"
		float h = 1;

		// Initialize the grid points
		for (int i=0; i<width; i+=2*step) {
			for (int j=0; j<height; j+=2*step) {
				map[i][j] = r.nextFloat() * (2*h);
			}
		}

		// Do the rest of the magic
		while (step > 0) {   
			// Diamond step
			for (int x = step; x < width; x+=2*step) {
				for (int y = step; y < height; y+=2*step) {
					sum = map[x-step][y-step] + //down-left
							map[x-step][y+step] + //up-left
							map[x+step][y-step] + //down-right
							map[x+step][y+step];  //up-right
					map[x][y] = sum/4 + ((r.nextFloat() * 2*h) - h);
				}
			}

			// Square step
			for (int x = 0; x < width; x+=step) {
				for (int y = step*(1-(x/step)%2); y<height; y+=2*step) {
					sum = 0;
					count = 0;
					if (x-step >= 0) {
						sum+=map[x-step][y];
						count++;
					}
					if (x+step < width) {
						sum+=map[x+step][y];
						count++;
					}
					if (y-step >= 0) {
						sum+=map[x][y-step];
						count++;
					}
					if (y+step < height) {
						sum+=map[x][y+step];
						count++;
					}
					if (count > 0) map[x][y] = sum/count + ((r.nextFloat() * 2*h) - h);
					else map[x][y] = 0;
				}

			}
			h /= smoothness;
			step /= 2;
		}

		// Normalize the map
		float max = Float.MIN_VALUE;
		float min = Float.MAX_VALUE;
		for (float[] row : map) {
			for (float d : row) {
				if (d > max) max = d;
				if (d < min) min = d;
			}
		}

		// Use the thresholds to fill in the return map
		for(int row = 0; row < map.length; row++){
			for(int col = 0; col < map[row].length; col++){
				map[row][col] = (map[row][col]-min)/(max-min);
				pmap.setColor(map[row][col],map[row][col],map[row][col],1);
				pmap.drawPixel(row, col);
			}
		}

		return pmap;
	}
}