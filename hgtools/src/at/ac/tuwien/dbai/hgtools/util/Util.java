package at.ac.tuwien.dbai.hgtools.util;

public class Util {

	/** Removes angular brackets from vertex names 
	 * 
	 * @param s A string s to be stringified
	 * @return  A stringified String
	 */
	public static String stringify(String s) {
		String newS = new String(s);
		
		newS = newS.replace('[', 'L');
		newS = newS.replace(']', 'J');
		
		return newS;
	}

}
