package com.pier.test.utils;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
/**
 * Class that write data to XSSF sheet
 * 
 */
public class DataWriter {

	
	public static void writeData(XSSFSheet outputSheet, String iD, String test_case, String string) {
		int lastNum = outputSheet.getLastRowNum();
		if(0 == lastNum){
			writeSheet(outputSheet.createRow(lastNum),"ID","TestCase","Response");
			lastNum ++;
		}
		writeSheet(outputSheet.createRow(lastNum),iD,test_case,string);
	}
	
	public static void writeSheet(XSSFRow row, String... data){
		for(int i=0;i<data.length;i++){
			row.createCell(i).setCellValue(data[i]);
		}
	}

	public static void writeData(XSSFSheet resultSheet, String string, String iD, String test_case, int i) {
		int lastNum = resultSheet.getLastRowNum();
		writeSheet(resultSheet.createRow(lastNum+1),iD,test_case,string);
	}

	public static void writeData(XSSFSheet comparsionSheet,String string, String msg, String iD, String test_case) {
		int lastNum = comparsionSheet.getLastRowNum();
		writeSheet(comparsionSheet.createRow(lastNum+1),iD,test_case);
		
	}

	public static void writeData(XSSFSheet resultSheet, double totalcase, double failedcase, String startTime,
			String endTime) {
		
		writeSheet(resultSheet.createRow(1),String.valueOf(totalcase),String.valueOf(failedcase),startTime,endTime);
	}

}