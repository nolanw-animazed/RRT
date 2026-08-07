package manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import data.InventoryItem;
import io.RestockIO;
import view.RestockGUI;

/**
 * Controller for the program, used to manage the data within the GUI and send Data to the IO Ports
 * 
 * @author Nolan Wright
 */
public class RestockController {
	
	/** GUI for the program */
	private RestockGUI gui;
	/** List of all Inventory Items for the Program, Used for Creating SpotChecks and Incoming Reports*/
	private List<InventoryItem> stockList;
	/** Hashmap used for Sales and Restock Reports [I Believe this is faster, but may clean it up]*/
	private HashMap<String, InventoryItem> quickStockList;
	/** List of the Location Names for the Program*/
	private List<String> locationNames;
	/** File name for the order data */
	private String dataFile;
	
	/**
	 * Default Constructor for the Program, starts the GUI Constructor
	 */
	public RestockController() {
		gui = new RestockGUI(this);
		//This are intialized later so they don't get resized constantly during big inventory dumps.
		stockList = null;
		quickStockList = null;
		locationNames = new ArrayList<String>();
		dataFile = null;
	}
	
	/**
	 * Called to Initialize the GUI For the program, specifically starting in loadInventory(), which will call parseInventoryData
	 */
	public void initialize() {
		gui.loadInventory();
	}
	
	
	/**
	 * Used to parse all the data from a given DataFile into their assorted locations
	 * @param fileName Name of the File for the Inventory Data
	 */
	public void parseInventoryData(String fileName) {
		//Read in the CSV File Data
		//Found Under Products > Inventory [Click Export, All Locations, All Products]
		List<String[]> inventoryDataUnsorted = RestockIO.readFileCSVData(fileName);
		
		String[] header = inventoryDataUnsorted.get(0);
		
		boolean findAllLocations = true; 
		int rowCounter = 1;
		
		//Copy all the Header Data into a HashMap so it can be called on later for column locations
		HashMap<String, Integer> headerData = new HashMap<String, Integer>(header.length);
		
		for(int i = 0; i < header.length; i++) {
			//System.out.println(header[i] + " " + i);
			headerData.put(header[i], i);
		}
		
		//If HeaderData doesn't continue SKU or location, the file is considered garbage.
		if(!headerData.containsKey("SKU") || !headerData.containsKey("Location")) {
			//This file is no fucking good.
			gui.showError("Please make sure it is an inventory file. Found Under Products > Inventory [Click Export, All Locations, All Products]");
			System.exit(-1);
		}
		
		
		//Technically HashMaps should have a O(1) get time, but I want to make sure the code is slightly less cluttered
		int skuLocation = headerData.get("SKU");
		int locationHeader = headerData.get("Location");;
		
		//Creates a Progress Bar [Otherwise the Program would just disappear for minutes]
		gui.createProgressScreen(inventoryDataUnsorted.size());
		
		//Find all the Locations Do While Loop
		//The system catalogs a location, then scoots forward, if it find the same location it ends the while loop
		do {
			String[] data = inventoryDataUnsorted.get(rowCounter);
			String locationName = data[locationHeader];
			
			if(locationNames.contains(locationName)) {
				findAllLocations = false;
				continue;
			}
			
			locationNames.add(locationName);
			rowCounter++;
		} while(findAllLocations);
		
		//Set the size of the Stock List [I have no fucking idea if this helps run time significantly, but shurrrrreeee]
		stockList = new ArrayList<InventoryItem>(inventoryDataUnsorted.size() / locationNames.size());
		
		//Grab the first SKU so we can start the cycling process.
		String sku = inventoryDataUnsorted.get(1)[skuLocation];
		
		//Create a List to contain all the inventory locations to send to each Inventory Item, this will be cleared after each use
		List<String[]> inventoryLocationsForAnItem = new ArrayList<String[]>();
		
		Iterator<String[]> dataIterator = inventoryDataUnsorted.iterator();
		
		dataIterator.next();
		
		int count = 1;
		
		do {
			String[] data = dataIterator.next();
			if(!data[skuLocation].equals(sku)) {
				InventoryItem item = new InventoryItem(inventoryLocationsForAnItem, headerData);
				//Add the item to this stockList
				stockList.add(item);
				//Set the new SKU to look for and clear the inventoryData
				sku = data[skuLocation];
				inventoryLocationsForAnItem.clear();
			}
			inventoryLocationsForAnItem.add(data);
			count++;
			
			//Every 1000 or so items, send a progress update to GUI, 1000 is used just so it isn't constantly prompting it to update.
			//Runtime impact should be minimal.
			if(count%1000 == 0) {
				gui.addProgressToTheBar(1000);
			}
		} while(dataIterator.hasNext());
		
 
		//Tell the GUI to set the LocationNames (this also is used to Initialize the GUI)
		gui.setLocationNames(locationNames);
	}

