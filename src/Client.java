import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class Client implements Runnable {
	private static final int THREADS_NUM = 100;
	private static final String HOST = "http://flask3.qst6ftqmmz.us-west-2.elasticbeanstalk.com";
	private static final String TOKEN = "/";
	private static final String FOLDER_PATH = "/Users/zhao/Documents/workspace/WebClient/data/";
	private final static int requests = 1000;
	private static Object lock = new Object();
	private final static List<String> wordList = new ArrayList<String>();
	private final static Map<Integer, Integer> file2WordCount = new HashMap<Integer, Integer>();
	private static String[] urls;
	private static int threadCount = THREADS_NUM;
	private static int totalQueryCount = 0;
	private static double totalQueryMeanTime = 0;
	private static long totalQueryWorstTime = 0;
	private static double totalPostTime = 0;
	private static int totalPostWord = 0;
	private static List<Integer> totalTimes = new ArrayList<Integer>();
	private static PoolingHttpClientConnectionManager connManager;
	
	private int id;
	private boolean isPost;
	private CloseableHttpClient client;
	private Thread thread;
	
	private int queryCount = 0;
	private double queryTime = 0;
	private long queryWorstTime = 0;
	private int postWordCount = 0;
	private List<Integer> times = new ArrayList<Integer>();
	
	public Client(int id, boolean isPost) {
		this.id = id;
		this.isPost = isPost;
		this.client = HttpClients.custom().setConnectionManager(connManager).build();
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
		
		try {
			this.client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		writeToFile();
		
		synchronized (lock) {
			Client.threadCount -= 1;
			totalQueryCount += this.queryCount;
			totalQueryMeanTime += this.queryTime;
			if (this.queryWorstTime > totalQueryWorstTime) {
				totalQueryWorstTime = this.queryWorstTime;
			}
			if (this.isPost) {
				totalPostTime += this.queryTime;
				totalPostWord += this.postWordCount;
			}
			
			totalTimes.addAll(times);
			System.out.println("Complete thread " + this.id + " Running threads: " + Client.threadCount);
			
			if (Client.threadCount == 0) {
//				System.out.println(totalTimes);
				Client.totalQueryMeanTime /= Client.totalQueryCount;
				System.out.println("Mean time: " + Client.totalQueryMeanTime);
				System.out.println("Worst time: " + Client.totalQueryWorstTime);
				
				System.out.println("Total post query time: " + Client.totalPostTime);
				System.out.println("Total post query words: " + Client.totalPostWord);
				Client.totalPostTime /= Client.totalPostWord;
				System.out.println("Time for each words: " + Client.totalPostTime);
				writeResults();
				connManager.close();
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
	
	private void writeResults() {
		File file = new File(FOLDER_PATH);
		FileWriter writer = null;
		try {
			writer = new FileWriter(file.getParent() + "/results/times.txt");
			for (int time: totalTimes) {
				writer.write(time + "\n");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			writer = new FileWriter(file.getParent() + "/results/stats.txt");
			writer.write("Mean time: " + Client.totalQueryMeanTime + "ms\n");
			writer.write("Worst time: " + Client.totalQueryWorstTime + "ms\n");
			writer.write("Total post query words: " + Client.totalPostWord + "\n");
			writer.write("Time for each words: " + Client.totalPostTime + "ms\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	private void writeToFile() {
		File file = new File(FOLDER_PATH);
		FileWriter writer = null;
		try {
			writer = new FileWriter(file.getParent() + "/results/" + this.id + ".txt");
			writer.write("Times");
			for (int time: this.times) {
				writer.write(" " + time);
			}
			writer.write("\n");
			writer.write("queryTime " + this.queryTime + "\n");
			writer.write("queryCount " + this.queryCount + "\n");
			writer.write("worstTime " + this.queryWorstTime + "\n");
			if (this.isPost) {
				writer.write("postTime " + this.queryTime + "\n");
				writer.write("postWord " + this.postWordCount + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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

		HttpGet request = new HttpGet(url);
		
		long startTime = System.currentTimeMillis();
		CloseableHttpResponse response = client.execute(request);
		long time = System.currentTimeMillis() - startTime;
		
		HttpEntity entity = response.getEntity();
	    EntityUtils.consume(entity);
	    response.close();
		
		
		if (time >= 1020) {
			times.add(1010);
		} else {
			times.add((int) time);
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
			String word = Client.wordList.get(wordIndex);
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

		HttpPost post = new HttpPost(url);
		
		long startTime = System.currentTimeMillis();
		CloseableHttpResponse response = client.execute(post);
		long time = System.currentTimeMillis() - startTime;
		
		HttpEntity entity = response.getEntity();
	    EntityUtils.consume(entity);
	    response.close();
		
		if (time >= 1020) {
			times.add(1010);
		} else {
			times.add((int) time);
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
		Client.connManager = new PoolingHttpClientConnectionManager();
		connManager.setMaxTotal(THREADS_NUM);
		HttpHost localhost = new HttpHost("locahost", 80);
		connManager.setMaxPerRoute(new HttpRoute(localhost), THREADS_NUM);
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
