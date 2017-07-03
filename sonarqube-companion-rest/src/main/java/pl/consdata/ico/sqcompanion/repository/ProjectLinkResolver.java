package pl.consdata.ico.sqcompanion.repository;

import pl.consdata.ico.sqcompanion.config.ProjectLink;

import java.util.List;

/**
 * @author gregorry
 */
public interface ProjectLinkResolver {

	List<Project> resolveProjectLink(final ProjectLink projectLink);

}
