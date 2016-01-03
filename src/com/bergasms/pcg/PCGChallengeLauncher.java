package com.bergasms.pcg;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class PCGChallengeLauncher {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1024;
		config.height = 768;
		
		long seed = -1;
		if(args.length > 0) {
			seed = Long.parseLong(args[0]);
		}
		
		new LwjglApplication( new PCGChallenge(seed), config);
	}

}
