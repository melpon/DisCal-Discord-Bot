package com.cloudcraftgaming.discal.internal.calendar.calendar;

import com.cloudcraftgaming.discal.database.DatabaseManager;
import com.cloudcraftgaming.discal.internal.calendar.CalendarAuth;
import com.cloudcraftgaming.discal.internal.data.CalendarData;
import com.cloudcraftgaming.discal.internal.email.EmailSender;
import com.google.api.services.calendar.model.AclRule;
import com.google.api.services.calendar.model.Calendar;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Nova Fox on 1/4/2017.
 * Website: www.cloudcraftgaming.com
 * For Project: DisCal
 */
public class CalendarCreator {
    private static CalendarCreator instance;

    private ArrayList<PreCalendar> calendars = new ArrayList<>();

    private CalendarCreator() {} //Prevent initialization

    /**
     * Gets the instance of the CalendarCreator.
     * @return The instance of the CalendarCreator.
     */
    public static CalendarCreator getCreator() {
        if (instance == null) {
            instance = new CalendarCreator();
        }
        return instance;
    }

    //Functionals
    /**
     * Initiates the CalendarCreator for the guild involved in the event.
     * @param e The event received upon creation start.
     * @param calendarName The name of the calendar to create.
     * @return The PreCalendar object created.
     */
    public PreCalendar init(MessageReceivedEvent e, String calendarName) {
        if (!hasPreCalendar(e.getMessage().getGuild().getID())) {
            PreCalendar event = new PreCalendar(e.getMessage().getGuild().getID(), calendarName);
            calendars.add(event);
            return event;
        }
        return getPreCalendar(e.getMessage().getGuild().getID());
    }

    /**
     * Gracefully closes down the CalendarCreator for the guild involved and DOES NOT create the calendar.
     * @param e The event received upon termination.
     * @return <codfe>true</codfe> if closed successfully, otherwise <code>false</code>.
     */
    public Boolean terminate(MessageReceivedEvent e) {
        if (hasPreCalendar(e.getMessage().getGuild().getID())) {
            calendars.remove(getPreCalendar(e.getMessage().getGuild().getID()));
            return true;
        }
        return false;
    }

    /**
     * Confirms the calendar and creates it within Google Calendar.
     * @param e The event received upon confirmation.
     * @return A CalendarCreatorResponse Object with detailed info about the confirmation.
     */
    public CalendarCreatorResponse confirmCalendar(MessageReceivedEvent e) {
        if (hasPreCalendar(e.getMessage().getGuild().getID())) {
            String guildId = e.getMessage().getGuild().getID();
            PreCalendar preCalendar = getPreCalendar(guildId);
            if (preCalendar.hasRequiredValues()) {
                Calendar calendar = new Calendar();
                calendar.setSummary(preCalendar.getSummary());
                calendar.setDescription(preCalendar.getDescription());
                calendar.setTimeZone(preCalendar.getTimezone());
                try {
                    Calendar confirmed = CalendarAuth.getCalendarService().calendars().insert(calendar).execute();
                    AclRule rule = new AclRule();
                    AclRule.Scope scope = new AclRule.Scope();
                    scope.setType("default");
                    rule.setScope(scope).setRole("reader");
                    CalendarAuth.getCalendarService().acl().insert(confirmed.getId(), rule).execute();
                    CalendarData calendarData = new CalendarData(guildId, 1);
                    calendarData.setCalendarId(confirmed.getId());
                    calendarData.setCalendarAddress(confirmed.getId());
                    DatabaseManager.getManager().updateCalendar(calendarData);
                    terminate(e);
                    return new CalendarCreatorResponse(true, confirmed);
                } catch (IOException ex) {
                    EmailSender.getSender().sendExceptionEmail(ex, this.getClass());
                    return new CalendarCreatorResponse(false);
                }
            }
        }
        return new CalendarCreatorResponse(false);
    }

    //Getters
    /**
     * Gets the PreCalendar for the guild in the creator.
     * @param guildId The ID of the guild whose PreCalendar is to be returned.
     * @return The PreCalendar belonging to the guild.
     */
    public PreCalendar getPreCalendar(String guildId) {
        for (PreCalendar c : calendars) {
            if (c.getGuildId().equals(guildId)) {
                return c;
            }
        }
        return null;
    }

    //Booleans/Checkers
    /**
     * Checks whether or not the specified Guild has a PreCalendar in the creator.
     * @param guildId The ID of the guild to check for.
     * @return <code>true</code> if a PreCalendar exists, else <code>false</code>.
     */
    public Boolean hasPreCalendar(String guildId) {
        for (PreCalendar c : calendars) {
            if (c.getGuildId().equals(guildId)) {
                return true;
            }
        }
        return false;
    }
}