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
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.BSpline;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class PCGChallenge extends ApplicationAdapter {
	
	Texture map;
	MidpointDisplacement mpd;
	MidpointDisplacement vegetation;
	MidpointDisplacement beaches;
	MidpointDisplacement noisy;
	
	Pixmap pmap;
	byte[][] featureMap;
	Feature[] features;
	
	final int mapW = 2048;
	final int mapH = 2048;
	final int mult = 4;
	final int fontsize = 24;
	
	SpriteBatch renderSprite;
	
	@Override
	public void create () {
		
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Inconsolata-Bold.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();

		parameter.size = fontsize;
		BitmapFont font = generator.generateFont(parameter);
		font.setColor(0, 0, 0, 1);
		
		generator.dispose();
		
		
		pmap = new Pixmap(mapW, mapH, Format.RGB888);
		renderSprite = new SpriteBatch();
		
		features = new Feature[40];
		
		int findbit = Math.max((mapW/mult), (mapH/mult));
		int bitcounter = 0;
		while((findbit & 1) == 0) {
			findbit = findbit >> 1;
			bitcounter++;
		}
		
		
		mpd = new MidpointDisplacement(bitcounter, 1.85f, mult);
		vegetation = new MidpointDisplacement(bitcounter, 1.45f, mult);
		beaches = new MidpointDisplacement(bitcounter, 2.15f, mult);
		noisy = new MidpointDisplacement(bitcounter, 0.85f, mult);
		Random r = new Random();
		
		int[][] intmap = mpd.getMap(r);
		
		int[] smoothIn = new int[intmap.length * intmap[0].length];
		int[] smoothOut = new int[intmap.length * intmap[0].length];
		
		for(int sstep = 0; sstep < 15; sstep++) {
			int ic = 0;
			for(int i=0; i<intmap.length; i++) {
				for(int j=0; j<intmap[0].length; j++) {
					smoothIn[ic++] = intmap[i][j];
				}
			}
			BlurUtils.blurPass(smoothIn, smoothOut, intmap.length, intmap[0].length, 10);
			ic = 0;
			for(int i=0; i<intmap.length; i++) {
				for(int j=0; j<intmap[0].length; j++) {
					intmap[i][j] = smoothOut[ic++];
				}
			}
		}
		
		int[][] vmap = vegetation.getMap(r);
		int[][] bmap = beaches.getMap(r);
		int[][] noise = noisy.getMap(r);
		featureMap = new byte[Math.min(mapW,mpd.width)][Math.min(mapH, mpd.height)];
		
		int linethickness = 3;
		int oceanShoreline = 100;
		
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
					if(intmap[i][j] < oceanShoreline - 4*linethickness) {
						pmap.setColor(0.4f, 0.6f, 0.6f, 1.0f);
						featureMap[i][j] = TERRAIN.asbyte(TERRAIN.TT_OCEAN_DEEP);
					}
					
				}
				//float cl = intmap[i][j]/255.0f;
				//pmap.setColor(cl, cl, cl, 1.0f);
				pmap.drawPixel(i, j);
			}
		}
		 
		
		findPotentialPlacesOfInterest(r,intmap);

		drawRivers(r);
		
		drawPath(r);
		
		placeFeaturesOfInterest(r,font);
		
		
		map = new Texture(pmap);
		
		FileHandle handle = new FileHandle("map.png");
		int tries = 1;
		while(handle.exists()) {
			handle = new FileHandle("map" + tries + ".png");
			tries++;
		}
		
		PixmapIO.writePNG(handle, pmap);
	}
	
	private void drawPath(Random r) {
		
	}

	private void drawRivers(Random r) {
		int rivers = 4;
		
		ArrayList<Feature> sourcePoints = new ArrayList<PCGChallenge.Feature>();
		
		for(Feature f : features) {
			if(f.type == FEATURE_TYPE.FT_MOUNTAIN) {
				sourcePoints.add(f);
			}
			
			if(f.type == FEATURE_TYPE.FT_FOREST) {
				if(r.nextBoolean()) {
					sourcePoints.add(f);
				}
			}
			
			if(f.type == FEATURE_TYPE.FT_LAND) {
				if(r.nextBoolean()) {
					sourcePoints.add(f);
				}
			}
		}
		
		rivers = Math.min(sourcePoints.size(),rivers);
		
		while(rivers > 0) {
			
			Feature f = sourcePoints.remove(r.nextInt(sourcePoints.size()));
			
			rivers--;
			
			int k = 100; //increase k for more fidelity to the spline
			int pset = 60;
		    Vector2[] points = new Vector2[k];
		    Vector2[] dataSet = new Vector2[pset];
		    
		    dataSet[0] = new Vector2(f.x,f.y);
		    
		    dataSet[pset-1] = new Vector2(f.x+3000,f.y);
		    dataSet[pset-1].rotate(r.nextInt(360));
		    
		    Vector2 rot = new Vector2(dataSet[pset-1]);
		    rot.rotate(90);
		    rot.nor();
		    
		    
		    
		    for(int i=1; i<pset-1; i++) {
		    	float xp = MathUtils.lerp(dataSet[0].x, dataSet[pset-1].x, i/(float)pset);
		    	float yp = MathUtils.lerp(dataSet[0].y, dataSet[pset-1].y, i/(float)pset);
		    	
		    	xp += (r.nextInt(100) - 50) * rot.x;
		    	yp += (r.nextInt(100) - 50) * rot.y;
		    	
		    	dataSet[i] = new Vector2(xp, yp);
		    }
		    
		    
		/*init()*/
		    CatmullRomSpline<Vector2> myCatmull = new CatmullRomSpline<Vector2>(dataSet, false);
		    for(int i = 0; i < k; ++i)
		    {
		        points[i] = new Vector2();
		        myCatmull.valueAt(points[i], ((float)i)/((float)k-1));
		    }
		    
		    pmap.setColor(0, 0, 1, 1);
		    int radius = 2;
		    int inc = 0;
		    Vector2 prev = null;
		    for(Vector2 vn : points) {
		    	inc++;
		    	
		    	if(inc % 10 == 0) {
		    		inc = 0;
		    		radius++;
		    		radius = radius > 6 ? 6 : radius;
		    	}
		    	
		    	
		    	
		    	if(prev == null) {
		    		prev = vn;
		    	} else {
		    		
		    		int nx = (int) vn.x;
		    		int ny = (int) vn.y;
		    		
		    		if(prev.x <0 || prev.x >= pmap.getWidth() || prev.y < 0 || prev.y >= pmap.getHeight()) {
		    			break;
		    		}
		    		
		    		if(featureMap[(int)prev.x][ (int)prev.y] == TERRAIN.asbyte(TERRAIN.TT_OCEAN) ||
		    				featureMap[(int)prev.x][ (int)prev.y] == TERRAIN.asbyte(TERRAIN.TT_OCEAN_DEEP)) {
		    			break;
		    		}
		    		
		    		BALine((int)prev.x, (int)prev.y,nx,ny,pmap,radius);
		    		
		    		prev = vn;
		    	}
		    }
		}
	}

	private void placeFeaturesOfInterest(Random r, BitmapFont font) {
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
					drawHouse(pmap,cx,cy,10);
					drawHouse(pmap,cx+10,cy-1,8);
				
			}
			
			else if(f.type == FEATURE_TYPE.FT_FOREST) {
				
				pmap.setColor(0.3f, 0.3f, 0.1f, 1.0f);
				pmap.fillRectangle(cx-3, cy, 6, 30);
				
				pmap.setColor(0.2f, 0.7f, 0, 1);
				pmap.fillCircle(cx, cy, rd);
				rd -= 5;
				cy -= 3;
				
				pmap.setColor(0, 0.9f, 0, 1);
				pmap.fillCircle(cx, cy, rd);
				
			}
			
			else {
				pmap.fillCircle(cx,cy,rd);
			}
		}
		for(Feature f : features) {
			
			int cx = (int)f.centre.x; 
			int cy = (int)f.centre.y;
			int rd = (int)f.radius;
			
			
			BitmapFontData data = font.getData();
			Pixmap fontPixmap = font.getRegion().getTexture().getTextureData().consumePixmap();
			fontPixmap.setColor(0, 0, 0, 1);
			pmap.setColor(0, 0, 0, 1);
			
		    int TILE_WIDTH = cx - (f.name.length() * (fontsize/2))/2;
		    int TILE_HEIGHT = cy + rd + fontsize;
		    
		    pmap.fillRectangle(TILE_WIDTH - 8, TILE_HEIGHT - 12, (f.name.length() * (fontsize/2))+2, fontsize+2);
		    
		    for(int id=0; id<f.name.length(); id++) {
		    	char c = f.name.charAt(id);
		    	Glyph glyph = data.getGlyph(c);
		    	pmap.drawPixmap(fontPixmap, TILE_WIDTH - (glyph.width/ 2), TILE_HEIGHT - (glyph.height / 2),
		                glyph.srcX, glyph.srcY, glyph.width, glyph.height);
		    	TILE_WIDTH += fontsize/2;
		    }
		    
		}
	}

	private void drawHouse(Pixmap pmap2, int cx, int cy, int i) {
		pmap2.setColor(0, 0, 0, 1);
		pmap2.drawRectangle(cx-5, cy, i+1, i);
		pmap2.drawLine(cx-i/2, cy, cx, cy-i/2);
		pmap2.drawLine(cx+i/2, cy, cx, cy-i/2);
		
		cy += 1;
		pmap2.drawRectangle(cx-5, cy, i+1, i);
		pmap2.drawLine(cx-i/2, cy, cx, cy-i/2);
		pmap2.drawLine(cx+i/2, cy, cx, cy-i/2);

		cy += 1;
		pmap2.drawRectangle(cx-5, cy, i+1, i);
		pmap2.drawLine(cx-i/2, cy, cx, cy-i/2);
		pmap2.drawLine(cx+i/2, cy, cx, cy-i/2);
		
	}

	private void findPotentialPlacesOfInterest(Random r, int[][] intmap) {
		boolean[][] visitMap = new boolean[intmap.length][intmap[0].length];
		
		int placed_oceans_left = features.length/3;
		int required_beaches = 2;
		int required_treasure = 1;
		int placed_features = 0;
		int feature_inset = 60;
		
		int tID = 0;
		
		while(placed_features < features.length) {
			Feature f = null;
			do {
				int radius = r.nextInt(10) + 10;
				Vector2 pos = new Vector2(r.nextInt(intmap.length - 2*feature_inset) + feature_inset, r.nextInt(intmap[0].length - 2*feature_inset) + feature_inset);
				f  = new Feature(pos,radius,tID++);
				
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
				
				float oceanPercent = (float)tctr[TERRAIN.TT_OCEAN_DEEP.ordinal()]/(float)total;
				float landPercent = (float)tctr[TERRAIN.TT_LAND.ordinal()]/(float)total;
				float forestPercent = (float)tctr[TERRAIN.TT_FOREST.ordinal()]/(float)total;
				float mountainPercent = (float)tctr[TERRAIN.TT_MOUNTAIN.ordinal()]/(float)total;
				float beachPercent = (float)tctr[TERRAIN.TT_BEACH.ordinal()]/(float)total;
				
				if(beachPercent > 0.1) {
					f.type = FEATURE_TYPE.FT_BEACH;
					required_beaches--;
				}
				
				else if(required_beaches > 0) {
					f.isValid = false;
				}

				else if(landPercent > 0.9) {
					if(required_treasure > 0){
						required_treasure--;
						f.type = FEATURE_TYPE.FT_DESTINATION;
					} else {
						f.type = FEATURE_TYPE.FT_LAND;
					}
				}
				
				else if(required_treasure > 0) {
					f.isValid = false;
				}
				
				else if(forestPercent > 0.6) {
					f.type = FEATURE_TYPE.FT_FOREST;
				}

				else if(mountainPercent > 0.7) {
					f.type = FEATURE_TYPE.FT_MOUNTAIN;
				}


				else if(oceanPercent > 0.95) {
					if(placed_oceans_left > 0) {
						f.type = FEATURE_TYPE.FT_OCEAN;
						placed_oceans_left--;
					} else {
						f.isValid = false;
					}
				}
				else {
					f.isValid = false;
				}
				
				if(f.isValid) {
					
					f.assignType(f.type, r);
					
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
		
		public int id;
		public FEATURE_TYPE type;
		public Vector2 centre;
		public float radius;
		public String name;
		Circle c;
		public boolean isValid;
		
		public int x,y,dx,dy;
		
		public Feature(Vector2 pos, int radius2, int id_) {
			c = new Circle(pos, radius2);
			id = id_;
			centre = pos;
			radius = radius2;
			isValid = true;
			x = (int)pos.x;
			y = (int)pos.y;
			dx = (int)pos.x + radius2*2;
			dy = (int)pos.y + radius2*2;
			
		}
		
		public void assignType(FEATURE_TYPE type, Random r) {
			this.type = type;

			generateName(r);
		}
		
		final String[] places = {"Beach","Forest","Mountains","Ocean","Village", "Treasure"};
		final String[] descriptor = {"Woe","Doom","Hell","Despair","Suffering","Malignancy","Terror","Fear","Forboding","Fright"};
		final String[] enemies_ocean = {"Dragons","Giant Squid","Whirlpools","Storms","Who Knows What"};
		
		private void generateName(Random r) {
			String namebuilder = places[this.type.ordinal()];
			if(this.type == FEATURE_TYPE.FT_DESTINATION) {
				this.name = namebuilder;
				return;
			}
			if(this.type == FEATURE_TYPE.FT_OCEAN && r.nextFloat() < 0.6f) {
				this.name = "Here be " + enemies_ocean[r.nextInt(enemies_ocean.length)];
				return;
			}
			namebuilder += " of ";
			namebuilder += descriptor[r.nextInt(descriptor.length)];
			this.name = namebuilder;
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
		FT_LAND,
		FT_DESTINATION;

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
			
			else if(this == FT_DESTINATION) {
				return new Color(0.0f,0.0f,0.0f,1.0f);	
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
		TT_MOUNTAIN, 
		TT_OCEAN_DEEP;
		
		public static byte asbyte(TERRAIN t) {
			return (byte)t.ordinal();
		}

		public static TERRAIN getType(byte b) {
			return TERRAIN.values()[b];
		}
	}
	

	static int abs(int a)
    {
	if (a < 0)
	    return -a;
	else return a;
    }
	
	public static void BALine(int x1, int y1, 
    		int x2, int y2, Pixmap view, int radius) {

    	//radius = 1;
    	// If slope is outside the range [-1,1], swap x and y
    	boolean xy_swap = false;
    	if (abs(y2 - y1) > abs(x2 - x1)) {
    		xy_swap = true;
    		int temp = x1;
    		x1 = y1;
    		y1 = temp;
    		temp = x2;
    		x2 = y2;
    		y2 = temp;
    	}

    	// If line goes from right to left, swap the endpoints
    	if (x2 - x1 < 0) {
    		int temp = x1;
    		x1 = x2;
    		x2 = temp;
    		temp = y1;
    		y1 = y2;
    		y2 = temp;
    	}

    	int x,                       // Current x position
    	y = y1,                  // Current y position
    	e = 0,                   // Current error
    	m_num = y2 - y1,         // Numerator of slope
    	m_denom = x2 - x1,       // Denominator of slope
    	threshold  = m_denom/2;  // Threshold between E and NE increment 

    	Color c = new Color();
    	
    	for (x = x1; x < x2; x++) {
    		if (xy_swap){
    			if(radius <= 1){
    				c.set(view.getPixel(y, x));
    				c.b = 1;
    				c.r *= 0.1f;
    				c.g *= 0.1f;
    				view.setColor(c);
    				view.drawPixel( y,x);
    			} else {
    				view.fillCircle(y, x, radius);
    			}
    			
    		}
    		else{
    			if(radius <= 1){
    				c.set(view.getPixel(x,y));
    				c.b = 1;
    				c.r *= 0.1f;
    				c.g *= 0.1f;
    				view.setColor(c);
    				view.drawPixel(x, y);
    			} else {
    				view.fillCircle(x,y, radius);
    			}
    		}

    		e += m_num;


    		// Deal separately with lines sloping upward and those
    		// sloping downward
    		if (m_num < 0) {
    			if (e < -threshold) {
    				e += m_denom;
    				y--;
    			}
    		}
    		else if (e > threshold) {
    			e -= m_denom;
    			y++;
    		}
    	}

    	if (xy_swap) {
    		if(radius <= 1){
    			c.set(view.getPixel(y, x));
    			c.b = 1;
    			c.r *= 0.1f;
				c.g *= 0.1f;
				view.setColor(c);
				view.drawPixel(y,x);
			} else {
				view.fillCircle(y,x, radius);
			}
    	}
    	else { 
    		if(radius <= 1){
    			c.set(view.getPixel(x, y));
    			c.b = 1;
				c.g *= 0.1f;
				c.r *= 0.1f;
				view.setColor(c);
				view.drawPixel(x, y);
			} else {
				view.fillCircle(x,y, radius);
			}
    	}

    }
}