	/**
	 * Used to load the orders DataFile.
	 * @param dataFile The Absolute Path of the Data File
	 */
	public void loadDataForOrders(String dataFile) {
		this.dataFile = dataFile;
	}

	/**
	 * Called by the GUI to start the reports process based off the File loaded
	 * @param restockLocation Location at which the restocks will be pulled from.
	 */
	public void runTheReports(String restockLocation) {
		if(dataFile != null) {
			runAllReports(dataFile, restockLocation);
		} else {
			runNonOrdersReports(restockLocation);
		}
		
	}

	/**
	 * Used to run all the reports neccasary for the program [Currently Restock, Sales, Spot Checks, Incoming, and Zeroes]
	 * @param fileName Given file name by the system [Technically this method could just get the DataFile, but it's not a significant runtime change]
	 * @param restockLocation The Location at which restocks will be pulled from.
	 */
	private void runAllReports(String fileName, String restockLocation) {
		//Prepare QuickStock Here so the Opening of the file is faster
		prepareQuickStockList();
		
		List<String[]> transactionData = RestockIO.readFileCSVData(fileName);
		//Take the DateLength from RestockReport so you don't have to recalc it over and over again.
		String dateLength = runRestockReport(transactionData, restockLocation);
		runSalesReport(transactionData, dateLength);
		
		//Item dependent reports, so the orders data isn't needed.
		runSpotChecksReport(dateLength);
		runIncomingTransfersReport(dateLength);
		runZeroesReport(dateLength, restockLocation);
	}
	
	/**
	 * Used to run all non Order Reports [Spot Checks, Incoming, and Zeroes if the Restock Location was Picked]
	 * @param restockLocation RestockLocation for Zeroes if it Exists.
	 */
	private void runNonOrdersReports(String restockLocation) {
		//Prepare QuickStock Here so the Opening of the file is faster
		prepareQuickStockList();
		//Generating the Current Data as there isn't an order Length
		String dateLength = java.time.LocalDate.now().toString() + "NonOrdersReports";
		//Item dependent reports, so the orders data isn't needed.
		runSpotChecksReport(dateLength);
		runIncomingTransfersReport(dateLength);
		if(restockLocation != null && !restockLocation.equals("")) {
			runZeroesReport(dateLength, restockLocation);
		}
		
	}

	/**
	 * Used to Prepare the Quick Stock List [Just all the Inventory Items put into a Hashmap.]
	 * This ?may? help with program speed, but unsure if it's fully needed.
	 */
	private void prepareQuickStockList() {
		quickStockList = new HashMap<String, InventoryItem>(stockList.size());
		
		for(int i = 0; i < stockList.size(); i++) {
			InventoryItem item = stockList.get(i);
			
			String sku = item.getSku();
			
			if(sku != null && !sku.equals("")) {
				quickStockList.put(sku, item);
			}
		}
	}

