import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;

public class Client implements Runnable {
	private static final int THREADS_NUM = 100;
	private static final String HOST = "http://flask3.qst6ftqmmz.us-west-2.elasticbeanstalk.com";
	private static final String TOKEN = "/";
	private static final String FOLDER_PATH = "./data/";
	private final static int requests = 1000;
	private static Object lock = new Object();
	private final static List<String> wordList = new ArrayList<String>();
	private final static Map<Integer, Integer> file2WordCount = new HashMap<Integer, Integer>();
	private static String[] urls;
	private Thread thread;
	private boolean isPost;
	private static volatile int threadCount = THREADS_NUM;
	private static volatile int totalQueryCount = 0;
	private static volatile double totalQueryMeanTime = 0;
	private static volatile long totalQueryWorstTime = 0;
	private static volatile double totalPostTime = 0;
	private static volatile int totalPostWord = 0;
	private static volatile int[] totalQueryStats = new int[51];
	
	private int queryCount = 0;
	private double queryTime = 0;
	private long queryWorstTime = 0;
	private int[] queryStats = new int[51];
	private int postWordCount = 0;
	
	
	private int id;
	
	public Client(int id, boolean isPost) {
		this.id = id;
		this.isPost = isPost;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if (isPost) {
			for (int i = 0; i < requests; i++) {
				try {
					sendPost();
					if (i%10 == 0) {
						System.out.println("Complete: " + this.id + " - " + i);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}			
		} else {
			for (int i = 0; i < requests; i++) {
				try {
					sendGet();
					if (i%10 == 0) {
						System.out.println("Complete: " + this.id + " - " + i);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		synchronized (lock) {
			Client.threadCount -= 1;
			System.out.println("Running threads: " + Client.threadCount);
			totalQueryCount += this.queryCount;
			totalQueryMeanTime += this.queryTime;
			if (this.queryWorstTime > totalQueryWorstTime) {
				totalQueryWorstTime = this.queryWorstTime;
			}
			if (this.isPost) {
				totalPostTime += this.queryTime;
				totalPostWord += this.postWordCount;
			}
			for (int i = 0; i < this.queryStats.length; i++) {
				totalQueryStats[i] += this.queryStats[i];
			}
			
			if (Client.threadCount == 0) {
				System.out.println("frequency ~ response: ");
				for (int i = 0; i < 50; i++) {
					System.out.print(Client.totalQueryStats[i] + ", ");
				}
				System.out.println();
				Client.totalQueryMeanTime /= Client.totalQueryCount;
				System.out.println("Mean time: " + Client.totalQueryMeanTime);
				System.out.println("Worst time: " + Client.totalQueryWorstTime);
				
				System.out.println("Total post query time: " + Client.totalPostTime);
				System.out.println("Total post query words: " + Client.totalPostWord);
				Client.totalPostTime /= Client.totalPostWord;
				System.out.println("Time for each words: " + Client.totalPostTime);
			}
		}
	}
	
	public void start () {
		if (thread == null) {
			thread = new Thread (this);
			thread.start ();
		}
	}
	
	public static void loadCounts() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(FOLDER_PATH + "word_count.txt"));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] fields = line.split(" ");
				Client.file2WordCount.put(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//		System.out.println(Client.file2WordCount.size());
	}
	
	public static void loadWords() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(FOLDER_PATH + "index.txt"));
			String line = null;
			while ((line = br.readLine()) != null) {
				wordList.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//		System.out.println(wordList.size());
	}
	
	private static void loadURL() {
		urls = new String[Client.file2WordCount.size()];
		BufferedReader br = null;
		int index = 0;
		try {
			br = new BufferedReader(new FileReader(FOLDER_PATH + "url.txt"));
			String line = null;
			
			while ((line = br.readLine()) != null) {
				Client.urls[index] = line;
				index++;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println(index);		
	}
	
	private void sendGet() throws Exception {
		String url = buildGetUrl();

		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url);
		
		long startTime = System.currentTimeMillis();
		HttpResponse response = client.execute(request);
		long time = System.currentTimeMillis() - startTime;
		
		long indexInStats = (time-1)/20;
		if (indexInStats >= 50) {
			this.queryStats[50]++;
		} else {
			this.queryStats[(int) indexInStats]++;
		}
		
		this.queryCount++;
		this.queryTime += time;
		if (time > this.queryWorstTime) {
			this.queryWorstTime = time;
		}
		

//		System.out.println("\nSending 'GET' request to URL : " + url);
//		System.out.println("Response Code : " + 
//                       response.getStatusLine().getStatusCode());
//
//		BufferedReader rd = new BufferedReader(
//                       new InputStreamReader(response.getEntity().getContent()));
//
//		StringBuffer result = new StringBuffer();
//		String line = "";
//		while ((line = rd.readLine()) != null) {
//			result.append(line);
//		}
//		System.out.println(result.toString());
	}
	
	private String buildGetUrl() {
		StringBuilder builder = new StringBuilder();
		builder.append(HOST);
		builder.append(TOKEN);
		builder.append("Counts");
		builder.append(TOKEN);
		
		Random rand = new Random();
		int wordSize = wordList.size();
		int randomSize = rand.nextInt(10);
		for (int i = 0; i < randomSize; i++) {
			int wordIndex = rand.nextInt(wordSize);
			String word = this.wordList.get(wordIndex);
			if (i != 0) builder.append(",");
			builder.append(word);
		} 
		
		return builder.toString();
	}

	// HTTP POST request
	private void sendPost() throws Exception {
		int[] wordCount = new int[1];
		String url = buildPostUrl(wordCount);
//		System.out.println(url);

		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);
		
		long startTime = System.currentTimeMillis();
		HttpResponse response = client.execute(post);
		long time = System.currentTimeMillis() - startTime;
		
		long indexInStats = (time-1)/20;
		if (indexInStats >= 50) {
			this.queryStats[50]++;
		} else {
			this.queryStats[(int) indexInStats]++;
		}
		
		this.queryCount++;
		this.queryTime += time;
		if (time > this.queryWorstTime) {
			this.queryWorstTime = time;
		}
		this.postWordCount += wordCount[0];
		
//		System.out.println("\nSending 'POST' request to URL : " + url);
//		System.out.println("Post parameters : " + post.getEntity());
//		System.out.println("Response Code : " + 
//                                    response.getStatusLine().getStatusCode());
//
//		BufferedReader rd = new BufferedReader(
//                        new InputStreamReader(response.getEntity().getContent()));
//
//		StringBuffer result = new StringBuffer();
//		String line = "";
//		while ((line = rd.readLine()) != null) {
//			result.append(line);
//		}
//		System.out.println(result.toString());
	}
	
	private String buildPostUrl(int[] wordCount) {
		StringBuilder builder = new StringBuilder();
		builder.append(HOST);
		builder.append(TOKEN);
		builder.append("String");
		builder.append(TOKEN);
		
		Random rand = new Random();
		int index = rand.nextInt(Client.urls.length);
		String content = urls[index];
//		System.out.println(index);
		wordCount[0] = Client.file2WordCount.get(index);
		builder.append(content);
		return builder.toString();
	}
	
	public static void main(String[] args) {
		loadWords();
		loadCounts();
		loadURL();
		System.out.println("Client starts...");
		
		for (int i = 0; i < THREADS_NUM; i++) {
			boolean postThread = false;
			if (i%10 == 0) {
				postThread = true;
			}
			Client client = new Client(i, postThread);
			client.start();
		}
	}
}
