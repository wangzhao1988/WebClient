import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

public class Process {
	private static final String folderPath = "./data/";
	private static final String ls = "%20";
	private static final int maxLength = 1800;
	// map[file name] = word count
	private static Map<Integer, Integer> fileIndex2WordCount = new HashMap<Integer, Integer>();
	private static List<String> urlList = new ArrayList<String>();
	
	public void processData() {
		File file = new File(folderPath + "articles");
		BufferedReader br = null;
		DigestUtils du = new DigestUtils();
		
		for (File data: file.listFiles()) {
			int wordCount = 0;
			
			StringBuilder builder = new StringBuilder();
			try {
				br = new BufferedReader(new FileReader(data));
				String line = null;
				while ((line = br.readLine()) != null) {
					String[] words = process_line(line);
					for (int i = 0; i < words.length; i++) {
						if (!isWord(words[i])) {
							continue;
						}
						if (builder.length() + words[i].length() > maxLength) {
							if (wordCount > 10) {
								int index = urlList.size();
								Process.fileIndex2WordCount.put(index, wordCount);
								urlList.add(builder.toString());
							}
							builder = new StringBuilder();
							wordCount = 0;
						} else {
							wordCount++;
							builder.append(words[i]);
							builder.append(ls);
							if (Math.random() < 0.01) {
								String md5 = du.md5Hex(du.md5(line));
								if (builder.length() + md5.length() + 1 <= maxLength) {
									builder.append(md5);
									builder.append(ls);
								}
							}							
						}
					}
					
					if (builder.length() != 0) {
						if (wordCount > 10) {
							int index = urlList.size();
							Process.fileIndex2WordCount.put(index, wordCount);
							urlList.add(builder.toString());
						}
						builder = new StringBuilder();
						wordCount = 0;
					}
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
		}
	}
	
	private void writeURL() {
		FileWriter writer = null;
		try {
			writer = new FileWriter(folderPath + "url.txt");
			for (int i = 0; i < urlList.size(); i++) {
//				System.out.println(urlList.get(i));
				writer.write(urlList.get(i) + "\n");
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
		
		
	}

	public void processIndex() {
		BufferedReader br = null;
		List<String> words = new ArrayList<String>();
		try {
			br = new BufferedReader(new FileReader(folderPath + "articles.train.txt"));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] fields = line.split(" ");
				if (isWord(fields[0])) {
					words.add(fields[0]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();;
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
//		System.out.println(words.size());
		
		FileWriter writer = null; 
		try {
			writer = new FileWriter(folderPath + "index.txt");
			for (String word: words) {
				writer.write(word + "\n");
			}
		} catch (Exception e) {
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
	
	public void writeCount() {
		FileWriter writer = null;
		try {
			writer = new FileWriter(folderPath + "word_count.txt");
			for (int index: fileIndex2WordCount.keySet()) {
				writer.write(index + " " + fileIndex2WordCount.get(index) + "\n");
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
		
	}

	private boolean isWord(String str) {
		char[] charArray = str.toCharArray();
		for (char c: charArray) {
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}
	
	public String[] process_line(String line) {
        String temp = line.replaceAll("\\.\\s+", " ");
        temp = temp.replaceAll(",\\s+", " ");
        return temp.toLowerCase().split("\\s+");
    }
	
	public static void main(String[] args) {
		Process p = new Process();
		p.processData();
		p.writeCount();
		p.writeURL();
//		System.out.println(fileIndex2WordCount.size());
	}
}
