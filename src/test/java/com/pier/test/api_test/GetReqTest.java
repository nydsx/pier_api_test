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
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.inject.spi.Message;
import com.jayway.restassured.response.Response;

public class GetReqTest implements ITest {
	protected static final Logger logger = LoggerFactory.getLogger(GetReqTest.class);
    private Response response;
    private DataReader myInputData;
    private DataReader myBaselineData;
    private String template;

    public String getTestName() {
        return "API Test";
    }
    
    String filePath = "";
  
    XSSFWorkbook wb = null;
    XSSFSheet inputSheet = null;
    XSSFSheet baselineSheet = null;
 
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
        baselineSheet = wb.getSheet("Baseline");

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
            myBaselineData = new DataReader(baselineSheet, true, true, 0);
        return test_IDs.iterator();
    }

	@Test(dataProvider = "WorkBookData", description = "GetReqTest")
    public void api_test(String ID, String test_case) {

        GetReq myReq = new GetReq();
        int code = 0;
        String message = "";
        try {
            myReq.generate_request(template, myInputData.get_record(ID));
            response = myReq.perform_request();
            logger.info("ResponseBody:"+response.asString());
            logger.info("statusLine:"+response.getStatusLine());
            JSONObject dataJson = new JSONObject(response.getBody().asString());
       	 	code = dataJson.getInt("code");
        } catch (Exception e) {
           Assert.fail("Problem using HTTPRequestGenerator to generate response: " + e.getMessage());
        }
        String pattern = myBaselineData.get_record(ID).get("Pattern");
        if (code == 200){
        	System.out.println(pattern+"-------------------");
        	Pattern r = Pattern.compile(pattern);
        	if(r.matcher(response.asString()).find()){
        		logger.info("API正常返回数据！");
				logger.info(ID+"\t\t" + test_case );
        	}else {
				logger.info("数据丢失！");
				logger.info(ID+"\t\t" + test_case );
			}
        }else{
        	switch (code) {
        	case 404:
				logger.info("数据未找到！");
		        logger.info(ID+"\t\t" + test_case );
				break;
        	case 400:
				logger.info("请求参数丢失！");
		        logger.info(ID+"\t\t" + test_case );
				break;
			case 1112:
				logger.info("授权码错误！");
		        logger.info(ID+"\t\t" + test_case );
				break;
			case 1065:
				logger.info("该邮箱被注册！");
		        logger.info(ID+"\t\t" + test_case );
				break;
			case 1001:
				logger.info("会话过期请重新登录！");
		        logger.info(ID+"\t\t" + test_case );
				break;
			case 1145:
				logger.info("验证码无效！");
		        logger.info(ID+"\t\t" + test_case );
				break;
			default:
				break;
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