package com.bergasms.pcg;

import java.util.ArrayList;
import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class PCGChallenge extends ApplicationAdapter {
	
	Texture map;
	MidpointDisplacement mpd;
	MidpointDisplacement vegetation;
	MidpointDisplacement beaches;
	MidpointDisplacement noisy;
	FeatureGenerator fg;
	Pixmap pmap;
	byte[][] featureMap;
	Feature[] features;
	
	final int mapW = 2048;
	final int mapH = 2048;
	final int mult = 4;
	
	SpriteBatch renderSprite;
	
	@Override
	public void create () {
		
		pmap = new Pixmap(mapW, mapH, Format.RGB888);
		renderSprite = new SpriteBatch();
		
		features = new Feature[80];
		
		int findbit = Math.max((mapW/mult), (mapH/mult));
		int bitcounter = 0;
		while((findbit & 1) == 0) {
			findbit = findbit >> 1;
			bitcounter++;
		}
		
		
		mpd = new MidpointDisplacement(bitcounter, 1.85f, mult);
		vegetation = new MidpointDisplacement(bitcounter, 1.45f, mult);
		beaches = new MidpointDisplacement(bitcounter, 2.25f, mult);
		noisy = new MidpointDisplacement(bitcounter, 0.85f, mult);
		Random r = new Random();
		fg = new FeatureGenerator(r);
		int[][] intmap = mpd.getMap(r);
		int[][] vmap = vegetation.getMap(r);
		int[][] bmap = beaches.getMap(r);
		int[][] noise = noisy.getMap(r);
		featureMap = new byte[Math.min(mapW,mpd.width)][Math.min(mapH, mpd.height)];
		
		int linethickness = 5;
		int oceanShoreline = 140;
		
		boolean tp = false;
		
		for(int i = 0; i<Math.min(mapW,mpd.width); i++) {
			for(int j=0; j<Math.min(mapH, mpd.height); j++) {
				
				if(intmap[i][j] > oceanShoreline && intmap[i][j] < oceanShoreline + linethickness ) {
					pmap.setColor(0, 0, 0, 1); //black lines for island borders
					featureMap[i][j] = TERRAIN.asbyte(TERRAIN.TT_CLIFF);
					
					//unless vmap.
					if(bmap[i][j] < 100) {
						pmap.setColor(1, 1, 0, 1);
						featureMap[i][j] = TERRAIN.asbyte(TERRAIN.TT_BEACH);
					}
					
					
				} else if(intmap[i][j] > 180) {
					float nm = noise[i][j]/255.0f;
					nm *= 0.1f;
					pmap.setColor(0.7f+nm, 0.6f+nm, 0.5f+nm, 1.0f); 
					//mountain
					featureMap[i][j] = TERRAIN.asbyte(TERRAIN.TT_MOUNTAIN);
				} else if(intmap[i][j] > oceanShoreline + linethickness) {
					if(vmap[i][j] > 160){
						//forest
						float nm = noise[i][j]/255.0f;
						nm *= 0.2f;
						pmap.setColor(0.3f-nm, 0.7f-nm, 0.3f-nm, 1.0f);
						featureMap[i][j] = TERRAIN.asbyte(TERRAIN.TT_FOREST);
					} else {
						pmap.setColor(0.8f, 0.7f, 0.6f, 1.0f); //landmass
						featureMap[i][j] = TERRAIN.asbyte(TERRAIN.TT_LAND);
					}
				} else {
					pmap.setColor(0.6f, 0.8f, 0.8f, 1.0f); //ocean
					featureMap[i][j] = TERRAIN.asbyte(TERRAIN.TT_OCEAN);
				}
				
				
				pmap.drawPixel(i, j);
			}
		}
		 
		
		findPotentialPlacesOfInterest(r,intmap);
		
		for(Feature f : features) {
			pmap.setColor(f.type.colourForFeature());
			int cx = (int)f.centre.x; 
			int cy = (int)f.centre.y;
			int rd = (int)f.radius;
			
			
			if(f.type == FEATURE_TYPE.FT_MOUNTAIN) {
				
				cx -= rd;
				cy -= rd/2;
				for(int i=0; i<2; i++) {
					float c = 0.1f;
					int rdo = rd;
					
					rdo += i%2==0?-15:0;

					while(c < 1.0f) {
						if(c < 0.9f){
							pmap.setColor(c+0.05f, c, c-0.05f, 1);
						} else {
							pmap.setColor(1,1,1, 1);
						}
						pmap.fillTriangle(cx-rdo/2, cy+rdo, cx, cy, cx+rdo/2, cy+rdo);
						rdo -= 2;
						c += 0.1f;
					}
					cx += rd/2;
				}

			}
			
			else if(f.type == FEATURE_TYPE.FT_OCEAN) {
				
				cy -= rd/2;
				for(int i=0; i<4; i++) {
					float c = 0.1f;
					int rdo = rd;
					
					rdo += i%2==0?5:0;

					while(c < 0.7f) {
						pmap.setColor(c, c, 1.0f, 1);
						pmap.fillTriangle(cx-rdo/2, cy+rdo, cx, cy, cx+rdo/2, cy+rdo);
						rdo -= 2;
						c += 0.1f;
					}
					cx += rd;
				}
			}
			
			else if(f.type == FEATURE_TYPE.FT_LAND) {
				
				float c = 0.0f;
				
				
				
					pmap.setColor(c, c, c, 1);
					pmap.fillTriangle(cx-rd/2, cy+rd, cx, cy, cx+rd/2, cy+rd);
					rd -= 2;
				
			}
			
			else if(f.type == FEATURE_TYPE.FT_FOREST) {
				
				pmap.setColor(0.3f, 0.3f, 0.1f, 1.0f);
				pmap.fillRectangle(cx-3, cy, 6, 30);
				
				pmap.setColor(0.2f, 0.8f, 0, 1);
				pmap.fillCircle(cx, cy, rd);
				rd -= 5;
				cy -= 3;
				
				pmap.setColor(0, 1, 0, 1);
				pmap.fillCircle(cx, cy, rd);
				
			}
			
			else {
				pmap.fillCircle(cx,cy,rd);
			}
		}
		
		System.out.println("Done");
		
		map = new Texture(pmap);
		FileHandle handle = new FileHandle("map.png");
		int tries = 1;
		while(handle.exists()) {
			handle = new FileHandle("map" + tries + ".png");
			tries++;
		}
		
		PixmapIO.writePNG(handle, pmap);
	}
	
	private void findPotentialPlacesOfInterest(Random r, int[][] intmap) {
		boolean[][] visitMap = new boolean[intmap.length][intmap[0].length];
		
		int placed_features = 0;
		int feature_inset = 60;
		while(placed_features < features.length) {
			Feature f = null;
			do {
				int radius = r.nextInt(10) + 10;
				Vector2 pos = new Vector2(r.nextInt(intmap.length - 2*feature_inset) + feature_inset, r.nextInt(intmap[0].length - 2*feature_inset) + feature_inset);
				f  = new Feature(pos,radius);
				
				int[] tctr = new int[TERRAIN.values().length];
				int total = 0;
				
				for(int i=f.x; i<f.dx; i++) {
					for(int j=f.y; j<f.dy; j++) {
						if(visitMap[i][j]) {
							f.isValid = false;
							break;
						} else {
							TERRAIN t = TERRAIN.getType(featureMap[i][j]);
							tctr[t.ordinal()]++;
							total++;
						}
					}	
				}
				
				float oceanPercent = (float)tctr[TERRAIN.TT_OCEAN.ordinal()]/(float)total;
				float landPercent = (float)tctr[TERRAIN.TT_LAND.ordinal()]/(float)total;
				float forestPercent = (float)tctr[TERRAIN.TT_FOREST.ordinal()]/(float)total;
				float mountainPercent = (float)tctr[TERRAIN.TT_MOUNTAIN.ordinal()]/(float)total;
				float beachPercent = (float)tctr[TERRAIN.TT_BEACH.ordinal()]/(float)total;
				
				if(beachPercent > 0.1) {
					f.type = FEATURE_TYPE.FT_BEACH;
				}
				
				else if(forestPercent > 0.6) {
					f.type = FEATURE_TYPE.FT_FOREST;
				}

				else if(mountainPercent > 0.7) {
					f.type = FEATURE_TYPE.FT_MOUNTAIN;
				}

				else if(landPercent > 0.9) {
					f.type = FEATURE_TYPE.FT_LAND;
				}

				else if(oceanPercent > 0.95) {
					f.type = FEATURE_TYPE.FT_OCEAN;
				}
				else {
					f.isValid = false;
				}
				
				if(f.isValid) {
					
					for(int i=f.x; i<f.dx; i++) {
						for(int j=f.y; j<f.dy; j++) {
							visitMap[i][j] = true;
						}	
					}
				
					if(f.type == FEATURE_TYPE.FT_MOUNTAIN) {
						f.radius *= 3;
					}
				}
				
			} while(f.isNotValid());
			
			features[placed_features] = f;
			
			placed_features++;
		}
		
	}

	@Override
	public void render () {
		renderSprite.begin();
		renderSprite.draw(map, 0, 0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		renderSprite.end();
	}
	
	private class Feature {
		
		public FEATURE_TYPE type;
		public Vector2 centre;
		public float radius;
		public String name;
		Circle c;
		public boolean isValid;
		
		public int x,y,dx,dy;
		
		public Feature(Vector2 pos, int radius2) {
			c = new Circle(pos, radius2);
			centre = pos;
			radius = radius2;
			isValid = true;
			x = (int)pos.x;
			y = (int)pos.y;
			dx = (int)pos.x + radius2*2;
			dy = (int)pos.y + radius2*2;
		}
		public boolean isNotValid() {
			// TODO Auto-generated method stub
			return isValid == false;
		}
	}
	
	private enum FEATURE_TYPE {
		FT_BEACH,
		FT_FOREST,
		FT_MOUNTAIN,
		FT_OCEAN,
		FT_LAND;

		public Color colourForFeature() {
			if(this == FT_BEACH) {
				return new Color(0.8f,0.8f,0.3f,1.0f);
			}
			
			else if(this == FT_FOREST) {
				return new Color(0.2f,0.9f,0.1f,1.0f);
			}

			else if(this == FT_MOUNTAIN) {
				return new Color(0.7f,0.9f,0.3f,1.0f);
			}

			else if(this == FT_OCEAN) {
				return new Color(0.2f,0.2f,0.8f,1.0f);
			}

			else if(this == FT_LAND) {
				return new Color(0.3f,0.2f,0.2f,1.0f);
			}
			return new Color(0.8f,0.8f,0.8f,1.0f);
		}
	}
	
	private enum TERRAIN {
		TT_OCEAN,
		TT_BEACH,
		TT_CLIFF,
		TT_LAND,
		TT_FOREST,
		TT_MOUNTAIN;
		
		public static byte asbyte(TERRAIN t) {
			return (byte)t.ordinal();
		}

		public static TERRAIN getType(byte b) {
			return TERRAIN.values()[b];
		}
	}
}
