package com.bergasms.pcg;

import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class PCGChallenge extends ApplicationAdapter {
	
	Texture map;
	MidpointDisplacement mpd;
	Pixmap pmap;
	
	final int mapW = 2048;
	final int mapH = 2048;
	final int mult = 4;
	
	SpriteBatch renderSprite;
	
	@Override
	public void create () {
		
		pmap = new Pixmap(mapW, mapH, Format.RGB888);
		renderSprite = new SpriteBatch();
		
		int findbit = Math.max((mapW/mult), (mapH/mult));
		int bitcounter = 0;
		while((findbit & 1) == 0) {
			findbit = findbit >> 1;
			bitcounter++;
		}
		
		
		mpd = new MidpointDisplacement(bitcounter, 1.75f, mult);
		int[][] intmap = mpd.getMap(new Random());
		
		int linethickness = 5;
		int oceanShoreline = 140;
		
		for(int i = 0; i<Math.min(mapW,mpd.width); i++) {
			for(int j=0; j<Math.min(mapH, mpd.height); j++) {
				
				if(intmap[i][j] > oceanShoreline && intmap[i][j] < oceanShoreline + linethickness ) {
					pmap.setColor(0, 0, 0, 1); //black lines for island borders
				} else if(intmap[i][j] > oceanShoreline + linethickness) {
					pmap.setColor(0.8f, 0.7f, 0.6f, 1.0f); //landmass
				} else {
					pmap.setColor(0.6f, 0.8f, 0.8f, 1.0f); //ocean
				}
				
				
				pmap.drawPixel(i, j);
			}
		}
		
		
		map = new Texture(pmap);
		FileHandle handle = new FileHandle("map.png");
		int tries = 1;
		while(handle.exists()) {
			handle = new FileHandle("map" + tries + ".png");
			tries++;
		}
		
		PixmapIO.writePNG(handle, pmap);
	}
	
	@Override
	public void render () {
		renderSprite.begin();
		renderSprite.draw(map, 0, 0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		renderSprite.end();
	}
}
