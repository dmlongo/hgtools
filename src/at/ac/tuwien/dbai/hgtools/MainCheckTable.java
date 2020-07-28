package at.ac.tuwien.dbai.hgtools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class MainCheckTable {

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter table:");
		String in = null;
		String input = "";
		while ((in = sc.nextLine()) != null) {
			System.out.println(in);
			if (in.equalsIgnoreCase(".")) {
				break;
			} else {
				input += in;
			}
		}
		sc.close();
		System.out.println("Input= " + input);
		String[] lines = input.split("\\\\"); // last one is empty
		ArrayList<String> rows = new ArrayList<>(lines.length);
		int n = 0;
		for (String l : lines) {
			if (!l.isEmpty() && !l.matches("\\s")) {
				System.out.println("r" + n++ + "= " + l);
				rows.add(l);
			}
		}
		int[][] table = new int[n][];
		int m = -1;
		for (int i = 0; i < n; i++) {
			String r = rows.get(i);
			// System.out.println("r" + i + ": " + r);
			String[] cols = r.split("&");
			m = cols.length;
			table[i] = new int[m];
			for (int j = 0; j < m; j++) {
				String c = cols[j].trim();
				try {
					table[i][j] = Integer.parseInt(c);
				} catch (Exception e) {
					table[i][j] = 0;
				}
			}
		}

		System.out.println();
		System.out.println("I read:");
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				System.out.print(table[i][j] + " ");
			}
			System.out.println();
		}
		System.out.println("Col sums:");
		int[] sums = new int[m];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				sums[j] += table[i][j];
			}
		}
		System.out.println(Arrays.toString(sums));
		
		System.out.println("Row sums:");
		int[] rSums = new int[n];
		for (int i = 0; i < n; i++) {
			for (int j = 1; j < m; j++) { // first col is index
				rSums[i] += table[i][j];
			}
		}
		System.out.println(Arrays.toString(rSums));
	}

}
