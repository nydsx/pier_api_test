package com.pier.test.utils;

import static com.jayway.restassured.RestAssured.given;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

/**
 * Wrapper for RestAssured. Uses an HTTP request template and a single record
 * housed in a RecordHandler object to generate and perform an HTTP requests.
 * 
 */
public class GetRequest {

	private static Logger LOG = Logger.getLogger(GetRequest.class);
	private RequestSpecification reqSpec;
	private String signinBody = ReadProperties.getBody();
	private String signinUrl = ReadProperties.getUrl();
	private String call_host = ReadProperties.getURL();
	private String call_suffix = "";
	private String call_url = "";
	private String call_type = "";
	private String body = "";
	private Map<String, String> headers = new HashMap<String, String>();
	private HashMap<String, String> cookie_list = new HashMap<String, String>();

	public Map<String, String> getHeaders() {
		return headers;
	}

	public String getCallString() {
		return call_url;
	}
	/**
	 * Constructor. Initializes the RequestSpecification (relaxedHTTPSValidation
	 * avoids certificate errors).
	 */
	public GetRequest() {
		reqSpec = given().relaxedHTTPSValidation();
	}

	public GetRequest(String proxy) {
		reqSpec = given().relaxedHTTPSValidation().proxy(proxy);
	}

	public GetRequest generate_request(String template, RecordHandler record) throws Exception {
		return generate_request(template, (HashMap<String, String>) record.get_map());
	}

	public GetRequest generate_request(String template, HashMap<String, String> record) throws Exception {
		String filled_template = "";
		Boolean found_replacement = true;
		headers.clear();

		try {
			String[] tokens = tokenize_template(template);
			while (found_replacement) {
				found_replacement = false;
				filled_template = "";

				for (String item : tokens) {
					if (item.startsWith("<<") && item.endsWith(">>")) {
						found_replacement = true;
						item = item.substring(2, item.length() - 2);
						if (!record.containsKey(item)) {
							LOG.info(
									"Template contained replacement string whose value did not exist in input record:["
											+ item + "]");
						}
						item = record.get(item);
					}
					filled_template += item;
				}
				tokens = tokenize_template(filled_template);
			}
		} catch (Exception e) {
			LOG.error("Problem performing replacements from template: ", e);
		}
		try {
			InputStream stream = IOUtils.toInputStream(filled_template, "UTF-8");
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			String line = "";
			String[] line_tokens;

			line = in.readLine();
			line_tokens = line.split(" ");

			call_type = line_tokens[0];
			call_suffix = line_tokens[1];
			call_url = call_host + call_suffix;
			
			line = in.readLine();
			while (line != null && !line.equals("")) {
				String lineP1 = line.substring(0, line.indexOf(":")).trim();
				String lineP2 = line.substring(line.indexOf(" "), line.length()).trim();
				headers.put(lineP1, lineP2);
				line = in.readLine();
			}

			if (line != null && line.equals("")) {
				body = "";
				while ((line = in.readLine()) != null && !line.equals("")) {
					body += line;
				}
			}
		} catch (Exception e) {
			LOG.error("Problem setting request values from template: ", e);
		}
		return this;
	}

	public Response perform_request() throws Exception {

		Response response = null;

		try {

			for (Map.Entry<String, String> entry : headers.entrySet()) {
				reqSpec.header(entry.getKey(), entry.getValue());
			}

			for (Map.Entry<String, String> entry : cookie_list.entrySet()) {
				reqSpec.cookie(entry.getKey(), entry.getValue());
			}
			switch (call_type) {
				case "GET": {
					response = reqSpec.get(call_url);
					LOG.info("GET URL:" + call_url);
					break;
				}
				case "POST": {
					String session_token = getSessionToken(reqSpec);
					String user_id = getUserId(reqSpec);
					body = body.replace("default_user_id", user_id);
					body = body.replace("templet_session_token", session_token);
					response = reqSpec.body(body).post(call_url);
					LOG.info("POST URL:" + call_url + "   RequestBody:" + body);
					break;
				}
				default: {
					LOG.error("Unknown call type: [" + call_type + "]");
				}
			}
		} catch (Exception e) {
			LOG.error("Problem performing request: ", e);
		}
		return response;
	}
	
	private String[] tokenize_template(String template) {
		return template.split("(?=[<]{2})|(?<=[>]{2})");
	}
	
	public String getSessionToken(RequestSpecification reqSpec) {
		String session_token = null;
		Response initRes = reqSpec.body(signinBody).post(signinUrl);
		try {
			JSONObject dataJson = new JSONObject(initRes.getBody().asString());
			session_token = dataJson.getJSONObject("result").getString("session_token");
		} catch (JSONException e) {
			e.printStackTrace();
			LOG.info("init signin error");
		}
		return session_token;
	}
	public String getUserId(RequestSpecification reqSpec){
		String user_id = null;
		Response initRes = reqSpec.body(signinBody).post(signinUrl);
		try {
			JSONObject dataJson = new JSONObject(initRes.getBody().asString());
			user_id = dataJson.getJSONObject("result").getString("user_id");
		} catch (JSONException e) {
			e.printStackTrace();
			LOG.info("get user id error");
		}
		return user_id;
	}
}
