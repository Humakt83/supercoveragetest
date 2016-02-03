package fi.ukkosnetti.coverage.strategy;

public final class DefaultCoverageStrategy implements CoverageStrategy {

	@Override
	public int getDepthForConstructorInjection() {
		return 5;
	}

	@Override
	public int getDepthToScanFilesFromFolders() {
		return 15;
	}

	@Override
	public boolean isPrintingOutEnabled() {
		return true;
	}

}
