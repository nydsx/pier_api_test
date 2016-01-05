package com.pier.test.utils;

import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ReadProperties {
	private static Logger logger = Logger.getLogger(ReadProperties.class);
	static private String body = null;
	static private String url = null;
	static private String URL = null;
	static {
		loads();
	}
	synchronized static public void loads() {
		if (body == null || url == null || URL == null) {
			InputStream is = ReadProperties.class.getResourceAsStream("/config.properties");
			Properties properties = new Properties();
			try {
				properties.load(is);

				body = properties.getProperty("signinBody").toString();
				url = properties.getProperty("url").toString();
				URL = properties.getProperty("URL").toString();
			} catch (Exception e) {
				logger.info("");
			}
		}
	}

	public static String getBody() {
		if (body == null)
			loads();
		return body;
	}

	public static String getUrl() {
		if (url == null)
			loads();
		return url;
	}
	
	public static String getURL() {
		if (URL == null)
			loads();
		return URL;
	}

}
