package com.pier.test.utils;

public class Utils {
    public static String removeSpaces(String str){
    	return str.replaceAll("[\\s]+", "");
    }
}