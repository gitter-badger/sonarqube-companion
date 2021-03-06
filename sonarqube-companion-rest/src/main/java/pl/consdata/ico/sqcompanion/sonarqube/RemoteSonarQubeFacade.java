package pl.consdata.ico.sqcompanion.sonarqube;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.consdata.ico.sqcompanion.sonarqube.sqapi.SQComponent;
import pl.consdata.ico.sqcompanion.sonarqube.sqapi.SQComponentSearchResponse;
import pl.consdata.ico.sqcompanion.sonarqube.sqapi.SQIssue;
import pl.consdata.ico.sqcompanion.sonarqube.sqapi.SQIssuesSearchResponse;
import pl.consdata.ico.sqcompanion.sonarqube.sqapi.SQMeasure;
import pl.consdata.ico.sqcompanion.sonarqube.sqapi.SQMeasureHistory;
import pl.consdata.ico.sqcompanion.sonarqube.sqapi.SQMeasuresSearchHistoryResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author gregorry
 */
@Service
@Slf4j
public class RemoteSonarQubeFacade implements SonarQubeFacade {

    public static final String ALL_VIOLATION_METRICS = "blocker_violations,critical_violations,major_violations,minor_violations,info_violations";
    private final SonarQubeConnector sonarQubeConnector;

    public RemoteSonarQubeFacade(final SonarQubeConnector sonarQubeConnector) {
        this.sonarQubeConnector = sonarQubeConnector;
    }

    @Override
    public List<SonarQubeProject> getProjects(final String serverId) {
        return sonarQubeConnector.getForPaginatedList(
                serverId,
                "api/components/search_projects",
                SQComponentSearchResponse.class,
                SQComponentSearchResponse::getComponents
        ).map(this::mapComponentToProject
        ).collect(Collectors.toList());
    }

    @Override
    public List<SonarQubeIssue> getIssues(final String serverId, final String projectKey) {
        return sonarQubeConnector.getForPaginatedList(
                serverId,
                "api/issues/search?projectKeys=" + projectKey,
                SQIssuesSearchResponse.class,
                SQIssuesSearchResponse::getIssues
        ).map(this::mapSqIssueToIssue
        ).collect(Collectors.toList());
    }

    @Override
    public List<SonarQubeMeasure> getProjectMeasureHistory(final String serverId, final String projectKey, final LocalDate fromDate) {
        final StringBuilder serviceUri = new StringBuilder("api/measures/search_history")
                .append("?component=" + projectKey)
                .append("&metrics=" + ALL_VIOLATION_METRICS);
        if (fromDate != null) {
            serviceUri.append("&from=" + fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }

        final List<SQMeasure> measures = sonarQubeConnector.getForPaginatedList(
                serverId,
                serviceUri.toString(),
                SQMeasuresSearchHistoryResponse.class,
                SQMeasuresSearchHistoryResponse::getMeasures
        ).collect(Collectors.toList());

        final Map<Date, SonarQubeMeasure> measureDateIndex = new HashMap<>();
        for (final SQMeasure measure : measures) {
            for (final SQMeasureHistory historyEntry : measure.getHistory()) {
                measureDateIndex.putIfAbsent(
                        historyEntry.getDate(),
                        SonarQubeMeasure.builder().date(historyEntry.getDate()).build()
                );
                final SonarQubeMeasure dateMeasure = measureDateIndex.get(historyEntry.getDate());
                dateMeasure.putMetric(measure.getMetric(), historyEntry.getValue());
            }
        }

        return measureDateIndex
                .values()
                .stream()
                .sorted(Comparator.comparing(SonarQubeMeasure::getDate))
                .collect(Collectors.toList());
    }

    private SonarQubeProject mapComponentToProject(final SQComponent component) {
        return SonarQubeProject
                .builder()
                .key(component.getKey())
                .name(component.getName())
                .build();
    }

    private SonarQubeIssue mapSqIssueToIssue(final SQIssue sqIssue) {
        return SonarQubeIssue
                .builder()
                .key(sqIssue.getKey())
                .creationDate(sqIssue.getCreationDate())
                .message(sqIssue.getMessage())
                .build();
    }

}
