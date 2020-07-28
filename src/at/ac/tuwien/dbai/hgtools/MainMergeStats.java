package at.ac.tuwien.dbai.hgtools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MainMergeStats {

	private static String statsFile;
	private static String widthFile;

	public static void main(String[] args) throws IOException {
		statsFile = args[0]; // name,vertices,edges,arity,degree,bip,bip3,bip4,vc
		widthFile = args[1]; // name,hw

		Map<String, String> statsMap = loadMap(statsFile);
		Map<String, String> widthMap = loadMap(widthFile);

		System.out.println("name,vertices,edges,arity,degree,bip,bip3,bip4,vc,uhw");
		for (String name : statsMap.keySet()) {
			String stats = statsMap.get(name);
			String width = widthMap.get(name);
			System.out.println(name + "," + stats + "," + width);
		}
	}

	private static Map<String, String> loadMap(String file) {
		Map<String, String> map = new TreeMap<String, String>();
		try (Stream<String> stream = Files.lines(Paths.get(file))) {
			stream.forEach(new Consumer<String>() {
				@Override
				public void accept(String s) {
					int idx = s.indexOf(',');
					String name = s.substring(0, idx);
					String rest = s.substring(idx + 1);
					map.put(name, rest);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

}