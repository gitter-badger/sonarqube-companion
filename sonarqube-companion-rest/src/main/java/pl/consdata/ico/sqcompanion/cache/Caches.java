package pl.consdata.ico.sqcompanion.cache;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class Caches {

    public static final String GROUP_OVERVIEW_CACHE = "cache_group_overview";
    public static final String GROUP_DETAILS_CACHE = "cache_group_details";
    public static final String GROUP_VIOLATIONS_HISTORY_CACHE = "cache_group_violations_history";

    public static final List<String> LIST = Arrays.asList(
            GROUP_OVERVIEW_CACHE, GROUP_DETAILS_CACHE, GROUP_VIOLATIONS_HISTORY_CACHE
    );

}
