package com.example.cashCombine.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LedgerApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createImportListAndChangeCategory() throws Exception {
		String groceriesId = categoryIdByName("Groceries");
		String diningId = categoryIdByName("Dining");

		MvcResult accountResult = mockMvc.perform(post("/api/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Everyday\",\"type\":\"COMMBANK\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Everyday"))
				.andExpect(jsonPath("$.type").value("COMMBANK"))
				.andReturn();
		String accountId = readJsonField(accountResult, "id");

		byte[] csv = """
				10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
				10/07/2026,"-12.50","CAFE EXAMPLE BLEND FAKETOWN AUS","+2467.50"
				""".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile file = new MockMultipartFile("file", "sample.csv", "text/csv", csv);

		mockMvc.perform(multipart("/api/accounts/" + accountId + "/import").file(file))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.accepted").value(2))
				.andExpect(jsonPath("$.duplicate").value(0))
				.andExpect(jsonPath("$.rejected").value(0));

		mockMvc.perform(get("/api/accounts/" + accountId + "/imports"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].filename").value("sample.csv"))
				.andExpect(jsonPath("$[0].accepted").value(2));

		MvcResult txResult = mockMvc.perform(get("/api/accounts/" + accountId + "/transactions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andReturn();

		String body = txResult.getResponse().getContentAsString();
		assertThat(body).contains("WOOLWORTHS");
		assertThat(body).contains(groceriesId);

		String cafeTxId = extractTransactionIdForDescription(body, "CAFE EXAMPLE");

		mockMvc.perform(patch("/api/transactions/" + cafeTxId + "/category")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + diningId + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categoryId").value(diningId))
				.andExpect(jsonPath("$.categoryAssignmentSource").value("MANUAL"));

		MvcResult importResult = mockMvc.perform(get("/api/accounts/" + accountId + "/imports"))
				.andExpect(status().isOk())
				.andReturn();
		String importId = readJsonField(importResult, "id");

		mockMvc.perform(delete("/api/accounts/" + accountId + "/imports/" + importId))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/accounts/" + accountId + "/imports"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/accounts/" + accountId + "/transactions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/accounts/" + accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasImports").value(false));

		mockMvc.perform(delete("/api/accounts/" + accountId)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/accounts/" + accountId)).andExpect(status().isNotFound());
	}

	@Test
	void createAndDeleteRule() throws Exception {
		String streamingId = categoryIdByName("Streaming");

		MvcResult ruleResult = mockMvc.perform(post("/api/rules")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"pattern\":\"CUSTOM-STREAM-TEST\",\"categoryId\":\"" + streamingId + "\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String ruleId = readJsonField(ruleResult, "id");

		mockMvc.perform(get("/api/rules"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.pattern=='CUSTOM-STREAM-TEST')]").isNotEmpty());

		mockMvc.perform(delete("/api/rules/" + ruleId)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/rules"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.pattern=='CUSTOM-STREAM-TEST')]").isEmpty());

		mockMvc.perform(delete("/api/rules/" + ruleId)).andExpect(status().isNotFound());
	}

	@Test
	void createAndDeleteCategory() throws Exception {
		MvcResult categoryResult = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Travel\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String categoryId = readJsonField(categoryResult, "id");

		mockMvc.perform(post("/api/rules")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"pattern\":\"AIRLINE-TEST\",\"categoryId\":\"" + categoryId + "\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/categories/" + categoryId)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.name=='Travel')]").isEmpty());

		mockMvc.perform(get("/api/rules"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.pattern=='AIRLINE-TEST')]").isEmpty());

		String uncategorisedId = categoryIdByName("Uncategorised");
		mockMvc.perform(delete("/api/categories/" + uncategorisedId)).andExpect(status().isConflict());
	}

	private String categoryIdByName(String name) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/categories")).andExpect(status().isOk()).andReturn();
		String json = result.getResponse().getContentAsString();
		String needle = "\"name\":\"" + name + "\"";
		int nameIndex = json.indexOf(needle);
		assertThat(nameIndex).as("category %s should exist", name).isGreaterThanOrEqualTo(0);
		int objectStart = json.lastIndexOf('{', nameIndex);
		String marker = "\"id\":\"";
		int idStart = json.indexOf(marker, objectStart) + marker.length();
		int idEnd = json.indexOf('"', idStart);
		return json.substring(idStart, idEnd);
	}

	private static String readJsonField(MvcResult result, String field) throws Exception {
		String json = result.getResponse().getContentAsString();
		String marker = "\"" + field + "\":\"";
		int start = json.indexOf(marker) + marker.length();
		int end = json.indexOf('"', start);
		return json.substring(start, end);
	}

	private static String extractTransactionIdForDescription(String jsonArray, String descriptionFragment) {
		int descIndex = jsonArray.indexOf(descriptionFragment);
		assertThat(descIndex).isPositive();
		int objectStart = jsonArray.lastIndexOf('{', descIndex);
		String marker = "\"id\":\"";
		int idStart = jsonArray.indexOf(marker, objectStart) + marker.length();
		int idEnd = jsonArray.indexOf('"', idStart);
		return jsonArray.substring(idStart, idEnd);
	}

}
