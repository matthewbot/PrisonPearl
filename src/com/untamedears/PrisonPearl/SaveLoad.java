package com.untamedears.PrisonPearl;

import java.io.File;
import java.io.IOException;

public interface SaveLoad {
	public void save(File file) throws IOException;
	public void load(File file) throws IOException;
	
	// --Commented out by Inspection (9/8/12 5:38 PM):public boolean isDirty();
}
