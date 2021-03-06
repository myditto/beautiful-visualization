package es.tid.haewoon.cdr.filter;

import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.FRIDAY;
import static java.util.Calendar.MONDAY;
import static java.util.Calendar.SUNDAY;
import static java.util.Calendar.THURSDAY;
import static java.util.Calendar.TUESDAY;
import static java.util.Calendar.WEDNESDAY;

import java.util.Calendar;

import es.tid.haewoon.cdr.util.CDR;

public class WeekdayFilter implements CDRFilter {

    @Override
    public boolean filter(CDR cdr) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(cdr.getDatetime());
        
        int day_of_week = calendar.get(DAY_OF_WEEK);
        
        // monday to thursday
        return (day_of_week == MONDAY || day_of_week == TUESDAY || day_of_week == WEDNESDAY || 
                day_of_week == THURSDAY); 
    }

}
