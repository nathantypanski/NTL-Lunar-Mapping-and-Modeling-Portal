package com.topcoder.nasa.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.topcoder.nasa.file.S3FileUrlCreator;
import com.topcoder.nasa.job.LmmpJob;
import com.topcoder.nasa.job.LmmpJob.Status;
import com.topcoder.nasa.job.LmmpJobRepository;

@Component
@Path("/status")
@Transactional
public class StatusResource {
    private static final Logger LOG = LoggerFactory.getLogger(StatusResource.class);

    @Autowired
    private LmmpJobRepository jobRepository;

    @Autowired
    private S3FileUrlCreator urlCreator;

    @GET
    @Produces("text/plain")
    public Response nop() {
        return Response.status(404).entity("Please use resource /status/{uuid} to poll status")
                .build();
    }

    @GET
    @Path("/{uuid}")
    @Produces("application/json")
    public StatusResponse generate(@PathParam("uuid") String uuid) {
        LOG.info("Proessing call to /status/{} resource", uuid);

        LmmpJob job = jobRepository.load(uuid);
        StatusResponse lmmpJobStatus = new StatusResponse();

        if (job == null) {
            lmmpJobStatus.setStatus("not_found");
            lmmpJobStatus.setReason("Unknown UUID!");
            Response the404 = Response.status(404).entity(lmmpJobStatus).build();
            throw new WebApplicationException(the404);
        }

        lmmpJobStatus.setStatus(job.getStatus().displayName());
        lmmpJobStatus.setReason(job.getFailInfo());

        if (job.getStatus().equals(Status.COMPLETED)) {
            lmmpJobStatus.setLink(urlCreator.generateUrlFor(job));
        }

        return lmmpJobStatus;
    }
}
