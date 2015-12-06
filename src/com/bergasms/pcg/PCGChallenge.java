package com.bergasms.pcg;

import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class PCGChallenge extends ApplicationAdapter {
	
	Texture map;
	MidpointDisplacement mpd;
	Pixmap pmap;
	
	final int mapW = 2048;
	final int mapH = 2048;
	
	SpriteBatch renderSprite;
	
	@Override
	public void create () {
		
		pmap = new Pixmap(mapW, mapH, Format.RGB888);
		renderSprite = new SpriteBatch();
		
		int findbit = Math.max(mapW, mapH);
		int bitcounter = 0;
		while((findbit & 1) == 0) {
			findbit = findbit >> 1;
			bitcounter++;
		}
		
		
		mpd = new MidpointDisplacement(bitcounter, 1.75f, 1);
		int[][] intmap = mpd.getMap(new Random());
		
		int linethickness = 3;
		int oceanShoreline = 160;
		
		for(int i = 0; i<Math.min(mapW,mpd.width); i++) {
			for(int j=0; j<Math.min(mapH, mpd.height); j++) {
				float cv = intmap[i][j] > oceanShoreline && intmap[i][j] < oceanShoreline + linethickness ? 0.0f : 1.0f;
				
				pmap.setColor(intmap[i][j] > oceanShoreline + linethickness ? cv-0.2f : cv,cv,cv,1);
				pmap.drawPixel(i, j);
			}
		}
		
		
		map = new Texture(pmap);
		
	}
	
	@Override
	public void render () {
		renderSprite.begin();
		renderSprite.draw(map, 0, 0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		renderSprite.end();
	}
}
