/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.environment.jsaga;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.IncorrectURLException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.job.Job;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.JobService;
import org.ogf.saga.monitoring.Metric;
import org.ogf.saga.task.State;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.model.execution.ExecutionState;
import org.openmole.core.model.execution.batch.IBatchJobService;


import fr.in2p3.jsaga.adaptor.job.SubState;
import org.openmole.core.implementation.execution.batch.BatchJob;
import org.openmole.plugin.environment.jsaga.internal.Activator;

public class JSAGAJob extends BatchJob {

    static final Pattern pattern = Pattern.compile("\\[(.*)\\]-\\[(.*)\\]");

    final String jobId;
    final JSAGAJobService jobService;

    public JSAGAJob(Job job, JSAGAJobService jobService) throws InternalProcessingError {
        super(jobService);
        this.jobService = jobService;
        try {
            this.jobId = job.getAttribute(Job.JOBID);
        } catch (NotImplementedException ex) {
           throw new InternalProcessingError(ex);
        } catch (AuthenticationFailedException ex) {
           throw new InternalProcessingError(ex);
        } catch (AuthorizationFailedException ex) {
           throw new InternalProcessingError(ex);
        } catch (PermissionDeniedException ex) {
           throw new InternalProcessingError(ex);
        } catch (IncorrectStateException ex) {
           throw new InternalProcessingError(ex);
        } catch (DoesNotExistException ex) {
           throw new InternalProcessingError(ex);
        } catch (TimeoutException ex) {
           throw new InternalProcessingError(ex);
        } catch (NoSuccessException ex) {
           throw new InternalProcessingError(ex);
        }
    }

    public synchronized Job getJob() throws InternalProcessingError {
        
       // URL serviceURL;
        String nativeJobId;
        
        Matcher matcher = pattern.matcher(jobId);

        if (matcher.find()) {
           /* try {
                serviceURL = URLFactory.createURL(matcher.group(1));
            } catch (BadParameterException e) {
                throw new InternalProcessingError(e);
            } catch (NoSuccessException e) {
                throw new InternalProcessingError(e);
            } catch (NotImplementedException e) {
                throw new InternalProcessingError(e);
            }*/
            nativeJobId = matcher.group(2);
        } else {
            throw new InternalProcessingError("Job ID does not match regular expression: " + pattern.pattern());
        }

        //JobService service;
        try {
          //  service = JobFactory.createJobService(Activator.getJSagaSessionService().getSession(), serviceURL);
            return jobService.getJobServiceCache().getJob(nativeJobId);
        } catch (NotImplementedException e) {
            throw new InternalProcessingError(e);
        } catch (AuthenticationFailedException e) {
            throw new InternalProcessingError(e);
        } catch (AuthorizationFailedException e) {
            throw new InternalProcessingError(e);
        } catch (PermissionDeniedException e) {
            throw new InternalProcessingError(e);
        } catch (TimeoutException e) {
            throw new InternalProcessingError(e);
        } catch (NoSuccessException e) {
            throw new InternalProcessingError(e);
        } catch (BadParameterException e) {
            throw new InternalProcessingError(e);
        } catch (DoesNotExistException e) {
            throw new InternalProcessingError(e);
        }
    }

    private ExecutionState translateStatus(Job job, State state) throws InternalProcessingError {

        String subState;

        switch (state) {
            case NEW:
                return ExecutionState.SUBMITED;
            case RUNNING:
                try {
                    subState = job.getMetric(fr.in2p3.jsaga.impl.job.instance.AbstractSyncJobImpl.JOB_SUBSTATE).getAttribute(Metric.VALUE);
                } catch (NotImplementedException e) {
                    throw new InternalProcessingError(e);
                } catch (AuthenticationFailedException e) {
                    throw new InternalProcessingError(e);
                } catch (AuthorizationFailedException e) {
                    throw new InternalProcessingError(e);
                } catch (PermissionDeniedException e) {
                    throw new InternalProcessingError(e);
                } catch (IncorrectStateException e) {
                    throw new InternalProcessingError(e);
                } catch (DoesNotExistException e) {
                    throw new InternalProcessingError(e);
                } catch (TimeoutException e) {
                    throw new InternalProcessingError(e);
                } catch (NoSuccessException e) {
                    throw new InternalProcessingError(e);
                }

                if (!subState.equals(SubState.RUNNING_ACTIVE.toString())) {
                    return ExecutionState.SUBMITED;
                } else {
                    return ExecutionState.RUNNING;
                }
            case DONE:
                return ExecutionState.DONE;
            case FAILED:
            case CANCELED:
            case SUSPENDED:
            default:
                return ExecutionState.FAILED;
        }
    }

    @Override
    public void deleteJob() throws InternalProcessingError {
        try {
            if (getState() == ExecutionState.SUBMITED || getState() == ExecutionState.RUNNING) {
                getJob().cancel();
            }
        } catch (NotImplementedException e) {
            throw new InternalProcessingError(e);
        } catch (IncorrectStateException e) {
            throw new InternalProcessingError(e);
        } catch (TimeoutException e) {
            throw new InternalProcessingError(e);
        } catch (NoSuccessException e) {
            throw new InternalProcessingError(e);
        }

    }

  
    @Override
    public ExecutionState updateState() throws InternalProcessingError {
        try {
            Job job = getJob();
            return translateStatus(job, job.getState());
        } catch (NotImplementedException e) {
            setState(ExecutionState.FAILED);
            throw new InternalProcessingError(e);
        } catch (TimeoutException e) {
            setState(ExecutionState.FAILED);
            throw new InternalProcessingError(e);
        } catch (NoSuccessException e) {
            setState(ExecutionState.FAILED);
            throw new InternalProcessingError(e);
        }

    }

    @Override
    public String toString() {
        return jobId;
    }

}
