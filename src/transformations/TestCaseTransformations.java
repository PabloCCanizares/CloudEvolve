package transformations;

import dataParser.TestCaseInput;
import dataParser.cloud.ECloudSimulator;
import mutation.MutableCloud.MutableCloud;

//TODO: Incluir
public interface TestCaseTransformations {

	MutableCloud transformTestCase2Cloud(TestCaseInput tcInput);
	TestCaseInput transformCloud2Testcase(ECloudSimulator ePlatformInfo, TestCaseInput tcInputNew, MutableCloud mutCloud);
}