	/**
	 * Runs a Restock Report for All Locations, via looking at the Restock Location's Stock Amount. If it has stock, it is printed in the location's report.
	 * @param transactionData All the Order data for the Time Period
	 * @param restockLocation The location that is the source of the restocks. If the location has 1 or more of an item, it will be exported for viewing.
	 * @return Returns the DataLength so other Reports don't need to redo calculation [helps a bit with runtime]
	 */
	private String runRestockReport(List<String[]> transactionData, String restockLocation) {
		
		//Header Data is put into Find Headers, so columns can be searched.
		String[] headerData = transactionData.get(0);
		HashMap<String, Integer> headerLocations = findHeaderLocations(headerData);
		
		//Time and Data Work
		//Takes the First Data on the Sheet and Last Data, compares them, and returns a string of the Dates
		String earliestDate = transactionData.get(1)[headerLocations.get("date")];
		
		//Try to get the Latest Date, if it fails go one up the sheet to try to get the correct date
		//This fixes a bug in Ver 1.1 where if there was a transaction with multiple items in the final order, it didn't get the date.
		String latestDate = "";
		
		int counterForDateChecking = transactionData.size()-1;
		do {
			//Check for LatestDate, and if nothing is found, try again
			latestDate = transactionData.get(counterForDateChecking)[headerLocations.get("date")];
			counterForDateChecking--;
		} while (latestDate.equals(""));
		
		String dateLength = figureOutDateRange(earliestDate, latestDate);
		
		//Creates a Hashmap used to link Location and List Data
		//This is used to contain each set of Output data linked to the Location
		HashMap<String, List<String[]>> restockOutputData = new HashMap<String, List<String[]>>();
		
		//Create all the Headers in each output File along with Initializing their lists
		for(int i = 0; i < locationNames.size(); i++) {
			List<String[]> data = new ArrayList<String[]>();
			String[] headerForRestockOutput = new String[7];
			String locationName = locationNames.get(i);
			headerForRestockOutput[0] = "Item Name";
			headerForRestockOutput[1] = "Variant";
			headerForRestockOutput[2] = "Sku";
			headerForRestockOutput[3] = "Price";
			headerForRestockOutput[4] = "# Sold";
			headerForRestockOutput[5] = locationName;
			headerForRestockOutput[6] = restockLocation;
			
			data.add(headerForRestockOutput);
			restockOutputData.put(locationName, data);
		}
		
		//Get the Header Locations into an Int [Helps code readability if I'm not constantly calling a hashmap]
		//Also creates a string to keep track of which items are going where
		//[Each line of the file is an item sale, but if it's not the first item, the location is blank until the next order, so you have to keep track of it.]
		String locationOfItems = null;
		int headerLocationOfLocation = (int) headerLocations.get("location");
		int headerLocationOfSku = (int) headerLocations.get("sku");
		
		//Skip Header, then start reading transaction Data
		for(int i = 1; i < transactionData.size(); i++) {
			//Grab the Item Transaction Data
			String[] lineDataForItem = transactionData.get(i);
			//If the Location isn't blank, set the location for the items
			if(lineDataForItem[headerLocationOfLocation] != null && !lineDataForItem[headerLocationOfLocation].equals("")) {
				locationOfItems = lineDataForItem[headerLocationOfLocation];
			}
			
			//If the order doesn't have a location skip it
			if(locationOfItems == null || locationOfItems.equals("")) {
				continue;
			}
			
			//If it's the restock from location, skip the item
			if(locationOfItems.equals(restockLocation)) {
				//Cannot restock from the same location
				continue;
			}
			
			//Grab the SKU, and if it it's blank or null, skip the Item
			String sku = lineDataForItem[headerLocationOfSku];
			if(sku == null || sku.equals("")) {
				//Skip lol
				continue;
			}
			//Look in the QuickStock list for the item [Assuming it has a sku, it should be in there]
			InventoryItem inventoryItem = quickStockList.get(sku);
			if(inventoryItem == null) {
				//Item doesn't exist? Usually because of an outdated Inventory File
				continue;
			}
			
			//Grab the Restock List for the Location you are looking at.
			List<String[]> listOfRestockItemsAtLocation = restockOutputData.get(locationOfItems);
			
			boolean found = false;
			//Loop used to add quanities onto Items
			//EX: If two of the same Goku sell in different transactions, there should just be one Mention of it on the restock, with a quantity of 2
			for(int j = 1; j < listOfRestockItemsAtLocation.size(); j++) {
				//Grab the Restock Data
				String[] data = listOfRestockItemsAtLocation.get(j);
				//If the SKU isn't the same, skip
				if(!data[2].equals(sku)) {
					continue;
				}
				//Add the Numbers together [They need to be parsed], replace the list [Due to it being an array, I think this is needed], then set the Found boolean to true to skip the rest of this loop
				data[4] = Integer.toString(Integer.parseInt(data[4]) + Integer.parseInt(lineDataForItem[headerLocations.get("quantity")]));
				listOfRestockItemsAtLocation.set(j, data);
				found = true;
			}
			
			//If the item was found in the restock, skip the rest of this
			if(found) {
				continue;
			}
			
			//This is a new item for the restock, grab the restock Output from the Inventory Item, with the restockLocation being where the item is being pulled from.
			String[] inventoryItemOutput = inventoryItem.getStockComparison(locationOfItems, restockLocation);
			//If it comes back Null, that means that the item didn't have stock in the restock location
			if(inventoryItemOutput == null) {
				//No Stock at location
				continue;
			}
			//Place Price and Quantity for the Item [The Inventory Data did not have this info so it's important to place it now, then add it to the list]
			inventoryItemOutput[3] = lineDataForItem[headerLocations.get("price")];
			inventoryItemOutput[4] = lineDataForItem[headerLocations.get("quantity")];
			listOfRestockItemsAtLocation.add(inventoryItemOutput);
			
		}
		
		//Output Loop for Restock Reports
		
		prepareDataForOutput(restockOutputData, dateLength, "RestockReport", null);

		//Pass the Datelength back to the other methods.
		return dateLength;
	}
	
