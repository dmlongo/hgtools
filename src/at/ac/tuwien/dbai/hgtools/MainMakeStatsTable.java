package at.ac.tuwien.dbai.hgtools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MainMakeStatsTable {

	public static void main(String[] args) {
		String statsFile = args[0]; // name,vertices,edges,arity,degree,bip,bip3,bip4,vc

		Map<String, String> statsMap = loadMap(statsFile);

		final int ROWS = 7;
		final int COLS = 6;
		int[][] tab = new int[ROWS][COLS];
		for (int i = 0; i < tab.length; i++) {
			tab[i][0] = i;
		}
		final int DEG = 3, BIP = 4, BIP3 = 5, BIP4 = 6, VC = 7;
		for (String name : statsMap.keySet()) {
			if (!name.equalsIgnoreCase("name")) {
				String stats = statsMap.get(name);
				String[] tokens = stats.split(",");
				//System.out.println("Original: " + Arrays.toString(tokens));
				if (tokens.length != 8) {
					System.out.println("line= " + name + "," + stats);
				}
				String[] tks = { tokens[DEG], tokens[BIP], tokens[BIP3], tokens[BIP4], tokens[VC] };
				if (Integer.parseInt(tokens[BIP3]) == 3) {System.out.println(name);}
				//System.out.println("Mod: " + Arrays.toString(tks));
				for (int j = 0; j < tks.length; j++) {
					int val = Integer.parseInt(tks[j]);
					val = (val < ROWS) ? val : ROWS - 1;
					tab[val][j + 1]++;
				}
				//System.out.println(Arrays.deepToString(tab));
			}
		}

		for (int i = 0; i < tab.length; i++) {
			for (int j = 0; j < tab[0].length; j++) {
				System.out.print(tab[i][j] + "\t");
				if (j < tab[0].length - 1) {
					System.out.print("&\t");
				}
			}
			System.out.println("\\\\");
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
