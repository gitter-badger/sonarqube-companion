package net.lipecki.sqcompanion.repository;

import net.lipecki.sqcompanion.sonarqube.SonarQubeService;
import net.lipecki.sqcompanion.sonarqube.issue.SonarQubeIssuesIssueResultDto;
import net.lipecki.sqcompanion.sonarqube.timemachine.SonarQubeTimeMachineResultCellsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static java.lang.Integer.parseInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Provides actual groups with projects data.
 * <p>
 * Abstraction layer over remote systems.
 * </p>
 */
public class RepositoryService {

    public static final int IDX_BLOCKER = 0;
    public static final int IDX_CRITICAL = 1;
    public static final int IDX_MAJOR = 2;
    public static final int IDX_MINOR = 3;
    public static final int IDX_INFO = 4;
    private final LayoutProvider layoutProvider;
    private final SonarQubeService sonarQubeService;
    private RepositoryModel repositoryModel;

    public RepositoryService(final LayoutProvider layoutProvider, final SonarQubeService sonarQubeService) {
        this.layoutProvider = layoutProvider;
        this.sonarQubeService = sonarQubeService;
    }

    public void loadData() {
        final Long startTime = System.currentTimeMillis();

        final Map<String, Project> loadedProjects = layoutProvider.getProjects()
                .stream()
                .map(config -> loadProjectData(config))
                .collect(toMap(Project::getKey, identity()));

        final Map<String, Group> loadedGroups = layoutProvider.getGroups()
                .stream()
                .map(config -> loadGroupData(config, loadedProjects))
                .collect(toMap(Group::getKey, identity()));

        final Long endTime = System.currentTimeMillis();
        LOGGER.info("Repository layout loaded [time={}]", (endTime - startTime));

        setRepositoryModel(RepositoryModel.of(loadedGroups, loadedProjects));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryService.class);

    private void setRepositoryModel(final RepositoryModel repositoryModel) {
        this.repositoryModel = repositoryModel;
    }

    public RepositoryModel getRepositoryModel() {
        return repositoryModel;
    }

    private Group loadGroupData(final GroupConfiguration config, final Map<String, Project> projects) {
        final Group group = new Group(config.getKey(), config.getName(), "");

        group.setProjects(config.getProjects()
                .stream()
                .map(key -> projects.get(key))
                .collect(toList()));
        group.setIssues(Issues.of(group.getProjects()
                .stream()
                .map(project -> project.getIssues().getAll())
                .collect(ArrayList::new, List::addAll, List::addAll)));
        group.setHistory(History.of(group.getProjects()
                .stream()
                .map(project -> project.getHistory().getHistoryPoints().values())
                .collect(ArrayList<HistoryPoint>::new, List::addAll, List::addAll).stream()
                .collect(
                        toMap(
                                HistoryPoint::getDate,
                                identity(),
                                HistoryPoint::sum
                        )
                )));

        return group;
    }

    private Project loadProjectData(final ProjectConfiguration config) {
        final Project project = new Project(config.getKey(), config.getKey(), config.getSonarqubeKey(), "");

        project.setIssues(
                Issues.of(
                        project,
                        Arrays.stream(sonarQubeService
                                .getIssues(project.getSonarqubeKey())
                                .getIssues())
                                .map(issue -> issueAsModel(issue))
                                .collect(toList())));
        project.setHistory(
                History.of(
                        Arrays.stream(sonarQubeService
                                .getHistory(project.getSonarqubeKey())
                                .getCells())
                                .sorted((cell1, cell2) -> cell1.getD().compareTo(cell2.getD()))
                                .map(cell -> historyPointAsModel(cell))
                                .collect(
                                        toMap(
                                                HistoryPoint::getDate,
                                                identity(),
                                                (a, b) -> b
                                        )
                                )));

        return project;
    }


    private HistoryPoint historyPointAsModel(final SonarQubeTimeMachineResultCellsDto cell) {
        final HistoryPoint historyPoint = new HistoryPoint(convertDateToLocalDate(cell.getD()));

        historyPoint.addBlockers(parseInt(cell.getV()[IDX_BLOCKER]));
        historyPoint.addCriticals(parseInt(cell.getV()[IDX_CRITICAL]));
        historyPoint.addMajors(parseInt(cell.getV()[IDX_MAJOR]));
        historyPoint.addMinors(parseInt(cell.getV()[IDX_MINOR]));
        historyPoint.addInfos(parseInt(cell.getV()[IDX_INFO]));

        return historyPoint;
    }

    private Issue issueAsModel(final SonarQubeIssuesIssueResultDto dto) {
        return new Issue(dto.getKey(), dto.getComponent(), dto.getMessage(), IssueSeverity.parse(dto.getSeverity()),
                dto.getCreationDate());
    }

    private LocalDate convertDateToLocalDate(final Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }


}