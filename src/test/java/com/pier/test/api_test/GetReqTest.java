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
import java.util.regex.Pattern;

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

public class GetReqTest implements ITest {
	//protected static final Logger logger = LoggerFactory.getLogger(GetReqTest.class);
	private static Logger logger = Logger.getLogger(GetReqTest.class);
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
            InputStream is = GetReqTest.class.getClassLoader().getResourceAsStream("request_template.txt");
        	template = IOUtils.toString(is, Charset.defaultCharset());
        } catch (Exception e) {
            Assert.fail("Problem fetching data from input file:" + e.getMessage());
        }
        
    }

    @DataProvider(name = "WorkBookData")
    protected Iterator<Object[]> testProvider(ITestContext context) {

    		List<Object[]> test_IDs = new ArrayList<Object[]>();

            myInputData = new DataReader(inputSheet, true, true, 0);

            // sort map in order so that test cases ran in a fixed order
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
            //myBaselineData = new DataReader(baselineSheet, true, true, 0);
        return test_IDs.iterator();
    }

	@Test(dataProvider = "WorkBookData", description = "GetReqTest")
    public void api_test(String ID, String test_case) {

        GetReq myReq = new GetReq();
        int code = 0;
        try {
            myReq.generate_request(template, myInputData.get_record(ID));
            response = myReq.perform_request();
            logger.info("ResponseBody:"+response.asString()+"\n"+"statusLine:"+response.getStatusLine());
            
            JSONObject dataJson = new JSONObject(response.getBody().asString());
       	 	code = dataJson.getInt("code");
       	 	
        } catch (Exception e) {
        		e.printStackTrace();
          }
        	String pattern = myInputData.get_record(ID).get("Pattern");
        //	logger.info(pattern);
        if (code == 200){
        		Pattern r = Pattern.compile(pattern);
        		if(r.matcher(response.asString()).find()){
        			logger.info("API正常返回数据！"+"\n"+ID+"\t\t" + test_case);
        		}else {
        			logger.info("请求未正常返回数据！"+"\n"+ID+"\t\t" + test_case);
        			Assert.fail();
        		}
        }else{
        		try {
					logger.info((new JSONObject(response.getBody().asString())).getString("message")+"\n"+ID+"\t\t" + test_case );
					Assert.fail();
				} catch (JSONException e) {
					e.printStackTrace();
				}
        }
       
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