package com.topcoder.nasa.rest;

import gov.nasa.pds.entities.SearchCriteria;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.topcoder.nasa.job.LmmpJob;
import com.topcoder.nasa.job.LmmpJobRepository;
import com.topcoder.nasa.job.LmmpJobWorkflow;

/**
 * The /generate resource. Responsible for create a new {@link LmmpJob} - and associated activities.
 * <p/>
 * Note that the GET method is provided as a convenient reminder that clients need to <b>POST</b>
 * these requests.
 *
 */
@Component
@Path("/generate")
@Transactional
public class GenerateResource {
    private static final Logger LOG = LoggerFactory.getLogger(GenerateResource.class);

    @Autowired
    private LmmpJobRepository lmmpJobRepository;

    @Autowired
    private LmmpJobWorkflow lmmpJobWorkflow;

    @GET
    @Produces("text/plain")
    public Response nop() {
        LOG.info("GET /generate does nothing... Did you mean POST /generate?");

        return Response.status(404).entity("Please POST to /generate").build();
    }

    @POST
    @Produces("application/json")
    public String generate(@SearchCriteriaParam LmmpSearchCriteria searchCriteria) {
        LOG.info("Proessing call to /generate resource");

        // create the LmmpJob
        LmmpJob lmmpJob = new LmmpJob();

        // force use of LRO
        searchCriteria.setUseLRO(true);

        // capture the output format
        lmmpJob.setOutputFormat(searchCriteria.getOutputFormat());

        // start workflow
        lmmpJobWorkflow.startFor(lmmpJob, searchCriteria);

        // persist the lmmp job
        lmmpJobRepository.add(lmmpJob);

        return "{ \"trackingId\" : \"" + lmmpJob.getUuid() + "\" }";
    }

}
