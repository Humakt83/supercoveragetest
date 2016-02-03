package fi.ukkosnetti.coverage.strategy;

public interface CoverageStrategy {
	
	int getDepthForConstructorInjection();
	
	int getDepthToScanFilesFromFolders();
	
	boolean isPrintingOutEnabled();
	
	default void printOut(String line) {
		if (isPrintingOutEnabled()) {
			System.out.println(line);
		}
	}

}
