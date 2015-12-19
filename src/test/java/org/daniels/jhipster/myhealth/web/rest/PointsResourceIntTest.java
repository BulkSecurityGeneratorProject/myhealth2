package org.daniels.jhipster.myhealth.web.rest;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.daniels.jhipster.myhealth.Application;
import org.daniels.jhipster.myhealth.domain.Points;
import org.daniels.jhipster.myhealth.domain.User;
import org.daniels.jhipster.myhealth.repository.PointsRepository;
import org.daniels.jhipster.myhealth.repository.UserRepository;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;


/**
 * Test class for the PointsResource REST controller.
 *
 * @see PointsResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest
public class PointsResourceIntTest {


    private static final Integer DEFAULT_EXERCISE = 1;
    private static final Integer UPDATED_EXERCISE = 2;

    private static final Integer DEFAULT_MEALS = 1;
    private static final Integer UPDATED_MEALS = 2;

    private static final Integer DEFAULT_ALCOHOL = 1;
    private static final Integer UPDATED_ALCOHOL = 2;
    private static final String DEFAULT_NOTES = "SAMPLE_TEXT";
    private static final String UPDATED_NOTES = "UPDATED_TEXT";
    private static final java.time.LocalDate DEFAULT_DATE = java.time.LocalDate.now();

    @Inject
    private PointsRepository pointsRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    private MockMvc restPointsMockMvc;

    private Points points;

    @Autowired
    private WebApplicationContext context;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PointsResource pointsResource = new PointsResource();
        ReflectionTestUtils.setField(pointsResource, "pointsRepository", pointsRepository);
        ReflectionTestUtils.setField(pointsResource, "userRepository", userRepository);
        this.restPointsMockMvc = MockMvcBuilders.standaloneSetup(pointsResource).setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        points = new Points();
        points.setDate(DEFAULT_DATE);
        points.setExercise(DEFAULT_EXERCISE);
        points.setMeals(DEFAULT_MEALS);
        points.setAlcohol(DEFAULT_ALCOHOL);
        points.setNotes(DEFAULT_NOTES);
    }

    @Test
    @Transactional
    public void createPoints() throws Exception {
        int databaseSizeBeforeCreate = pointsRepository.findAll().size();

        // create security-aware mockMvc
        restPointsMockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        // Create the Points
        restPointsMockMvc.perform(post("/api/points")
                .with(user("user")).with(csrf())
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(points)))
                .andExpect(status().isCreated());

        // Validate the Points in the database
        List<Points> points = pointsRepository.findAll();
        assertThat(points).hasSize(databaseSizeBeforeCreate + 1);
        Points testPoints = points.get(points.size() - 1);
        assertThat(testPoints.getExercise()).isEqualTo(DEFAULT_EXERCISE);
        assertThat(testPoints.getMeals()).isEqualTo(DEFAULT_MEALS);
        assertThat(testPoints.getAlcohol()).isEqualTo(DEFAULT_ALCOHOL);
        assertThat(testPoints.getNotes()).isEqualTo(DEFAULT_NOTES);
    }

    @Test
    @Transactional
    public void checkDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = pointsRepository.findAll().size();
        // set the field null
        points.setDate(null);

        // Create the Points, which fails.

        restPointsMockMvc.perform(post("/api/points")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(points)))
                .andExpect(status().isBadRequest());

        List<Points> points = pointsRepository.findAll();
        assertThat(points).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllPoints() throws Exception {
        // Initialize the database
        pointsRepository.saveAndFlush(points);

        // create security-aware mockMvc
        restPointsMockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        // Get all the points
        restPointsMockMvc.perform(get("/api/points")
            .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(points.getId().intValue())))
            .andExpect(jsonPath("$.[*].exercise").value(hasItem(DEFAULT_EXERCISE)))
            .andExpect(jsonPath("$.[*].meals").value(hasItem(DEFAULT_MEALS)))
            .andExpect(jsonPath("$.[*].alcohol").value(hasItem(DEFAULT_ALCOHOL)))
            .andExpect(jsonPath("$.[*].notes").value(hasItem(DEFAULT_NOTES.toString())));
    }

    @Test
    @Transactional
    public void getPoints() throws Exception {
        // Initialize the database
        pointsRepository.saveAndFlush(points);

        // Get the points
        restPointsMockMvc.perform(get("/api/points/{id}", points.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(points.getId().intValue()))
            .andExpect(jsonPath("$.exercise").value(DEFAULT_EXERCISE))
            .andExpect(jsonPath("$.meals").value(DEFAULT_MEALS))
            .andExpect(jsonPath("$.alcohol").value(DEFAULT_ALCOHOL))
            .andExpect(jsonPath("$.notes").value(DEFAULT_NOTES.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingPoints() throws Exception {
        // Get the points
        restPointsMockMvc.perform(get("/api/points/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updatePoints() throws Exception {
        // Initialize the database
        pointsRepository.saveAndFlush(points);

		int databaseSizeBeforeUpdate = pointsRepository.findAll().size();

        // Update the points
		points.setDate(DEFAULT_DATE);
        points.setExercise(UPDATED_EXERCISE);
        points.setMeals(UPDATED_MEALS);
        points.setAlcohol(UPDATED_ALCOHOL);
        points.setNotes(UPDATED_NOTES);


        restPointsMockMvc.perform(put("/api/points")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(points)))
                .andExpect(status().isOk());

        // Validate the Points in the database
        List<Points> points = pointsRepository.findAll();
        assertThat(points).hasSize(databaseSizeBeforeUpdate);
        Points testPoints = points.get(points.size() - 1);
        assertThat(testPoints.getExercise()).isEqualTo(UPDATED_EXERCISE);
        assertThat(testPoints.getMeals()).isEqualTo(UPDATED_MEALS);
        assertThat(testPoints.getAlcohol()).isEqualTo(UPDATED_ALCOHOL);
        assertThat(testPoints.getNotes()).isEqualTo(UPDATED_NOTES);
    }

    @Test
    @Transactional
    public void deletePoints() throws Exception {
        // Initialize the database
        pointsRepository.saveAndFlush(points);

		int databaseSizeBeforeDelete = pointsRepository.findAll().size();

        // Get the points
        restPointsMockMvc.perform(delete("/api/points/{id}", points.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Points> points = pointsRepository.findAll();
        assertThat(points).hasSize(databaseSizeBeforeDelete - 1);
    }


}
