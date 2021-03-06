package fi.ukkosnetti.coverage.strategy;

public interface CoverageStrategy {
	
	int getDepthToScanFilesFromFolders();
	
	long getTimeoutForMethodExecution();
	
	boolean isPrintingOutEnabled();
	
	boolean isThrowablePrintingOutEnabled();
	
	default void printOut(String line) {
		if (isPrintingOutEnabled()) {
			System.out.println(line);
		}
	}
	
	default void printOut(Throwable throwable) {
		if (isThrowablePrintingOutEnabled()) {
			System.out.println(throwable);
		}
	}

}
