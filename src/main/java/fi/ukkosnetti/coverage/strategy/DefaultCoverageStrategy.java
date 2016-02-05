package fi.ukkosnetti.coverage.strategy;

public final class DefaultCoverageStrategy implements CoverageStrategy {

	@Override
	public int getDepthToScanFilesFromFolders() {
		return 15;
	}

	@Override
	public boolean isPrintingOutEnabled() {
		return true;
	}

	@Override
	public boolean isThrowablePrintingOutEnabled() {
		return true;
	}

	@Override
	public long getTimeoutForMethodExecution() {
		return 500;
	}
	
}
