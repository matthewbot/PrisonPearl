package com.untamedears.PrisonPearl;

import java.io.File;
import java.io.IOException;

public interface SaveLoad {
	public void save(File file) throws IOException;
	public void load(File file) throws IOException;
	
	public boolean isDirty();
}
