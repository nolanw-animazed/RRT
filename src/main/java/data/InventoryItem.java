/**
 * 
 */
package data;

import java.util.HashMap;
import java.util.List;

/**
 * Container Class for all of the Inventory Data relating to a single SKU.
 * @author Nolan Wright
 *
 */
public class InventoryItem {
	/** Hashmap [Location String, Number of Available Stock] used to get the available stock amount for a location.*/
	private HashMap<String, Integer> availableStockData;
	/** Hashmap [Location String, Number of Incoming Stock] used to get the incoming stock amount for a location.*/
	private HashMap<String, Integer> incomingStockData;
	/** Title of the Item for identification during output */
	private String title;
	/** SKU of the Item used, as the main point of comparison [They should be unique]*/
	private String sku;
	/** Name of the Variant [Ex: Set of plushes, the variant is the name of the specific plush.]*/
	private String variant;

	/**
	 * Constructor for the InventoryItem, using the Inventory Item Data given by RestockController along with Header Data to
	 * sort all the data into the correct lists.
	 * @param inventoryLocationsForAnItem Information for each Location
	 * @param headerData Locations of where Items are in the Header
	 */
	public InventoryItem(List<String[]> inventoryLocationsForAnItem, HashMap<String, Integer> headerData) {
		//Creates Hashmaps for the Incoming and Avaiable Stock
		availableStockData = new HashMap<String, Integer>();
		incomingStockData = new HashMap<String, Integer>();
		for(int i = 0; i < inventoryLocationsForAnItem.size(); i++) {
			//For All Locations grab the Data needed, and put it into the hashlists.
			String[] lineItem = inventoryLocationsForAnItem.get(i);
			
			availableStockData.put(lineItem[headerData.get("Location")], Integer.parseInt(lineItem[headerData.get("Available (not editable)")]));
			incomingStockData.put(lineItem[headerData.get("Location")], Integer.parseInt(lineItem[headerData.get("Incoming (not editable)")]));
		}
		
		String[] lineItem = inventoryLocationsForAnItem.get(0);
		//Set the other Variables
		title = lineItem[headerData.get("Title")];
		sku = lineItem[headerData.get("SKU")];
		variant = lineItem[headerData.get("Option1 Value")];
	}

	/**
	 * Getter Method for the Title
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Setter Method for the Title
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Getter Method for the SKU
	 * @return the sku
	 */
	public String getSku() {
		return sku;
	}

	/**
	 * Setter Method for the SKU
	 * @param sku the sku to set
	 */
	public void setSku(String sku) {
		this.sku = sku;
	}

	/**
	 * Getter Method for the Variant
	 * @return the variant
	 */
	public String getVariant() {
		return variant;
	}

	/**
	 * Setter Method for the Variants
	 * @param variant the variant to set
	 */
	public void setVariant(String variant) {
		this.variant = variant;
	}
	
	/**
	 * Used for Restock Reporting, it takes in two locations [RestockTo is the location that needs the stock, and RestockFrom is where the stock will be pulled from]
	 * It then asks the restockLocation if has any stock, and if it does it generates the data Array and returns it.
	 * @param restockTo Location that needs the Stock from the Restock
	 * @param restockFrom Location the Inventory is being pulled from
	 * @return Returns Null if the RestockLocation doesn't have stock, else it returns a String array of data
	 */
	public String[] getStockComparison(String restockTo, String restockFrom) {
		//Ask if the Restock Location has any stock, if not return null
		int fromCount = availableStockData.get(restockFrom);
		if(fromCount <= 0) {
			return null;
		}
		//String[] For Reporting
		String[] data = new String[7];
		data[0] = title;
		data[1] = variant;
		data[2] = sku;
		data[5] = Integer.toString(availableStockData.get(restockTo));
		data[6] = Integer.toString(fromCount);
		return data;
	}
	
	/**
	 * Used to get the sales report for each location, simply returning the count of the item at the location requested.
	 * @param location Location where the item sold
	 * @return Returns a Data Array of the stock at the location.
	 */
	public String[] getSalesForLocation(String location) {
		//Sales has no Fail Conditions, so it just asks for the stock and makes the data
		int count = availableStockData.get(location);
		String[] data = new String[6];
		data[0] = title;
		data[1] = variant;
		data[2] = sku;
		data[5] = Integer.toString(count);
		return data;
	}

	/**
	 * Used to Check if there is any incoming stock to a specific location, and if there it is it returns that data.
	 * @param location Location being checked for Incoming Stock
	 * @return Returns null if there is no Incoming Stock, otherwise it will return a data array of the incoming stock.
	 */
	public String[] getIncomingStock(String location) {
		//If there is no coming stock, return Null
		if(incomingStockData.get(location) <= 0) {
			return null;
		}
		//Else Create the Data Array for Incoming Stock
		String[] data = new String[4];
		data[0] = title;
		data[1] = variant;
		data[2] = sku;
		data[3] = Integer.toString(incomingStockData.get(location));
		return data;
	}
	
	/**
	 * Used to Get Spot Checks for a location, Simply checking to see if at a location the stock is negative.
	 * @param location Location being checked for negatives.
	 * @return Returns Null if the location isn't negative, Otherwise it returns an array of data.
	 */
	public String[] getSpotCheck(String location) {
		//If there isn't a negative stock at the location return null
		if(availableStockData.get(location) >= 0) {
			return null;
		}
		
		//Else Return the Data Array with the Negative
		String[] data = new String[4];
		data[0] = title;
		data[1] = variant;
		data[2] = sku;
		data[3] = Integer.toString(availableStockData.get(location));
		return data;
	}

	/**
	 * Used to get the Zeroes based off the locations given. If there is no stock (incoming or available) in the location given, and there is stock in the restockLocation
	 * then it returns an array of Data with that info.
	 * @param location Location that is being checked for Zeroes
	 * @param restockLocation Location used to pull the inventory from
	 * @return Returns Null if it hits a fail condition, otherwise it returns the String Array of Data
	 */
	public String[] getZeroes(String location, String restockLocation) {
		//3 Fail Conditions, If There is available Stock or Incoming Stock in the location that is being checked, or if the RestockLocation has no Stock
		if(availableStockData.get(location) > 0) {
			return null;
		}
		if(incomingStockData.get(location) > 0) {
			return null;
		}
		if(availableStockData.get(restockLocation) <= 0) {
			return null;
		}
		
		//Else Create a Data Array with both stock counts for the locations
		//(I could probably cut the Location counts out of here given they are going to be 0 at all times)
		String[] data = new String[5];
		data[0] = title;
		data[1] = variant;
		data[2] = sku;
		data[3] = Integer.toString(availableStockData.get(location));
		data[4] = Integer.toString(availableStockData.get(restockLocation));
		
		return data;
	}
	
}
