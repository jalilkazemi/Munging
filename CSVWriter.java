package com.jalil.munge;

import java.io.*;

public class CSVWriter {
	
	BufferedWriter out = null;
	int ncolumn;
	boolean eof = false;

	public CSVWriter(String csvFile, String[] colnames) {
		try {
			out = new BufferedWriter(new FileWriter(csvFile));
			ncolumn = colnames.length;
			StringBuilder builder = new StringBuilder();
			for (String colname : colnames) {
				builder.append("," + colname);
			}
			out.write(builder.deleteCharAt(0).toString());
			out.newLine();
		} catch (IOException e) {
			eof = true;
			e.printStackTrace();
		}
	}

	public void writeLineDouble(double[] row) {
		if(eof)
			throw new RuntimeException("The end of file has reached.");
		try {
			if(row.length != ncolumn)
				System.out.println("The number of columns doesn't match.");
			StringBuilder builder = new StringBuilder();
			for (double field : row) {
				builder.append("," + field);
			}
			out.write(builder.deleteCharAt(0).toString());
			out.newLine();
		} catch (IOException e) {
			eof = true;
			e.printStackTrace();
		}
	}

	public void close() {
		eof = true;
		try{
			if(out != null) 
				out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}