	/**
	 * Used to Run the Sales Reports for the Orders [Restock without the restock location]
	 * @param transactionData All the Orders for the DateLength
	 * @param dateLength Given DateLength from the Restock Reports
	 */
	private void runSalesReport(List<String[]> transactionData, String dateLength) {
		String[] headerData = transactionData.get(0);
		HashMap<String, Integer> headerLocations = findHeaderLocations(headerData);
		
		HashMap<String, List<String[]>> salesOutputData = new HashMap<String, List<String[]>>();
		
		for(int i = 0; i < locationNames.size(); i++) {
			List<String[]> data = new ArrayList<String[]>();
			String[] headerForSalesOutput = new String[6];
			String locationName = locationNames.get(i);
			headerForSalesOutput[0] = "Item Name";
			headerForSalesOutput[1] = "Variant";
			headerForSalesOutput[2] = "Sku";
			headerForSalesOutput[3] = "Price";
			headerForSalesOutput[4] = "# Sold";
			headerForSalesOutput[5] = locationName;
			
			data.add(headerForSalesOutput);
			salesOutputData.put(locationName, data);
		}
		
		String locationOfItems = null;
		int headerLocationOfLocation = (int) headerLocations.get("location");
		int headerLocationOfSku = (int) headerLocations.get("sku");
		
		for(int i = 1; i < transactionData.size(); i++) {
			String[] lineDataForItem = transactionData.get(i);
			if(lineDataForItem[headerLocationOfLocation] != null && !lineDataForItem[headerLocationOfLocation].equals("")) {
				locationOfItems = lineDataForItem[headerLocationOfLocation];
			}
			
			//If it doesn't have a SKU Skip it
			String sku = lineDataForItem[headerLocationOfSku];
			if(sku == null || sku.equals("")) {
				//Skip lol
				continue;
			}
			
			//If the order doesn't have a location skip it
			if(locationOfItems == null || locationOfItems.equals("")) {
				continue;
			}
			
			//If no inventory item is found skip it
			InventoryItem inventoryItem = quickStockList.get(sku);
			if(inventoryItem == null) {
				continue;
			}
			
			List<String[]> listOfSalesItemsAtLocation = salesOutputData.get(locationOfItems);
			
			boolean found = false;
			//Loop used to add quanities onto Items
			//EX: If two of the same Goku sell in different transactions, there should just be one Mention of it on the restock, with a quantity of 2
			//Taken from the Restock Report Function
			for(int j = 1; j < listOfSalesItemsAtLocation.size(); j++) {
				String[] data = listOfSalesItemsAtLocation.get(j);
				if(!data[2].equals(sku)) {
					continue;
				}
				data[4] = Integer.toString(Integer.parseInt(data[4]) + Integer.parseInt(lineDataForItem[headerLocations.get("quantity")]));
				listOfSalesItemsAtLocation.set(j, data);
				found = true;
			}
			//If it was already in the list, go ahead and skip the rest
			if(found) {
				continue;
			}
			
			String[] inventoryItemOutput = inventoryItem.getSalesForLocation(locationOfItems);
			if(inventoryItemOutput == null) {
				//No Stock at location
				continue;
			}
			inventoryItemOutput[3] = lineDataForItem[headerLocations.get("price")];
			inventoryItemOutput[4] = lineDataForItem[headerLocations.get("quantity")];
			listOfSalesItemsAtLocation.add(inventoryItemOutput);
			
		}
		//Send Data to Output Prep Method
		prepareDataForOutput(salesOutputData, dateLength, "SalesReport", null);
	}

