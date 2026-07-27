package io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvException;

/**
 * IO Class used for interacting with the Files for the Restock Report Tool
 * 
 * @author Nolan Wright
 */
public class RestockIO {
	
	/**
	 * Used to read in simple CSV Data from Files within the Program
	 * @param fileLocation Filelocation of the file
	 * @return Returns an ArrayList of the File Data
	 */
	public static List<String[]> readFileCSVData(String fileLocation) {
		List<String[]> myEntries = null;
		try {
			CSVReader reader = new CSVReaderBuilder(new FileReader(fileLocation)).build();
			myEntries = reader.readAll();
		    reader.close();
		} catch (IOException | CsvException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		return myEntries;
	}
	
	/**
	 * Used to write data to CSV Files from within the Program
	 * @param myEntries List of String[] Data from the program
	 * @param fileLocation FileLocation used for saving.
	 */
	public static void writeFileCSVData(List<String[]> myEntries, String fileLocation) {
		try {
			ICSVWriter writer = new CSVWriterBuilder(new FileWriter(fileLocation)).build();
			writer.writeAll(myEntries);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * Used to write data to an XLSX File from within the Program
	 * @param workbookForOutput Created Workbook used for the Output
	 * @param fileLocation FileLocation used for Saving.
	 */
	public static void weiteFileXLSXData(Workbook workbookForOutput, String fileLocation) {
			OutputStream output;
			try {
				output = new FileOutputStream(fileLocation);
				workbookForOutput.write(output);
			} catch (IOException e) {
				System.out.println("Fuck XLSX");
				e.printStackTrace();
			}
			
	}
}
