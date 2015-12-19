package org.daniels.jhipster.myhealth.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.validation.Valid;

import org.daniels.jhipster.myhealth.domain.BloodPressure;
import org.daniels.jhipster.myhealth.repository.BloodPressureRepository;
import org.daniels.jhipster.myhealth.repository.UserRepository;
import org.daniels.jhipster.myhealth.security.AuthoritiesConstants;
import org.daniels.jhipster.myhealth.security.SecurityUtils;
import org.daniels.jhipster.myhealth.web.rest.dto.BloodPressureByPeriod;
import org.daniels.jhipster.myhealth.web.rest.util.HeaderUtil;
import org.daniels.jhipster.myhealth.web.rest.util.PaginationUtil;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;

/**
 * REST controller for managing BloodPressure.
 */
@RestController
@RequestMapping("/api")
public class BloodPressureResource {

    private final Logger log = LoggerFactory.getLogger(BloodPressureResource.class);

    @Inject
    private BloodPressureRepository bloodPressureRepository;

    @Inject
    private UserRepository userRepository;

    /**
     * POST  /bloodPressures -> Create a new bloodPressure.
     */
    @RequestMapping(value = "/bloodPressures",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<BloodPressure> create(@Valid @RequestBody BloodPressure bloodPressure) throws URISyntaxException {
        log.debug("REST request to save BloodPressure : {}", bloodPressure);
        if (bloodPressure.getId() != null) {
            return ResponseEntity.badRequest().header("Failure", "A new bloodPressure cannot already have an ID").body(null);
        }
        if (!SecurityUtils.isUserInRole(AuthoritiesConstants.ADMIN)) {
            log.debug("No user passed in, using current user: {}", SecurityUtils.getCurrentLogin());
            bloodPressure.setUser(userRepository.findOneByLogin(SecurityUtils.getCurrentLogin().getUsername()).get());
        }
        BloodPressure result = bloodPressureRepository.save(bloodPressure);
        bloodPressureRepository.save(result);
        return ResponseEntity.created(new URI("/api/bloodPressures/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("bloodPressure", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /bloodPressures -> Updates an existing bloodPressure.
     */
    @RequestMapping(value = "/bloodPressures",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<BloodPressure> update(@Valid @RequestBody BloodPressure bloodPressure) throws URISyntaxException {
        log.debug("REST request to update BloodPressure : {}", bloodPressure);
        if (bloodPressure.getId() == null) {
            return create(bloodPressure);
        }
        BloodPressure result = bloodPressureRepository.save(bloodPressure);
        bloodPressureRepository.save(bloodPressure);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("bloodPressure", bloodPressure.getId().toString()))
            .body(result);
    }

    /**
     * GET  /bloodPressures -> get all the bloodPressures.
     */
    @RequestMapping(value = "/bloodPressures",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<BloodPressure>> getAll(@RequestParam(value = "page", required = false) Integer offset,
                                                      @RequestParam(value = "per_page", required = false) Integer limit)
        throws URISyntaxException {
        Page<BloodPressure> page;
        if (SecurityUtils.isUserInRole(AuthoritiesConstants.ADMIN)) {
            page = bloodPressureRepository.findAllByOrderByTimestampDesc(PaginationUtil.generatePageRequest(offset, limit));
        } else {
            page = bloodPressureRepository.findAllForCurrentUser(PaginationUtil.generatePageRequest(offset, limit));
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/bloodPressures", offset, limit);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /bp-by-days -> get all the blood pressure readings by last x days.
     */
    @RequestMapping(value = "/bp-by-days/{days}")
    @Timed
    public ResponseEntity<BloodPressureByPeriod> getByDays(@PathVariable int days) {
        LocalDate today = new LocalDate();
        LocalDate previousDate = today.minusDays(days);
        DateTime daysAgo = previousDate.toDateTimeAtCurrentTime();
        DateTime rightNow = today.toDateTimeAtCurrentTime();

        List<BloodPressure> readings = bloodPressureRepository.findAllByTimestampBetweenOrderByTimestampDesc(daysAgo, rightNow);
        BloodPressureByPeriod response = new BloodPressureByPeriod("Last " + days + " Days", filterByUser(readings));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * GET  /bp-by-days -> get all the blood pressure readings for a particular month.
     */
    @RequestMapping(value = "/bp-by-month/{date}")
    @Timed
    public ResponseEntity<BloodPressureByPeriod> getByMonth(@PathVariable @DateTimeFormat(pattern = "yyyy-MM") LocalDate date) {
        LocalDate firstDay = date.dayOfMonth().withMinimumValue();
        LocalDate lastDay = date.dayOfMonth().withMaximumValue();

        List<BloodPressure> readings = bloodPressureRepository.
            findAllByTimestampBetweenOrderByTimestampDesc(firstDay.toDateTimeAtStartOfDay(),
                                                          lastDay.plusDays(1).toDateTimeAtStartOfDay());

        DateTimeFormatter fmt = org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM");
        String yearAndMonth = fmt.print(firstDay);

        BloodPressureByPeriod response = new BloodPressureByPeriod(yearAndMonth, filterByUser(readings));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private List<BloodPressure> filterByUser(List<BloodPressure> readings) {
        Stream<BloodPressure> userReadings = readings.stream()
            .filter(bp -> bp.getUser().getLogin().equals(SecurityUtils.getCurrentLogin()));
        return userReadings.collect(Collectors.toList());
    }

    /**
     * GET  /bloodPressures/:id -> get the "id" bloodPressure.
     */
    @RequestMapping(value = "/bloodPressures/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<BloodPressure> get(@PathVariable Long id) {
        log.debug("REST request to get BloodPressure : {}", id);
        return Optional.ofNullable(bloodPressureRepository.findOne(id))
            .map(bloodPressure -> new ResponseEntity<>(
                bloodPressure,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /bloodPressures/:id -> delete the "id" bloodPressure.
     */
    @RequestMapping(value = "/bloodPressures/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST request to delete BloodPressure : {}", id);
        bloodPressureRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("bloodPressure", id.toString())).build();
    }

    /**
     * SEARCH  /_search/bloodPressures/:query -> search for the bloodPressure corresponding
     * to the query.
     */
    @RequestMapping(value = "/_search/bloodPressures/{query}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<BloodPressure> search(@PathVariable String query) {
    	return Lists.newArrayList();
//        return StreamSupport
//            .stream(bloodPressureRepository.search(query).spliterator(), false)
//            .collect(Collectors.toList());
    }
}