	/**
	 * Used to run the Incoming Transfers Report, Checking the incoming column on each item and seeing if there is anything there.
	 * @param dateLength Date Length for the Report taken from Restock [100% unnneeded, but helps with folder management]
	 */
	private void runIncomingTransfersReport(String dateLength) {
		//Output hashmap, same as the other reports
		HashMap<String, List<String[]>> transferOutputData = new HashMap<String, List<String[]>>();
		//Prepare the Headers for each of the Outputs.
		//I could maybe shorten Column 4?
		for(int i = 0; i < locationNames.size(); i++) {
			List<String[]> data = new ArrayList<String[]>();
			String[] headerForTransferOutput = new String[4];
			String locationName = locationNames.get(i);
			headerForTransferOutput[0] = "Item Name";
			headerForTransferOutput[1] = "Variant";
			headerForTransferOutput[2] = "Sku";
			headerForTransferOutput[3] = "# incoming " + locationName;
			
			data.add(headerForTransferOutput);
			transferOutputData.put(locationName, data);
		}
		
		//Creates the Incoming Reports for each location
		for(int i = 0; i < locationNames.size(); i++) {
			//Chooses the location to focus on
			String location = locationNames.get(i);
			for(int j = 0; j < stockList.size(); j++) {
				//Ask each stock item if there is stock incoming, if null is returned then nothing is incoming.
				String[] data = stockList.get(j).getIncomingStock(location);
				
				if(data == null) {
					continue;
				}
				
				transferOutputData.get(location).add(data);
			}
		}
		//Send Data to Output Prep Method
		prepareDataForOutput(transferOutputData, dateLength, "IncomingReport", null);
	}
	
	/**
	 * Used to run Spot Checks Report, looking for negative stock values in each location
	 * @param dateLength Date Length for the Report taken from Restock [100% unnneeded, but helps with folder management]
	 */
	private void runSpotChecksReport(String dateLength) {
		//Output hashmap, same as the other reports
		HashMap<String, List<String[]>> spotChecksOutputData = new HashMap<String, List<String[]>>();
		
		//Creates Header for each of the Outputs, however this only has the item info and a column for the variant count
		for(int i = 0; i < locationNames.size(); i++) {
			List<String[]> data = new ArrayList<String[]>();
			String[] headerForSpotChecksOutput = new String[4];
			String locationName = locationNames.get(i);
			headerForSpotChecksOutput[0] = "Item Name";
			headerForSpotChecksOutput[1] = "Variant";
			headerForSpotChecksOutput[2] = "Sku";
			headerForSpotChecksOutput[3] = locationName;
			
			data.add(headerForSpotChecksOutput);
			spotChecksOutputData.put(locationName, data);
		}
		
		//Creates each spot check list per location
		for(int i = 0; i < locationNames.size(); i++) {
			//For Each Location, Ask Each Stock if they Have Spot Checks
			String location = locationNames.get(i);
			for(int j = 0; j < stockList.size(); j++) {
				//Grab the Data from Spot Checks, if it is Null, that means there are no spot checks, so the item is skipped
				String[] data = stockList.get(j).getSpotCheck(location);
				
				if(data == null) {
					continue;
				}
				
				spotChecksOutputData.get(location).add(data);
			}
		}
		
		//Send Data to Output Prep Method
		prepareDataForOutput(spotChecksOutputData, dateLength, "SpotCheck", null);
	}
	

