package com.bergasms.pcg;



public class MPolygon {

	float[][] coords;
	int count;
	
	public MPolygon(){
		this(0);
	}

	public MPolygon(int points){
		coords = new float[points][2];
		count = 0;
	}

	public void add(float x, float y){
		coords[count][0] = x;
		coords[count++][1] = y;
	}


	public void draw(){
		
		for(int i=0; i<count; i++){
		//	g.vertex(coords[i][0], coords[i][1]);
		}
		
	}

	public int count(){
		return count;
	}

	public float[][] getCoords(){
		return coords;
	}
	

}