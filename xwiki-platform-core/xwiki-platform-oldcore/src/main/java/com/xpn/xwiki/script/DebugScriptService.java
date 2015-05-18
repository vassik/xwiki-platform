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
package com.xpn.xwiki.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.job.event.status.JobProgress;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.web.XWikiAction;

/**
 * Various internal debug tools.
 *
 * @version $Id$
 * @since 7.1M2
 */
@Component
@Singleton
@Named("debug")
public class DebugScriptService implements ScriptService
{
    @Inject
    private Execution execution;

    /**
     * @return true if detailed progress is available for the current action, false otherwise
     */
    public boolean isEnabled()
    {
        return getActionProgress() != null;
    }

    /**
     * @return the detailed progress of the current action
     */
    public JobProgress getActionProgress()
    {
        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            return (JobProgress) econtext.getProperty(XWikiAction.ACTION_PROGRESS);
        }

        return null;
    }
}