	/**
	 * Used to run the Zeroes for each location, via seeing if there is stock in the restocklocation that other locations do not have
	 * @param dateLength Used for the File Extension
	 * @param restockLocation Location the Zeroes are being pulled from.
	 */
	private void runZeroesReport(String dateLength, String restockLocation) {
		//Output hashmap, same as the other reports
		HashMap<String, List<String[]>> zeroesOutputData = new HashMap<String, List<String[]>>();
		
		//Creates Header for each of the Outputs, however this only has the item info and a column for the variant count
		for(int i = 0; i < locationNames.size(); i++) {
			List<String[]> data = new ArrayList<String[]>();
			String[] headerForZeroesOutput = new String[5];
			String locationName = locationNames.get(i);
			headerForZeroesOutput[0] = "Item Name";
			headerForZeroesOutput[1] = "Variant";
			headerForZeroesOutput[2] = "Sku";
			headerForZeroesOutput[3] = locationName;
			headerForZeroesOutput[4] = restockLocation;
			
			data.add(headerForZeroesOutput);
			zeroesOutputData.put(locationName, data);
		}
		
		//Creates each zeroes list per location
		for(int i = 0; i < locationNames.size(); i++) {
			//For Each Location, check if there is no stock (incoming or available) in the location, then check if the restocklocation has any, if so, send that data
			String location = locationNames.get(i);
			for(int j = 0; j < stockList.size(); j++) {
				//Grab the Data from Zeroes, if it is Null, that means there are no spot checks, so the item is skipped
				String[] data = stockList.get(j).getZeroes(location, restockLocation);
				
				if(data == null) {
					continue;
				}
				
				zeroesOutputData.get(location).add(data);
			}
		}
		
		//Send Data to Output Prep Method
		prepareDataForOutput(zeroesOutputData, dateLength, "ZeroesReport", null);
	}
	
