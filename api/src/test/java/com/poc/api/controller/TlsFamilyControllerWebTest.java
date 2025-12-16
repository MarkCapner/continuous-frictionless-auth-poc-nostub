package com.poc.api.controller;

import com.poc.api.admin.controller.AdminTlsFamiliesController;
import com.poc.api.showcase.controller.ShowcaseTlsFamilyController;
import com.poc.api.showcase.persistence.TlsFamilyRepository;
import com.poc.api.showcase.service.TlsFamilyBackfillService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ShowcaseTlsFamilyController.class, AdminTlsFamiliesController.class})
@TestPropertySource(properties = "poc.admin.token=test-admin-token")
class TlsFamilyControllerWebTest {

  @Autowired MockMvc mvc;

  @MockBean TlsFamilyRepository repo;
  @MockBean TlsFamilyBackfillService backfillService;

  @Test
  void showcase_familyLookup_unknownFp_returnsNotObservedDto() throws Exception {
    Mockito.when(repo.findFamilyByRawFp("abc")).thenReturn(Optional.empty());

    mvc.perform(get("/api/showcase/tls-fp/family")
            .param("fp", "abc")
            .param("variants_limit", "12")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fp").value("abc"))
        .andExpect(jsonPath("$.notObserved").value(true))
        .andExpect(jsonPath("$.message").value("Family not yet observed"))
        .andExpect(jsonPath("$.variants").isArray())
        .andExpect(jsonPath("$.variants.length()").value(0));
  }

  @Test
  void admin_backfill_requiresToken() throws Exception {
    mvc.perform(post("/api/admin/tls-families/backfill")
            .param("batchSize", "10")
            .param("maxBatches", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  void admin_backfill_withToken_returnsOk() throws Exception {
    Mockito.when(backfillService.backfill(10, 1))
        .thenReturn(new TlsFamilyBackfillService.BackfillResult(3, 3, 1, true, "zzz"));

    mvc.perform(post("/api/admin/tls-families/backfill")
            .header("X-Admin-Token", "test-admin-token")
            .param("batchSize", "10")
            .param("maxBatches", "1")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.processed").value(3))
        .andExpect(jsonPath("$.classified").value(3))
        .andExpect(jsonPath("$.batches").value(1))
        .andExpect(jsonPath("$.complete").value(true))
        .andExpect(jsonPath("$.lastFp").value("zzz"));
  }

  @Test
  void admin_forceClassify_withoutMeta_returnsBadRequest() throws Exception {
    Mockito.when(repo.findFamilyByRawFp("fp1")).thenReturn(Optional.empty());

    mvc.perform(post("/api/admin/tls-families/force-classify")
            .header("X-Admin-Token", "test-admin-token")
            .param("fp", "fp1")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fp").value("fp1"))
        .andExpect(jsonPath("$.notObserved").value(true))
        .andExpect(jsonPath("$.message").exists());
  }
}
