package pl.consdata.ico.sqcompanion.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.consdata.ico.sqcompanion.SQCompanionException;
import pl.consdata.ico.sqcompanion.config.AppConfig;
import pl.consdata.ico.sqcompanion.config.GroupDefinition;
import pl.consdata.ico.sqcompanion.config.ProjectLink;
import pl.consdata.ico.sqcompanion.config.ServerDefinition;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Możliwości rozwoju:
 * - odświeżanie automatyczne przez cron
 *
 * @author gregorry
 */
@Slf4j
@Service
public class RepositoryService {

    private final AppConfig appConfig;
    private final ProjectLinkResolverFactory projectLinkResolverFactory;
    private Group rootGroup;

    public RepositoryService(final AppConfig appConfig,
                             final ProjectLinkResolverFactory projectLinkResolverFactory) {
        this.appConfig = appConfig;
        this.projectLinkResolverFactory = projectLinkResolverFactory;
        syncGroups();
    }

    public void syncGroups() {
        final GroupDefinition rootGroupConfig = appConfig.getRootGroup();
        if (Objects.nonNull(rootGroupConfig)) {
            this.rootGroup = buildGroup(rootGroupConfig);
        } else {
            log.info("Root group not synced due to empty configuration");
        }
    }

    public Group getRootGroup() {
        return rootGroup;
    }

    public Optional<Group> getGroup(final String uuid) {
        return rootGroup
                .getAllGroups()
                .stream()
                .filter(g -> g.getUuid().equals(uuid))
                .findFirst();
    }

    private Group buildGroup(final GroupDefinition group) {
        try {
            final List<Group> subGroups = group.getGroups().stream().map(this::buildGroup).collect(Collectors.toList());
            final List<Project> projects = group.getProjectLinks()
                    .stream()
                    .flatMap(this::linkProjects)
                    .collect(Collectors.toList());
            return Group.builder()
                    .uuid(group.getUuid())
                    .name(group.getName())
                    .groups(subGroups)
                    .projects(projects)
                    .build();
        } catch (final Exception exception) {
            log.error("Can't sync group details [group={}]", group, exception);
            return Group.builder().build();
        }
    }

    private Stream<Project> linkProjects(final ProjectLink projectLink) {
        try {
            return projectLinkResolverFactory
                    .getResolver(projectLink.getType())
                    .resolveProjectLink(projectLink)
                    .stream()
                    .map(project -> project.withUrl(getProjectUrl(project.getKey(), project.getServerId())));
        } catch (final Exception exception) {
            log.error("Can't resolve project link [projectLink={}]", projectLink, exception);
            return Stream.empty();
        }
    }

    private String getProjectUrl(final String projectKey, final String serverId) {
        final ServerDefinition server = getServerDefinition(serverId);
        return String.format("%sdashboard?id=%s", server.getUrl(), encodeUrl(projectKey));
    }

    private String encodeUrl(String projectKey) {
        try {
            return URLEncoder.encode(projectKey, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new SQCompanionException(e.getMessage(), e);
        }
    }

    private ServerDefinition getServerDefinition(final String serverId) {
        return appConfig.getServer(serverId);
    }

}
