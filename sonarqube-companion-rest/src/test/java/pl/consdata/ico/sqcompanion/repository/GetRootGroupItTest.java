package pl.consdata.ico.sqcompanion.repository;

import org.junit.Test;
import pl.consdata.ico.sqcompanion.BaseItTest;
import pl.consdata.ico.sqcompanion.TestAppConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author gregorry
 */
public class GetRootGroupItTest extends BaseItTest {

	@Test
	public void shouldLoadRootGroup() {
		// when
		final Group rootGroup = repositoryService.getRootGroup();

		// then
		assertThat(rootGroup).isNotNull();
		assertThat(rootGroup.getProjects()).isNotEmpty();
		assertThat(rootGroup.getProjects().get(0).getKey()).isEqualTo(TestAppConfig.RootGroup.Project1.KEY);
	}

}
