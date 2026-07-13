.PHONY: testAll testUnit testIntegration testReport testReportUnit testReportIntegration

## Run all tests — live PASSED/FAILED lines + summary
testAll:
	./scripts/test-all

## Run unit tests only
testUnit:
	./scripts/test-unit

## Run integration / flow tests only
testIntegration:
	./scripts/test-integration

## Summary of last full test run
testReport:
	./scripts/test-report test

## Summary of last unit test run
testReportUnit:
	./scripts/test-report unitTest

## Summary of last integration test run
testReportIntegration:
	./scripts/test-report integrationTest
