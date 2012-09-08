package com.untamedears.PrisonPearl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Set;

import org.bukkit.Bukkit;

public class AltsList {
	private HashMap<String, String[]> altsHash;
	private boolean initialised = false;
	
	public AltsList() {
	}
	
	public void load(File file) {
		try {
			loadAlts(file);
			initialised = true;
		} catch (IOException e) {
			e.printStackTrace();
			Bukkit.getLogger().info("Failed to load file!");
			initialised = false;
		}
	}
	
	private void loadAlts(File file) throws IOException {
		altsHash = new HashMap<String, String[]>();
		FileInputStream fis;
		fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.length() > 1) {
				String parts[] = line.split(" ");
				String[] newString = new String[parts.length];
				for (int i = 0; i < parts.length; i++) {
					newString[i] = parts[i];
				}
				for (int j = 0; j < parts.length; j++) {
					altsHash.put(parts[j], newString);
				}
			}
		}
	}
	
	public String[] getAltsArray(String name){
		if (initialised && altsHash.containsKey(name)) {
			String[] names = altsHash.get(name);
			String[] alts = new String[names.length-1];
			for (int i = 0, j = 0; i < names.length; i++) {
				if (!names[i].equals(name)) {
					alts[j] = names[i];
					j++;
				}
			}
			return alts;
		}
		return new String[0];
	}
	
	public Set<String> getAllNames() {
		return altsHash.keySet();
	}
}
