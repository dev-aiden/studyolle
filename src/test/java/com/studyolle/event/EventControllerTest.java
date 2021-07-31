package com.studyolle.event;

import com.studyolle.account.form.SignUpForm;
import com.studyolle.domain.Account;
import com.studyolle.domain.Event;
import com.studyolle.domain.EventType;
import com.studyolle.domain.Study;
import com.studyolle.study.StudyControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest extends StudyControllerTest {

    @Autowired EventService eventService;
    @Autowired EnrollmentRepository enrollmentRepository;

    @BeforeEach
    void beforeEach() {
        SignUpForm signUpForm = new SignUpForm();
        signUpForm.setNickname("aiden");
        signUpForm.setEmail("test@email.com");
        signUpForm.setPassword("12345678");
        accountService.processNewAccount(signUpForm);
    }

    @Test
    @DisplayName("선착순 모임에 참가 신청 - 자동 수락")
    @WithUserDetails(value = "aiden", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void newEnrollment_to_FCFS_event_accepted() throws Exception {
        Account jeong = createAccount("jeong");
        Study study = createStudy("test-study", jeong);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, jeong);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        Account aiden = accountRepository.findByNickname("aiden");
        isAccepted(aiden, event);
    }

    @Test
    @DisplayName("선착순 모임에 참가 신청 - 대기중 (이미 인원이 꽉차서)")
    @WithUserDetails(value = "aiden", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void newEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account jeong = createAccount("jeong");
        Study study = createStudy("test-study", jeong);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, jeong);

        Account may = createAccount("may");
        Account june = createAccount("june");
        eventService.newEnrollment(event, may);
        eventService.newEnrollment(event, june);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        Account aiden = accountRepository.findByNickname("aiden");
        isNotAccepted(aiden, event);
    }

    @Test
    @DisplayName("참가신청 확정자가 선착순 모임에 참가 신청을 취소하는 경우, 바로 다음 대기자를 자동으로 신청 확인한다.")
    @WithUserDetails(value = "aiden", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void accepted_account_cancelEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account aiden = accountRepository.findByNickname("aiden");
        Account jeong = createAccount("jeong");
        Account may = createAccount("may");
        Study study = createStudy("test-study", jeong);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, jeong);

        eventService.newEnrollment(event, may);
        eventService.newEnrollment(event, aiden);
        eventService.newEnrollment(event, jeong);

        isAccepted(may, event);
        isAccepted(aiden, event);
        isNotAccepted(jeong, event);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/disenroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        isAccepted(may, event);
        isAccepted(jeong, event);
        assertNull(enrollmentRepository.findByEventAndAccount(event, aiden));
    }

    @Test
    @DisplayName("참가신청 비확정자가 선착순 모임에 참가 신청을 취소하는 경우, 기존 확정자를 그대로 유지하고 새로운 확정자는 없다.")
    @WithUserDetails(value = "aiden", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void not_accepterd_account_cancelEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account aiden = accountRepository.findByNickname("aiden");
        Account jeong = createAccount("jeong");
        Account may = createAccount("may");
        Study study = createStudy("test-study", jeong);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, jeong);

        eventService.newEnrollment(event, may);
        eventService.newEnrollment(event, jeong);
        eventService.newEnrollment(event, aiden);

        isAccepted(may, event);
        isAccepted(jeong, event);
        isNotAccepted(aiden, event);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/disenroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        isAccepted(may, event);
        isAccepted(jeong, event);
        assertNull(enrollmentRepository.findByEventAndAccount(event, aiden));
    }

    private void isNotAccepted(Account jeong, Event event) {
        assertFalse(enrollmentRepository.findByEventAndAccount(event, jeong).isAccepted());
    }

    private void isAccepted(Account account, Event event) {
        assertTrue(enrollmentRepository.findByEventAndAccount(event, account).isAccepted());
    }

    @Test
    @DisplayName("관리자 확인 모임에 참가 신청 - 대기중")
    @WithUserDetails(value = "aiden", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void newEnrollment_to_CONFIMATIVE_event_not_accepted() throws Exception {
        Account jeong = createAccount("jeong");
        Study study = createStudy("test-study", jeong);
        Event event = createEvent("test-event", EventType.CONFIRMATIVE, 2, study, jeong);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        Account aiden = accountRepository.findByNickname("aiden");
        isNotAccepted(aiden, event);
    }

    private Event createEvent(String eventTitle, EventType eventType, int limit, Study study, Account account) {
        Event event = new Event();
        event.setEventType(eventType);
        event.setLimitOfEnrollments(limit);
        event.setTitle(eventTitle);
        event.setCreatedDateTime(LocalDateTime.now());
        event.setEndEnrollmentDateTime(LocalDateTime.now().plusDays(1));
        event.setStartDateTime(LocalDateTime.now().plusDays(1).plusHours(5));
        event.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(7));
        return eventService.createEvent(event, study, account);
    }

}