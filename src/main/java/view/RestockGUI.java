package view;

import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import manager.RestockController;

/**
 * Main GUI Class for the Restock Report Tool, used to manage the visuals
 * 
 * @author Nolan Wright
 */
public class RestockGUI {

	/** Main Controller of the Program, used to handle the data operations */
	private RestockController rc;
	
	/** Main Frame for the project, stored for regeneration purposes */
	private JFrame mainFrame;	

	/** Left Panel for the Project, stored for regeneration purposes */
	private JPanel leftPanel;

	/** Right Scrollable Panel for the Project, stored for regeneration purposes */
	private JScrollPane rightPanel;

	/** Inner Right Panel for the Project, used within the Scroll Bar stored for regeneration purposes */
	private JPanel innerRightPanel;
	
	/** All the Location Buttons for the Right side, used to show all the added Data. */
	private List<JButton> locationButtons;
	
	/** All the Location Names saved from the Inventory Export */
	private List<String> locationNames;
	
	/** The Picked Restock Location for the reports */
	private String restockLocation;
	
	/** Label Used to place the Restock location Name */
	private JLabel selectedLocation;
	
	/** Progress Bar Panel, saved so the Data Could be accessed if needed */
	private JFrame progressPanel;
	
	/** Progress Bar Label, contains the Amount of Records for the project */
	private JLabel progressBar;
	
	/**
	 * Main Constructor for the GUI, used to set the RC
	 * @param rc RestockController for the program
	 */
	public RestockGUI(RestockController rc) {
		//Set stuff to null just to make sure it's starting at null
		//Not needed just helps with thought Process.
		this.rc = rc;
		mainFrame = null;
		locationButtons = new ArrayList<JButton>();
		restockLocation = null;
		selectedLocation = new JLabel("<html>No Selected Location.</html>");
		progressPanel = null;
	}
	
	/**
	 * Used to Initialize the Window, via placing all key elements
	 */
	public void initializeWindow() {		
		//Sets Up the Main Frame
		mainFrame = new JFrame("Restock Report Tool");
		
		mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		mainFrame.setSize(400, 400);
		
		//Sets up the Left Panel
		leftPanel = new JPanel();
		
		leftPanel.setLayout(new GridLayout(5,1));
		JLabel explanationLabel = new JLabel("<html> Please pick the location you wish to restock from. </html>");
		
		//Setup the Drop Down
		JComboBox<String> dropdownBox = setUpDropdownBox();
		
		JButton runReportsButton = new JButton("<html>Run Reports.</html>");
		
		//Used to call RC to run the Reports.
		runReportsButton.addActionListener(e -> {
			rc.runTheReports(restockLocation);
		});
		
		//Used to Collect the Orders Export for all Locations
		JButton allLocationsButton = new JButton("<html>Add Orders Export for All Locations</html>");
		allLocationsButton.addActionListener(e -> {
			boolean found = pickFileForOrdersData();
			if(found) {
				//allLocations.setText("<html> All Locations data added.</html>");
				
				//Change all Locations Buttons to show their data has been added
				for(int i = 0; i < locationButtons.size(); i++) {
					JButton j = locationButtons.get(i);
					String text = j.getText();
					
					if(text.contains("data added.")) {
						continue;
					}
					String[] dirty = text.split("<");
					
					j.setText("<" + dirty[1] + " data added.</html>");
				}
				regenerateWindow();
			}
		});
		
		//Adds all the Buttons and Items to the Left Panel
		leftPanel.add(explanationLabel);
		leftPanel.add(dropdownBox);
		leftPanel.add(selectedLocation);
		leftPanel.add(allLocationsButton);
		leftPanel.add(runReportsButton);
		
		//Set up the ScrollPane on the Right Side
		innerRightPanel = new JPanel();
		rightPanel = new JScrollPane(innerRightPanel);
		
		//Maybe unneeded, but this is to keep the vertical bar active at all times for clarity
		rightPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		//Finish Setting up the Main Frame, Add the Buttons to the Right, and Regen the Window
		mainFrame.setLayout(new GridLayout(1,2));
		
		mainFrame.add(leftPanel);
		mainFrame.add(rightPanel);
		
		addButtonsToRightPanel();
		
		regenerateWindow();
	}
	
	/**
	 * Set sup a Dropdown Box for the RestockLocations
	 * @return Returns a Dropdown Box full of Restock Locations
	 */
	private JComboBox<String> setUpDropdownBox() {
		String[] choices = new String[locationNames.size() + 1];
		choices[0] = "Please select a location.";
		for(int i = 0; i < locationNames.size(); i++) {
			choices[i+1] = locationNames.get(i);
		}
		JComboBox<String> dropdownBox = new JComboBox<>(choices);
		
		dropdownBox.addActionListener(e -> {
			restockLocation = (String) dropdownBox.getSelectedItem();
			selectedLocation.setText("<html>" + restockLocation + " Selected.</html>");
		});
		
		return dropdownBox;
	}

