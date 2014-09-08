package com.topcoder.nasa.job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.topcoder.nasa.job.LmmpJob.Status;

// CREATE TABLE Job (uuid char(36) primary key, status varchar(20), hadoop_job_id varchar(100), fail_reason varchar(200), output_format varchar(10));

/**
 * Plain JDBC implementation of a {@link LmmpJobRepository}. </p>Note that this repository takes
 * responsibility for marking all RUNNING jobs as KILLED when it is created AND when it is
 * destroyed: see {@link #killAllRunningJobs()}.
 * <p/>
 * This has the effect of:
 * <ul>
 * <li>When a server is gracefully shutdown, the jobs are updated to KILLED at that time, in case a
 * casual observer wants to check the state in the DB</li>
 * <li>When a server dies forcefully (kill -9 etc), the jobs are not updated to KILLED at that time.
 * So, the next time the server comes up, RUNNING jobs are marked as KILLED. THis is not ideal as
 * casual observers in the DB won't see the change while the server is down, but it has been deemed
 * an acceptable solution for this challenge</li>
 * </ul>
 */
@Repository
public class JdbcLmmpJobRepository implements LmmpJobRepository {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcLmmpJobRepository.class);

    @Autowired
    @Qualifier("lmmpJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    // =========================================================================

    private static final String ADD_SQL = "INSERT INTO Job (uuid, status, hadoop_job_id, fail_reason, output_format) VALUES (?,?,?,?,?)";
    private static final String UPDATE_SQL = "UPDATE Job SET status = ?, hadoop_job_id = ?, fail_reason = ?, output_format = ? WHERE uuid = ?";
    private static final String LOAD_SQL = "SELECT uuid, status, hadoop_job_id, fail_reason, output_format FROM Job WHERE uuid = ?";
    private static final String LOAD_RUNNING_HADOOP_JOBS_SQL = "SELECT uuid, status, hadoop_job_id, fail_reason, output_format FROM Job WHERE status = 'RUNNING_HADOOP'";
    private static final String LOAD_RUNNING_JOBS_SQL = "SELECT uuid, status, hadoop_job_id, fail_reason, output_format FROM Job WHERE status IN ('RUNNING_ODE', 'RUNNING_HADOOP', 'RUNNING_EXECUTABLES')";

    // =========================================================================

    /** Maps a row from the ResultSet to an LmmpJob. */
    static RowMapper<LmmpJob> LMMP_JOB_ROW_MAPPER = new RowMapper<LmmpJob>() {
        public LmmpJob mapRow(ResultSet rs, int rowNum) throws SQLException {
            return createLmmpJobFromRow(rs);
        }
    };

    // =========================================================================

    @Override
    public void add(final LmmpJob job) {
        jdbcTemplate.execute(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                return con.prepareStatement(ADD_SQL);
            }
        }, new PreparedStatementCallback<LmmpJob>() {
            public LmmpJob doInPreparedStatement(PreparedStatement ps) throws SQLException,
                    DataAccessException {
                ps.setString(1, job.getUuid());
                ps.setString(2, job.getStatus().name());
                ps.setString(3, job.getHadoopJobId());
                ps.setString(4, job.getFailInfo());
                ps.setString(5, job.getOutputFormat());

                ps.execute();

                return null;
            }
        });

    }

    @Override
    public void update(final LmmpJob job) {
        jdbcTemplate.execute(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                return con.prepareStatement(UPDATE_SQL);
            }
        }, new PreparedStatementCallback<LmmpJob>() {
            public LmmpJob doInPreparedStatement(PreparedStatement ps) throws SQLException,
                    DataAccessException {
                ps.setString(1, job.getStatus().name());
                ps.setString(2, job.getHadoopJobId());
                ps.setString(3, job.getFailInfo());
                ps.setString(4, job.getOutputFormat());
                ps.setString(5, job.getUuid());

                ps.execute();

                return null;
            }
        });
    }

    @Override
    public LmmpJob load(final String uuid) {
        LOG.debug("Loading uuid {}", uuid);

        try {
            return jdbcTemplate
                    .queryForObject(LOAD_SQL, new Object[] { uuid }, LMMP_JOB_ROW_MAPPER);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<LmmpJob> findRunningHadoopJobs() {
        return jdbcTemplate.query(LOAD_RUNNING_HADOOP_JOBS_SQL, LMMP_JOB_ROW_MAPPER);
    }

    @Override
    public List<LmmpJob> findRunningJobs() {
        return jdbcTemplate.query(LOAD_RUNNING_JOBS_SQL, LMMP_JOB_ROW_MAPPER);
    }

    /**
     * Creates an LmppJob instance from the current ResultSet's row.
     * 
     * @param rs
     *            the ResultSet to pull data from
     * @return the LmmpJob the row represents
     * @throws SQLException
     *             if something went wrong during the parse
     */
    private static LmmpJob createLmmpJobFromRow(ResultSet rs) throws SQLException {
        return new LmmpJob(rs.getString(1), // uuid
                rs.getString(2), // status
                rs.getString(3), // haoop_job_id
                rs.getString(4), // fail_reason
                rs.getString(5)); // output_format
    }

    // =========================================================================

    @PostConstruct
    @PreDestroy
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void killAllRunningJobs() {
        List<LmmpJob> runningJobs = findRunningJobs();

        for (LmmpJob job : runningJobs) {
            LOG.info("Marking job uuid {} as KILLED", job.getUuid());
            job.killed();
            update(job);
        }

    }

}
