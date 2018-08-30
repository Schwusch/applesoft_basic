package com.bocker.applesoft.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.bocker.applesoft.AppleSoft;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration.disableAudio = true;
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		new LwjglApplication(new AppleSoft(), config);
	}
}