	/**
	 * Used to Prepare Given Data for both a CSV and XLSX Output.
	 * @param outputData Given Output Data from the Reports
	 * @param dateLength Given DateLength for File Location
	 * @param nameOfReport Name of which Report
	 * @param restockLocation Specific Restock Location, used to block the Restock Report from making a file for the main restock location.
	 */
	private void prepareDataForOutput(HashMap<String, List<String[]>> outputData, String dateLength, String nameOfReport, String restockLocation) {
		for(int i = 0; i < locationNames.size(); i++) {
			//Grab the location Name for Output, if it's the restock location skip it
			String locationName = locationNames.get(i);
			
			if(nameOfReport.equals("RestockReport")) {
				if(locationName.equals(restockLocation)) {
					continue;
				}
			}
			
			//Grab the output list for the location
			List<String[]> outputListBasedOfLocation = outputData.get(locationName);
			//Pull out the Header before sorting the list
			String[] header = outputListBasedOfLocation.removeFirst();
			//Sort the list around the SKU location on the Restock
			outputListBasedOfLocation.sort(Comparator.comparing(array -> array[2]));
			//Reinsert the Header
			outputListBasedOfLocation.addFirst(header);
			//Create a File Output Path, and create all the folders for it
			String outputFilePath = dateLength + "/" + nameOfReport + "/" + locationName;
			Path path = Paths.get(outputFilePath);
			
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				System.out.println("FUCK");
				System.exit(0);
			}
			//Create the File Name, and pass it to the CSV and XLSX Writer [XLSX has to go through a helper method first]
			String outputFileName = outputFilePath + "/" + nameOfReport + "For" + locationName + "On" + dateLength;
			RestockIO.writeFileCSVData(outputListBasedOfLocation, outputFileName + ".csv");
			prepareWorkbookDataForExport(outputListBasedOfLocation, outputFileName + ".xlsx");
		}
	}

	/**
	 * Used to figure out the Date Range for the Order Histories
	 * @param earliestDate Earliest Date on the Order Sheet
	 * @param latestDate Latest Date on the Order Sheet
	 * @return Date range, if the dates are the same it just returns one.
	 */
	private String figureOutDateRange(String earliestDate, String latestDate) {
		String firstDate = earliestDate.split(" ")[0];
		String secondDate = latestDate.split(" ")[0];
		
		if(firstDate.equals(secondDate) ) {
			return firstDate;
		}
		
		return secondDate + "-" + firstDate;
	}

	/**
	 * Used to find all the locations of Important Pieces in the Header [Used in case of Shopify moving data around]
	 * @param headerData Raw Header Data from the top of the Orders File
	 * @return Returns a hashmap of specific maps that is needed by the reports
	 */
	private HashMap<String, Integer> findHeaderLocations(String[] headerData) {
		//Creates a Hashmap of [Specific Name for Header Location, Column Location]
		HashMap<String, Integer> headerLocations = new HashMap<String, Integer>();
		
		for(int i = 0; i < headerData.length; i++) {
			String headerPiece = headerData[i];
			
			//This switch statement looks for specific headers.
			//If at anytime, the Data Format for Orders is changed, this is what needs to change primarily.
			switch(headerPiece) {
				//Price of the Item, used in Restock
				case "Lineitem price":
					headerLocations.put("price", i);
					break;
				//Quantity of Item sold
				case "Lineitem quantity":
					headerLocations.put("quantity", i);
					break;
				//SKU of the Item
				case "Lineitem sku":
					headerLocations.put("sku", i);
					break;
				//Location where the Item sold.
				case "Location":
					headerLocations.put("location", i);
					break;
				//This goes unused, but it could be helpful to remove refunds from restocks.
				//For now the system leaves them in the restocks for clarity.
				case "Refunded Amount":
					headerLocations.put("refund", i);
					break;
				//When the Customer Pays [Used for Date]
				case "Paid at":
					headerLocations.put("date", i);
					break;
			}
		}
		
		return headerLocations;
	}
	
	/**
	 * Used to Prepare Data for Export for XLSX Files via the use of [org.apache.poi]. It changes the files into Workbooks.
	 * @param outputData The Data in the Format Needed for CSV Writer, needs to be organized.
	 * @param fileName FileName for Output [has .xlsx taped on the end]
	 */
	private void prepareWorkbookDataForExport(List<String[]> outputData, String fileName) {
		//Create the XSSF Workbook along with the main Sheet
		Workbook workbook = new XSSFWorkbook();
		
		Sheet sheet = workbook.createSheet();
		
		//Create a Print set up to assist with time saving on printing
		PrintSetup printSetup = sheet.getPrintSetup();
		
		printSetup.setLandscape(true);
		//This one may not do anything???? I can't seem to see if it does, but it doesn't hurt
		printSetup.setFitWidth((short) 1);
		
		//Create a Boxed style for all of the Cells, with Medium Thickness
		CellStyle borderStyle = workbook.createCellStyle();
		
		borderStyle.setBorderBottom(BorderStyle.MEDIUM);
		borderStyle.setBorderTop(BorderStyle.MEDIUM);
		borderStyle.setBorderLeft(BorderStyle.MEDIUM);
		borderStyle.setBorderRight(BorderStyle.MEDIUM);
		
		//Transpose all the Data via just some For Loop Transposing.
		for(int i = 0; i < outputData.size(); i++) {
			Row row = sheet.createRow(i);
			String[] rowData = outputData.get(i);
			for(int j = 0; j < rowData.length; j++) {
				Cell cell = row.createCell(j);
				cell.setCellValue(rowData[j]);
				cell.setCellStyle(borderStyle);
			}
		}
		
		//Resize all the columns so the user doesn't have to do it on Excel open.
		int columns = outputData.get(0).length;
		
		for(int i = 0; i < columns; i++) {
			sheet.autoSizeColumn(i);
		}
		
		//Send it to the IO for output
		RestockIO.weiteFileXLSXData(workbook, fileName);
	}
}
