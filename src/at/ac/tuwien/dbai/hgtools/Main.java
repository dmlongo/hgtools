package at.ac.tuwien.dbai.hgtools;

public class Main {

	static final String STATS = "-stats";
	static final String CONVERT = "-convert";
	static final String EXTRACT = "-extract";
	static final String TRANSLATE = "-translate";

	static final String SQL = "-sql";
	static final String XCSP = "-xcsp";
	static final String HG = "-hg";

	static final String H2P = "-hg2pace";

	static boolean verbose = false; // TODO add flag to change this

	public static void main(String[] args) throws Exception {
		String action = args[0];
		String type = args[1];
		if (action.equals(STATS)) {
			if (!type.equals(SQL) && !type.equals(XCSP) && !type.equals(HG)) {
				throw new UnsupportedCommandException(type);
			}
			MainStats.main(type, args, 2);
		} else if (action.equals(CONVERT)) {
			if (!type.equals(SQL) && !type.equals(XCSP)) {
				throw new UnsupportedCommandException(type);
			}
			Converter.convert(type, args, 2);
		} else if (action.equals(EXTRACT)) {
			if (!type.equals(SQL)) {
				throw new UnsupportedCommandException(type);
			}
			MainExtract.main(type, args, 2);
		} else if (action.equals(TRANSLATE)) {
			if (!type.equals(H2P)) {
				throw new UnsupportedCommandException(type);
			}
			MainTranslate.main(type, args, 2);
		} else {
			throw new UnsupportedCommandException(action);
		}
	}

	static class UnsupportedCommandException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public UnsupportedCommandException() {
			super();
		}

		public UnsupportedCommandException(String string) {
			super("Unkown command: " + string);
		}
	}

}
