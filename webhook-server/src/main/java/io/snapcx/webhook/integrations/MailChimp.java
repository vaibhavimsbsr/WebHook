package io.snapcx.webhook.integrations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.snapcx.webhook.Utils;

@Repository("mailChimp")
public class MailChimp {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final String providerName = "MAILCHIMP";

	@Value("${mailChimp.base.url}")
	private String endPointURL;

	@Value("${mailChimp.default.listId}")
	private String listId;

	@Value("${mailChimp.userId}")
	private String userId;

	@Value("${mailChimp.apiKey}")
	private String apiKey;
	
	private File file = new File("/home/online1439/notification.json");
	private static final BlockingQueue<String> QUEUE =  new LinkedBlockingQueue<>();

	@Autowired
	private RestTemplate restTemplate;
	private HttpHeaders httpHeaders = null;

	public String addNewMemberToList(String emailAddress, String orgName, String fName, String lName) {
		try {
			MCAddNewSubscriberRequest request = new MCAddNewSubscriberRequest();
			request.email_address = emailAddress;
			request.merge_fields = new MCMergeFields();
			request.merge_fields.FNAME = fName;
			request.merge_fields.LNAME = lName;
			request.merge_fields.MMERGE3 = orgName;

			String queueMessage = "";
			Random random = new Random();
			request.id = Integer.toString(random.nextInt(100));
			// JsonNode node = this.getJSONTreeNodeUsingRestTemplate(request,
			// this.endPointURL+"/lists/"+this.listId+"/members");
			JsonNode node = this.getJSONTreeNodeUsingRestTemplate(request, this.endPointURL);

			if(!file.exists()){
				file.createNewFile();
			}
			BufferedWriter out = new BufferedWriter( 
	                new FileWriter(file, true));
			queueMessage = request.id+","+fName +","+emailAddress;
			out.write(queueMessage+"\n"); 
	        out.close(); 
	        System.out.println("Successfully write to File...="+file.getAbsolutePath());
	        QUEUE.add(queueMessage);
			
			
			
			// String newId = node.get("id").asText();
			String newId = request.id;
			this.logger.info("Returned subscriber id for user " + emailAddress + " is " + request.id);

			if (newId == null || newId.isEmpty()) {
				this.logger.error("Errorneous response string is \n" + node.toString());
				throw new Exception("Something went wrong with request. Returned new id is empty");
			}
			return newId;
		} catch (Exception ex) {
			this.logger.error("Exception in adding new member " + emailAddress, ex);
		}

		return null;
	}
	
	
	public String getQueueNotification(){
		String message = null;
		if(QUEUE.size() >0){
			message =  QUEUE.poll();
		}
		return message;
	}
	
	public List<String> getAllNotification() {
		if (!file.exists() || !file.canRead()) {
			return new ArrayList<>();
		}
		List<String> notifications = new ArrayList<>();
		String st;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((st = br.readLine()) != null)
				notifications.add(st);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return notifications;
	}

	protected JsonNode getJSONTreeNodeUsingRestTemplate(Object requestObj, String endPointURL) throws Exception {
		String jsonContent = this.invokeServiceUsingRestTemplate(requestObj, endPointURL);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readValue(jsonContent, JsonNode.class);
		return rootNode;
	}

	protected String invokeServiceUsingRestTemplate(Object requestObj, String endPointURL) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("intuit-signature", "authHeaderValue");
		HttpEntity<?> httpEntity = new HttpEntity(this.getHttpEntity(requestObj),headers);
		logger.info(httpEntity);
		
		ResponseEntity<String> responseEntity = this.restTemplate.exchange(endPointURL, HttpMethod.POST, httpEntity,
				String.class);
		String jsonBodyResponse = responseEntity.getBody();
		HttpStatus httpStatus = responseEntity.getStatusCode();
		HttpHeaders headersResponse = responseEntity.getHeaders();// Future use

		if (httpStatus.is2xxSuccessful()) {
			return jsonBodyResponse;

		} else {
			String errMessage = "RestService returned status is " + httpStatus + " ,and URL is " + endPointURL;
			throw new IOException(errMessage);
		}
	}

	protected HttpEntity<?> getHttpEntity(Object request) throws Exception {
		HttpHeaders headers = this.createHeaders(this.userId, this.apiKey);
		String requestBody = "vaibhav";
		requestBody = this.createRequestBody(request);

		return new HttpEntity(requestBody, headers);
	}

	private String createRequestBody(Object request) throws Exception {
		String jsonStr = Utils.getJSOnStr(request);
		return jsonStr;
	}

	private HttpHeaders createHeaders(final String username, final String password) {
		if (null == this.httpHeaders) {
			this.httpHeaders = new HttpHeaders() {
				{
					String auth = username + ":" + password;
					byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII")));
					String encodedAuth2 = "Basic " + new String(encodedAuth);
					set("Authorization", encodedAuth2);
					set("Content-Type", "application/json");
				}
			};
		}
		return this.httpHeaders;
	}

}