	/**
	 * Add all the Location Buttons to the Right Scrolling Panel
	 */
	private void addButtonsToRightPanel() {
		innerRightPanel.setLayout(new BoxLayout(innerRightPanel, BoxLayout.Y_AXIS));
		
		for(int i = 0; i < locationButtons.size(); i++) {
			innerRightPanel.add(locationButtons.get(i));
		}
		
		innerRightPanel.revalidate();
		innerRightPanel.repaint();
		
		rightPanel.revalidate();
		rightPanel.repaint();
		
	}

	/**
	 * Calls PickFile to get the Inventory File, and as long as it's not null, it send it to the RC to Parse the Data
	 */
	public void loadInventory() {
		String filePath = pickFile();
		
		if(filePath == null) {
			System.exit(-1);
		}
	
		rc.parseInventoryData(filePath);
		
	}
	
	/**
	 * PickFile is taken from the MCTT Project but will be explained here
	 * Used to call the FileChooser and Pick the File.
	 * @return Returns the Absolute File Path for a File.
	 */
	private String pickFile() {
		//Sets the directory of the fileChooser to the current directory
		//Code Taken from MCTT
		JFileChooser fileChooser = new JFileChooser(".");
				
		int response = fileChooser.showOpenDialog(null);
				
		//If a file is given back, it takes that exact path and sends it back, if not system exits.
		if(response == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			return file.getAbsolutePath();
		} else {
			return null;
		}
	}

	/**
	 * Used to Regnerate the window if needed by the program.
	 */
	private void regenerateWindow() {
		innerRightPanel.revalidate();
		innerRightPanel.repaint();
		
		rightPanel.revalidate();
		rightPanel.repaint();
		
		mainFrame.revalidate();
		mainFrame.repaint();
		mainFrame.setVisible(true);
	}

	/**
	 * Used to set the Location Buttons up with their Location Names
	 * @param locationNames The Given Location Names from the Inventory Export
	 */
	public void setLocationNames(List<String> locationNames) {
		this.locationNames = locationNames;
		locationNames.sort(null);
		
		for(int i = 0; i < locationNames.size(); i++) {
			String location = locationNames.get(i);
			JButton j = new JButton("<html>" + location + "</html>");
			locationButtons.add(j);
		}
		terminateProgressScreen();
		initializeWindow();
	}

	/**
	 * Used to Call the PickFile Functionality then send that dataFile to RC to load the orders.
	 * @return Returns True if the file could be picked, otherwise it returns false.
	 */
	private boolean pickFileForOrdersData() {
		String dataFile = pickFile();

		rc.loadDataForOrders(dataFile);
		
		if(dataFile != null && !dataFile.equals("")) {
			return true;
		}
		
		return false;
	}

	/**
	 * Used to show any errors needed by the program
	 * @param errorMsg Error Message given by the RC or GUI
	 */
	public void showError(String errorMsg) {
		JOptionPane.showConfirmDialog(null, errorMsg, "Error" , JOptionPane.DEFAULT_OPTION);
	}

	/**
	 * Used to set up the Progress bar for the Program
	 * @param size Size of the Inventory Report being Processed.
	 */
	public void createProgressScreen(int size) {
		progressPanel  = new JFrame("Progress for RRT");
		progressPanel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		progressPanel.setSize(400, 200);
		
		progressPanel.setLayout(new GridLayout(1,2));
		
		progressBar = new JLabel("<html> 0 records loaded. </html>");
		progressBar.setBorder(new EmptyBorder(10,10,10,10));
		
		JLabel rightLabel = new JLabel("<html>" + size + " total records to load </html>");
		
		rightLabel.setBorder(new EmptyBorder(10,10,10,10));
		
		progressPanel.add(progressBar);
		progressPanel.add(rightLabel);
		
		progressPanel.revalidate();
		progressPanel.repaint();
		progressPanel.setVisible(true);
	}
	
	/**
	 * Used to add data to the progress bar, via an Int handed from the RC
	 * @param progressToAdd Progress to add [Currently in increments of 1000]
	 */
	public void addProgressToTheBar(int progressToAdd) {
		int progressAmount = Integer.parseInt(progressBar.getText().replaceAll("\\D",""));
		progressBar.setText("<html>" + (progressAmount + progressToAdd) + " records loaded. </html>");
	}
	
	/**
	 * Gets rid of the Progress Bar following the Data Load
	 */
	private void terminateProgressScreen() {
		progressBar.setEnabled(false);
		progressPanel.dispose();
	};
}
