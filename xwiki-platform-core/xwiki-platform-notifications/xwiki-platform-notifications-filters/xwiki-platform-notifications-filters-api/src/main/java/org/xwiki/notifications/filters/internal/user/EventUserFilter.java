/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.notifications.filters.internal.user;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.preferences.NotificationPreference;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.not;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Handle a black list of user not to watch.
 *
 * @version $Id$
 * @since 9.10RC1
 */
@Component
@Singleton
@Named(EventUserFilter.FILTER_NAME)
public class EventUserFilter implements NotificationFilter
{
    /**
     * Name of the filter.
     */
    public static final String FILTER_NAME = "eventUserNotificationFilter";

    @Inject
    private EventUserFilterPreferencesGetter preferencesGetter;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public boolean filterEvent(Event event, DocumentReference user, NotificationFormat format)
    {
        return preferencesGetter.isUserExcluded(serializer.serialize(event.getUser()), user, format);
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        // As the filter is applied globally, it’s not bound to any preference
        return false;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user, NotificationPreference preference)
    {
        // We don't handle this use-case
        return null;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user, NotificationFilterType type,
            NotificationFormat format)
    {
        if (type == NotificationFilterType.EXCLUSIVE) {
            Collection<String> users = preferencesGetter.getExcludedUsers(user, format);
            if (!users.isEmpty()) {
                return not(value(EventProperty.USER).inStrings(users));
            }
        }

        return null;
    }

    @Override
    public String getName()
    {
        return FILTER_NAME;
    }
}
