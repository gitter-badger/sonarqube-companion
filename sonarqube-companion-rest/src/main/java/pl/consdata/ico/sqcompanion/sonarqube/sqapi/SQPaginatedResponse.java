package pl.consdata.ico.sqcompanion.sonarqube.sqapi;

import lombok.Data;

/**
 * @author gregorry
 */
@Data
public abstract class SQPaginatedResponse {

	private SQPaging paging;

}
