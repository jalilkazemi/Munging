package com.jalil.munge;

import java.io.*;

public class CSVReader {

	BufferedReader in = null;
	int ncolumn;
	boolean eof = false;

	public CSVReader(String csvFile) {
		
		try {
			in = new BufferedReader(new FileReader(csvFile));
			String scan = in.readLine(); // It assumes the file has a header.
			String[] fields;
			int n_row;
			if(scan == null)
				throw new IllegalArgumentException(csvFile + " has no records.");
			ncolumn = scan.split(",").length;
		} catch(IOException e) {
			eof = true;
			e.printStackTrace();
		} 
	}
	
	// REWRITE!!! see the comments.
	public double[] readLineDouble() {
		if(eof)
			throw new RuntimeException("The end of file has reached.");
		double[] row = null;
		try {
			String scan = in.readLine();
			if(scan == null) {
				eof = true;
				return null;
			}
			String[] fields = scan.split(","); // It does not consider the global case where a field can contain " " with comma in beteween.
			if(fields.length != ncolumn)
				throw new IllegalArgumentException("The number of columns doesn't match the first row.");
			row = new double[ncolumn];
			for(int i = 0; i < ncolumn; i++)
				row[i] = Double.parseDouble(fields[i]);
		} catch(IOException e) {
			eof = true;
			e.printStackTrace();
		} 

		return row;
	}

	public void close() {
		eof = true;
		try{
			if(in != null) 
				in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}