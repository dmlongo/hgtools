package at.ac.tuwien.dbai.hgtools;

import java.time.Duration;
import java.time.Instant;

public class Main {

	static final String STATS = "-stats";
	static final String CONVERT = "-convert";
	static final String EXTRACT = "-extract";
	static final String TRANSLATE = "-translate";

	static final String SQL = "-sql";
	static final String XCSP = "-xcsp";
	static final String HG = "-hg";

	static final String H2P = "-hg2pace";

	public static void main(String[] args) throws Exception {
		String action = args[0];
		String type = args[1];
		// Instant start = Instant.now();
		if (action.equals(STATS)) {
			if (!type.equals(SQL) && !type.equals(XCSP) && !type.equals(HG)) {
				throw new UnsupportedCommandException(type);
			}
			MainStats.main(type, args, 2);
		} else if (action.equals(CONVERT)) {
			if (!type.equals(SQL) && !type.equals(XCSP)) {
				throw new UnsupportedCommandException(type);
			}
			Instant start = Instant.now();
			MainConvert.main(type, args, 2);
			Instant finish = Instant.now();
			System.out.println("time= " + Duration.between(start, finish).toMillis() + "ms");
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
		// Instant finish = Instant.now();
		// System.out.println(Duration.between(start, finish).toMillis());
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
