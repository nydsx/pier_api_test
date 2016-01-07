package com.pier.test.api_test;

import com.pier.test.utils.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.jayway.restassured.response.Response;

public class APITest implements ITest {
	private static Logger LOG = Logger.getLogger(APITest.class);
    private Response response;
    private DataReader myInputData;
    private String template;

    public String getTestName() {
        return "API Test";
    }
    
    String filePath = "";
    XSSFWorkbook wb = null;
    XSSFSheet inputSheet = null;
 
    @BeforeTest
    @Parameters("workBook")
    public void setup(String path) {
        filePath = path;
     
        try {
            wb = new XSSFWorkbook(new FileInputStream(filePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        inputSheet = wb.getSheet("Input");

        try {    	   
            InputStream is = APITest.class.getClassLoader().getResourceAsStream("request_template.txt");
        	   template = IOUtils.toString(is, Charset.defaultCharset());
        } catch (Exception e) {
            Assert.fail("Problem fetching data from input file:" + e.getMessage());
        }
    }

    @DataProvider(name = "WorkBookData")
    protected Iterator<Object[]> testProvider(ITestContext context) {
    		List<Object[]> test_IDs = new ArrayList<Object[]>();
         myInputData = new DataReader(inputSheet, true, true, 0);
         Map<String, RecordHandler> sortmap = new TreeMap<String,RecordHandler>(new Comparator<String>(){
				public int compare(String key1, String key2) {
					return key1.compareTo(key2);
				}
            });
            sortmap.putAll(myInputData.get_map());
            for (Map.Entry<String, RecordHandler> entry : sortmap.entrySet()) {
                String test_ID = entry.getKey();
                String test_case = entry.getValue().get("TestCase");
                if (!test_ID.equals("") && !test_case.equals("")) {
                    test_IDs.add(new Object[] { test_ID, test_case });
                }
            }
        return test_IDs.iterator();
    }

	@Test(dataProvider = "WorkBookData", description = "GetReqTest")
    public void api_test(String ID, String test_case) {

        GetRequest myReq = new GetRequest();
        int code = 0;
        String jsonResult = "";
        try {
            myReq.generate_request(template, myInputData.get_record(ID));
            response = myReq.perform_request();
            jsonResult = response.asString();
			   LOG.info("ResponseBody:" + jsonResult);
            JSONObject dataJson = new JSONObject(jsonResult);
       	 	code = dataJson.getInt("code");
       	 	LOG.info("状态码 : "+code);
        } catch (Exception e) {
        		e.printStackTrace();
          }
        	String pattern = myInputData.get_record(ID).get("Templet");
        if (code == 200){
        		if(checkResult(jsonResult, pattern)){
        			LOG.info("API正常返回数据！"+"\n"+ID+"\t\t" + test_case);
        		}else {
        			LOG.info("请求未正常返回数据，请检查！！！"+"\n"+ID+"\t\t" + test_case);
        			Assert.fail();
				}
        }else{
        		try {
					LOG.info(new JSONObject(jsonResult).getString("message")+"\n"+ID+"\t\t" + test_case );
				} catch (JSONException e) {
					e.printStackTrace();
				}
        }
    }
	
	public boolean checkResult(String jsonResult,String pattern){
		String [] result = new String[20];
		boolean flag = true;
		if(pattern.contains(","))
			result = pattern.split(",");
		else 
			result[0] = pattern;
		for(int a = 0;a<result.length;a++){
			if(jsonResult.contains(result[a])){
				flag = true;
			}else{
				flag = false;
				break;
			}
		}
		return flag;
	}
	
    @AfterTest
    public void teardown() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            wb.write(fileOutputStream);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}