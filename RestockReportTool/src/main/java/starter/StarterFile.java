package starter;

import manager.RestockController;

/**
 * StarterFile for the program, used to simply start the program.
 * [100% better way to do this, but just keeps the code separated]
 * 
 * @author Nolan Wright
 */
public class StarterFile {
	
	public static void main(String[] args) {
		RestockController rc = new RestockController();
		
		rc.initialize();
	}
}
